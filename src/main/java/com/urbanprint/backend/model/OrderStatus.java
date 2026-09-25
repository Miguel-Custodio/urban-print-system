package com.urbanprint.backend.model;

public enum OrderStatus {
    PENDING_PAYMENT("Pending Payment"),
    PENDING_FILE("Pending File"),
    GRAPHICS("Graphics"),
    SF_QUEUE("Small Format Queue"),
    SF_PRINTING("SF Printing"),
    WF_QUEUE("Wide Format Queue"),
    WF_PRINTING("WF Printing"),
    FINISHING("Finishing"),
    READY_FOR_PICKUP("Ready for Pick Up"),
    SHIPPING("Shipping"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}