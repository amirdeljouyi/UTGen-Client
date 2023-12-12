package org.evosuite.llm;

import com.apollographql.apollo3.api.ApolloResponse;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.SunUnsafeReflectionProvider;
import com.thoughtworks.xstream.security.AnyTypePermission;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.statements.EntityWithParametersStatement;
import org.evosuite.testcase.variable.VariableReference;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;
import org.junit.Test;
//import spoon.reflect.code.CtBlock;
//import spoon.reflect.code.CtLocalVariable;
//import spoon.reflect.code.CtStatement;
//import spoon.reflect.declaration.CtClass;
//import spoon.reflect.declaration.CtMethod;

import static spoon.Launcher.*;

public class LLMHandler {

//    TestSuiteChromosome testSuite;
    GraphQLClient client;
    public LLMHandler(){
//        this.testSuite = testSuite;
        this.client = new GraphQLClient();
    }

    public String improveUnderstandability(String test){
        LoggingUtils.getEvoLogger().info("** Improve Understandability: " + "** Test Code is: " + test);

        try {
//            XStream xstream = new XStream(new SunUnsafeReflectionProvider()); // XML
//            xstream.ignoreUnknownElements();
//            xstream.addPermission(AnyTypePermission.ANY);
//                Path p = FileSystems.getDefault().getPath("SerializedTest" + test.getID() + ".xml");
//                BufferedWriter bw = Files.newBufferedWriter(p, StandardCharsets.UTF_8);
//                if (this.testSuite == null || bw == null) {
//                    throw new NullPointerException();
//                }
//
//                bw.write(xstream.toXML(test));
//                bw.flush();

                ApolloResponse<PromptQuery.Data> result = client.promptQuery(test).blockingGet();
//                LoggingUtils.getEvoLogger().info("** Data is: " + result.data);
                if(result.data == null) {
                    LoggingUtils.getEvoLogger().info("** Null Pointer Exception");
                    return null;
                }
                String testResponse = result.data.prompt.llmResponse;
                if(testResponse.contains("ERROR:")){
                    LoggingUtils.getEvoLogger().info("** Error is: " + result.data.prompt.llmResponse);
                    return null;
                }
                LoggingUtils.getEvoLogger().info("Test Response is: " + testResponse);
                return parseLLMTest(testResponse);
            } catch (Exception exp) {
            throw new RuntimeException(exp);
        }
    }

    public String parseLLMTest(String testResponse){
//        String dummyClass = "class DummyClass { " + testResponse + " }";
//        LoggingUtils.getEvoLogger().info("Dummy Class is: " + dummyClass);
//        CtClass<?> ctClass = parseClass(dummyClass);
//        TestCase testNew = test.clone();
//        int statementSizeOfTestNew = testNew.size();
//        LoggingUtils.getEvoLogger().info("statement size is: " + statementSizeOfTestNew);
//
//        for(int i=0; i<statementSizeOfTestNew; i++){
//            LoggingUtils.getEvoLogger().info("statement [" + i + "] is: " + testNew.getStatement(i) + " . " + testNew.getStatement(i).getClass());
//        }

//        for(CtMethod<?> ctMethod: ctClass.getMethods()){
//            CtBlock<?> body = ctMethod.getBody();
//            String bodyString = body.toString();
            return removeTwoFirstAndLast(testResponse);

//            int i = 0;
//            for(CtStatement statement: body.getStatements()){
//                if(i == statementSizeOfTestNew){
//                    break;
//                }
//                LoggingUtils.getEvoLogger().info(i + ": " + statement + " . " + statement.getClass());
//
//                if(statement instanceof CtLocalVariable && testNew.getStatement(i) instanceof EntityWithParametersStatement) {
//                    CtLocalVariable<?> local = (CtLocalVariable<?>) statement;
//                    EntityWithParametersStatement entity = (EntityWithParametersStatement) testNew.getStatement(i);
////                    System.out.println("assignment is: " + local.getAssignment());
//                    LoggingUtils.getEvoLogger().info("reference is: " + local.getReference());
////                    System.out.println("Label is: " + local.getLabel());
//                    for(VariableReference vc:entity.getVariableReferences()){
//                        LoggingUtils.getEvoLogger().info("variable reference is: " + vc.getName() + " . ");
//                    }
//                    LoggingUtils.getEvoLogger().info("Testcase position is: " + entity + " . " + entity.getClass());
//
//                }
//                i++;
//            }
//        }
//        return null;
//        return testNew;

    }

    public void variableNaming(TestCase testcase, int position, String newVariable){
//        testcase.getStatement(position).
    }

    public static String removeTwoFirstAndLast(String str)
    {
        String[] lines = str.split("\\r?\\n");

        // Use a StringBuilder to efficiently build the final string
        StringBuilder result = new StringBuilder();
        LoggingUtils.getEvoLogger().info("Lines size: " + lines.length);

        // Append lines to the result, skipping the first two and the last one
        for (int i = 3; i < lines.length - 1; i++) {
            result.append(removeTab(lines[i]));
            if (i < lines.length - 2) {
                result.append("\n");
            }
        }

        LoggingUtils.getEvoLogger().info("refined test is: " + result.toString());

        return result.toString();
    }

    public static String removeTab(String str)
    {
        if(str.length() < 5)
            return str;

        // Creating a StringBuilder object
        StringBuilder sb = new StringBuilder(str);

        // Removing the first character
        // of a string
        sb.deleteCharAt(0);
        sb.deleteCharAt(0);
        sb.deleteCharAt(0);
        sb.deleteCharAt(0);

        // Converting StringBuilder into a string
        // and return the modified string
        return sb.toString();
    }

}
