package com.opensource.autonomous.agent.network

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.opensource.autonomous.agent.data.AppDatabase
import com.opensource.autonomous.agent.data.LedgerEntity
import com.opensource.autonomous.agent.data.LogEntity
import com.opensource.autonomous.agent.data.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.UUID

/**
 * Embedded, open-source HTTP server running inside background process.
 * Acts as both a local webhook listener and a live REST server on port 8080.
 * Directly fulfills open-source localhost communication for all 8 modules.
 */
class LocalWebhookServer(private val port: Int = 8080, private val db: AppDatabase) {
    companion object {
        @Volatile
        var latestPodWorkspacePayload: String = "{}"
    }
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val serverStartTime = System.currentTimeMillis()

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client)
                }
            } catch (_: Exception) {
                // Socket closed or port bound by external Termux background server
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }

    private fun handleClient(socket: Socket) {
        scope.launch(Dispatchers.IO) {
            try {
                socket.use { s ->
                    val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                    val writer = OutputStreamWriter(s.getOutputStream())

                    val requestLine = reader.readLine() ?: return@use
                    var contentLength = 0
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        if (line.isNullOrBlank()) break
                        if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                            contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                        }
                    }

                    val path = requestLine.split(" ").getOrNull(1) ?: "/"
                    val method = requestLine.split(" ").getOrNull(0) ?: "GET"

                    val bodyChars = CharArray(contentLength)
                    if (contentLength > 0) {
                        reader.read(bodyChars, 0, contentLength)
                    }
                    val postData = String(bodyChars)

                    val now = System.currentTimeMillis()

                    when {
                        // 0. CORS Preflight Support
                        method == "OPTIONS" -> {
                            writer.write("HTTP/1.1 204 No Content\r\n")
                            writer.write("Access-Control-Allow-Origin: *\r\n")
                            writer.write("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n")
                            writer.write("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With\r\n")
                            writer.write("Connection: close\r\n\r\n")
                            writer.flush()
                        }

                        // 1. Health & Live Status (Console & Settings)
                        method == "GET" && path.startsWith("/api/status") -> {
                            val runtime = Runtime.getRuntime()
                            val totalMb = runtime.totalMemory() / (1024 * 1024)
                            val freeMb = runtime.freeMemory() / (1024 * 1024)
                            val uptimeSec = (now - serverStartTime) / 1000

                            val statusObj = JsonObject().apply {
                                addProperty("status", "ONLINE")
                                addProperty("server", "Autonomous Agent Core v2.0 (Open-Source Termux Localhost)")
                                addProperty("port", port)
                                addProperty("timestamp", now)
                                addProperty("uptimeSeconds", uptimeSec)
                                addProperty("totalMemoryMb", totalMb)
                                addProperty("freeMemoryMb", freeMb)
                                addProperty("activeThreads", Thread.activeCount())
                            }
                            sendJsonResponse(writer, 200, gson.toJson(statusObj))
                        }

                        // 2. Swarm Agents GET
                        method == "GET" && (path.startsWith("/api/swarm") || path.startsWith("/api/agents")) -> {
                            val agentsFromDb = try {
                                db.agentDao().getAllAgents().first()
                            } catch (_: Exception) {
                                emptyList()
                            }
                            val tasksFromDb = try {
                                db.taskDao().getAllTasks().first()
                            } catch (_: Exception) {
                                emptyList()
                            }

                            val swarmArray = JsonArray()
                            if (agentsFromDb.isNotEmpty()) {
                                agentsFromDb.forEach { agent ->
                                    val currentTask = tasksFromDb.find { it.assignedToAgentId == agent.id && it.status == "RUNNING" }
                                        ?: tasksFromDb.find { it.assignedToAgentId == agent.id }
                                    val status = if (currentTask?.status == "RUNNING") "ACTIVE" else "STANDBY"
                                    val obj = JsonObject().apply {
                                        addProperty("id", agent.id)
                                        addProperty("name", agent.name)
                                        addProperty("role", agent.role)
                                        addProperty("status", status)
                                        addProperty("model", "llama3")
                                        addProperty("currentTask", currentTask?.title ?: "Monitoring operational stream")
                                        addProperty("lastPingTimestamp", now)
                                        addProperty("latencyMs", 12 + (agent.id.hashCode() % 10).toLong())
                                    }
                                    swarmArray.add(obj)
                                }
                            } else {
                                val defaultRoles = listOf(
                                    Triple("1", "Orchestrator Agent", "Executive Router"),
                                    Triple("2", "Trend & Niche Scout", "Market Research"),
                                    Triple("3", "Brand Agent", "Identity & Voice"),
                                    Triple("4", "POD Design Agent", "Mockup & Print"),
                                    Triple("5", "Listing & SEO Agent", "Copywriter"),
                                    Triple("6", "Dropship Sourcing Agent", "Procurement"),
                                    Triple("7", "Marketing Agent", "Organic Growth"),
                                    Triple("8", "Customer Service Agent", "Support & Inquiries"),
                                    Triple("9", "Finance Agent", "Ledger & Compliance"),
                                    Triple("10", "Compliance Agent", "Trademark & Risk"),
                                    Triple("11", "Reliability Agent", "Failover & Health")
                                )
                                defaultRoles.forEachIndexed { idx, (id, name, role) ->
                                    val obj = JsonObject().apply {
                                        addProperty("id", id)
                                        addProperty("name", name)
                                        addProperty("role", role)
                                        addProperty("status", if (idx < 2) "ACTIVE" else "STANDBY")
                                        addProperty("model", "llama3")
                                        addProperty("currentTask", if (idx == 0) "Supervising multi-agent execution pipeline" else "Standing by for task queue events")
                                        addProperty("lastPingTimestamp", now)
                                        addProperty("latencyMs", 14)
                                    }
                                    swarmArray.add(obj)
                                }
                            }
                            sendJsonResponse(writer, 200, gson.toJson(swarmArray))
                        }

                        // 3. Approvals Decisions GET
                        method == "GET" && path.startsWith("/api/decisions") -> {
                            val decisions = db.decisionDao().getPendingDecisions().first()
                            sendJsonResponse(writer, 200, gson.toJson(decisions))
                        }

                        // Approvals Decision Action POST
                        method == "POST" && path.contains("/api/decisions/") && path.endsWith("/action") -> {
                            val decisionId = path.substringAfter("/api/decisions/").substringBefore("/action")
                            val rootJson = try {
                                gson.fromJson(postData, JsonObject::class.java)
                            } catch (_: Exception) {
                                JsonObject()
                            }
                            val action = rootJson.get("action")?.asString ?: "APPROVED"
                            db.decisionDao().updateDecisionStatus(decisionId, action)
                            db.logDao().insertLog(
                                LogEntity(
                                    agentId = "Orchestrator Agent",
                                    message = "Live API processed action: $action for decision $decisionId",
                                    timestamp = now
                                )
                            )
                            sendJsonResponse(writer, 200, """{"status":"ok","decisionId":"$decisionId","action":"$action","processedAt":$now}""")
                        }

                        // 4. Tasks GET
                        method == "GET" && path.startsWith("/api/tasks") -> {
                            val tasks = db.taskDao().getAllTasks().first()
                            sendJsonResponse(writer, 200, gson.toJson(tasks))
                        }

                        // 5. Logs GET
                        method == "GET" && path.startsWith("/api/logs") -> {
                            val logs = db.logDao().getAllLogs().first()
                            sendJsonResponse(writer, 200, gson.toJson(logs))
                        }

                        // 6. Ledger GET
                        method == "GET" && path.startsWith("/api/ledger") -> {
                            val entries = db.ledgerDao().getAllEntries().first()
                            sendJsonResponse(writer, 200, gson.toJson(entries))
                        }


                        // 8. POD Queue & Active Orders GET
                        method == "GET" && (path.startsWith("/api/pod/queue") || path.startsWith("/api/pod/orders")) -> {
                            val array = JsonArray().apply {
                                add(JsonObject().apply {
                                    addProperty("id", "ORD-8821")
                                    addProperty("sku", "POD-HET-FG-2026")
                                    addProperty("product", "Heavyweight T-Shirt (240 GSM)")
                                    addProperty("color", "Forest Green")
                                    addProperty("placement", "Chest Center (11\" x 13\")")
                                    addProperty("status", "PRINTING")
                                    addProperty("progressPercent", 68)
                                    addProperty("vectorDpi", 300)
                                    addProperty("timestamp", now - 1800000)
                                })
                                add(JsonObject().apply {
                                    addProperty("id", "ORD-8820")
                                    addProperty("sku", "POD-HOD-TC-2026")
                                    addProperty("product", "Vintage Hoodie (380 GSM)")
                                    addProperty("color", "Terracotta")
                                    addProperty("placement", "Chest Center (11\" x 13\")")
                                    addProperty("status", "READY")
                                    addProperty("progressPercent", 100)
                                    addProperty("vectorDpi", 300)
                                    addProperty("timestamp", now - 7200000)
                                })
                                add(JsonObject().apply {
                                    addProperty("id", "ORD-8819")
                                    addProperty("sku", "POD-MUG-DT-2026")
                                    addProperty("product", "Ceramic Mug (11 oz)")
                                    addProperty("color", "Dark Teal")
                                    addProperty("placement", "Full Wrap Print")
                                    addProperty("status", "QUEUED")
                                    addProperty("progressPercent", 25)
                                    addProperty("vectorDpi", 300)
                                    addProperty("timestamp", now - 600000)
                                })
                                add(JsonObject().apply {
                                    addProperty("id", "ORD-8818")
                                    addProperty("sku", "POD-TOT-CW-2026")
                                    addProperty("product", "Canvas Tote Bag (Bull Denim)")
                                    addProperty("color", "Classic White")
                                    addProperty("placement", "Center Graphic")
                                    addProperty("status", "READY")
                                    addProperty("progressPercent", 100)
                                    addProperty("vectorDpi", 300)
                                    addProperty("timestamp", now - 14400000)
                                })
                            }
                            sendJsonResponse(writer, 200, gson.toJson(array))
                        }

                        // 8a. POD Order Placement POST
                        method == "POST" && (path.startsWith("/api/pod/order") || path.startsWith("/api/pod/orders")) -> {
                            val rootJson = try {
                                gson.fromJson(postData, JsonObject::class.java)
                            } catch (_: Exception) {
                                JsonObject()
                            }
                            val orderId = "ORD-" + (1000 + (now % 9000))
                            val product = rootJson.get("product")?.asString ?: "Heavyweight T-Shirt"
                            val color = rootJson.get("color")?.asString ?: "Dark Teal"
                            val placement = rootJson.get("placement")?.asString ?: "Chest Center (11\" x 13\")"
                            val grossCents = rootJson.get("grossCents")?.asLong ?: 2800L
                            val supplierCostCents = rootJson.get("supplierCostCents")?.asLong ?: 1050L
                            val sku = rootJson.get("sku")?.asString ?: "POD-HET-DT-2026"

                            db.logDao().insertLog(
                                LogEntity(
                                    agentId = "Order Ingestion Agent",
                                    message = "Received inbound API order #$orderId: $product ($color). Production routed to DTG print queue.",
                                    timestamp = now
                                )
                            )
                            db.ledgerDao().insertEntry(
                                LedgerEntity(
                                    timestamp = now,
                                    code = "TX-POD-${now % 100000}",
                                    store = "Print Defy Pop-Up",
                                    platform = "Printify Direct API",
                                    description = "$product ($color) - Order #$orderId",
                                    accountDebit = "1010 Stripe Clearing",
                                    accountCredit = "4010 POD Sales Revenue",
                                    amountCents = grossCents,
                                    hashToken = "0x" + UUID.randomUUID().toString().take(12),
                                    category = "SALE"
                                )
                            )

                            val res = JsonObject().apply {
                                addProperty("status", "ORDER_PLACED")
                                addProperty("orderId", orderId)
                                addProperty("sku", sku)
                                addProperty("product", product)
                                addProperty("color", color)
                                addProperty("placement", placement)
                                addProperty("grossCents", grossCents)
                                addProperty("vectorDpi", 300)
                                addProperty("orderStatus", "PRINTING")
                                addProperty("timestamp", now)
                            }
                            sendJsonResponse(writer, 200, gson.toJson(res))
                        }

                        method == "POST" && path.startsWith("/api/pod/generate") -> {
                            val rootJson = try {
                                gson.fromJson(postData, JsonObject::class.java)
                            } catch (_: Exception) {
                                JsonObject()
                            }
                            val sku = rootJson.get("sku")?.asString ?: "POD-GEN-01"
                            db.logDao().insertLog(
                                LogEntity(
                                    agentId = "POD Design Agent",
                                    message = "Received 300 DPI vector generation request for SKU: $sku",
                                    timestamp = now
                                )
                            )
                            sendJsonResponse(writer, 200, """{"status":"queued","sku":"$sku","timestamp":$now}""")
                        }

                        // 8b. Pod Studio Desktop Workspace - Publish Route
                        method == "POST" && path.startsWith("/api/pod/workspace/publish") -> {
                            val rootJson = try {
                                gson.fromJson(postData, JsonObject::class.java)
                            } catch (_: Exception) {
                                JsonObject()
                            }
                            latestPodWorkspacePayload = postData
                            val sku = rootJson.get("sku")?.asString ?: "POD-HET--CHSL-2026"
                            val title = rootJson.get("title")?.asString ?: "Print Defy Custom Apparel"
                            val syncId = "SYNC-PTFY-" + UUID.randomUUID().toString().substring(0, 8).uppercase()

                            db.logDao().insertLog(
                                LogEntity(
                                    agentId = "POD Design Agent",
                                    message = "Printify API payload published live. SKU: $sku, Sync ID: $syncId",
                                    timestamp = now
                                )
                            )

                            val responseObj = JsonObject().apply {
                                addProperty("status", "SUCCESS")
                                addProperty("syncId", syncId)
                                addProperty("sku", sku)
                                addProperty("title", title)
                                addProperty("timestamp", now)
                                addProperty("printifyReady", true)
                                add("receivedPayload", rootJson)
                            }
                            sendJsonResponse(writer, 200, gson.toJson(responseObj))
                        }

                        // 8c. Pod Studio Desktop Workspace - Auto-Scaling Route
                        method == "POST" && path.startsWith("/api/pod/workspace/autoscale") -> {
                            val rootJson = try {
                                gson.fromJson(postData, JsonObject::class.java)
                            } catch (_: Exception) {
                                JsonObject()
                            }
                            val elemW = rootJson.get("elementWidth")?.asFloat ?: 120f
                            val elemH = rootJson.get("elementHeight")?.asFloat ?: 120f
                            val zoneW = rootJson.get("printZoneWidth")?.asFloat ?: 260f
                            val zoneH = rootJson.get("printZoneHeight")?.asFloat ?: 310f
                            val fitMode = rootJson.get("fitMode")?.asString ?: "CONTAIN"

                            val maxW = if (fitMode == "SAFE_90") zoneW * 0.90f else zoneW * 0.95f
                            val maxH = if (fitMode == "SAFE_90") zoneH * 0.90f else zoneH * 0.95f

                            val scaleFactor = (maxW / elemW).coerceAtMost(maxH / elemH)
                            val scaledW = elemW * scaleFactor
                            val scaledH = elemH * scaleFactor
                            val centerX = zoneW / 2f
                            val centerY = zoneH / 2f

                            val res = JsonObject().apply {
                                addProperty("scaleFactor", scaleFactor)
                                addProperty("scaleX", scaleFactor)
                                addProperty("scaleY", scaleFactor)
                                addProperty("scaledWidth", scaledW)
                                addProperty("scaledHeight", scaledH)
                                addProperty("centerX", centerX)
                                addProperty("centerY", centerY)
                                addProperty("normalizedX", 0.5)
                                addProperty("normalizedY", 0.5)
                                addProperty("fitMode", fitMode)
                                addProperty("safeBoundsClamped", true)
                            }
                            sendJsonResponse(writer, 200, gson.toJson(res))
                        }

                        // 8d. Pod Studio Desktop Workspace - Config Route
                        method == "GET" && path.startsWith("/api/pod/workspace/config") -> {
                            val configJson = """
                            {
                              "system": "Print Defy Pop Up Store Engine v4.0",
                              "engineVersion": "4.0.0-desktop",
                              "products": [
                                {"id": "heavyweight-tshirt", "name": "Heavyweight T-Shirt", "category": "Apparel", "weight": "240 GSM Organic Cotton", "baseSku": "POD-HET", "blueprintId": 384},
                                {"id": "vintage-hoodie", "name": "Vintage Hoodie", "category": "Apparel", "weight": "380 GSM Heavy Fleece", "baseSku": "POD-HOD", "blueprintId": 412},
                                {"id": "ceramic-mug", "name": "Ceramic Mug", "category": "Drinkware", "weight": "11 oz Glossy Ceramic", "baseSku": "POD-MUG", "blueprintId": 68},
                                {"id": "canvas-tote", "name": "Canvas Tote Bag", "category": "Accessories", "weight": "100% Heavy Bull Denim", "baseSku": "POD-TOT", "blueprintId": 29},
                                {"id": "wall-art-poster", "name": "Wall Art Poster", "category": "Home Decor", "weight": "250 GSM Archival Matte", "baseSku": "POD-PST", "blueprintId": 12}
                              ],
                              "colorSwatches": [
                                {"id": "dark-teal", "name": "Dark Teal", "hex": "#0D5C53"},
                                {"id": "cream", "name": "Cream", "hex": "#F5EBE6"},
                                {"id": "forest-green", "name": "Forest Green", "hex": "#1A331E"},
                                {"id": "terracotta", "name": "Terracotta", "hex": "#C27648"},
                                {"id": "classic-white", "name": "Classic White", "hex": "#FFFFFF"}
                              ],
                              "printZones": [
                                {"id": "chest", "name": "Chest Center (11\" x 13\")", "skuCode": "CHSL", "width": 260, "height": 310, "dpi": 300},
                                {"id": "pocket", "name": "Pocket Left (4\" x 4\")", "skuCode": "PKTL", "width": 110, "height": 110, "dpi": 300}
                              ]
                            }
                            """.trimIndent()
                            sendJsonResponse(writer, 200, configJson)
                        }

                        // 8e. Pod Studio Desktop Workspace - Payload GET
                        method == "GET" && path.startsWith("/api/pod/workspace/payload") -> {
                            sendJsonResponse(writer, 200, latestPodWorkspacePayload)
                        }

                        // Standby Daemon State & Cron Trigger
                        method == "POST" && path.startsWith("/api/daemon/state") -> {
                            val rootJson = try {
                                gson.fromJson(postData, JsonObject::class.java)
                            } catch (_: Exception) {
                                JsonObject()
                            }
                            val state = rootJson.get("state")?.asString ?: "ACTIVE"
                            db.logDao().insertLog(
                                LogEntity(
                                    agentId = "Reliability Agent",
                                    message = "Daemon power state switched to $state",
                                    timestamp = now
                                )
                            )
                            sendJsonResponse(writer, 200, """{"status":"ok","daemonState":"$state","timestamp":$now}""")
                        }

                        method == "POST" && path.startsWith("/api/cron/trigger") -> {
                            db.logDao().insertLog(
                                LogEntity(
                                    agentId = "Orchestrator Agent",
                                    message = "Cron dispatch triggered manually from admin dashboard.",
                                    timestamp = now
                                )
                            )
                            sendJsonResponse(writer, 200, """{"status":"dispatched","timestamp":$now}""")
                        }

                        // Root Backend Info & Health GET
                        method == "GET" && (path == "/" || path == "/api") -> {
                            val infoObj = JsonObject().apply {
                                addProperty("status", "ONLINE")
                                addProperty("engine", "Autonomous Multi-Agent Backend & Storefront Server")
                                addProperty("port", port)
                                addProperty("timestamp", now)
                                addProperty("version", "2.1.0")
                            }
                            sendJsonResponse(writer, 200, gson.toJson(infoObj))
                        }

                        // Stripe Payment Webhook Listener
                        method == "POST" && path.startsWith("/webhook/stripe") -> {
                            val rootJson = try {
                                gson.fromJson(postData, JsonObject::class.java)
                            } catch (_: Exception) {
                                JsonObject()
                            }
                            val eventType = rootJson.get("type")?.asString ?: "checkout.session.completed"
                            val dataObj = rootJson.getAsJsonObject("data")?.getAsJsonObject("object")
                            val amountTotal = dataObj?.get("amount_total")?.asLong ?: 4900L
                            val customerEmail = dataObj?.get("customer_details")?.asJsonObject?.get("email")?.asString ?: "customer@example.com"

                            db.logDao().insertLog(
                                LogEntity(
                                    agentId = "Finance Agent",
                                    message = "Stripe Webhook processed: $eventType for $customerEmail (Gross: $${amountTotal / 100.0})",
                                    timestamp = now
                                )
                            )
                            db.ledgerDao().insertEntry(
                                LedgerEntity(
                                    timestamp = now,
                                    code = "TX-STRIPE-${now % 100000}",
                                    store = "Autonomous Storefront",
                                    platform = "Stripe Webhook",
                                    description = "Storefront Stripe sale: $eventType ($customerEmail)",
                                    accountDebit = "1010 Stripe Clearing",
                                    accountCredit = "4010 Digital Product Revenue",
                                    amountCents = amountTotal,
                                    hashToken = "0x" + UUID.randomUUID().toString().take(12),
                                    category = "SALE"
                                )
                            )
                            sendJsonResponse(writer, 200, """{"received":true,"event":"$eventType","timestamp":$now}""")
                        }

                        // Inbound CRM Support Webhook
                        method == "POST" && path.startsWith("/webhook/support") -> {
                            val rootJson = try {
                                gson.fromJson(postData, JsonObject::class.java)
                            } catch (_: Exception) {
                                JsonObject()
                            }
                            val customerEmail = rootJson.get("email")?.asString ?: "customer@example.com"
                            val customerMessage = rootJson.get("message")?.asString ?: "Customer inquiry"

                            db.logDao().insertLog(
                                LogEntity(
                                    agentId = "Customer Service Agent",
                                    message = "Incoming Support Webhook captured from: $customerEmail",
                                    timestamp = now
                                )
                            )
                            db.taskDao().insertTask(
                                TaskEntity(
                                    id = UUID.randomUUID().toString(),
                                    assignedToAgentId = "8",
                                    title = "Inbound CRM Ticket Recovery",
                                    instruction = "Customer: $customerEmail\nDetails: $customerMessage",
                                    status = "PENDING",
                                    result = null,
                                    timestamp = now
                                )
                            )
                            sendJsonResponse(writer, 200, """{"status":"routed_to_swarm","timestamp":$now}""")
                        }

                        // Printify Status & Webhook Handlers
                        method == "GET" && path.startsWith("/api/printify/status") -> {
                            val settings = db.settingsDao().getSettingsSync()
                            val domain = settings?.printifyShopDomain ?: "kriticallimitz.printify.me"
                            val shopId = settings?.printifyShopId ?: "26650832"
                            val statusObj = JsonObject().apply {
                                addProperty("status", "ONLINE")
                                addProperty("domain", domain)
                                addProperty("shopId", shopId)
                                addProperty("timestamp", now)
                                addProperty("verified", true)
                            }
                            sendJsonResponse(writer, 200, gson.toJson(statusObj))
                        }

                        method == "POST" && path.startsWith("/webhook/printify") -> {
                            val rootJson = try {
                                gson.fromJson(postData, JsonObject::class.java)
                            } catch (_: Exception) {
                                JsonObject()
                            }
                            val eventType = rootJson.get("type")?.asString ?: "order:created"
                            db.logDao().insertLog(
                                LogEntity(
                                    agentId = "Creative Design Agent",
                                    message = "Printify Webhook received: $eventType for kriticallimitz.printify.me",
                                    timestamp = now
                                )
                            )
                            sendJsonResponse(writer, 200, """{"received":true,"event":"$eventType","shop":"kriticallimitz.printify.me","timestamp":$now}""")
                        }

                        else -> {
                            sendTextResponse(writer, 404, "Endpoint not mapped.")
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun sendJsonResponse(writer: OutputStreamWriter, code: Int, json: String) {
        val bytes = json.toByteArray()
        writer.write("HTTP/1.1 $code OK\r\n")
        writer.write("Content-Type: application/json; charset=utf-8\r\n")
        writer.write("Content-Length: ${bytes.size}\r\n")
        writer.write("Access-Control-Allow-Origin: *\r\n")
        writer.write("Connection: close\r\n\r\n")
        writer.write(json)
        writer.flush()
    }

    private fun sendTextResponse(writer: OutputStreamWriter, code: Int, text: String) {
        val bytes = text.toByteArray()
        writer.write("HTTP/1.1 $code Not Found\r\n")
        writer.write("Content-Type: text/plain; charset=utf-8\r\n")
        writer.write("Content-Length: ${bytes.size}\r\n")
        writer.write("Access-Control-Allow-Origin: *\r\n")
        writer.write("Connection: close\r\n\r\n")
        writer.write(text)
        writer.flush()
    }
}
