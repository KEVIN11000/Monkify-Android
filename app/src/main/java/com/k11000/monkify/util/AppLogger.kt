package com.k11000.monkify.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

data class LogEntry(
    val timestamp: String,
    val level: String,
    val tag: String,
    val message: String,
    val throwable: String? = null
) {
    override fun toString(): String {
        val err = if (throwable != null) "\n$throwable" else ""
        return "[$timestamp] [$level/$tag] $message$err"
    }
}

object AppLogger {
    private const val MAX_ENTRIES = 400
    private val buffer = ConcurrentLinkedDeque<LogEntry>()
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    private var logFile: File? = null
    private val dateFormatLock = Any()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val tokenParamRegex = Regex("([?&]token=)[^&\\s#\"']+", RegexOption.IGNORE_CASE)
    private val bearerRegex = Regex("(Bearer\\s+)[A-Za-z0-9._\\-~+/=]+", RegexOption.IGNORE_CASE)
    private val headerTokenRegex = Regex("(X-Monkify-Token\\s*[:=]\\s*)[^\\s,;\"']+", RegexOption.IGNORE_CASE)

    fun sanitizeSensitiveText(raw: String): String {
        if (raw.isEmpty()) return raw
        return raw
            .replace(tokenParamRegex, "$1[REDACTED]")
            .replace(bearerRegex, "$1[REDACTED]")
            .replace(headerTokenRegex, "$1[REDACTED]")
    }

    private fun formatNow(): String = synchronized(dateFormatLock) {
        dateFormat.format(Date())
    }

    fun init(context: Context) {
        ioScope.launch {
            val logDir = File(context.filesDir, "logs").apply { if (!exists()) mkdirs() }
            logFile = File(logDir, "monkify.log")
            i("AppLogger", "Sistema de diagnóstico inicializado correctamente")
        }
    }

    fun d(tag: String, message: String) = log("DEBUG", tag, message, null)
    fun i(tag: String, message: String) = log("INFO", tag, message, null)
    fun w(tag: String, message: String, tr: Throwable? = null) = log("WARN", tag, message, tr)
    fun e(tag: String, message: String, tr: Throwable? = null) = log("ERROR", tag, message, tr)

    private fun log(level: String, tag: String, message: String, tr: Throwable?) {
        val time = formatNow()
        val sanitizedMessage = sanitizeSensitiveText(message)
        val throwableStr = tr?.let { sanitizeSensitiveText(it.stackTraceToString()) }
        val entry = LogEntry(time, level, tag, sanitizedMessage, throwableStr)

        when (level) {
            "DEBUG" -> Log.d(tag, sanitizedMessage)
            "INFO" -> Log.i(tag, sanitizedMessage)
            "WARN" -> Log.w(tag, sanitizedMessage)
            "ERROR" -> Log.e(tag, sanitizedMessage)
        }

        buffer.add(entry)
        while (buffer.size > MAX_ENTRIES) {
            buffer.pollFirst()
        }
        _logsFlow.value = buffer.toList()

        // Escritura asíncrona en Dispatchers.IO fuera del Main Thread
        val targetFile = logFile
        if (targetFile != null) {
            val line = entry.toString() + "\n"
            ioScope.launch {
                try {
                    synchronized(dateFormatLock) {
                        if (targetFile.exists() && targetFile.length() > 512 * 1024) {
                            targetFile.delete()
                        }
                        targetFile.appendText(line)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun getLogsAsText(): String {
        return sanitizeSensitiveText(buffer.joinToString("\n") { it.toString() })
    }

    fun clear() {
        buffer.clear()
        _logsFlow.value = emptyList()
        ioScope.launch {
            try {
                logFile?.delete()
            } catch (_: Exception) {}
        }
    }
}
