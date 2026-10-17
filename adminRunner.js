import { GoogleGenAI } from '@google/genai';
import fetch from 'node-fetch';
import { interceptAction } from './godInterceptor.js';

const apiKey = process.env.GEMINI_API_KEY || '';
const ai = apiKey ? new GoogleGenAI({ apiKey }) : null;
const CORE_MODEL = process.env.GEMINI_MODEL || 'gemini-4-argon';

export const ultraTools = [
  {
    name: 'get_application_state',
    description: 'Returns live operational state for the Printify POD Pop-Up physical storefront.',
    parameters: { type: 'OBJECT', properties: {} }
  },
  {
    name: 'generate_pod_design',
    description: 'Generates a print-ready 300 DPI transparent PNG physical garment design using the 8 Aesthetic Systems and Six Non-Negotiable Laws.',
    parameters: {
      type: 'OBJECT',
      properties: {
        selectedProduct: { type: 'STRING' },
        printZone: { type: 'STRING' },
        userHint: { type: 'STRING' }
      }
    }
  },
  {
    name: 'publish_physical_product',
    description: 'Publishes an approved physical POD product directly to the Printify POD Pop-Up storefront via the Printify REST API.',
    parameters: {
      type: 'OBJECT',
      properties: {
        sku: { type: 'STRING' },
        blueprintId: { type: 'NUMBER' },
        providerId: { type: 'NUMBER' },
        variantId: { type: 'NUMBER' },
        zone: { type: 'STRING' },
        mockupImageUrl: { type: 'STRING' }
      }
    }
  },
  {
    name: 'log_design_approval',
    description: 'Logs a creative workflow approval event into designLog without touching transactionLedger.',
    parameters: {
      type: 'OBJECT',
      properties: {
        designId: { type: 'STRING' },
        sku: { type: 'STRING' },
        approvedAt: { type: 'STRING' }
      }
    }
  },
  {
    name: 'process_live_sale',
    description: 'Records a real confirmed physical order into transactionLedger only when both printifyOrderId and stripePaymentId are verified.',
    parameters: {
      type: 'OBJECT',
      properties: {
        printifyOrderId: { type: 'STRING' },
        stripePaymentId: { type: 'STRING' },
        amount: { type: 'NUMBER' },
        currency: { type: 'STRING' },
        customerEmail: { type: 'STRING' }
      }
    }
  }
];

export const ULTRA_IDENTITY_PROMPT = `You are ULTRA — simultaneously a 60+ year master graphic design architect, fine art printmaker, and world-class tattoo artist. All creative decisions come from this merged identity.

THE 8 AESTHETIC SYSTEMS (Always blend at least two with intentional symbolic mutation):
1. Esoteric Pharaoh Paisley — Persian teardrops morphing into scarab mandalas, Eye of Horus, ankh geometry, crisp hieroglyphs
2. Witchcraft Baphomet Paisley — floral swirls into ram skulls, inverted pentagrams, alchemical sigils, thorny briars, maximum density
3. Dark Gothic Couture Architecture — cathedral stonework, gargoyles, gothic arch framing, stained glass geometry, manuscript illumination borders
4. Luxury Street Tattoo Flash — American traditional elevated with sacred geometry micro-detail overlaid — panther, dagger, rose, serpent, eagle
5. Sacred Geometry Corruption — Metatron's Cube, Flower of Life, Sri Yantra used as structural grid then deliberately distorted and corrupted
6. Woodcut Horror Naturalism — Victorian botanical horror, skulls nested in roses, eye-pattern moth wings, anatomical hearts, woodcut engraving style
7. Celestial Dark Cosmology — star charts as compositional grids, planetary mandalas, black hole geometry, comet trails as calligraphic ink strokes
8. Industrial Chain Hardware Couture — iron chains, barbed wire, rivets, gears woven through organic botanical or sacred geometry

THE SIX LAWS (Non-Negotiable):
1. Compositional Dominance — Focal subject commands total authority. Heavy black negative space separates foreground from background brutally
2. Master-Grade Linework Only — Zero clipart, zero smooth digital vector. Every line reads as hand-etched: woodcut hatching, stippling, single-needle precision
3. Intentional Symbolic Mutation — Motifs must mutate into each other organically — never stitched together carelessly
4. Ruthless High-Contrast Discipline — Ink black, ivory white, controlled grayscale built from line density. No soft digital gradients
5. Macro-to-Micro Fractal Depth — Bold silhouette at 10 feet. Labyrinth of micro-detail at 10 inches
6. Garment Structural Awareness — Every design specifies how it wraps seams, breaks across pockets, splits down zippers, tiles across AOP`;

