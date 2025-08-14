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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Custom log handler for capturing log messages during tests
 *
 * @since 1.0
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestLogHandler extends Handler {
    private final List<String> logMessages = new ArrayList<>();
    private final List<LogRecord> logRecords = new ArrayList<>();

    /**
     * Clears the log messages and records.
     */
    public void clear() {
        logRecords.clear();
        logMessages.clear();
    }

    /**
     * Checks if the log contains the exact message.
     *
     * @param message the message to check for
     * @return {@code true} if the log contains the message, {@code false} otherwise
     */
    public boolean containsExactMessage(String message) {
        return logMessages.stream().anyMatch(msg -> msg != null && msg.equals(message));
    }

    /**
     * Checks if the log contains a message containing the given text.
     *
     * @param message the text to check for
     * @return {@code true} if the log contains the message, {@code false} otherwise
     */
    public boolean containsMessage(String message) {
        return message != null && logMessages.stream().anyMatch(msg -> msg != null && msg.contains(message));
    }

    /**
     * Counts the number of messages containing the given text.
     *
     * @param message the text to check for
     * @return the number of messages containing the given text
     */
    public long countMessagesContaining(String message) {
        if (message == null || message.isEmpty()) {
            return 0;
        }
        return logMessages.stream()
                .filter(msg -> msg != null && msg.contains(message))
                .count();
    }

    /**
     * Gets the first log record containing the given text.
     *
     * @param message the text to check for
     * @return the first log record containing the given text, or {@code null} if not found
     */
    public LogRecord getFirstRecordContaining(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        return logRecords.stream()
                .filter(record -> record.getMessage() != null && record.getMessage().contains(message))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the log messages.
     *
     * @return the log messages
     */
    public List<String> getLogMessages() {
        return new ArrayList<>(logMessages);
    }

    /**
     * Gets the log records.
     *
     * @return the log records
     */
    public List<LogRecord> getLogRecords() {
        return new ArrayList<>(logRecords);
    }

    /**
     * Checks if the log contains a record with the given level.
     *
     * @param level the level to check for
     * @return {@code true} if the log contains a record with the given level, {@code false} otherwise
     */
    public boolean hasLogLevel(Level level) {
        return logRecords.stream().anyMatch(record -> record.getLevel().equals(level));
    }

    /**
     * Publishes a log record.
     *
     * @param record description of the log event. A null record is silently ignored and is not published
     */
    @Override
    public void publish(LogRecord record) {
        if (record == null) {
            return;
        }

        logRecords.add(record);
        logMessages.add(record.getMessage());
    }

    /**
     * Flushes this log handler.
     */
    @Override
    public void flush() {
        // no-op
    }

    /**
     * Closes this log handler.
     *
     * @throws SecurityException if a security manager exists and its {@code checkPermission} method denies
     */
    @Override
    public void close() throws SecurityException {
        //no-op
    }
}
