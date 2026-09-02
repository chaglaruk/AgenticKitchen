package com.agentickitchen.shared.agents

import com.agentickitchen.shared.models.PantryIntelReport

interface PantryIntelAgent {
    fun analyze(
        ingredients: List<String>,
        equipment: Set<String>,
        dietType: String = "none"
    ): PantryIntelReport
}
