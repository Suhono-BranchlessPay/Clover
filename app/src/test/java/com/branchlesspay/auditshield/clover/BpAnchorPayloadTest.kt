package com.branchlesspay.auditshield.clover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionFormatterTest {
    @Test
    fun statusLabels() {
        assertEquals("Pending", TransactionFormatter.statusLabel(QueueStatus.PENDING))
        assertEquals("Anchored", TransactionFormatter.statusLabel(QueueStatus.ANCHORED))
        assertEquals("Failed", TransactionFormatter.statusLabel(QueueStatus.FAILED))
    }

    @Test
    fun formatAmount_fromPayloadJson() {
        val json = """{"amount":15.0,"currency":"USD"}"""
        assertEquals("$15.00", TransactionFormatter.formatAmount(json))
    }

    @Test
    fun buildVerifyUrl() {
        val url = TransactionFormatter.buildVerifyUrl("abc-123")
        assertEquals("https://branchlesspay.com/verify/abc-123", url)
    }

    @Test
    fun formatMoney_usd() {
        assertEquals("$10.50", TransactionFormatter.formatMoney(10.5, "USD"))
    }
}

class CloverPaymentParserTest {
    @Test
    fun parseExtras_cloverCents() {
        val event = CloverPaymentParser.parseExtras(
            mapOf(
                "paymentId" to "PAY-1001",
                "amount" to "1500",
                "currency" to "USD",
                "tenderType" to "CREDIT",
            ),
        )
        assertEquals("PAY-1001", event!!.transactionId)
        assertEquals(1500L, event.amountCents)
        assertEquals("CREDIT", event.paymentMethod)
    }

    @Test
    fun parseExtras_dollarString() {
        val event = CloverPaymentParser.parseExtras(
            mapOf(
                "id" to "PAY-2",
                "amount" to "25.50",
                "currency" to "USD",
            ),
        )
        assertEquals(2550L, event!!.amountCents)
    }

    @Test
    fun parseExtras_missingAmount_returnsNull() {
        assertEquals(null, CloverPaymentParser.parseExtras(mapOf("paymentId" to "PAY-1")))
    }
}

class AnchorProcessorTest {
    private val event = PaymentEvent(
        transactionId = "PAY-UNIT-1",
        amountCents = 1500,
        currency = "USD",
        paymentMethod = "card",
    )

    @Test
    fun processPayment_onlineSuccess_recordsHistory() {
        val queue = InMemoryAnchorQueue()
        val processor = AnchorProcessor(
            queue = queue,
            apiClientFactory = {
                object : BpApiClient("test-key") {
                    override fun postAnchor(payload: Map<String, Any>): BpAnchorResult =
                        BpAnchorResult(
                            ok = true,
                            httpStatus = 202,
                            anchorId = "anchor-1",
                            verifyUrl = "https://branchlesspay.com/verify/anchor-1",
                            status = "queued",
                            contentHash = "hash",
                            error = null,
                            rawBody = "{}",
                        )
                }
            },
            isOnline = { true },
        )
        val result = processor.processPayment(event, "Flex", "SN1")
        assertTrue(result.ok)
        assertEquals(0, queue.countPending())
        assertEquals(1, queue.countAnchored())
    }

    @Test
    fun processPayment_offlineQueues() {
        val queue = InMemoryAnchorQueue()
        val processor = AnchorProcessor(
            queue = queue,
            apiClientFactory = { BpApiClient("test-key") },
            isOnline = { false },
        )
        val result = processor.processPayment(event, "Flex", "SN1")
        assertTrue(result.queuedOffline)
        assertEquals(1, queue.countPending())
    }

    @Test
    fun flushQueue_anchorsPendingItems() {
        val queue = InMemoryAnchorQueue()
        val payload = BpAnchorPayload.buildPayment(event, "Flex", "SN1")
        queue.enqueue(event.transactionId, "clover_payment", payload)
        val mockProcessor = AnchorProcessor(
            queue = queue,
            apiClientFactory = {
                object : BpApiClient("test-key") {
                    override fun postAnchor(payload: Map<String, Any>): BpAnchorResult =
                        BpAnchorResult(
                            ok = true,
                            httpStatus = 202,
                            anchorId = "a2",
                            verifyUrl = "https://branchlesspay.com/verify/a2",
                            status = "queued",
                            contentHash = "h",
                            error = null,
                            rawBody = "{}",
                        )
                }
            },
            isOnline = { true },
        )
        val flush = mockProcessor.flushQueue()
        assertEquals(1, flush.anchored)
        assertEquals(1, queue.countAnchored())
    }

