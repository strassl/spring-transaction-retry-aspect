package at.sigmoid.transaction.retry.util

import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.transaction.annotation.Transactional

fun getActiveTransactionalAnnotation(signature: MethodSignature): Transactional? {
    val methodTransactionAnnotation = AnnotatedElementUtils.findMergedAnnotation(signature.method, Transactional::class.java)
    val classTransactionalAnnotation = AnnotatedElementUtils.findMergedAnnotation(signature.declaringType, Transactional::class.java)

    val transactionAnnotation = (methodTransactionAnnotation ?: classTransactionalAnnotation)

    return transactionAnnotation
}