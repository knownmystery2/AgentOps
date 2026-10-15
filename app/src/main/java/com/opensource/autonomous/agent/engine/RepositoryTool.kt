package com.opensource.autonomous.agent.engine

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File

/**
 * Open-source repository tool integration.
 * Provides safe workspace file reading and standardized Gemini tool schema declarations
 * for the Direct REST API without depending on deprecated client libraries.
 */
object RepositoryTool {
    const val TOOL_NAME = "readRepositoryFile"
    const val TOOL_DESCRIPTION = "Reads the contents of a specific file from the user's repository."

    /**
     * Builds the JSON tool declaration conforming to the Gemini generateContent REST specification.
     */
    fun getToolDeclarationJson(): JsonObject {
        val root = JsonObject()
        val funcDeclarations = JsonArray()

        val func = JsonObject().apply {
            addProperty("name", TOOL_NAME)
            addProperty("description", TOOL_DESCRIPTION)

            val params = JsonObject().apply {
                addProperty("type", "OBJECT")
                val props = JsonObject().apply {
                    val filePath = JsonObject().apply {
                        addProperty("type", "STRING")
                        addProperty("description", "The relative path to the file inside the repository (e.g., 'src/Main.kt').")
                    }
                    add("filePath", filePath)
                }
                add("properties", props)
                val required = JsonArray().apply {
                    add("filePath")
                }
                add("required", required)
            }
            add("parameters", params)
        }

        funcDeclarations.add(func)
        root.add("functionDeclarations", funcDeclarations)
        return root
    }

    /**
     * Formulates a tool response structure for Gemini Function Calling REST interactions.
     */
    fun buildFunctionResponse(name: String, responseContent: String): JsonObject {
        val part = JsonObject()
        val funcResp = JsonObject().apply {
            addProperty("name", name)
            val resp = JsonObject().apply {
                addProperty("content", responseContent)
            }
            add("response", resp)
        }
        part.add("functionResponse", funcResp)
        return part
    }
}

/**
 * Safely reads the contents of a repository file.
 * Prevents directory traversal attacks and handles missing files gracefully.
 */
fun readRepositoryFile(filePath: String): String {
    val cleanPath = filePath.trim().removePrefix("/").removePrefix("./")
    val repoRoot = File(".")
    val targetFile = File(repoRoot, cleanPath)

    return try {
        if (!targetFile.canonicalPath.startsWith(repoRoot.canonicalPath)) {
            "Error: Access denied (path traversal outside workspace)."
        } else if (targetFile.exists() && targetFile.isFile) {
            targetFile.readText()
        } else {
            "Error: File '$filePath' not found in local workspace."
        }
    } catch (e: Exception) {
        "Error reading file '$filePath': ${e.message}"
    }
}
