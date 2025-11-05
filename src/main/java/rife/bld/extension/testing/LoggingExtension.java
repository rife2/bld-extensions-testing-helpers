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

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JUnit extension for configuring console logging for test suites.
 * <p>
 * This extension sets up a console handler with a configurable logging level before each test method and restores
 * the original logger configuration after each test method completes. This provides maximum isolation between
 * individual test methods.
 * <p>
 * The logger is configured to output directly to the console without using parent handlers.
 *
 * <h3>Usage Examples:</h3>
 *
 * <blockquote><pre>
 * &#64;ExtendWith(LoggingExtension.class)
 * class MyTestClass {
 *     // Default configuration (uses LoggingExtension logger with Level.ALL)
 *     &#64;Test
 *     void myTest() { ... }
 *
 *     // Custom logger with default level
 *     &#64;RegisterExtension
 *     private static final LoggingExtension LOGGING_EXTENSION = new LoggingExtension("MyCustomLogger");
 *
 *     // Custom logger and level
 *     &#64;RegisterExtension
 *     private static final LoggingExtension LOGGING_EXTENSION = new LoggingExtension(
 *         MyClass.getLogger(),
 *         Level.INFO
 *     );
 *
 *     // Custom logger with test log handler
 *     private static final Logger LOGGER = Logger.getLogger(MyClass.class.getName());
 *     private static final TestLogHandler TEST_LOG_HANDLER = new TestLogHandler();
 *
 *     &#64;RegisterExtension
 *     private static final LoggingExtension LOGGING_EXTENSION = new LoggingExtension(
 *         LOGGER,
 *         TEST_LOG_HANDLER
 *     );
 *
 *     // Custom logger with existing handler and level override
 *     &#64;RegisterExtension
 *     private static final LoggingExtension LOGGING_EXTENSION = new LoggingExtension(
 *         MyClass.getLogger(),
 *         myExistingHandler,
 *         Level.WARNING
 *     );
 * }</pre></blockquote>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see AfterEachCallback
 * @see BeforeEachCallback
 * @see Level
 * @see Logger
 * @see TestLogHandler
 * @since 1.0
 */
@SuppressWarnings({"PMD.MoreThanOneLogger"})
public class LoggingExtension implements BeforeEachCallback, AfterEachCallback {
    /**
     * Default logger instance used when no custom logger is specified.
     */
    private static final Logger DEFAULT_LOGGER = Logger.getLogger(LoggingExtension.class.getName());
    /**
     * Store configurations per test method to allow proper cleanup after each test.
     */
    private static final Map<Class<?>, Map<String, LoggerState>> TEST_METHOD_CONFIGS = new ConcurrentHashMap<>();
    /**
     * The handler to use for logging output. If null, a ConsoleHandler will be created.
     */
    private final Handler handler;
    /**
     * The logging level to set for both the logger and console handler.
     */
    private final Level level;
    /**
     * The logger instance to configure.
     */
    private final Logger logger;

    /**
     * Creates a LoggingExtension with the default logger and {@link Level#ALL}.
     * <p>
     * The default logger is named after
     * {@link rife.bld.extension.testing.LoggingExtension this class}.
     */
    public LoggingExtension() {
        this(DEFAULT_LOGGER, null, Level.ALL);
    }

    /**
     * Creates a LoggingExtension with a custom logger and {@link Level#ALL}.
     *
     * @param logger the logger to configure for console output
     * @throws NullPointerException if logger is {@code null}
     */
    public LoggingExtension(Logger logger) {
        this(logger, null, Level.ALL);
    }

    /**
     * Creates a LoggingExtension with a custom logger and logging level.
     *
     * @param logger the logger to configure for console output
     * @param level  the logging level to set for both logger and console handler
     * @throws NullPointerException if logger or level is {@code null}
     */
    public LoggingExtension(Logger logger, Level level) {
        this(logger, null, level);
    }

    /**
     * Creates a LoggingExtension with a custom logger and existing handler.
     * <p>
     * The handler's existing level will be preserved unless overridden by constructor parameters.
     *
     * @param logger  the logger to configure for output
     * @param handler the existing handler to use for logging output
     * @throws NullPointerException if logger or handler is {@code null}
     */
    public LoggingExtension(Logger logger, Handler handler) {
        this(logger, handler, handler != null ? handler.getLevel() : Level.ALL);
    }

    /**
     * Creates a LoggingExtension with a custom logger, existing handler, and logging level override.
     * <p>
     * The specified level will be applied to both the logger and the handler, overriding the handler's existing
     * level configuration.
     *
     * @param logger  the logger to configure for output
     * @param handler the existing handler to use for logging output
     * @param level   the logging level to set for both logger and handler
     * @throws NullPointerException if logger or level is {@code null}
     */
    public LoggingExtension(Logger logger, Handler handler, Level level) {
        this.logger = logger;
        this.handler = handler;
        this.level = level;
    }

    /**
     * Creates a LoggingExtension with a custom logger name and {@link Level#ALL}.
     *
     * @param loggerName the fully qualified logger name to configure for console output
     */
    public LoggingExtension(String loggerName) {
        this(Logger.getLogger(loggerName));
    }