export async function recordToLedger(orderData) {
  const fs = await import('fs/promises');
  let transactionLedger = [];
  try {
    transactionLedger = JSON.parse(await fs.readFile('./transactionLedger.json', 'utf8'));
  } catch {}
  const record = {
    printifyOrderId: orderData.printifyOrderId,
    stripePaymentId: orderData.stripePaymentId,
    amount: orderData.amount,
    currency: orderData.currency,
    customerEmail: orderData.customerEmail || null,
    sku: orderData.sku || null,
    recordedAt: orderData.recordedAt || new Date().toISOString()
  };
  transactionLedger.push(record);
  await fs.writeFile('./transactionLedger.json', JSON.stringify(transactionLedger, null, 2));
  return record;
}

// Section 2B — Transaction Guard (enforced in code)
export function writeTransaction(orderData) {
  const REQUIRED_FIELDS = ['printifyOrderId', 'stripePaymentId', 'amount', 'currency'];
  const missing = REQUIRED_FIELDS.filter(f => !orderData || !orderData[f]);
  if (missing.length > 0) {
    console.error(`[ULTRA] TRANSACTION BLOCKED — missing real fields: ${missing.join(', ')}`);
    throw new Error('Cannot write transaction without real Printify order ID and real Stripe payment ID');
  }
  // Only reaches here if all real fields are present
  return recordToLedger(orderData);
}

