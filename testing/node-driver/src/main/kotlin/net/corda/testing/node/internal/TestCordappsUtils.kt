package net.corda.testing.node.internal

import io.github.classgraph.ClassGraph
import net.corda.core.internal.createDirectories
import net.corda.core.internal.deleteIfExists
import net.corda.core.internal.outputStream
import net.corda.node.internal.cordapp.createTestManifest
import net.corda.testing.TestCordapp
import org.apache.commons.io.IOUtils
import java.io.OutputStream
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.reflect.KClass

/**
 * Packages some [JarEntryInfo] into a CorDapp JAR with specified [path].
 * @param path The path of the JAR.
 * @param willResourceBeAddedBeToCorDapp A filter for the inclusion of [JarEntryInfo] in the JAR.
 */
internal fun Iterable<JarEntryInfo>.packageToCorDapp(path: Path, name: String, version: String, vendor: String, title: String = name, willResourceBeAddedBeToCorDapp: (String, URL) -> Boolean = { _, _ -> true }) {

    var hasContent = false
    try {
        hasContent = packageToCorDapp(path.outputStream(), name, version, vendor, title, willResourceBeAddedBeToCorDapp)
    } finally {
        if (!hasContent) {
            path.deleteIfExists()
        }
    }
}

/**
 * Packages some [JarEntryInfo] into a CorDapp JAR using specified [outputStream].
 * @param outputStream The [OutputStream] for the JAR.
 * @param willResourceBeAddedBeToCorDapp A filter for the inclusion of [JarEntryInfo] in the JAR.
 */
internal fun Iterable<JarEntryInfo>.packageToCorDapp(outputStream: OutputStream, name: String, version: String, vendor: String, title: String = name, willResourceBeAddedBeToCorDapp: (String, URL) -> Boolean = { _, _ -> true }): Boolean {

    val manifest = createTestManifest(name, title, version, vendor)
    return JarOutputStream(outputStream, manifest).use { jos -> zip(jos, willResourceBeAddedBeToCorDapp) }
}

/**
 * Transforms a [Class] into a [JarEntryInfo].
 */
internal fun Class<*>.jarEntryInfo(): JarEntryInfo {

    return JarEntryInfo.ClassJarEntryInfo(this)
}

/**
 * Packages some [TestCordapp]s under a root [directory], each with it's own JAR.
 * @param directory The parent directory in which CorDapp JAR will be created.
 */
fun Iterable<TestCordappImpl>.packageInDirectory(directory: Path) {
    directory.createDirectories()
    forEach { cordapp -> cordapp.packageAsJarInDirectory(directory) }
}

/**
 * Returns all classes within the [targetPackage].
 */
fun allClassesForPackage(targetPackage: String): Set<Class<*>> {
    return ClassGraph()
            .whitelistPackages(targetPackage)
            .enableAllInfo()
            .scan()
            .use { it.allClasses.loadClasses() }
            .toSet()
}

/**
 * Creates a [TestCordapp] for each distinct package. This is useful for reducing the amount of CorDapp jar creation
 * that occurs in our tests. If instead you need all the packages packaged into just one [TestCordapp] then use
 * [TestCordapp.Factory.fromPackages].
 */
fun cordappsFromPackages(vararg packageNames: String): List<TestCordapp> {
    return simplifyScanPackages(packageNames.asList()).map { TestCordapp.Factory.fromPackages(it) }
}

fun getCallerClass(directCallerClass: KClass<*>): Class<*>? {

    val stackTrace = Throwable().stackTrace
    val index = stackTrace.indexOfLast { it.className == directCallerClass.java.name }
    if (index == -1) return null
    return try {
        Class.forName(stackTrace[index + 1].className)
    } catch (e: ClassNotFoundException) {
        null
    }
}

fun getCallerPackage(directCallerClass: KClass<*>): String? {

    return getCallerClass(directCallerClass)?.`package`?.name
}

/**
 * Squashes child packages if the parent is present. Example: ["com.foo", "com.foo.bar"] into just ["com.foo"].
 */
fun simplifyScanPackages(scanPackages: Collection<String>): Set<String> {
    return scanPackages.sorted().fold(emptySet()) { soFar, packageName ->
        when {
            soFar.isEmpty() -> setOf(packageName)
            packageName.startsWith("${soFar.last()}.") -> soFar
            else -> soFar + packageName
        }
    }
}

/**
 * Transforms a class or package name into a path segment.
 */
internal fun String.packageToJarPath() = replace(".", "/")

private fun Iterable<JarEntryInfo>.zip(outputStream: ZipOutputStream, willResourceBeAddedBeToCorDapp: (String, URL) -> Boolean): Boolean {

    val entries = filter { (fullyQualifiedName, url) -> willResourceBeAddedBeToCorDapp(fullyQualifiedName, url) }
    if (entries.isNotEmpty()) {
        zip(outputStream, entries)
    }
    return entries.isNotEmpty()
}



private fun zip(outputStream: ZipOutputStream, allInfo: Iterable<JarEntryInfo>) {
    val time = FileTime.from(Instant.EPOCH)
    val classLoader = Thread.currentThread().contextClassLoader
    allInfo.distinctBy { it.url }.sortedBy { it.url.toExternalForm() }.forEach { info ->
        try {
            val entry = ZipEntry(info.entryName).setCreationTime(time).setLastAccessTime(time).setLastModifiedTime(time)
            outputStream.putNextEntry(entry)
            classLoader.getResourceAsStream(info.entryName).use {
                IOUtils.copy(it, outputStream)
            }
        } finally {
            outputStream.closeEntry()
        }
    }
}

fun TestCordappImpl.packageAsJar(file: Path) {
    ClassGraph().whitelistPackages(*packages.toTypedArray()).enableAllInfo().scan().use {

    }
}
