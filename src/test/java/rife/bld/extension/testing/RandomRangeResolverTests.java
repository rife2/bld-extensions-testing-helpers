package rife.bld.extension.testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UseUtilityClass", "PMD.AvoidAccessibilityAlteration"})
class RandomRangeResolverTests {
    // Fake methods for extracting real Parameter objects
    @SuppressWarnings({"EmptyMethod", "unused"})
    static void intPrimitiveParamMethod(int param) {
        // no-op
    }

    @SuppressWarnings({"EmptyMethod", "unused"})
    static void integerParamMethod(Integer param) {
        // no-op
    }

    @SuppressWarnings({"EmptyMethod", "unused"})
    @RandomRange(min = 1, max = 2)
    static void sampleMethod(int v) {
        // no-op
    }

    @SuppressWarnings({"EmptyMethod", "unused"})
    static void stringParamMethod(String param) {
        // no-op
    }

    @Nested
    @DisplayName("Edge Cases and Full Branches")
    class EdgeCases {
        @Test
        void generateRandomValueThrowsIfMinEqualsMaxIsValid() {
            var resolver = new RandomRangeResolver();
            var annotation = new RandomRange() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return RandomRange.class;
                }

                @Override
                public int max() {
                    return 7;
                }

                @Override
                public int min() {
                    return 7;
                }
            };
            // Should not throw, 7 is a valid value
            var value = invokeGenerateRandomValue(resolver, annotation);
            assertInstanceOf(Integer.class, value);
            assertEquals(7, value);
        }

        // Use reflection to invoke the private method for coverage
        @SuppressWarnings({"PMD.AvoidThrowingRawExceptionTypes", "PMD.AvoidAccessibilityAlteration"})
        private Object invokeGenerateRandomValue(RandomRangeResolver resolver, RandomRange ann) {
            try {
                var m = RandomRangeResolver.class.getDeclaredMethod("generateRandomValue", RandomRange.class);
                m.setAccessible(true);
                return m.invoke(resolver, ann);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    class FieldAnnotationTests {
        @RandomRange(min = 100, max = 101)
        @SuppressWarnings("unused")
        private static int staticRandomField;

        @RandomRange(min = 21, max = 23)
        @SuppressWarnings("unused")
        int randomField;

        @Nested
        @DisplayName("Field Annotation Coverage")
        class FieldAnnotationCoverage {
            @Mock
            ExtensionContext extensionContext;
            @Mock
            ParameterContext parameterContext;

            public FieldAnnotationCoverage() {
                MockitoAnnotations.openMocks(this);
            }

            @Test
            void fieldAnnotationCanBeReadAndHasCorrectValues() throws NoSuchFieldException {
                var fieldAnn = getFieldRandomRangeAnnotation("randomField");
                assertNotNull(fieldAnn);
                assertEquals(21, fieldAnn.min());
                assertEquals(23, fieldAnn.max());

                var staticFieldAnn = getFieldRandomRangeAnnotation("staticRandomField");
                assertNotNull(staticFieldAnn);
                assertEquals(100, staticFieldAnn.min());
                assertEquals(101, staticFieldAnn.max());
            }

            @Test
            void generateRandomValueWorksWithFieldAnnotation()
                    throws NoSuchMethodException, NoSuchFieldException, InvocationTargetException, IllegalAccessException {
                var fieldAnn = getFieldRandomRangeAnnotation("randomField");
                var resolver = new RandomRangeResolver();

                // Use reflection to access the private method
                var m = RandomRangeResolver.class.getDeclaredMethod("generateRandomValue", RandomRange.class);
                m.setAccessible(true);
                var value = m.invoke(resolver, fieldAnn);

                assertInstanceOf(Integer.class, value);
                int v = (Integer) value;
                assertTrue(v >= 21 && v <= 23);
            }

            private RandomRange getFieldRandomRangeAnnotation(String fieldName) throws NoSuchFieldException {
                var field = FieldAnnotationTests.class.getDeclaredField(fieldName);
                return field.getAnnotation(RandomRange.class);
            }

            @Test
            void supportsParameterReturnsFalseForFieldAnnotationOnly() throws NoSuchMethodException {
                // Initialize mocks for this test
                MockitoAnnotations.openMocks(this);
                // Field-level annotations not supported for parameter resolution by the resolver.
                // Only method and parameter-level annotations are considered.
                var intParam = RandomRangeResolverTests.class
                        .getDeclaredMethod("intPrimitiveParamMethod", int.class)
                        .getParameters()[0];

                when(parameterContext.getParameter()).thenReturn(intParam);
                when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(false);
                when(extensionContext.getTestMethod()).thenReturn(Optional.empty());

                var resolver = new RandomRangeResolver();
                assertFalse(resolver.supportsParameter(parameterContext, extensionContext));
            }
        }
    }

    @Nested
    @DisplayName("postProcessTestInstance Coverage")
    class PostProcessTestInstanceCoverage {
        @Test
        @DisplayName("injects using default min/max if not specified")
        void injectsDefaultRangeWhenNotSpecified() throws Exception {
            class TestClass {
                @RandomRange
                private int field;

                int getField() {
                    return field;
                }
            }
            var testInstance = new TestClass();
            var resolver = new RandomRangeResolver();

            resolver.postProcessTestInstance(testInstance, null);

            var value = testInstance.getField();
            assertTrue(value >= 0 && value <= 100);
        }

        @Test
        @DisplayName("injects random int into multiple and inherited fields")
        void injectsMultipleAndInheritedFields() throws Exception {
            class BaseClass {
                @RandomRange(min = 20, max = 30)
                private int baseField;

                int getBaseField() {
                    return baseField;
                }
            }

            class ChildClass extends BaseClass {
                @RandomRange(min = 1, max = 2)
                private int childField;

                int getChildField() {
                    return childField;
                }
            }

            var testInstance = new ChildClass();
            var resolver = new RandomRangeResolver();

            resolver.postProcessTestInstance(testInstance, null);

            var childVal = testInstance.getChildField();
            var baseVal = testInstance.getBaseField();
            assertTrue(childVal >= 1 && childVal <= 2, "childField: " + childVal);
            assertTrue(baseVal >= 20 && baseVal <= 30, "baseField: " + baseVal);
        }

        @Test
        @DisplayName("inject random int into Integer field")
        void injectsRandomIntIntoIntegerField() throws Exception {
            class TestClass {
                @RandomRange(min = 10, max = 15)
                private Integer field;
            }
            var testInstance = new TestClass();
            var resolver = new RandomRangeResolver();

            resolver.postProcessTestInstance(testInstance, null);

            var value = testInstance.field;
            assertTrue(value >= 10 && value <= 15, "Injected value: " + value);
        }

        @Test
        @DisplayName("injects random int into private field")
        void injectsRandomIntPrivateField() throws Exception {
            class TestClass {
                @RandomRange(min = 10, max = 15)
                private int field;

                int getField() {
                    return field;
                }
            }
            var testInstance = new TestClass();
            var resolver = new RandomRangeResolver();

            resolver.postProcessTestInstance(testInstance, null);

            var value = testInstance.getField();
            assertTrue(value >= 10 && value <= 15, "Injected value: " + value);
        }

        @Test
        @DisplayName("injects value into private field even if originally inaccessible")
        void injectsValueIntoPrivateFieldRegardlessOfAccessibility() throws Exception {
            class TestClass {
                @RandomRange(min = 1, max = 2)
                private int field;

                int getField() {
                    return field;
                }
            }
            var testInstance = new TestClass();
            var resolver = new RandomRangeResolver();

            var field = TestClass.class.getDeclaredField("field");
            field.setAccessible(false);

            resolver.postProcessTestInstance(testInstance, null);

            // The field should have a value in range, regardless of accessibility state
            var value = testInstance.getField();
            assertTrue(value == 1 || value == 2, "Injected value: " + value);
        }

        @Test
        @DisplayName("injects when min equals max")
        void injectsWhenMinEqualsMax() throws Exception {
            class TestClass {
                @RandomRange(min = 5, max = 5)
                private int field;

                int getField() {
                    return field;
                }
            }
            var testInstance = new TestClass();
            var resolver = new RandomRangeResolver();

            resolver.postProcessTestInstance(testInstance, null);

            assertEquals(5, testInstance.getField());
        }

        @Test
        @DisplayName("no-op for classes with no annotated fields")
        void noAnnotatedFieldsNoOp() throws Exception {
            @SuppressWarnings("unused")
            class TestClass {
                private int a;
                private String b;
            }
            var testInstance = new TestClass();
            var resolver = new RandomRangeResolver();

            resolver.postProcessTestInstance(testInstance, null);

            assertEquals(0, testInstance.a);
            assertNull(testInstance.b);
        }

        @Test
        @DisplayName("skips static fields and non-int fields")
        void skipsStaticAndNonIntFields() throws Exception {
            class TestClass {
                @RandomRange
                @SuppressWarnings({"PMD.MutableStaticState"})
                public static int staticInt;

                @RandomRange
                private int injected;

                @SuppressWarnings("unused")
                private String notAnInt;
            }
            var testInstance = new TestClass();
            var resolver = new RandomRangeResolver();

            resolver.postProcessTestInstance(testInstance, null);

            var injectedField = TestClass.class.getDeclaredField("injected");
            injectedField.setAccessible(true);
            var injectedVal = injectedField.getInt(testInstance);
            assertTrue(injectedVal >= 0 && injectedVal <= 100);

            var staticField = TestClass.class.getDeclaredField("staticInt");
            staticField.setAccessible(true);
            assertEquals(0, staticField.getInt(null)); // Static field should not be injected

            var notAnIntField = TestClass.class.getDeclaredField("notAnInt");
            notAnIntField.setAccessible(true);
            assertNull(notAnIntField.get(testInstance));
        }

        @Test
        @DisplayName("throws if min > max")
        void throwsIfMinGreaterThanMax() {
            class TestClass {
                @RandomRange(min = 10, max = 5)
                private int failField;
            }
            var testInstance = new TestClass();
            var resolver = new RandomRangeResolver();

            var ex = assertThrows(Exception.class, () -> resolver.postProcessTestInstance(testInstance, null));
            assertTrue(ex.getMessage().toLowerCase().contains("min") || ex.getMessage().toLowerCase().contains("greater"));
        }
    }

    @Nested
    @DisplayName("Resolve Parameter Tests")
    class ResolveParameterTests {
        @Mock
        ExtensionContext extensionContext;
        @Mock
        Method method;
        @Mock
        ParameterContext parameterContext;

        public ResolveParameterTests() {
            MockitoAnnotations.openMocks(this);
        }

        private RandomRange mockAnnotation(int min, int max) {
            return new RandomRange() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return RandomRange.class;
                }

                @Override
                public int max() {
                    return max;
                }

                @Override
                public int min() {
                    return min;
                }
            };
        }

        @Test
        void throwsIfMinGreaterThanMax() {
            var paramAnn = mockAnnotation(10, 5);
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(paramAnn));
            var resolver = new RandomRangeResolver();
            assertThrows(ParameterResolutionException.class, () ->
                    resolver.resolveParameter(parameterContext, extensionContext));
        }

        @Test
        void usesDefaultRangeIfNoAnnotation() {
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.empty());
            when(extensionContext.getTestMethod()).thenReturn(Optional.of(method));
            when(method.getAnnotation(RandomRange.class)).thenReturn(null);

            var resolver = new RandomRangeResolver();
            var value = resolver.resolveParameter(parameterContext, extensionContext);
            assertInstanceOf(Integer.class, value);
            int v = (Integer) value;
            assertTrue(v >= 0 && v <= 100);
        }

        @Test
        void usesDefaultRangeIfNoTestMethod() {
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.empty());
            when(extensionContext.getTestMethod()).thenReturn(Optional.empty());

            var resolver = new RandomRangeResolver();
            var value = resolver.resolveParameter(parameterContext, extensionContext);
            assertInstanceOf(Integer.class, value);
            int v = (Integer) value;
            assertTrue(v >= 0 && v <= 100);
        }

        @Test
        void usesMethodLevelAnnotation() {
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.empty());

            var methodAnn = mockAnnotation(11, 13);
            when(extensionContext.getTestMethod()).thenReturn(Optional.of(method));
            when(method.getAnnotation(RandomRange.class)).thenReturn(methodAnn);

            var resolver = new RandomRangeResolver();
            var value = resolver.resolveParameter(parameterContext, extensionContext);
            assertInstanceOf(Integer.class, value);
            int v = (Integer) value;
            assertTrue(v >= 11 && v <= 13);
        }

        @Test
        void usesParameterLevelAnnotation() {
            var paramAnn = mockAnnotation(5, 15);
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(paramAnn));

            var resolver = new RandomRangeResolver();
            var value = resolver.resolveParameter(parameterContext, extensionContext);
            assertInstanceOf(Integer.class, value);
            int v = (Integer) value;
            assertTrue(v >= 5 && v <= 15);
        }
    }

    @Nested
    @DisplayName("Supports Parameter Tests")
    class SupportsParameterTests {
        @Mock
        ExtensionContext extensionContext;
        @Mock
        ParameterContext parameterContext;

        public SupportsParameterTests() {
            MockitoAnnotations.openMocks(this);
        }

        @Test
        void returnsFalseForNonIntType() throws Exception {
            var stringParam = RandomRangeResolverTests.class
                    .getDeclaredMethod("stringParamMethod", String.class)
                    .getParameters()[0];

            when(parameterContext.getParameter()).thenReturn(stringParam);

            var resolver = new RandomRangeResolver();
            assertFalse(resolver.supportsParameter(parameterContext, extensionContext));
        }

        @Test
        void returnsFalseIfNoAnnotationsPresent() throws Exception {
            var intParam = RandomRangeResolverTests.class
                    .getDeclaredMethod("intPrimitiveParamMethod", int.class)
                    .getParameters()[0];

            when(parameterContext.getParameter()).thenReturn(intParam);
            when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(false);

            // Use a real method without @RandomRange
            var m = RandomRangeResolverTests.class
                    .getDeclaredMethod("intPrimitiveParamMethod", int.class);
            when(extensionContext.getTestMethod()).thenReturn(Optional.of(m));

            var resolver = new RandomRangeResolver();
            assertFalse(resolver.supportsParameter(parameterContext, extensionContext));
        }

        @Test
        void returnsFalseIfNoTestMethodPresent() throws Exception {
            var intParam = RandomRangeResolverTests.class
                    .getDeclaredMethod("intPrimitiveParamMethod", int.class)
                    .getParameters()[0];

            when(parameterContext.getParameter()).thenReturn(intParam);
            when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(false);
            when(extensionContext.getTestMethod()).thenReturn(Optional.empty());

            var resolver = new RandomRangeResolver();
            assertFalse(resolver.supportsParameter(parameterContext, extensionContext));
        }

        @Test
        void returnsTrueForMethodLevelAnnotation() throws Exception {
            var intParam = RandomRangeResolverTests.class
                    .getDeclaredMethod("sampleMethod", int.class)
                    .getParameters()[0];

            when(parameterContext.getParameter()).thenReturn(intParam);
            when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(false);

            // Use the real annotated method
            var m = RandomRangeResolverTests.class
                    .getDeclaredMethod("sampleMethod", int.class);
            when(extensionContext.getTestMethod()).thenReturn(Optional.of(m));

            var resolver = new RandomRangeResolver();
            assertTrue(resolver.supportsParameter(parameterContext, extensionContext));
        }

        @Test
        void returnsTrueForParameterLevelAnnotation() throws Exception {
            var intParam = RandomRangeResolverTests.class
                    .getDeclaredMethod("intPrimitiveParamMethod", int.class)
                    .getParameters()[0];

            when(parameterContext.getParameter()).thenReturn(intParam);
            when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(true);

            var resolver = new RandomRangeResolver();
            assertTrue(resolver.supportsParameter(parameterContext, extensionContext));
        }

        @Test
        void returnsTrueForParameterLevelAnnotationWithInteger() throws Exception {
            var intParam = RandomRangeResolverTests.class
                    .getDeclaredMethod("integerParamMethod", Integer.class)
                    .getParameters()[0];

            when(parameterContext.getParameter()).thenReturn(intParam);
            when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(true);

            var resolver = new RandomRangeResolver();
            assertTrue(resolver.supportsParameter(parameterContext, extensionContext));
        }
    }
}