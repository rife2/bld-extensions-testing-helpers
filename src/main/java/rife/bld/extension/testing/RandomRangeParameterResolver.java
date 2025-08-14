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
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.security.SecureRandom;

/**
 * Parameter resolver for the {@link RandomRange} annotation.
 * <p>
 * This resolver automatically injects random integer values into test method parameters
 * that are annotated with {@code @RandomRange}.
 * </p>
 *
 * <p>To use this resolver, register it with your test class:</p>
 * <pre>{@code
 * @ExtendWith(RandomRangeParameterResolver.class)
 * public class MyTest {
 *     @Test
 *     public void testMethod(@RandomRange(min = 1, max = 100) int randomValue) {
 *         // Test logic using randomValue
 *     }
 * }
 * }</pre>
 *
 * <p>The resolver validates that:</p>
 * <ul>
 *   <li>The parameter is annotated with {@code @RandomRange}</li>
 *   <li>The parameter type is {@code int}</li>
 *   <li>The minimum value is not greater than the maximum value</li>
 * </ul>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. THauvin</a>
 * @see RandomRange
 * @see ParameterResolver
 * @since 1.0
 */
public class RandomRangeParameterResolver implements ParameterResolver {
    private static final SecureRandom secureRandom = new SecureRandom();

    private int generateRandomInt(int min, int max) {
        return secureRandom.nextInt(max - min + 1) + min;
    }

    /**
     * Determines if this resolver can resolve a parameter.
     * <p>
     * This method returns {@code true} if the parameter is annotated with {@link RandomRange}
     * and is of type {@code int}.
     * </p>
     *
     * @param parameterContext the context for the parameter to be resolved
     * @param extensionContext the extension context for the test being executed
     * @return {@code true} if this resolver can resolve the parameter, {@code false} otherwise
     * @throws ParameterResolutionException if an error occurs while determining support
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.isAnnotated(RandomRange.class)
                && parameterContext.getParameter().getType() == int.class;
    }

    /**
     * Resolves a parameter by generating a random integer within the specified range.
     * <p>
     * This method extracts the {@code min} and {@code max} values from the {@link RandomRange}
     * annotation and generates a random integer within that range (inclusive of both bounds).
     * </p>
     *
     * @param parameterContext the context for the parameter to be resolved
     * @param extensionContext the extension context for the test being executed
     * @return a random integer within the specified range
     * @throws ParameterResolutionException if the annotation is missing, or if min > max
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext)
            throws ParameterResolutionException {
        RandomRange annotation = parameterContext.findAnnotation(RandomRange.class).orElse(null);
        if (annotation == null) {
            throw new ParameterResolutionException("RandomRange annotation not found");
        }

        int min = annotation.min();
        int max = annotation.max();

        if (min > max) {
            throw new ParameterResolutionException(
                    String.format("Min value (%d) cannot be greater than max value (%d)", min, max)
            );
        }

        return generateRandomInt(min, max);
    }
}

