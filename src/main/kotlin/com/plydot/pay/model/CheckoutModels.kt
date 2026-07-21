package com.plydot.pay.model

import java.time.Instant
import java.util.UUID

data class CreateCheckoutRequest(
    val merchantId: UUID? = null,
    val productId: String? = null,
    val packageId: UUID? = null,
    val amountMinor: Long? = null,
    val currency: String? = null,
    val accountRef: String? = null,
    val description: String? = null,
    val metadata: Map<String, Any?>? = null,
)

data class CreateCheckoutResponse(
    val id: UUID,
    val merchantId: UUID,
    val productId: String?,
    val catalogProductId: UUID? = null,
    val packageId: UUID? = null,
    val creditTierId: UUID? = null,
    val creditsToGrant: Long? = null,
    val ugxPerCredit: Long? = null,
    val accountRef: String?,
    val amountMinor: Long,
    val currency: String,
    val description: String?,
    val metadata: Map<String, Any?>? = null,
    val status: CheckoutStatus,
    val expiresAt: Instant? = null,
    val createdAt: Instant,
)

data class PayCheckoutRequest(
    val paymentMethodCode: String,
    val payerRef: String? = null,
)
