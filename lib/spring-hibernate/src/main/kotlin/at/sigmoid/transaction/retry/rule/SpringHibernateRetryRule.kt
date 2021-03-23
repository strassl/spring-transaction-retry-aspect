package at.sigmoid.transaction.retry.rule

import at.sigmoid.spring.transaction.retry.findRootCause
import org.hibernate.exception.LockAcquisitionException
import org.springframework.dao.ConcurrencyFailureException
import java.sql.SQLException

private const val SQL_STATE_SERIALIZATION_FAILURE = "40001"
private const val SQL_STATE_DEADLOCK_DETECTED = "40P01"

fun isSpringTxRetryableException(t: Throwable): Boolean {
    if (t is ConcurrencyFailureException) return true

    return false
}

fun isHibernateRetryableException(t: Throwable): Boolean {
    if (t is LockAcquisitionException) return true

    return false
}

fun isRetryableException(t: Throwable): Boolean {
    if (isSpringTxRetryableException(t)) return true
    if (isHibernateRetryableException(t)) return true

    // Apparently Hibernate does not correctly map the various SQL errors if they occur during a commit
    // Thus we manually check if the rollback is due to a serialization failure and retry if this is the case
    val rootCause = findRootCause(t)
    if (rootCause is SQLException) {
        val sqlState = rootCause.sqlState
        return when (sqlState) {
            SQL_STATE_SERIALIZATION_FAILURE -> true
            SQL_STATE_DEADLOCK_DETECTED -> true
            else -> false
        }
    }

    return false
}

