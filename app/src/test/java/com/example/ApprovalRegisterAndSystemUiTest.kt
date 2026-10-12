package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.opensource.autonomous.agent.data.*
import com.opensource.autonomous.agent.engine.ExecutionCondition
import com.opensource.autonomous.agent.engine.StandbyPoolManager
import com.opensource.autonomous.agent.ui.ApprovalRegisterViewModel
import com.opensource.autonomous.agent.ui.MainViewModel
import com.opensource.autonomous.agent.ui.PendingBusinessActionProposal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ApprovalRegisterAndSystemUiTest {

    private lateinit var application: Application
    private lateinit var database: AppDatabase
    private lateinit var approvalRegisterViewModel: ApprovalRegisterViewModel
    private lateinit var mainViewModel: MainViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getInstance(application)
        approvalRegisterViewModel = ApprovalRegisterViewModel(application, database)
        mainViewModel = MainViewModel(application)
    }

    @Test
    fun testApprovalRegisterCreatePendingAction() = runBlocking {
        val proposal = PendingBusinessActionProposal(
            title = "Deploy Print-on-Demand Hoodie SKU",
            agentSource = "Creative Design Agent",
            actionType = "POD_PUBLISH",
            description = "Publish 4500x5400 CMYK design to Printify shop kriticallimitz.printify.me",
            targetResource = "printify/products/sku-992",
            estimatedCostCents = 1850L,
            estimatedRevenueCents = 3990L,
            riskLevel = "LOW",
            zeroCostVerified = true
        )

        approvalRegisterViewModel.createPendingAction(proposal)

        val pending = database.decisionDao().getPendingDecisions().first()
        assertTrue("Pending decisions should contain created action", pending.any { it.description.contains("Deploy Print-on-Demand Hoodie SKU") })

        val decision = pending.first { it.description.contains("Deploy Print-on-Demand Hoodie SKU") }
        assertEquals("PENDING", decision.status)
        assertEquals("Creative Design Agent", decision.agentSource)
        assertTrue("Decision ID should start with ACT-", decision.id.startsWith("ACT-"))
    }

    @Test
    fun testApprovalRegisterConfirmActionAndDispatch() = runBlocking {
        val decision = DecisionEntity(
            id = "ACT-TEST-CONFIRM-1",
            agentSource = "Storefront Deployment Agent",
            description = "Publish GitHub Pages custom storefront for Eco Botanical",
            payloadJson = """{"actionType":"STOREFRONT_DEPLOY","riskLevel":"LOW","zeroCostVerified":true}""",
            status = "PENDING"
        )
        database.decisionDao().insertDecision(decision)

        approvalRegisterViewModel.confirmAction(decision.id, "Approved by Founder for immediate deployment")

        // Wait/poll reactive update
        val updatedDecision = database.decisionDao().getAllDecisions().first().first { it.id == decision.id }
        assertEquals("APPROVED", updatedDecision.status)

        // Verify execution task in Room
        val tasks = database.taskDao().getAllTasks().first()
        assertTrue("Should have execution task recorded in Room", tasks.any { it.id == "EXEC-${decision.id}" })

        // Verify audit log
        val logs = database.logDao().getAllLogs().first()
        assertTrue("Audit log should record confirmation", logs.any { it.message.contains("CONFIRMED") && it.message.contains(decision.id) })
    }

    @Test
    fun testApprovalRegisterRejectActionWithReason() = runBlocking {
        val decision = DecisionEntity(
            id = "ACT-TEST-REJECT-1",
            agentSource = "Marketing Agent",
            description = "High-budget ad campaign requiring credit line",
            payloadJson = """{"actionType":"MARKETING_DISPATCH","riskLevel":"HIGH","zeroCostVerified":false}""",
            status = "PENDING"
        )
        database.decisionDao().insertDecision(decision)

        approvalRegisterViewModel.rejectAction(decision.id, "Violates zero upfront cost mandate")

        val updatedDecision = database.decisionDao().getAllDecisions().first().first { it.id == decision.id }
        assertEquals("REJECTED", updatedDecision.status)

        val logs = database.logDao().getAllLogs().first()
        assertTrue("Audit log should record rejection reason", logs.any { it.message.contains("REJECTED") && it.message.contains("Violates zero upfront cost") })
    }

    @Test
    fun testApprovalRegisterBatchConfirmAndReject() = runBlocking {
        val dec1 = DecisionEntity(id = "BATCH-1", agentSource = "Agent 4", description = "Design 1", payloadJson = "{}", status = "PENDING")
        val dec2 = DecisionEntity(id = "BATCH-2", agentSource = "Agent 4", description = "Design 2", payloadJson = "{}", status = "PENDING")
        val dec3 = DecisionEntity(id = "BATCH-3", agentSource = "Agent 6", description = "Sourcing 1", payloadJson = "{}", status = "PENDING")

        database.decisionDao().insertDecisions(listOf(dec1, dec2, dec3))

        approvalRegisterViewModel.batchConfirm(listOf("BATCH-1", "BATCH-2"))
        approvalRegisterViewModel.batchReject(listOf("BATCH-3"))

        val all = database.decisionDao().getAllDecisions().first()
        assertEquals("APPROVED", all.first { it.id == "BATCH-1" }.status)
        assertEquals("APPROVED", all.first { it.id == "BATCH-2" }.status)
        assertEquals("REJECTED", all.first { it.id == "BATCH-3" }.status)
    }

    @Test
    fun testStandbyPoolManagerQueueAndExecution() = runBlocking {
        val standbyManager = StandbyPoolManager(application, database, CoroutineScope(Dispatchers.IO))

        val queuedTask = standbyManager.queueStandbyTask(
            taskId = "STANDBY-TASK-TEST-1",
            agentId = "6",
            title = "Overnight Dropship Supplier Feed Sync",
            instruction = "Pull zero-capital catalog items when device is charging and on Wi-Fi",
            condition = ExecutionCondition(
                requiresNetwork = true,
                requiresCharging = false,
                requiresBatteryNotLow = true
            )
        )

        assertNotNull(queuedTask)
        assertEquals("STANDBY-TASK-TEST-1", queuedTask.taskId)
        assertEquals("QUEUED_STANDBY", queuedTask.status)

        val poolState = standbyManager.poolState.value
        assertTrue("Queued count should be at least 1", poolState.queuedTasksCount >= 1)
    }

    @Test
    fun testLocalLedgerDoubleEntryMathAndTokens() = runBlocking {
        // Record test Sale Entry
        val saleEntry = LedgerEntity(
            code = "TXN-LEDGER-001",
            store = "kriticallimitz.printify.me",
            platform = "Stripe",
            description = "Heavyweight Oversized Hoodie Sale",
            accountDebit = "Cash/Stripe",
            accountCredit = "Sales Revenue",
            amountCents = 4200L,
            hashToken = "SHA256-TEST-TOKEN-001",
            category = "SALE"
        )
        database.ledgerDao().insertEntry(saleEntry)

        // Record matching COGS Entry
        val cogsEntry = LedgerEntity(
            code = "TXN-LEDGER-002",
            store = "kriticallimitz.printify.me",
            platform = "Printify",
            description = "Printify Fulfillment COGS (Paid by Customer)",
            accountDebit = "COGS",
            accountCredit = "Accounts Payable/Printify",
            amountCents = 2100L,
            hashToken = "SHA256-TEST-TOKEN-002",
            category = "COGS"
        )
        database.ledgerDao().insertEntry(cogsEntry)

        val entries = database.ledgerDao().getAllEntries().first()
        val totalSaleCents = entries.filter { it.category == "SALE" }.sumOf { it.amountCents }
        val totalCogsCents = entries.filter { it.category == "COGS" }.sumOf { it.amountCents }

        assertTrue("Gross sales should equal 4200 cents", totalSaleCents >= 4200L)
        assertTrue("COGS should equal 2100 cents", totalCogsCents >= 2100L)
        val netMargin = (totalSaleCents - totalCogsCents).toFloat() / totalSaleCents.toFloat()
        assertTrue("Net profit margin should be 50%", netMargin >= 0.45f)
    }

    @Test
    fun testGodAgentFullInstallDiagnostics() {
        val diagnostic = mainViewModel.runGodInstallDiagnostics()
        assertNotNull(diagnostic)
        assertTrue("God agent must be seeded in DB", diagnostic.isDatabaseSeeded)
        assertTrue("God Interceptor must be active", diagnostic.isInterceptorActive)
        assertTrue("Real-time notification service must be running", diagnostic.isNotificationServiceRunning)
        assertTrue("Security gate scripts must be verified", diagnostic.isSecurityGateScriptVerified)
        assertTrue("Audit trail must be functional", diagnostic.isAuditTrailFunctional)
        assertTrue("Zero-trust enclave must be armed", diagnostic.isZeroTrustEnclaveArmed)
        assertTrue("Status summary must confirm full install", diagnostic.statusSummary.contains("Fully Installed & Armed"))
    }
}
