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
     * A string constant containing all uppercase letters, lowercase letters, and numeric digits.
     */
    public static final String ALPHANUMERIC_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    /**
     * A string constant containing all hexadecimal digits and letters.
     */
    public static final String HEXADECIMAL_CHARACTERS = "0123456789ABCDEF";
    /**
     * A string constant containing all lowercase letters.
     */
    public static final String LOWERCASE_CHARACTERS = "abcdefghijklmnopqrstuvwxyz";
    /**
     * A string constant containing all numeric digits.
     */
    public static final String NUMERIC_CHARACTERS = "0123456789";
    /**
     * A string constant containing all uppercase letters.
     */
    public static final String UPPERCASE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    /**
     * A string constant representing a set of characters that are safe for use in URLs.
     * <p>
     * It includes uppercase and lowercase letters, digits, and the symbols {@code -} and {@code _}.
     */
    public static final String URL_SAFE_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TestingUtils() {
    }

    /**
     * Generates a random integer within the specified range.
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
        return SECURE_RANDOM.nextInt((max - min) + 1) + min;
    }

    /**
     * Generates a random string with specified parameters.
     *
     * @param length     the desired length of the generated string
     * @param characters the character set to use
     * @return a randomly generated string of the specified length
     * @throws IllegalArgumentException if the length is non-positive or the character set is null/empty
     */
    public static String generateRandomString(int length, String characters) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }
        if (characters == null || characters.isEmpty()) {
            throw new IllegalArgumentException("Characters cannot be null or empty");
        }

        var result = new StringBuilder(length);
        var charLen = characters.length();
        var randomBytes = new byte[length];

        SECURE_RANDOM.nextBytes(randomBytes);

        for (int i = 0; i < length; i++) {
            // Mask to byte range [0,255], then reduce to [0, charLen)
            int idx = (randomBytes[i] & 0xFF) % charLen;
            result.append(characters.charAt(idx));
        }
        return result.toString();
    }

    /**
     * Generates a random string with default parameters.
     *
     * @return a 10-character random alphanumeric string
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
     */
    public static String generateRandomString(int length) {
        return generateRandomString(length, ALPHANUMERIC_CHARACTERS);
    }
}