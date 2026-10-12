package com.opensource.autonomous.agent.engine

import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.FunctionDeclaration
import com.google.ai.client.generativeai.type.FunctionResponsePart
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.content
import org.json.JSONObject
import java.io.File

// Typealias for the official GenerativeModel SDK class
typealias GeminiModel = com.google.ai.client.generativeai.GenerativeModel

// =========================================================================
// DSL Schema & Tool Definition Support
// =========================================================================
enum class Type {
    OBJECT,
    STRING,
    INTEGER,
    NUMBER,
    BOOLEAN,
    ARRAY
}

class PropertyBuilder(val name: String, val type: Type) {
    var description: String = ""
}

class SchemaBuilder(val parent: FunctionDeclarationBuilder) {
    fun property(name: String, type: Type, block: PropertyBuilder.() -> Unit = {}) {
        val prop = PropertyBuilder(name, type).apply(block)
        parent.parametersList.add(Schema.str(name, prop.description))
    }

    fun required(paramName: String) {
        parent.requiredList.add(paramName)
    }
}

class ParametersBuilder(val parent: FunctionDeclarationBuilder) {
    fun schema(type: Type, block: SchemaBuilder.() -> Unit) {
        SchemaBuilder(parent).apply(block)
    }
}

class FunctionDeclarationBuilder(val name: String) {
    var description: String = ""
    val parametersList = mutableListOf<Schema<*>>()
    val requiredList = mutableListOf<String>()

    fun parameters(block: ParametersBuilder.() -> Unit) {
        ParametersBuilder(this).apply(block)
    }

    fun build(): FunctionDeclaration {
        return FunctionDeclaration(
            name = name,
            description = description,
            parameters = parametersList,
            requiredParameters = requiredList
        )
    }
}

class ToolBuilder {
    private val functionDeclarations = mutableListOf<FunctionDeclaration>()

    fun function(name: String, block: FunctionDeclarationBuilder.() -> Unit) {
        val builder = FunctionDeclarationBuilder(name).apply(block)
        functionDeclarations.add(builder.build())
    }

    fun build(): Tool = Tool(functionDeclarations)
}

fun tool(block: ToolBuilder.() -> Unit): Tool {
    return ToolBuilder().apply(block).build()
}

// FunctionResponse helper matching the user-specified GenerativeModel.FunctionResponse syntax
object GenerativeModel {
    fun FunctionResponse(name: String, response: Map<String, Any?>): Content {
        return content("function") {
            part(FunctionResponsePart(name, JSONObject(response)))
        }
    }
}

// =========================================================================
// 1. Define the tool structure so Gemini knows it exists
// =========================================================================
val repoTool = tool {
    function(name = "readRepositoryFile") {
        description = "Reads the contents of a specific file from the user's repository."
        parameters {
            schema(type = Type.OBJECT) {
                property("filePath", Type.STRING) {
                    description = "The relative path to the file inside the repository (e.g., 'src/main.kt')."
                }
                required("filePath")
            }
        }
    }
}

// =========================================================================
// 2. Write the actual Kotlin code that executes when Gemini asks for it
// =========================================================================
fun readRepositoryFile(filePath: String): String {
    val repoRoot = File("/path/to/your/local/repositories") // Target your repo folder
    val targetFile = File(repoRoot, filePath)

    // Safety check: Prevent the AI from escaping your repository folder
    if (!targetFile.canonicalPath.startsWith(repoRoot.canonicalPath)) {
        return "Error: Access denied."
    }

    return if (targetFile.exists()) {
        targetFile.readText()
    } else {
        "Error: File not found."
    }
}

/**
 * 2. Pass the Tool to your Gemini Model
 *
 * When you initialize your model in Kotlin, simply pass the repoTool into the configuration.
 */
fun createRepositoryGeminiModel(apiKey: String, modelName: String = "gemini-2.5-flash"): GeminiModel {
    return GeminiModel(
        modelName = modelName,
        apiKey = apiKey,
        tools = listOf(repoTool)
    )
}

/**
 * 3. Handle the Agent's Request
 *
 * When you send a prompt like "Look at my main.kt file and fix any bugs," Gemini won't just answer;
 * it will return a FunctionCall. You catch that call, run your Kotlin function, and send the code back to Gemini.
 */
suspend fun handleAgentRepositoryRequest(chat: Chat, prompt: String = "Check my src/Main.kt file for errors."): String? {
    val response = chat.sendMessage(prompt)

    // Check if Gemini wants to use your repository tool
    val functionCall = response.functionCalls.firstOrNull()
    if (functionCall != null && functionCall.name == "readRepositoryFile") {
        val filePath = (functionCall.args["filePath"] ?: "") as String

        // Read the actual code from your hard drive
        val fileContent = readRepositoryFile(filePath)

        // Send the code back to Gemini so it can finish its thought
        val finalResponse = chat.sendMessage(
            GenerativeModel.FunctionResponse(
                name = "readRepositoryFile",
                response = mapOf("content" to fileContent)
            )
        )
        println(finalResponse.text)
        return finalResponse.text
    }

    return response.text
}
