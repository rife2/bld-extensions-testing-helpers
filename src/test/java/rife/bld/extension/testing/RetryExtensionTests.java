package rife.bld.extension.testing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings({"EmptyMethod", "PMD.UncommentedEmptyMethodBody", "PMD.AvoidDuplicateLiterals",
        "PMD.AvoidAccessibilityAlteration", "PMD.DetachedTestCase"})
class RetryExtensionTests {
    @RetryTest(name = "CustomTestName", value = 1)
    void customNameTestMethod() {
    }

    @RetryTest(1)
    void defaultNameTestMethod() {
    }

    void notAnnotatedMethod() {
    }

    @Test
    void provideTestTemplateInvocationContextsInvokesContextsWithReflection() throws Exception {
        var extension = new RetryExtension();
        var retryMethod = this.getClass().getDeclaredMethod("defaultNameTestMethod");
        var context = Mockito.mock(ExtensionContext.class);
        var store = Mockito.mock(ExtensionContext.Store.class);

        Mockito.when(context.getTestMethod()).thenReturn(Optional.of(retryMethod));
        Mockito.when(context.getRequiredTestMethod()).thenReturn(retryMethod);
        Mockito.when(context.getDisplayName()).thenReturn("defaultNameTestMethod");
        Mockito.when(context.getStore(Mockito.any())).thenReturn(store);
        Mockito.doNothing().when(store).put(Mockito.any(), Mockito.any());

        Stream<TestTemplateInvocationContext> contexts = extension.provideTestTemplateInvocationContexts(context);
        var contextList = contexts.toList();
        Assertions.assertEquals(2, contextList.size()); // @RetryTest(1) yields 2 attempts

        for (var i = 0; i < contextList.size(); i++) {
            var invocationContext = contextList.get(i);
            var displayName = invocationContext.getDisplayName(i);
            Assertions.assertTrue(displayName.contains("attempt"));
            var extensions = invocationContext.getAdditionalExtensions();
            Assertions.assertFalse(extensions.isEmpty());

            // Reflection: Check type and fields
            var ext = extensions.get(0);
            Assertions.assertEquals(
                    "rife.bld.extension.testing.RetryExtension$RetryExceptionHandler",
                    ext.getClass().getName()
            );
            var currentAttempt = (int) ext.getClass().getMethod("currentAttempt").invoke(ext);
            var maxAttempts = (int) ext.getClass().getMethod("maxAttempts").invoke(ext);
            Assertions.assertEquals(i + 1, currentAttempt);
            Assertions.assertEquals(2, maxAttempts);
        }
    }

    // Existing tests for coverage and correctness

    @Test
    void provideTestTemplateInvocationContextsUsesContextDisplayNameIfNameEmpty() throws Exception {
        var extension = new RetryExtension();
        var method = this.getClass().getDeclaredMethod("defaultNameTestMethod");
        var context = Mockito.mock(ExtensionContext.class);
        var store = Mockito.mock(ExtensionContext.Store.class);

        Mockito.when(context.getTestMethod()).thenReturn(Optional.of(method));
        Mockito.when(context.getRequiredTestMethod()).thenReturn(method);
        Mockito.when(context.getDisplayName()).thenReturn("defaultNameTestMethod");
        Mockito.when(context.getStore(Mockito.any())).thenReturn(store);
        Mockito.doNothing().when(store).put(Mockito.any(), Mockito.any());

        Stream<TestTemplateInvocationContext> stream = extension.provideTestTemplateInvocationContexts(context);
        var contexts = stream.toList();
        Assertions.assertFalse(contexts.isEmpty());
        var firstDisplayName = contexts.get(0).getDisplayName(0);
        Assertions.assertTrue(firstDisplayName.startsWith("defaultNameTestMethod"),
                "Should use context.getDisplayName() when name is empty");
    }

