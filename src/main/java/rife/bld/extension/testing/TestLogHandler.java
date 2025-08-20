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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

/**
 * Thread-safe custom log handler for capturing log messages during tests.
 *
 * <h3>Usage Examples:</h3>
 *
 * <blockquote><pre>
 * // Using the LoggingExtension
 * private static final Logger LOGGER = Logger.getLogger(MyClass.class.getName());
 * private static final TestLogHandler TEST_LOG_HANDLER = new TestLogHandler();
 *
 * &#64;RegisterExtension
 * &#64;SuppressWarnings("unused")
 * private static final LoggingExtension LOGGING_EXTENSION = new LoggingExtension(
 *     LOGGER,
 *     TEST_LOG_HANDLER,
 *     Level.ALL
 * );
 *
 * // Manually, in a test method
 * &#64;Test
 * void testMethod() {
 *     var logger = Logger.getLogger(MyClass.class.getName());
 *     var logHandler = new TestLogHandler();
 *
 *     logger.addHandler(logHandler);
 *     logger.setLevel(Level.ALL);
 *
 *     // ...
 *
 *     logger.removeHandler(logHandler);
 * }</pre></blockquote>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see LoggingExtension
 * @since 1.0
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestLogHandler extends Handler {
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final List<LogRecord> logRecords = new CopyOnWriteArrayList<>();

    /**
     * Clears all captured log records and messages.
     * <p>
     * Thread-safe operation.
     */
    public void clear() {
        logRecords.clear();
    }

    /**
     * Checks if the log contains the exact message.
     *
     * @param message the message to check for
     * @return {@code true} if the log contains the message, {@code false} otherwise
     */
    public boolean containsExactMessage(String message) {
        return message != null && logRecords.stream().anyMatch(record -> message.equals(record.getMessage()));
    }

    /**
     * Checks if the log contains a message containing the given text.
     *
     * @param message the text to check for
     * @return {@code true} if the log contains a message with the text, {@code false} otherwise
     */
    public boolean containsMessage(String message) {
        return message != null && logRecords.stream().anyMatch(record ->
                record.getMessage() != null && record.getMessage().contains(message));
    }

    /**
     * Checks if the log contains a message matching the given regex pattern.
     *
     * @param pattern the regex pattern to match against
     * @return {@code true} if any message matches the pattern, {@code false} otherwise
     */
    public boolean containsMessageMatching(Pattern pattern) {
        return pattern != null && logRecords.stream().anyMatch(record ->
                record.getMessage() != null && pattern.matcher(record.getMessage()).find());
    }

    /**
     * Counts the number of messages containing the given text.
     *
     * @param message the text to check for
     * @return the number of messages containing the given text
     */
    public long countMessagesContaining(String message) {
        return message == null || message.isEmpty() ? 0 :
                logRecords.stream().filter(record ->
                        record.getMessage() != null && record.getMessage().contains(message)).count();
    }

    /**
     * Counts the number of log records at the specified level.
     *
     * @param level the log level to count
     * @return the number of records at the specified level
     */
    public long countRecordsAtLevel(Level level) {
        return level == null ? 0 :
                logRecords.stream().filter(record -> level.equals(record.getLevel())).count();
    }

    /**
     * Gets the first log record containing the given text.
     *
     * @param message the text to check for
     * @return the first log record containing the given text, or {@code null} if not found
     */
    public LogRecord getFirstRecordContaining(String message) {
        if (message != null && !message.isEmpty()) {
            return logRecords.stream()
                    .filter(record -> record.getMessage() != null && record.getMessage().contains(message))
                    .findFirst()
                    .orElse(null);
        } else {
            return null;
        }
    }

    /**
     * Gets the most recent log record, if any.
     *
     * @return the most recent log record, or {@code null} if no records exist
     */
    public LogRecord getLastRecord() {
        int size = logRecords.size();
        return size == 0 ? null : logRecords.get(size - 1);
    }

    /**
     * Gets the last log record containing the given text.
     *
     * @param message the text to check for
     * @return the last log record containing the given text, or {@code null} if not found
     */
    public LogRecord getLastRecordContaining(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        // Iterate backwards for efficiency
        for (int i = logRecords.size() - 1; i >= 0; i--) {
            LogRecord record = logRecords.get(i);
            if (record.getMessage() != null && record.getMessage().contains(message)) {
                return record;
            }
        }
        return null;
    }

    /**
     * Gets all captured log messages as strings.
     * <p>
     * Returns an immutable snapshot of current messages.
     *
     * @return immutable list of log messages
     */
    public List<String> getLogMessages() {
        return logRecords.stream()
                .map(LogRecord::getMessage)
                .toList();
    }

    /**
     * Gets all captured log records.
     * <p>
     * Returns an immutable snapshot of current records.
     *
     * @return immutable list of log records
     */
    public List<LogRecord> getLogRecords() {
        return List.copyOf(logRecords);
    }

    /**
     * Gets the total number of captured log records.
     *
     * @return the number of log records
     */
    public int getRecordCount() {
        return logRecords.size();
    }

    /**
     * Gets all log records at or above the specified level.
     *
     * @param level the minimum log level
     * @return immutable list of log records at or above the specified level
     */
    public List<LogRecord> getRecordsAtOrAboveLevel(Level level) {
        if (level == null) {
            return Collections.emptyList();
        } else {
            return logRecords.stream()
                    .filter(record -> record.getLevel().intValue() >= level.intValue())
                    .toList();
        }
    }

    /**
     * Checks if the log contains a record with the given level.
     *
     * @param level the level to check for
     * @return {@code true} if the log contains a record with the given level, {@code false} otherwise
     */
    public boolean hasLogLevel(Level level) {
        return level != null && logRecords.stream().anyMatch(record -> level.equals(record.getLevel()));
    }

    /**
     * Checks if this handler has been closed.
     *
     * @return {@code true} if the handler is closed, {@code false} otherwise
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Checks if any log records have been captured.
     *
     * @return {@code true} if no records have been captured, {@code false} otherwise
     */
    public boolean isEmpty() {
        return logRecords.isEmpty();
    }

    /**
     * Publishes a log record if the handler is not closed.
     *
     * @param record description of the log event. A null record is silently ignored and is not published
     */
    @Override
    public void publish(LogRecord record) {
        if (record == null || closed.get() || !isLoggable(record)) {
            return;
        }
        logRecords.add(record);
    }

    /**
     * Flushes this log handler.
     * <p>
     * No-op implementation as records are immediately available.
     */
    @Override
    public void flush() {
        // no-op - records are immediately available
    }

    /**
     * Closes this log handler and prevents further logging.
     * <p>
     * Thread-safe operation.
     */
    @Override
    public void close() {
        closed.set(true);
        logRecords.clear();
    }
}