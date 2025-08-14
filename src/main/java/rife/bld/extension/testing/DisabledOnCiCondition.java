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

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Disables tests on CI condition.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class DisabledOnCiCondition implements ExecutionCondition {
    /**
     * Returns {@code true} if the environment variable {@code CI} is set.
     *
     * @return {@code true} if the environment variable {@code CI} is set, {@code false} otherwise
     */
    public static boolean isCi() {
        return System.getenv("CI") != null;
    }

    /**
     * Evaluates the execution condition.
     *
     * @param context the current extension context; never {@code null}
     * @return the condition evaluation result
     */
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (isCi()) {
            return ConditionEvaluationResult.disabled("Test disabled on CI");
        } else {
            return ConditionEvaluationResult.enabled("Test enabled on CI");
        }
    }
}
