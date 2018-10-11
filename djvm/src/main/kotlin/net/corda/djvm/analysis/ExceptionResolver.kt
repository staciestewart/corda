package net.corda.djvm.analysis

import org.objectweb.asm.Type

class ExceptionResolver(
    private val jvmExceptionClasses: Set<String>,
    private val sandboxPrefix: String
) {
    companion object {
        const val DJVM_EXCEPTION_NAME = "\$1DJVM"

        fun isDJVMException(className: String): Boolean = className.endsWith(DJVM_EXCEPTION_NAME)
        fun getDJVMException(className: String): String = className + DJVM_EXCEPTION_NAME
        fun getDJVMExceptionOwner(className: String): String = className.dropLast(DJVM_EXCEPTION_NAME.length)
    }

    fun getThrowableName(clazz: Class<*>): String {
        return getDJVMException(Type.getInternalName(clazz))
    }

    fun getThrowableSuperName(clazz: Class<*>): String {
        val superName = Type.getInternalName(clazz.superclass)
        return if (superName in jvmExceptionClasses) {
            superName.unsandboxed
        } else {
            getDJVMException(superName)
        }
    }

//    private val String.sandboxed: String get() = if (startsWith(sandboxPrefix)) {
//        this
//    } else {
//        sandboxPrefix + this
//    }
//
    private val String.unsandboxed: String get() = if (startsWith(sandboxPrefix)) {
        drop(sandboxPrefix.length)
    } else {
        this
    }
}