package net.corda.testing.node.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestCordappsUtilsTest {
    @Test
    fun `test simplifyScanPackages`() {
        assertThat(simplifyScanPackages(emptyList())).isEmpty()
        assertThat(simplifyScanPackages(listOf("com.foo.bar"))).containsOnly("com.foo.bar")
        assertThat(simplifyScanPackages(listOf("com.foo", "com.bar"))).containsOnly("com.foo", "com.bar")
        assertThat(simplifyScanPackages(listOf("com.foo", "com.foo.bar"))).containsOnly("com.foo")
        assertThat(simplifyScanPackages(listOf("com.foo.bar", "com.foo"))).containsOnly("com.foo")
        assertThat(simplifyScanPackages(listOf("com.foobar", "com.foo.bar"))).containsOnly("com.foobar", "com.foo.bar")
        assertThat(simplifyScanPackages(listOf("com.foobar", "com.foo"))).containsOnly("com.foobar", "com.foo")
    }
}
