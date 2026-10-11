import { GoogleGenAI } from '@google/genai';
import { autoStartAICrashLoop } from './agentSwarm.js';

const apiKey = process.env.GEMINI_API_KEY || '';
const ai = apiKey ? new GoogleGenAI({ apiKey }) : null;
const CORE_MODEL = 'gemini-2.5-pro';

/**
 * Tool schemas available to the AdminRunner
 */
export const adminRunnerTools = [
  {
    name: "get_application_state",
    description: "Returns data on what needs processing in the store and swarm.",
    parameters: { type: "OBJECT", properties: {} }
  },
  {
    name: "trigger_swarm_cycle",
    description: "Instructs the swarm to create 5 non-copyright, 100% zero-cost ($0 COGS) verified digital products for a pop-up theme.",
    parameters: { type: "OBJECT", properties: { theme: { type: "STRING" } } }
  },
  {
    name: "post_digital_product",
    description: "Publishes a verified zero-cost digital product to live storefront with Stripe links.",
    parameters: { type: "OBJECT", properties: { productId: { type: "STRING" } } }
  },
  {
    name: "launch_marketing_campaign",
    description: "Dispatches organic zero-ad-spend marketing (Pinterest, TikTok, Twitter, Email).",
    parameters: { type: "OBJECT", properties: { productId: { type: "STRING" } } }
  },
  {
    name: "process_live_sale",
    description: "Records a live customer sale with 100% net margin ($0 COGS) into the double-entry ledger.",
    parameters: { type: "OBJECT", properties: { productId: { type: "STRING" } } }
  },
  {
    name: "log_manager_status",
    description: "Records what the AI runner is doing to the system logs.",
    parameters: { type: "OBJECT", properties: { message: { type: "STRING" } } }
  }
];

/**
 * Step 3: Handle the Startup Trigger in Code
 * Pings the model immediately upon app launch with the 'APP_START' payload.
 * ENFORCES ZERO-COST RULE: Products thought up, created, or sent for approval must cost the founder $0.00.
 */
export async function initializeAdminRunner() {
  console.log("⚡ AdminRunner boot: fetching current application state...");
  const currentState = await fetchCurrentAppState();
  
  console.log("📡 Prompting Gemini 2.5 Pro with APP_START initialization payload (Zero-Cost Mandate Active)...");
  
  if (ai) {
    try {
      const response = await ai.models.generateContent({
        model: CORE_MODEL,
        contents: `APP_START: The application has initialized. Current state: ${JSON.stringify(currentState)}. Enforce the absolute zero-cost rule ($0 COGS to founder). Determine requirements and execute the next operational step.`,
      });

      // Check if the AI administrative runner decided to call a function/tool
      if (response.functionCalls && response.functionCalls.length > 0) {
        await executeAIRequestedFunctions(response.functionCalls);
        return response.functionCalls;
      }
    } catch (e) {
      console.warn("Gemini API call failed during AdminRunner init, executing autonomous fallback:", e);
    }
  }

  // Fallback autonomous execution: execute core swarm cycle
  console.log("🚀 Executing autonomous startup cycle with zero-cost enforcement...");
  const queue = await autoStartAICrashLoop();
  return queue;
}

export async function fetchCurrentAppState() {
  return {
    status: "ONLINE",
    uptimeSeconds: Math.floor(process.uptime ? process.uptime() : 0),
    activeAgents: 11,
    productsInCatalog: 0,
    pendingApprovals: 0,
    ledgerBalanceCents: 0,
    zeroCostRuleActive: true,
    nextRequiredAction: "INITIALIZE_POP_UP_PRODUCTS"
  };
}

import { interceptAction } from './godInterceptor.js';

export async function executeAIRequestedFunctions(functionCalls) {
  for (const call of functionCalls) {
    console.log(`🔧 AdminRunner proposing tool: ${call.name} with args:`, call.args);
    
    // The Interceptor Architecture: God intercepts before execution
    const verdict = await interceptAction({
      sourceAgent: "AI Administrator (AdminRunner)",
      actionType: "EXECUTE_TOOL",
      targetResource: `tool/${call.name}`,
      payload: call.args
    });

    if (verdict.status === "HALTED") {
      console.error(`🚨 [EXECUTION BLOCKED BY OVERSEER] Tool '${call.name}' halted! Hazard: ${verdict.hazard_detected}. Remediation: ${verdict.remediation}`);
      continue;
    }

    if (call.name === "trigger_swarm_cycle") {
      await autoStartAICrashLoop();
    }
  }
}

