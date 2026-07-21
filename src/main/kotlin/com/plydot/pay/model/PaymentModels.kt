package com.plydot.pay.model

import java.time.Instant
import java.util.UUID

data class PaymentResponse(
    val id: UUID,
    val checkoutId: UUID,
    val merchantId: UUID,
    val amountMinor: Long,
    val currency: String,
    val status: PaymentStatus,
    val providerReference: String?,
    val refundedAmountMinor: Long = 0,
    val instructions: Map<String, Any?>? = null,
    val createdAt: Instant,
)

data class PayCheckoutResponse(
    val checkoutId: UUID,
    val payment: PaymentResponse,
)
