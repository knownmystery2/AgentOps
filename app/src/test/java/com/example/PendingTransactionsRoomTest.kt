package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.opensource.autonomous.agent.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PendingTransactionsRoomTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: AgentRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getInstance(context)
        repository = AgentRepository(database)
    }

    @Test
    fun testPendingTransactionDaoInsertAndQuery() = runBlocking {
        val tx = PendingTransactionEntity(
            id = "TEST-TX-001",
            transactionCode = "TX-UNIT-101",
            title = "Test Wholesale Apparel Restock",
            description = "Fulfillment batch for testing Room DB DAO",
            storeOrEntity = "Print Defy Studio",
            platform = "Printify POD Gateway",
            vendorOrPayee = "SwiftPOD Testing",
            accountDebit = "5010 Cost of Goods Sold",
            accountCredit = "1010 Operating Cash Clearing",
            amountCents = 12500L,
            category = "COGS",
            riskScore = 15,
            riskReason = "Unit test passed.",
            status = "PENDING"
        )

        database.pendingTransactionDao().insertTransaction(tx)

        val pending = database.pendingTransactionDao().getPendingTransactions().first()
        assertTrue("Pending transaction should exist in database", pending.any { it.id == "TEST-TX-001" })

        val retrieved = database.pendingTransactionDao().getTransactionById("TEST-TX-001")
        assertNotNull(retrieved)
        assertEquals("TX-UNIT-101", retrieved?.transactionCode)
        assertEquals(12500L, retrieved?.amountCents)
        assertEquals("PENDING", retrieved?.status)
    }

    @Test
    fun testApproveTransactionUpdatesStatusAndPostsToLedger() = runBlocking {
        val tx = PendingTransactionEntity(
            id = "TEST-TX-APPROVE",
            transactionCode = "TX-APPR-TEST",
            title = "Hoodie Fulfillment Batch",
            description = "Batch fulfillment needing approval",
            storeOrEntity = "Print Defy Studio",
            platform = "Printify Gateway",
            vendorOrPayee = "District Photo Inc",
            accountDebit = "5010 Cost of Goods Sold",
            accountCredit = "1010 Operating Cash Clearing",
            amountCents = 4850L,
            category = "COGS",
            riskScore = 10,
            status = "PENDING"
        )

        database.pendingTransactionDao().insertTransaction(tx)

        val result = repository.approvePendingTransaction("TEST-TX-APPROVE")
        assertTrue("Approval should succeed", result.isSuccess)

        val updated = database.pendingTransactionDao().getTransactionById("TEST-TX-APPROVE")
        assertNotNull(updated)
        assertEquals("APPROVED", updated?.status)
        assertNotNull(updated?.reviewedAt)

        // Verify that a corresponding entry was posted into the double-entry Ledger
        val ledgerEntries = database.ledgerDao().getAllEntries().first()
        assertTrue(
            "Double-entry ledger should record the approved transaction",
            ledgerEntries.any { it.code == "TX-APPR-TEST" && it.amountCents == 4850L }
        )
    }

    @Test
    fun testRejectTransactionUpdatesStatusAndRecordsAuditLog() = runBlocking {
        val tx = PendingTransactionEntity(
            id = "TEST-TX-REJECT",
            transactionCode = "TX-REJ-TEST",
            title = "Unbudgeted Equipment Expense",
            description = "Spurious charge requiring denial",
            storeOrEntity = "Print Defy Studio",
            platform = "Hardware Direct",
            vendorOrPayee = "Unknown Supplier",
            accountDebit = "6010 Miscellaneous Expense",
            accountCredit = "1010 Operating Cash Clearing",
            amountCents = 8900L,
            category = "VENDOR_EXPENSE",
            riskScore = 85,
            status = "PENDING"
        )

        database.pendingTransactionDao().insertTransaction(tx)

        val result = repository.rejectPendingTransaction("TEST-TX-REJECT", "Exceeds discretionary threshold")
        assertTrue("Rejection should succeed", result.isSuccess)

        val updated = database.pendingTransactionDao().getTransactionById("TEST-TX-REJECT")
        assertNotNull(updated)
        assertEquals("REJECTED", updated?.status)
        assertNotNull(updated?.reviewedAt)

        // Verify audit log has the rejection reason
        val logs = database.logDao().getAllLogs().first()
        assertTrue(
            "Audit log should record the transaction rejection reason",
            logs.any { it.message.contains("REJECTED") && it.message.contains("TX-REJ-TEST") }
        )
    }

    @Test
    fun testBatchApproveAndBatchReject() = runBlocking {
        val tx1 = PendingTransactionEntity(
            id = "BATCH-TX-1",
            transactionCode = "TX-B1",
            title = "Batch 1",
            description = "Description 1",
            amountCents = 1500L,
            status = "PENDING"
        )
        val tx2 = PendingTransactionEntity(
            id = "BATCH-TX-2",
            transactionCode = "TX-B2",
            title = "Batch 2",
            description = "Description 2",
            amountCents = 2500L,
            status = "PENDING"
        )
        val tx3 = PendingTransactionEntity(
            id = "BATCH-TX-3",
            transactionCode = "TX-B3",
            title = "Batch 3",
            description = "Description 3",
            amountCents = 3500L,
            status = "PENDING"
        )

        database.pendingTransactionDao().insertTransactions(listOf(tx1, tx2, tx3))

        repository.batchApproveTransactions(listOf("BATCH-TX-1", "BATCH-TX-2"))
        repository.batchRejectTransactions(listOf("BATCH-TX-3"), "Rejected in batch")

        val r1 = database.pendingTransactionDao().getTransactionById("BATCH-TX-1")
        val r2 = database.pendingTransactionDao().getTransactionById("BATCH-TX-2")
        val r3 = database.pendingTransactionDao().getTransactionById("BATCH-TX-3")

        assertEquals("APPROVED", r1?.status)
        assertEquals("APPROVED", r2?.status)
        assertEquals("REJECTED", r3?.status)
    }
}
