package io.github.tobi.laa.embedded.valkey

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class ValkeyCleanupExtension : AfterEachCallback {

    override fun afterEach(context: ExtensionContext) {
        context.requiredTestClass.declaredFields.filter {
            Valkey::class.java.isAssignableFrom(it.type)
        }.forEach {
            it.isAccessible = true
            it.get(context.requiredTestInstance)?.let { valkey ->
                stopSafely(valkey as Valkey, removeWorkingDir = true)
            }
        }
    }
}