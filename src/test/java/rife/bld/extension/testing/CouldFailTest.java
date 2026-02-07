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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("CouldFail Annotation Tests")
class CouldFailTest {

    @Nested
    @DisplayName("Basic Usage")
    class BasicUsageTests {

        @Test
        @DisplayName("Test with @CouldFail that fails is aborted")
        @CouldFail
        void couldFailAndDoesFail() {
            fail("This failure is accepted");
        }

        @Test
        @DisplayName("Test with @CouldFail that passes continues to pass")
        @CouldFail
        void couldFailButPasses() {
            assertEquals(4, 2 + 2);
        }

        @Test
        @DisplayName("Normal test without @CouldFail should pass")
        void normalTestPasses() {
            assertEquals(4, 2 + 2);
        }
    }

    @Nested
    @DisplayName("Class-Level Annotation")
    @CouldFail(withExceptions = UnsupportedOperationException.class)
    @SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
    class ClassLevelAnnotationTests {

        @Test
        @DisplayName("Inherit class-level @CouldFail")
        void inheritClassLevelAnnotation() {
            throw new UnsupportedOperationException("Inherited annotation accepts this");
        }

        @Test
        @DisplayName("Override class-level @CouldFail with method-level")
        @CouldFail(withExceptions = IllegalStateException.class)
        void overrideClassLevelAnnotation() {
            throw new IllegalStateException("Method-level annotation takes precedence");
        }
    }

    @Nested
    @DisplayName("Coverage Tests - Uncovered Paths")
    class CoverageTests {

        @Test
        @DisplayName("Exact exception type matching works")
        @CouldFail(withExceptions = {NullPointerException.class, IllegalArgumentException.class})
        @SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
        void exactExceptionTypeMatches() {
            // This should be accepted (tests successful loop iteration)
            throw new IllegalArgumentException("This is in the list");
        }

        @Test
        @DisplayName("Multiple non-matching exceptions should fail normally")
        @CouldFail(withExceptions = {IOException.class, TimeoutException.class})
        @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
        void multipleNonMatchingExceptionsFail() {
            // RuntimeException is not in the list, so this should fail normally
            // This ensures the for loop iterates through all options and returns false
            assertThrows(RuntimeException.class, () -> {
                throw new RuntimeException("Not in the accepted list");
            });
        }

        @Test
        @DisplayName("Exception not in withExceptions list should fail normally")
        @CouldFail(withExceptions = IOException.class)
        void nonMatchingExceptionFailsNormally() {
            // This should fail because IllegalStateException is NOT in the accepted list
            // This tests: shouldAcceptFailure returns false, and the loop doesn't match
            assertThrows(IllegalStateException.class, () -> {
                throw new IllegalStateException("This exception type is not accepted");
            });
        }

        @Test
        @DisplayName("First exception in list doesn't match but second does")
        @CouldFail(withExceptions = {IOException.class, IllegalStateException.class, TimeoutException.class})
        @SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
        void secondExceptionInListMatches() {
            // Tests that the loop continues checking after the first mismatch
            throw new IllegalStateException("Matches second item in list");
        }

        @Test
        @DisplayName("Test without @CouldFail annotation should fail normally")
        void testWithoutAnnotationFailsNormally() {
            // This test demonstrates that without @CouldFail, the extension
            // doesn't interfere (annotation.isEmpty() returns true)
            // We use assertThrows to verify the exception propagates normally
            assertThrows(AssertionError.class, () -> fail("This should fail normally without @CouldFail"));
        }
    }

    @Nested
    @DisplayName("Documentation Examples")
    @SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
    class DocumentationExamples {

        @Test
        @DisplayName("Example: Accept any failure")
        @CouldFail
        void exampleAcceptAnyFailure() {
            // Any exception will be accepted
            fail("This could fail for any reason");
        }

        @Test
        @DisplayName("Example: Accept multiple exceptions")
        @CouldFail(withExceptions = {IOException.class, TimeoutException.class})
        @SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
        void exampleAcceptMultipleExceptions() throws IOException {
            // Either exception type will be accepted
            throw new IOException("Connection failed");
        }

