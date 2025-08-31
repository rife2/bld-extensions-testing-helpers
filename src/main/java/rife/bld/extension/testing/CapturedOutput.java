package rife.bld.extension.testing;

import java.io.ByteArrayOutputStream;

/**
 * Container for captured stdout and stderr output during test execution.
 * <p>
 * This class provides methods to analyze and inspect console output that was
 * captured during a test method annotated with {@link CaptureOutput}. All captured
 * output is converted using the default character encoding.
 * <p>
 * Instances of this class are automatically created by the {@link CaptureOutputExtension}
 * and injected into test methods as parameters. The class offers various methods to
 * access and analyze the captured output:
 *
 * <ul>
 *     <li>String access methods: {@link #getOut()}, {@link #getErr()}, {@link #getAll()}</li>
 *     <li>Byte array access methods: {@link #getOutAsBytes()}, {@link #getErrAsBytes()}</li>
 *     <li>Utility methods: {@link #isEmpty()}, {@link #contains(String)}, {@link #outContains(String)},
 *     {@link #errContains(String)}</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 *
 * <blockquote><pre>
 * &#64;Test
 * &#64;CaptureOutput
 * void testConsoleOutput(CapturedOutput output) {
 *     System.out.println("Hello World");
 *     System.err.println("Error");
 *
 *     // Access individual streams
 *     assertEquals("Hello World\n", output.getOut());
 *     assertEquals("Error\n", output.getErr());
 *
 *     // Search within streams
 *     assertTrue(output.outContains("Hello"));
 *     assertTrue(output.errContains("Error"));
 *     assertTrue(output.contains("World"));
 *
 *     // Check if output is empty
 *     assertFalse(output.isEmpty());
 *
 *     // Get raw bytes if needed
 *     byte[] stdoutBytes = output.getOutAsBytes();
 * }</pre></blockquote>
 *
 * <p>
 * <strong>Thread Safety:</strong> This class is not thread-safe. Each instance
 * should only be used within the context of a single test method execution.
 *
 * @see CaptureOutput
 * @see CaptureOutputExtension
 */
@SuppressWarnings("ClassCanBeRecord")
public class CapturedOutput {
    private final ByteArrayOutputStream stderr;
    private final ByteArrayOutputStream stdout;

    /**
     * Creates a new CapturedOutput instance with the specified output streams.
     * <p>
     * This constructor is used internally by the {@link CaptureOutputExtension}
     * to wrap the captured stdout and stderr streams. It is not intended for direct
     * use by test code.
     *
     * @param stdout the ByteArrayOutputStream containing captured stdout data
     * @param stderr the ByteArrayOutputStream containing captured stderr data
     */
    CapturedOutput(ByteArrayOutputStream stdout, ByteArrayOutputStream stderr) {
        this.stdout = stdout;
        this.stderr = stderr;
    }

    /**
     * Searches for the specified text within both stdout and stderr content.
     * <p>
     * This method performs a case-sensitive substring search within both
     * the stdout and stderr content. It returns {@code true} if the text is
     * found in either stream. This is useful when you don't care which specific
     * stream contains the text, just that it was output somewhere.
     * <p>
     * This method is equivalent to:<br>
     * {@code outContains(text) || errContains(text)}
     *
     * @param text the text to search for in either stdout or stderr (must not be {@code null})
     * @return {@code true} if either stdout or stderr contains the specified text, {@code false} otherwise
     * @throws NullPointerException if text is {@code null}
     * @see #outContains(String)
     * @see #errContains(String)
     */
    public boolean contains(String text) {
        return outContains(text) || errContains(text);
    }

    /**
     * Searches for the specified text within the captured stderr content.
     * <p>
     * This method performs a case-sensitive substring search within the
     * stderr content. It's equivalent to calling {@code getErr().contains(text)}
     * but provides a more readable API for common assertions.
     *
     * @param text the text to search for in stderr (must not be {@code null})
     * @return {@code true} if stderr contains the specified text, {@code false} otherwise
     * @throws NullPointerException if text is {@code null}
     * @see #outContains(String)
     * @see #contains(String)
     * @see String#contains(CharSequence)
     */
    public boolean errContains(String text) {
        return getErr().contains(text);
    }

