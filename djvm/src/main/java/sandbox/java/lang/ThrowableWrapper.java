package sandbox.java.lang;

import org.jetbrains.annotations.NotNull;

/**
 * Pinned exceptions inherit from [java.lang.Throwable], but we
 * still need to be able to pass them through the sandbox's
 * exception handlers. In which case we will wrap them inside
 * one of these.
 *
 * Exceptions wrapped inside one of these cannot be caught.
 */
final class ThrowableWrapper extends Throwable {
    private final java.lang.Throwable throwable;

    ThrowableWrapper(java.lang.Throwable t) {
        throwable = t;
    }

    @Override
    @NotNull
    final java.lang.Throwable fromDJVM() {
        return throwable;
    }
}
