package at.sigmoid.transaction.retry.util.test

import java.util.concurrent.atomic.AtomicLong

class TestException : RuntimeException() {
    companion object {
        private val currentId = AtomicLong(-1)
    }

    val id: Long = currentId.incrementAndGet()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestException

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
