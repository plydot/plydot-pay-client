package com.plydot.pay.model

import java.util.UUID

data class PaymentMethodResponse(
    val id: UUID,
    val code: String,
    val displayName: String,
    val active: Boolean,
)
