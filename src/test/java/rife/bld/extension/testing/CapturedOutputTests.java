package rife.bld.extension.testing;

import org.junit.jupiter.api.Test;

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
}