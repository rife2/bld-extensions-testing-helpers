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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoggingExtensionTests {
    private LoggingExtension extension;
    @Mock
    private ExtensionContext mockContext;
    @Mock
    private Logger mockLogger;

    /**
     * Provides various logging levels for parameterized tests.
     */
    static Stream<Level> provideLoggingLevels() {
        return Stream.of(
                Level.OFF,
                Level.SEVERE,
                Level.WARNING,
                Level.INFO,
                Level.CONFIG,
                Level.FINE,
                Level.FINER,
                Level.FINEST,
                Level.ALL
        );
    }

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        // Set up default behavior for mock logger
        when(mockLogger.getName()).thenReturn("MockLogger");
        resetConfiguredLoggers();
    }

    @Test
    @SuppressWarnings("PMD.DoNotUseThreads")
    void concurrentAccessShouldBeSafe() throws InterruptedException {
        // mockLogger already has name set in setUp()
        var numThreads = 10;
        var threads = new Thread[numThreads];
        var exceptions = new Exception[numThreads];

        for (int i = 0; i < numThreads; i++) {
            int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    var ext = new LoggingExtension(mockLogger, Level.INFO);
                    ext.beforeAll(mockContext);
                } catch (Exception e) {
                    exceptions[threadIndex] = e;
                }
            });
        }

        for (var thread : threads) {
            thread.start();
        }

        for (var thread : threads) {
            thread.join();
        }

        for (var exception : exceptions) {
            assertNull(exception, "No exceptions should occur during concurrent access");
        }
        verify(mockLogger, times(1)).addHandler(any(ConsoleHandler.class));
    }

    @ParameterizedTest
    @NullSource
    void constructorWithNullLevel(Level nullLevel) {
        assertThrows(NullPointerException.class, () -> {
            extension = new LoggingExtension(mockLogger, nullLevel);
            extension.beforeAll(mockContext);
        });
    }

    @ParameterizedTest
    @NullSource
    void constructorWithNullLogger(Logger nullLogger) {
        assertThrows(NullPointerException.class, () -> {
            extension = new LoggingExtension(nullLogger);
            extension.beforeAll(mockContext);
        });
    }

    @Test
    void defaultConstructor() {
        extension = new LoggingExtension();

        assertNotNull(extension);
        // We can't directly access private fields, but we can test behavior
        extension.beforeAll(mockContext);
        // The fact that no exception is thrown indicates proper initialization
    }

    /**
     * Helper method to find ConsoleHandler in the array of handlers.
     */
    private ConsoleHandler findConsoleHandler(Handler... handlers) {
        for (var handler : handlers) {
            if (handler instanceof ConsoleHandler consoleHandler) {
                return consoleHandler;
            }
        }
        return null;
    }

    @ParameterizedTest
    @MethodSource("provideLoggingLevels")
    void fullConstructor(Level level) {
        extension = new LoggingExtension(mockLogger, level);

        assertNotNull(extension);
        extension.beforeAll(mockContext);

        verify(mockLogger).addHandler(any(ConsoleHandler.class));
        verify(mockLogger).setLevel(level);
        verify(mockLogger).setUseParentHandlers(false);
    }

    @ParameterizedTest
    @MethodSource("provideLoggingLevels")
    void integrationWithVariousLevels(Level testLevel) {
        // Use the unique logger name to avoid conflicts between parameterized test runs
        var testLogger = Logger.getLogger("IntegrationTest-" + testLevel.getName() + "-" + System.currentTimeMillis());
        extension = new LoggingExtension(testLogger, testLevel);

        extension.beforeAll(mockContext);

        assertEquals(testLevel, testLogger.getLevel());
        assertFalse(testLogger.getUseParentHandlers());

        // Verify handler was added and configured
        var handlers = testLogger.getHandlers();
        assertTrue(handlers.length > 0);

        var consoleHandler = findConsoleHandler(handlers);
        assertNotNull(consoleHandler, "ConsoleHandler should be added");
        assertEquals(testLevel, consoleHandler.getLevel());
    }

    @Test
    void loggerConstructor() {
        extension = new LoggingExtension(mockLogger);

        assertNotNull(extension);
        extension.beforeAll(mockContext);

        verify(mockLogger).addHandler(any(ConsoleHandler.class));
        verify(mockLogger).setLevel(Level.ALL);
        verify(mockLogger).setUseParentHandlers(false);
    }

    /**
     * Helper method to reset the static CONFIGURED_LOGGERS map using reflection.
     * This ensures each test starts with a clean state.
     */
    @SuppressWarnings({"PMD.AvoidAccessibilityAlteration", "unchecked"})
    private void resetConfiguredLoggers() {
        try {
            var field = LoggingExtension.class.getDeclaredField("CONFIGURED_LOGGERS");
            field.setAccessible(true);
            var configuredLoggers = (ConcurrentHashMap<String, Boolean>) field.get(null);
            configuredLoggers.clear();
        } catch (Exception e) {
            fail("Could not reset CONFIGURED_LOGGERS: " + e.getMessage());
        }
    }

    @Test
    void shouldAllowDifferentLoggersWithDifferentConfigurations() {
        var mockLogger1 = mock(Logger.class);
        var mockLogger2 = mock(Logger.class);
        when(mockLogger1.getName()).thenReturn("Logger1");
        when(mockLogger2.getName()).thenReturn("Logger2");

        var extension1 = new LoggingExtension(mockLogger1, Level.INFO);
        var extension2 = new LoggingExtension(mockLogger2, Level.FINE);

        extension1.beforeAll(mockContext);
        extension2.beforeAll(mockContext);

        verify(mockLogger1).addHandler(any(ConsoleHandler.class));
        verify(mockLogger1).setLevel(Level.INFO);
        verify(mockLogger1).setUseParentHandlers(false);

        verify(mockLogger2).addHandler(any(ConsoleHandler.class));
        verify(mockLogger2).setLevel(Level.FINE);
        verify(mockLogger2).setUseParentHandlers(false);
    }

    @Test
    void shouldConfigureConsoleHandler() {
        extension = new LoggingExtension(mockLogger, Level.INFO);

        extension.beforeAll(mockContext);

        var handlerCaptor = org.mockito.ArgumentCaptor.forClass(Handler.class);
        verify(mockLogger).addHandler(handlerCaptor.capture());

        var capturedHandler = handlerCaptor.getValue();
        assertInstanceOf(ConsoleHandler.class, capturedHandler);
        assertEquals(Level.INFO, capturedHandler.getLevel());
    }

    @Test
    void shouldConfigureEachLoggerOnlyOnce() {
        // mockLogger already has name set in setUp()
        var extension1 = new LoggingExtension(mockLogger, Level.INFO);
        var extension2 = new LoggingExtension(mockLogger, Level.FINE);

        extension1.beforeAll(mockContext);
        extension2.beforeAll(mockContext);

        verify(mockLogger, times(1)).addHandler(any(ConsoleHandler.class));
        verify(mockLogger, times(1)).setLevel(Level.INFO); // First level set
        verify(mockLogger, times(1)).setUseParentHandlers(false);
        verify(mockLogger, never()).setLevel(Level.FINE); // Second level ignored
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5, 10})
    void shouldConfigureLoggerOnlyOnceWithMultipleCalls(int numberOfCalls) {
        // mockLogger already has name set in setUp()
        extension = new LoggingExtension(mockLogger);

        for (int i = 0; i < numberOfCalls; i++) {
            extension.beforeAll(mockContext);
        }

        verify(mockLogger, times(1)).addHandler(any(ConsoleHandler.class));
        verify(mockLogger, times(1)).setLevel(any(Level.class));
        verify(mockLogger, times(1)).setUseParentHandlers(false);
    }

    @Test
    void shouldDisableParentHandlers() {
        extension = new LoggingExtension(mockLogger);

        extension.beforeAll(mockContext);

        verify(mockLogger).setUseParentHandlers(false);
    }

    @Test
    void shouldHandleNullLoggerName() {
        // Create a separate mock for this test to avoid affecting others
        var nullNameLogger = mock(Logger.class);
        when(nullNameLogger.getName()).thenReturn(null);
        extension = new LoggingExtension(nullNameLogger, Level.INFO);

        // Should handle null logger name gracefully and still configure the logger
        assertDoesNotThrow(() -> extension.beforeAll(mockContext));

        // Verify the logger was still configured despite the null name
        verify(nullNameLogger).addHandler(any(ConsoleHandler.class));
        verify(nullNameLogger).setLevel(Level.INFO);
        verify(nullNameLogger).setUseParentHandlers(false);
    }

    @Test
    void shouldSetLoggerLevel() {
        var testLevel = Level.WARNING;
        extension = new LoggingExtension(mockLogger, testLevel);

        extension.beforeAll(mockContext);

        verify(mockLogger).setLevel(testLevel);
    }

    @Test
    void shouldTrackConfiguredLoggersCorrectly() {
        var logger1 = mock(Logger.class);
        var logger2 = mock(Logger.class);
        var logger3 = mock(Logger.class);
        when(logger1.getName()).thenReturn("TrackingTest1");
        when(logger2.getName()).thenReturn("TrackingTest2");
        when(logger3.getName()).thenReturn("TrackingTest1"); // Same name as logger1

        var extension1 = new LoggingExtension(logger1, Level.INFO);
        var extension2 = new LoggingExtension(logger2, Level.FINE);
        var extension3 = new LoggingExtension(logger3, Level.WARNING);

        extension1.beforeAll(mockContext);
        extension2.beforeAll(mockContext);
        extension3.beforeAll(mockContext);

        verify(logger1, times(1)).addHandler(any(ConsoleHandler.class));
        verify(logger2, times(1)).addHandler(any(ConsoleHandler.class));
        verify(logger3, never()).addHandler(any(ConsoleHandler.class)); // Skipped due to the same name
    }

    @Test
    void staticInitialization() {
        extension = new LoggingExtension();

        assertDoesNotThrow(() -> extension.beforeAll(mockContext));
    }

    @Test
    void withRealLoggerShouldNotThrow() {
        var realLogger = Logger.getLogger("TestLogger-" + System.currentTimeMillis());
        extension = new LoggingExtension(realLogger, Level.FINE);

        assertDoesNotThrow(() -> extension.beforeAll(mockContext));

        // Verify logger was configured
        assertTrue(realLogger.getHandlers().length > 0);
        assertEquals(Level.FINE, realLogger.getLevel());
        assertFalse(realLogger.getUseParentHandlers());
    }
}