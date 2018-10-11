package net.corda.djvm.rules.implementation

import net.corda.djvm.code.EMIT_TRAPPING_EXCEPTIONS
import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.instructions.CodeLabel
import net.corda.djvm.code.instructions.TryBlock
import net.corda.djvm.code.instructions.TryCatchBlock
import org.objectweb.asm.Label

/**
 * Converts an exception from [java.lang.Throwable] to [sandbox.java.lang.Throwable]
 * at the beginning of either a catch block or a finally block.
 */
class HandleExceptionUnwrapper : Emitter {
    private val handlers = mutableMapOf<Label, String>()

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is TryBlock) {
            handlers[instruction.handler] = (instruction as? TryCatchBlock)?.typeName ?: ""
        } else if (instruction is CodeLabel) {
            handlers[instruction.label]?.let { exceptionType ->
                invokeStatic("sandbox/java/lang/DJVM", "toDJVM", "(Ljava/lang/Throwable;)Lsandbox/java/lang/Throwable;")

                /**
                 * When catching exceptions, we also need to tell the verifier which
                 * which kind of [sandbox.java.lang.Throwable] to expect this to be.
                 */
                if (exceptionType.isNotEmpty()) {
                    castObjectTo(exceptionType)
                }
            }
        }
    }

    override val priority: Int
        get() = EMIT_TRAPPING_EXCEPTIONS + 1
}