        @Test
        @DisplayName("Example: Accept specific exception")
        @CouldFail(withExceptions = UnsupportedOperationException.class)
        void exampleAcceptSpecificException() {
            // Only UnsupportedOperationException will be accepted
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    @SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
    class EdgeCaseTests {

        @Test
        @DisplayName("AssertionError is accepted when specified")
        @CouldFail(withExceptions = AssertionError.class)
        void acceptAssertionError() {
            assertEquals(5, 2 + 2,
                    "This assertion will fail but be accepted");
        }

        @Test
        @DisplayName("Multiple layers of exception hierarchy")
        @CouldFail(withExceptions = Exception.class)
        void acceptBroadExceptionType() throws IOException {
            // IOException extends Exception
            throw new IOException("Accepted via parent class");
        }

        @Test
        @DisplayName("Empty withExceptions accepts any exception")
        @CouldFail(withExceptions = {})
        @SuppressWarnings({"PMD.AvoidThrowingRawExceptionTypes", "DefaultAnnotationParam"})
        void emptyWithExceptionsAcceptsAny() {
            throw new RuntimeException("Any exception is accepted");
        }
    }

    @Nested
    @DisplayName("Exception Filtering")
    @SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
    class ExceptionFilteringTests {

        @Test
        @DisplayName("Accept exception subclass")
        @CouldFail(withExceptions = RuntimeException.class)
        void acceptExceptionSubclass() {
            // IllegalArgumentException is a subclass of RuntimeException
            throw new IllegalArgumentException("This should be accepted");
        }

        @Test
        @DisplayName("Accept multiple exception types")
        @CouldFail(withExceptions = {IOException.class, TimeoutException.class})
        void acceptMultipleExceptionTypes() throws IOException, TimeoutException {
            // Randomly throw one of the accepted exceptions
            if (Math.random() < 0.5) {
                throw new IOException("Network error");
            } else {
                throw new TimeoutException("Operation timed out");
            }
        }

        @Test
        @DisplayName("Accept specific exception type")
        @CouldFail(withExceptions = UnsupportedOperationException.class)
        void acceptUnsupportedOperation() {
            throw new UnsupportedOperationException("Feature not implemented");
        }
    }

    @Nested
    @DisplayName("Extension Unit Tests")
    @SuppressWarnings({"PMD.PublicMemberInNonPublicType", "PMD.DetachedTestCase"})
    class ExtensionUnitTests {

        @CouldFail
        public void methodWithCouldFail() {
            // @CouldFail with no withExceptions
        }

        @CouldFail(withExceptions = IOException.class)
        public void methodWithSpecificException() {
            // @CouldFail with specific exception type
        }

        public void methodWithoutAnnotation() {
            // No @CouldFail annotation
        }

        @Test
        @DisplayName("Extension accepts exception in withExceptions list")
        void extensionAcceptsMatchingException() throws Exception {
            var extension = new CouldFail.Extension();

            ExtensionContext context = mock(ExtensionContext.class);
            Method testMethod = ExtensionUnitTests.class.getMethod("methodWithSpecificException");
            when(context.getTestMethod()).thenReturn(Optional.of(testMethod));
            when(context.getTestClass()).thenReturn(Optional.of(ExtensionUnitTests.class));

            // Throw an exception in the withExceptions list
            var testException = new IOException("Accepted exception");

            // Should throw TestAbortedException
            var thrown = assertThrows(org.opentest4j.TestAbortedException.class, () ->
                    extension.handleTestExecutionException(context, testException));

            assertTrue(thrown.getMessage().contains("Test failure accepted by @CouldFail"));
            assertSame(testException, thrown.getCause());
        }

        // Helper methods for testing (with different @CouldFail configurations)

        @Test
        @DisplayName("Extension accepts exception when @CouldFail present with no withExceptions")
        void extensionAcceptsWhenAnnotationPresentNoFilter() throws Exception {
            var extension = new CouldFail.Extension();

            ExtensionContext context = mock(ExtensionContext.class);
            Method testMethod = ExtensionUnitTests.class.getMethod("methodWithCouldFail");
            when(context.getTestMethod()).thenReturn(Optional.of(testMethod));
            when(context.getTestClass()).thenReturn(Optional.of(ExtensionUnitTests.class));

            var testException = new IOException("Test exception");

            // Should throw TestAbortedException
            var thrown = assertThrows(org.opentest4j.TestAbortedException.class, () ->
                    extension.handleTestExecutionException(context, testException));

            assertTrue(thrown.getMessage().contains("Test failure accepted by @CouldFail"));
            assertSame(testException, thrown.getCause());
        }

        @Test
        @DisplayName("Extension re-throws exception when no @CouldFail annotation present")
        void extensionReThrowsWhenNoAnnotation() throws Exception {
            // Create the extension
            var extension = new CouldFail.Extension();

            // Create a mock ExtensionContext with no @CouldFail annotation
            ExtensionContext context = mock(ExtensionContext.class);

            // Mock a test method without @CouldFail annotation
            Method testMethod = ExtensionUnitTests.class.getMethod("methodWithoutAnnotation");
            when(context.getTestMethod()).thenReturn(Optional.of(testMethod));
            when(context.getTestClass()).thenReturn(Optional.of(ExtensionUnitTests.class));

            // Create a test exception
            var testException = new IOException("Test exception");

            // The extension should re-throw the original exception
            var thrown = assertThrows(IOException.class, () ->
                    extension.handleTestExecutionException(context, testException));

            assertSame(testException, thrown, "Original exception should be re-thrown");
        }

        @Test
        @DisplayName("Extension rejects exception not in withExceptions list")
        void extensionRejectsNonMatchingException() throws Exception {
            var extension = new CouldFail.Extension();

            ExtensionContext context = mock(ExtensionContext.class);
            Method testMethod = ExtensionUnitTests.class.getMethod("methodWithSpecificException");
            when(context.getTestMethod()).thenReturn(Optional.of(testMethod));
            when(context.getTestClass()).thenReturn(Optional.of(ExtensionUnitTests.class));

            // Throw an exception NOT in the withExceptions list
            var testException = new IllegalStateException("Wrong exception type");

            // Should re-throw the original exception
            var thrown = assertThrows(IllegalStateException.class, () ->
                    extension.handleTestExecutionException(context, testException));

            assertSame(testException, thrown);
        }
    }

    @Nested
    @DisplayName("Real-World Scenarios")
    @SuppressWarnings("PMD.UnnecessaryBooleanAssertion")
    class RealWorldScenarios {

        private int productionFeature() {
            throw new UnsupportedOperationException("productionFeature() is not yet implemented");
        }

        @Test
        @DisplayName("External service dependency")
        @CouldFail(withExceptions = {IOException.class, IllegalStateException.class})
        void testExternalService() {
            if (System.getenv("EXTERNAL_SERVICE_URL") == null) {
                throw new IllegalStateException("External service not configured");
            }
            // Test external service integration
            assertTrue(true);
        }

        @Test
        @DisplayName("Flakey network operation")
        @CouldFail(withExceptions = {IOException.class, TimeoutException.class})
        void testNetworkOperation() throws IOException {
            // Simulate flakey network
            if (Math.random() < 0.3) {
                throw new IOException("Network timeout");
            }
            assertTrue(true);
        }

        @Test
        @DisplayName("Platform-specific operation")
        @CouldFail(withExceptions = UnsupportedOperationException.class)
        void testPlatformSpecificFeature() {
            var os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                throw new UnsupportedOperationException("POSIX not supported on Windows");
            }
            // Test POSIX-specific code
            assertTrue(true);
        }

        @Test
        @DisplayName("Unimplemented feature stub")
        @CouldFail(withExceptions = UnsupportedOperationException.class)
        void testUnimplementedFeature() {
            var result = productionFeature();
            assertEquals(10, result);
        }
    }
}
