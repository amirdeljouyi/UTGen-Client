# UTGen-Client
## llm-relevant package
The module we looked at is the `client`, and the package that integrates LLM into the test generation is located in `org.evosuite.llm`.

Phases of LLM Integration

EvoSuite-LLM utilizes LLM in three major phases:

1. **Test Data Improvement**:
    - Classes such as `Parser` and `Refinement` contribute to this phase.
    - Utilizes the `improveTestData` method.
2. **Enhancement of Identifier and Adding Comments**:
    - Performed by the `JUnit5LLMTestAdapter`, which employs the `improveUnderstandability` method.
    - Focuses on adding comments and improving identifiers.
3. **Test Naming**:
    - Managed by the `LLMBasedTestNameStrategy` using the `suggestTestName` method.

The first phase is considered ***test data***, while the second and third phases are considered ***post-processing***.

### Configuration Properties

To tailor the use of LLM within the test generation process, the following properties can be adjusted or disabled:

- `LLM_TEST_DATA`
- `LLM_POST_PROCESSING_REPROMPT_BUDGET`
- `LLM_POST_PROCESSING`

You can optimize LLM usage according to specific project needs.