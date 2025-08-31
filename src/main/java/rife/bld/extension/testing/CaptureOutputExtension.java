package rife.bld.extension.testing;

import org.junit.jupiter.api.extension.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * JUnit extension that captures stdout and stderr during test execution.
 * <p>
 * This extension is automatically applied when using the {@link CaptureOutput}
 * annotation. It intercepts the standard output and error streams before test
 * execution and restores them afterward, making the captured content available
 * through parameter injection.
 * <p>
 * The extension implements the following JUnit 5 extension interfaces:
 * <ul>
 *     <li>{@link BeforeEachCallback} - Sets up output capture before each test</li>
 *     <li>{@link AfterEachCallback} - Restores original streams after each test</li>
 *     <li>{@link ParameterResolver} - Injects {@link CapturedOutput} parameters</li>
 * </ul>
 * <p>
 * <strong>Note:</strong> This class is not intended to be used directly.
 * Use the {@link CaptureOutput} annotation instead.
 *
 * @see CaptureOutput
 * @see CapturedOutput
 */
public class CaptureOutputExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static final String CAPTURED_OUTPUT_KEY = "capturedOutput";
    private ByteArrayOutputStream capturedErr;
    private ByteArrayOutputStream capturedOut;
    private PrintStream originalErr;
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
    }

    /**
     * Sets up output capture before each test method execution.
     * <p>
     * This method stores references to the original stdout and stderr streams,
     * creates new capture streams, and redirects system output to the capture streams.
     * The captured output is stored in the extension context for later parameter injection.
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

        // Replace system streams with capture streams
        System.setOut(new PrintStream(capturedOut));
        System.setErr(new PrintStream(capturedErr));

        // Store captured output in the extension context for parameter injection
        var output = new CapturedOutput(capturedOut, capturedErr);
        context.getStore(ExtensionContext.Namespace.create(getClass()))
                .put(CAPTURED_OUTPUT_KEY, output);
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
}