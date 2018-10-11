@file:JvmName("DJVM")
@file:Suppress("unused")
package sandbox.java.lang

import net.corda.djvm.analysis.AnalysisConfiguration.Companion.JVM_EXCEPTIONS
import net.corda.djvm.analysis.ExceptionResolver.Companion.getDJVMException
import org.objectweb.asm.Opcodes.ACC_ENUM
import org.objectweb.asm.Type
import sandbox.net.corda.djvm.rules.RuleViolationError

private const val SANDBOX_PREFIX = "sandbox."

fun Any.unsandbox(): Any {
    return when (this) {
        is Object -> fromDJVM()
        is Array<*> -> fromDJVMArray()
        else -> this
    }
}

@Throws(ClassNotFoundException::class)
fun Any.sandbox(): Any {
    return when (this) {
        is kotlin.String -> String.toDJVM(this)
        is kotlin.Char -> Character.toDJVM(this)
        is kotlin.Long -> Long.toDJVM(this)
        is kotlin.Int -> Integer.toDJVM(this)
        is kotlin.Short -> Short.toDJVM(this)
        is kotlin.Byte -> Byte.toDJVM(this)
        is kotlin.Float -> Float.toDJVM(this)
        is kotlin.Double -> Double.toDJVM(this)
        is kotlin.Boolean -> Boolean.toDJVM(this)
        is kotlin.Enum<*> -> toDJVMEnum()
        is Array<*> -> toDJVMArray<Object>()
        else -> this
    }
}

private fun Array<*>.fromDJVMArray(): Array<*> = Object.fromDJVM(this)

/**
 * These functions use the "current" classloader, i.e. classloader
 * that owns this DJVM class.
 */
@Throws(ClassNotFoundException::class)
internal fun Class<*>.toDJVMType(): Class<*> = Class.forName(name.toSandboxPackage())

@Throws(ClassNotFoundException::class)
internal fun Class<*>.fromDJVMType(): Class<*> = Class.forName(name.fromSandboxPackage())

private fun kotlin.String.toSandboxPackage(): kotlin.String {
    return if (startsWith(SANDBOX_PREFIX)) {
        this
    } else {
        SANDBOX_PREFIX + this
    }
}

private fun kotlin.String.fromSandboxPackage(): kotlin.String {
    return if (startsWith(SANDBOX_PREFIX)) {
        drop(SANDBOX_PREFIX.length)
    } else {
        this
    }
}

private inline fun <reified T : Object> Array<*>.toDJVMArray(): Array<out T?> {
    @Suppress("unchecked_cast")
    return (java.lang.reflect.Array.newInstance(javaClass.componentType.toDJVMType(), size) as Array<T?>).also {
        for ((i, item) in withIndex()) {
            it[i] = item?.sandbox() as T
        }
    }
}

@Throws(ClassNotFoundException::class)
internal fun Enum<*>.fromDJVMEnum(): kotlin.Enum<*> {
    return javaClass.fromDJVMType().enumConstants[ordinal()] as kotlin.Enum<*>
}

@Throws(ClassNotFoundException::class)
private fun kotlin.Enum<*>.toDJVMEnum(): Enum<*> {
    @Suppress("unchecked_cast")
    return (getEnumConstants(javaClass.toDJVMType() as Class<Enum<*>>) as Array<Enum<*>>)[ordinal]
}

/**
 * Replacement functions for the members of Class<*> that support Enums.
 */
fun isEnum(clazz: Class<*>): kotlin.Boolean
        = (clazz.modifiers and ACC_ENUM != 0) && (clazz.superclass == sandbox.java.lang.Enum::class.java)

fun getEnumConstants(clazz: Class<out Enum<*>>): Array<*>? {
    return getEnumConstantsShared(clazz)?.clone()
}

internal fun enumConstantDirectory(clazz: Class<out Enum<*>>): sandbox.java.util.Map<String, out Enum<*>>? {
    // DO NOT replace get with Kotlin's [] because Kotlin would use java.util.Map.
    return allEnumDirectories.get(clazz) ?: createEnumDirectory(clazz)
}

@Suppress("unchecked_cast")
internal fun getEnumConstantsShared(clazz: Class<out Enum<*>>): Array<out Enum<*>>? {
    return if (isEnum(clazz)) {
        // DO NOT replace get with Kotlin's [] because Kotlin would use java.util.Map.
        allEnums.get(clazz) ?: createEnum(clazz)
    } else {
        null
    }
}

@Suppress("unchecked_cast")
private fun createEnum(clazz: Class<out Enum<*>>): Array<out Enum<*>>? {
    return clazz.getMethod("values").let { method ->
        method.isAccessible = true
        method.invoke(null) as? Array<out Enum<*>>
    // DO NOT replace put with Kotlin's [] because Kotlin would use java.util.Map.
    }?.apply { allEnums.put(clazz, this) }
}

