/*
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.junit.naming.methods;

import org.evosuite.Properties;
import org.evosuite.coverage.FitnessFunctions;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.coverage.exception.ExceptionCoverageTestFitness;
import org.evosuite.coverage.io.input.InputCoverageTestFitness;
import org.evosuite.coverage.io.input.InputObserver;
import org.evosuite.coverage.io.output.OutputCoverageTestFitness;
import org.evosuite.coverage.io.output.OutputObserver;
import org.evosuite.coverage.method.MethodCoverageTestFitness;
import org.evosuite.coverage.method.MethodNoExceptionCoverageTestFitness;
import org.evosuite.llm.LLMHandler;
import org.evosuite.runtime.mock.EvoSuiteMock;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionObserver;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.TestCaseExecutor;
import org.evosuite.testcase.statements.ConstructorStatement;
import org.evosuite.testcase.statements.MethodStatement;
import org.evosuite.testcase.statements.Statement;
import org.objectweb.asm.Type;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Gordon Fraser
 * @author Ermira Daka
 */
public class LLMBasedTestNameGenerationStrategy implements TestNameGenerationStrategy {

    private final Map<TestCase, String> testToName = new LinkedHashMap<>();

    private final Map<String, Set<String>> methodCount = new LinkedHashMap<>();

    private CoverageGoalTestNameGenerationStrategy coverageGoalStrategy;

    private final LLMHandler llm = new LLMHandler();

    public static final int MAX_SIMILAR_GOALS = 2;

    public static final int MAX_CHARS = 70;

    public LLMBasedTestNameGenerationStrategy(List<TestCase> testCases, List<ExecutionResult> results) {
        coverageGoalStrategy = new CoverageGoalTestNameGenerationStrategy(testCases, results);
        generateNames(testCases);
    }

    /**
     * This assumes all goals are already saved in the tests
     *
     * @param testCases
     */
    public LLMBasedTestNameGenerationStrategy(List<TestCase> testCases) {
        coverageGoalStrategy = new CoverageGoalTestNameGenerationStrategy(testCases);
        generateNames(testCases);
    }

    /**
     * Calculate the test names from the current goals
     *
     * @param testToGoals
     */
    private void setTestNames(Map<TestCase, Set<TestFitnessFunction>> testToGoals) {
        for (Map.Entry<TestCase, Set<TestFitnessFunction>> entry : testToGoals.entrySet()) {
            testToName.put(entry.getKey(), getTestName(entry.getKey()));
        }
    }

    private String getTestName(TestCase test) {
        String suggestedTestName = llm.suggestTestName(test.toCode());
        if(suggestedTestName == null)
            return coverageGoalStrategy.getName(test);
        return suggestedTestName;
    }


    private void generateNames(List<TestCase> testCases) {
        for (TestCase testCase: testCases) {
            testToName.put(testCase, getTestName(testCase));
        }

        // Add numbers to remaining duplicate names
        fixAmbiguousTestNames();
    }

    /**
     * There may be tests with the same calculated name, in which case we add a number suffix
     */
    private void fixAmbiguousTestNames() {
        Map<String, Integer> nameCount = new LinkedHashMap<>();
        Map<String, Integer> testCount = new LinkedHashMap<>();
        for (String methodName : testToName.values()) {
            if (nameCount.containsKey(methodName))
                nameCount.put(methodName, nameCount.get(methodName) + 1);
            else {
                nameCount.put(methodName, 1);
                testCount.put(methodName, 0);
            }
        }
        for (Map.Entry<TestCase, String> entry : testToName.entrySet()) {
            if (nameCount.get(entry.getValue()) > 1) {
                int num = testCount.get(entry.getValue());
                testCount.put(entry.getValue(), num + 1);
                testToName.put(entry.getKey(), entry.getValue() + num);
            }
        }
    }

    @Override
    public String getName(TestCase test) {
        return testToName.get(test);
    }
}

