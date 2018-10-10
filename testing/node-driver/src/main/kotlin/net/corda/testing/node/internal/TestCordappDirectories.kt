package net.corda.testing.node.internal

import net.corda.core.internal.createDirectories
import net.corda.core.internal.deleteRecursively
import net.corda.core.internal.div
import net.corda.core.utilities.loggerFor
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal object TestCordappDirectories {
    private val logger = loggerFor<TestCordappDirectories>()

    private val whitespace = "\\s".toRegex()
    private const val whitespaceReplacement = "_"

    private val testCordappsCache = ConcurrentHashMap<TestCordappImpl, Path>()

    fun cached(cordapps: Iterable<TestCordappImpl>, cordappsDirectory: Path = defaultCordappsDirectory): List<Path> {
        return cordapps.map { cached(it, cordappsDirectory) }
    }

    fun cached(cordapp: TestCordappImpl, cordappsDirectory: Path = defaultCordappsDirectory): Path {
        return testCordappsCache.computeIfAbsent(cordapp) {
            val cordappJar = cordappsDirectory / "${cordapp.name}_${cordapp.version}_${UUID.randomUUID()}.jar".replace(whitespace, whitespaceReplacement)

            cordappJar
        }
//            cordapp.packageAsJarInDirectory(cordappDirectory)
    }

    private val defaultCordappsDirectory: Path by lazy {
        val cordappsDirectory = (Paths.get("build") / "tmp" / getTimestampAsDirectoryName() / "generated-test-cordapps").toAbsolutePath()
        logger.info("Initialising generated test CorDapps directory in $cordappsDirectory")
        cordappsDirectory.toFile().deleteOnExit()
        cordappsDirectory.deleteRecursively()
        cordappsDirectory.createDirectories()
    }
}
