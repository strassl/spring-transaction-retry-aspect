package at.sigmoid.transaction.retry.util

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.jvm.javaMethod

internal class TransactionUtilTest {
    @Test
    fun `getActiveTransactionAnnotation should correctly discover nested annotation`() {
        val sig = mock<MethodSignature>()
        given(sig.method).willReturn(TestClassNested::foo.javaMethod)
        given(sig.declaringType).willReturn(TestClassNested::class.java)
        val annotation = getActiveTransactionalAnnotation(sig)!!

        assertThat(annotation.readOnly, equalTo(false))
        assertThat(annotation.isolation, equalTo(Isolation.SERIALIZABLE))
    }

    @Test
    fun `getActiveTransactionAnnotation should correctly discover class annotation`() {
        val sig = mock<MethodSignature>()
        given(sig.method).willReturn(TestClassOuter::foo.javaMethod)
        given(sig.declaringType).willReturn(TestClassOuter::class.java)
        val annotation = getActiveTransactionalAnnotation(sig)!!

        assertThat(annotation.isolation, equalTo(Isolation.REPEATABLE_READ))
    }

    @Test
    fun `getActiveTransactionAnnotation should correctly discover meta annotation`() {
        val sig = mock<MethodSignature>()
        given(sig.method).willReturn(TestClassMeta::foo.javaMethod)
        given(sig.declaringType).willReturn(TestClassMeta::class.java)
        val annotation = getActiveTransactionalAnnotation(sig)!!

        assertThat(annotation.isolation, equalTo(Isolation.REPEATABLE_READ))
    }

    @Test
    fun `getActiveTransactionAnnotation should ignore meta annotation override on class`() {
        val sig = mock<MethodSignature>()
        given(sig.method).willReturn(TestClassMetaOverride::foo.javaMethod)
        given(sig.declaringType).willReturn(TestClassMetaOverride::class.java)
        val annotation = getActiveTransactionalAnnotation(sig)!!

        assertThat(annotation.isolation, equalTo(Isolation.REPEATABLE_READ))
    }

    @Test
    fun `getActiveTransactionAnnotation should correctly discover meta annotation override in method`() {
        val sig = mock<MethodSignature>()
        given(sig.method).willReturn(TestClassMetaMethodOverride::foo.javaMethod)
        given(sig.declaringType).willReturn(TestClassMetaMethodOverride::class.java)
        val annotation = getActiveTransactionalAnnotation(sig)!!

        assertThat(annotation.isolation, equalTo(Isolation.READ_COMMITTED))
    }
}

@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
private class TestClassNested {
    @Transactional(isolation = Isolation.SERIALIZABLE)
    fun foo() {
    }
}

@Transactional(isolation = Isolation.REPEATABLE_READ)
private class TestClassOuter {
    fun foo() {
    }
}

@Transactional(isolation = Isolation.REPEATABLE_READ)
private annotation class Meta

@Meta
private class TestClassMeta {
    fun foo() {
    }
}

@Meta
@Transactional(isolation = Isolation.SERIALIZABLE)
private class TestClassMetaOverride {
    fun foo() {
    }
}

@Meta
private class TestClassMetaMethodOverride {
    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun foo() {
    }
}