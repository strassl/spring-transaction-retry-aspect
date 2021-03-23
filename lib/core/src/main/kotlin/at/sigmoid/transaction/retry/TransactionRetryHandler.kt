package at.sigmoid.transaction.retry

import org.slf4j.LoggerFactory
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

class TransactionRetriesExceededException(message: String, cause: Throwable) : RuntimeException(message, cause)

private data class TransactionRetryState(
    val active: Boolean,
    val noRetry: Boolean
)

interface TransactionRetryScope<T> {
    val type: RetryScopeType

    fun proceed(): T
}

enum class RetryScopeType {
    /**
     * Transaction may be retried at this scope (but must not)
     */
    CAN_RETRY,

    /**
     * Transaction cannot be retried at this scope (but may be retried at another scope)
     */
    CAN_NOT_RETRY,

    /**
     * Transaction must not be retried ever (e.g. if it has side effects)
     */
    MUST_NOT_RETRY
}

typealias Sleeper = (Long) -> Unit

/**
 * Retries nested transactional function calls. This class is meant to intercept every transaction scope (e.g. every
 * method call annotated with @Transactional) and will attempt to retry at the outermost scope.
 * All transaction calls are assumed to occur on the same thread.
 * The retry handler is thread safe.
 * @param maxRetries the maximum number of times the call shall be retried
 * @param minDelayMs the minimum time between subsequent retries in ms
 * @param delayMultiplier the multiplicative factor for the exponential backoff
 * @param isRetryableException predicate whether a transaction is a retryable transaciton failure
 * @param random source of randomness for fuzziness in backoff
 * @param sleeper function to block the execution for an arbitrary number of ms. Only used for testing - you most
 * likely want to use Thread::sleep for any production use case
 */
class TransactionRetryHandler @JvmOverloads constructor(
    private val maxRetries: Int,
    private val minDelayMs: Long,
    private val delayMultiplier: Double,
    private val isRetryableException: (e: Throwable) -> Boolean,
    // Mostly used for tests
    private val random: Random = Random.Default,
    private val sleeper: Sleeper = Thread::sleep
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionRetryHandler::class.java)
    }

    private val _state = ThreadLocal.withInitial { cleanState() }
    private var state: TransactionRetryState
        get() = _state.get()
        set(value) = _state.set(value)

    @Throws(Throwable::class)
    fun <T> handle(scope: TransactionRetryScope<T>): T {
        // If a method is marked as NoRetry, it poisons the entire current transaction
        if (scope.type === RetryScopeType.MUST_NOT_RETRY) {
            state = state.copy(noRetry = true)
        }

        // If we are in an existing transaction we just go on as usual, retries happen at the outermost layer
        if (state.active) {
            return scope.proceed()
        }

        // Transaction cannot be retried at this point, just call it
        if (scope.type == RetryScopeType.CAN_NOT_RETRY) {
            return scope.proceed()
        }

        val prevState = state
        try {
            var currentTry = 0

            while (true) {
                state = prevState.copy(active = true)

                val throwable: Throwable
                try {
                    return scope.proceed()
                } catch (t: Throwable) {
                    throwable = t
                }

                if (isRetryableException(throwable)) {
                    if (state.noRetry) {
                        // If some method is marked as no-retry, we stop here and just fail
                        throw TransactionRetriesExceededException("Marked as no-retry", throwable)
                    }
                    // The initial try is round 0 (we always try at least one time)
                    // After this we are going to retry at most MAX_RETRIES times
                    // e.g. if MAX_RETRIES=3 then we have at most
                    // 0 (always) -> 1 -> 2 -> 3 -> abort
                    if (currentTry < maxRetries) {
                        // We wait at least MIN_DELAY_MS ms long and the back off
                        // Also randomize the base backoff time to be between [MIN_DELAY_MS and 2*MIN_DELAY_MS]
                        // to reduce the likelihood of two conflicting transactions being executed at the same time again
                        // Therefore the wait time is baseBackoff * delayMultiplier^currentTry e.g.
                        // try 0: baseBackoff * 1
                        // try 1: baseBackoff * delayMultiplier
                        // try 2: baseBackoff * delayMultiplier^2
                        // try 3: baseBackoff * delayMultiplier^3
                        // ...
                        // Note that baseBackoff is randomized again each time to reduce impact of "unlucky" requests
                        val randFactor = random.nextDouble()
                        val baseBackoff = minDelayMs * (1 + randFactor)
                        val delayMs = (baseBackoff * delayMultiplier.pow(currentTry)).roundToLong()
                        LOGGER.debug("Serialization problem in transaction - retrying in $delayMs ms ($currentTry of $maxRetries)", throwable)
                        sleeper(delayMs)
                    } else {
                        // We did attempt to retry the transaction but it did not work out
                        LOGGER.warn("Maximum number of transaction retries ($maxRetries) exceeded, aborting")
                        throw TransactionRetriesExceededException("Maximum number of transaction retries ($maxRetries) exceeded", throwable)
                    }
                } else {
                    // It is an entirely different exception, just rethrow it
                    throw throwable
                }

                currentTry += 1
            }
        } finally {
            state = prevState
        }
    }

    private fun cleanState(): TransactionRetryState {
        return TransactionRetryState(active = false, noRetry = false)
    }
}
