package net.corda.testing

import net.corda.core.DoNotImplement
import net.corda.core.internal.PLATFORM_VERSION
import net.corda.testing.node.internal.TestCordappImpl
import net.corda.testing.node.internal.simplifyScanPackages

/**
 * Represents information about a CorDapp. Used to generate CorDapp JARs in tests.
 */
@DoNotImplement
interface TestCordapp {
    /**
     *
     */
    val name: String

    /**
     *
     */
    val title: String

    /**
     *
     */
    val version: String

    /**
     *
     */
    val vendor: String

    /**
     *
     */
    val targetVersion: Int

    /**
     *
     */
    val packages: Set<String>

    /**
     *
     */
    fun withName(name: String): TestCordapp

    /**
     *
     */
    fun withTitle(title: String): TestCordapp

    /**
     *
     */
    fun withVersion(version: String): TestCordapp

    /**
     *
     */
    fun withVendor(vendor: String): TestCordapp

    /**
     *
     */
    fun withTargetVersion(targetVersion: Int): TestCordapp

    class Factory {
        companion object {
            /**
             *
             */
            @JvmStatic
            fun fromPackages(vararg packageNames: String): TestCordapp = fromPackages(packageNames.asList())

            /**
             *
             */
            @JvmStatic
            fun fromPackages(packageNames: Collection<String>): TestCordapp {
                return TestCordappImpl(
                        name = "test",
                        version = "1.0",
                        vendor = "Corda",
                        title = "test",
                        targetVersion = PLATFORM_VERSION,
                        packages = simplifyScanPackages(packageNames)
                )
            }
        }
    }
}
