package com.example.localcam

import org.junit.Assert.assertEquals
import org.junit.Test

class RetryPolicyTest {
    @Test
    fun `delay increases then caps at final value`() {
        val policy = RetryPolicy(listOf(1000, 2000, 4000))

        assertEquals(1000, policy.delayForAttempt(0))
        assertEquals(1000, policy.delayForAttempt(1))
        assertEquals(2000, policy.delayForAttempt(2))
        assertEquals(4000, policy.delayForAttempt(3))
        assertEquals(4000, policy.delayForAttempt(8))
    }
}
