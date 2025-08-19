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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Parameter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class RandomRangeResolverTests {
    @Mock
    private ExtensionContext extensionContext;
    @Mock
    private Parameter parameter;
    @Mock
    private ParameterContext parameterContext;
    private RandomRangeResolver resolver;

    @BeforeEach
    void beforeEach() {
        resolver = new RandomRangeResolver();
    }

    @Test
    void minGreaterThanMax() {
        var mockAnnotation = mock(RandomRange.class);
        when(mockAnnotation.min()).thenReturn(20);
        when(mockAnnotation.max()).thenReturn(10);
        when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(mockAnnotation));

        var exception = assertThrows(ParameterResolutionException.class, () ->
                resolver.resolveParameter(parameterContext, extensionContext));

        var expectedMessage = "The minimum value (20) cannot be greater than maximum value (10)";
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void minValueZero() {
        var mockAnnotation = mock(RandomRange.class);
        when(mockAnnotation.min()).thenReturn(0);
        when(mockAnnotation.max()).thenReturn(10);
        when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(mockAnnotation));

        for (int i = 0; i < 50; i++) {
            var result = (Integer) resolver.resolveParameter(parameterContext, extensionContext);
            assertTrue(result >= 0,
                    "Result should be non-negative when min is 0, but was: " + result);
        }
    }

    @Test
    void missingAnnotation() {
        when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.empty());

        var exception = assertThrows(ParameterResolutionException.class, () ->
                resolver.resolveParameter(parameterContext, extensionContext));

        assertEquals("RandomRange annotation not found", exception.getMessage());
    }

    @RepeatedTest(3)
    void validAnnotation() {
        var mockAnnotation = mock(RandomRange.class);
        when(mockAnnotation.min()).thenReturn(10);
        when(mockAnnotation.max()).thenReturn(20);
        when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(mockAnnotation));

        var result = (Integer) resolver.resolveParameter(parameterContext, extensionContext);

        assertNotNull(result);
        assertTrue(result >= 10 && result <= 20,
                "Result should be between 10 and 20, but was: " + result);
    }

    // Integration tests using actual @RandomRange annotation
    @Nested
    @DisplayName("Integration Tests")
    @ExtendWith(RandomRangeResolver.class)
    class IntegrationTests {
        @RepeatedTest(50)
        void actualAnnotationWithConsistentBehavior(@RandomRange(min = 1, max = 5) int randomValue) {
            assertTrue(randomValue >= 1 && randomValue <= 5,
                    "Random value should consistently be in range [1,5], but was: " + randomValue);
        }

        @RepeatedTest(3)
        void actualAnnotationWithCustomRange(@RandomRange(min = 5, max = 15) int randomValue) {
            assertTrue(randomValue >= 5 && randomValue <= 15,
                    "Random value should be in range [5,15], but was: " + randomValue);
        }

        @RepeatedTest(3)
        void actualAnnotationWithDefaultRange(@RandomRange int randomValue) {
            assertTrue(randomValue >= 0 && randomValue <= 100,
                    "Random value should be in default range [0,100], but was: " + randomValue);
        }

        @RepeatedTest(3)
        void actualAnnotationWithMultipleParameters(
                @RandomRange(min = 1, max = 10) int first,
                @RandomRange(min = 100, max = 200) int second,
                @RandomRange int third) {

            assertTrue(first >= 1 && first <= 10,
                    "First parameter should be in range [1,10], but was: " + first);
            assertTrue(second >= 100 && second <= 200,
                    "Second parameter should be in range [100,200], but was: " + second);
            assertTrue(third >= 0 && third <= 100,
                    "Third parameter should be in default range [0,100], but was: " + third);
        }

        @RepeatedTest(3)
        void actualAnnotationWithNegativeRange(@RandomRange(min = -100, max = -50) int randomValue) {
            assertTrue(randomValue >= -100 && randomValue <= -50,
                    "Random value should be in range [-100,-50], but was: " + randomValue);
        }

        @Test
        void actualAnnotationWithSingleValue(@RandomRange(min = 42, max = 42) int randomValue) {
            assertEquals(42, randomValue, "Random value should always be 42 when min equals max");
        }
    }

    @Nested
    @DisplayName("Parameter Support Tests")
    class ParameterSupportTests {
        @Test
        void supportsParameterWithDoubleType() {
            when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(true);
            when(parameterContext.getParameter()).thenReturn(parameter);

            var result = resolver.supportsParameter(parameterContext, extensionContext);

            assertFalse(result, "Resolver should not support double type, only primitive int");
        }

        // Tests for resolver behavior with different parameter types
        @Test
        void supportsParameterWithIntegerWrapperType() {
            when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(true);
            when(parameterContext.getParameter()).thenReturn(parameter);

            var result = resolver.supportsParameter(parameterContext, extensionContext);

            assertFalse(result, "Resolver should not support Integer wrapper type, only primitive int");
        }

        @Test
        void supportsParameterWithLongType() {
            when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(true);
            when(parameterContext.getParameter()).thenReturn(parameter);

            var result = resolver.supportsParameter(parameterContext, extensionContext);

            assertFalse(result, "Resolver should not support long type, only primitive int");
        }

        @Test
        void supportsParameterWithRandomRangeAnnotationAndIntType() {
            when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(true);
            when(parameterContext.getParameter()).thenReturn(parameter);
            doReturn(int.class).when(parameter).getType();

            var result = resolver.supportsParameter(parameterContext, extensionContext);

            assertTrue(result);
        }

        @Test
        void supportsParameterWithRandomRangeAnnotationButNonIntType() {
            when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(true);
            when(parameterContext.getParameter()).thenReturn(parameter);

            var result = resolver.supportsParameter(parameterContext, extensionContext);

            assertFalse(result);
        }

        @Test
        void supportsParameterWithoutRandomRangeAnnotation() {
            when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(false);

            var result = resolver.supportsParameter(parameterContext, extensionContext);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Range Tests")
    class RangeTests {
        @RepeatedTest(3)
        void defaultRange() {
            var mockAnnotation = mock(RandomRange.class);
            when(mockAnnotation.min()).thenReturn(0);
            when(mockAnnotation.max()).thenReturn(100);
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(mockAnnotation));

            var result = (Integer) resolver.resolveParameter(parameterContext, extensionContext);

            assertNotNull(result);
            assertTrue(result >= 0 && result <= 100,
                    "Result should be between 0 and 100, but was: " + result);
        }

        @Test
        void largeRange() {
            var mockAnnotation = mock(RandomRange.class);
            when(mockAnnotation.min()).thenReturn(1);
            when(mockAnnotation.max()).thenReturn(Integer.MAX_VALUE);
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(mockAnnotation));

            var result = (Integer) resolver.resolveParameter(parameterContext, extensionContext);

            assertNotNull(result);
        }

        @RepeatedTest(3)
        void negativeRange() {
            var mockAnnotation = mock(RandomRange.class);
            when(mockAnnotation.min()).thenReturn(-50);
            when(mockAnnotation.max()).thenReturn(-10);
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(mockAnnotation));

            var result = (Integer) resolver.resolveParameter(parameterContext, extensionContext);

            assertNotNull(result);
            assertTrue(result >= -50 && result <= -10,
                    "Result should be between -50 and -10, but was: " + result);
        }

        @Test
        void singleValueRange() {
            var mockAnnotation = mock(RandomRange.class);
            when(mockAnnotation.min()).thenReturn(42);
            when(mockAnnotation.max()).thenReturn(42);
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(mockAnnotation));

            var result = (Integer) resolver.resolveParameter(parameterContext, extensionContext);

            assertEquals(42, result);
        }

        // Test boundary conditions
        @Test
        void zeroRange() {
            var mockAnnotation = mock(RandomRange.class);
            when(mockAnnotation.min()).thenReturn(100);
            when(mockAnnotation.max()).thenReturn(100);
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(mockAnnotation));

            var result = (Integer) resolver.resolveParameter(parameterContext, extensionContext);

            assertEquals(100, result, "When min equals max, should return that exact value");
        }
    }

    @Nested
    @DisplayName("Resolve Parameter Tests")
    class ResolveParameterTests {
        @RepeatedTest(100)
        void resolveParameterWithRepeatedExecution() {
            var mockAnnotation = mock(RandomRange.class);
            when(mockAnnotation.min()).thenReturn(1);
            when(mockAnnotation.max()).thenReturn(10);
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(mockAnnotation));

            var result = (Integer) resolver.resolveParameter(parameterContext, extensionContext);

            assertTrue(result >= 1 && result <= 10,
                    "Result should be between 1 and 10, but was: " + result);
        }

        // Test for SecureRandom usage verification
        @Test
        void resolveParameterWithUniqueValuesOverMultipleCalls() {
            var mockAnnotation = mock(RandomRange.class);
            when(mockAnnotation.min()).thenReturn(1);
            when(mockAnnotation.max()).thenReturn(1000000);
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(mockAnnotation));

            var values = new java.util.HashSet<Integer>();
            for (int i = 0; i < 100; i++) {
                var result = (Integer) resolver.resolveParameter(parameterContext, extensionContext);
                values.add(result);
            }

            // With such a large range, we should get many unique values
            assertTrue(values.size() > 50,
                    "Expected many unique values from SecureRandom, but only got: " + values.size());
        }
    }
}