package com.school.management.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:School Management System}")
    private String appName;

    /**
     * Send school registration email to admin
     */
    @Async
    public void sendSchoolRegistrationEmail(
            String schoolName,
            String adminName,
            String adminEmail,
            String schoolId) {

        try {
            Context context = new Context();
            context.setVariable("schoolName", schoolName);
            context.setVariable("adminName", adminName);
            context.setVariable("schoolId", schoolId);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/school-registration", context);

            sendHtmlEmail(
                    adminEmail,
                    "Welcome to " + appName + " - School Registered Successfully",
                    htmlContent
            );

            log.info("School registration email sent to: {}", adminEmail);

        } catch (Exception e) {
            log.error("Failed to send school registration email to: {}", adminEmail, e);
        }
    }

    /**
     * Send staff creation email with credentials
     */
    @Async
    public void sendStaffCreationEmail(
            String schoolName,
            String staffName,
            String staffEmail,
            String password,
            String designation,
            String department) {

        try {
            Context context = new Context();
            context.setVariable("schoolName", schoolName);
            context.setVariable("staffName", staffName);
            context.setVariable("email", staffEmail);
            context.setVariable("password", password);
            context.setVariable("designation", designation);
            context.setVariable("department", department);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/staff-creation", context);

            sendHtmlEmail(
                    staffEmail,
                    "Welcome to " + schoolName + " - Your Account Details",
                    htmlContent
            );

            log.info("Staff creation email sent to: {}", staffEmail);

        } catch (Exception e) {
            log.error("Failed to send staff creation email to: {}", staffEmail, e);
        }
    }

    /**
     * Send parent account creation email with credentials
     */
    @Async
    public void sendParentAccountEmail(
            String schoolName,
            String parentName,
            String parentEmail,
            String password,
            List<String> studentNames) {

        try {
            Context context = new Context();
            context.setVariable("schoolName", schoolName);
            context.setVariable("parentName", parentName);
            context.setVariable("email", parentEmail);
            context.setVariable("password", password);
            context.setVariable("studentNames", studentNames);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/parent-account", context);

            sendHtmlEmail(
                    parentEmail,
                    "Parent Portal Access - " + schoolName,
                    htmlContent
            );

            log.info("Parent account email sent to: {}", parentEmail);

        } catch (Exception e) {
            log.error("Failed to send parent account email to: {}", parentEmail, e);
        }
    }

    /**
     * Send fee payment receipt
     */
    @Async
    public void sendFeePaymentReceiptEmail(
            String parentEmail,
            String feeName,
            byte[] receiptPdf,
            String schoolName) {

        try {
            Context context = new Context();
            context.setVariable("schoolName", schoolName);
            context.setVariable("feeName", feeName);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/fee-receipt", context);

            sendHtmlEmailWithAttachment(
                    parentEmail,
                    "Fee Payment Receipt - " + schoolName,
                    htmlContent,
                    "Fee_Receipt.pdf",
                    receiptPdf
            );

            log.info("Fee receipt email sent to: {}", parentEmail);

        } catch (Exception e) {
            log.error("Failed to send fee receipt email to: {}", parentEmail, e);
        }
    }

    /**
     * Send payslip to staff
     */
    @Async
    public void sendPayslipEmail(
            String staffEmail,
            String month,
            Integer year,
            byte[] payslipPdf,
            String schoolName) {

        try {
            Context context = new Context();
            context.setVariable("schoolName", schoolName);
            context.setVariable("month", month);
            context.setVariable("year", year);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/payslip", context);

            sendHtmlEmailWithAttachment(
                    staffEmail,
                    "Payslip for " + month + " " + year + " - " + schoolName,
                    htmlContent,
                    "Payslip_" + month + "_" + year + ".pdf",
                    payslipPdf
            );

            log.info("Payslip email sent to: {}", staffEmail);

        } catch (Exception e) {
            log.error("Failed to send payslip email to: {}", staffEmail, e);
        }
    }

    /**
     * Send offer letter to staff
     */
    @Async
    public void sendOfferLetterEmail(
            String staffEmail,
            String staffName,
            String designation,
            byte[] offerLetterPdf,
            String schoolName) {

        try {
            Context context = new Context();
            context.setVariable("schoolName", schoolName);
            context.setVariable("staffName", staffName);
            context.setVariable("designation", designation);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/offer-letter", context);

            sendHtmlEmailWithAttachment(
                    staffEmail,
                    "Offer Letter - " + designation + " - " + schoolName,
                    htmlContent,
                    "Offer_Letter.pdf",
                    offerLetterPdf
            );

            log.info("Offer letter email sent to: {}", staffEmail);

        } catch (Exception e) {
            log.error("Failed to send offer letter email to: {}", staffEmail, e);
        }
    }

    /**
     * Send report card to parent
     */
    @Async
    public void sendReportCardEmail(
            String parentEmail,
            String studentName,
            String examName,
            byte[] reportCardPdf,
            String schoolName) {

        try {
            Context context = new Context();
            context.setVariable("schoolName", schoolName);
            context.setVariable("studentName", studentName);
            context.setVariable("examName", examName);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/report-card", context);

            sendHtmlEmailWithAttachment(
                    parentEmail,
                    "Report Card - " + examName + " - " + studentName,
                    htmlContent,
                    "Report_Card_" + studentName.replaceAll(" ", "_") + ".pdf",
                    reportCardPdf
            );

            log.info("Report card email sent to: {}", parentEmail);

        } catch (Exception e) {
            log.error("Failed to send report card email to: {}", parentEmail, e);
        }
    }

    /**
     * Send student ID card
     */
    @Async
    public void sendIDCardEmail(
            String parentEmail,
            String studentName,
            byte[] idCardPdf,
            String schoolName) {

        try {
            Context context = new Context();
            context.setVariable("schoolName", schoolName);
            context.setVariable("studentName", studentName);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/id-card", context);

            sendHtmlEmailWithAttachment(
                    parentEmail,
                    "Student ID Card - " + studentName,
                    htmlContent,
                    "ID_Card_" + studentName.replaceAll(" ", "_") + ".pdf",
                    idCardPdf
            );

            log.info("ID card email sent to: {}", parentEmail);

        } catch (Exception e) {
            log.error("Failed to send ID card email to: {}", parentEmail, e);
        }
    }

    /**
     * Send leave application notification to admin
     */
    @Async
    public void sendLeaveApplicationEmail(
            String adminEmail,
            String applicantName,
            String leaveType,
            String startDate,
            String endDate,
            String reason,
            String schoolName) {

        try {
            Context context = new Context();
            context.setVariable("schoolName", schoolName);
            context.setVariable("applicantName", applicantName);
            context.setVariable("leaveType", leaveType);
            context.setVariable("startDate", startDate);
            context.setVariable("endDate", endDate);
            context.setVariable("reason", reason);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/leave-application", context);

            sendHtmlEmail(
                    adminEmail,
                    "New Leave Application - " + applicantName,
                    htmlContent
            );

            log.info("Leave application email sent to: {}", adminEmail);

        } catch (Exception e) {
            log.error("Failed to send leave application email to: {}", adminEmail, e);
        }
    }

    /**
     * Send leave status update to applicant
     */
    @Async
    public void sendLeaveStatusEmail(
            String applicantEmail,
            String applicantName,
            String leaveType,
            String status,
            String schoolName) {

        try {
            Context context = new Context();
            context.setVariable("schoolName", schoolName);
            context.setVariable("applicantName", applicantName);
            context.setVariable("leaveType", leaveType);
            context.setVariable("status", status);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/leave-status", context);

            sendHtmlEmail(
                    applicantEmail,
                    "Leave Application " + status + " - " + schoolName,
                    htmlContent
            );

            log.info("Leave status email sent to: {}", applicantEmail);

        } catch (Exception e) {
            log.error("Failed to send leave status email to: {}", applicantEmail, e);
        }
    }

    /**
     * Send library book issue notification
     */
    @Async
    public void sendBookIssueEmail(
            String userEmail,
            String userName,
            String bookTitle,
            String dueDate,
            String schoolName) {

        try {
            Context context = new Context();
            context.setVariable("schoolName", schoolName);
            context.setVariable("userName", userName);
            context.setVariable("bookTitle", bookTitle);
            context.setVariable("dueDate", dueDate);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/book-issue", context);

            sendHtmlEmail(
                    userEmail,
                    "Book Issued - " + bookTitle,
                    htmlContent
            );

            log.info("Book issue email sent to: {}", userEmail);

        } catch (Exception e) {
            log.error("Failed to send book issue email to: {}", userEmail, e);
        }
    }

    /**
     * Send library book overdue reminder
     */
    @Async
    public void sendBookOverdueEmail(
            String userEmail,
            String userName,
            String bookTitle,
            String dueDate,
            String fineAmount,
            String schoolName) {

        try {
            Context context = new Context();
            context.setVariable("schoolName", schoolName);
            context.setVariable("userName", userName);
            context.setVariable("bookTitle", bookTitle);
            context.setVariable("dueDate", dueDate);
            context.setVariable("fineAmount", fineAmount);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/book-overdue", context);

            sendHtmlEmail(
                    userEmail,
                    "Overdue Book Reminder - " + bookTitle,
                    htmlContent
            );

            log.info("Book overdue email sent to: {}", userEmail);

        } catch (Exception e) {
            log.error("Failed to send book overdue email to: {}", userEmail, e);
        }
    }

    /**
     * Send announcement notification
     */
    @Async
    public void sendAnnouncementEmail(
            String recipientEmail,
            String recipientName,
            String title,
            String message,
            String schoolName) {

        try {
            Context context = new Context();
            context.setVariable("schoolName", schoolName);
            context.setVariable("recipientName", recipientName);
            context.setVariable("title", title);
            context.setVariable("message", message);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/announcement", context);

            sendHtmlEmail(
                    recipientEmail,
                    "Announcement: " + title,
                    htmlContent
            );

            log.info("Announcement email sent to: {}", recipientEmail);

        } catch (Exception e) {
            log.error("Failed to send announcement email to: {}", recipientEmail, e);
        }
    }

    /**
     * Send complaint acknowledgment
     */
    @Async
    public void sendComplaintAcknowledgmentEmail(
            String parentEmail,
            String parentName,
            String complaintTitle,
            String complaintId,
            String schoolName) {

        try {
            Context context = new Context();
            context.setVariable("schoolName", schoolName);
            context.setVariable("parentName", parentName);
            context.setVariable("complaintTitle", complaintTitle);
            context.setVariable("complaintId", complaintId);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/complaint-acknowledgment", context);

            sendHtmlEmail(
                    parentEmail,
                    "Complaint Received - " + complaintTitle,
                    htmlContent
            );

            log.info("Complaint acknowledgment email sent to: {}", parentEmail);

        } catch (Exception e) {
            log.error("Failed to send complaint acknowledgment email to: {}", parentEmail, e);
        }
    }

    /**
     * Send complaint status update
     */
    @Async
    public void sendComplaintStatusEmail(
            String parentEmail,
            String parentName,
            String complaintTitle,
            String status,
            String schoolName) {

        try {
            Context context = new Context();
            context.setVariable("schoolName", schoolName);
            context.setVariable("parentName", parentName);
            context.setVariable("complaintTitle", complaintTitle);
            context.setVariable("status", status);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/complaint-status", context);

            sendHtmlEmail(
                    parentEmail,
                    "Complaint Status Update - " + complaintTitle,
                    htmlContent
            );

            log.info("Complaint status email sent to: {}", parentEmail);

        } catch (Exception e) {
            log.error("Failed to send complaint status email to: {}", parentEmail, e);
        }
    }

    /**
     * Send password reset email
     */
    @Async
    public void sendPasswordResetEmail(
            String userEmail,
            String userName,
            String resetToken,
            String schoolName) {

        try {
            Context context = new Context();
            context.setVariable("schoolName", schoolName);
            context.setVariable("userName", userName);
            context.setVariable("resetToken", resetToken);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/password-reset", context);

            sendHtmlEmail(
                    userEmail,
                    "Password Reset Request - " + schoolName,
                    htmlContent
            );

            log.info("Password reset email sent to: {}", userEmail);

        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", userEmail, e);
        }
    }

    /**
     * Send bulk email (for announcements to multiple recipients)
     */
    @Async
    public void sendBulkEmail(
            List<String> recipientEmails,
            String subject,
            String htmlContent) {

        for (String email : recipientEmails) {
            try {
                sendHtmlEmail(email, subject, htmlContent);
                Thread.sleep(100); // Small delay to avoid overwhelming SMTP server
            } catch (Exception e) {
                log.error("Failed to send bulk email to: {}", email, e);
            }
        }

        log.info("Bulk email sent to {} recipients", recipientEmails.size());
    }

    // ============= HELPER METHODS =============

    /**
     * Send plain HTML email
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent)
            throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * Send HTML email with PDF attachment
     */
    private void sendHtmlEmailWithAttachment(
            String to,
            String subject,
            String htmlContent,
            String attachmentName,
            byte[] attachmentData) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        helper.addAttachment(
                attachmentName,
                new ByteArrayResource(attachmentData)
        );

        mailSender.send(message);
    }
}