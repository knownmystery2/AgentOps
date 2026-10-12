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
@Config(sdk = [36])
class BusinessOperationsGeminiRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var secretManager: SecretManager
    private lateinit var repository: BusinessOperationsGeminiRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getInstance(context)
        secretManager = SecretManager(database.settingsDao())
        repository = BusinessOperationsGeminiRepository(
            db = database,
            secretManager = secretManager,
            context = context
        )
    }

    @Test
    fun testRepositoryInitializationAndConfiguration() {
        assertNotNull(repository)
        // With default unconfigured test environment, repository handles fallback gracefully
        val isConfigured = repository.isGeminiConfigured()
        // Should resolve cleanly without crashing
        val apiKey = repository.resolveGeminiApiKey()
        assertNotNull(apiKey)
    }

    @Test
    fun testProcessBusinessPromptPodWorkflow() = runBlocking {
        val prompt = "Create a heavyweight vintage washed hoodie line on Printify for sustainable apparel"
        val contextData = BusinessContext(
            storeTheme = "Vintage Eco Apparel",
            primaryShopDomain = "kriticallimitz.printify.me",
            targetNiche = "Sustainable Streetwear"
        )

        val result = repository.processBusinessPrompt(prompt, contextData)
        assertTrue("Analysis result should be successful", result.isSuccess)

        val analysis = result.getOrNull()
        assertNotNull(analysis)
        assertEquals("Sustainable Streetwear", analysis?.nicheFocus)
        assertFalse("Task suggestions should not be empty", analysis?.taskSuggestions.isNullOrEmpty())

        val tasks = analysis?.taskSuggestions.orEmpty()
        assertTrue("Should contain a POD mockup task", tasks.any { it.category == "POD_MOCKUP" })
        assertTrue("Should contain a compliance task", tasks.any { it.category == "COMPLIANCE" })

        val podTask = tasks.first { it.category == "POD_MOCKUP" }
        assertEquals("4", podTask.assignedAgentId)
        assertEquals("Creative Design Agent", podTask.assignedAgentRole)
        assertTrue("Margin should be 40% or higher", podTask.estimatedMarginPercent >= 40)
        assertTrue("Should mandate zero upfront cost", podTask.zeroCostCompliance)
    }

    @Test
    fun testProcessBusinessPromptLedgerAndFinance() = runBlocking {
        val prompt = "Audit double-entry ledger, reconcile Stripe fees, and calculate taxes"
        val result = repository.processBusinessPrompt(prompt)
        assertTrue(result.isSuccess)

        val analysis = result.getOrNull()
        assertNotNull(analysis)
        val financeTasks = analysis?.taskSuggestions?.filter { it.category == "FINANCE" }.orEmpty()
        assertTrue("Should produce finance tasks", financeTasks.isNotEmpty())
        assertEquals("9", financeTasks.first().assignedAgentId)
        assertEquals("Finance Agent", financeTasks.first().assignedAgentRole)
    }

    @Test
    fun testEnqueueSuggestionAsActiveTask() = runBlocking {
        val suggestion = BusinessTaskSuggestion(
            title = "Launch Cyberpunk Mug POD Campaign",
            category = "POD_MOCKUP",
            assignedAgentId = "4",
            assignedAgentRole = "Creative Design Agent",
            priority = "HIGH",
            detailedInstruction = "Deploy 11oz Ceramic Mug to Printify shop kriticallimitz.printify.me",
            estimatedMarginPercent = 55,
            suggestedAction = "DEPLOY_POD"
        )

        val enqueuedTask = repository.enqueueSuggestionAsActiveTask(suggestion)
        assertNotNull(enqueuedTask)
        assertEquals(suggestion.id, enqueuedTask.id)
        assertEquals("PENDING", enqueuedTask.status)

        // Verify persisted in Room Database
        val allTasks = database.taskDao().getAllTasks().first()
        assertTrue("Task should be present in database", allTasks.any { it.id == suggestion.id })
    }

    @Test
    fun testEnqueueSuggestionForHumanApproval() = runBlocking {
        val suggestion = BusinessTaskSuggestion(
            title = "USPTO Trademark Clearance for 'RetroLimit'",
            category = "COMPLIANCE",
            assignedAgentId = "10",
            assignedAgentRole = "Legal Compliance Agent",
            priority = "HIGH",
            detailedInstruction = "Verify trademark clearance in apparel class 025",
            suggestedAction = "AUDIT_TRADEMARK"
        )

        val decision = repository.enqueueSuggestionForHumanApproval(suggestion)
        assertNotNull(decision)
        assertTrue("Decision ID should start with DEC-", decision.id.startsWith("DEC-"))
        assertEquals("PENDING", decision.status)

        // Verify persisted in Room Database
        val allDecisions = database.decisionDao().getAllDecisions().first()
        assertTrue("Decision should be present in database", allDecisions.any { it.id == decision.id })
    }

    @Test
    fun testExecuteSuggestedBusinessAction() = runBlocking {
        val deployPodSuggestion = BusinessTaskSuggestion(
            title = "Deploy Vintage Tote Bag",
            category = "PRODUCT_DESIGN",
            assignedAgentId = "4",
            assignedAgentRole = "Creative Design Agent",
            priority = "HIGH",
            detailedInstruction = "Create 300 DPI Canvas Tote Bag blueprint",
            suggestedAction = "DEPLOY_POD",
            targetResource = "kriticallimitz.printify.me"
        )

        val actionResult = repository.executeSuggestedBusinessAction(deployPodSuggestion)
        assertTrue("Action execution should succeed", actionResult.isSuccess)
        val message = actionResult.getOrNull()
        assertNotNull(message)
        assertTrue("Message should confirm POD routing", message?.contains("POD blueprint") == true)
    }

    @Test
    fun testSuggestTasksForNiche() = runBlocking {
        val result = repository.suggestTasksForNiche("Minimalist Ceramic Homeware", "Printify")
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull().orEmpty()
        assertTrue("Suggestions for niche should be non-empty", suggestions.isNotEmpty())
    }

    @Test
    fun testProductLaunchStrategyAndAuditOperations() = runBlocking {
        val launchResult = repository.generateProductLaunchStrategy(
            productTitle = "Oversized Minimalist Acid Wash Tee",
            targetNiche = "Vintage Creator Apparel",
            priceCents = 3800L
        )
        assertTrue(launchResult.isSuccess)
        assertNotNull(launchResult.getOrNull())

        val auditResult = repository.auditBusinessOperations("Printify dropshipping with Stripe payments")
        assertTrue(auditResult.isSuccess)
        assertNotNull(auditResult.getOrNull())
    }
}
