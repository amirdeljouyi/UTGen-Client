package org.evosuite.llm;

import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.statements.*;
import org.evosuite.testcase.statements.numeric.IntPrimitiveStatement;
import org.evosuite.testcase.variable.VariableReference;
import org.evosuite.utils.LoggingUtils;
import spoon.Launcher;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.support.reflect.code.CtVariableReadImpl;

import java.util.*;

public class Parser {

    HashMap<String, VariableReference> variableReferences = new HashMap<>();
    HashMap<String, Integer> calleeStatements = new HashMap<>();
    HashMap<Integer, Integer> statementsIndex = new HashMap<>();
    int position = 0;
    TestCase oldTestCase;


    public Parser(TestCase oldTestCase){
        this.oldTestCase = oldTestCase.clone();
    }

    public TestCase parseTestSnippet(String improvedTest) {
        String dummyClass = "class DummyClass { public static void main (){" + improvedTest + " }}";
        CtClass<?> ctClass = Launcher.parseClass(dummyClass);

        TestCase testCase = new DefaultTestCase();
        for (CtMethod<?> ctMethod : ctClass.getMethods()) {
            CtBlock<?> body = ctMethod.getBody();
            for (CtStatement statement : body.getStatements()) {
                LoggingUtils.getEvoLogger().info("statement is: " + statement + ", " + statement.getClass());

                if (statement instanceof CtLocalVariable) {
                    CtLocalVariable<?> stmType = (CtLocalVariable<?>) statement;
                    CtLocalVariableReference<?> reference = stmType.getReference();
                    CtExpression<?> assignment = stmType.getAssignment();
                    LoggingUtils.getEvoLogger().info(assignment.getClass().toString());
                    if (assignment instanceof CtLiteral) {
                        Statement stm = parsePrimitiveType(testCase, assignment, reference.getType().getActualClass());
                        if (stm != null) {
                            addVariableStatement(testCase, stm, stmType.getSimpleName());
                        }
                    } else if(assignment instanceof CtUnaryOperator){
                        Statement stm = parseUnaryOperator(testCase, assignment, reference);
                        if (stm != null) {
                            addVariableStatement(testCase, stm, stmType.getSimpleName());
                        }
                    } else if (assignment instanceof CtConstructorCall) {
                        Statement stm = parseConstructorCall(testCase, assignment);
                        if (stm != null) {
                            addVariableStatement(testCase, stm, stmType.getSimpleName());
                        }
                    } else if(assignment instanceof CtInvocation){
                        Statement stm = parseMethodStatement(testCase, (CtInvocation<?>) assignment);
                        if (stm != null) {
                            addVariableStatement(testCase, stm, stmType.getSimpleName());
                        }
                    }
                }

                if (statement instanceof CtInvocation) {
                    CtInvocation<?> stmType = (CtInvocation<?>) statement;
                    Statement stm = parseMethodStatement(testCase, stmType);
                    if (stm != null) {
                        addStatement(testCase, stm);
//                        LoggingUtils.getEvoLogger().info(stm.toString());
                    }
                }
            }
        }

        int unusedVars = testCase.size() - position;
        if(unusedVars>0)
            for(int i=position; i<testCase.size(); i++)
                testCase.remove(i);

        for (Statement s : testCase) {
            if (s instanceof PrimitiveStatement) {
                VariableReference var = s.getReturnValue();
                if (!testCase.hasReferences(var)) {
                    LoggingUtils.getEvoLogger().info("* this is vulnerable: " + s);
                }
            }
        }

//        LoggingUtils.getEvoLogger().info("TestCase is: " + testCase.toCode());
//        testCase = recreate(testCase);

//        removeUnusedVariables(testCase);
//        System.out.println(ctClass);
//
        if(!testCase.isEmpty())
            LoggingUtils.getEvoLogger().info("TestCase is: " + testCase.toCode());

        return testCase;
    }


    public ConstructorStatement parseConstructorCall(TestCase testCase, CtExpression<?> assignment) {
        CtConstructorCall<?> constructorCall = ((CtConstructorCall<?>) assignment);

        ConstructorStatement matched = matches(constructorCall);
        if(matched == null)
            return null;

        List<CtExpression<?>> args = constructorCall.getArguments();
        ArrayList<VariableReference> parameters = new ArrayList<>();
        for (CtExpression<?> expression : args) {
            VariableReference variableReference = parseArgStatement(testCase, expression);
            if(variableReference!=null)
                parameters.add(variableReference);
        }

        for (int i=0; i < Math.min(parameters.size(), matched.getNumParameters()); i++){
            matched.replaceParameterReference(parameters.get(i), i);
        }

        return matched;
//        if (matched != null) {
//            evoStm = new ConstructorStatement(testCase, matched.getConstructor(), parameters);
//            // Now you have the java.lang.reflect.Constructor object
//        }
//
//        return evoStm;
    }

