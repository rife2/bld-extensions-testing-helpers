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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Parameter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith({RandomStringResolver.class, MockitoExtension.class})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RandomStringResolverTests {
    @Nested
    @DisplayName("Parameter Injection Integration")
    class ParameterInjection {
        @RepeatedTest(3)
        @DisplayName("should inject custom character set string")
        void injectCustomCharacterSetString(
                @RandomString(
                        length = 8,
                        characters = TestingUtils.UPPERCASE_CHARACTERS
                ) String randomStr) {
            assertNotNull(randomStr);
            assertEquals(8, randomStr.length());
            assertTrue(randomStr.matches("[A-Z]+"), "Result: " + randomStr);
        }

        @RepeatedTest(3)
        @DisplayName("should inject custom length string")
        void injectCustomLengthString(@RandomString(length = 15) String randomStr) {
            assertNotNull(randomStr);
            assertEquals(15, randomStr.length());
            assertTrue(randomStr.matches("[A-Za-z0-9]+"), "Result: " + randomStr);
        }

        @RepeatedTest(3)
        @DisplayName("should inject default random string")
        void injectDefaultRandomString(@RandomString String randomStr) {
            assertNotNull(randomStr);
            assertEquals(10, randomStr.length());
            assertTrue(randomStr.matches("[A-Za-z0-9]+"), "Result: " + randomStr);
        }

        @RepeatedTest(3)
        @DisplayName("should inject hexadecimal string")
        void injectHexadecimalString(
                @RandomString(
                        length = 16,
                        characters = TestingUtils.HEXADECIMAL_CHARACTERS
                ) String hexStr) {
            assertNotNull(hexStr);
            assertEquals(16, hexStr.length());
            assertTrue(hexStr.matches("[0-9A-F]+"), "Result: " + hexStr);
        }

        @RepeatedTest(3)
        @DisplayName("should inject multiple different random strings")
        void injectMultipleDifferentRandomStrings(
                @RandomString(
                        length = 6,
                        characters = TestingUtils.NUMERIC_CHARACTERS
                ) String numbersOnly,
                @RandomString(length = 12) String defaultStr) {

            assertNotNull(numbersOnly);
            assertEquals(6, numbersOnly.length());
            assertTrue(numbersOnly.matches("[0-9]+"), "Result: " + numbersOnly);

            assertNotNull(defaultStr);
            assertEquals(12, defaultStr.length());
            assertTrue(defaultStr.matches("[A-Za-z0-9]+"), "Result: " + defaultStr);

            assertNotEquals(numbersOnly, defaultStr);
        }

        @RepeatedTest(3)
        @DisplayName("should inject URL-safe string")
        void injectUrlSafeString(
                @RandomString(
                        length = 20,
                        characters = TestingUtils.URL_SAFE_CHARACTERS
                ) String urlSafeStr) {
            assertNotNull(urlSafeStr);
            assertEquals(20, urlSafeStr.length());
            assertTrue(urlSafeStr.matches("[A-Za-z0-9\\-_]+"), "Result: " + urlSafeStr);
        }
    }

    @Nested
    @DisplayName("Parameter Resolution")
    class ParameterResolution {
        @Mock
        private ExtensionContext extensionContext;
        @Mock
        private Parameter parameter;
        @Mock
        private ParameterContext parameterContext;

        private RandomString createMockAnnotation(int length, String characters) {
            return new RandomString() {
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() {
                    return RandomString.class;
                }

                @Override
                public String characters() {
                    return characters;
                }

                @Override
                public int length() {
                    return length;
                }
            };
        }

        @Test
        @DisplayName("should not support non-String parameters")
        void notSupportNonStringParameters() {
            var extension = new RandomStringResolver();

            when(parameterContext.getParameter()).thenReturn(parameter);
            doReturn(Integer.class).when(parameter).getType();
            // Remove the unnecessary stubbing for isAnnotated() since it's never called

            boolean result = extension.supportsParameter(parameterContext, extensionContext);

            assertFalse(result);
        }

        @Test
        @DisplayName("should not support String parameters without RandomString annotation")
        void notSupportStringParametersWithoutAnnotation() {
            var extension = new RandomStringResolver();

            when(parameterContext.getParameter()).thenReturn(parameter);
            doReturn(String.class).when(parameter).getType();
            when(parameterContext.isAnnotated(RandomString.class)).thenReturn(false);

            boolean result = extension.supportsParameter(parameterContext, extensionContext);

            assertFalse(result);
        }

        @Test
        @DisplayName("should resolve parameter with custom annotation values")
        void resolveParameterWithCustomAnnotation() {
            var extension = new RandomStringResolver();
            var annotation = createMockAnnotation(5, "XYZ");

            when(parameterContext.findAnnotation(RandomString.class))
                    .thenReturn(Optional.of(annotation));

            var result = (String) extension.resolveParameter(parameterContext, extensionContext);

            assertNotNull(result);
            assertEquals(5, result.length());
            assertTrue(result.matches("[XYZ]+"), "Result: " + result);
        }

        @Test
        @DisplayName("should resolve parameter with default annotation values")
        void resolveParameterWithDefaultAnnotation() {
            var extension = new RandomStringResolver();
            var annotation = createMockAnnotation(10, TestingUtils.ALPHANUMERIC_CHARACTERS);

            when(parameterContext.findAnnotation(RandomString.class))
                    .thenReturn(Optional.of(annotation));

            var result = (String) extension.resolveParameter(parameterContext, extensionContext);

            assertNotNull(result);
            assertEquals(10, result.length());
            assertTrue(result.matches("[A-Za-z0-9]+"), "Result: " + result);
        }

        @Test
        @DisplayName("should resolve parameter with fallback when annotation not found")
        void resolveParameterWithFallback() {
            var extension = new RandomStringResolver();

            when(parameterContext.findAnnotation(RandomString.class))
                    .thenReturn(Optional.empty());

            var result = (String) extension.resolveParameter(parameterContext, extensionContext);

            assertNotNull(result);
            assertEquals(10, result.length());
            assertTrue(result.matches("[A-Za-z0-9]+"), "Result: " + result);
        }

        @Test
        @DisplayName("should support String parameters with RandomString annotation")
        void supportStringParametersWithAnnotation() {
            var extension = new RandomStringResolver();

            when(parameterContext.getParameter()).thenReturn(parameter);
            doReturn(String.class).when(parameter).getType();
            when(parameterContext.isAnnotated(RandomString.class)).thenReturn(true);

            boolean result = extension.supportsParameter(parameterContext, extensionContext);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Static String Generation Methods")
    class StaticMethods {
        @RepeatedTest(3)
        @DisplayName("should generate default 10-character alphanumeric string")
        void generateDefaultString() {
            var result = TestingUtils.generateRandomString();

            assertNotNull(result);
            assertEquals(10, result.length());
            assertTrue(result.matches("[A-Za-z0-9]+"), "Result: " + result);
        }

        @RepeatedTest(3)
        @DisplayName("should generate different strings on subsequent calls")
        void generateDifferentStrings() {
            var first = TestingUtils.generateRandomString(15);
            var second = TestingUtils.generateRandomString(15);

            assertNotEquals(first, second, "Generated strings should be different");
        }

        @RepeatedTest(3)
        @DisplayName("should generate string with custom length")
        void generateStringWithCustomLength() {
            var result = TestingUtils.generateRandomString(20);

            assertNotNull(result);
            assertEquals(20, result.length());
            assertTrue(result.matches("[A-Za-z0-9]+"), "Result: " + result);
        }

        @RepeatedTest(3)
        @DisplayName("should generate string with custom length and characters")
        void generateStringWithCustomLengthAndCharacters() {
            var result = TestingUtils.generateRandomString(8, "ABC123");

            assertNotNull(result);
            assertEquals(8, result.length());
            assertTrue(result.matches("[ABC123]+"), "Result: " + result);
        }

        @Test
        @DisplayName("should throw exception for empty characters")
        void throwExceptionForEmptyCharacters() {
            var exception = assertThrows(IllegalArgumentException.class,
                    () -> TestingUtils.generateRandomString(10, ""));
            assertEquals("Characters cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("should throw exception for negative length")
        void throwExceptionForNegativeLength() {
            var exception = assertThrows(IllegalArgumentException.class,
                    () -> TestingUtils.generateRandomString(-5));
            assertEquals("Length must be greater than 0", exception.getMessage());
        }

        @Test
        @DisplayName("should throw exception for null characters")
        void throwExceptionForNullCharacters() {
            var exception = assertThrows(IllegalArgumentException.class,
                    () -> TestingUtils.generateRandomString(10, null));
            assertEquals("Characters cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("should throw exception for zero length")
        void throwExceptionForZeroLength() {
            var exception = assertThrows(IllegalArgumentException.class,
                    () -> TestingUtils.generateRandomString(0));
            assertEquals("Length must be greater than 0", exception.getMessage());
        }
    }
}