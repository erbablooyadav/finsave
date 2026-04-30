package com.finsave.domain

import io.kotest.property.checkAll
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Trivial smoke test to verify that the Kotest property-testing dependency
 * (io.kotest:kotest-property:5.9.1) resolves and that [checkAll] compiles
 * and runs correctly in the domain module's JVM test source set.
 */
class KotestSmokeTest {

    @Test
    fun `checkAll compiles and runs with a trivial property`() = runTest {
        checkAll(100, Arb.int()) { n ->
            // Trivial identity property: every Int equals itself
            assert(n == n) { "Expected $n == $n" }
        }
    }
}
