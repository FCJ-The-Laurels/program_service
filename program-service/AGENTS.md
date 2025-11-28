# Repository Guidelines

## Project Structure & Module Organization
- Source lives in `src/main/java/com/smokefree/program` with layers for config/auth/security/util, domain models/repos/services, and web controllers/dto/error/mapper.
- Resources under `src/main/resources`: `application.properties` for profiles/DB; Flyway SQL in `db/migration` (versioned `V##_...` and repeatable `R__...` files); static assets in `static/tester.html`.
- Tests in `src/test/java/com/smokefree/program`; docs like `SYSTEM_ARCHITECTURE.md` and `API_DOCUMENTATION_COMPLETE.md` sit in the repo root for architecture/API references.

## Build, Test, and Development Commands
- `./mvnw clean package` builds the jar, runs unit/integration tests, and validates Flyway migrations.
- `./mvnw test` runs the test suite only; use for quick feedback loops.
- `./mvnw spring-boot:run` starts the dev API on port 8080 (PostgreSQL must match `application.properties`; override with env vars or `--spring.profiles.active`).
- `./mvnw flyway:info` checks applied/pending migrations against the current database.

## Coding Style & Naming Conventions
- Java 25, Spring Boot 3.5; Lombok is preferred for boilerplate getters/setters/constructors.
- Use 4-space indentation and constructor injection for components (`@Service`, `@RestController`). Final fields where possible.
- Class names PascalCase; methods/fields camelCase; DTOs suffixed `Req/Res`, repositories `...Repository`/`...Repo`, services `...Service/Impl`.
- Keep controllers in `web` using dedicated request/response DTOs and mapper helpers for domain conversions.
- Migrations follow `V##__description.sql` (ordered) or `R__name.sql` (repeatable); avoid editing applied migrations.

## Testing Guidelines
- JUnit 5 with `@SpringBootTest` is in place; add focused tests near new code.
- Name test files `*Test.java` and mirror source packages for auto-discovery.
- Prefer sliced tests (`@WebMvcTest`, `@DataJpaTest`) when possible; stub external calls and ensure DB-backed tests set a profile or containerized Postgres.

## Commit & Pull Request Guidelines
- History uses short, sometimes bilingual summaries; keep messages concise and imperative (e.g., `Add quiz template cloning`) and include the scope when useful.
- PRs should note intent, impact, and verification steps; link tickets; add API/DB change notes and screenshots or sample payloads for reviewers.
- Run tests/build before opening; call out new migrations or config toggles so environments stay aligned.

## Security & Configuration Tips
- Dev profile points to Postgres `ojt_prj` with user `program_app_rw`; never commit secrets; override via env vars or `application-*.properties` files.
- Keep security filters (`auth`, `security` packages) intact; do not relax auth for production profiles.
- Flyway `clean` is disabled; add new migrations rather than modifying existing ones.
