package com.k11000.monkify.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class GitHubIssueUrlBuilderTest {

    @Test
    fun `buildUrl genera URL valida para reporte de bug con logs`() {
        val params = GitHubIssueUrlBuilder.IssueParams(
            type = GitHubIssueUrlBuilder.IssueType.BUG,
            comment = "La reproduccion se corta al cambiar de cancion",
            appVersion = "0.3.0",
            versionCode = 3,
            deviceModel = "Pixel 7 Pro",
            androidVersion = "Android 14 (API 34)",
            logs = "[ERROR] ExoPlayer error 404"
        )

        val url = GitHubIssueUrlBuilder.buildUrl(params)

        assertTrue(url.startsWith("https://github.com/KEVIN11000/Monkify-Android/issues/new?"))
        assertTrue(url.contains("labels=bug"))

        val decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name())
        assertTrue(decodedUrl.contains("title=[Bug]: La reproduccion se corta al cambiar de cancion"))
        assertTrue(decodedUrl.contains("Versión de Monkify:** 0.3.0 (código 3)"))
        assertTrue(decodedUrl.contains("Dispositivo:** Pixel 7 Pro"))
        assertTrue(decodedUrl.contains("Versión de Android:** Android 14 (API 34)"))
        assertTrue(decodedUrl.contains("<details>"))
        assertTrue(decodedUrl.contains("<summary>Registros Técnicos de Diagnóstico (Logs)</summary>"))
        assertTrue(decodedUrl.contains("[ERROR] ExoPlayer error 404"))
    }

    @Test
    fun `buildUrl genera URL valida para sugerencia sin logs`() {
        val params = GitHubIssueUrlBuilder.IssueParams(
            type = GitHubIssueUrlBuilder.IssueType.FEATURE,
            comment = "Agregar temporizador de apagado automatico",
            appVersion = "0.3.0",
            versionCode = 3,
            deviceModel = "Samsung Galaxy S23",
            androidVersion = "Android 13 (API 33)",
            logs = null
        )

        val url = GitHubIssueUrlBuilder.buildUrl(params)

        assertTrue(url.startsWith("https://github.com/KEVIN11000/Monkify-Android/issues/new?"))
        assertTrue(url.contains("labels=enhancement"))

        val decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name())
        assertTrue(decodedUrl.contains("title=[Sugerencia]: Agregar temporizador de apagado automatico"))
        assertTrue(decodedUrl.contains("Agregar temporizador de apagado automatico"))
        assertFalse(decodedUrl.contains("<details>"))
    }

    @Test
    fun `buildUrl trunca comentarios largos en el titulo sin romper la URL`() {
        val longComment = "A".repeat(100)
        val params = GitHubIssueUrlBuilder.IssueParams(
            type = GitHubIssueUrlBuilder.IssueType.BUG,
            comment = longComment,
            appVersion = "0.3.0",
            versionCode = 3,
            deviceModel = "Generic Device",
            androidVersion = "Android 12",
            logs = ""
        )

        val url = GitHubIssueUrlBuilder.buildUrl(params)
        val decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name())

        assertTrue(decodedUrl.contains("[Bug]: ${"A".repeat(47)}..."))
        assertFalse(decodedUrl.contains("<details>"))
    }
}
