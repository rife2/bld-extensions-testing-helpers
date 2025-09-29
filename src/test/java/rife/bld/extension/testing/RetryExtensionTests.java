package rife.bld.extension.testing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"EmptyMethod", "PMD.UncommentedEmptyMethodBody", "PMD.AvoidDuplicateLiterals",
        "PMD.AvoidAccessibilityAlteration", "PMD.DetachedTestCase"})
class RetryExtensionTests {
    @RetryTest(name = "CustomTestName", value = 1)
    void customNameTestMethod() {
    }

    @RetryTest(1)
    void defaultNameTestMethod() {
    }

    @RetryTest(value = 2, delay = 3)
    void delayTestMethod() {
    }

    void notAnnotatedMethod() {
    }

    @SuppressWarnings("CatchMayIgnoreException")
    @Test
    void retryExceptionHandlerSuppressAndThrowViaReflection() throws Exception {
        var method = this.getClass().getDeclaredMethod("defaultNameTestMethod");
        var context = mock(ExtensionContext.class);
        var store = mock(ExtensionContext.Store.class);

        Mockito.when(context.getTestMethod()).thenReturn(Optional.of(method));
        Mockito.when(context.getRequiredTestMethod()).thenReturn(method);
        Mockito.when(context.getStore(Mockito.any())).thenReturn(store);

        // testPassed = false for the first case
        Mockito.when(store.get("testPassed")).thenReturn(false);

        // Get the RetryExceptionHandler class via reflection
        var handlerClass = Class.forName("rife.bld.extension.testing.RetryExtension$RetryExceptionHandler");
        var ctor = handlerClass.getDeclaredConstructor(int.class, int.class, int.class);
        ctor.setAccessible(true);

        // suppress case
        var handlerSuppress = ctor.newInstance(1, 2, 0);
        var handleMethod = handlerClass.getMethod(
                "handleTestExecutionException",
                ExtensionContext.class,
                Throwable.class
        );
        handleMethod.invoke(handlerSuppress, context, new RuntimeException("fail"));
        // should NOT throw

        // throw case
        var handlerThrow = ctor.newInstance(2, 2, 0);
        try {
            handleMethod.invoke(handlerThrow, context, new RuntimeException("fail last"));
            fail("Should have thrown");
        } catch (Exception e) {
            assertInstanceOf(RuntimeException.class, e.getCause());
            assertEquals("fail last", e.getCause().getMessage());
        }

        // testPassed=true case
        Mockito.when(store.get("testPassed")).thenReturn(true);
        var handlerPassed = ctor.newInstance(1, 2, 0);
        handleMethod.invoke(handlerPassed, context, new RuntimeException("should not throw"));
        // should NOT throw
    }

    @Test
    void retryInvocationContextReflectionTest() throws Exception {
        var contextClass = Class.forName("rife.bld.extension.testing.RetryExtension$RetryInvocationContext");
        var ctor = contextClass.getDeclaredConstructor(String.class, int.class, int.class, int.class);
        ctor.setAccessible(true);

        var instance = ctor.newInstance("TestName", 2, 3, 0);
        var displayNameMethod = contextClass.getMethod("getDisplayName", int.class);
        var name = displayNameMethod.invoke(instance, 1);
        assertEquals("TestName (attempt 2/3)", name);

        var extMethod = contextClass.getMethod("getAdditionalExtensions");
        var extList = (List<?>) extMethod.invoke(instance);
        assertEquals(1, extList.size());
        var ext = extList.get(0);
        assertEquals(
                "rife.bld.extension.testing.RetryExtension$RetryExceptionHandler",
                ext.getClass().getName()
        );
    }

