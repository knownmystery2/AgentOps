package com.opensource.autonomous.agent.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.opensource.autonomous.agent.data.AppDatabase
import com.opensource.autonomous.agent.data.LogEntity
import com.opensource.autonomous.agent.data.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

/**
 * Embedded, open-source HTTP callback endpoint listener running directly inside your background process.
 * Routes incoming production support/inquiries straight to Agent 6 (Support CRM).
 */
class LocalWebhookServer(private val port: Int = 8080, private val db: AppDatabase) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

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
            } catch (e: Exception) {
                // Socket closed or port unavailable
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

                    if (requestLine.startsWith("POST") && requestLine.contains("/webhook/support")) {
                        val bodyChars = CharArray(contentLength)
                        if (contentLength > 0) {
                            reader.read(bodyChars, 0, contentLength)
                        }
                        val postData = String(bodyChars)

                        val rootJson = try {
                            gson.fromJson(postData, JsonObject::class.java)
                        } catch (e: Exception) {
                            JsonObject()
                        }

                        val customerEmail = rootJson.get("email")?.asString ?: "customer@example.com"
                        val customerMessage = rootJson.get("message")?.asString ?: "Customer inquiry via local webhook."

                        db.logDao().insertLog(
                            LogEntity(
                                agentId = "6",
                                message = "Incoming Support Webhook captured from: $customerEmail"
                            )
                        )
                        db.taskDao().insertTask(
                            TaskEntity(
                                id = UUID.randomUUID().toString(),
                                assignedToAgentId = "6",
                                title = "Inbound CRM Ticket Recovery",
                                instruction = "Customer Sender: $customerEmail\nInquiry Details: $customerMessage",
                                status = "PENDING",
                                result = null
                            )
                        )

                        val responseBody = """{"status":"routed_to_swarm"}"""
                        writer.write("HTTP/1.1 200 OK\r\n")
                        writer.write("Content-Type: application/json\r\n")
                        writer.write("Content-Length: ${responseBody.toByteArray().size}\r\n")
                        writer.write("Connection: close\r\n\r\n")
                        writer.write(responseBody)
                    } else {
                        val notFound = "Endpoint not mapped."
                        writer.write("HTTP/1.1 404 Not Found\r\n")
                        writer.write("Content-Type: text/plain\r\n")
                        writer.write("Content-Length: ${notFound.toByteArray().size}\r\n")
                        writer.write("Connection: close\r\n\r\n")
                        writer.write(notFound)
                    }
                    writer.flush()
                }
            } catch (e: Exception) {
                // Handle read/write error cleanly
            }
        }
    }
}
