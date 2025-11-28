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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import java.lang.reflect.Modifier;

/**
 * Parameter and field resolver for the {@link RandomString} annotation.
 * <p>
 * This resolver automatically injects random string values into test method parameters that are annotated with
 * {@code @RandomString} or are part of test methods annotated with {@code @RandomString} at the method level.
 *
 * <h3>Resolution Priority:</h3>
 * <p>
 * Parameter-level annotation takes precedence over method-level.
 *
 * <h3>Security:</h3>
 * <p>
 * Uses {@link java.security.SecureRandom} for cryptographically strong random number generation.
 *
 * <h3>Default Configuration:</h3>
 * <p>
 * <ul>
 *   <li><strong>Length:</strong> 10 characters</li>
 *   <li><strong>Character Set:</strong> Alphanumeric (A-Z, a-z, 0-9)</li>
 * </ul>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see ParameterResolver
 * @see RandomString
 * @see TestInstancePostProcessor
 * @since 1.0
 */
public class RandomStringResolver implements ParameterResolver, TestInstancePostProcessor {
    /**
     * Processes fields of the test instance annotated with {@link RandomString}.
     * <p>
     * Enables field injection for random strings. Field must be of type {@code String}.
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
                if (field.getType() == String.class && field.isAnnotationPresent(RandomString.class)) {
                    var annotation = field.getAnnotation(RandomString.class);
                    var randomValue = TestingUtils.generateRandomString(annotation.length(), annotation.characters());
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
     * Supports String parameters annotated with {@link RandomString} at parameter or method level.
     *
     * @param parameterContext information about the parameter to be resolved
     * @param extensionContext the current extension context
     * @return {@code true} if the parameter can be resolved by this extension, {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        if (parameterContext.getParameter().getType() != String.class) {
            return false;
        }
        if (parameterContext.isAnnotated(RandomString.class)) {
            return true;
        }
        var testMethod = extensionContext.getTestMethod();
        return testMethod.isPresent() && testMethod.get().isAnnotationPresent(RandomString.class);
    }

    /**
     * Resolves the parameter by generating a random string based on annotation configuration.
     * <p>
     * Priority: Parameter-level > Method-level > Default (10, alphanumeric).
     *
     * @param parameterContext information about the parameter to be resolved
     * @param extensionContext the current extension context
     * @return a randomly generated string matching the annotation specifications
     * @throws IllegalArgumentException if the annotation specifies invalid parameters
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        var parameterAnnotation = parameterContext.findAnnotation(RandomString.class);
        if (parameterAnnotation.isPresent()) {
            var annotation = parameterAnnotation.get();
            return TestingUtils.generateRandomString(annotation.length(), annotation.characters());
        }
        var testMethod = extensionContext.getTestMethod();
        if (testMethod.isPresent()) {
            var methodAnnotation = testMethod.get().getAnnotation(RandomString.class);
            if (methodAnnotation != null) {
                return TestingUtils.generateRandomString(methodAnnotation.length(), methodAnnotation.characters());
            }
        }
        return TestingUtils.generateRandomString();
    }
}