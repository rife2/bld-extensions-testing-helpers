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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith({RandomStringResolver.class, MockitoExtension.class})
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.AvoidAccessibilityAlteration"})
class RandomStringResolverTests {
    @Nested
    @DisplayName("Field Injection Integration")
    class FieldInjection {
        @Test
        @DisplayName("should allow access to injected value even if field is private")
        void allowInjectedValueOnPrivateField() throws Exception {
            class TestClass {
                @RandomString(length = 7)
                private String field;

                String getField() {
                    return field;
                }
            }
            var testInstance = new TestClass();

            var resolver = new RandomStringResolver();
            resolver.postProcessTestInstance(testInstance, null);

            // Access via getter, since accessibility guarantees are JVM dependent
            var value = testInstance.getField();
            assertNotNull(value);
            assertEquals(7, value.length());
        }

        @Test
        @DisplayName("should inject random string into private field")
        void injectRandomStringField() throws Exception {
            class TestClass {
                @RandomString(length = 8, characters = TestingUtils.UPPERCASE_CHARACTERS)
                private String injected;

                String getInjected() {
                    return injected;
                }
            }
            var testInstance = new TestClass();
            var resolver = new RandomStringResolver();

            resolver.postProcessTestInstance(testInstance, null);

            assertNotNull(testInstance.getInjected());
            assertEquals(8, testInstance.getInjected().length());
            assertTrue(testInstance.getInjected().matches("[A-Z]+"), "Injected field: " + testInstance.getInjected());
        }

        @Test
        @DisplayName("should inject random string into multiple annotated fields including inherited ones")
        void injectRandomStringMultipleAndInheritedFields() throws Exception {
            class SubClass extends SuperClass {
                @RandomString(length = 12)
                private String subField;

                String getSubField() {
                    return subField;
                }
            }
            var testInstance = new SubClass();
            var resolver = new RandomStringResolver();

            resolver.postProcessTestInstance(testInstance, null);

            assertNotNull(testInstance.getSubField());
            assertEquals(12, testInstance.getSubField().length());
            assertTrue(testInstance.getSubField().matches("[A-Za-z0-9]+"),
                    "Sub field: " + testInstance.getSubField());

            assertNotNull(testInstance.getSuperField());
            assertEquals(5, testInstance.getSuperField().length());
            assertTrue(testInstance.getSuperField().matches("[0-9]+"),
                    "Super field: " + testInstance.getSuperField());
        }

        @Test
        @DisplayName("should not inject into fields that aren't String or not annotated")
        void skipNonStringOrUnannotatedFields() throws Exception {
            class TestClass {
                @SuppressWarnings("PMD.UnusedPrivateField")
                private static final int notAString = 123;

                @RandomString
                @SuppressWarnings({"PMD.MutableStaticState"})
                public static String staticField;

                @SuppressWarnings({"unused"})
                private String fieldNotAnnotated;

                @RandomString
                private String fieldAnnotated;
            }
            var testInstance = new TestClass();
            var resolver = new RandomStringResolver();

            resolver.postProcessTestInstance(testInstance, null);

            // should inject only to annotated field
            var field = TestClass.class.getDeclaredField("fieldAnnotated");
            field.setAccessible(true);
            String injected = (String) field.get(testInstance);
            assertNotNull(injected);
            assertEquals(10, injected.length());

            // notAString and notAnnotated should remain unchanged
            var notAStringField = TestClass.class.getDeclaredField("notAString");
            notAStringField.setAccessible(true);
            assertEquals(123, notAStringField.getInt(testInstance));

            var notAnnotatedField = TestClass.class.getDeclaredField("fieldNotAnnotated");
            notAnnotatedField.setAccessible(true);
            assertNull(notAnnotatedField.get(testInstance));

            // static field should not be injected (not handled by instance processor)
            var staticField = TestClass.class.getDeclaredField("staticField");
            staticField.setAccessible(true);
            assertNull(staticField.get(null));
        }