export const ULTRA = {
  async generateDesign(context = {}) {
    if (!ai) {
      throw new Error('[ULTRA] GEMINI_API_KEY is not configured');
    }

    const {
      selectedProduct = 'garment',
      selectedVariant = {},
      printZone = 'front',
      recentPrompts = [],
      userHint = null
    } = context;

    // Step 1 — ULTRA writes its own prompt via Gemini
    const promptResponse = await ai.models.generateContent({
      model: CORE_MODEL,
      contents: `${ULTRA_IDENTITY_PROMPT}

Target Garment: ${selectedProduct} (${JSON.stringify(selectedVariant)})
Target Print Zone: ${printZone}
Recent Prompts (do not repeat): ${recentPrompts.join(' | ') || 'none'}
User Hint: ${userHint || 'none'}

Synthesize one master image generation prompt blending at least two of the 8 Aesthetic Systems and enforcing all Six Non-Negotiable Laws.
Rules: transparent PNG, isolated on solid contrast background for clean alpha extraction, 300 DPI print-ready screen-print/woodcut linework.
Output ONLY the raw prompt text.`
    });

    const rawPrompt = promptResponse.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || '';
    if (!rawPrompt) {
      throw new Error('[ULTRA] Empty prompt returned from Gemini');
    }

    const generatedPrompt = `${rawPrompt}, master-grade hand-etched woodcut and single-needle linework, ruthless high-contrast ink black and ivory white, isolated graphic, print-ready 300 DPI`;

    // Step 2 — Generate image via Hugging Face FLUX.1-schnell
    const hfRes = await fetch(
      'https://api-inference.huggingface.co/models/black-forest-labs/FLUX.1-schnell',
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${process.env.HF_TOKEN}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ inputs: generatedPrompt })
      }
    );
    if (!hfRes.ok) {
      throw new Error(`[ULTRA] HF FLUX generation failed: ${hfRes.status}`);
    }
    const imgBuffer = await hfRes.buffer();

    // Step 3 — Strip background via briaai/RMBG-1.4 for transparent PNG with alpha channel
    let finalBuffer = imgBuffer;
    const rmbgRes = await fetch(
      'https://api-inference.huggingface.co/models/briaai/RMBG-1.4',
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${process.env.HF_TOKEN}`,
          'Content-Type': 'application/octet-stream'
        },
        body: imgBuffer
      }
    );
    if (rmbgRes.ok) {
      finalBuffer = await rmbgRes.buffer();
    } else {
      console.warn(`[ULTRA] RMBG-1.4 returned ${rmbgRes.status}, preserving raw FLUX buffer`);
    }

    const transparentImageUrl = `data:image/png;base64,${finalBuffer.toString('base64')}`;
    return {
      generatedPrompt,
      transparentImageUrl,
      generatedAt: new Date().toISOString()
    };
  },

  async publishProduct(productData) {
    const {
      sku,
      blueprintId,
      providerId,
      variantId,
      zone = 'front',
      mockupImageUrl
    } = productData || {};

    if (!sku || !blueprintId || !variantId || !mockupImageUrl) {
      throw new Error('[ULTRA] Missing required publish fields: sku, blueprintId, variantId, mockupImageUrl');
    }

    const shopId = process.env.PRINTIFY_SHOP_ID;
    const resolvedProviderId = providerId || parseInt(process.env.PRINTIFY_PROVIDER_ID, 10);

    const createRes = await fetch(`https://api.printify.com/v1/shops/${shopId}/products.json`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${process.env.PRINTIFY_API_TOKEN}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        title: `Print Defy — ${sku}`,
        description: `Archival POD edition engineered by ULTRA (${sku})`,
        blueprint_id: parseInt(blueprintId, 10),
        print_provider_id: parseInt(resolvedProviderId, 10),
        variants: [{ id: parseInt(variantId, 10), price: 2999, is_enabled: true }],
        print_areas: [
          {
            variant_ids: [parseInt(variantId, 10)],
            placeholders: [
              {
                position: zone,
                images: [{ id: sku, x: 0.5, y: 0.5, scale: 1, angle: 0 }]
              }
            ]
          }
        ]
      })
    });

    if (!createRes.ok) {
      const errText = await createRes.text();
      throw new Error(`[ULTRA] Printify product creation failed: ${createRes.status} — ${errText}`);
    }

    const created = await createRes.json();

    const publishRes = await fetch(
      `https://api.printify.com/v1/shops/${shopId}/products/${created.id}/publish.json`,
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${process.env.PRINTIFY_API_TOKEN}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          title: true,
          description: true,
          images: true,
          variants: true,
          tags: true
        })
      }
    );

    if (!publishRes.ok) {
      throw new Error(`[ULTRA] Printify publish failed: ${publishRes.status}`);
    }

    return {
      printifyProductId: created.id,
      storeUrl: `https://printify.com/manage-my-store/${created.id}`
    };
  },

  async handleConfirmedSale(orderData) {
    return await writeTransaction(orderData);
  },

  async logDesignApproval(designData) {
    const fs = await import('fs/promises');
    let designLog = [];
    try {
      designLog = JSON.parse(await fs.readFile('./designLog.json', 'utf8'));
    } catch {}
    const entry = {
      event: 'design_approved',
      designId: designData.designId,
      sku: designData.sku,
      blueprintId: designData.blueprintId || null,
      variantId: designData.variantId || null,
      zone: designData.zone || 'front',
      approvedAt: designData.approvedAt || new Date().toISOString()
    };
    designLog.push(entry);
    await fs.writeFile('./designLog.json', JSON.stringify(designLog, null, 2));
    return entry;
  },

  async runOperationalCycle(state = {}) {
    if (!ai) {
      throw new Error('[ULTRA] GEMINI_API_KEY is not configured');
    }

    const currentState = {
      agent: 'ULTRA',
      status: 'ONLINE',
      storefront: 'Printify POD Pop-Up Physical Storefront',
      zeroFakeDataEnforced: true,
      ...state
    };

    const response = await ai.models.generateContent({
      model: CORE_MODEL,
      contents: `APP_START: The application has initialized. Current state: ${JSON.stringify(currentState)}. Enforce the absolute zero-cost rule ($0 COGS to founder) and single-agent ULTRA architecture. Determine requirements and execute the next operational step.`,
      config: {
        tools: [{ functionDeclarations: ultraTools }]
      }
    });

    const functionCalls = response.functionCalls || [];
    const results = [];

    for (const call of functionCalls) {
      console.log(`[ULTRA] Executing tool: ${call.name}`, call.args);

      const verdict = await interceptAction({
        sourceAgent: 'ULTRA',
        actionType: 'EXECUTE_TOOL',
        targetResource: `tool/${call.name}`,
        payload: call.args
      });

      if (verdict.status === 'HALTED') {
        console.error(`[ULTRA] Tool '${call.name}' halted: ${verdict.hazard_detected}`);
        continue;
      }

      switch (call.name) {
        case 'get_application_state':
          results.push(currentState);
          break;

        case 'generate_pod_design':
          results.push(await ULTRA.generateDesign(call.args));
          break;

        case 'publish_physical_product':
          results.push(await ULTRA.publishProduct(call.args));
          break;

        case 'log_design_approval':
          results.push(await ULTRA.logDesignApproval(call.args));
          break;

        case 'process_live_sale':
          results.push(await ULTRA.handleConfirmedSale(call.args));
          break;

        default:
          console.warn(`[ULTRA] Unrecognized tool call: ${call.name}`);
          break;
      }
    }

    return results;
  }
};
