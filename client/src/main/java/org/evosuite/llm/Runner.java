package org.evosuite.llm;

public class Runner {
    public static void main(String[] args) {
        GraphQLClient graphQLClient = new GraphQLClient();
        graphQLClient.promptQuery("@Test\n" +
                "  public void testGetFirstNameReturningNonEmptyString()  throws Throwable  {\n" +
                "      Person person0 = new Person();\n" +
                "      person0.setFirstName(\"Lw*`onX`MIV%\");\n" +
                "      String string0 = person0.getFirstName();\n" +
                "      assertEquals(\"Lw*`onX`MIV%\", string0);\n" +
                "  }");
    }
}
