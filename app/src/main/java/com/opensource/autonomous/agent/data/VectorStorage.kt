package com.opensource.autonomous.agent.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import kotlin.math.sqrt

/**
 * Lightweight local mathematical vector utility for similarity comparison.
 * Allows the Support and CRM agents to evaluate incoming inquiries against 
 * stored business rules or context files without external service tracking.
 */
object VectorMath {
    fun cosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        if (vectorA.size != vectorB.size) return 0.0f
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }
        return if (normA == 0.0f || normB == 0.0f) 0.0f else (dotProduct / (sqrt(normA.toDouble()) * sqrt(normB.toDouble()))).toFloat()
    }
}

@Entity(tableName = "knowledge_base")
data class KnowledgeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val textContent: String,
    val vectorMetadataJson: String // Serialized FloatArray representation of content embeddings
)
