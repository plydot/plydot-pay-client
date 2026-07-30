package com.plydot.pay.model

import java.time.Instant
import java.util.UUID

enum class PayoutAccountType {
    BANK,
    MOMO,
}

enum class SettlementPayoutStatus {
    PENDING_VERIFICATION,
    VERIFYING,
    VERIFIED,
    VERIFICATION_FAILED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
}

data class SettlementBalanceResponse(
    val availableMinor: Long,
    val reservedMinor: Long,
    val currency: String,
    val unsettledPaymentCount: Int,
)

data class PayoutAccountResponse(
    val merchantId: UUID,
    val accountType: PayoutAccountType,
    val accountName: String,
    val accountNumber: String,
    val bankName: String?,
    val bankCode: String?,
    val currency: String,
    val active: Boolean,
    val updatedAt: Instant,
)

/** Omit [amountMinor] to request the full available balance. */
data class SubmitPayoutRequest(
    val amountMinor: Long? = null,
)

data class SettlementPayoutItemResponse(
    val id: UUID,
    val paymentId: UUID,
    val amountMinor: Long,
    val providerReference: String?,
    val verifiedAt: Instant?,
    val verificationError: String?,
)

data class SettlementPayoutResponse(
    val id: UUID,
    val merchantId: UUID,
    val amountMinor: Long,
    val currency: String,
    val status: SettlementPayoutStatus,
    val requestedBy: String,
    val destinationSnapshot: Map<String, Any?> = emptyMap(),
    val platformNote: String? = null,
    val failureReason: String? = null,
    val verifiedAt: Instant? = null,
    val completedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val items: List<SettlementPayoutItemResponse> = emptyList(),
)
