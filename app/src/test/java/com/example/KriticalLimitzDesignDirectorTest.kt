package com.example

import com.opensource.autonomous.agent.data.AgentPrompts
import com.opensource.autonomous.agent.ui.KriticalLimitzMockupPayload
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests validating the Lead Autonomous Design Director AI for "Kritical Limitz" (KL)
 * and strict enforcement of live-data-only and verification boundaries:
 * 1. Brand Core & Aesthetic DNA synthesis
 * 2. Structured 4-part payload for image generation logistics
 * 3. Mandatory operational constraints (No unauthorized publishing & No unauthorized store creation)
 * 4. Zero demo data policy
 */
class KriticalLimitzDesignDirectorTest {

    @Test
    fun testKriticalLimitzPromptAestheticDNA() {
        val prompt = AgentPrompts.KRITICAL_LIMITZ_POD_DESIGN_DIRECTOR_PROMPT
        assertNotNull(prompt)

        // Verify Brand Core Pillars
        assertTrue("Must include Chicano & West Coast Street Culture", prompt.contains("Chicano & West Coast Street Culture"))
        assertTrue("Must include Dark Occult & Esoteric Imagery", prompt.contains("Dark Occult & Esoteric Imagery"))
        assertTrue("Must include Luxury Egyptian Mysticism", prompt.contains("Luxury Egyptian Mysticism"))
        assertTrue("Must include Hyper-Detailed Texturing", prompt.contains("Hyper-Detailed Texturing"))

        // Verify Hard Boundaries
        assertTrue("Must enforce NO UNAUTHORIZED STOREFRONT PUBLISHING", prompt.contains("NO UNAUTHORIZED STOREFRONT PUBLISHING"))
        assertTrue("Must enforce NO UNAUTHORIZED STORE CREATION", prompt.contains("NO UNAUTHORIZED STORE CREATION"))
        assertTrue("Must require PENDING STAGING state", prompt.contains("PENDING STAGING"))
        assertTrue("Must enforce kriticallimitz singular assigned domain", prompt.contains("Kritical Limitz"))

        // Verify Output Formatting
        assertTrue("Must enforce Design Title & Concept Breakdown", prompt.contains("Design Title & Concept Breakdown"))
        assertTrue("Must enforce The Master Prompt", prompt.contains("The Master Prompt"))
        assertTrue("Must enforce Placement Matrix", prompt.contains("Placement Matrix"))
        assertTrue(
            "Must enforce exact staging tag string",
            prompt.contains("[STATUS: PENDING HUMAN REVIEW - LIFT LISTING RESTRICTION TO PUBLISH]")
        )
    }

    @Test
    fun testKriticalLimitzMockupPayloadStructure() {
        val stagingTag = "[STATUS: PENDING HUMAN REVIEW - LIFT LISTING RESTRICTION TO PUBLISH]"
        val payload = KriticalLimitzMockupPayload(
            title = "Kritical Limitz - Crowned Hellhound in Barbed Wire",
            conceptBreakdown = "Synthesis of underground West Coast fine-line ink work, dark occult sigils, and luxury Egyptian mysticism.",
            masterPrompt = "Master-class dark custom streetwear illustration of snarling three-headed hellhound bound in rusted barbed wire, fine-line black and grey ink work, chiaroscuro rim lighting $stagingTag",
            placementMatrix = "Center Chest (11\" x 13\") & Full Back Print (14\" x 16\")",
            mockupImageUrl = "https://pollinations.ai/p/test",
            garmentType = "Heavyweight Luxury Tee (280 GSM)",
            garmentColorHex = "#121417",
            stagingTag = stagingTag
        )

        assertEquals("Kritical Limitz - Crowned Hellhound in Barbed Wire", payload.title)
        assertTrue(payload.conceptBreakdown.isNotBlank())
        assertTrue(payload.masterPrompt.contains(stagingTag))
        assertEquals(stagingTag, payload.stagingTag)
        assertTrue(payload.placementMatrix.contains("Center Chest"))
        assertTrue(payload.mockupImageUrl.startsWith("https://"))
    }
}
