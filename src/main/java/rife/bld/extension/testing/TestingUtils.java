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

import java.security.SecureRandom;

@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class TestingUtils {
    /**
     * A string containing the uppercase and lowercase letters of the English alphabet,
     * as well as numeric digits 0 through 9.
     */
    public static final String ALPHANUMERIC_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    /**
     * A string containing the set of characters used in hexadecimal notation.
     */
    public static final String HEXADECIMAL_CHARACTERS = "0123456789ABCDEF";
    /**
     * A string containing all lowercase alphabetical characters.
     */
    public static final String LOWERCASE_CHARACTERS = "abcdefghijklmnopqrstuvwxyz";
    /**
     * A string containing all numeric characters.
     */
    public static final String NUMERIC_CHARACTERS = "0123456789";
    /**
     * A string containing all uppercase alphabetical characters.
     */
    public static final String UPPERCASE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    /**
     * A string containing the set of characters used in URL-safe Base64 notation.
     */
    public static final String URL_SAFE_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TestingUtils() {
        // no-op
    }

    /**
     * Generates a random integer within the specified range.
     * <p>
     * This method uses a secure random generator to generate a number inclusively
     * between {@code min} and {@code max}.
     *
     * @param min the minimum value (inclusive) of the random number
     * @param max the maximum value (inclusive) of the random number
     * @return a random integer between {@code min} and {@code max}, inclusive
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}
     */
    public static int generateRandomInt(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException(
                    String.format("The minimum value (%d) cannot be greater than maximum value (%d)", min, max));
        }
        return SECURE_RANDOM.nextInt(max - min + 1) + min;
    }

    /**
     * Generates a random string with specified parameters.
     *
     * <p>
     * This method uses {@link SecureRandom} to generate random bytes, which are then
     * mapped to the provided character set using modulo operation. The resulting string
     * has a uniform distribution across the character set.
     *
     * <h4>Algorithm:</h4>
     * <ol>
     *   <li>Generate {@code length} random bytes using {@link SecureRandom}</li>
     *   <li>For each byte, compute {@code Math.abs(byte) % characters.length()}</li>
     *   <li>Use the result as an index into the character set</li>
     *   <li>Append the selected character to the result</li>
     * </ol>
     *
     * @param length     the desired length of the generated string
     * @param characters the character set to use
     * @return a randomly generated string of the specified length
     * @throws IllegalArgumentException if the length is non-positive or the character set is null/empty
     * @see SecureRandom#nextBytes(byte[])
     */
    public static String generateRandomString(int length, String characters) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }

        if (characters == null || characters.isEmpty()) {
            throw new IllegalArgumentException("Characters cannot be null or empty");
        }

        var result = new StringBuilder(length);
        var randomBytes = new byte[length];
        SECURE_RANDOM.nextBytes(randomBytes);

        for (int i = 0; i < length; i++) {
            var randomIndex = Math.abs(randomBytes[i]) % characters.length();
            result.append(characters.charAt(randomIndex));
        }

        return result.toString();
    }

    /**
     * Generates a random string with default parameters.
     *
     * @return a 10-character random alphanumeric string
     * @see #generateRandomString(int, String)
     */
    public static String generateRandomString() {
        return generateRandomString(10, ALPHANUMERIC_CHARACTERS);
    }

    /**
     * Generates a random string with a specified length.
     *
     * @param length the desired length of the generated string
     * @return a random alphanumeric string of the specified length
     * @throws IllegalArgumentException if length is non-positive
     * @see #generateRandomString(int, String)
     */
    public static String generateRandomString(int length) {
        return generateRandomString(length, ALPHANUMERIC_CHARACTERS);
    }
}
