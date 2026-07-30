package com.plydot.pay.settlement

import com.plydot.pay.PlydotPayClient
import com.plydot.pay.model.SettlementBalanceResponse
import com.plydot.pay.model.SettlementPayoutResponse
import com.plydot.pay.model.SettlementPayoutStatus
import com.plydot.pay.model.SubmitPayoutRequest
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeoutException

/**
 * Merchant settlement payout flow: balance → submit → poll until verified.
 *
 * Requires a **merchant admin JWT** ([merchantAccessToken]) for submit; reads use the API key
 * configured on [client].
 */
class SettlementWorkflow(
    private val client: PlydotPayClient,
    private val merchantAccessToken: String,
    private val merchantId: UUID,
) {
    fun getBalance(): SettlementBalanceResponse = client.getSettlementBalance()

    fun getPayoutAccount() = client.getPayoutAccount(merchantId)

    fun submitPayout(request: SubmitPayoutRequest = SubmitPayoutRequest()): SettlementPayoutResponse =
        client.submitPayoutRequest(request, merchantAccessToken)

    fun submitFullBalance(): SettlementPayoutResponse =
        submitPayout(SubmitPayoutRequest())

    fun getPayout(payoutId: UUID): SettlementPayoutResponse =
        client.getPayoutRequest(payoutId)

    fun listPayouts(
        status: SettlementPayoutStatus? = null,
        page: Int = 0,
        size: Int = 50,
    ): List<SettlementPayoutResponse> =
        client.listPayoutRequests(status = status, page = page, size = size)

    /**
     * Poll until the payout reaches one of [targetStatuses] or [timeout] elapses.
     * Default targets: [SettlementPayoutStatus.VERIFIED] or [SettlementPayoutStatus.VERIFICATION_FAILED].
     */
    @JvmOverloads
    fun waitForPayout(
        payoutId: UUID,
        timeout: Duration = Duration.ofMinutes(5),
        pollInterval: Duration = Duration.ofSeconds(3),
        targetStatuses: Set<SettlementPayoutStatus> = TERMINAL_VERIFY_STATUSES,
    ): SettlementPayoutResponse =
        client.waitForPayoutRequest(
            payoutId = payoutId,
            timeout = timeout,
            pollInterval = pollInterval,
            targetStatuses = targetStatuses,
        )

    companion object {
        val TERMINAL_VERIFY_STATUSES: Set<SettlementPayoutStatus> =
            setOf(
                SettlementPayoutStatus.VERIFIED,
                SettlementPayoutStatus.VERIFICATION_FAILED,
            )
    }
}