    private ConstructorStatement matches(CtConstructorCall<?> constructorCall) {
        for (Statement statement : oldTestCase) {
            if (statement instanceof ConstructorStatement) {
                ConstructorStatement ctStatement = (ConstructorStatement) statement;
//                LoggingUtils.getEvoLogger().info("getDeclaringClassName is: " + ctStatement.getDeclaringClassName());
                // also we should check types
                if (ctStatement.getDeclaringClassName().equals(constructorCall.getType().getQualifiedName()) || ctStatement.getSimpleName().equals(constructorCall.getType().getQualifiedName())) {
                    if(ctStatement.getNumParameters() == constructorCall.getArguments().size())
//                        ctStatement.getParameterReferences().get(0).
                        return ctStatement;
                }
            }
        }

        return null;
    }

    private VariableReference findVariable (String name){
        for(Map.Entry<String, VariableReference> variable: variableReferences.entrySet()){
            if(variable.getKey().equals(name)){
                return variable.getValue();
            }
        }
        return null;
    }

    private PrimitiveStatement<?> parsePrimitiveType(TestCase testCase, CtExpression<?> assignment, Class<?> actualClass) {
        Object value = ((CtLiteral<?>) assignment).getValue();
        PrimitiveStatement<?> evoStm = null;
        if (value == null) {
            evoStm = new NullStatement(testCase, actualClass);
        } else if (value instanceof String)
            evoStm = new StringPrimitiveStatement(testCase, (String) value);
        else if (Integer.class.isInstance(value))
            evoStm = new IntPrimitiveStatement(testCase, (Integer) value);

        return evoStm;
    }

    private PrimitiveStatement<?> parsePrimitiveType(TestCase testCase, CtExpression<?> assignment) {
        Object value = ((CtLiteral<?>) assignment).getValue();
        PrimitiveStatement<?> evoStm = null;
        if (value == null) {
            LoggingUtils.getEvoLogger().info("IT HAS NOT BEEN SUPPORTED YET: " + assignment);
        } else if (value instanceof String)
            evoStm = new StringPrimitiveStatement(testCase, (String) value);
        else if (Integer.class.isInstance(value))
            evoStm = new IntPrimitiveStatement(testCase, (Integer) value);

        LoggingUtils.getEvoLogger().info("IT HAS NOT BEEN SUPPORTED YET: " + value);

        return evoStm;
    }

    private PrimitiveStatement<?> parseUnaryOperator(TestCase testCase, CtExpression<?> assignment, CtLocalVariableReference<?> reference) {
//        Object value = ((CtUnaryOperator<?>) assignment).();
        PrimitiveStatement<?> evoStm = null;
//        if (value == null) {
//            evoStm = new NullStatement(testCase, reference.getType().getActualClass());
//        } else if (value instanceof String)
//            evoStm = new StringPrimitiveStatement(testCase, (String) value);
//        else if (Integer.class.isInstance(value))
//            evoStm = new IntPrimitiveStatement(testCase, (Integer) value);
//
        return evoStm;
    }

    private MethodStatement parseMethodStatement(TestCase testCase, CtInvocation<?> invocation) {
        MethodStatement matched = matchMethods(testCase, invocation);

        List<CtExpression<?>> args = invocation.getArguments();
        ArrayList<VariableReference> parameters = new ArrayList<>();

        if (matched == null) {
            return null;
        }

        LoggingUtils.getEvoLogger().info("Matched Method is: " + matched);

//        VariableReference callee = getVariableReference(testCase, matched.getCallee());
//        if(callee == null)
//            return null;

        for (CtExpression<?> expression : args) {
            VariableReference variableReference = parseArgStatement(testCase, expression);
            if(variableReference!=null)
                parameters.add(variableReference);
        }

        for (int i=0; i < parameters.size(); i++){
            matched.replaceParameterReference(parameters.get(i), i);
        }

        List<VariableReference> references = matched.getParameterReferences();


        for (int i=parameters.size(); i < references.size(); i++){
            LoggingUtils.getEvoLogger().info("i " + i + " Parameters size() " + parameters.size() + " Num of Method Parameters: " + references.size()   );

            Statement statement = new NullStatement(testCase, references.get(i).getType());

            matched.replaceParameterReference(addStatement(testCase, statement), i);
        }

        return matched;
    }

    private VariableReference getVariableReference(TestCase testCase, VariableReference var){
        if(var.getStPosition() != -1)
            return null;
        if(var.getStPosition() > testCase.size())
            return null;
        Statement statement = testCase.getStatement(var.getStPosition());
        if(!statement.getReturnType().equals(var.getType()))
            return null;
        return statement.getReturnValue();
    }

