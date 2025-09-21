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

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a test method for retry on failure.
 * <p>
 * When a test fails, it will be retried up to the specified number of times.
 * This annotation automatically includes the RetryExtension, so no additional
 * {@code @ExtendWith} annotation is required.
 *
 * <p>Usage example:
 * <pre>
 * {@code
 * @RetryTest(3)
 * void unstableTest() {
 *     // Test code that might fail intermittently
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(RetryExtension.class)
public @interface RetryTest {
    /**
     * Optional name for the test template. If not specified,
     * a default name will be generated.
     *
     * @return the display name for the retry test template
     */
    String name() default "";

    /**
     * The maximum number of retry attempts for a failing test.
     * <p>
     * The test will be executed at most (value + 1) times in total.
     *
     * @return the number of retry attempts. Must be greater than 0
     */
    int value() default 3;
}

