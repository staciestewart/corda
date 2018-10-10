package net.corda.testing.node.internal

import net.corda.testing.TestCordapp
import java.io.File
import java.net.URL

data class TestCordappImpl(override val name: String,
                           override val version: String,
                           override val vendor: String,
                           override val title: String,
                           override val targetVersion: Int,
                           override val packages: Set<String>) : TestCordapp {

    companion object {
        private const val jarExtension = ".jar"
        private const val whitespace = " "
        private const val whitespaceReplacement = "_"
        private val productionPathSegments = setOf<(String) -> String>(
                { "out${File.separator}production${File.separator}classes" },
                { fullyQualifiedName -> "main${File.separator}${fullyQualifiedName.packageToJarPath()}" }
        )
        private val excludedCordaPackages = setOf("net.corda.core", "net.corda.node")

        fun filterTestCorDappClass(fullyQualifiedName: String, url: URL): Boolean {

            return isTestResource(fullyQualifiedName, url) || !isInExcludedCordaPackage(fullyQualifiedName)
        }

        private fun isTestResource(fullyQualifiedName: String, url: URL): Boolean {

            return productionPathSegments.map { it.invoke(fullyQualifiedName) }.none { url.toString().contains(it) }
        }

        private fun isInExcludedCordaPackage(packageName: String): Boolean {

            return excludedCordaPackages.any { packageName.startsWith(it) }
        }

        fun jarEntriesFromClasses(classes: Set<Class<*>>): Set<JarEntryInfo> {
            val illegal = classes.filter { it.protectionDomain?.codeSource?.location == null }
            require(illegal.isEmpty()) {
                "Some classes do not have a location, typically because they are part of Java or Kotlin. " +
                        "Offending types were: ${illegal.joinToString(", ", "[", "]") { it.simpleName }}"
            }
            return classes.map(Class<*>::jarEntryInfo).toSet()
        }
    }

    override fun withName(name: String): TestCordappImpl = copy(name = name)

    override fun withVersion(version: String): TestCordappImpl = copy(version = version)

    override fun withVendor(vendor: String): TestCordappImpl = copy(vendor = vendor)

    override fun withTitle(title: String): TestCordappImpl = copy(title = title)

    override fun withTargetVersion(targetVersion: Int): TestCordappImpl = copy(targetVersion = targetVersion)

//    private fun packageAsJarWithPath(jarFilePath: Path) {
//        jarEntries.packageToCorDapp(jarFilePath, name, version, vendor, title, TestCordappImpl.Companion::filterTestCorDappClass)
//    }
//
//    fun packageAsJarInDirectory(parentDirectory: Path): Path = (parentDirectory / defaultJarName()).also { packageAsJarWithPath(it) }
//
//    private fun defaultJarName(): String = "${name}_$version$jarExtension".replace(whitespace, whitespaceReplacement)
}
