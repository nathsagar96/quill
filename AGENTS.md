# AGENTS.md

## Stack

- Spring Boot **4.0.6**, Java **25**, single Maven module (`com.quill:quill`).
- WebMVC, Spring Data JPA (Hibernate), Flyway + `flyway-database-postgresql`, PostgreSQL 18, Bean Validation.
- Lombok + `spring-boot-configuration-processor` are wired only as `annotationProcessorPaths` in `maven-compiler-plugin`
  — do **not** move them to plain `<dependency>` entries.
- Runtime dep `spring-boot-docker-compose` auto-starts services from `compose.yaml` in dev/test.

## Build / run

- Use the wrapper: `./mvnw` (system `mvn` is not assumed). Requires JDK 25.
- Common goals: `./mvnw spring-boot:run`, `./mvnw test`, `./mvnw package`.
- Single test: `./mvnw test -Dtest=PostServiceTest` or method `-Dtest=PostServiceTest#createsAndReturns`.
- Final jar is `target/quill.jar` (not `quill-0.0.1-SNAPSHOT.jar`) — `pom.xml` sets
  `<finalName>${project.artifactId}</finalName>`.
- `HELP.md` is gitignored Spring Initializr boilerplate; ignore it.

## Database

- `src/main/resources/application.yaml` has **no** datasource — connection is auto-wired by `spring-boot-docker-compose`
  from `compose.yaml` (user `myuser`, pw `secret`, db `mydatabase`, port 5432).
- `spring.jpa.hibernate.ddl-auto=validate` — schema MUST come from Flyway. Any new entity field requires a new
  `Vn__*.sql` in `src/main/resources/db/migration/` (current baseline: `V1__init_schema.sql` for `users`/`posts`/
  `comments`).
- `spring.jpa.open-in-view=false` is on; lazy associations only resolve inside a transactional boundary.
- Tests do **not** use `compose.yaml`; they spin up Postgres via Testcontainers (`postgres:18-alpine`) with
  `@ServiceConnection` in `src/test/java/com/quill/TestcontainersConfiguration.java`. Keep that image in sync with
  `compose.yaml`. Tests require a running Docker daemon.
- Flyway runs in `@DataJpaTest` (via `spring-boot-starter-flyway-test`), so repository tests see the real schema.

## Layout

- Entrypoint: `src/main/java/com/quill/Application.java`. `main` is package-private `static void main(String[] args)`
  (Spring Boot 4 / Java 25 allow this — do **not** "fix" to `public`).
- Package layout under `com.quill`: `model/` (JPA entities), `repository/`, `service/`, `mapper/`, `dto/`,
  `exception/`, `config/`. No `controller/` package yet — services and DTOs exist, REST layer is not wired.
- `JpaConfig` carries `@EnableJpaAuditing`; `@DataJpaTest` slices must
  `@Import({TestcontainersConfiguration.class, JpaConfig.class})` or `@CreatedDate`/`@LastModifiedDate` stay null.
- No `.github/`, no CI, no Maven format/lint plugin. IntelliJ uses Palantir Java Format
  (`.idea/palantir-java-format.xml`); match its style for new code but expect no build-time enforcement.

## Conventions

- DTOs are Java `record`s with Jakarta validation annotations (see `dto/PostRequest.java`).
- Entities use Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` plus
  `@EntityListeners(AuditingEntityListener.class)` and `@CreatedDate`/`@LastModifiedDate Instant` fields. Mirror this
  for new entities and remember the matching `created_at`/`updated_at` columns in the migration.
- Domain exceptions extend the sealed `ApplicationException` (`exception/ApplicationException.java`). New not-found /
  domain errors must be `final`, carry an `HttpStatus`, and be added to its `permits` clause.
- Error responses go through `GlobalExceptionHandler` as RFC 7807 `ProblemDetail` (enabled by
  `spring.mvc.problemdetails.enabled=true`). Don't bypass it with custom JSON error bodies.
- Services: `@Service @RequiredArgsConstructor @Transactional(readOnly = true)` at class level, `@Transactional` on
  write methods. `updatePost` mutates the loaded entity and relies on dirty checking — no explicit `save()` call;
  preserve this pattern.
- Mapping is handwritten (`@Component PostMapper`). No MapStruct — don't add it without a reason.
- Tests: JUnit 5 + AssertJ + Mockito. Service tests use `@ExtendWith(MockitoExtension.class)` with `@Nested` +
  `@DisplayName` BDD blocks. Repo tests use `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` against
  Testcontainers.
- New code lives under `com.quill.*`; do not introduce new top-level packages without a reason.

## Maven plugin gotchas

- Do **not** add `lombok` or `spring-boot-configuration-processor` as regular `<dependency>` entries — they are already
  declared as `annotationProcessorPaths`.
- Do **not** delete the `spring-boot-maven-plugin` `<excludes>` block for Lombok — it keeps Lombok out of the fat jar.
