# Unit Test Types Report Across Microservices

This report catalogs the unit test styles and test slice types currently used across all backend microservices under `backEnd/Microservices`.

## Scope

- Included: all Java test files in `backEnd/Microservices/*/src/test/**/*.java`
- Method used: annotation/import pattern scan (`@ExtendWith(MockitoExtension.class)`, `@WebMvcTest`, `@SpringBootTest`, `@DataJpaTest`, `@Nested`, `@DisplayName`, `MockedStatic`)

## Test Types Found

1. **Mockito-based unit tests**  
   Based on `@ExtendWith(MockitoExtension.class)` with mocked collaborators.

2. **Web MVC slice tests**  
   Based on `@WebMvcTest(...)`, typically controller-focused tests using MockMvc.

3. **Spring Boot context/integration-style tests**  
   Based on `@SpringBootTest`, loading application context (some are integration-style).

4. **JPA repository slice tests**  
   Based on `@DataJpaTest`, focused persistence/repository behavior.

5. **Structured JUnit 5 tests**  
   Based on `@Nested` and/or `@DisplayName` for organized, descriptive test suites.

6. **Static mocking tests (Mockito)**  
   Based on `MockedStatic`/`mockStatic(...)`.

## Per-Microservice Mapping

| Microservice | Test Types Used |
|---|---|
| `AImodel` | Mockito unit tests, Web MVC slice tests |
| `Chat` | Mockito unit tests, Structured JUnit 5 tests |
| `Contract` | Mockito unit tests, Web MVC slice tests |
| `FreelanciaJob` | Mockito unit tests, Web MVC slice tests, Spring Boot context/integration-style tests, JPA repository slice tests, Structured JUnit 5 tests, Static mocking tests |
| `gamification` | Mockito unit tests, Web MVC slice tests, Spring Boot context/integration-style tests |
| `Meeting` | Mockito unit tests, Web MVC slice tests, Spring Boot context/integration-style tests, JPA repository slice tests, Structured JUnit 5 tests |
| `Notification` | Mockito unit tests, Spring Boot context/integration-style tests |
| `Offer` | Mockito unit tests, Spring Boot context/integration-style tests |
| `planning` | Mockito unit tests, Web MVC slice tests, Spring Boot context/integration-style tests |
| `Portfolio` | Mockito unit tests, Web MVC slice tests |
| `Project` | Mockito unit tests, Web MVC slice tests, Spring Boot context/integration-style tests |
| `review` | Mockito unit tests, Spring Boot context/integration-style tests |
| `Subcontracting` | Mockito unit tests |
| `task` | Mockito unit tests, Web MVC slice tests, Spring Boot context/integration-style tests |
| `ticket-service` | Mockito unit tests |
| `user` | Mockito unit tests, Web MVC slice tests |

## Evidence Examples (Representative Files)

- **Mockito unit tests**:  
  `backEnd/Microservices/task/src/test/java/com/esprit/task/service/TaskServiceTest.java`  
  `backEnd/Microservices/ticket-service/src/test/java/com/esprit/ticket/service/TicketServiceTest.java`

- **Web MVC slice tests**:  
  `backEnd/Microservices/task/src/test/java/com/esprit/task/controller/TaskControllerTest.java`  
  `backEnd/Microservices/Project/src/test/java/tn/esprit/project/Controllers/ProjectControllerTest.java`

- **Spring Boot context/integration-style tests**:  
  `backEnd/Microservices/Meeting/src/test/java/tn/esprit/meeting/MeetingIntegrationTest.java`  
  `backEnd/Microservices/FreelanciaJob/src/test/java/tn/esprit/freelanciajob/FreelanciaJobIntegrationTest.java`

- **JPA repository slice tests**:  
  `backEnd/Microservices/Meeting/src/test/java/tn/esprit/meeting/repository/MeetingRepositoryTest.java`  
  `backEnd/Microservices/FreelanciaJob/src/test/java/tn/esprit/freelanciajob/repository/JobRepositoryTest.java`

- **Structured JUnit 5 tests**:  
  `backEnd/Microservices/Chat/src/test/java/tn/esprit/chat/service/ChatServiceTest.java`  
  `backEnd/Microservices/FreelanciaJob/src/test/java/tn/esprit/freelanciajob/service/JobServiceTest.java`

- **Static mocking tests**:  
  `backEnd/Microservices/FreelanciaJob/src/test/java/tn/esprit/freelanciajob/FreelanciaJobCoverageHelpersTest.java`

## Notes

- `@SpringBootTest` files often include application context smoke tests and integration-style endpoint flows; both are grouped under the same annotation-based type.
- This report is annotation-driven and reflects the patterns currently present in the codebase.
