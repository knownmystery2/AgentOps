import { GoogleGenAI } from '@google/genai';
import { autoStartAICrashLoop } from './agentSwarm.js';
import { interceptAction } from './godInterceptor.js';

const apiKey = process.env.GEMINI_API_KEY || '';
const ai = apiKey ? new GoogleGenAI({ apiKey }) : null;
const CORE_MODEL = 'gemini-4-argon'; // Paid tier — Gemini 4 Argon (sole paid component in build)

/**
 * Tool schemas available to the unified ULTRA Agent
 */
export const adminRunnerTools = [
  {
    name: "get_application_state",
    description: "Returns data on what needs processing in the Printify POD Pop-Up studio.",
    parameters: { type: "OBJECT", properties: {} }
  },
  {
    name: "trigger_swarm_cycle",
    description: "Instructs ULTRA to generate non-copyright, 100% zero-cost ($0 COGS) verified physical POD designs for the Printify POD Pop-Up.",
    parameters: { type: "OBJECT", properties: { theme: { type: "STRING" } } }
  },
  {
    name: "post_digital_product",
    description: "Disabled — POD Pop-Up only accepts physical Printify products.",
    parameters: { type: "OBJECT", properties: { productId: { type: "STRING" } } }
  },
  {
    name: "launch_marketing_campaign",
    description: "Dispatches organic zero-ad-spend marketing (Pinterest, TikTok, Twitter, Email).",
    parameters: { type: "OBJECT", properties: { productId: { type: "STRING" } } }
  },
  {
    name: "process_live_sale",
    description: "Records a live confirmed physical order with real Printify order ID and real Stripe payment ID into the transaction ledger.",
    parameters: {
      type: "OBJECT",
      properties: {
        productId: { type: "STRING" },
        printifyOrderId: { type: "STRING" },
        stripePaymentId: { type: "STRING" },
        amount: { type: "NUMBER" },
        currency: { type: "STRING" }
      }
    }
  },
  {
    name: "log_manager_status",
    description: "Records what ULTRA is doing to the system logs.",
    parameters: { type: "OBJECT", properties: { message: { type: "STRING" } } }
  }
];

export async function recordSaleToLedger(args) {
  // Hard guard — never write without real IDs
  if (!args || !args.printifyOrderId || !args.stripePaymentId) {
    throw new Error('[ULTRA] Cannot record transaction without real printifyOrderId and stripePaymentId');
  }
  const fs = await import('fs/promises');
  let ledger = [];
  try { ledger = JSON.parse(await fs.readFile('./ledger.json', 'utf8')); } catch {}
  ledger.push({ ...args, recordedAt: new Date().toISOString() });
  await fs.writeFile('./ledger.json', JSON.stringify(ledger, null, 2));
}

export async function recordCampaignIntent(args) {
  const fs = await import('fs/promises');
  let log = [];
  try { log = JSON.parse(await fs.readFile('./campaigns.json', 'utf8')); } catch {}
  log.push({ ...args, queuedAt: new Date().toISOString() });
  await fs.writeFile('./campaigns.json', JSON.stringify(log, null, 2));
}

export async function fetchCurrentAppState() {
  return {
    agent: "ULTRA",
    status: "ONLINE",
    storefront: "Printify POD Pop-Up Physical Storefront",
    uptimeSeconds: Math.floor(process.uptime ? process.uptime() : 0),
    zeroCostRuleActive: true,
    nextRequiredAction: "INITIALIZE_POD_POPUP_STUDIO"
  };
}

/**
 * Startup Trigger — Pings Gemini 4 Argon with APP_START payload and wired tool schemas
 */
export async function initializeAdminRunner() {
  const currentState = await fetchCurrentAppState();

  if (ai) {
    try {
      const response = await ai.models.generateContent({
        model: CORE_MODEL,
        contents: `APP_START: ${JSON.stringify(currentState)}. Enforce $0 COGS rule. Execute next operational step.`,
        config: {
          tools: [{ functionDeclarations: adminRunnerTools }]
        }
      });

      if (response.functionCalls && response.functionCalls.length > 0) {
        await executeAIRequestedFunctions(response.functionCalls);
        return response.functionCalls;
      }
    } catch (e) {
      console.error("[ULTRA] Gemini API call failed during initialization:", e.message);
      throw e;
    }
  }

  const queue = await autoStartAICrashLoop();
  return queue;
}

export async function executeAIRequestedFunctions(functionCalls) {
  for (const call of functionCalls) {
    console.log(`[ULTRA] Executing: ${call.name}`, call.args);

    const verdict = await interceptAction({
      sourceAgent: 'ULTRA',
      actionType: 'EXECUTE_TOOL',
      targetResource: `tool/${call.name}`,
      payload: call.args
    });

    if (verdict.status === 'HALTED') {
      console.error(`[ULTRA] Halted: ${verdict.hazard_detected}`);
      continue;
    }

    switch (call.name) {
      case 'trigger_swarm_cycle':
        await autoStartAICrashLoop();
        break;

      case 'post_digital_product':
        // POD Pop-Up only accepts physical Printify products
        console.warn('[ULTRA] Digital product posting is disabled — POD Pop-Up is physical only');
        break;

      case 'launch_marketing_campaign':
        console.log(`[ULTRA] Organic campaign queued: ${call.args.productId}`);
        await recordCampaignIntent(call.args);
        break;

      case 'process_live_sale':
        // Only process if real Printify order ID and Stripe payment ID are present
        if (!call.args.printifyOrderId || !call.args.stripePaymentId) {
          console.error('[ULTRA] REJECTED fake sale — missing real order IDs. No record written.');
          break;
        }
        await recordSaleToLedger(call.args);
        break;

      case 'log_manager_status':
        console.log(`[ULTRA STATUS] ${call.args.message}`);
        break;

      case 'get_application_state':
        await fetchCurrentAppState();
        break;

      default:
        console.warn(`[ULTRA] Unknown tool: ${call.name}`);
    }
  }
}
