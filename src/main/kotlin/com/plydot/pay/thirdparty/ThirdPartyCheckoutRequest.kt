package com.plydot.pay.thirdparty

import com.plydot.pay.model.CreateCheckoutRequest
import java.util.UUID

data class Customer(
    val name: String,
    val phone: String? = null,
    val email: String? = null,
) {
    init {
        require(name.isNotBlank()) { "customer name is required" }
        require(!phone.isNullOrBlank() || !email.isNullOrBlank()) {
            "customer phone and/or email is required"
        }
    }
}

class ThirdPartyCheckoutRequest private constructor(
    val productId: String,
    val amountMinor: Long,
    val currency: String,
    val customer: Customer,
    val providerId: UUID,
    val payerId: UUID,
    val description: String?,
) {
    fun toCreateCheckoutRequest(): CreateCheckoutRequest {
        val metadata = linkedMapOf<String, Any?>(
            "customerName" to customer.name,
        )
        customer.phone?.takeIf { it.isNotBlank() }?.let { metadata["customerPhone"] = it }
        customer.email?.takeIf { it.isNotBlank() }?.let { metadata["customerEmail"] = it }

        return CreateCheckoutRequest(
            productId = productId,
            amountMinor = amountMinor,
            currency = currency,
            providerId = providerId,
            payerId = payerId,
            description = description,
            metadata = metadata,
        )
    }

    class Builder {
        private var productId: String? = null
        private var amountMinor: Long? = null
        private var currency: String? = null
        private var customer: Customer? = null
        private var providerId: UUID? = null
        private var payerId: UUID? = null
        private var description: String? = null

        fun productId(value: String) = apply { productId = value }

        fun amountMinor(value: Long) = apply { amountMinor = value }

        fun currency(value: String) = apply { currency = value }

        fun customer(value: Customer) = apply { customer = value }

        fun providerId(value: UUID) = apply { providerId = value }

        fun payerId(value: UUID) = apply { payerId = value }

        fun description(value: String?) = apply { description = value }

        fun build(): ThirdPartyCheckoutRequest {
            val resolvedProductId = productId?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("productId is required")
            val resolvedAmount = amountMinor
                ?: throw IllegalArgumentException("amountMinor is required")
            val resolvedCurrency = currency?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("currency is required")
            val resolvedCustomer = customer
                ?: throw IllegalArgumentException("customer is required")
            val resolvedProviderId = providerId
                ?: throw IllegalArgumentException("providerId is required")
            val resolvedPayerId = payerId
                ?: throw IllegalArgumentException("payerId is required")

            return ThirdPartyCheckoutRequest(
                productId = resolvedProductId,
                amountMinor = resolvedAmount,
                currency = resolvedCurrency,
                customer = resolvedCustomer,
                providerId = resolvedProviderId,
                payerId = resolvedPayerId,
                description = description,
            )
        }
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
