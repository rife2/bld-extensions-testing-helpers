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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.extension.*;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parameter and field resolver for the {@link RandomRange} annotation.
 * <p>
 * This resolver automatically injects random integer values into test method parameters that are annotated with
 * {@code @RandomRange} or are part of test methods annotated with {@code @RandomRange} at the method level.
 *
 * <h3>Supported Types:</h3>
 * <p>
 * <ul>
 *   <li>{@code int} - single random integer (when size = 0)</li>
 *   <li>{@code List<Integer>} - list of random integers (when size > 0)</li>
 *   <li>{@code Set<Integer>} - set of unique random integers (when size > 0)</li>
 * </ul>
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
 *   <li>The parameter type is {@code int}, {@code List<Integer>}, or {@code Set<Integer>}</li>
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
     * Processes fields of the test instance annotated with {@link RandomRange}.
     * <p>
     * Enables field injection for random ints. The field must be of type {@code int}, {@code List<Integer>},
     * or {@code Set<Integer>}.
     *
     * @param testInstance the test class instance
     * @param context      the current extension context
     */
    @Override
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    @SuppressFBWarnings("RFI_SET_ACCESSIBLE")
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        for (var clazz = testInstance.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (var field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (field.isAnnotationPresent(RandomRange.class)) {
                    var annotation = field.getAnnotation(RandomRange.class);
                    Object randomValue;

                    if (field.getType() == int.class || field.getType() == Integer.class) {
                        randomValue = generateRandomValue(annotation);
                    } else if (field.getType() == List.class) {
                        randomValue = generateRandomIntList(annotation.size(), annotation.min(), annotation.max());
                    } else if (field.getType() == Set.class) {
                        randomValue = generateRandomIntSet(annotation.size(), annotation.min(), annotation.max());
                    } else {
                        continue;
                    }

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
        var parameterType = parameterContext.getParameter().getType();

        if (parameterType != int.class && parameterType != Integer.class &&
                parameterType != List.class && parameterType != Set.class) {
            return false;
        }

        if ((parameterType == List.class || parameterType == Set.class) &&
                !isIntegerCollection(parameterContext.getParameter().getParameterizedType())) {
            return false;
        }

        if (parameterContext.isAnnotated(RandomRange.class)) {
            return true;
        }
        var testMethod = extensionContext.getTestMethod();
        return testMethod.isPresent() && testMethod.get().isAnnotationPresent(RandomRange.class);
    }

    /**
     * Resolves a parameter by generating a random integer within the specified range.
     * <p>
     * Priority: Parameter-level > Method-level > Default (0-100, for safety).
     *
     * @param parameterContext the context for the parameter to be resolved
     * @param extensionContext the extension context for the test being executed
     * @return a random integer, list, or set within the specified range
     * @throws ParameterResolutionException if {@code min} > {@code max}
     * @see TestingUtils#generateRandomInt(int, int)
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        // Determine parameter type, defaulting to int.class for backward compatibility
        var parameter = parameterContext.getParameter();
        var parameterType = parameter != null ? parameter.getType() : int.class;

        var parameterAnnotation = parameterContext.findAnnotation(RandomRange.class);

        if (parameterAnnotation.isPresent()) {
            var annotation = parameterAnnotation.get();
            return generateValue(parameterType, annotation.size(), annotation.min(), annotation.max());
        }

        var testMethod = extensionContext.getTestMethod();
        if (testMethod.isPresent()) {
            var methodAnnotation = testMethod.get().getAnnotation(RandomRange.class);
            if (methodAnnotation != null) {
                return generateValue(parameterType, methodAnnotation.size(), methodAnnotation.min(),
                        methodAnnotation.max());
            }
        }

        return generateValue(parameterType, 0, 0, 100);
    }

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

    // Generates a list of random integers.
    private List<Integer> generateRandomIntList(int size, int min, int max) {
        var list = new ArrayList<Integer>(size);
        for (int i = 0; i < size; i++) {
            list.add(TestingUtils.generateRandomInt(min, max));
        }
        return list;
    }

    // Generates a set of unique random integers.
    private Set<Integer> generateRandomIntSet(int size, int min, int max) {
        // Calculate the range of possible values
        long range = (long) max - min + 1;

        if (size > range) {
            throw new IllegalArgumentException(
                    String.format("Cannot generate %d unique integers in range [%d, %d]. " +
                                    "Maximum possible unique integers: %d",
                            size, min, max, range));
        }

        var set = new HashSet<Integer>(size);
        int maxAttempts = size * 100; // Reasonable limit
        int attempts = 0;

        while (set.size() < size) {
            if (attempts++ > maxAttempts) {
                throw new IllegalStateException(
                        String.format("Failed to generate %d unique integers after %d attempts. " +
                                        "Consider using a larger range.",
                                size, maxAttempts));
            }
            set.add(TestingUtils.generateRandomInt(min, max));
        }
        return set;
    }

    // Generates the appropriate value based on the parameter type.
    @SuppressFBWarnings("URV_UNRELATED_RETURN_VALUES")
    private Object generateValue(Class<?> parameterType, int size, int min, int max) {
        if (min > max) {
            throw new ParameterResolutionException(
                    String.format("The minimum value (%d) cannot be greater than maximum value (%d)", min, max)
            );
        }

        if (parameterType == int.class || parameterType == Integer.class) {
            return TestingUtils.generateRandomInt(min, max);
        } else if (parameterType == List.class) {
            return generateRandomIntList(size, min, max);
        } else if (parameterType == Set.class) {
            return generateRandomIntSet(size, min, max);
        }
        throw new IllegalArgumentException("Unsupported parameter type: " + parameterType);
    }

    // Checks if a parameterized type is a collection of integers.
    private boolean isIntegerCollection(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            var typeArguments = parameterizedType.getActualTypeArguments();
            return typeArguments.length == 1 && typeArguments[0] == Integer.class;
        }
        return false;
    }
}