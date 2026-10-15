package com.k11000.monkify.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun init(context: Context) {
        val logDir = File(context.filesDir, "logs").apply { if (!exists()) mkdirs() }
        logFile = File(logDir, "monkify.log")
        i("AppLogger", "Sistema de diagnóstico inicializado correctamente")
    }

    fun d(tag: String, message: String) = log("DEBUG", tag, message, null)
    fun i(tag: String, message: String) = log("INFO", tag, message, null)
    fun w(tag: String, message: String, tr: Throwable? = null) = log("WARN", tag, message, tr)
    fun e(tag: String, message: String, tr: Throwable? = null) = log("ERROR", tag, message, tr)

    private fun log(level: String, tag: String, message: String, tr: Throwable?) {
        val time = dateFormat.format(Date())
        val throwableStr = tr?.let { Log.getStackTraceString(it) }
        val entry = LogEntry(time, level, tag, message, throwableStr)

        when (level) {
            "DEBUG" -> Log.d(tag, message, tr)
            "INFO" -> Log.i(tag, message, tr)
            "WARN" -> Log.w(tag, message, tr)
            "ERROR" -> Log.e(tag, message, tr)
        }

        buffer.add(entry)
        while (buffer.size > MAX_ENTRIES) {
            buffer.pollFirst()
        }
        _logsFlow.value = buffer.toList()

        // Escritura asíncrona segura en archivo local
        logFile?.let { file ->
            try {
                if (file.length() > 512 * 1024) {
                    file.delete()
                }
                file.appendText(entry.toString() + "\n")
            } catch (_: Exception) {}
        }
    }

    fun getLogsAsText(): String {
        return buffer.joinToString("\n") { it.toString() }
    }

    fun clear() {
        buffer.clear()
        _logsFlow.value = emptyList()
        try {
            logFile?.delete()
        } catch (_: Exception) {}
    }
}
