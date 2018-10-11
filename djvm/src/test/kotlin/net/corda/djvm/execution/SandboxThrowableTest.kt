package net.corda.djvm.execution

import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.*
import org.junit.Test
import java.util.function.Function

class SandboxThrowableTest : TestBase() {

    @Test
    fun `test user exception handling`() = sandbox(DEFAULT) {
        val contractExecutor = DeterministicSandboxExecutor<String, Array<String>>(configuration)
        contractExecutor.run<ThrowAndCatch>("Hello World").apply {
            assertThat(result)
                .isEqualTo(arrayOf("FIRST FINALLY", "BASE EXCEPTION", "Hello World", "SECOND FINALLY"))
        }
    }

}

class ThrowAndCatch : Function<String, Array<String>> {
    override fun apply(input: String): Array<String> {
        val data = mutableListOf<String>()
        try {
            try {
                throw MyExampleException(input)
            } finally {
                data += "FIRST FINALLY"
            }
        } catch (e: MyBaseException) {
            data += "BASE EXCEPTION"
            e.message?.apply { data += this }
        } catch (e: Exception) {
            data += "NOT THIS ONE!"
        } finally {
            data += "SECOND FINALLY"
        }

        return data.toTypedArray()
    }
}

open class MyBaseException(message: String) : Exception(message)
class MyExampleException(message: String) : MyBaseException(message)