    @Test
    void testDelayBetweenRetries() throws Exception {
        var testMethod = this.getClass().getDeclaredMethod("delayTestMethod");
        var context = mock(ExtensionContext.class);
        when(context.getTestMethod()).thenReturn(Optional.of(testMethod));
        when(context.getRequiredTestMethod()).thenReturn(testMethod);
        when(context.getDisplayName()).thenReturn("delayTestMethod");

        var store = mock(ExtensionContext.Store.class);
        when(context.getStore(any(ExtensionContext.Namespace.class))).thenReturn(store);
        when(store.get("testPassed")).thenReturn(false);

        RetryExtension extension = new RetryExtension();
        assertTrue(extension.supportsTestTemplate(context));

        var invocationContexts = extension.provideTestTemplateInvocationContexts(context).toList();
        assertEquals(3, invocationContexts.size());

        var firstInvocation = invocationContexts.get(0);
        var extensions = firstInvocation.getAdditionalExtensions();
        assertEquals(1, extensions.size());

        var exceptionHandler = extensions.get(0);
        assertInstanceOf(TestExecutionExceptionHandler.class, exceptionHandler);

        // Verify the handler doesn't throw on non-final attempts
        var handler = (TestExecutionExceptionHandler) exceptionHandler;
        var testException = new AssertionError("Test failed");

        assertDoesNotThrow(() -> handler.handleTestExecutionException(context, testException));

        // Verify the delay value is configured correctly via reflection
        var delayField = handler.getClass().getDeclaredField("delaySeconds");
        delayField.setAccessible(true);
        var delayValue = (int) delayField.get(handler);
        assertEquals(3, delayValue); // @RetryTest(value = 2, delay = 3) -> delay is in seconds
    }

    @Test
    @SuppressWarnings("PMD.DoNotUseThreads")
    void testDelayInterruptedException() throws Exception {
        var method = this.getClass().getDeclaredMethod("delayTestMethod");
        var context = mock(ExtensionContext.class);
        var store = mock(ExtensionContext.Store.class);

        when(context.getTestMethod()).thenReturn(Optional.of(method));
        when(context.getRequiredTestMethod()).thenReturn(method);
        when(context.getStore(any(ExtensionContext.Namespace.class))).thenReturn(store);
        when(store.get("testPassed")).thenReturn(false);

        // Get the RetryExceptionHandler class via reflection
        var handlerClass = Class.forName("rife.bld.extension.testing.RetryExtension$RetryExceptionHandler");
        var ctor = handlerClass.getDeclaredConstructor(int.class, int.class, int.class);
        ctor.setAccessible(true);

        // Create a handler with delay > 0, non-final attempt
        var handler = ctor.newInstance(1, 3, 5); // attempt 1 of 3, with a 5-second delay
        var handleMethod = handlerClass.getMethod(
                "handleTestExecutionException",
                ExtensionContext.class,
                Throwable.class
        );

        // Interrupt the thread before invoking the handler
        Thread.currentThread().interrupt();

        // Should catch InterruptedException and wrap it in RuntimeException
        var exception = assertThrows(Exception.class, () ->
                handleMethod.invoke(handler, context, new AssertionError("Test failed"))
        );

        // Verify the wrapped exception
        assertInstanceOf(RuntimeException.class, exception.getCause());
        assertEquals("Retry delay was interrupted", exception.getCause().getMessage());
        assertInstanceOf(InterruptedException.class, exception.getCause().getCause());

        // Verify the interrupt flag is restored
        assertTrue(Thread.interrupted(), "Thread interrupt flag should be set");
    }

    @Test
    void testTemplateInvocationIfCustomNameGiven() throws Exception {
        var extension = new RetryExtension();
        var method = this.getClass().getDeclaredMethod("customNameTestMethod");
        var context = mock(ExtensionContext.class);
        var store = mock(ExtensionContext.Store.class);

        Mockito.when(context.getTestMethod()).thenReturn(Optional.of(method));
        Mockito.when(context.getRequiredTestMethod()).thenReturn(method);
        Mockito.when(context.getDisplayName()).thenReturn("customNameTestMethod");
        Mockito.when(context.getStore(Mockito.any())).thenReturn(store);
        Mockito.doNothing().when(store).put(Mockito.any(), Mockito.any());

        var stream = extension.provideTestTemplateInvocationContexts(context);
        var contexts = stream.toList();
        assertFalse(contexts.isEmpty());
        var firstDisplayName = contexts.get(0).getDisplayName(0);
        assertTrue(firstDisplayName.startsWith("CustomTestName"),
                "Should use RetryTest.name() if present");
    }

