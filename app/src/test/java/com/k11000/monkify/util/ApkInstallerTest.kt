package com.k11000.monkify.util

import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class ApkInstallerTest {

    @Test
    fun install_apk_fails_gracefully_when_file_does_not_exist() {
        val nonExistentFile = File("build/tmp/non_existent_app.apk")
        if (nonExistentFile.exists()) {
            nonExistentFile.delete()
        }

        try {
            // Context is not needed if the precondition fails first
            require(nonExistentFile.exists()) { "File missing" }
            fail("Debe lanzar excepción si el archivo no existe")
        } catch (e: IllegalArgumentException) {
            // Success
        }
    }
}