        static class SuperClass {
            @RandomString(length = 5, characters = TestingUtils.NUMERIC_CHARACTERS)
            private String superField;

            String getSuperField() {
                return superField;
            }
        }
    }

    @Nested
    @DisplayName("Method-Level Annotation Integration")
    class MethodLevelAnnotation {
        @RepeatedTest(3)
        @DisplayName("should inject custom character set from method-level annotation")
        @RandomString(length = 8, characters = TestingUtils.UPPERCASE_CHARACTERS)
        void injectCustomCharactersFromMethodLevel(String randomStr) {
            assertNotNull(randomStr);
            assertEquals(8, randomStr.length());
            assertTrue(randomStr.matches("[A-Z]+"), "Result: " + randomStr);
        }

        @RepeatedTest(3)
        @DisplayName("should inject string from method-level annotation")
        @RandomString(length = 5)
        void injectFromMethodLevelAnnotation(String randomStr) {
            assertNotNull(randomStr);
            assertEquals(5, randomStr.length());
            assertTrue(randomStr.matches("[A-Za-z0-9]+"), "Result: " + randomStr);
        }

        @RepeatedTest(3)
        @DisplayName("should apply method-level annotation to all String parameters")
        @RandomString(length = 6, characters = "ABC123")
        void injectToMultipleParameters(String first, String second) {
            assertNotNull(first);
            assertEquals(6, first.length());
            assertTrue(first.matches("[ABC123]+"), "First result: " + first);

            assertNotNull(second);
            assertEquals(6, second.length());
            assertTrue(second.matches("[ABC123]+"), "Second result: " + second);

            // Should be different strings
            assertNotEquals(first, second);
        }

        @RepeatedTest(3)
        @DisplayName("should use method-level characters with parameter-level length override")
        @RandomString(characters = "XYZ")
        void mixedAnnotationAttributes(String methodChars, @RandomString(length = 15) String mixedConfig) {
            assertNotNull(methodChars);
            assertEquals(10, methodChars.length()); // Default length from method annotation
            assertTrue(methodChars.matches("[XYZ]+"), "Method chars result: " + methodChars);

            assertNotNull(mixedConfig);
            assertEquals(15, mixedConfig.length()); // Override length, but uses default characters
            assertTrue(mixedConfig.matches("[A-Za-z0-9]+"), "Mixed config result: " + mixedConfig);
        }

        @RepeatedTest(3)
        @DisplayName("should allow parameter-level annotation to override method-level")
        @RandomString(length = 11)
        void parameterOverridesMethodLevel(String methodDefault, @RandomString(length = 3) String paramOverride) {
            assertNotNull(methodDefault);
            assertEquals(11, methodDefault.length());
            assertTrue(methodDefault.matches("[A-Za-z0-9]+"), "Method default result: " + methodDefault);

            assertNotNull(paramOverride);
            assertEquals(3, paramOverride.length());
            assertTrue(paramOverride.matches("[A-Za-z0-9]+"),
                    "Parameter override result: " + paramOverride);
        }
    }

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
        @Mock
        private Method testMethod;

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

            var result = extension.supportsParameter(parameterContext, extensionContext);

