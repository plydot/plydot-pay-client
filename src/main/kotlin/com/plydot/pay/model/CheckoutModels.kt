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
    /** Assigned platform provider (from GET /v1/providers). */
    val providerId: UUID,
    /** Payer / payment-method catalog UUID nested under that provider. */
    val payerId: UUID,
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
    val providerId: UUID? = null,
    val payerId: UUID? = null,
    val failureReason: String? = null,
    val amountMinor: Long,
    val currency: String,
    val description: String?,
    val metadata: Map<String, Any?>? = null,
    val status: CheckoutStatus,
    val expiresAt: Instant? = null,
    val createdAt: Instant,
)

data class PayCheckoutRequest(
    val payerRef: String? = null,
)

data class ProviderOptionResponse(
    val code: String,
    val displayName: String,
    val providerId: UUID,
    val providerName: String,
    val payers: List<PayerOptionResponse>,
)

data class PayerOptionResponse(
    val id: UUID,
    val code: String,
    val displayName: String,
)
