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

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extension for configuring console logging for test suites.
 * <p>
 * This extension sets up a console handler with a configurable logging level
 * and ensures the configuration is applied only once per logger across all test classes.
 * The logger is configured to output directly to the console without using
 * parent handlers.
 * </p>
 *
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Default configuration (uses LoggingExtension logger with Level.ALL)
 * @ExtendWith(LoggingExtension.class)
 * class MyTestClass {
 *     @Test void myTest() { ... }
 * }
 *
 * // Custom logger with default level
 * @RegisterExtension
 * static LoggingExtension extension = new LoggingExtension(
 *     Logger.getLogger("MyCustomLogger")
 * );
 *
 * // Custom logger and level
 * @RegisterExtension
 * static LoggingExtension extension = new LoggingExtension(
 *     Logger.getLogger("MyLogger"),
 *     Level.INFO
 * );
 * }</pre>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see BeforeAllCallback
 * @see Logger
 * @see Level
 * @since 1.0
 */
@SuppressWarnings("PMD.MoreThanOneLogger")
public class LoggingExtension implements BeforeAllCallback {
    /**
     * Tracks which loggers have been configured to prevent duplicate setup.
     */
    private static final Map<String, Boolean> CONFIGURED_LOGGERS = new ConcurrentHashMap<>();

    /**
     * Default logger instance used when no custom logger is specified.
     */
    private static final Logger DEFAULT_LOGGER = Logger.getLogger(LoggingExtension.class.getName());

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
     * The default logger is named after this class:
     * {@code rife.bld.extension.testing.LoggingExtension}
     * </p>
     */
    public LoggingExtension() {
        this(DEFAULT_LOGGER, Level.ALL);
    }

    /**
     * Creates a LoggingExtension with a custom logger and {@link Level#ALL}.
     *
     * @param logger the logger to configure for console output
     * @throws NullPointerException if logger is null
     */
    public LoggingExtension(Logger logger) {
        this(logger, Level.ALL);
    }

    /**
     * Creates a LoggingExtension with a custom logger and logging level.
     *
     * @param logger the logger to configure for console output
     * @param level  the logging level to set for both logger and console handler
     * @throws NullPointerException if logger or level is null
     */
    public LoggingExtension(Logger logger, Level level) {
        this.logger = logger;
        this.level = level;
    }

    /**
     * Configures the logger with console output before all tests run.
     * <p>
     * This method is called once per logger, regardless of how many
     * test classes use the same logger. It performs the following configuration:
     * </p>
     * <ul>
     *   <li>Creates a {@link ConsoleHandler} with the specified level</li>
     *   <li>Adds the handler to the logger</li>
     *   <li>Sets the logger's level</li>
     *   <li>Disables parent handler usage to prevent duplicate output</li>
     * </ul>
     *
     * @param context the extension context (unused but required by interface)
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        var loggerName = logger.getName();
        // Handle null logger names by using a default key
        var configKey = loggerName != null ? loggerName : "null-logger-" + System.identityHashCode(logger);

        if (CONFIGURED_LOGGERS.putIfAbsent(configKey, true) == null) {
            var handler = new ConsoleHandler();
            handler.setLevel(level);
            logger.addHandler(handler);
            logger.setLevel(level);
            logger.setUseParentHandlers(false);
        }
    }
}