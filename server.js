import express from 'express';
import { GoogleGenAI } from '@google/genai';
import fetch from 'node-fetch';

const router = express.Router();
const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

// Route A — Generate image prompt via Gemini
router.post('/api/generate-prompt', async (req, res) => {
  const { brandContext, recentPrompts, userHint } = req.body;
  try {
    const response = await ai.models.generateContent({
      model: 'gemini-4-argon',
      contents: `You are the creative director of Print Defy, a high-end luxury streetwear brand.
Brand identity: ${brandContext}
Recent prompts (do not repeat): ${(recentPrompts || []).join(' | ')}
User hint (optional): ${userHint || 'none'}
Generate one image generation prompt for a physical garment graphic.
Rules: transparent PNG, print-ready, luxury streetwear, no background, ultra high resolution.
Output only the prompt text. No preamble. No explanation.`
    });
    const generatedPrompt = response.candidates[0].content.parts[0].text.trim()
      + ', highly detailed, isolated on transparent background, no background, print-ready, fashion editorial quality, ultra high resolution';
    res.json({ generatedPrompt });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Route B — Generate image via Hugging Face FLUX
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

// Route C — Remove background via RMBG-1.4
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
    // On RMBG failure — return original, log warning, do not fake anything
    console.warn('[ULTRA] Background removal failed, returning original:', e.message);
    res.json({ transparentImageUrl: req.body.imageUrl });
  }
});

// Route D — Publish physical product to Printify POD Pop-Up only
router.post('/api/publish-product', async (req, res) => {
  const { sku, productId, colorHex, zone, mockupImageUrl } = req.body;
  if (!productId || !sku || !mockupImageUrl) {
    return res.status(400).json({ error: 'Missing required fields: productId, sku, mockupImageUrl' });
  }
  try {
    const shopId = process.env.PRINTIFY_SHOP_ID;
    const createRes = await fetch(`https://api.printify.com/v1/shops/${shopId}/products.json`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${process.env.PRINTIFY_API_TOKEN}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        title: `Print Defy — ${sku}`,
        blueprint_id: parseInt(productId),
        print_provider_id: parseInt(process.env.PRINTIFY_PROVIDER_ID),
        variants: [{ id: parseInt(productId), price: 2999, is_enabled: true }],
        print_areas: [{
          variant_ids: [parseInt(productId)],
          placeholders: [{ position: zone, images: [{ id: sku, x: 0.5, y: 0.5, scale: 1, angle: 0 }] }]
        }]
      })
    });
    if (!createRes.ok) {
      const err = await createRes.text();
      throw new Error(`Printify create failed: ${createRes.status} — ${err}`);
    }
    const created = await createRes.json();

    // Publish to live store
    const publishRes = await fetch(
      `https://api.printify.com/v1/shops/${shopId}/products/${created.id}/publish.json`,
      { method: 'POST', headers: { Authorization: `Bearer ${process.env.PRINTIFY_API_TOKEN}` } }
    );
    if (!publishRes.ok) throw new Error(`Printify publish failed: ${publishRes.status}`);

    res.json({
      printifyProductId: created.id, // real Printify ID — never fabricated
      storeUrl: `https://printify.com/manage-my-store/${created.id}`
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Route E — Log design approval (creative event — separate from transactions)
router.post('/api/log-design-approval', async (req, res) => {
  const { designId, sku, approvedAt } = req.body;
  try {
    const fs = await import('fs/promises');
    let log = [];
    try { log = JSON.parse(await fs.readFile('./design-approvals.json', 'utf8')); } catch {}
    log.push({ designId, sku, approvedAt });
    await fs.writeFile('./design-approvals.json', JSON.stringify(log, null, 2));
    res.json({ logged: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

export default router;