    private VariableReference parseArgStatement(TestCase testCase, CtExpression<?> expression){
        Statement stm;
        if (expression instanceof CtLiteral) {
            stm = parsePrimitiveType(testCase, expression);
            if(stm != null)
                return addStatement(testCase, stm);
        } else if (expression instanceof CtConstructorCall) {
            stm = parseConstructorCall(testCase, expression);
            if(stm != null)
                return addStatement(testCase, stm);
        } else if(expression instanceof CtVariableReadImpl){
            CtVariableReadImpl<?> ctVariable = (CtVariableReadImpl<?>) expression;
//            LoggingUtils.getEvoLogger().info("Variable is: " + ctVariable.getVariable());
            //                statement.getV
            return variableReferences.get(findVariable(ctVariable.getVariable().getSimpleName()));
        }
        return null;
    }

    private MethodStatement matchMethods(TestCase testCase, CtInvocation<?> invocation) {
        ArrayList<MethodStatement> potentialStatements = new ArrayList<>();
        CtExpression<?> target = invocation.getTarget();
        CtExecutableReference<?> method = invocation.getExecutable();
        String id = target + "." + method;

        if(testCase.isEmpty()) {
            return null;
        }

        if(target.getType() == null)
            return null;

        for (Statement statement : oldTestCase) {
            if (statement instanceof MethodStatement) {
                MethodStatement ctStatement = (MethodStatement) statement;
                VariableReference callee = ctStatement.getCallee();
                // also we should check types

                // Check Static Methods
                if(callee == null){
                    if(ctStatement.isStatic()){
                        if(ctStatement.getMethodName().equals(method.getSimpleName())) {
                            if (ctStatement.getParameterReferences().size() == invocation.getArguments().size()) {
                                potentialStatements.add(ctStatement);
                            }
                        }
                    }
                    continue;
                }

                if(testCase.size() < callee.getStPosition()) {
                    continue;
                }

                String qualifiedName = target.getType().getQualifiedName();
                if (callee.getType().getTypeName().equals(qualifiedName) || ((Class) callee.getType()).getSimpleName().equals(qualifiedName)) {
                    if(ctStatement.getMethodName().equals(method.getSimpleName())) {
//                        LoggingUtils.getEvoLogger().info("Callee: " + ((Class) callee.getType()).getSimpleName());
//                        LoggingUtils.getEvoLogger().info("Target: " + target.getType().getQualifiedName());
//                        LoggingUtils.getEvoLogger().info("Method name is: " + ctStatement.getMethodName());
//                        LoggingUtils.getEvoLogger().info("invocation is: " + invocation.getExecutable().getSimpleName());
//                        LoggingUtils.getEvoLogger().info("Num of Parameters is: " + ctStatement.getParameterReferences().size());
//                        LoggingUtils.getEvoLogger().info("invocation args: " + invocation.getArguments().size());
                        if (ctStatement.getParameterReferences().size() == invocation.getArguments().size()) {
                            if(testCase.size() < callee.getStPosition())
                                continue;

                            Statement sourceStatement = testCase.getStatement(callee.getStPosition());
                            LoggingUtils.getEvoLogger().info("source statement: " + sourceStatement + "callee: " + callee);
                            if (sourceStatement.getReturnType().equals(callee.getType())) {
                                potentialStatements.add(ctStatement);
                            }
                        }
                    }
                }
            }
        }

        if(potentialStatements.isEmpty())
            return null;

        if(potentialStatements.size() == 1) {
            LoggingUtils.getEvoLogger().info("Potential Statements - 0 - " + potentialStatements.get(0).getPosition());
            return potentialStatements.get(0);
        }

        int number = calleeStatements.get(id);
        if(number == 0) {
            calleeStatements.put(id, 1);
            LoggingUtils.getEvoLogger().info("Potential Statements - 1 - " + potentialStatements.get(0).getPosition());
            return potentialStatements.get(0);
        }
        LoggingUtils.getEvoLogger().info("Potential Statements - 2 - " + potentialStatements.get(number).getPosition());
        calleeStatements.put(id, number + 1);
        return potentialStatements.get(number);
    }

    private VariableReference addStatement(TestCase testCase, Statement statement){
        VariableReference variableReference = testCase.addStatement(statement.clone(testCase));
//        VariableReference variableReference = testCase.addStatement(statement, position);
//        variableReferences.put(position, variableReference);
        position++;
        return variableReference;
    }

    private VariableReference addVariableStatement(TestCase testCase, Statement statement, String name){
        VariableReference variableReference = testCase.addStatement(statement.clone(testCase));
//        VariableReference variableReference = testCase.addStatement(statement, position);
        variableReferences.put(name, variableReference);
        position++;
        return variableReference;
    }
}
