package at.sigmoid.transaction.retry.scope

import at.sigmoid.transaction.retry.RetryScopeType
import at.sigmoid.transaction.retry.TransactionRetryScope
import at.sigmoid.transaction.retry.util.getActiveTransactionalAnnotation
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.transaction.annotation.Propagation

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class NoRetryTx

class JoinPointTransactionRetryScope(
    private val pjp: ProceedingJoinPoint
) : TransactionRetryScope<Any?> {
    override val type: RetryScopeType
        get() {
            return if (isNoRetry(pjp)) {
                RetryScopeType.MUST_NOT_RETRY
            } else {
                if (isPotentialTransactionInitiator(pjp)) {
                    RetryScopeType.CAN_RETRY
                } else {
                    RetryScopeType.CAN_NOT_RETRY
                }
            }
        }

    override fun proceed(): Any? {
        return pjp.proceed()
    }
}

private fun isNoRetry(pjp: ProceedingJoinPoint): Boolean {
    val method = (pjp.signature as MethodSignature).method
    val noRetryAnnotation = AnnotationUtils.findAnnotation(method, NoRetryTx::class.java)
    return noRetryAnnotation != null
}

private fun isPotentialTransactionInitiator(pjp: ProceedingJoinPoint): Boolean {
    val transactionAnnotation = getActiveTransactionalAnnotation(pjp.signature as MethodSignature)!!

    val isInitiator = when (transactionAnnotation.propagation) {
        Propagation.REQUIRED -> true
        Propagation.SUPPORTS -> false
        Propagation.MANDATORY -> false
        Propagation.REQUIRES_NEW -> true
        Propagation.NOT_SUPPORTED -> false
        Propagation.NEVER -> false
        Propagation.NESTED -> true
    }

    return isInitiator
}
