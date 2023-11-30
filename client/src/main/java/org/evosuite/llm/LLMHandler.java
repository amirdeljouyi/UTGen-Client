package org.evosuite.llm;

import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testsuite.TestSuiteChromosome;

public class LLMHandler {

    TestSuiteChromosome testSuite;
    GraphQLClient client;
    public LLMHandler(TestSuiteChromosome testSuite){
        this.testSuite = testSuite;
        this.client = new GraphQLClient();
    }

    public void improveUnderstandability(){
        for (TestCase test: this.testSuite.getTests()){
            client.promptQuery(test.toCode());
        }
    }
}
