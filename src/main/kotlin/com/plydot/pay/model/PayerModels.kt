package com.plydot.pay.model

import java.util.UUID

data class PayerResponse(
    val id: UUID,
    val code: String,
    val displayName: String,
    val active: Boolean,
)

data class CreatePayerRequest(
    val code: String,
    val displayName: String,
)

data class UpdatePayerRequest(
    val displayName: String? = null,
    val active: Boolean? = null,
)

data class AssignProviderPayersRequest(
    val payerIds: List<UUID>,
)

data class ProviderPayerAssignmentResponse(
    val providerId: UUID,
    val payerIds: List<UUID>,
)
