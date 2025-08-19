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
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Parameter resolver for the {@link RandomString} annotation.
 *
 * <h3>Usage examples:</h3>
 *
 * <pre>{@code @ExtendWith(RandomStringResolver.class)
 * class MyTest {
 *     // Default: 10 alphanumeric characters
 *     @Test
 *     void test(@RandomString String randomStr) { ... }
 *
 *     // Custom length and characters
 *     @Test
 *     void test(@RandomString(length = 8, characters = "ABC123") String hexStr) { ... }
 *
 *     // Multiple parameters
 *     @Test
 *     void test(@RandomString String str1, @RandomString(length = 5) String str2) { ... }
 * }}</pre>
 *
 * <h3>Security:</h3>
 * <p>
 * Uses {@link java.security.SecureRandom SecureRandom} for cryptographically strong random number generation, making
 * it suitable for generating test data that mimics security-sensitive strings like passwords, tokens, or API keys.
 *
 * <h3>Default Configuration:</h3>
 * <ul>
 *   <li><strong>Length:</strong> 10 characters</li>
 *   <li><strong>Character Set:</strong> Alphanumeric (A-Z, a-z, 0-9)</li>
 * </ul>
 *
 * <h3>Thread Safety:</h3>
 * <p>
 * This extension is thread-safe and can be used in parallel test execution. The underlying
 * {@link java.security.SecureRandom SecureRandom} instance is thread-safe.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see ParameterResolver
 * @see RandomString
 * @since 1.0
 */
public class RandomStringResolver implements ParameterResolver {
    /**
     * Resolves the parameter by generating a random string based on the annotation configuration.
     *
     * @param parameterContext information about the parameter to be resolved
     * @param extensionContext the current extension context
     * @return a randomly generated string matching the annotation specifications
     * @throws IllegalArgumentException if the annotation specifies invalid parameters
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == String.class
                && parameterContext.isAnnotated(RandomString.class);
    }

    /**
     * Determines if this extension can resolve the given parameter.
     *
     * @param parameterContext information about the parameter to be resolved
     * @param extensionContext the current extension context
     * @return {@code true} if the parameter is a String annotated with {@link RandomString @RandomString}
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        var annotation = parameterContext.findAnnotation(RandomString.class).orElse(null);

        if (annotation == null) {
            return TestingUtils.generateRandomString(10, TestingUtils.ALPHANUMERIC_CHARACTERS);
        }

        return TestingUtils.generateRandomString(annotation.length(), annotation.characters());
    }

}

