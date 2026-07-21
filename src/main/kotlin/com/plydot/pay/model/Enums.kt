package com.plydot.pay.model

enum class CheckoutStatus {
    PENDING,
    COMPLETED,
    EXPIRED,
    CANCELLED,
    REFUNDED,
}

enum class PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    REFUNDED,
    STALE,
}
