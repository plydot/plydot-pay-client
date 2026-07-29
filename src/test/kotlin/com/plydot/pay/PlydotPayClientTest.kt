package com.plydot.pay

import com.plydot.pay.exception.PlydotPayException
import com.plydot.pay.model.CheckoutStatus
import com.plydot.pay.model.PaymentStatus
import com.plydot.pay.model.PayCheckoutRequest
import com.plydot.pay.thirdparty.Customer
import com.plydot.pay.thirdparty.ThirdPartyCheckoutRequest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class PlydotPayClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: PlydotPayClient

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        client =
            PlydotPayClient.builder()
                .apiKey("pk_test_demo")
                .baseUrl(server.url("/").toString().removeSuffix("/"))
                .build()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `create pay and get payment happy path`() {
        val checkoutId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val paymentId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val merchantId = UUID.fromString("33333333-3333-3333-3333-333333333333")

        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": "$checkoutId",
                      "merchantId": "$merchantId",
                      "productId": "acme-sku",
                      "catalogProductId": null,
                      "packageId": null,
                      "creditTierId": null,
                      "creditsToGrant": null,
                      "ugxPerCredit": null,
                      "accountRef": "acme-store",
                      "amountMinor": 50000,
                      "currency": "UGX",
                      "description": "Gold plan",
                      "metadata": {
                        "customerName": "Jane Okello",
                        "customerPhone": "256700000099"
                      },
                      "status": "PENDING",
                      "expiresAt": "2026-07-21T10:00:00Z",
                      "createdAt": "2026-07-21T09:00:00Z"
                    }
                    """.trimIndent(),
                ),
        )

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "checkoutId": "$checkoutId",
                      "payment": {
                        "id": "$paymentId",
                        "checkoutId": "$checkoutId",
                        "merchantId": "$merchantId",
                        "amountMinor": 50000,
                        "currency": "UGX",
                        "status": "PENDING",
                        "providerReference": "yo-ref-1",
                        "refundedAmountMinor": 0,
                        "instructions": {"type": "YOPAYMENTS"},
                        "createdAt": "2026-07-21T09:00:05Z"
                      }
                    }
                    """.trimIndent(),
                ),
        )

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": "$paymentId",
                      "checkoutId": "$checkoutId",
                      "merchantId": "$merchantId",
                      "amountMinor": 50000,
                      "currency": "UGX",
                      "status": "SUCCEEDED",
                      "providerReference": "yo-ref-1",
                      "refundedAmountMinor": 0,
                      "instructions": null,
                      "createdAt": "2026-07-21T09:00:05Z"
                    }
                    """.trimIndent(),
                ),
        )

        val checkout =
            client.createThirdPartyCheckout(
                ThirdPartyCheckoutRequest.builder()
                    .productId("acme-sku")
                    .amountMinor(50_000)
                    .currency("UGX")
                    .providerId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                    .payerId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
                    .customer(Customer("Jane Okello", phone = "256700000099"))
                    .description("Gold plan")
                    .build(),
                idempotencyKey = "idem-1",
            )

        assertEquals(checkoutId, checkout.id)
        assertEquals(CheckoutStatus.PENDING, checkout.status)

        val pay =
            client.payCheckout(
                checkoutId,
                PayCheckoutRequest(payerRef = "256700000099"),
            )
        assertEquals(PaymentStatus.PENDING, pay.payment.status)

        val payment = client.getPayment(paymentId)
        assertEquals(PaymentStatus.SUCCEEDED, payment.status)

        val createRequest = server.takeRequest()
        assertEquals("POST", createRequest.method)
        assertEquals("idem-1", createRequest.getHeader("Idempotency-Key"))
        assertTrue(createRequest.body.readUtf8().contains("\"customerName\":\"Jane Okello\""))
    }

    @Test
    fun `maps API error to PlydotPayException`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "code": "CHECKOUT_EXPIRED",
                      "message": "Checkout has expired",
                      "timestamp": "2026-07-21T09:00:00Z"
                    }
                    """.trimIndent(),
                ),
        )

        val ex =
            assertThrows(PlydotPayException::class.java) {
                client.getCheckout(UUID.randomUUID())
            }

        assertEquals("CHECKOUT_EXPIRED", ex.code)
        assertEquals(409, ex.httpStatus)
        assertTrue(ex.isCheckoutExpired())
    }
}
