package com.example.localcam

class RetryPolicy(
    private val delaysMs: List<Long> = listOf(1_000L, 2_000L, 4_000L, 8_000L, 10_000L),
    val maxAttempts: Int = 12
) {
    fun delayForAttempt(attempt: Int): Long {
        if (attempt <= 0) return delaysMs.first()
        val index = (attempt - 1).coerceAtMost(delaysMs.lastIndex)
        return delaysMs[index]
    }
}
