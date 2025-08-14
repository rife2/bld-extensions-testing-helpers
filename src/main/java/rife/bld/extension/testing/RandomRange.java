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
 * Annotation for generating random integer values within a specified range.
 * <p>
 * This annotation can be applied to test method parameters of type {@code int} to automatically
 * inject random values during test execution.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @ExtendWith(RandomRangeParameterResolver.class)
 * public class MyTest {
 *     @Test
 *     public void testMethod(@RandomRange(min = 1, max = 10) int randomValue) {
 *         // randomNum will be a random integer between 1 and 10 (inclusive)
 *     }
 * }
 * }</pre>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see RandomRangeParameterResolver
 * @since 1.0
 */
@Target(ElementType.PARAMETER)
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
