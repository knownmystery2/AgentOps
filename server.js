import express from 'express';
import Stripe from 'stripe';
import { GoogleGenAI } from '@google/genai';
import fetch from 'node-fetch';
import { ULTRA, writeTransaction, ULTRA_IDENTITY_PROMPT } from './adminRunner.js';

const router = express.Router();
const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
const stripe = process.env.STRIPE_SECRET_KEY ? new Stripe(process.env.STRIPE_SECRET_KEY) : null;
const CORE_MODEL = process.env.GEMINI_MODEL || 'gemini-4-argon';

// Section 3B — Stripe webhook for real order confirmation (must use raw body, not JSON-parsed body)
router.post(
  '/api/stripe-webhook',
  express.raw({ type: 'application/json' }),
  async (req, res) => {
    if (!stripe) {
      return res.status(500).send('Stripe is not configured');
    }
    const sig = req.headers['stripe-signature'];
    let event;
    try {
      event = stripe.webhooks.constructEvent(
        req.body,
        sig,
        process.env.STRIPE_WEBHOOK_SECRET // from Stripe Dashboard -> Webhooks -> Signing secret
      );
    } catch (err) {
      console.error('[ULTRA] Webhook signature failed:', err.message);
      return res.status(400).send(`Webhook Error: ${err.message}`);
    }

    if (event.type === 'payment_intent.succeeded') {
      const paymentIntent = event.data.object;
      // Extract Printify order ID from metadata (set this when creating PaymentIntent)
      const printifyOrderId = paymentIntent.metadata?.printifyOrderId;

      if (!printifyOrderId) {
        console.error('[ULTRA] payment_intent.succeeded received but no printifyOrderId in metadata');
        return res.json({ received: true }); // Acknowledge but do not write
      }

      // Write REAL transaction — both IDs confirmed
      await writeTransaction({
        printifyOrderId,
        stripePaymentId: paymentIntent.id,
        amount: paymentIntent.amount,
        currency: paymentIntent.currency,
        customerEmail: paymentIntent.receipt_email,
        recordedAt: new Date().toISOString()
      });

      console.log(`[ULTRA] Real transaction recorded: ${paymentIntent.id}`);
    }

    res.json({ received: true });
  }
);

// Section 4B — Fetch live Printify catalog blueprints
router.get('/api/catalog', async (req, res) => {
  try {
    const response = await fetch('https://api.printify.com/v1/catalog/blueprints.json', {
      headers: { Authorization: `Bearer ${process.env.PRINTIFY_API_TOKEN}` }
    });
    if (!response.ok) throw new Error(`Catalog fetch failed: ${response.status}`);
    const data = await response.json();
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Section 4B — Fetch variants and provider for a specific Printify blueprint
router.get('/api/catalog/:blueprintId/variants', async (req, res) => {
  try {
    const { blueprintId } = req.params;
    // Get print providers for this blueprint first
    const provRes = await fetch(
      `https://api.printify.com/v1/catalog/blueprints/${blueprintId}/print_providers.json`,
      { headers: { Authorization: `Bearer ${process.env.PRINTIFY_API_TOKEN}` } }
    );
    if (!provRes.ok) throw new Error(`Providers fetch failed: ${provRes.status}`);
    const providers = await provRes.json();
    const providerId = providers[0]?.id; // Use first available provider
    if (!providerId) throw new Error('No print provider available for this blueprint');

    const varRes = await fetch(
      `https://api.printify.com/v1/catalog/blueprints/${blueprintId}/print_providers/${providerId}/variants.json`,
      { headers: { Authorization: `Bearer ${process.env.PRINTIFY_API_TOKEN}` } }
    );
    if (!varRes.ok) throw new Error(`Variants fetch failed: ${varRes.status}`);
    const varData = await varRes.json();
    const variants = Array.isArray(varData) ? varData : (varData.variants || []);
    res.json({ variants, providerId });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Section 5D Step 1 — ULTRA writes its own prompt via Gemini
router.post('/api/generate-prompt', async (req, res) => {
  const { selectedProduct, selectedVariant, printZone, recentPrompts, userHint } = req.body;
  try {
    const response = await ai.models.generateContent({
      model: CORE_MODEL,
      contents: `${ULTRA_IDENTITY_PROMPT}

Selected Garment: ${selectedProduct || 'garment'}
Variant Options: ${JSON.stringify(selectedVariant || {})}
Print Position: ${printZone || 'front'}
Recent Prompts (do not repeat): ${(recentPrompts || []).join(' | ') || 'none'}
User Hint (optional): ${userHint || 'none'}

Generate one master image generation prompt blending at least two of the 8 Aesthetic Systems and enforcing all Six Non-Negotiable Laws.
Rules: transparent PNG, hand-etched woodcut/single-needle linework, ruthless high-contrast ink black and ivory white, no background, ultra high resolution 300 DPI.
Output only the prompt text. No preamble. No explanation.`
    });
    const generatedPrompt =
      response.candidates[0].content.parts[0].text.trim() +
      ', master-grade hand-etched linework, woodcut hatching, single-needle precision, isolated on transparent background, no background, 300 DPI print-ready';
    res.json({ generatedPrompt });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Section 5D Step 2 — Generate image via Hugging Face FLUX.1-schnell
router.post('/api/generate-image', async (req, res) => {
  const { prompt } = req.body;
  try {
    const hfRes = await fetch(
      'https://api-inference.huggingface.co/models/black-forest-labs/FLUX.1-schnell',
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${process.env.HF_TOKEN}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ inputs: prompt })
      }
    );
    if (!hfRes.ok) throw new Error(`HF API error: ${hfRes.status}`);
    const buffer = await hfRes.buffer();
    const imageUrl = `data:image/png;base64,${buffer.toString('base64')}`;
    res.json({ imageUrl });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Section 5D Step 3 — Remove background via RMBG-1.4 for transparent PNG with alpha channel
router.post('/api/remove-background', async (req, res) => {
  const { imageUrl } = req.body;
  try {
    const base64Data = imageUrl.replace(/^data:image\/\w+;base64,/, '');
    const imageBuffer = Buffer.from(base64Data, 'base64');
    const hfRes = await fetch(
      'https://api-inference.huggingface.co/models/briaai/RMBG-1.4',
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${process.env.HF_TOKEN}`,
          'Content-Type': 'application/octet-stream'
        },
        body: imageBuffer
      }
    );
    if (!hfRes.ok) throw new Error(`RMBG error: ${hfRes.status}`);
    const resultBuffer = await hfRes.buffer();
    const transparentImageUrl = `data:image/png;base64,${resultBuffer.toString('base64')}`;
    res.json({ transparentImageUrl });
  } catch (e) {
    console.warn('[ULTRA] Background removal failed, returning original:', e.message);
    res.json({ transparentImageUrl: req.body.imageUrl });
  }
});

// Publish physical product to Printify POD Pop-Up only
router.post('/api/publish-product', async (req, res) => {
  try {
    const {
      sku,
      blueprintId,
      productId,
      providerId,
      variantId,
      zone,
      mockupImageUrl
    } = req.body;

    const result = await ULTRA.publishProduct({
      sku,
      blueprintId: blueprintId || productId,
      providerId,
      variantId: variantId || productId,
      zone,
      mockupImageUrl
    });

    res.json(result);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Log design approval (creative workflow event — never touches transactionLedger)
router.post('/api/log-design-approval', async (req, res) => {
  try {
    const entry = await ULTRA.logDesignApproval(req.body);
    res.json({ logged: true, entry });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

export default router;
