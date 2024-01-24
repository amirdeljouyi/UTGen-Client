package org.evosuite.llm;

import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.statements.*;
import org.evosuite.testcase.statements.numeric.*;
import org.evosuite.testcase.variable.VariableReference;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.ParameterizedTypeImpl;
import spoon.Launcher;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtVariableReadImpl;

import java.util.*;

public class Parser {

    HashMap<String, VariableReference> variableReferences = new HashMap<>();
    HashMap<String, Integer> calleeStatements = new HashMap<>();
    HashMap<Integer, Integer> statementsIndex = new HashMap<>();
    int position = 0;
    TestCase oldTestCase;


    public Parser(TestCase oldTestCase) {
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

                    if (assignment == null)
                        continue;

                    LoggingUtils.getEvoLogger().info(assignment.getClass().toString());
                    Statement stm = null;

                    if (assignment instanceof CtLiteral) {
                        if (((CtLiteral<?>) assignment).getValue() == null)
                            stm = parseNullStatement(testCase, reference.getType());
                        else
                            stm = parsePrimitiveType(testCase, assignment);
                    } else if (assignment instanceof CtUnaryOperator) {
                        stm = parseUnaryOperator(testCase, assignment, reference);
                    } else if (assignment instanceof CtConstructorCall) {
                        stm = parseConstructorCall(testCase, assignment);
                    } else if (assignment instanceof CtInvocation) {
                        stm = parseMethodStatement(testCase, (CtInvocation<?>) assignment);
                    } else if (assignment instanceof CtNewArray) {
                        stm = parseArrayStatement(testCase, (CtNewArray<?>) assignment);
                    } else if (assignment instanceof CtFieldRead) {
                        stm = parseFieldStatement(testCase, (CtFieldRead<?>) assignment);
                    } else {
                        LoggingUtils.getEvoLogger().info("IT HAS NOT BEEN SUPPORTED YET: " + assignment);
                    }

                    if (stm != null) {
                        addVariableStatement(testCase, stm, stmType.getSimpleName());
                    }
                } else if (statement instanceof CtInvocation) {
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
        if (unusedVars > 0)
            for (int i = position; i < testCase.size(); i++)
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
        if (!testCase.isEmpty())
            LoggingUtils.getEvoLogger().info("TestCase is: " + testCase.toCode());

        return testCase;
    }


    public ConstructorStatement parseConstructorCall(TestCase testCase, CtExpression<?> assignment) {
        CtConstructorCall<?> constructorCall = ((CtConstructorCall<?>) assignment);

        ConstructorStatement matched = matches(constructorCall);
        if (matched == null)
            return null;

        List<CtExpression<?>> args = constructorCall.getArguments();
        ArrayList<VariableReference> parameters = new ArrayList<>();
        for (CtExpression<?> expression : args) {
            VariableReference variableReference = parseArgStatement(testCase, expression);
            if (variableReference != null)
                parameters.add(variableReference);
        }

        for (int i = 0; i < Math.min(parameters.size(), matched.getNumParameters()); i++) {
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
                ConstructorStatement evStatement = (ConstructorStatement) statement;
//                LoggingUtils.getEvoLogger().info("getDeclaringClassName is: " + evStatement.getDeclaringClassName());
                // also we should check types
                if (evStatement.getDeclaringClassName().equals(constructorCall.getType().getQualifiedName()) || evStatement.getSimpleName().equals(constructorCall.getType().getQualifiedName())) {
                    if (evStatement.getNumParameters() == constructorCall.getArguments().size())
//                        evStatement.getParameterReferences().get(0).
                        return evStatement;
                }
            }
        }

