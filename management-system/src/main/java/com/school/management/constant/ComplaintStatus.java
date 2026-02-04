package com.school.management.constant;

public enum ComplaintStatus {
    Open("Open"),
    In_Progress("In Progress"),
    Resolved("Resolved"),
    Closed("Closed");

    private final String displayName;

    ComplaintStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}