    @Test
    fun flushQueue_maxRetryMarksFailed() {
        val queue = InMemoryAnchorQueue()
        val payload = BpAnchorPayload.buildPayment(event, "Flex", "SN1")
        val id = queue.enqueue(event.transactionId, "clover_payment", payload)
        repeat(AnchorProcessor.MAX_RETRIES - 1) {
            queue.incrementRetry(id, "fail")
        }
        val processor = AnchorProcessor(
            queue = queue,
            apiClientFactory = {
                object : BpApiClient("test-key") {
                    override fun postAnchor(payload: Map<String, Any>): BpAnchorResult =
                        BpAnchorResult(false, 500, null, null, null, null, "server error", "{}")
                }
            },
            isOnline = { true },
        )
        processor.flushQueue()
        assertEquals(1, queue.recent(5).count { it.status == QueueStatus.FAILED })
    }
}

class InMemoryQueueTest {
    @Test
    fun enqueueAndMarkAnchored() {
        val queue = InMemoryAnchorQueue()
        val id = queue.enqueue("PAY-1", "clover_payment", mapOf("reference_id" to "PAY-1"))
        assertEquals(1, queue.countPending())
        queue.markAnchored(id, "anchor-x", "https://branchlesspay.com/verify/anchor-x")
        assertEquals(0, queue.countPending())
        assertEquals(1, queue.countAnchored())
    }

    @Test
    fun recordAnchored_insertsHistoryRow() {
        val queue = InMemoryAnchorQueue()
        queue.recordAnchored(
            referenceId = "PAY-2",
            eventType = "clover_payment",
            payload = mapOf("reference_id" to "PAY-2", "amount" to 15.0, "currency" to "USD"),
            anchorId = "id-2",
            verifyUrl = "https://branchlesspay.com/verify/id-2",
        )
        assertEquals(1, queue.countAnchored())
        assertEquals("PAY-2", queue.getById(1)?.referenceId)
    }
}

class PaymentEventTest {
    @Test
    fun simulated_hasPayPrefix() {
        val event = PaymentEvent.simulated()
        assertTrue(event.transactionId.startsWith("PAY-"))
        assertEquals("USD", event.currency)
        assertEquals(1500L, event.amountCents)
    }
}

class BpAnchorPayloadTest {
    @Test
    fun buildTestTransaction_hasCloverFields() {
        val payload = BpAnchorPayload.buildTestTransaction("Clover Flex", "SN12345")
        assertEquals("clover_transaction", payload["event_type"])
        assertEquals("USD", payload["currency"])
        assertEquals(15.0, payload["amount"])
        @Suppress("UNCHECKED_CAST")
        val metadata = payload["metadata"] as Map<String, Any>
        assertEquals("clover", metadata["erp"])
        assertEquals("Clover Flex", metadata["device_model"])
        assertEquals("SN12345", metadata["device_sn"])
        assertTrue((payload["reference_id"] as String).startsWith("TEST-"))
    }

    @Test
    fun buildPayment_hasCloverPaymentEventType() {
        val event = PaymentEvent.simulated(amountCents = 2550, currency = "USD")
        val payload = BpAnchorPayload.buildPayment(event, "Flex", "SN999")
        assertEquals("clover_payment", payload["event_type"])
        assertEquals(event.transactionId, payload["reference_id"])
        assertEquals(25.5, payload["amount"])
        @Suppress("UNCHECKED_CAST")
        val metadata = payload["metadata"] as Map<String, Any>
        assertEquals("card", metadata["payment_method"])
        assertEquals(2550L, metadata["amount_cents"])
        assertEquals("Flex", metadata["device_model"])
    }
}

class BpApiClientTest {
    @Test
    fun legacyContentHash_isDeterministic() {
        val payload = linkedMapOf<String, Any>(
            "event_type" to "clover_transaction",
            "reference_id" to "TEST-FIXED",
            "amount" to 15.0,
            "currency" to "USD",
            "timestamp" to "2026-06-14T00:00:00Z",
            "metadata" to mapOf("erp" to "clover"),
        )
        val hash1 = BpApiClient.legacyContentHash(payload)
        val hash2 = BpApiClient.legacyContentHash(payload)
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length)
    }

    @Test
    fun postAnchor_withoutLicenseKey_failsFast() {
        val result = BpApiClient(licenseKey = "").postAnchor(
            BpAnchorPayload.buildTestTransaction("Flex", "dev-sn"),
        )
        assertTrue(!result.ok)
        assertEquals("BP license key is not configured", result.error)
    }
}
