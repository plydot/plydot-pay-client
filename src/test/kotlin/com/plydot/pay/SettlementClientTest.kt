package com.plydot.pay

import com.plydot.pay.model.PayoutAccountType
import com.plydot.pay.model.SettlementPayoutStatus
import com.plydot.pay.model.SubmitPayoutRequest
import com.plydot.pay.settlement.SettlementWorkflow
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class SettlementClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: PlydotPayClient
    private val merchantId = UUID.fromString("33333333-3333-3333-3333-333333333333")
    private val payoutId = UUID.fromString("44444444-4444-4444-4444-444444444444")

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        client =
            PlydotPayClient.builder()
                .apiKey("pk_live_demo")
                .baseUrl(server.url("/").toString().removeSuffix("/"))
                .build()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `settlement balance and submit payout`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "availableMinor": 50000,
                      "reservedMinor": 0,
                      "currency": "UGX",
                      "unsettledPaymentCount": 2
                    }
                    """.trimIndent(),
                ),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(202)
                .setHeader("Content-Type", "application/json")
                .setBody(payoutJson(SettlementPayoutStatus.PENDING_VERIFICATION.name)),
        )

        val balance = client.getSettlementBalance()
        assertEquals(50_000, balance.availableMinor)
        assertEquals(2, balance.unsettledPaymentCount)

        val payout =
            client.submitPayoutRequest(
                SubmitPayoutRequest(amountMinor = 50_000),
                merchantAccessToken = "jwt-merchant-admin",
            )
        assertEquals(payoutId, payout.id)
        assertEquals(SettlementPayoutStatus.PENDING_VERIFICATION, payout.status)

        val balanceRequest = server.takeRequest()
        assertEquals("Bearer pk_live_demo", balanceRequest.getHeader("Authorization"))

        val submitRequest = server.takeRequest()
        assertEquals("POST", submitRequest.method)
        assertEquals("Bearer jwt-merchant-admin", submitRequest.getHeader("Authorization"))
        assertEquals("""{"amountMinor":50000}""", submitRequest.body.readUtf8())
    }

    @Test
    fun `settlement workflow polls until verified`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(payoutJson("VERIFYING")),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(payoutJson("VERIFIED")),
        )

        val workflow = SettlementWorkflow(client, "jwt-merchant-admin", merchantId)
        val payout = workflow.waitForPayout(payoutId, pollInterval = java.time.Duration.ofMillis(1))

        assertEquals(SettlementPayoutStatus.VERIFIED, payout.status)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `get payout account`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "merchantId": "$merchantId",
                      "accountType": "BANK",
                      "accountName": "Wekume Ltd",
                      "accountNumber": "1234567890",
                      "bankName": "Stanbic Bank Uganda",
                      "bankCode": "SBICUGKX",
                      "currency": "UGX",
                      "active": true,
                      "updatedAt": "2026-07-30T08:00:00Z"
                    }
                    """.trimIndent(),
                ),
        )

        val account = client.getPayoutAccount(merchantId)
        assertEquals(PayoutAccountType.BANK, account.accountType)
        assertEquals("Wekume Ltd", account.accountName)
    }

    private fun payoutJson(status: String): String =
        """
        {
          "id": "$payoutId",
          "merchantId": "$merchantId",
          "amountMinor": 50000,
          "currency": "UGX",
          "status": "$status",
          "requestedBy": "merchant.admin",
          "destinationSnapshot": {
            "accountType": "BANK",
            "accountName": "Wekume Ltd",
            "accountNumber": "1234567890",
            "currency": "UGX"
          },
          "platformNote": null,
          "failureReason": null,
          "verifiedAt": null,
          "completedAt": null,
          "createdAt": "2026-07-30T08:00:00Z",
          "updatedAt": "2026-07-30T08:00:00Z",
          "items": []
        }
        """.trimIndent()
}
