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

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for disabling tests on CI/CD environments.
 *
 * <p>
 * The decision is made by checking whether the {@code CI} environment variable is defined. It can be set manually
 * but is set automatically by most CI/CD environments, such as
 * <a href="https://docs.github.com/en/actions/reference/workflows-and-actions/variables#default-environment-variables">GitHub Actions</a>,
 * <a href="https://docs.gitlab.com/ci/variables/predefined_variables/">GitLab</a>, etc.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @ExtendWith(RandomRangeParameterResolver.class)
 * public class MyTest {
 *     @Test
 *     public void testMethod(@RandomRange(min = 1, max = 100) int randomValue) {
 *         // randomNum will be a random integer between 1 and 10 (inclusive)
 *     }
 * }
 * }</pre>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledOnCiCondition.class)
public @interface DisabledOnCi {
}
