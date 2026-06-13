# AGENTS.md

## Stack

- Spring Boot **4.0.6**, Java **25**, single Maven module (`com.quill:quill`).
- Virtual threads enabled (`spring.threads.virtual.enabled: true`).
- WebMVC, Spring Data JPA (Hibernate), Flyway + `flyway-database-postgresql`, PostgreSQL 18, Bean Validation.
- Spring Security with **JWT** auth (JJWT 0.13) — stateless, CSRF disabled.
- Caffeine caching + ETag via `ShallowEtagHeaderFilter` on categories, tags, posts.
- Springdoc OpenAPI at `/swagger-ui/`, `/v3/api-docs/`.
- Actuator + Prometheus metrics.
- Lombok + `spring-boot-configuration-processor` are wired only as `annotationProcessorPaths`
  in `maven-compiler-plugin` — do **not** move them to plain `<dependency>` entries.
- Runtime dep `spring-boot-docker-compose` auto-starts services from `compose.yaml` in dev/test.

## Build / run

- Use the wrapper: `./mvnw` (system `mvn` is not assumed). Requires JDK 25.
- Common goals: `./mvnw spring-boot:run`, `./mvnw test`, `./mvnw package`, `./mvnw spotless:apply`.
- Single test: `./mvnw test -Dtest=PostServiceTest` or method `-Dtest=PostServiceTest#createsAndReturns`.
- Final jar is `target/quill.jar` — `pom.xml` sets `<finalName>${project.artifactId}</finalName>`.

## Database

- Datasource is auto-wired by `spring-boot-docker-compose` from `compose.yaml` in dev (user `quill`,
  pw `secret`, db `quill`, port 5432). Static fallback in `application.yaml` points to same values.
- `spring.jpa.hibernate.ddl-auto=validate` — schema MUST come from Flyway. New entity fields require
  `V{n}__*.sql` in `src/main/resources/db/migration/` (current baseline: V1 – V12 covering `users`, `posts`,
  `comments`, `categories`, `tags`, join tables, roles, bios, slugs, excerpts, `refresh_tokens`,
  email verification on `users`, `password_reset_tokens`, `status`/`published_at`/`scheduled_at`
  on `posts`, and fulltext search via `search_vector tsvector` with GIN index).
- `spring.jpa.open-in-view=false`; lazy associations only resolve inside a transactional boundary.
- Tests use Testcontainers (`postgres:18-alpine`) via `@ServiceConnection` in
  `TestcontainersConfiguration.java` — not `compose.yaml`. Keep image in sync. Requires Docker.
- Flyway runs in `@DataJpaTest` (`spring-boot-starter-flyway-test`), so repos see real schema.
- `compose.yaml` also starts **Mailpit** (axllent/mailpit) on ports 1025 (SMTP) and 8025 (web UI) for dev email.

## Layout

- Entrypoint: `Application.java`. `main` is **package-private** (`static void main`) — do not make `public`.
- Packages under `com.quill`: `model/`, `repository/`, `service/`, `controller/`, `mapper/`,
  `dto/request/`, `dto/response/`, `exception/`, `config/`, `security/`, `scheduler/`, `filter/`.
- `JpaConfig` carries `@EnableJpaAuditing` *and* `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`;
  `@DataJpaTest` slices must `@Import({TestcontainersConfiguration.class, JpaConfig.class})` or
  `@CreatedDate`/`@LastModifiedDate` stay null.
- `SchedulingConfig` (`@EnableScheduling`) turns on `@Scheduled`; `PostScheduler` polls every 60s to
  publish due `SCHEDULED` posts.
- `CacheConfig` (`@EnableCaching`) uses Caffeine for `categories` and `tags` (max 100, 1h TTL), and
  registers `ShallowEtagHeaderFilter` on `/api/categories/*`, `/api/tags/*`, `/api/posts/**`.
- `CacheControlFilter` adds `Cache-Control` headers on GET: categories/tags → `max-age=3600, public`,
  post list → `max-age=60, public, stale-while-revalidate=300`, individual post → `max-age=60, private`.
- `GlobalExceptionHandler` extends `ResponseEntityExceptionHandler` and maps `ApplicationException` +
  Spring Security exceptions to RFC 7807 `ProblemDetail`. Also handles `MethodArgumentNotValidException`
  with field-level error details.
- `spotless-maven-plugin` (Palantir Java Format) is **not** bound to lifecycle — run manually:
  `./mvnw spotless:apply`.
