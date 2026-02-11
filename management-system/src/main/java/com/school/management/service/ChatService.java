package com.school.management.service;

import com.school.management.constant.MessageStatus;
import com.school.management.dto.request.SendMessageRequest;
import com.school.management.dto.response.ConversationResponse;
import com.school.management.dto.response.MessageResponse;
import com.school.management.dto.response.MessagesResponse;
import com.school.management.dto.response.UserResponse;
import com.school.management.entity.*;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.exception.UnauthorizedException;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final SimpMessagingTemplate messagingTemplate; // For WebSocket

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations() {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<Conversation> conversations = conversationRepository
                .findBySchoolIdAndUserId(schoolId, userId);

        return conversations.stream()
                .map(conversation -> {
                    // Determine other user
                    User otherUser = conversation.getUserA().getId().equals(userId) ?
                            conversation.getUserB() : conversation.getUserA();

                    // Get last message
                    Message lastMessage = messageRepository
                            .findLatestByConversationId(conversation.getId())
                            .orElse(null);

                    // Count unread messages
                    Long unreadCount = messageRepository
                            .countByConversation_IdAndReceiver_IdAndStatusNotAndDeletedAtIsNull(
                                    conversation.getId(),
                                    userId,
                                    MessageStatus.READ
                            );

                    return ConversationResponse.builder()
                            .id(conversation.getId())
                            .otherUser(mapToUserResponse(otherUser))
                            .lastMessage(lastMessage != null ?
                                    mapToMessageResponse(lastMessage) : null)
                            .unreadCount(unreadCount.intValue())
                            .updatedAt(conversation.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ConversationResponse getOrCreateConversation(UUID otherUserId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        if (currentUserId.equals(otherUserId)) {
            throw new BadRequestException("Cannot chat with yourself");
        }

        // Verify other user exists in same school
        User otherUser = userRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                        otherUserId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found or not available for chat"));

        // Check for existing conversation
        Conversation conversation = conversationRepository
                .findBySchoolIdAndBothUsers(schoolId, currentUserId, otherUserId)
                .orElse(null);

        if (conversation == null) {
            // Create new conversation
            School school = new School();
            school.setId(schoolId);

            User currentUser = new User();
            currentUser.setId(currentUserId);

            conversation = Conversation.builder()
                    .school(school)
                    .userA(currentUser)
                    .userB(otherUser)
                    .lastMessageAt(LocalDateTime.now())
                    .build();

            conversation = conversationRepository.save(conversation);
        }

        return ConversationResponse.builder()
                .id(conversation.getId())
                .otherUser(mapToUserResponse(otherUser))
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public MessagesResponse getMessages(UUID conversationId, Integer limit,
                                        LocalDateTime cursor) {
        UUID userId = SecurityUtils.getCurrentUserId();

        // Verify participant
        Conversation conversation = conversationRepository
                .findByIdAndDeletedAtIsNull(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getUserA().getId().equals(userId) &&
                !conversation.getUserB().getId().equals(userId)) {
            throw new UnauthorizedException("Not authorized to view messages");
        }

        Pageable pageable = PageRequest.of(0, limit != null ? limit : 50);

        List<Message> messages;
        if (cursor != null) {
            messages = messageRepository.findByConversationIdBeforeCursor(
                    conversationId, cursor, pageable);
        } else {
            messages = messageRepository.findByConversation_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
                    conversationId, pageable);
        }

        // Reverse for chronological order
        List<MessageResponse> messageResponses = messages.stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());
        java.util.Collections.reverse(messageResponses);

        LocalDateTime nextCursor = messages.isEmpty() ? null :
                messages.get(0).getCreatedAt();

        return MessagesResponse.builder()
                .messages(messageResponses)
                .nextCursor(nextCursor)
                .build();
    }

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request) {
        UUID senderId = SecurityUtils.getCurrentUserId();

        // Verify participant
        Conversation conversation = conversationRepository
                .findByIdAndDeletedAtIsNull(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getUserA().getId().equals(senderId) &&
                !conversation.getUserB().getId().equals(senderId)) {
            throw new UnauthorizedException("Not authorized");
        }

        UUID receiverId = conversation.getUserA().getId().equals(senderId) ?
                conversation.getUserB().getId() : conversation.getUserA().getId();

        User sender = new User();
        sender.setId(senderId);

        User receiver = new User();
        receiver.setId(receiverId);

        // Create message
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() :
                        com.school.management.constant.MessageType.TEXT)
                .status(MessageStatus.SENT)
                .build();

        message = messageRepository.save(message);

        // Update conversation timestamp
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Send via WebSocket (best effort)
        try {
            MessageResponse response = mapToMessageResponse(message);
            messagingTemplate.convertAndSend(
                    "/topic/user/" + receiverId + "/messages", response);
            messagingTemplate.convertAndSend(
                    "/topic/user/" + senderId + "/messages", response);
        } catch (Exception e) {
            log.error("Failed to send WebSocket message", e);
        }

        return mapToMessageResponse(message);
    }

    @Transactional
    public MessageResponse markRead(UUID messageId) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Message message = messageRepository.findByIdAndReceiver_IdAndDeletedAtIsNull(
                        messageId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Message not found or not receiver"));

        if (message.getStatus() != MessageStatus.READ) {
            message.setStatus(MessageStatus.READ);
            message = messageRepository.save(message);

            // Notify sender via WebSocket
            try {
                messagingTemplate.convertAndSend(
                        "/topic/user/" + message.getSender().getId() + "/read-receipts",
                        Map.of("messageId", messageId,
                                "conversationId", message.getConversation().getId())
                );
            } catch (Exception e) {
                log.error("Failed to send read receipt", e);
            }
        }

        return mapToMessageResponse(message);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getChatUsers(String search) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<User> users;

        if (search != null && !search.isEmpty()) {
            users = userRepository.findChatUsersExcludingCurrentWithSearch(
                    schoolId, currentUserId, search);
        } else {
            users = userRepository.findChatUsersExcludingCurrent(schoolId, currentUserId);
        }

        return users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    // ============= MAPPERS =============

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private MessageResponse mapToMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .type(message.getType())
                .status(message.getStatus())
                .senderId(message.getSender().getId())
                .receiverId(message.getReceiver().getId())
                .conversationId(message.getConversation().getId())
                .createdAt(message.getCreatedAt())
                .build();
    }
}