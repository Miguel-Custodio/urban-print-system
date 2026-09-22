package com.urbanprint.backend.model;

public enum OrderStatus {
    PENDING,
    IN_PRODUCTION,
    QUALITY_CHECK,
    READY_FOR_PICKUP,
    DELIVERED,
    CANCELLED
}