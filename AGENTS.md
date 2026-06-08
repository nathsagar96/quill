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

## Database

- `src/main/resources/application.yaml` has **no** datasource — connection is auto-wired by
  `spring-boot-docker-compose` from `compose.yaml` (user `myuser`, pw `secret`, db `mydatabase`, port 5432).
- `spring.jpa.hibernate.ddl-auto=validate` — schema MUST come from Flyway. New entity fields require
  `V{n}__*.sql` in `src/main/resources/db/migration/` (current baseline: V1–V7 covering `users`, `posts`,
  `comments`, `categories`, `tags`, join tables, roles, excerpts, bios, slugs).
- `spring.jpa.open-in-view=false`; lazy associations only resolve inside a transactional boundary.
- `hibernate.jdbc.time_zone: UTC`.
- Tests use Testcontainers (`postgres:18-alpine`) via `@ServiceConnection` in
  `TestcontainersConfiguration.java` — not `compose.yaml`. Keep image in sync. Requires Docker.
- Flyway runs in `@DataJpaTest` (`spring-boot-starter-flyway-test`), so repos see real schema.

## Layout

- Entrypoint: `Application.java`. `main` is **package-private** (`static void main`) — do not make `public`.
- Packages under `com.quill`: `model/`, `repository/`, `service/`, `controller/`, `mapper/`,
  `dto/request/`, `dto/response/`, `exception/`, `config/`, `security/`.
- `JpaConfig` carries `@EnableJpaAuditing`; `@DataJpaTest` slices must
  `@Import({TestcontainersConfiguration.class, JpaConfig.class})` or `@CreatedDate`/`@LastModifiedDate` stay null.
- `spotless-maven-plugin` (Palantir Java Format) is **not** bound to lifecycle — run manually:
  `./mvnw spotless:apply`.
- No CI, no `.github/`.

## Security

- `SecurityConfig` registers the filter chain:
    - `GET /api/posts/**`, `/api/categories/**`, `/api/tags/**` — permit all
    - `/api/auth/**` — permit all
    - `DELETE /api/posts/**`, `/api/categories/**`, `/api/tags/**` — `ROLE_ADMIN`
    - Everything else — authenticated
- JWT: HMAC-SHA via `JwtService`. Signing key from `quill.jwt.secret` (base64 in `application.yaml`).
  `JwtAuthenticationFilter` skips `/api/auth/**` via `PathPatternRequestMatcher`, sends 401 on expired/invalid.
- `PasswordEncoder` is BCrypt. `@EnableMethodSecurity` is on.
- `Role` enum: `USER` (default), `ADMIN`.

## Conventions

- DTOs are Java `record`s with Jakarta validation (`dto/request/`); responses in `dto/response/`.
- Entities use Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` +
  `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate`/`@LastModifiedDate Instant`. New entities
  need matching `created_at`/`updated_at` columns in a Flyway migration.
- Sealed exception hierarchy: `ApplicationException` permits `CategoryNotFoundException`,
  `DuplicateEmailException`, `DuplicateUsernameException`, `ForbiddenOperationException`,
  `PostNotFoundException`, `TagNotFoundException`, `UserNotFoundException`. New ones must be `final`,
  extend `ApplicationException`, carry an `HttpStatus`, and join the `permits` clause.
- Error responses are RFC 7807 `ProblemDetail` (`spring.mvc.problemdetails.enabled=true`). Don't write
  custom JSON bodies.
- Services: `@Service @RequiredArgsConstructor @Transactional(readOnly = true)` at class level,
  `@Transactional` on writes. Mutate loaded entities and rely on dirty checking — no explicit `save()` on
  updates. Ownership checks use an `isAdmin` boolean from the controller (extracted from
  `Authentication.getAuthorities()`). Handwritten `@Component` mappers (no MapStruct).
- New code under `com.quill.*` only.

## Tests

- JUnit 5 + AssertJ + Mockito.
- **Service tests**: `@ExtendWith(MockitoExtension.class)`, `@Nested` + `@DisplayName`.
- **Repository tests**: `@DataJpaTest`
  `@AutoConfigureTestDatabase(replace = NONE)`
  `@Import({TestcontainersConfiguration.class, JpaConfig.class})`.
- **Controller tests**: `@WebMvcTest(SomeController.class)` + `@Import(TestSecurityConfig.class)`.
  Uses AssertJ `MockMvcTester` (`@Autowired`) and `@MockitoBean` (Boot 4.x replacement for `@MockBean`).
  `TestSecurityConfig` mirrors security rules with HTTP Basic instead of JWT.