        return null;
    }

    private VariableReference findVariable(String name) {
        for (Map.Entry<String, VariableReference> variable : variableReferences.entrySet()) {
            if (variable.getKey().equals(name)) {
                return variable.getValue();
            }
        }
        return null;
    }

    private PrimitiveStatement<?> parsePrimitiveType(TestCase testCase, CtExpression<?> assignment) {
        Object value = ((CtLiteral<?>) assignment).getValue();
        if (value instanceof String)
            return new StringPrimitiveStatement(testCase, (String) value);
        else if (Integer.class.isInstance(value))
            return new IntPrimitiveStatement(testCase, (Integer) value);
        else if (Double.class.isInstance(value))
            return new DoublePrimitiveStatement(testCase, (Double) value);
        else if (Short.class.isInstance(value))
            return new ShortPrimitiveStatement(testCase, (Short) value);
        else if (Float.class.isInstance(value))
            return new FloatPrimitiveStatement(testCase, (Float) value);
        else if (Long.class.isInstance(value))
            return new LongPrimitiveStatement(testCase, (Long) value);
        else if (value instanceof Boolean)
            return new BooleanPrimitiveStatement(testCase, (Boolean) value);
        else if (value instanceof Character)
            return new CharPrimitiveStatement(testCase, (Character) value);
        else if (value instanceof char[])
            return new CharPrimitiveStatement(testCase, ((char[]) value)[0]);

        LoggingUtils.getEvoLogger().info("IT HAS NOT BEEN SUPPORTED YET: " + assignment);

        return null;
    }

    private PrimitiveStatement<?> parseNullStatement(TestCase testCase, CtTypeReference<?> type) {
        for (Statement statement : oldTestCase) {
            if (statement instanceof NullStatement) {
                NullStatement evStatement = (NullStatement) statement;
                String simpleClassName = ((Class) evStatement.getReturnType()).getSimpleName();
                String qualifiedName = type.getQualifiedName();

                if (evStatement.getReturnType().getTypeName().equals(type.getQualifiedName()) || simpleClassName.equals(qualifiedName)) {
                    return evStatement;
                }
            }
        }
        return null;
    }

    private PrimitiveStatement<?> parseUnaryOperator(TestCase testCase, CtExpression<?> assignment, CtLocalVariableReference<?> reference) {
        LoggingUtils.getEvoLogger().info("IT HAS NOT BEEN SUPPORTED YET: " + assignment);

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

    private EntityWithParametersStatement parseMethodStatement(TestCase testCase, CtInvocation<?> invocation) {
        EntityWithParametersStatement matched = matchMethods(testCase, invocation);

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
            if (variableReference != null)
                parameters.add(variableReference);
        }

        for (int i = 0; i < parameters.size(); i++) {
            matched.replaceParameterReference(parameters.get(i), i);
        }

        List<VariableReference> references = matched.getParameterReferences();


        for (int i = parameters.size(); i < references.size(); i++) {
            LoggingUtils.getEvoLogger().info("i " + i + " Parameters size() " + parameters.size() + " Num of Method Parameters: " + references.size());

            Statement statement = new NullStatement(testCase, references.get(i).getType());

            matched.replaceParameterReference(addStatement(testCase, statement), i);
        }

        return matched;
    }

    private VariableReference getVariableReference(TestCase testCase, VariableReference var) {
        if (var.getStPosition() != -1)
            return null;
        if (var.getStPosition() > testCase.size())
            return null;
        Statement statement = testCase.getStatement(var.getStPosition());
        if (!statement.getReturnType().equals(var.getType()))
            return null;
        return statement.getReturnValue();
    }

    private VariableReference parseArgStatement(TestCase testCase, CtExpression<?> expression) {
        Statement stm;
        if (expression instanceof CtLiteral) {
            stm = parsePrimitiveType(testCase, expression);
            if (stm != null)
                return addStatement(testCase, stm);
        } else if (expression instanceof CtConstructorCall) {
            stm = parseConstructorCall(testCase, expression);
            if (stm != null)
                return addStatement(testCase, stm);
        } else if (expression instanceof CtVariableReadImpl) {
            CtVariableReadImpl<?> ctVariable = (CtVariableReadImpl<?>) expression;
//            LoggingUtils.getEvoLogger().info("Variable is: " + ctVariable.getVariable());
//            LoggingUtils.getEvoLogger().info("variable is: " + variable);
            VariableReference variable = findVariable(ctVariable.getVariable().getSimpleName());
            return variable;
        }
        return null;
    }

    private EntityWithParametersStatement matchMethods(TestCase testCase, CtInvocation<?> invocation) {
        CtExpression<?> target = invocation.getTarget();
        CtExecutableReference<?> method = invocation.getExecutable();
        LoggingUtils.getEvoLogger().info("invocation is: " + invocation + " target is: " + target + " method is: " + method);
        // Mock Method
        if (method.getSimpleName().equals("mock")) {
            return parseMockMethod(invocation);
        }

        // TODO: Static Method
        MethodStatement staticMethod = parseStaticMethod(testCase, invocation);
        if (staticMethod != null)
            return staticMethod;

        // Instance Method
        return parseInstanceMethod(testCase, invocation);
    }

    private FunctionalMockStatement parseMockMethod(CtInvocation<?> invocation) {
        for (Statement statement : oldTestCase) {
            if (statement instanceof FunctionalMockStatement) {
                FunctionalMockStatement evStatement = (FunctionalMockStatement) statement;
                LoggingUtils.getEvoLogger().info("target name is: " + evStatement.getTargetClass().getSimpleName() + " argument is: " + invocation.getArguments().get(0).toString());
                String className = invocation.getArguments().get(0).toString().replace(".class", "");
                if (evStatement.getTargetClass().getSimpleName().equals(className)) {
                    return evStatement;
                }
            }
        }
        return null;
    }

    private MethodStatement parseStaticMethod(TestCase testCase, CtInvocation<?> invocation) {
        CtExecutableReference<?> method = invocation.getExecutable();
        CtExpression<?> target = invocation.getTarget();

        for (Statement statement : oldTestCase) {
            if (statement instanceof MethodStatement) {
                MethodStatement evStatement = (MethodStatement) statement;
                VariableReference callee = evStatement.getCallee();
                // also we should check types

//                LoggingUtils.getEvoLogger().info("isStatic: " + evStatement.isStatic() + " callee: " + callee);
                if (!evStatement.isStatic()) {
                    continue;
                }

                // Check Static Methods
                if (callee == null) {
                    if (evStatement.getMethodName().equals(method.getSimpleName())) {
                        if (evStatement.getParameterReferences().size() == invocation.getArguments().size()) {
                            return evStatement;
                        }
                    }
                }
            }
        }

        return null;
    }

    private MethodStatement parseInstanceMethod(TestCase testCase, CtInvocation<?> invocation) {
        ArrayList<MethodStatement> potentialStatements = new ArrayList<>();
        CtExpression<?> target = invocation.getTarget();
        CtExecutableReference<?> method = invocation.getExecutable();
        String id = target + "." + method;

        if (testCase.isEmpty()) {
            return null;
        }

        if (target.getType() == null) {
            return null;
        }

        for (Statement statement : oldTestCase) {
            if (statement instanceof MethodStatement) {
                MethodStatement evStatement = (MethodStatement) statement;
                VariableReference callee = evStatement.getCallee();
                // also we should check types

                if (callee == null) {
                    continue;
                }


                if (typeChecker(callee.getType(), target.getType())) {
                    if (evStatement.getMethodName().equals(method.getSimpleName())) {
//                        LoggingUtils.getEvoLogger().info("Callee: " + ((Class) callee.getType()).getSimpleName());
//                        LoggingUtils.getEvoLogger().info("Target: " + target.getType().getQualifiedName());
//                        LoggingUtils.getEvoLogger().info("Method name is: " + evStatement.getMethodName());
//                        LoggingUtils.getEvoLogger().info("invocation is: " + invocation.getExecutable().getSimpleName());
//                        LoggingUtils.getEvoLogger().info("Num of Parameters is: " + evStatement.getParameterReferences().size());
//                        LoggingUtils.getEvoLogger().info("invocation args: " + invocation.getArguments().size());
                        if (evStatement.getParameterReferences().size() == invocation.getArguments().size()) {
                            if (!statementsIndex.containsKey(callee.getStPosition()))
                                continue;

                            int index = statementsIndex.get(callee.getStPosition());
                            if (testCase.size() < index)
                                continue;

                            Statement sourceStatement = testCase.getStatement(index);
                            LoggingUtils.getEvoLogger().info("source statement: " + sourceStatement + " callee: " + callee + "index is: " + index);
                            if (sourceStatement.getReturnType().equals(callee.getType())) {
                                potentialStatements.add(evStatement);
                            }
                        }
                    }
                }
            }
        }

        if (potentialStatements.isEmpty())
            return null;

        if (potentialStatements.size() == 1) {
            return potentialStatements.get(0);
        }

        if (!calleeStatements.containsKey(id)) {
            calleeStatements.put(id, 1);
            return potentialStatements.get(0);
        }

        int number = calleeStatements.get(id);
        calleeStatements.put(id, number + 1);
        return potentialStatements.get(number);
    }

    private FieldStatement parseFieldStatement(TestCase testCase, CtFieldRead<?> field) {
        if (field.getTarget() == null)
            return null;

        String fieldType = field.getTarget().toString();
        LoggingUtils.getEvoLogger().info("Field is: " + field + " is " + field.getTarget());

        for (Statement statement : oldTestCase) {
            if (statement instanceof FieldStatement) {
                FieldStatement evStatement = (FieldStatement) statement;
                String ctTypeName = evStatement.getField().getField().getType().getSimpleName();
                LoggingUtils.getEvoLogger().info("CtField is: " + evStatement.getField().getField() + " " + evStatement.getField().getField().getType().getSimpleName() + " " + evStatement.getField().getName());
                if (ctTypeName.equals(fieldType)) {
                    return evStatement;
                }
            }
        }

        return null;
    }

    private ArrayStatement parseArrayStatement(TestCase testCase, CtNewArray<?> newArray) {
        // TODO: Now Only 1 dimension array is supported
        ArrayStatement evoStm = null;
        for (Statement statement : oldTestCase) {
            if (statement instanceof ArrayStatement) {
                ArrayStatement ctStatement = (ArrayStatement) statement;
                List<Integer> lengths = ctStatement.getLengths();

                LoggingUtils.getEvoLogger().info("Dimension: " + newArray.getDimensionExpressions().toString() + " Ct is: " + ctStatement.getCode() + " isAssignment: " + ctStatement.isAssignmentStatement()  + " Array: " + newArray);
                LoggingUtils.getEvoLogger().info("lengths: " +  lengths);
//                int size = ctStatement.size();
                // also we should check types


            }
        }

        return evoStm;
    }

    private VariableReference addStatement(TestCase testCase, Statement statement) {
        VariableReference variableReference = testCase.addStatement(statement.clone(testCase));
//        VariableReference variableReference = testCase.addStatement(statement, position);
//        variableReferences.put(position, variableReference);
        position++;
        return variableReference;
    }

    private VariableReference addVariableStatement(TestCase testCase, Statement statement, String name) {
        if(!statement.getTestCase().equals(testCase))
            statementsIndex.put(statement.getPosition(), position);

        VariableReference variableReference = testCase.addStatement(statement.clone(testCase));
//        VariableReference variableReference = testCase.addStatement(statement, position);
        variableReferences.put(name, variableReference);
        position++;
        return variableReference;
    }
}