    /**
     * Combines and retrieves both stdout and stderr content as a single string.
     * <p>
     * This method concatenates the stdout content followed by the stderr content.
     * The order reflects the sequence in which output was written during test execution,
     * with stdout content appearing first, followed by stderr content.
     * <p>
     * <strong>Note:</strong> This does not preserve the exact interleaving of stdout and stderr
     * as it would appear in a real console, but rather groups all stdout first,
     * then all stderr.
     *
     * @return the combined stdout and stderr content, or empty string if no output was captured
     * @see #getOut()
     * @see #getErr()
     */
    public String getAll() {
        return getOut() + getErr();
    }

    /**
     * Retrieves the captured stderr content as a string.
     * <p>
     * This method converts all data written to {@code System.err} during the test
     * execution into a string using the default character encoding. Line separators
     * are preserved as they were originally written.
     *
     * @return the complete stderr content as a string, or empty string if no stderr was captured
     * @see #getOut()
     * @see #getAll()
     */
    public String getErr() {
        return stderr.toString();
    }

    /**
     * Retrieves the captured stderr content as a raw byte array.
     * <p>
     * This method provides access to the raw bytes that were written to stderr,
     * without any character encoding conversion. This can be useful for analyzing
     * binary data or when specific encoding handling is required.
     *
     * @return a copy of the stderr byte data, or empty array if no stderr was captured
     * @see #getErr()
     * @see #getOutAsBytes()
     */
    public byte[] getErrAsBytes() {
        return stderr.toByteArray();
    }

    /**
     * Retrieves the captured stdout content as a string.
     * <p>
     * This method converts all data written to {@code System.out} during the test
     * execution into a string using the default character encoding. Line separators
     * are preserved as they were originally written.
     *
     * @return the complete stdout content as a string, or empty string if no stdout was captured
     * @see #getErr()
     * @see #getAll()
     */
    public String getOut() {
        return stdout.toString();
    }

    /**
     * Retrieves the captured stdout content as a raw byte array.
     * <p>
     * This method provides access to the raw bytes that were written to stdout,
     * without any character encoding conversion. This can be useful for analyzing
     * binary data or when specific encoding handling is required.
     *
     * @return a copy of the stdout byte data, or empty array if no stdout was captured
     * @see #getOut()
     * @see #getErrAsBytes()
     */
    public byte[] getOutAsBytes() {
        return stdout.toByteArray();
    }

    /**
     * Determines if any output was captured to either stdout or stderr.
     * <p>
     * This method returns {@code true} only when both stdout and stderr
     * are completely empty (zero bytes captured). It's useful for verifying
     * that a test method produces no console output.
     *
     * @return {@code true} if both stdout and stderr are empty, {@code false} otherwise
     * @see #getOut()
     * @see #getErr()
     */
    public boolean isEmpty() {
        return stdout.size() == 0 && stderr.size() == 0;
    }

    /**
     * Searches for the specified text within the captured stdout content.
     * <p>
     * This method performs a case-sensitive substring search within the
     * stdout content. It's equivalent to calling {@code getOut().contains(text)}
     * but provides a more readable API for common assertions.
     *
     * @param text the text to search for in stdout (must not be {@code null})
     * @return {@code true} if stdout contains the specified text, {@code false} otherwise
     * @throws NullPointerException if text is {@code null}
     * @see #errContains(String)
     * @see #contains(String)
     * @see String#contains(CharSequence)
     */
    public boolean outContains(String text) {
        return getOut().contains(text);
    }

    /**
     * Provides a string representation of this captured output for debugging purposes.
     * <p>
     * The returned string includes both the stdout and stderr content in a
     * structured format that's useful for debugging test failures. The format
     * shows the class name and both captured streams.
     * <p>
     * Example output:<br>
     * {@code CapturedOutput{stdout='Hello World\n', stderr='Error\n'}}
     *
     * @return a string representation of the captured output
     */
    @Override
    public String toString() {
        return "CapturedOutput{" +
                "stdout='" + getOut() + '\'' +
                ", stderr='" + getErr() + '\'' +
                '}';
    }
}