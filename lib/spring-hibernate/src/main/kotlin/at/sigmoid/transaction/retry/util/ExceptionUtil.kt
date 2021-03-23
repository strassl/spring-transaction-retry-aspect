package at.sigmoid.spring.transaction.retry

fun findRootCause(throwable: Throwable): Throwable {
    var rootCause: Throwable = throwable

    while (true) {
        val nextCause = rootCause.cause
        if (nextCause != null) {
            rootCause = nextCause
        } else {
            return rootCause
        }
    }
}