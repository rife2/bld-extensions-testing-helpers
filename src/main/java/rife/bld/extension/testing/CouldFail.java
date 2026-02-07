/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rife.bld.extension.testing;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Marks a test as allowed to fail.
 *
 * <p>When a test annotated with {@code @CouldFail} fails, the failure is accepted
 * and the test is aborted (shown as skipped). If the test passes, it continues
 * to pass normally.</p>
 *
 * <p>This is similar to JUnit Pioneer's {@code @ExpectedToFail} but with inverted
 * logic: a passing test continues to pass (not fail), making it suitable for
 * acknowledging known issues without expecting them to be fixed.</p>
 *
 * <h2>Usage</h2>
 *
 * <h3>Accept Any Failure</h3>
 * <pre>{@code
 * @Test
 * @CouldFail
 * void testFlakeyOperation() {
 *     // Test that might fail - any exception will be accepted
 * }
 * }</pre>
 *
 * <h3>Accept Specific Exceptions</h3>
 * <pre>{@code
 * @Test
 * @CouldFail(withExceptions = UnsupportedOperationException.class)
 * void testUnimplementedFeature() {
 *     // Only UnsupportedOperationException will be accepted
 *     // Other exceptions will fail the test normally
 * }
 * }</pre>
 *
 * <h3>Accept Multiple Exception Types</h3>
 * <pre>{@code
 * @Test
 * @CouldFail(withExceptions = {IOException.class, TimeoutException.class})
 * void testNetworkOperation() {
 *     // IOException or TimeoutException will be accepted
 *     // Other exceptions will fail the test normally
 * }
 * }</pre>
 *
 * <h2>When to Use</h2>
 * <ul>
 *   <li>Known bugs or issues that haven't been fixed yet</li>
 *   <li>Intermittent failures in CI/CD pipelines</li>
 *   <li>Platform-specific issues</li>
 *   <li>External service dependencies that may be unavailable</li>
 *   <li>Unimplemented features (stub code throwing UnsupportedOperationException)</li>
 * </ul>
 *
 * <h2>Behavior</h2>
 * <ul>
 *   <li>If the test passes: continues to pass (no special handling)</li>
 *   <li>If the test fails with accepted exception: test is aborted (shown as skipped)</li>
 *   <li>If the test fails with non-accepted exception: test fails normally</li>
 * </ul>
 *
 * @since 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(CouldFail.Extension.class)
public @interface CouldFail {

    /**
     * Specific exception types that will be accepted as failures.
     *
     * <p>If empty (default), any exception will be accepted. If specified,
     * only these exception types (and their subclasses) will be accepted.
     * Any other exception will cause the test to fail normally.</p>
     *
     * @return array of exception classes to accept
     */
    Class<? extends Throwable>[] withExceptions() default {};

    /**
     * Internal extension that implements the @CouldFail behavior.
     *
     * <p>This extension is automatically registered via the {@code @ExtendWith}
     * meta-annotation on {@code @CouldFail}, so users don't need to register
     * it separately.</p>
     *
     * <h2>Execution Flow</h2>
     * <ol>
     *   <li>Test executes normally</li>
     *   <li>If test passes, extension does nothing</li>
     *   <li>If test fails, {@code handleTestExecutionException} is called:
     *     <ol>
     *       <li>Extension checks for {@code @CouldFail} on method, then class</li>
     *       <li>If annotation not found, the test fails normally</li>
     *       <li>If {@code withExceptions} is empty, accept any exception</li>
     *       <li>If {@code withExceptions} is specified, check if thrown exception matches</li>
     *       <li>If the exception is accepted: throw {@code TestAbortedException}</li>
     *       <li>If the exception is not accepted: re-throw the original exception</li>
     *     </ol>
     *   </li>
     * </ol>
     *
     * <h2>Exception Matching</h2>
     * <p>An exception is considered "accepted" if:</p>
     * <ul>
     *   <li>{@code withExceptions} is empty (accepts any exception), OR</li>
     *   <li>The thrown exception is an instance of any class in {@code withExceptions}</li>
     * </ul>
     *
     * <h2>Logging</h2>
     * <p>When a failure is accepted, an INFO-level log message is generated:</p>
     * <pre>
     * Test failure accepted by @CouldFail (Original exception: IOException: Connection timeout)
     * </pre>
     *
     * @see TestExecutionExceptionHandler
     */
    class Extension implements TestExecutionExceptionHandler {

        private static final Logger LOGGER = Logger.getLogger(Extension.class.getName());

        @Override
        public void handleTestExecutionException(ExtensionContext context, Throwable throwable)
                throws Throwable {

            var annotation = findCouldFailAnnotation(context);

            if (annotation.isEmpty()) {
                // No @CouldFail annotation, propagate exception normally
                throw throwable;
            }

            var couldFail = annotation.get();

            if (shouldAcceptFailure(couldFail, throwable)) {
                // Exception is accepted - abort the test
                var message = buildAcceptedFailureMessage(throwable);
                LOGGER.info(message);
                throw new TestAbortedException(message, throwable);
            } else {
                // Exception is not accepted - fail normally
                throw throwable;
            }
        }

        private String buildAcceptedFailureMessage(Throwable originalException) {
            return "Test failure accepted by @CouldFail" + " (Original exception: " +
                    originalException.getClass().getSimpleName() +
                    ": " +
                    originalException.getMessage() +
                    ")";
        }

        private Optional<CouldFail> findCouldFailAnnotation(ExtensionContext context) {
            // Check method first, then class
            return context.getTestMethod()
                    .flatMap(method -> Optional.ofNullable(method.getAnnotation(CouldFail.class)))
                    .or(() -> context.getTestClass()
                            .flatMap(clazz -> Optional.ofNullable(clazz.getAnnotation(CouldFail.class))));
        }

        private boolean shouldAcceptFailure(CouldFail couldFail, Throwable throwable) {
            var withExceptions = couldFail.withExceptions();

            // If no exceptions specified, accept any exception
            if (withExceptions.length == 0) {
                return true;
            }

            // Check if the thrown exception matches any of the specified exception types
            for (var exceptionType : withExceptions) {
                if (exceptionType.isInstance(throwable)) {
                    return true;
                }
            }

            return false;
        }
    }
}