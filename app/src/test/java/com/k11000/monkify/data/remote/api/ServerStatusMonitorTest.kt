package com.k11000.monkify.data.remote.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class ServerStatusMonitorTest {

    @Test
    fun `flujo normal saludable permanece en Idle sin saturar de avisos flotantes`() {
        val monitor = ServerStatusMonitor()
        assertEquals(GlobalServerStatus.Idle, monitor.status.value)

        // Petición rutinaria en background (paginación / carga normal)
        monitor.reportConnecting("Sincronizando catálogo...", isExplicit = false)
        assertEquals(GlobalServerStatus.Idle, monitor.status.value)

        // Respuesta 200 OK en flujo normal -> sigue en Idle (0 spam visual)
        monitor.reportConnected("Conectado al servidor")
        assertEquals(GlobalServerStatus.Idle, monitor.status.value)
    }

    @Test
    fun `ante fallo 401 muestra aviso Unauthorized y al recuperar muestra Connected`() {
        val monitor = ServerStatusMonitor()

        monitor.reportUnauthorized("Error 401: Clave inválida")
        assertTrue(monitor.status.value is GlobalServerStatus.Unauthorized)

        // Cuando el usuario corrige el token y la siguiente llamada tiene éxito, confirma la recuperación
        monitor.reportConnected("Conectado al servidor Monkify")
        assertTrue(monitor.status.value is GlobalServerStatus.Connected)
    }

    @Test
    fun `si el usuario descarta aviso de modo offline no vuelve a spamear errores de red en background`() {
        val monitor = ServerStatusMonitor()

        monitor.reportException("Sincronización", UnknownHostException("Unable to resolve host"))
        assertTrue(monitor.status.value is GlobalServerStatus.Error)

        // El usuario cierra el banner con 'X' porque sabe que está sin internet
        monitor.dismiss(byUser = true)
        assertEquals(GlobalServerStatus.Idle, monitor.status.value)

        // Otra petición automática en background falla por red -> no vuelve a interrumpir
        monitor.reportException("Carga de álbumes", UnknownHostException("Offline"), isExplicit = false)
        assertEquals(GlobalServerStatus.Idle, monitor.status.value)

        // Si el usuario pulsa explícitamente "Reintentar", sí muestra el estado
        monitor.reportConnecting("Reintentando conexión...", isExplicit = true)
        assertTrue(monitor.status.value is GlobalServerStatus.Connecting)
    }
}