private fun createEnumDirectory(clazz: Class<out Enum<*>>): sandbox.java.util.Map<String, out Enum<*>> {
    val universe = getEnumConstantsShared(clazz) ?: throw IllegalArgumentException("${clazz.name} is not an enum type")
    val directory = sandbox.java.util.LinkedHashMap<String, Enum<*>>(2 * universe.size)
    for (entry in universe) {
        // DO NOT replace put with Kotlin's [] because Kotlin would use java.util.Map.
        directory.put(entry.name(), entry)
    }
    // DO NOT replace put with Kotlin's [] because Kotlin would use java.util.Map.
    allEnumDirectories.put(clazz, directory)
    return directory
}

private val allEnums: sandbox.java.util.Map<Class<out Enum<*>>, Array<out Enum<*>>> = sandbox.java.util.LinkedHashMap()
private val allEnumDirectories: sandbox.java.util.Map<Class<out Enum<*>>, sandbox.java.util.Map<String, out Enum<*>>> = sandbox.java.util.LinkedHashMap()

/**
 * Replacement functions for Class<*>.forName(...) which protect
 * against users loading classes from outside the sandbox.
 */
@Throws(ClassNotFoundException::class)
fun classForName(className: kotlin.String): Class<*> {
    return Class.forName(toSandbox(className))
}

@Throws(ClassNotFoundException::class)
fun classForName(className: kotlin.String, initialize: kotlin.Boolean, classLoader: ClassLoader): Class<*> {
    return Class.forName(toSandbox(className), initialize, classLoader)
}

/**
 * Force the qualified class name into the sandbox.* namespace.
 * Throw [ClassNotFoundException] anyway if we wouldn't want to
 * return the resulting sandbox class. E.g. for any of our own
 * internal classes.
 */
private fun toSandbox(className: kotlin.String): kotlin.String {
    if (bannedClasses.any { it.matches(className) }) {
        throw ClassNotFoundException(className)
    }
    return SANDBOX_PREFIX + className
}

private val bannedClasses = setOf(
    "^java\\.lang\\.DJVM(.*)?\$".toRegex(),
    "^net\\.corda\\.djvm\\..*\$".toRegex(),
    "^Task\$".toRegex()
)

/**
 * Exception Management.
 */
fun fromDJVM(t: Throwable?): kotlin.Throwable {
    return if (t is ThrowableWrapper) {
        t.fromDJVM()
    } else {
        try {
            val sandboxedName = t!!.javaClass.name
            if (Type.getInternalName(t.javaClass) in JVM_EXCEPTIONS) {
                Class.forName(sandboxedName.fromSandboxPackage()).createKotlinThrowable(t)
            } else {
                Class.forName(getDJVMException(sandboxedName))
                    .getDeclaredConstructor(sandboxThrowable)
                    .newInstance(t) as kotlin.Throwable
            }
        } catch (e: Exception) {
            throw RuleViolationError(e.message ?: "")
        }
    }
}

fun toDJVM(t: kotlin.Throwable): Throwable {
    return if (t is ThreadDeath) {
        /**
         * [RuleViolationError] and [ThresholdViolationError] are
         * both [ThreadDeath] exceptions, and we're not allowed
         * to catch them. (And we shouldn't catch [ThreadDeath]
         * either!) But they should still be able to pass through
         * a finally block unhindered.
         */
        ThrowableWrapper(t)
    } else {
        try {
            (t as? DJVMException)?.getThrowable() ?: t.javaClass.toDJVMType().createDJVMThrowable(t)
        } catch (e: ClassNotFoundException) {
            ThrowableWrapper(e)
        }
    }
}

private fun Class<*>.createDJVMThrowable(t: kotlin.Throwable): Throwable {
    return (try {
        getDeclaredConstructor(String::class.java).newInstance(String.toDJVM(t.message))
    } catch (e: NoSuchMethodException) {
        newInstance()
    } as Throwable).apply {
        t.cause?.also {
            initCause(toDJVM(it))
        }
    }
}

private fun Class<*>.createKotlinThrowable(t: Throwable): kotlin.Throwable {
    return (try {
        getDeclaredConstructor(kotlin.String::class.java).newInstance(String.fromDJVM(t.message))
    } catch (e: NoSuchMethodException) {
        newInstance()
    } as kotlin.Throwable).apply {
        t.cause?.also {
            initCause(fromDJVM(it))
        }
    }
}

private val sandboxThrowable: Class<*> = Throwable::class.java
