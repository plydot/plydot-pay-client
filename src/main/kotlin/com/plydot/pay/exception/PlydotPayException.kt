package com.plydot.pay.exception

import com.plydot.pay.model.ErrorResponse

class PlydotPayException(
    val error: ErrorResponse,
    val httpStatus: Int,
) : RuntimeException("${error.code}: ${error.message}") {

    val code: String get() = error.code

    fun isCheckoutExpired(): Boolean = code == "CHECKOUT_EXPIRED"

    fun isCheckoutNotPayable(): Boolean = code == "CHECKOUT_NOT_PAYABLE"

    fun isPaymentInProgress(): Boolean = code == "PAYMENT_IN_PROGRESS"

    fun isIdempotencyConflict(): Boolean =
        code == "IDEMPOTENCY_KEY_IN_PROGRESS" || code == "IDEMPOTENCY_KEY_REUSED"

    fun isValidationError(): Boolean = code == "VALIDATION_ERROR"

    fun isUnauthorized(): Boolean = code == "UNAUTHORIZED"

    fun isForbidden(): Boolean = code == "FORBIDDEN"

    fun isNotFound(): Boolean = code.endsWith("_NOT_FOUND") || code == "NOT_FOUND"

    fun isMerchantContextRequired(): Boolean = code == "MERCHANT_CONTEXT_REQUIRED"

    fun isInsufficientBalance(): Boolean = code == "INSUFFICIENT_BALANCE"

    fun isPayoutAlreadyOpen(): Boolean = code == "PAYOUT_ALREADY_OPEN"

    fun isPayoutAccountNotConfigured(): Boolean = code == "PAYOUT_ACCOUNT_NOT_FOUND"
}
