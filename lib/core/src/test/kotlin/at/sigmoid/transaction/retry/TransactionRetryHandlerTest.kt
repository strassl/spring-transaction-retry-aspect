package at.sigmoid.transaction.retry

import at.sigmoid.transaction.retry.util.test.TestException
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.test.assertFailsWith

class TransactionRetryHandlerTest {
    private val random = mock<Random>()
    private val sleeper = mock<Sleeper>()
    private val retryHandler = TransactionRetryHandler(3, 4, 1.5, isRetryableException = { it is TestException }, random = random, sleeper = sleeper)

    @BeforeEach
    fun setup() {
        given(random.nextDouble()).willReturn(0.25)
    }

    @Test
    fun `it should calculate the correct backoff for each run`() {
        assertFailsWith(TransactionRetriesExceededException::class) {
            retryHandler.handle(
                RetryScope {
                    throw TestException()
                }
            )
        }

        // (4 * 1.25) * 1.5^0
        verify(sleeper).invoke(5)
        // (4 * 1.25) * 1.5^1, rounded
        verify(sleeper).invoke(8)
        // (4 * 1.25) * 1.5^2, rounded
        verify(sleeper).invoke(11)
    }

    @Test
    fun `it should retry transactions up to maxRetries times`() {
        val counter = AtomicInteger(0)

        assertFailsWith(TransactionRetriesExceededException::class) {
            retryHandler.handle(
                RetryScope {
                    counter.incrementAndGet()
                    throw TestException()
                }
            )
        }

        assertThat(counter.get(), equalTo(4))
    }

    @Test
    fun `it should not retry nested transactions`() {
        val counter = AtomicInteger(0)

        assertFailsWith(TransactionRetriesExceededException::class) {
            retryHandler.handle(
                RetryScope {
                    counter.incrementAndGet()
                    retryHandler.handle<Any>(
                        RetryScope {
                            counter.incrementAndGet()
                            throw TestException()
                        }
                    )
                }
            )
        }

        assertThat(counter.get(), equalTo(8))
    }

    @Test
    fun `it should not retry non-retryable scopes`() {
        val counter = AtomicInteger(0)

        assertFailsWith(TestException::class) {
            retryHandler.handle(
                RetryScope(
                    type = RetryScopeType.CAN_NOT_RETRY,
                    action = {
                        counter.incrementAndGet()
                        throw TestException()
                    }
                )
            )
        }

        assertThat(counter.get(), equalTo(1))
    }

    @Test
    fun `it should not retry transactions if a sub-scope precludes retry`() {
        val counter = AtomicInteger(0)

        assertFailsWith(TransactionRetriesExceededException::class) {
            retryHandler.handle(
                RetryScope(
                    type = RetryScopeType.CAN_RETRY,
                    action = {
                        counter.incrementAndGet()
                        retryHandler.handle<Any>(
                            RetryScope(
                                type = RetryScopeType.MUST_NOT_RETRY,
                                action = {
                                    counter.incrementAndGet()
                                    throw TestException()
                                }
                            )
                        )
                    }
                )
            )
        }

        assertThat(counter.get(), equalTo(2))
    }

    @Test
    fun `it should retry transactions if a sub-scope cannot be retried`() {
        val counter = AtomicInteger(0)

        assertFailsWith(TransactionRetriesExceededException::class) {
            retryHandler.handle(
                RetryScope(
                    type = RetryScopeType.CAN_RETRY,
                    action = {
                        counter.incrementAndGet()
                        retryHandler.handle<Any>(
                            RetryScope(
                                type = RetryScopeType.CAN_NOT_RETRY,
                                action = {
                                    counter.incrementAndGet()
                                    throw TestException()
                                }
                            )
                        )
                    }
                )
            )
        }

        assertThat(counter.get(), equalTo(8))
    }
}

private class RetryScope<T>(
    override val type: RetryScopeType = RetryScopeType.CAN_RETRY,
    private val action: () -> T
) : TransactionRetryScope<T> {
    override fun proceed(): T {
        return action()
    }
}
