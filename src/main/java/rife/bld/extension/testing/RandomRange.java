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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JUnit annotation for generating random integer values within a specified range.
 * <p>
 * This annotation can be applied to test method parameters of type {@code int} to automatically
 * inject random values during test execution. It can also be applied at the method level to
 * configure default settings for all int parameters in that method.
 *
 * <h3>Example usage:</h3>
 *
 * <blockquote><pre>
 * &#64;ExtendWith(RandomRangeResolver.class)
 * public class MyTest {
 *     // Parameter-level annotation
 *     &#64;Test
 *     public void testMethod(&#64;RandomRange(min = 1, max = 10) int randomValue) {
 *         // randomValue will be a random integer between 1 and 10 (inclusive)
 *     }
 *
 *     // Method-level annotation for single parameter
 *     &#64;Test
 *     &#64;RandomRange(min = 5, max = 15)
 *     public void testWithMethodLevel(int randomValue) {
 *         // randomValue will be a random integer between 5 and 15 (inclusive)
 *     }
 *
 *     // Method-level annotation applies to all int parameters
 *     &#64;Test
 *     &#64;RandomRange(min = 1, max = 100)
 *     public void testMultiple(int first, int second) {
 *         // Both first and second will be random integers between 1 and 100 (inclusive)
 *     }
 *
 *     // Parameter-level annotation overrides method-level
 *     &#64;Test
 *     &#64;RandomRange(min = 1, max = 10)
 *     public void testMixed(int defaultRange, &#64;RandomRange(min = 50, max = 60) int customRange) {
 *         // defaultRange: 1-10, customRange: 50-60
 *     }
 * }</pre></blockquote>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see RandomRangeResolver
 * @since 1.0
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RandomRange {
    /**
     * The maximum value (inclusive) for the random number generation.
     *
     * @return the maximum value, defaults to 100
     */
    int max() default 100;

    /**
     * The minimum value (inclusive) for the random number generation.
     *
     * @return the minimum value, defaults to 0
     */
    int min() default 0;
}