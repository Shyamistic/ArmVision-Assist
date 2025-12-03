package com.example.armvisionassist

import com.google.mlkit.vision.text.Text

object SmartTextProcessor {

    data class AnalysisResult(
        val category: String,
        val summary: String,
        val cleanText: String,
        val riskLevel: Int // 0=Safe, 1=Risk
    )

    private val MEDICAL_KEYWORDS = setOf("mg", "dosage", "pill", "doctor", "pharmacy")
    private val FINANCIAL_KEYWORDS = setOf("total", "amount", "tax", "invoice", "bank")
    private val TECHNICAL_KEYWORDS = setOf("function", "val", "var", "import", "code", "class")
    private val DINING_KEYWORDS = setOf("menu", "starter", "veg", "chicken", "served")

    fun analyze(mlKitText: Text): AnalysisResult {
        val rawText = mlKitText.text.lowercase()
        val words = rawText.split("\\s+".toRegex())

        var medical = 0; var financial = 0; var technical = 0; var dining = 0

        for (word in words) {
            if (word in MEDICAL_KEYWORDS) medical++
            if (word in FINANCIAL_KEYWORDS) financial++
            if (word in TECHNICAL_KEYWORDS) technical++
            if (word in DINING_KEYWORDS) dining++
        }

        var category = "📄 DOCUMENT"
        var summary = "Text Detected"

        val maxScore = maxOf(medical, financial, technical, dining)
        if (maxScore > 0) {
            when (maxScore) {
                medical -> { category = "💊 MEDICAL"; summary = "Medical Info" }
                financial -> { category = "💳 FINANCIAL"; summary = "Financial Doc" }
                technical -> { category = "💻 TECHNICAL"; summary = "Code / Script" }
                dining -> { category = "🍽️ DINING"; summary = "Menu Item" }
            }
        }

        var risk = 0
        if (rawText.contains("danger") || rawText.contains("warning") || rawText.contains("poison")) {
            risk = 1
            category = "⚠️ HAZARD"
            summary = "SAFETY ALERT"
        }

        return AnalysisResult(category, summary, mlKitText.text, risk)
    }
}