    @Test
    void testTemplateInvocationIfDisplayNameEmpty() throws Exception {
        var extension = new RetryExtension();
        var method = this.getClass().getDeclaredMethod("defaultNameTestMethod");
        var context = mock(ExtensionContext.class);
        var store = mock(ExtensionContext.Store.class);

        Mockito.when(context.getTestMethod()).thenReturn(Optional.of(method));
        Mockito.when(context.getRequiredTestMethod()).thenReturn(method);
        Mockito.when(context.getDisplayName()).thenReturn("defaultNameTestMethod");
        Mockito.when(context.getStore(Mockito.any())).thenReturn(store);
        Mockito.doNothing().when(store).put(Mockito.any(), Mockito.any());

        Stream<TestTemplateInvocationContext> stream = extension.provideTestTemplateInvocationContexts(context);
        var contexts = stream.toList();
        assertFalse(contexts.isEmpty());
        var firstDisplayName = contexts.get(0).getDisplayName(0);
        assertTrue(firstDisplayName.startsWith("defaultNameTestMethod"),
                "Should use context.getDisplayName() when name is empty");
    }

    @Test
    void testTemplateInvocationWithReflection() throws Exception {
        var extension = new RetryExtension();
        var retryMethod = this.getClass().getDeclaredMethod("defaultNameTestMethod");
        var context = mock(ExtensionContext.class);
        var store = mock(ExtensionContext.Store.class);

        Mockito.when(context.getTestMethod()).thenReturn(Optional.of(retryMethod));
        Mockito.when(context.getRequiredTestMethod()).thenReturn(retryMethod);
        Mockito.when(context.getDisplayName()).thenReturn("defaultNameTestMethod");
        Mockito.when(context.getStore(Mockito.any())).thenReturn(store);
        Mockito.doNothing().when(store).put(Mockito.any(), Mockito.any());

        var contexts = extension.provideTestTemplateInvocationContexts(context);
        var contextList = contexts.toList();
        assertEquals(2, contextList.size()); // @RetryTest(1) yields 2 attempts

        for (var i = 0; i < contextList.size(); i++) {
            var invocationContext = contextList.get(i);
            var displayName = invocationContext.getDisplayName(i);
            assertTrue(displayName.contains("attempt"));
            var extensions = invocationContext.getAdditionalExtensions();
            assertFalse(extensions.isEmpty());

            // Reflection: Check type and fields
            var ext = extensions.get(0);
            assertEquals(
                    "rife.bld.extension.testing.RetryExtension$RetryExceptionHandler",
                    ext.getClass().getName()
            );
            var currentAttempt = (int) ext.getClass().getMethod("currentAttempt").invoke(ext);
            var maxAttempts = (int) ext.getClass().getMethod("maxAttempts").invoke(ext);
            assertEquals(i + 1, currentAttempt);
            assertEquals(2, maxAttempts);
        }
    }

    @Test
    void testTemplateReflectionForAnnotatedAndNonAnnotated() throws Exception {
        var extension = new RetryExtension();

        var annotated = this.getClass().getDeclaredMethod("defaultNameTestMethod");
        var notAnnotated = this.getClass().getDeclaredMethod("notAnnotatedMethod");

        var context1 = mock(ExtensionContext.class);
        Mockito.when(context1.getTestMethod()).thenReturn(Optional.of(annotated));

        var context2 = mock(ExtensionContext.class);
        Mockito.when(context2.getTestMethod()).thenReturn(Optional.of(notAnnotated));

        assertTrue(extension.supportsTestTemplate(context1));
        assertFalse(extension.supportsTestTemplate(context2));
    }
}