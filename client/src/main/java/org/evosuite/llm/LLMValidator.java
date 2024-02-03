package org.evosuite.llm;

import org.evosuite.utils.LoggingUtils;
import spoon.Launcher;
import spoon.SpoonException;
import spoon.reflect.declaration.CtClass;

public class LLMValidator {
    private final LLMHandler llm = new LLMHandler();

    private String improveUnderstandability(String code, int i) {
        String improvedCode = llm.improveUnderstandability(code);

        if (i == 5) {
            return code;
        }

        if (improvedCode == null) {
            return improveUnderstandability(code, i + 1);
        }

        String dummyClass = "class DummyClass { public static void main (){" + improvedCode + " }}";
        try {
            CtClass<?> ctClass = Launcher.parseClass(dummyClass);
            LoggingUtils.getEvoLogger().info("** It was a parseable test case! ");
            return improvedCode;
        } catch (SpoonException e) {
            return improveUnderstandability(code, i + 1);
        }
    }

    public String improveUnderstandability(String code) {
        return improveUnderstandability(code, 0);
    }


    private CtClass<?> improveTestData(String code, int i) {
        String improvedCode = llm.improveTestData(code);

        if (i == 5) {
            return null;
        }

        if (improvedCode == null) {
            return improveTestData(code, i + 1);
        }

        String dummyClass = "class DummyClass { public static void main (){" + improvedCode + " }}";
        try {
            CtClass<?> ctClass = Launcher.parseClass(dummyClass);
            LoggingUtils.getEvoLogger().info("** It was a parseable test case! ");
            return ctClass;
        } catch (SpoonException e) {
            return improveTestData(code, i + 1);
        }
    }

    public CtClass<?> improveTestData(String code) {
        return improveTestData(code, 0);
    }
}
