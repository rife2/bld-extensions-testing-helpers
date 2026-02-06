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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * JUnit 6 extension that implements the @CouldFail annotation behavior.
 *
 * <p>This extension intercepts test failures and checks if they match the
 * specified exception types. If so, the test is aborted (shown as skipped).
 * If not, the test fails normally.</p>
 *
 * <h2>Registration</h2>
 *
 * <h3>Declarative Registration</h3>
 * <p>Register on test class using {@code @ExtendWith}:</p>
 * <pre>{@code
 * @ExtendWith(CouldFailExtension.class)
 * class MyTest {
 *     @Test
 *     @CouldFail
 *     void testFlakeyOperation() { }
 * }
 * }</pre>
 *
 * <h3>Automatic Registration</h3>
 * <p>Register globally via {@code junit-platform.properties}:</p>
 * <pre>
 * junit.jupiter.extensions.autodetection.enabled=true
 * </pre>
 * <p>Then create file {@code META-INF/services/org.junit.jupiter.api.extension.Extension}:</p>
 * <pre>
 * rife.bld.extension.testing.CouldFailExtension
 * </pre>
 *
 * <h2>Execution Flow</h2>
 * <ol>
 *   <li>Test executes normally</li>
 *   <li>If test passes, extension does nothing</li>
 *   <li>If test fails, {@code handleTestExecutionException} is called:
 *     <ol>
 *       <li>Extension checks for {@code @CouldFail} on method, then class</li>
 *       <li>If annotation not found, test fails normally</li>
 *       <li>If {@code withExceptions} is empty, accept any exception</li>
 *       <li>If {@code withExceptions} is specified, check if thrown exception matches</li>
 *       <li>If exception is accepted: throw {@code TestAbortedException}</li>
 *       <li>If exception is not accepted: re-throw original exception</li>
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
 * @see CouldFail
 * @see TestExecutionExceptionHandler
 * @since 1.0
 */
public class CouldFailExtension implements TestExecutionExceptionHandler {

    private static final Logger LOGGER = Logger.getLogger(CouldFailExtension.class.getName());

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

        // Check if thrown exception matches any of the specified exception types
        for (var exceptionType : withExceptions) {
            if (exceptionType.isInstance(throwable)) {
                return true;
            }
        }

        return false;
    }
}
