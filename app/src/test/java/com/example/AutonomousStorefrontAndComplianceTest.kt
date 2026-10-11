package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.opensource.autonomous.agent.data.*
import com.opensource.autonomous.agent.engine.MultiAgentOrchestrator
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
class AutonomousStorefrontAndComplianceTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: AgentRepository
    private lateinit var orchestrator: MultiAgentOrchestrator

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getInstance(context)
        repository = AgentRepository(database)
        orchestrator = MultiAgentOrchestrator(database, repository)
    }

    @Test
    fun testAppLabel() {
        val appName = context.getString(R.string.app_name)
        assertEquals("AgentOps", appName)
    }

    @Test
    fun testZeroCostStorefrontCreationPipeline() = runBlocking {
        val result = orchestrator.createZeroCostStorefront(
            companyName = "Vanguard Organic Botanics",
            businessNiche = "Vintage Flora Apparel & Decor"
        )

        assertTrue("Storefront creation should succeed", result.isSuccess)
        val store = result.getOrNull()
        assertNotNull(store)
        assertEquals("Vanguard Organic Botanics", store?.companyName)
        assertEquals(0L, store?.monthlyCostCents)
        assertEquals("VERIFIED_100_CLEAN", store?.complianceStatus)
        assertEquals("PENDING_APPROVAL", store?.status)

        // Verify it was saved to Room
        val savedStores = repository.getStorefronts().first()
        assertTrue("Storefront list should not be empty", savedStores.isNotEmpty())
        assertTrue("Storefront should exist in Room", savedStores.any { it.companyName == "Vanguard Organic Botanics" })
    }

    @Test
    fun testTrademarkPreClearanceDetection() = runBlocking {
        // Clean title
        val cleanResult = orchestrator.verifyTrademarkAndCopyright(
            title = "Aesthetic Mountain Pine Line Art",
            category = "POD_DESIGN"
        )
        assertEquals("CLEARED_SAFE", cleanResult.verificationStatus)
        assertTrue("Clearance certificate hash should be populated", cleanResult.clearanceHash.startsWith("CERT-"))

        // Trademark collision title
        val collisionResult = orchestrator.verifyTrademarkAndCopyright(
            title = "Disney Marvel Spider-Man Action Hoodie",
            category = "POD_DESIGN"
        )
        assertEquals("REJECTED_COLLISION", collisionResult.verificationStatus)
        assertTrue("Should detect collision detail", collisionResult.trademarkCollisions.contains("Collision detected"))
    }

    @Test
    fun testMultiAgentPitchCollaboration() = runBlocking {
        val result = orchestrator.orchestratePitchBrainstorm("Cyberpunk Retro 80s")
        assertTrue("Pitch brainstorm should complete successfully", result.isSuccess)

        val decision = result.getOrNull()
        assertNotNull(decision)
        assertTrue("Decision should require human approval", decision?.status == "PENDING")
        assertTrue("Decision should verify copyright and trademark clearance", decision?.payloadJson?.contains("Cleared") == true)

        // Verify inter-agent messages in Room
        val messages = repository.getAgentMessages().first()
        assertTrue("Inter-agent communication messages should be logged", messages.isNotEmpty())
    }

    @Test
    fun testDoubleEntryLedgerIntegrity() = runBlocking {
        val entries = repository.getLedgerEntries().first()
        assertFalse("Ledger should contain seeded entries", entries.isEmpty())

        val saleEntries = entries.filter { it.category == "SALE" }
        assertTrue("Should have sale entries", saleEntries.isNotEmpty())
        saleEntries.forEach { sale ->
            assertTrue("Amount in whole cents must be positive", sale.amountCents > 0)
        }
    }

    @Test
    fun testSecretManagerMaskingAndLocalPersistence() = runBlocking {
        val secretManager = orchestrator.secretManager

        // Test masking
        val masked = secretManager.maskSecret("ghp_1234567890abcdef", 4, 4)
        assertEquals("ghp_••••••••cdef", masked)

        val unconfigMasked = secretManager.maskSecret("", 4, 4)
        assertEquals("Not Configured", unconfigMasked)

        // Save new test keys
        secretManager.saveGitHubToken("ghp_test_token_123456")
        secretManager.saveStripeSecretKey("sk_test_mock_stripe_key_999")

        val status = secretManager.secretStatus.value
        assertTrue("Should recognize configured github key", secretManager.hasValidGitHubToken() || status.isGitHubConfigured)
    }

    @Test
    fun testPaymentServiceWebhookSignatureVerification() = runBlocking {
        val paymentService = orchestrator.paymentService
        val payload = """{"id":"evt_test_123","type":"checkout.session.completed"}"""
        val secret = "whsec_test_secret_abc123"
        val timestamp = (System.currentTimeMillis() / 1000).toString()

        // Generate valid HMAC SHA256 signature
        val signedPayload = "$timestamp.$payload"
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        val computedHex = mac.doFinal(signedPayload.toByteArray()).joinToString("") { "%02x".format(it) }

        val header = "t=$timestamp,v1=$computedHex"
        val isValid = paymentService.verifyWebhookSignature(payload, header, secret)
        assertTrue("Signature verification should succeed for valid HMAC", isValid)

        val isInvalid = paymentService.verifyWebhookSignature(payload, "t=$timestamp,v1=tampered_hash", secret)
        assertFalse("Signature verification should fail for tampered signature", isInvalid)
    }

    @Test
    fun testDigitalProductCreationAnd100PercentMargin() = runBlocking {
        val result = orchestrator.generateAndPublishDigitalProduct(
            niche = "AI Developer Workflows",
            category = "PROMPT_GUIDE",
            companyName = "AgentOps Digital"
        )

        assertTrue("Digital product creation should succeed", result.isSuccess)
        val product = result.getOrNull()
        assertNotNull(product)
        assertEquals(0L, product?.costCents) // $0 COGS
        assertEquals(100, product?.profitMarginPercent) // 100% net margin
        assertTrue("Price should be positive", (product?.priceCents ?: 0) > 0)
        assertTrue("Clearance certificate should be issued", product?.clearanceCertificate?.startsWith("CERT-") == true)
        assertTrue("Payment link should be populated", product?.stripePaymentLink?.contains("stripe") == true)

        // Verify stored in Room database
        val savedProducts = repository.getDigitalProducts().first()
        assertTrue("Should have saved digital products in Room", savedProducts.any { it.id == product?.id })
    }
}