    /**
     * Creates a LoggingExtension with a custom logger name and logging level.
     *
     * @param loggerName the fully qualified logger name to configure for console output
     * @param level      the logging level to set for both logger and console handler
     */
    public LoggingExtension(String loggerName, Level level) {
        this(Logger.getLogger(loggerName), level);
    }

    /**
     * Creates a LoggingExtension with a custom logger name and existing handler.
     * <p>
     * The handler's existing level will be preserved.
     *
     * @param loggerName the fully qualified logger name to configure for output
     * @param handler    the existing handler to use for logging output
     * @throws NullPointerException if handler is {@code null}
     */
    public LoggingExtension(String loggerName, Handler handler) {
        this(Logger.getLogger(loggerName), handler);
    }

    /**
     * Creates a LoggingExtension with a custom logger name, existing handler, and logging level override.
     *
     * @param loggerName the fully qualified logger name to configure for output
     * @param handler    the existing handler to use for logging output
     * @param level      the logging level to set for both logger and handler
     * @throws NullPointerException if handler or level is {@code null}
     */
    public LoggingExtension(String loggerName, Handler handler, Level level) {
        this(Logger.getLogger(loggerName), handler, level);
    }

    /**
     * Restores the original logger and handler configuration after each test method completes.
     * <p>
     * This method removes any handlers added by this extension, restores the logger's original level and parent
     * handler usage settings, and resets any modified handler levels back to their original state. If the handler
     * is a {@link TestLogHandler}, it also clears its captured log records. This ensures that both logger and
     * handler state don't leak between individual test methods.
     *
     * @param context the extension context providing access to the test class
     */
    @Override
    public void afterEach(ExtensionContext context) {
        var testClass = context.getRequiredTestClass();
        var methodConfigs = TEST_METHOD_CONFIGS.get(testClass);

        if (methodConfigs != null) {
            for (var entry : methodConfigs.entrySet()) {
                var configKey = entry.getKey();
                var state = entry.getValue();

                var targetLogger = configKey.startsWith("null-logger-")
                        ? this.logger
                        : Logger.getLogger(configKey);

                // If the handler is a TestLogHandler, clear its captured records
                if (state.addedHandler instanceof TestLogHandler testLogHandler) {
                    testLogHandler.clear();
                }

                // Remove our added handler from the logger
                targetLogger.removeHandler(state.addedHandler);

                // If we modified an existing handler, restore its original level
                if (state.originalHandlerLevel != null) {
                    state.addedHandler.setLevel(state.originalHandlerLevel);
                }

                // If we created a new ConsoleHandler, close it to clean up resources
                if (handler == null && state.addedHandler instanceof ConsoleHandler consoleHandler) {
                    consoleHandler.close();
                }

                // Restore original handlers
                for (var originalHandler : state.originalHandlers) {
                    targetLogger.addHandler(originalHandler);
                }

                // Restore original logger configuration
                targetLogger.setLevel(state.originalLevel);
                targetLogger.setUseParentHandlers(state.originalUseParentHandlers);
            }

            // Clear the configuration for the next test method
            methodConfigs.clear();
        }
    }

    /**
     * Configures the logger with console output before each test method runs.
     * <p>
     * This method is called before every test method. It performs the following configuration:
     *
     * <ul>
     *   <li>Uses the provided handler or creates a {@link ConsoleHandler} with the specified level</li>
     *   <li>Adds the handler to the logger</li>
     *   <li>Sets the logger's level</li>
     *   <li>Disables parent handler usage to prevent duplicate output</li>
     *   <li>Stores the original logger and handler state for restoration after the test method completes</li>
     * </ul>
     *
     * @param context the extension context providing access to the test class
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        var testClass = context.getRequiredTestClass();
        var loggerName = logger.getName();
        var configKey = loggerName != null ? loggerName : "null-logger-" + System.identityHashCode(logger);

        // Get or create a configuration map for this test method
        var methodConfigs = TEST_METHOD_CONFIGS.computeIfAbsent(testClass, k ->
                new ConcurrentHashMap<>());

        // Configure for each test method (always store fresh state)
        var handlerToUse = handler != null ? handler : new ConsoleHandler();
        handlerToUse.setLevel(level);

        // Store the original state before modification (this captures the original handler level too)
        methodConfigs.put(configKey, new LoggerState(logger, handlerToUse));

        logger.addHandler(handlerToUse);
        logger.setLevel(level);
        logger.setUseParentHandlers(false);
    }

    /**
     * Holds the original state of a logger and its handlers before modification, allowing complete restoration
     * after tests complete.
     */
    private static class LoggerState {
        final Handler addedHandler;
        final Level originalHandlerLevel; // Store original handler level if using existing handler
        final Handler[] originalHandlers;
        final Level originalLevel;
        final boolean originalUseParentHandlers;

        LoggerState(Logger logger, Handler addedHandler) {
            this.originalLevel = logger.getLevel();
            this.originalUseParentHandlers = logger.getUseParentHandlers();
            this.originalHandlers = logger.getHandlers().clone();
            this.addedHandler = addedHandler;
            // If we're reusing an existing handler, store its original level
            this.originalHandlerLevel = (addedHandler != null) ? addedHandler.getLevel() : null;
        }
    }
}