    @Test
    void provideTestTemplateInvocationContextsUsesCustomNameIfGiven() throws Exception {
        var extension = new RetryExtension();
        var method = this.getClass().getDeclaredMethod("customNameTestMethod");
        var context = Mockito.mock(ExtensionContext.class);
        var store = Mockito.mock(ExtensionContext.Store.class);

        Mockito.when(context.getTestMethod()).thenReturn(Optional.of(method));
        Mockito.when(context.getRequiredTestMethod()).thenReturn(method);
        Mockito.when(context.getDisplayName()).thenReturn("customNameTestMethod");
        Mockito.when(context.getStore(Mockito.any())).thenReturn(store);
        Mockito.doNothing().when(store).put(Mockito.any(), Mockito.any());

        Stream<TestTemplateInvocationContext> stream = extension.provideTestTemplateInvocationContexts(context);
        var contexts = stream.toList();
        Assertions.assertFalse(contexts.isEmpty());
        var firstDisplayName = contexts.get(0).getDisplayName(0);
        Assertions.assertTrue(firstDisplayName.startsWith("CustomTestName"),
                "Should use RetryTest.name() if present");
    }

    @SuppressWarnings("CatchMayIgnoreException")
    @Test
    void retryExceptionHandlerSuppressAndThrowViaReflection() throws Exception {
        var method = this.getClass().getDeclaredMethod("defaultNameTestMethod");
        var context = Mockito.mock(ExtensionContext.class);
        var store = Mockito.mock(ExtensionContext.Store.class);

        Mockito.when(context.getTestMethod()).thenReturn(Optional.of(method));
        Mockito.when(context.getRequiredTestMethod()).thenReturn(method);
        Mockito.when(context.getStore(Mockito.any())).thenReturn(store);

        // testPassed = false for the first case
        Mockito.when(store.get("testPassed")).thenReturn(false);

        // Get the RetryExceptionHandler class via reflection
        var handlerClass = Class.forName("rife.bld.extension.testing.RetryExtension$RetryExceptionHandler");
        Constructor<?> ctor = handlerClass.getDeclaredConstructor(int.class, int.class);
        ctor.setAccessible(true);

        // suppress case
        var handlerSuppress = ctor.newInstance(1, 2);
        Method handleMethod = handlerClass.getMethod("handleTestExecutionException", ExtensionContext.class, Throwable.class);
        handleMethod.invoke(handlerSuppress, context, new RuntimeException("fail"));
        // should NOT throw

        // throw case
        var handlerThrow = ctor.newInstance(2, 2);
        try {
            handleMethod.invoke(handlerThrow, context, new RuntimeException("fail last"));
            Assertions.fail("Should have thrown");
        } catch (Exception e) {
            Assertions.assertInstanceOf(RuntimeException.class, e.getCause());
            Assertions.assertEquals("fail last", e.getCause().getMessage());
        }

        // testPassed=true case
        Mockito.when(store.get("testPassed")).thenReturn(true);
        var handlerPassed = ctor.newInstance(1, 2);
        handleMethod.invoke(handlerPassed, context, new RuntimeException("should not throw"));
        // should NOT throw
    }

    @Test
    void retryInvocationContextReflectionTest() throws Exception {
        var contextClass = Class.forName("rife.bld.extension.testing.RetryExtension$RetryInvocationContext");
        Constructor<?> ctor = contextClass.getDeclaredConstructor(String.class, int.class, int.class);
        ctor.setAccessible(true);

        var instance = ctor.newInstance("TestName", 2, 3);
        Method displayNameMethod = contextClass.getMethod("getDisplayName", int.class);
        var name = displayNameMethod.invoke(instance, 1);
        Assertions.assertEquals("TestName (attempt 2/3)", name);

        Method extMethod = contextClass.getMethod("getAdditionalExtensions");
        var extList = (List<?>) extMethod.invoke(instance);
        Assertions.assertEquals(1, extList.size());
        var ext = extList.get(0);
        Assertions.assertEquals(
                "rife.bld.extension.testing.RetryExtension$RetryExceptionHandler",
                ext.getClass().getName()
        );
    }

    @Test
    void supportsTestTemplateReflectionForAnnotatedAndNonAnnotated() throws Exception {
        var extension = new RetryExtension();

        var annotated = this.getClass().getDeclaredMethod("defaultNameTestMethod");
        var notAnnotated = this.getClass().getDeclaredMethod("notAnnotatedMethod");

        var context1 = Mockito.mock(ExtensionContext.class);
        Mockito.when(context1.getTestMethod()).thenReturn(Optional.of(annotated));

        var context2 = Mockito.mock(ExtensionContext.class);
        Mockito.when(context2.getTestMethod()).thenReturn(Optional.of(notAnnotated));

        Assertions.assertTrue(extension.supportsTestTemplate(context1));
        Assertions.assertFalse(extension.supportsTestTemplate(context2));
    }
}