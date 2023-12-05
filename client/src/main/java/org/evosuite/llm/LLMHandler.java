package org.evosuite.llm;

import com.apollographql.apollo3.api.ApolloResponse;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;

public class LLMHandler {

    TestSuiteChromosome testSuite;
    GraphQLClient client;
    public LLMHandler(TestSuiteChromosome testSuite){
        this.testSuite = testSuite;
        this.client = new GraphQLClient();
    }

    public void improveUnderstandability(){
        LoggingUtils.getEvoLogger().info("** Improve Understandability: " + "Test Suite Size<" + testSuite.size() + "> Tests<" + this.testSuite.getTests().size() + ">");
        for (TestCase test: this.testSuite.getTests()){
            LoggingUtils.getEvoLogger().info("** Test Code is: " + test.toCode());
            ApolloResponse<PromptQuery.Data> result = client.promptQuery(test.toCode()).blockingGet();
            LoggingUtils.getEvoLogger().info("** Data is: " + result.data);
            if(result.data == null) {
                LoggingUtils.getEvoLogger().info("** Null Pointer Exception");
                break;
            }
            if(result.data.prompt.llmResponse.contains("ERROR:")){
                LoggingUtils.getEvoLogger().info("** Error is: " + result.data.prompt.llmResponse);
                break;
            }
//            result.errors.lis
        }
    }
}
