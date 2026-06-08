# AGENTS.md

## Stack

- Spring Boot **4.0.6**, Java **25**, single Maven module (`com.quill:quill`).
- WebMVC, Spring Data JPA (Hibernate), Flyway + `flyway-database-postgresql`, PostgreSQL 18, Bean Validation.
- Spring Security with **JWT** auth (JJWT 0.13) — stateless, CSRF disabled.
- Lombok + `spring-boot-configuration-processor` are wired only as `annotationProcessorPaths`
  in `maven-compiler-plugin` — do **not** move them to plain `<dependency>` entries.
- Runtime dep `spring-boot-docker-compose` auto-starts services from `compose.yaml` in dev/test.

## Build / run

- Use the wrapper: `./mvnw` (system `mvn` is not assumed). Requires JDK 25.
- Common goals: `./mvnw spring-boot:run`, `./mvnw test`, `./mvnw package`, `./mvnw spotless:apply`.
- Single test: `./mvnw test -Dtest=PostServiceTest` or method `-Dtest=PostServiceTest#createsAndReturns`.
- Final jar is `target/quill.jar` (not `quill-0.0.1-SNAPSHOT.jar`) — `pom.xml` sets
  `<finalName>${project.artifactId}</finalName>`.
- `HELP.md` is gitignored Spring Initializr boilerplate; ignore it.

## Database

- `src/main/resources/application.yaml` has **no** datasource — connection is auto-wired by
  `spring-boot-docker-compose` from `compose.yaml` (user `myuser`, pw `secret`, db `mydatabase`, port 5432).
- `spring.jpa.hibernate.ddl-auto=validate` — schema MUST come from Flyway. Any new entity field requires a new
  `Vn__*.sql` in `src/main/resources/db/migration/` (current baseline: `V1__init_schema.sql` for `users`/`posts`/
  `comments`; `V2__add_role_to_users.sql`).
- `spring.jpa.open-in-view=false`; lazy associations only resolve inside a transactional boundary.
- `hibernate.jdbc.time_zone: UTC` is set in `application.yaml`.
- Tests do **not** use `compose.yaml`; they spin up Postgres via Testcontainers (`postgres:18-alpine`) with
  `@ServiceConnection` in `src/test/java/com/quill/TestcontainersConfiguration.java`. Keep that image in sync with
  `compose.yaml`. Tests require a running Docker daemon.
- Flyway runs in `@DataJpaTest` (via `spring-boot-starter-flyway-test`), so repository tests see the real schema.

## Layout

- Entrypoint: `src/main/java/com/quill/Application.java`. `main` is package-private `static void main(String[] args)`
  (Spring Boot 4 / Java 25 allow this — do **not** "fix" to `public`).
- Package layout under `com.quill`: `model/` (JPA entities), `repository/`, `service/`, `controller/`, `mapper/`,
  `dto/`, `exception/`, `config/`, `security/`.
- `JpaConfig` carries `@EnableJpaAuditing`; `@DataJpaTest` slices must
  `@Import({TestcontainersConfiguration.class, JpaConfig.class})` or `@CreatedDate`/`@LastModifiedDate` stay null.
- `spotless-maven-plugin` (3.6.0, Palantir Java Format) is configured but **not** bound to lifecycle — run manually:
  `./mvnw spotless:apply`. IntelliJ also uses Palantir Java Format (`.idea/palantir-java-format.xml`).
- No `.github/`, no CI.

## Security

- `SecurityConfig` (`config/SecurityConfig.java`) registers the filter chain:
    - `/api/auth/**` and `GET /api/posts/**` — permit all
    - `DELETE /api/posts/**` — requires `ROLE_ADMIN`
    - Everything else — authenticated
- JWT: `JwtService` generates/validates HMAC-SHA tokens. The signing key is derived from
  `quill.jwt.secret` (base64, configures in `application.yaml`). `JwtAuthenticationFilter` skips `/api/auth/**`
  via `PathPatternRequestMatcher` and sends 401 on expired/invalid tokens.
- `Role` enum in `model/Role.java` has values `USER` and `ADMIN`. Default is `USER`.
- Password hashing: BCrypt via `PasswordEncoder` bean.
- `@EnableMethodSecurity` is on, so method-level `@PreAuthorize` etc. would work.

## Conventions

- DTOs are Java `record`s with Jakarta validation annotations (see `dto/PostRequest.java`).
- Entities use Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` plus
  `@EntityListeners(AuditingEntityListener.class)` and `@CreatedDate`/`@LastModifiedDate Instant` fields. Mirror this
  for new entities and remember the matching `created_at`/`updated_at` columns in the migration.
- Domain exceptions extend the sealed `ApplicationException` (`exception/ApplicationException.java`). New exceptions
  must be `final`, extend `ApplicationException`, carry an `HttpStatus`, and be added to the `permits` clause.
- Error responses go through `GlobalExceptionHandler` as RFC 7807 `ProblemDetail` (enabled by
  `spring.mvc.problemdetails.enabled=true`). Don't bypass it with custom JSON error bodies.
- Services: `@Service @RequiredArgsConstructor @Transactional(readOnly = true)` at class level, `@Transactional` on
  write methods. `updatePost` mutates the loaded entity and relies on dirty checking — no explicit `save()` call;
  preserve this pattern. Ownership checks use an `isAdmin` boolean passed from the controller (extracted from
  `Authentication.getAuthorities()`).
- Mapping is handwritten (`@Component PostMapper`, `@Component CommentMapper`). No MapStruct.
- New code lives under `com.quill.*`; do not introduce new top-level packages without a reason.

## Tests

- JUnit 5 + AssertJ + Mockito.
- **Service tests**: `@ExtendWith(MockitoExtension.class)` with `@Nested` + `@DisplayName` BDD blocks.
- **Repository tests**: `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` +
  `@Import({TestcontainersConfiguration.class, JpaConfig.class})`.
- **Controller tests**: `@WebMvcTest(SomeController.class)` + `@Import(TestSecurityConfig.class)`.
  Uses AssertJ `MockMvcTester` (injected with `@Autowired`) and `@MockitoBean` (the Boot 4.x replacement for
  `@MockBean`).
  `TestSecurityConfig` mirrors the real security rules but uses HTTP Basic instead of JWT.
