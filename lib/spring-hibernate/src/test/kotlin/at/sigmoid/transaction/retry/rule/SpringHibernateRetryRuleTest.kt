package at.sigmoid.transaction.retry.rule

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.hibernate.exception.LockAcquisitionException
import org.junit.jupiter.api.Test
import org.springframework.transaction.TransactionSystemException
import java.sql.SQLException

internal class SpringHibernateRetryRuleTest {
    @Test
    fun `isRetryableException with SQLException and retryable sql state should return true`() {
        val root = SQLException("", "40001")
        val exception = RuntimeException(RuntimeException(root))
        assertThat(isRetryableException(exception), equalTo(true))
    }

    @Test
    fun `isRetryableException with SQLException and non-retryable sql state in JpaSystemException should return false`() {
        val root = SQLException("", "42601")
        val exception = RuntimeException(RuntimeException(root))
        assertThat(isRetryableException(exception), equalTo(false))
    }

    @Test
    fun `isRetryableException with SQLException and retryable sql state in TransactionSystemException should return true`() {
        val root = SQLException("", "40P01")
        val exception = TransactionSystemException("", RuntimeException(root))
        assertThat(isRetryableException(exception), equalTo(true))
    }

    @Test
    fun `isRetryableException with SQLException and non-retryable sql state in TransactionSystemException should return false`() {
        val root = SQLException("", "42601")
        val exception = TransactionSystemException("", RuntimeException(root))
        assertThat(isRetryableException(exception), equalTo(false))
    }

    @Test
    fun `isRetryableException with LockAcquisitionException should return true`() {
        assertThat(isRetryableException(LockAcquisitionException("", null)), equalTo(true))
    }

    @Test
    fun `isRetryableException with ConcurrencyFailureException should return true`() {
        assertThat(isRetryableException(LockAcquisitionException("", null)), equalTo(true))
    }
}