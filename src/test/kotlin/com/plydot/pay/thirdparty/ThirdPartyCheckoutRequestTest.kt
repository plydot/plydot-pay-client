package com.plydot.pay.thirdparty

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class ThirdPartyCheckoutRequestTest {
    private val providerId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val payerId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")

    @Test
    fun `builds checkout request with metadata`() {
        val request =
            ThirdPartyCheckoutRequest.builder()
                .productId("sku-1")
                .amountMinor(500)
                .currency("UGX")
                .providerId(providerId)
                .payerId(payerId)
                .customer(Customer("Jane Okello", phone = "256700000099"))
                .build()

        val checkout = request.toCreateCheckoutRequest()
        assertEquals("sku-1", checkout.productId)
        assertEquals(500, checkout.amountMinor)
        assertEquals("UGX", checkout.currency)
        assertEquals(providerId, checkout.providerId)
        assertEquals(payerId, checkout.payerId)
        assertEquals("Jane Okello", checkout.metadata?.get("customerName"))
        assertEquals("256700000099", checkout.metadata?.get("customerPhone"))
    }

    @Test
    fun `requires customer name before HTTP`() {
        assertThrows(IllegalArgumentException::class.java) {
            Customer(name = "  ")
        }
    }

    @Test
    fun `requires customer contact before HTTP`() {
        assertThrows(IllegalArgumentException::class.java) {
            Customer(name = "Jane Okello")
        }
    }

    @Test
    fun `requires product id`() {
        assertThrows(IllegalArgumentException::class.java) {
            ThirdPartyCheckoutRequest.builder()
                .amountMinor(500)
                .currency("UGX")
                .providerId(providerId)
                .payerId(payerId)
                .customer(Customer("Jane Okello", email = "jane@example.com"))
                .build()
        }
    }
}