- No CI, no `.github/`.

## Security

- `SecurityConfig` registers the filter chain:
    - `/api/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` — permit all
    - `/actuator/health`, `/actuator/info`, `/actuator/prometheus` — permit all
    - `GET /api/posts`, `/api/posts/*`, `/api/posts/slug/*`, `/api/categories`, `/api/categories/*`,
      `/api/tags`, `/api/tags/*` — permit all
    - `DELETE /api/posts/**`, `/api/categories/**`, `/api/tags/**` — `ROLE_ADMIN`
    - `/actuator/**` — `ROLE_ADMIN`
    - Everything else — authenticated
- CORS allows `http://localhost:5173` (Vite dev server) via `CorsProperties`.
- JWT: HMAC-SHA via `JwtService`. Signing key from `quill.jwt.secret` (base64 in `application.yaml`, 24h expiry).
  `JwtAuthenticationFilter` skips `/api/auth/**` via `PathPatternRequestMatcher`, sends 401 on expired/invalid.
- `PasswordEncoder` is BCrypt. `@EnableMethodSecurity` is on.
- `Role` enum: `USER` (default), `ADMIN`.
- `AppConfig` enables `@ConfigurationProperties` for `JwtProperties`, `AppProperties`, `CorsProperties`.

## Conventions

- DTOs are Java `record`s with Jakarta validation (`dto/request/`); responses in `dto/response/`.
- Entities use Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` +
  `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate`/`@LastModifiedDate Instant`. New entities
  need matching `created_at`/`updated_at` columns in a Flyway migration.
- `PostStatus` enum: `DRAFT`, `SCHEDULED`, `PUBLISHED`. Created posts default to `DRAFT`.
- Scheduler (`PostScheduler`) transitions `SCHEDULED` → `PUBLISHED` based on `scheduled_at < now`.
- `SlugService.toUniqueSlug(title, entityType, existsBySlug)` generates unique slugs; used by PostService
  on creation and title changes.
- Fulltext search via `posts.search_vector` tsvector (GIN index, V12). Trigger auto-updates on title/body
  changes. `PostRepository.searchPosts(query, pageable)` queries via the index.
- Sealed exception hierarchy: `ApplicationException` permits `CategoryNotFoundException`,
  `CommentNotFoundException`, `DuplicateEmailException`, `DuplicateUsernameException`,
  `EmailVerificationException`, `ForbiddenOperationException`, `PasswordResetTokenException`,
  `PostNotFoundException`, `RefreshTokenException`, `TagNotFoundException`, `UserNotFoundException`.
  New ones must be `final`, extend `ApplicationException`, carry an `HttpStatus`, and join the `permits` clause.
- Error responses are RFC 7807 `ProblemDetail` (`spring.mvc.problemdetails.enabled=true`). Don't write
  custom JSON bodies.
- Services: `@Service @RequiredArgsConstructor @Transactional(readOnly = true)` at class level,
  `@Transactional` on writes. Mutate loaded entities and rely on dirty checking — no explicit `save()` on
  updates. Ownership checks use an `isAdmin` boolean from the controller (extracted from
  `Authentication.getAuthorities()`). Handwritten `@Component` mappers (no MapStruct).
- Events (`event/`): `UserRegisteredEvent` and `PasswordResetRequestedEvent` are Spring `record`s
  published via `ApplicationEventPublisher`; listeners in the same service layer handle email dispatch.
- `application-prod.yaml` provides production overrides: Hikari pool tuning, structured ECS logging,
  Swagger disabled, readiness/liveness probes enabled.
- New code under `com.quill.*` only.

## Tests

- JUnit 5 + AssertJ + Mockito.
- **Service tests**: `@ExtendWith(MockitoExtension.class)`, `@Nested` + `@DisplayName`.
- **Repository tests**: `@DataJpaTest`
  `@AutoConfigureTestDatabase(replace = NONE)`
  `@Import({TestcontainersConfiguration.class, JpaConfig.class})`.
- **Controller tests**: `@WebMvcTest(SomeController.class)` +
  `@Import({TestSecurityConfig.class, CacheControlFilter.class})`.
  Uses AssertJ `MockMvcTester` (`@Autowired`) and `@MockitoBean` (Boot 4.x replacement for `@MockBean`).
  `TestSecurityConfig` mirrors security rules with HTTP Basic instead of JWT.
- **Mapper tests**: Plain JUnit 5 (no `@ExtendWith`), instantiate mapper with `new` directly.
