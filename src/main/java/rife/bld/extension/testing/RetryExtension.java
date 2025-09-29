/*
 * Copyright 2025 the original author or authors.
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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * JUnit extension that provides retry functionality for failing tests.
 * <p>
 * This extension implements {@link TestTemplateInvocationContextProvider} to create
 * multiple invocations of a test method when failures occur.
 * <p>
 * The extension works by:
 * <ol>
 *   <li>Detecting methods annotated with {@link RetryTest @RetryTest}</li>
 *   <li>Creating multiple test invocation contexts</li>
 *   <li>Tracking failures and successes across invocations</li>
 *   <li>Optionally waiting between retry attempts</li>
 *   <li>Stopping retries when a test passes or max retries are reached</li>
 * </ol>
 *
 * <p>
 * Usage: Apply the {@link RetryTest @RetryTest} annotation to test methods that should be retried on failure.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see RetryTest
 * @since 1.0
 */
public class RetryExtension implements TestTemplateInvocationContextProvider {
    /**
     * Store key for tracking the maximum number of attempts in the extension context.
     */
    private static final String MAX_ATTEMPTS_KEY = "maxAttempts";

    /**
     * Store key for tracking whether the test has passed in any previous attempt.
     */
    private static final String TEST_PASSED_KEY = "testPassed";

    /**
     * Store key for tracking the delay time in seconds between retry attempts.
     */
    private static final String DELAY_SECONDS_KEY = "delaySeconds";

    /**
     * Determines if this extension supports the given test context.
     * <p>
     * Returns {@code true} if the test method is annotated with {@link RetryTest @RetryTest}.
     *
     * @param context the extension context for the test
     * @return {@code true} if the test method is annotated with {@link RetryTest @RetryTest}, {@code false} otherwise
     */
    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return AnnotationSupport.isAnnotated(context.getTestMethod(), RetryTest.class);
    }

    /**
     * Provides invocation contexts for retry attempts.
     * <p>
     * Creates a stream of invocation contexts based on the retry count specified
     * in the {@link RetryTest @RetryTest} annotation.
     *
     * @param context the extension context for the test
     * @return a stream of {@link TestTemplateInvocationContext} for each retry attempt
     */
    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        var retryTest = AnnotationSupport.findAnnotation(context.getTestMethod(), RetryTest.class)
                .orElseThrow(() -> new IllegalStateException("@RetryTest annotation not found"));

        var maxAttempts = retryTest.value() + 1; // +1 for the initial attempt
        var testName = retryTest.name().isEmpty() ? context.getDisplayName() : retryTest.name();
        var delaySeconds = retryTest.delay();

        // Initialize context store values
        var store = context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestMethod()));
        store.put(MAX_ATTEMPTS_KEY, maxAttempts);
        store.put(TEST_PASSED_KEY, false);
        store.put(DELAY_SECONDS_KEY, delaySeconds);

        return IntStream.rangeClosed(1, maxAttempts)
                .mapToObj(attempt -> new RetryInvocationContext(testName, attempt, maxAttempts, delaySeconds));
    }

    /**
     * Internal class representing a single invocation context for a retry attempt.
     * <p>
     * Each instance represents one execution attempt of the test method.
     */
    private static class RetryInvocationContext implements TestTemplateInvocationContext {
        /**
         * The current attempt number (1-based).
         */
        private final int currentAttempt;
        /**
         * The display name for this invocation.
         */
        private final String displayName;
        /**
         * The maximum number of attempts allowed.
         */
        private final int maxAttempts;
        /**
         * The number of seconds to wait before retry attempts.
         */
        private final int delaySeconds;

        /**
         * Creates a new retry invocation context.
         *
         * @param testName       the base name of the test
         * @param currentAttempt the current attempt number (1-based)
         * @param maxAttempts    the maximum number of attempts
         * @param delaySeconds   the number of seconds to wait before retry attempts
         */
        public RetryInvocationContext(String testName, int currentAttempt, int maxAttempts, int delaySeconds) {
            this.displayName = String.format("%s (attempt %d/%d)", testName, currentAttempt, maxAttempts);
            this.currentAttempt = currentAttempt;
            this.maxAttempts = maxAttempts;
            this.delaySeconds = delaySeconds;
        }

        /**
         * Returns the display name for this invocation attempt.
         *
         * @return formatted string showing the attempt number
         */
        @Override
        public String getDisplayName(int invocationIndex) {
            return displayName;
        }

        /**
         * Provides additional extensions for this invocation.
         * <p>
         * Returns the retry exception handler that manages failure logic.
         *
         * @return list containing the retry exception handler
         */
        @Override
        public List<org.junit.jupiter.api.extension.Extension> getAdditionalExtensions() {
            return List.of(new RetryExceptionHandler(currentAttempt, maxAttempts, delaySeconds));
        }
    }

    /**
     * Exception handler that manages retry logic for failed test attempts
     * <p>
     * This handler decides whether to retry a test or let the failure propagate
     * based on the current attempt number and whether previous attempts succeeded.
     * If configured, it will wait for a specified duration before allowing a retry.
     *
     * @param currentAttempt The current attempt number for this invocation.
     * @param maxAttempts    The maximum number of attempts allowed.
     * @param delaySeconds   The number of seconds to wait before retry attempts.
     */
    private record RetryExceptionHandler(int currentAttempt, int maxAttempts, int delaySeconds)
            implements TestExecutionExceptionHandler {
        /**
         * Handles exceptions thrown during test execution.
         * <p>
         * Decides whether to suppress the exception (for retry) or let it propagate.
         * If suppressing for retry and a delay time is configured, waits before returning.
         *
         * @param context   the extension context
         * @param throwable the exception that was thrown
         * @throws Throwable the original exception if no more retries are available
         */
        @Override
        @SuppressWarnings({"PMD.SystemPrintln", "PMD.DoNotUseThreads", "PMD.AvoidThrowingRawExceptionTypes"})
        public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
            var store = context.getStore(
                    ExtensionContext.Namespace.create(
                            RetryExtension.class,
                            context.getRequiredTestMethod()
                    ));
            var testPassed = (Boolean) store.get(TEST_PASSED_KEY);

            // If the test passed in a previous attempt, don't execute remaining attempts
            if (Boolean.TRUE.equals(testPassed)) {
                return;
            }

            // If this is the last attempt, let the exception propagate
            if (currentAttempt >= maxAttempts) {
                throw throwable;
            }

            // Otherwise, suppress the exception to allow retrying
            // Log the failure for debugging purposes
            System.err.printf("Test failed on attempt %d/%d: %s%n",
                    currentAttempt, maxAttempts, throwable.getMessage());

            // Wait before the next retry if configured
            if (delaySeconds > 0) {
                try {
                    System.err.printf("Waiting %d second(s) before retry...%n", delaySeconds);
                    Thread.sleep(delaySeconds * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry delay was interrupted", e);
                }
            }
        }
    }
}