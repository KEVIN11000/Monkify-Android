package com.k11000.monkify.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object GitHubIssueUrlBuilder {
    private const val BASE_URL = "https://github.com/KEVIN11000/Monkify-Android/issues/new"

    enum class IssueType(val label: String, val prefix: String) {
        BUG("bug", "[Bug]: "),
        FEATURE("enhancement", "[Sugerencia]: ")
    }

    data class IssueParams(
        val type: IssueType,
        val comment: String,
        val appVersion: String,
        val versionCode: Int,
        val deviceModel: String,
        val androidVersion: String,
        val logs: String? = null
    )

    fun buildUrl(params: IssueParams): String {
        val trimmedComment = params.comment.trim()
        val titleSummary = if (trimmedComment.length > 50) {
            "${trimmedComment.take(47)}..."
        } else {
            trimmedComment.ifBlank { "Nuevo reporte" }
        }
        val title = "${params.type.prefix}$titleSummary"

        val bodyBuilder = StringBuilder().apply {
            appendLine("### Descripción del Reporte")
            appendLine(trimmedComment.ifBlank { "Sin descripción proporcionada." })
            appendLine()
            appendLine("---")
            appendLine("### Información del Entorno")
            appendLine("- **Versión de Monkify:** ${params.appVersion} (código ${params.versionCode})")
            appendLine("- **Dispositivo:** ${params.deviceModel}")
            appendLine("- **Versión de Android:** ${params.androidVersion}")
            appendLine()
            if (!params.logs.isNullOrBlank()) {
                appendLine("<details>")
                appendLine("<summary>Registros Técnicos de Diagnóstico (Logs)</summary>")
                appendLine()
                appendLine("```text")
                appendLine(params.logs.takeLast(2500).trim())
                appendLine("```")
                appendLine("</details>")
            }
        }

        val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.name())
        val encodedBody = URLEncoder.encode(bodyBuilder.toString(), StandardCharsets.UTF_8.name())
        val encodedLabel = URLEncoder.encode(params.type.label, StandardCharsets.UTF_8.name())

        return "$BASE_URL?title=$encodedTitle&labels=$encodedLabel&body=$encodedBody"
    }
}
