package rife.bld.extension.testing;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.Locale;

/**
 * JUnit extension that captures stdout and stderr during test execution.
 * <p>
 * This extension is automatically applied when using the {@link CaptureOutput}
 * annotation. It intercepts the standard output and error streams before test
 * execution and restores them afterward, making the captured content available
 * through parameter injection with chronological tracking support.
 * <p>
 * The extension implements the following JUnit 5 extension interfaces:
 * <ul>
 *     <li>{@link BeforeEachCallback} - Sets up output capture before each test</li>
 *     <li>{@link AfterEachCallback} - Restores original streams after each test</li>
 *     <li>{@link ParameterResolver} - Injects {@link CapturedOutput} parameters</li>
 * </ul>
 * <p>
 * The extension supports chronological tracking of output, recording both
 * the type (stdout/stderr) and timestamp of each output operation.
 * <p>
 * <strong>Note:</strong> This class is not intended to be used directly.
 * Use the {@link CaptureOutput} annotation instead.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see CaptureOutput
 * @see CapturedOutput
 * @since 1.0
 */
public class CaptureOutputExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
    /**
     * The key used to store captured output in the extension context.
     */
    private static final String CAPTURED_OUTPUT_KEY = "capturedOutput";

    /**
     * The ByteArrayOutputStream for capturing stderr.
     */
    private ByteArrayOutputStream capturedErr;

    /**
     * The ByteArrayOutputStream for capturing stdout.
     */
    private ByteArrayOutputStream capturedOut;
    /**
     * The CapturedOutput instance for chronological tracking.
     */
    @SuppressWarnings("PMD.SingularField")
    private CapturedOutput capturedOutput;
    /**
     * The original stderr PrintStream before capture began.
     */
    private PrintStream originalErr;
    /**
     * The original stdout PrintStream before capture began.
     */
    private PrintStream originalOut;

    /**
     * Restores original output streams after each test method execution.
     * <p>
     * This method restores the original stdout and stderr streams and closes
     * the capture streams to free up resources. This ensures that later
     * tests and framework output work normally.
     *
     * @param context the current extension context
     * @throws Exception if an error occurs during cleanup
     */
    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // Restore original streams
        System.setOut(originalOut);
        System.setErr(originalErr);

        // Clean up
        if (capturedOut != null) {
            capturedOut.close();
        }
        if (capturedErr != null) {
            capturedErr.close();
        }

        // Clear reference
        capturedOutput = null;
    }

    /**
     * Sets up output capture before each test method execution.
     * <p>
     * This method stores references to the original stdout and stderr streams,
     * creates new capture streams with chronological tracking, and redirects
     * system output to the capture streams. The captured output is stored in
     * the extension context for later parameter injection.
     *
     * @param context the current extension context
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        // Store original streams
        originalOut = System.out;
        originalErr = System.err;

        // Create capture streams
        capturedOut = new ByteArrayOutputStream();
        capturedErr = new ByteArrayOutputStream();

        // Create the captured output instance
        capturedOutput = new CapturedOutput(capturedOut, capturedErr);

        // Replace system streams with chronological tracking streams
        System.setOut(new ChronologicalPrintStream(capturedOut, CapturedOutput.OutputType.STDOUT, capturedOutput));
        System.setErr(new ChronologicalPrintStream(capturedErr, CapturedOutput.OutputType.STDERR, capturedOutput));

        // Store captured output in the extension context for parameter injection
        context.getStore(ExtensionContext.Namespace.create(getClass()))
                .put(CAPTURED_OUTPUT_KEY, capturedOutput);
    }

    /**
     * Determines if this extension can resolve the given parameter.
     * <p>
     * This method returns {@code true} only for parameters of type
     * {@link CapturedOutput}, enabling automatic injection of the captured
     * output data into test methods.
     *
     * @param parameterContext the context for the parameter for which resolution is attempted
     * @param extensionContext the extension context for the executable about to be invoked
     * @return {@code true} if the parameter type is {@link CapturedOutput}
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == CapturedOutput.class;
    }

    /**
     * Resolves the {@link CapturedOutput} parameter for injection into test methods.
     * <p>
     * This method retrieves the captured output instance that was stored
     * during the {@link #beforeEach(ExtensionContext)} callback and returns
     * it for injection into the test method parameter.
     *
     * @param parameterContext the context for the parameter for which resolution is attempted
     * @param extensionContext the extension context for the executable about to be invoked
     * @return the {@link CapturedOutput} instance containing captured stdout and stderr
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return extensionContext.getStore(ExtensionContext.Namespace.create(getClass()))
                .get(CAPTURED_OUTPUT_KEY, CapturedOutput.class);
    }

    /**
     * A specialized PrintStream that tracks chronological output with timestamps.
     * <p>
     * This PrintStream wrapper captures output to the underlying stream while
     * simultaneously recording entries with timestamps and output types for
     * chronological tracking.
     */
    private static class ChronologicalPrintStream extends PrintStream {
        /**
         * The CapturedOutput instance to record chronological entries.
         */
        private final CapturedOutput capturedOutput;
        /**
         * The output type (STDOUT or STDERR) for this stream.
         */
        private final CapturedOutput.OutputType outputType;

        /**
         * Creates a new ChronologicalPrintStream.
         *
         * @param out            the underlying output stream
         * @param outputType     the type of output (STDOUT or STDERR)
         * @param capturedOutput the CapturedOutput instance to record entries
         */
        ChronologicalPrintStream(ByteArrayOutputStream out, CapturedOutput.OutputType outputType, CapturedOutput capturedOutput) {
            super(out);
            this.outputType = outputType;
            this.capturedOutput = capturedOutput;
        }

        /**
         * Prints a boolean value and records it chronologically.
         *
         * @param b the boolean to print
         */
        @Override
        public void print(boolean b) {
            var content = String.valueOf(b);
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints a character and records it chronologically.
         *
         * @param c the character to print
         */
        @Override
        public void print(char c) {
            var content = String.valueOf(c);
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints an integer and records it chronologically.
         *
         * @param i the integer to print
         */
        @Override
        public void print(int i) {
            var content = String.valueOf(i);
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints the specified long and records it chronologically.
         *
         * @param l the long to print
         */
        @Override
        public void print(long l) {
            var content = String.valueOf(l);
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints a float and records it chronologically.
         *
         * @param f the float to print
         */
        @Override
        public void print(float f) {
            var content = String.valueOf(f);
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints a double and records it chronologically.
         *
         * @param d the double to print
         */
        @Override
        public void print(double d) {
            var content = String.valueOf(d);
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints a character array and records it chronologically.
         *
         * @param s the character array to print
         */
        @Override
        public void print(char @NotNull [] s) {
            var content = String.valueOf(s);
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints a string and records it chronologically.
         *
         * @param s the string to print
         */
        @Override
        public void print(String s) {
            super.print(s);
            if (s != null) {
                capturedOutput.addEntry(outputType, s, Instant.now());
            }
        }

        /**
         * Prints an object and records it chronologically.
         *
         * @param obj the object to print
         */
        @Override
        public void print(Object obj) {
            var content = String.valueOf(obj);
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints a line separator and records it chronologically.
         */
        @Override
        public void println() {
            var content = System.lineSeparator();
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints a boolean value followed by a line separator and records it chronologically.
         *
         * @param b the boolean to print
         */
        @Override
        public void println(boolean b) {
            var content = b + System.lineSeparator();
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints a character followed by a line separator and records it chronologically.
         *
         * @param c the character to print
         */
        @Override
        public void println(char c) {
            var content = c + System.lineSeparator();
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints an integer followed by a line separator and records it chronologically.
         *
         * @param i the integer to print
         */
        @Override
        public void println(int i) {
            var content = i + System.lineSeparator();
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints a long followed by a line separator and records it chronologically.
         *
         * @param l the long to print
         */
        @Override
        public void println(long l) {
            var content = l + System.lineSeparator();
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints a float followed by a line separator and records it chronologically.
         *
         * @param f the float to print
         */
        @Override
        public void println(float f) {
            var content = f + System.lineSeparator();
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints a double followed by a line separator and records it chronologically.
         *
         * @param d the double to print
         */
        @Override
        public void println(double d) {
            var content = d + System.lineSeparator();
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints a character array followed by a line separator and records it chronologically.
         *
         * @param s the character array to print
         */
        @Override
        public void println(char @NotNull [] s) {
            var content = String.valueOf(s) + System.lineSeparator();
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints a string followed by a line separator and records it chronologically.
         *
         * @param s the string to print
         */
        @Override
        public void println(String s) {
            var content = s + System.lineSeparator();
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints an object followed by a line separator and records it chronologically.
         *
         * @param obj the object to print
         */
        @Override
        public void println(Object obj) {
            var content = obj + System.lineSeparator();
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
        }

        /**
         * Prints formatted output. Records it chronologically.
         *
         * @param format the format string
         * @param args   the arguments for the format string
         * @return this PrintStream
         */
        @Override
        public PrintStream printf(@NotNull String format, Object... args) {
            var content = String.format(format, args);
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
            return this;
        }

        /**
         * Prints formatted output. Records it chronologically.
         *
         * @param locale the locale to use for formatting
         * @param format the format string
         * @param args   the arguments for the format string
         * @return this PrintStream
         */
        @Override
        public PrintStream printf(Locale locale, @NotNull String format, Object... args) {
            var content = String.format(locale, format, args);
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
            return this;
        }

        /**
         * Appends a character sequence and records it chronologically.
         *
         * @param csq the character sequence to append (can be null)
         * @return this PrintStream
         */
        @Override
        public PrintStream append(CharSequence csq) {
            var content = String.valueOf(csq);
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
            return this;
        }

        /**
         * Appends a subsequence of a character sequence and records it chronologically.
         *
         * @param csq   the character sequence to append (can be null)
         * @param start the start index of the subsequence
         * @param end   the end index of the subsequence
         * @return this PrintStream
         */
        @Override
        public PrintStream append(CharSequence csq, int start, int end) {
            var content = csq == null ? "null" : String.valueOf(csq.subSequence(start, end));
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
            return this;
        }

        /**
         * Appends a character and records it chronologically.
         *
         * @param c the character to append
         * @return this PrintStream
         */
        @Override
        public PrintStream append(char c) {
            var content = String.valueOf(c);
            super.print(content);
            capturedOutput.addEntry(outputType, content, Instant.now());
            return this;
        }
    }
}