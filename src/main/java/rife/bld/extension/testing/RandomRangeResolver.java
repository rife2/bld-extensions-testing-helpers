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

import org.junit.jupiter.api.extension.*;

import java.lang.reflect.Modifier;

/**
 * Parameter and field resolver for the {@link RandomRange} annotation.
 * <p>
 * This resolver automatically injects random integer values into test method parameters that are annotated with
 * {@code @RandomRange} or are part of test methods annotated with {@code @RandomRange} at the method level.
 *
 * <h3>Resolution Priority:</h3>
 * <p>
 * When both parameter-level and method-level {@code @RandomRange} annotations are present,
 * the parameter-level annotation takes precedence.
 * <p>
 * If only a method-level annotation exists, its configuration applies to all int parameters in that method.
 * <p>
 * The resolver validates that:
 * <ul>
 *   <li>The parameter is annotated with {@code @RandomRange} or the method is annotated with {@code @RandomRange}</li>
 *   <li>The parameter type is {@code int}</li>
 *   <li>The minimum value is not greater than the maximum value</li>
 * </ul>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see ParameterResolver
 * @see RandomRange
 * @since 1.0
 */
public class RandomRangeResolver implements ParameterResolver, TestInstancePostProcessor {
    /**
     * Generates a random integer value based on the annotation configuration.
     *
     * @param annotation the RandomRange annotation containing min and max values
     * @return a random integer within the specified range
     * @throws ParameterResolutionException if min > max
     */
    private static int generateRandomValue(RandomRange annotation) {
        int min = annotation.min();
        int max = annotation.max();
        if (min > max) {
            throw new ParameterResolutionException(
                    String.format("The minimum value (%d) cannot be greater than maximum value (%d)", min, max)
            );
        }
        return TestingUtils.generateRandomInt(min, max);
    }

    /**
     * Processes fields of the test instance annotated with {@link RandomRange}.
     * <p>
     * Enables field injection for random ints. The field must be of type {@code int}.
     *
     * @param testInstance the test class instance
     * @param context      the current extension context
     */
    @Override
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        for (var clazz = testInstance.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (var field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (field.getType() == int.class && field.isAnnotationPresent(RandomRange.class)) {
                    var annotation = field.getAnnotation(RandomRange.class);
                    var randomValue = TestingUtils.generateRandomInt(annotation.min(), annotation.max());
                    boolean wasAccessible = field.canAccess(testInstance);
                    field.setAccessible(true);
                    field.set(testInstance, randomValue);
                    field.setAccessible(wasAccessible);
                }
            }
        }
    }

    /**
     * Determines if this resolver can resolve a parameter.
     * <p>
     * Supports int parameters annotated with {@link RandomRange} at parameter or method level.
     *
     * @param parameterContext the context for the parameter to be resolved
     * @param extensionContext the extension context for the test being executed
     * @return {@code true} if this resolver can resolve the parameter, {@code false} otherwise
     * @throws ParameterResolutionException if an error occurs while determining support
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        if (parameterContext.getParameter().getType() != int.class) {
            return false;
        }
        return parameterContext.isAnnotated(RandomRange.class) ||
                extensionContext.getTestMethod()
                        .map(m -> m.isAnnotationPresent(RandomRange.class))
                        .orElse(false);
    }

    /**
     * Resolves a parameter by generating a random integer within the specified range.
     * <p>
     * Priority: Parameter-level > Method-level > Default (0-100, for safety).
     *
     * @param parameterContext the context for the parameter to be resolved
     * @param extensionContext the extension context for the test being executed
     * @return a random integer within the specified range
     * @throws ParameterResolutionException if {@code min} > {@code max}
     * @see TestingUtils#generateRandomInt(int, int)
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        var parameterAnnotation = parameterContext.findAnnotation(RandomRange.class);
        if (parameterAnnotation.isPresent()) {
            return generateRandomValue(parameterAnnotation.get());
        }
        var testMethod = extensionContext.getTestMethod();
        if (testMethod.isPresent()) {
            var methodAnnotation = testMethod.get().getAnnotation(RandomRange.class);
            if (methodAnnotation != null) {
                return generateRandomValue(methodAnnotation);
            }
        }
        return TestingUtils.generateRandomInt(0, 100);
    }
}