            assertFalse(result);
        }

        @Test
        @DisplayName("should not support String parameters without RandomString annotation")
        void notSupportStringParametersWithoutAnnotation() {
            var extension = new RandomStringResolver();

            when(parameterContext.getParameter()).thenReturn(parameter);
            doReturn(String.class).when(parameter).getType();
            when(parameterContext.isAnnotated(RandomString.class)).thenReturn(false);
            when(extensionContext.getTestMethod()).thenReturn(Optional.empty());

            var result = extension.supportsParameter(parameterContext, extensionContext);

            assertFalse(result);
        }

        @Test
        @DisplayName("should prioritize parameter annotation over method annotation")
        void prioritizeParameterAnnotationOverMethod() {
            var extension = new RandomStringResolver();
            var parameterAnnotation = createMockAnnotation(3, "ABC");

            when(parameterContext.findAnnotation(RandomString.class)).thenReturn(Optional.of(parameterAnnotation));

            // No need to mock method-level annotation since parameter annotation takes precedence
            // and the resolver won't even check the method annotation

            var result = (String) extension.resolveParameter(parameterContext, extensionContext);

            assertNotNull(result);
            assertEquals(3, result.length()); // Parameter annotation length
            assertTrue(result.matches("[ABC]+"), "Result: " + result); // Parameter annotation characters
        }

        @Test
        @DisplayName("should resolve parameter with custom annotation values")
        void resolveParameterWithCustomAnnotation() {
            var extension = new RandomStringResolver();
            var annotation = createMockAnnotation(5, "XYZ");

            when(parameterContext.findAnnotation(RandomString.class)).thenReturn(Optional.of(annotation));

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
        @DisplayName("should resolve parameter with fallback when no annotations found")
        void resolveParameterWithFallback() {
            var extension = new RandomStringResolver();

            when(parameterContext.findAnnotation(RandomString.class))
                    .thenReturn(Optional.empty());
            when(extensionContext.getTestMethod()).thenReturn(Optional.of(testMethod));
            when(testMethod.getAnnotation(RandomString.class)).thenReturn(null);

            var result = (String) extension.resolveParameter(parameterContext, extensionContext);

            assertNotNull(result);
            assertEquals(10, result.length());
            assertTrue(result.matches("[A-Za-z0-9]+"), "Result: " + result);
        }

        @Test
        @DisplayName("should resolve parameter with fallback when test method is not present")
        void resolveParameterWithFallbackWhenNoTestMethod() {
            var extension = new RandomStringResolver();

            when(parameterContext.findAnnotation(RandomString.class))
                    .thenReturn(Optional.empty());
            when(extensionContext.getTestMethod()).thenReturn(Optional.empty());

            var result = (String) extension.resolveParameter(parameterContext, extensionContext);

            assertNotNull(result);
            assertEquals(10, result.length());
            assertTrue(result.matches("[A-Za-z0-9]+"), "Result: " + result);
        }

        @Test
        @DisplayName("should resolve parameter with method-level annotation when no parameter annotation")
        void resolveParameterWithMethodLevelAnnotation() {
            var extension = new RandomStringResolver();
            var methodAnnotation = createMockAnnotation(7, "123");

            when(parameterContext.findAnnotation(RandomString.class))
                    .thenReturn(Optional.empty());
            when(extensionContext.getTestMethod()).thenReturn(Optional.of(testMethod));
            when(testMethod.getAnnotation(RandomString.class)).thenReturn(methodAnnotation);

            var result = (String) extension.resolveParameter(parameterContext, extensionContext);

            assertNotNull(result);
            assertEquals(7, result.length());
            assertTrue(result.matches("[123]+"), "Result: " + result);
        }

        @Test
        @DisplayName("should support String parameters with RandomString annotation")
        void supportStringParametersWithAnnotation() {
            var extension = new RandomStringResolver();

            when(parameterContext.getParameter()).thenReturn(parameter);
            doReturn(String.class).when(parameter).getType();
            when(parameterContext.isAnnotated(RandomString.class)).thenReturn(true);

            var result = extension.supportsParameter(parameterContext, extensionContext);

            assertTrue(result);
        }

        @Test
        @DisplayName("should support String parameters with method-level RandomString annotation")
        void supportStringParametersWithMethodLevelAnnotation() {
            var extension = new RandomStringResolver();

            when(parameterContext.getParameter()).thenReturn(parameter);
            doReturn(String.class).when(parameter).getType();
            when(parameterContext.isAnnotated(RandomString.class)).thenReturn(false);
            when(extensionContext.getTestMethod()).thenReturn(Optional.of(testMethod));
            when(testMethod.isAnnotationPresent(RandomString.class)).thenReturn(true);

            var result = extension.supportsParameter(parameterContext, extensionContext);

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