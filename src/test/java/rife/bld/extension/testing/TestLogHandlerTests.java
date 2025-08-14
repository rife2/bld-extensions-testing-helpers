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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TestLogHandler}
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class TestLogHandlerTests {
    private TestLogHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TestLogHandler();
    }

    @Nested
    @DisplayName("Clear Functionality Tests")
    class ClearFunctionalityTests {

        @Test
        @DisplayName("Should clear all messages and records")
        void shouldClearAllMessagesAndRecords() {
            handler.publish(new LogRecord(Level.INFO, "Message 1"));
            handler.publish(new LogRecord(Level.WARNING, "Message 2"));

            assertEquals(2, handler.getLogMessages().size());
            assertEquals(2, handler.getLogRecords().size());

            handler.clear();

            assertTrue(handler.getLogMessages().isEmpty());
            assertTrue(handler.getLogRecords().isEmpty());
            assertFalse(handler.containsMessage("Message"));
            assertFalse(handler.hasLogLevel(Level.INFO));
        }

        @Test
        @DisplayName("Should handle clear on empty handler")
        void shouldHandleClearOnEmptyHandler() {
            handler.clear();

            assertTrue(handler.getLogMessages().isEmpty());
            assertTrue(handler.getLogRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @ParameterizedTest
        @ValueSource(strings = {"Case", "Sensitive"})
        @DisplayName("Should find case-sensitive messages")
        void shouldFindCaseSensitiveMessages(String searchTerm) {
            handler.publish(new LogRecord(Level.INFO, "Case Sensitive Message"));
            assertTrue(handler.containsMessage(searchTerm));
        }

        @Test
        @DisplayName("Should handle very long message")
        void shouldHandleVeryLongMessage() {
            var longMessage = "Long message ".repeat(10000);
            var record = new LogRecord(Level.INFO, longMessage);

            handler.publish(record);

            assertTrue(handler.containsMessage("Long message"));
            assertEquals(1, handler.countMessagesContaining("Long"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"case", "sensitive"})
        @DisplayName("Should not find lowercase in case-sensitive search")
        void shouldNotFindLowercaseInCaseSensitiveSearch(String searchTerm) {
            handler.publish(new LogRecord(Level.INFO, "Case Sensitive Message"));
            assertFalse(handler.containsMessage(searchTerm));
        }
    }

    @Nested
    @DisplayName("Handler Interface Tests")
    class HandlerInterfaceTests {

        static Stream<Runnable> handlerMethods() {
            return Stream.of(
                    () -> new TestLogHandler().close(),
                    () -> new TestLogHandler().flush()
            );
        }

        @ParameterizedTest
        @MethodSource("handlerMethods")
        @DisplayName("Should implement handler methods without exceptions")
        void shouldImplementHandlerMethodsWithoutExceptions(Runnable method) {
            assertDoesNotThrow(method::run);
        }
    }

    @Nested
    @DisplayName("Log Level Tests")
    class LogLevelTests {

        static Stream<Level> allLogLevels() {
            return Stream.of(
                    Level.INFO,
                    Level.WARNING,
                    Level.SEVERE,
                    Level.FINE,
                    Level.CONFIG,
                    Level.FINEST,
                    Level.FINER,
                    Level.ALL,
                    Level.OFF
            );
        }

        static Stream<Arguments> logLevelTestData() {
            return Stream.of(
                    Arguments.of(Level.INFO, "Info message", true),
                    Arguments.of(Level.WARNING, "Warning message", true),
                    Arguments.of(Level.SEVERE, "Error message", true),
                    Arguments.of(Level.FINE, null, false),
                    Arguments.of(Level.CONFIG, null, false)
            );
        }

        @ParameterizedTest
        @MethodSource("logLevelTestData")
        @DisplayName("Should detect log levels correctly")
        void shouldDetectLogLevelsCorrectly(Level level,
                                            @SuppressWarnings("unused") String message,
                                            boolean shouldExist) {
            // Publish the test levels
            handler.publish(new LogRecord(Level.INFO, "Info message"));
            handler.publish(new LogRecord(Level.WARNING, "Warning message"));
            handler.publish(new LogRecord(Level.SEVERE, "Error message"));

            assertEquals(shouldExist, handler.hasLogLevel(level));
        }

        @Test
        @DisplayName("Should return false for non-existent log level")
        void shouldReturnFalseForNonExistentLogLevel() {
            handler.publish(new LogRecord(Level.INFO, "Test message"));
            assertFalse(handler.hasLogLevel(Level.SEVERE));
        }

        @ParameterizedTest
        @MethodSource("allLogLevels")
        @DisplayName("Should return false for non-existent log level on empty handler")
        void shouldReturnFalseForNonExistentLogLevelOnEmptyHandler(Level level) {
            assertFalse(handler.hasLogLevel(level));
        }
    }

    @Nested
    @DisplayName("Message Publishing Tests")
    class MessagePublishingTests {
        @ParameterizedTest
        @ValueSource(strings = {"INFO", "WARNING", "SEVERE"})
        @DisplayName("Should publish log record and capture message")
        void shouldPublishLogRecordAndCaptureMessage(String levelName) {
            var level = Level.parse(levelName);
            var message = levelName.toLowerCase() + " message";
            var record = new LogRecord(level, message);

            handler.publish(record);

            var messages = handler.getLogMessages();
            var records = handler.getLogRecords();

            assertEquals(1, messages.size());
            assertEquals(1, records.size());
            assertEquals(message, messages.get(0));
            assertEquals(record, records.get(0));

            // Clear for the next iteration
            handler.clear();
        }

        @Test
        @DisplayName("Should publish multiple log records")
        void shouldPublishMultipleLogRecords() {
            var record1 = new LogRecord(Level.INFO, "First message");
            var record2 = new LogRecord(Level.WARNING, "Second message");
            var record3 = new LogRecord(Level.SEVERE, "Third message");

            handler.publish(record1);
            handler.publish(record2);
            handler.publish(record3);

            var messages = handler.getLogMessages();
            var records = handler.getLogRecords();

            assertEquals(3, messages.size());
            assertEquals(3, records.size());
            assertEquals("First message", messages.get(0));
            assertEquals("Second message", messages.get(1));
            assertEquals("Third message", messages.get(2));
        }
    }

    @Nested
    @DisplayName("Message Searching Tests")
    class MessageSearchingTests {
        @BeforeEach
        void setUpMessages() {
            handler.publish(new LogRecord(Level.INFO, "Hello world"));
            handler.publish(new LogRecord(Level.WARNING, "Warning: something happened"));
            handler.publish(new LogRecord(Level.SEVERE, "Error occurred"));
            handler.publish(new LogRecord(Level.INFO, "Hello again"));
        }

        @ParameterizedTest
        @CsvSource({
                "Hello, 2",
                "Warning, 1",
                "Error, 1",
                "Debug, 0",
                "world, 1",
                "occurred, 1"
        })
        @DisplayName("Should count messages containing text")
        void shouldCountMessagesContainingText(String searchText, int expectedCount) {
            assertEquals(expectedCount, handler.countMessagesContaining(searchText));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Hello world", "Warning: something happened", "Error occurred", "Hello again"})
        @DisplayName("Should find exact messages")
        void shouldFindExactMessages(String exactMessage) {
            assertTrue(handler.containsExactMessage(exactMessage));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Hello", "world", "Warning", "Error", "occurred", "something"})
        @DisplayName("Should find messages containing text")
        void shouldFindMessagesContainingText(String searchText) {
            assertTrue(handler.containsMessage(searchText));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Debug", "Non-existent", "xyz", "123"})
        @DisplayName("Should not find non-existent text in messages")
        void shouldNotFindNonExistentTextInMessages(String searchText) {
            assertFalse(handler.containsMessage(searchText));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Hello", "world", "Non-existent message", "partial"})
        @DisplayName("Should not find partial matches as exact messages")
        void shouldNotFindPartialMatchesAsExactMessages(String partialMessage) {
            // Only test messages that shouldn't be found as exact matches
            if (!"Hello world".equals(partialMessage) &&
                    !"Warning: something happened".equals(partialMessage) &&
                    !"Error occurred".equals(partialMessage) &&
                    !"Hello again".equals(partialMessage)) {
                assertFalse(handler.containsExactMessage(partialMessage));
            }
        }
    }

    @Nested
    @DisplayName("Null Value Handling Tests")
    class NullValueHandlingTests {

        @ParameterizedTest
        @CsvSource({
                "'', 0",           // Empty string should return 0 for null messages
                "test, 0"          // Any non-null string should return 0 for null messages
        })
        @DisplayName("Should handle counting with null messages")
        void shouldHandleCountingWithNullMessages(String searchTerm, int expectedCount) {
            handler.publish(new LogRecord(Level.INFO, null));
            handler.publish(new LogRecord(Level.WARNING, null));

            assertEquals(expectedCount, handler.countMessagesContaining(searchTerm));
        }

        @Test
        @DisplayName("Should handle exact message search with null messages present")
        void shouldHandleExactMessageSearchWithNullMessagesPresent() {
            handler.publish(new LogRecord(Level.INFO, null));
            handler.publish(new LogRecord(Level.WARNING, "Exact message"));
            handler.publish(new LogRecord(Level.SEVERE, null));

            assertTrue(handler.containsExactMessage("Exact message"));
            assertFalse(handler.containsExactMessage("Exact"));
            assertFalse(handler.containsExactMessage(null));
        }

        @Test
        @DisplayName("Should handle getFirstRecordContaining with null messages present")
        void shouldHandleGetFirstRecordContainingWithNullMessagesPresent() {
            var nullRecord1 = new LogRecord(Level.INFO, null);
            var validRecord = new LogRecord(Level.WARNING, "Find me");
            var nullRecord2 = new LogRecord(Level.SEVERE, null);

            handler.publish(nullRecord1);
            handler.publish(validRecord);
            handler.publish(nullRecord2);

            var result = handler.getFirstRecordContaining("Find");
            assertNotNull(result);
            assertEquals(validRecord, result);
            assertEquals("Find me", result.getMessage());

            var nullResult = handler.getFirstRecordContaining("nonexistent");
            assertNull(nullResult);
        }

        @Test
        @DisplayName("Should handle LogRecord with null message")
        void shouldHandleLogRecordWithNullMessage() {
            var record = new LogRecord(Level.INFO, null);

            handler.publish(record);

            assertEquals(1, handler.getLogRecords().size());
            assertEquals(1, handler.getLogMessages().size());
            assertNull(handler.getLogMessages().get(0));
        }

        @Test
        @DisplayName("Should handle mixed null and non-null messages")
        void shouldHandleMixedNullAndNonNullMessages() {
            handler.publish(new LogRecord(Level.INFO, null));
            handler.publish(new LogRecord(Level.WARNING, "Valid message"));
            handler.publish(new LogRecord(Level.SEVERE, null));
            handler.publish(new LogRecord(Level.INFO, "Another valid message"));

            assertEquals(4, handler.getLogRecords().size());
            assertEquals(4, handler.getLogMessages().size());

            // Check null messages are handled correctly
            assertNull(handler.getLogMessages().get(0));
            assertEquals("Valid message", handler.getLogMessages().get(1));
            assertNull(handler.getLogMessages().get(2));
            assertEquals("Another valid message", handler.getLogMessages().get(3));

            // Search should still work with non-null messages
            assertTrue(handler.containsMessage("Valid"));
            assertTrue(handler.containsMessage("Another"));
            assertEquals(1, handler.countMessagesContaining("Valid"));
            assertEquals(1, handler.countMessagesContaining("Another"));
        }

        @Test
        @DisplayName("Should handle null log level in hasLogLevel")
        void shouldHandleNullLogLevelInHasLogLevel() {
            handler.publish(new LogRecord(Level.INFO, "Test message"));

            assertDoesNotThrow(() -> {
                boolean result = handler.hasLogLevel(null);
                assertFalse(result);
            });
        }

        @Test
        @DisplayName("Should handle null LogRecord")
        void shouldHandleNullLogRecord() {
            // This should either throw an exception or handle gracefully
            // The behavior depends on the implementation requirements
            assertDoesNotThrow(() -> handler.publish(null));
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Should handle null search terms in containsExactMessage")
        void shouldHandleNullSearchTermsInContainsExactMessage(String nullSearchTerm) {
            handler.publish(new LogRecord(Level.INFO, "Test message"));

            assertDoesNotThrow(() -> {
                boolean result = handler.containsExactMessage(nullSearchTerm);
                assertFalse(result);
            });
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Should handle null search terms in containsMessage")
        void shouldHandleNullSearchTermsInContainsMessage(String nullSearchTerm) {
            handler.publish(new LogRecord(Level.INFO, "Test message"));

            // Should handle null gracefully - either return false or throw rhw appropriate exception
            assertDoesNotThrow(() -> {
                boolean result = handler.containsMessage(nullSearchTerm);
                // Most implementations would return false for null search terms
                assertFalse(result);
            });
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Should handle null search terms in countMessagesContaining")
        void shouldHandleNullSearchTermsInCountMessagesContaining(String nullSearchTerm) {
            handler.publish(new LogRecord(Level.INFO, "Test message"));

            assertDoesNotThrow(() -> {
                long count = handler.countMessagesContaining(nullSearchTerm);
                assertEquals(0, count);
            });
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Should handle null search terms in getFirstRecordContaining")
        void shouldHandleNullSearchTermsInGetFirstRecordContaining(String nullSearchTerm) {
            handler.publish(new LogRecord(Level.INFO, "Test message"));

            assertDoesNotThrow(() -> {
                var result = handler.getFirstRecordContaining(nullSearchTerm);
                assertNull(result);
            });
        }
    }

    @Nested
    @DisplayName("Record Retrieval Tests")
    class RecordRetrievalTests {
        static Stream<Arguments> recordRetrievalTestData() {
            return Stream.of(
                    Arguments.of("hello", "First hello message", Level.INFO),
                    Arguments.of("Second", "Second hello message", Level.WARNING),
                    Arguments.of("Different", "Different message", Level.SEVERE)
            );
        }

        @ParameterizedTest
        @MethodSource("recordRetrievalTestData")
        @DisplayName("Should get first record containing text")
        void shouldGetFirstRecordContainingText(String searchText, String expectedMessage, Level expectedLevel) {
            var record1 = new LogRecord(Level.INFO, "First hello message");
            var record2 = new LogRecord(Level.WARNING, "Second hello message");
            var record3 = new LogRecord(Level.SEVERE, "Different message");

            handler.publish(record1);
            handler.publish(record2);
            handler.publish(record3);

            var result = handler.getFirstRecordContaining(searchText);

            assertNotNull(result);
            assertEquals(expectedMessage, result.getMessage());
            assertEquals(expectedLevel, result.getLevel());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty search terms in record retrieval")
        void shouldHandleNullAndEmptySearchTermsInRecordRetrieval(String searchTerm) {
            handler.publish(new LogRecord(Level.INFO, "Test message"));

            var result = handler.getFirstRecordContaining(searchTerm);

            assertNull(result);
        }

        @Test
        @DisplayName("Should handle null messages in getFirstRecordContaining")
        void shouldHandleNullMessagesInGetFirstRecordContaining() {
            var nullRecord = new LogRecord(Level.INFO, null);
            var validRecord = new LogRecord(Level.WARNING, "Valid message");

            handler.publish(nullRecord);
            handler.publish(validRecord);

            var result = handler.getFirstRecordContaining("Valid");

            assertNotNull(result);
            assertEquals(validRecord, result);
        }

        @Test
        @DisplayName("Should return defensive copies of lists")
        void shouldReturnDefensiveCopiesOfLists() {
            handler.publish(new LogRecord(Level.INFO, "Test message"));

            var messages = handler.getLogMessages();
            var records = handler.getLogRecords();

            assertEquals(1, messages.size());
            assertEquals(1, records.size());

            // Modify returned lists
            messages.add("Modified");
            records.add(new LogRecord(Level.SEVERE, "Modified record"));

            // The original handler should be unchanged
            assertEquals(1, handler.getLogMessages().size());
            assertEquals(1, handler.getLogRecords().size());
        }

        @ParameterizedTest
        @ValueSource(strings = {"nonexistent", "missing", "notfound"})
        @DisplayName("Should return null when no record contains text")
        void shouldReturnNullWhenNoRecordContainsText(String searchText) {
            handler.publish(new LogRecord(Level.INFO, "Some message"));

            var result = handler.getFirstRecordContaining(searchText);

            assertNull(result);
        }
    }
}