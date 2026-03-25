# AI Error Analyzer - Knowledge Base

## Error: NullInjectorError for HttpClient in Angular 19
**Log Signature:** `NullInjectorError: No provider for HttpClient!`
**Context:** In Angular 19 standalone mode, `HttpClientModule` is deprecated.
**Solution:** Do not import `HttpClientModule` in your component. Instead, provide it at the application root level. Open `app.config.ts` and add `provideHttpClient()` to the `providers` array. Inside your component, use `private http = inject(HttpClient);`.

## Error: PGVector Extension Missing in Spring AI
**Log Signature:** `org.postgresql.util.PSQLException: ERROR: type "vector" does not exist`
**Context:** Spring AI requires the PostgreSQL database to have the `pgvector` extension installed and enabled to store text embeddings.
**Solution:** Ensure you are using the `pgvector/pgvector:pg16` Docker image, not the standard `postgres` image. Connect to your database and run the SQL command: `CREATE EXTENSION IF NOT EXISTS vector;`. Restart the Spring Boot application after the extension is created.

## Error: Nx Monorepo Circular Dependency
**Log Signature:** `NX   ERROR  A circular dependency was found in the workspace`
**Context:** During migration to an Nx workspace, two libraries are importing each other, which breaks the build DAG (Directed Acyclic Graph).
**Solution:** Identify the shared interface or DTO causing the loop. Extract this shared code into a third, independent library (e.g., `libs/shared/types`). Update both original libraries to import from this new shared library instead of each other.

## Error: Resilience4j CircuitBreaker 'OPEN' State
**Log Signature:** `CallNotPermittedException: CircuitBreaker 'openAiApi' is OPEN and does not permit further calls`
**Context:** The LLM API is timing out or returning 500 errors, so the Circuit Breaker has tripped to protect the application.
**Solution:** This is expected behavior during an outage. Check your application properties for `resilience4j.circuitbreaker.instances.openAiApi.waitDurationInOpenState`. Ensure your frontend is configured to read the fallback method response ("AI is currently unavailable, please try again later") and display it gracefully to the user.