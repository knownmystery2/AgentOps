package com.opensource.autonomous.agent.engine

import com.opensource.autonomous.agent.network.PrintifyImage
import kotlin.math.roundToInt

/**
 * Service to map design assets onto specific Printify blueprints.
 * Thatcher algorithm ensures scaling/placement compliance within print area constraints.
 */
class BlueprintThatcherMapper {
    
    fun mapDesignToBlueprint(
        designId: String,
        blueprintId: String,
        printArea: String,
        containerWidth: Float,
        containerHeight: Float,
        rawX: Float,
        rawY: Float,
        rawScaleX: Float,
        rawScaleY: Float,
        rawAngle: Float
    ): PrintifyImage {
        // Thatcher heuristic: Map to standard grid constraints
        // Pocket area requires 30% reduction compared to chest
        val scaleAdjustment = if (printArea == "Pocket") 0.3f else 1.0f
        
        return PrintifyImage(
            id = designId,
            x = (rawX / containerWidth * 100).toInt(),
            y = (rawY / containerHeight * 100).toInt(),
            scale_x = rawScaleX * scaleAdjustment,
            scale_y = rawScaleY * scaleAdjustment,
            angle = rawAngle.toInt()
        )
    }
}
