package rife.bld.extension.testing;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JUnit annotation to capture stdout and stderr output for specific test methods.
 * <p>
 * This annotation can be applied to individual test methods to capture all
 * console output (both {@code System.out} and {@code System.err}) during test
 * execution. The captured output is then made available through a
 * {@link CapturedOutput} parameter that can be injected into the test method.
 * <p>
 * The annotation automatically handles the setup and teardown of output
 * capture, ensuring that the original stdout and stderr streams are properly
 * restored after each test execution.
 *
 * <h3>Usage Example:</h3>
 *
 * <blockquote><pre>
 * class MyTest {
 *     &#64;Test
 *     &#64;CaptureOutput
 *     void testConsoleOutput(CapturedOutput output) {
 *         System.out.println("Hello World");
 *         System.err.println("Error message");
 *
 *         assertEquals("Hello World\n", output.getOut());
 *         assertEquals("Error message\n", output.getErr());
 *         assertTrue(output.contains("Hello"));
 *     }
 *
 *     &#64;Test
 *     void regularTest() {
 *         // This test runs normally without output capture
 *         System.out.println("Goes to console");
 *     }
 * }</pre></blockquote>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *     <li>Targeted capture - only applies to annotated test methods</li>
 *     <li>Automatic cleanup - restores original streams after test execution</li>
 *     <li>Parameter injection - provides {@link CapturedOutput} for analysis</li>
 *     <li>Thread-safe - each test gets its own capture instance</li>
 * </ul>
 *
 * @see CapturedOutput
 * @see CaptureOutputExtension
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(CaptureOutputExtension.class)
public @interface CaptureOutput {
}