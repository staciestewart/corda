package net.corda.djvm.code.instructions

import org.objectweb.asm.Label

/**
 * Try-finally block.
 *
 * @property handler The handler for the finally-block.
 * @property isMonitor Whether this handler has been created for a MONITOREXIT instruction.
 */
class TryFinallyBlock(
        handler: Label,
        val isMonitor: Boolean
) : TryBlock(handler)
