package rife.bld.extension.testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CapturedOutputTests {
    @Test
    void bothStreams() {
        var out = new java.io.ByteArrayOutputStream();
        var err = new java.io.ByteArrayOutputStream();
        out.writeBytes("Stdout output\n".getBytes());
        err.writeBytes("Stderr output\n".getBytes());
        var captured = new CapturedOutput(out, err);

        assertFalse(captured.isEmpty());
        assertEquals("Stdout output\n", captured.getOut());
        assertEquals("Stderr output\n", captured.getErr());
        assertEquals("Stdout output\nStderr output\n", captured.getAll());
        assertTrue(captured.outContains("Stdout"));
        assertTrue(captured.errContains("Stderr"));
        assertTrue(captured.contains("output"));
        assertTrue(captured.contains("Stdout"));
        assertTrue(captured.contains("Stderr"));
        assertFalse(captured.contains("NotThere"));
    }

    @Test
    void checkToStringFormat() {
        var out = new java.io.ByteArrayOutputStream();
        var err = new java.io.ByteArrayOutputStream();
        out.writeBytes("A\n".getBytes());
        err.writeBytes("B\n".getBytes());
        var captured = new CapturedOutput(out, err);

        var expected = "CapturedOutput{stdout='A\n', stderr='B\n'}";
        assertEquals(expected, captured.toString());
    }

    @Test
    void emptyOutput() {
        var out = new java.io.ByteArrayOutputStream();
        var err = new java.io.ByteArrayOutputStream();
        var captured = new CapturedOutput(out, err);

        assertTrue(captured.isEmpty());
        assertEquals("", captured.getOut());
        assertEquals("", captured.getErr());
        assertEquals("", captured.getAll());
        assertArrayEquals(new byte[0], captured.getOutAsBytes());
        assertArrayEquals(new byte[0], captured.getErrAsBytes());
        assertFalse(captured.contains("anything"));
        assertFalse(captured.outContains("foo"));
        assertFalse(captured.errContains("bar"));
    }

    @Test
    void nullArgumentChecks() {
        var out = new java.io.ByteArrayOutputStream();
        var err = new java.io.ByteArrayOutputStream();
        out.writeBytes("x".getBytes());
        err.writeBytes("y".getBytes());
        var captured = new CapturedOutput(out, err);

        assertThrows(NullPointerException.class, () -> captured.outContains(null));
        assertThrows(NullPointerException.class, () -> captured.errContains(null));
        assertThrows(NullPointerException.class, () -> captured.contains(null));
    }

    @Test
    void stderrOnly() {
        var out = new java.io.ByteArrayOutputStream();
        var err = new java.io.ByteArrayOutputStream();
        err.writeBytes("Error message\n".getBytes());
        var captured = new CapturedOutput(out, err);

        assertFalse(captured.isEmpty());
        assertEquals("", captured.getOut());
        assertEquals("Error message\n", captured.getErr());
        assertEquals("Error message\n", captured.getAll());
        assertFalse(captured.outContains("Hello"));
        assertTrue(captured.errContains("Error"));
        assertTrue(captured.contains("Error"));
        assertFalse(captured.contains("Hello"));
        assertArrayEquals(new byte[0], captured.getOutAsBytes());
        assertArrayEquals("Error message\n".getBytes(), captured.getErrAsBytes());
    }

    @Test
    void stdoutOnly() {
        var out = new java.io.ByteArrayOutputStream();
        var err = new java.io.ByteArrayOutputStream();
        out.writeBytes("Hello World\n".getBytes());
        var captured = new CapturedOutput(out, err);

        assertFalse(captured.isEmpty());
        assertEquals("Hello World\n", captured.getOut());
        assertEquals("", captured.getErr());
        assertEquals("Hello World\n", captured.getAll());
        assertTrue(captured.outContains("Hello"));
        assertFalse(captured.errContains("Error"));
        assertTrue(captured.contains("World"));
        assertFalse(captured.contains("Error"));
        assertArrayEquals("Hello World\n".getBytes(), captured.getOutAsBytes());
        assertArrayEquals(new byte[0], captured.getErrAsBytes());
    }

    @Nested
    @DisplayName("Get Lines Tests")
    class GetLinesTests {
        @Test
        void allLinesWithBothStreams() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            out.writeBytes("Stdout line 1\nStdout line 2\n".getBytes());
            err.writeBytes("Stderr line 1\nStderr line 2\n".getBytes());
            var captured = new CapturedOutput(out, err);

            var lines = captured.getAllLines();
            assertEquals(List.of("Stdout line 1", "Stdout line 2", "Stderr line 1", "Stderr line 2"), lines);
        }

        @Test
        void allLinesWithEmptyOutput() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            var captured = new CapturedOutput(out, err);

            var lines = captured.getAllLines();
            assertTrue(lines.isEmpty());
        }

        @Test
        void errLinesWithDifferentLineSeparators() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            // Test with different line separators: \n, \r\n, \r
            err.writeBytes("Error Unix\nError Windows\r\nError Mac\r".getBytes());
            var captured = new CapturedOutput(out, err);

            var lines = captured.getErrLines();
            assertEquals(List.of("Error Unix", "Error Windows", "Error Mac"), lines);
        }

        @Test
        void errLinesWithEmptyOutput() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            var captured = new CapturedOutput(out, err);

            var lines = captured.getErrLines();
            assertTrue(lines.isEmpty());
        }

        @Test
        void errLinesWithMultipleLines() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            err.writeBytes("Error 1\nError 2\nError 3\n".getBytes());
            var captured = new CapturedOutput(out, err);

            var lines = captured.getErrLines();
            assertEquals(List.of("Error 1", "Error 2", "Error 3"), lines);
        }

        @Test
        void errLinesWithSingleLine() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            err.writeBytes("Error message\n".getBytes());
            var captured = new CapturedOutput(out, err);

            var lines = captured.getErrLines();
            assertEquals(List.of("Error message"), lines);
        }

        @Test
        void linesWithTrailingNewlineHandling() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            // Content ending with a newline should not create an empty trailing line
            out.writeBytes("Line 1\nLine 2".getBytes()); // No trailing newline
            err.writeBytes("Error 1\nError 2\n".getBytes()); // With trailing newline
            var captured = new CapturedOutput(out, err);

            assertEquals(List.of("Line 1", "Line 2"), captured.getOutLines());
            assertEquals(List.of("Error 1", "Error 2"), captured.getErrLines());
            assertEquals(List.of("Line 1", "Line 2Error 1", "Error 2"), captured.getAllLines());
        }

        @Test
        void outLinesWithDifferentLineSeparators() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            // Test with different line separators: \n, \r\n, \r
            out.writeBytes("Unix\nWindows\r\nClassic Mac\r".getBytes());
            var captured = new CapturedOutput(out, err);

            var lines = captured.getOutLines();
            assertEquals(List.of("Unix", "Windows", "Classic Mac"), lines);
        }

        @Test
        void outLinesWithEmptyLines() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            out.writeBytes("Line 1\n\nLine 3\n".getBytes());
            var captured = new CapturedOutput(out, err);

            var lines = captured.getOutLines();
            assertEquals(List.of("Line 1", "", "Line 3"), lines);
        }

        @Test
        void outLinesWithEmptyOutput() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            var captured = new CapturedOutput(out, err);

            var lines = captured.getOutLines();
            assertTrue(lines.isEmpty());
        }

        @Test
        void outLinesWithMultipleLines() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            out.writeBytes("Line 1\nLine 2\nLine 3\n".getBytes());
            var captured = new CapturedOutput(out, err);

            var lines = captured.getOutLines();
            assertEquals(List.of("Line 1", "Line 2", "Line 3"), lines);
        }

        @Test
        void outLinesWithSingleLine() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            out.writeBytes("Hello World\n".getBytes());
            var captured = new CapturedOutput(out, err);

            var lines = captured.getOutLines();
            assertEquals(List.of("Hello World"), lines);
        }
    }
}