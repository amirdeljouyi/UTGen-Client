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

Customize how LLM is utilized during the test generation process with the following adjustable properties:
- `LLM_TEST_DATA`  
Default: true  
Enables LLMs to enhance test data generation.

- `LLM_POST_PROCESSING_REPROMPT_BUDGET`  
Default: 2  
Sets the number of additional prompts used for improving test cases during post-processing.  

- ` LLM_POST_PROCESSING`  
Default: true  
Uses LLMs to refine the readability and clarity of test cases by improving identifiers and comments.

- `LLM_GRAPHQL_ENTRYPOINT`  
Default: 0.0.0.0:8000/graphql  
Specifies the entrypoint URL for connecting to the utgen-llm-server.

- `TEST_NAMING_STRATEGY`  
  Default: numbered  
  For LLM-based improvements to test names, set this to LLM_BASED.

- `Test_Format`  
Default: JUNIT5LLM  
To leverage LLMs at any stage of the process, set this to JUNIT5LLM.

- `Reformat`  
Default: true  
Automatically runs a prettifier to format the generated test cases.


These properties allow you to fine-tune LLM integration to meet the specific needs of your project.