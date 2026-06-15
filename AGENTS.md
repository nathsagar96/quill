# AGENTS.md

## Stack

- Spring Boot **4.0.7**, Java **25**, single Maven module (`com.quill:quill`).
- Virtual threads enabled (`spring.threads.virtual.enabled: true`) — critical for async processing.
- WebMVC, Spring Data JPA (Hibernate), Flyway + `flyway-database-postgresql`, PostgreSQL 18, Bean Validation.
- Spring Security with **JWT** auth (JJWT 0.13) — stateless, CSRF disabled.
- ETag via `ShallowEtagHeaderFilter` on categories, tags, posts.
- Springdoc OpenAPI at `/swagger-ui/`, `/v3/api-docs/`.
- Actuator + Prometheus metrics.
- Lombok + `spring-boot-configuration-processor` only in `annotationProcessorPaths` — do not move to `<dependency>`.
- `JWT_SECRET` env var **required** — app fails with `IllegalStateException` if unset.
- Redis used for both caching (`spring.cache.type=redis`) and Bucket4j rate limiting.
- ShedLock for distributed scheduling (PG-backed `shedlock` table).
- `spring-boot-docker-compose` auto-starts services from `compose.yaml` in dev/test.

## Build / run

- Use the wrapper: `./mvnw` (system `mvn` is not assumed). Requires JDK 25.
- Common goals: `./mvnw spring-boot:run`, `./mvnw test`, `./mvnw package`, `./mvnw spotless:apply`.
- Single test: `./mvnw test -Dtest=PostServiceTest` or method `-Dtest=PostServiceTest#createsAndReturns`.
- Coverage: `./mvnw verify` enforces **80% instruction / 70% branch** minimum (JaCoCo). Excludes: `Application`, `config/*`, `filter/*`, `dto/*`, `scheduler/*`, `EmailNotificationListener`.
- Final jar is `target/quill.jar`.
- `spotless:apply` runs Palantir Java Format manually — not bound to lifecycle.

## Database

- Datasource auto-wired by `spring-boot-docker-compose` from `compose.yaml` in dev (user `quill`, pw `secret`, db `quill`, port 5432). Static fallback in `application.yaml`.
- `spring.jpa.hibernate.ddl-auto=validate` — schema MUST come from Flyway. New entity fields require `V{n}__*.sql` in `src/main/resources/db/migration/` (current: V1–V16).
- `spring.jpa.open-in-view=false`; lazy associations only resolve inside a transactional boundary.
- Tests use Testcontainers for **both** PostgreSQL (`postgres:18-alpine`) **and** Redis (`redis:8-alpine`) via `@ServiceConnection` in `TestcontainersConfiguration.java` — not `compose.yaml`. Requires Docker.
- Flyway runs in `@DataJpaTest` (`spring-boot-starter-flyway-test`), so repos see real schema.
- `compose.yaml` also starts **Mailpit** (axllent/mailpit) on ports 1025 (SMTP) and 8025 (web UI) for dev email.

## Layout

- Entrypoint: `Application.java`. `main` is **package-private** (`static void main`) — do not make `public`.
- Packages under `com.quill`: `model/`, `repository/`, `service/`, `controller/`, `mapper/`, `dto/request/`, `dto/response/`, `exception/`, `config/`, `security/`, `scheduler/`, `filter/`, `event/`.
- `AppConfig` enables `@EnableAsync` (needed for email events) and registers `@ConfigurationProperties` for `JwtProperties`, `AppProperties`, `CorsProperties`, `RateLimitProperties`.
- `JpaConfig` carries `@EnableJpaAuditing` *and* `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`; `@DataJpaTest` slices must `@Import({TestcontainersConfiguration.class, JpaConfig.class})` or `@CreatedDate`/`@LastModifiedDate` stay null.
- `SchedulingConfig` (`@EnableScheduling` + `@EnableSchedulerLock`) enables `@Scheduled` + ShedLock distributed locks.
- `PostScheduler` publishes `SCHEDULED` posts every 60s.
- `TokenCleanupScheduler` deletes expired password-reset tokens (daily 3am) and expired/revoked refresh tokens (daily 4am), both with ShedLock.
- `CacheConfig` registers `ShallowEtagHeaderFilter` on `/api/categories/*`, `/api/tags/*`, `/api/posts/**` and `@EnableCaching`.
- `CacheControlFilter` sets Cache-Control on GET: categories/tags → `max-age=3600, public`, post lists → `max-age=60, public, stale-while-revalidate=300`, single post → `max-age=60, private`.
- `RateLimitingFilter` (filter order 2) — Redis-backed Bucket4j, per-endpoint config from `app.rate-limit`, skips `/actuator`, `/swagger-ui`, `/v3/api-docs`. 429 responses use RFC 7807 `ProblemDetail`.
- `GlobalExceptionHandler` extends `ResponseEntityExceptionHandler`, maps `ApplicationException` + Spring Security exceptions to RFC 7807 `ProblemDetail`. Handles `MethodArgumentNotValidException` with field-level error details.

## Security

- `SecurityConfig` filter chain:
    - `/api/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` — permit all
    - `GET /api/posts/me` — authenticated
    - `GET /api/posts`, `/api/posts/*`, `/api/posts/slug/*`, `/api/categories`, `/api/categories/*`, `/api/tags`, `/api/tags/*` — permit all
    - `/actuator/health`, `/actuator/info`, `/actuator/prometheus` — permit all
    - `DELETE /api/posts/**`, `/api/categories/**`, `/api/tags/**` — `ROLE_ADMIN`
    - `/actuator/**` — `ROLE_ADMIN`
    - Everything else — authenticated
- CORS allows `http://localhost:5173` (Vite dev server) via `CorsProperties`.
- JWT: HMAC-SHA via `JwtService`. Signing key from `app.jwt.secret` (base64 in `application.yaml`, 24h expiry). `JWT_SECRET` env var **required** — app fails with `IllegalStateException` if unset. `JwtAuthenticationFilter` skips `/api/auth/**` via `PathPatternRequestMatcher`, logs warnings on expired/invalid tokens (does not short-circuit — 401 handled by Spring Security).
- `PasswordEncoder` is BCrypt. `@EnableMethodSecurity` is on.
- `Role` enum: `USER` (default), `ADMIN`.

## Conventions

- DTOs are Java `record`s with Jakarta validation (`dto/request/`); responses in `dto/response/`.
- Entities use Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` + `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate`/`@LastModifiedDate Instant`. New entities need matching `created_at`/`updated_at` columns in a Flyway migration.
- `PostStatus` enum: `DRAFT`, `SCHEDULED`, `PUBLISHED`. Created posts default to `DRAFT`.
- Scheduler (`PostScheduler`) transitions `SCHEDULED` → `PUBLISHED` based on `scheduled_at < now`.
- `SlugService.toUniqueSlug(title, entityType, existsBySlug)` generates unique slugs; used by PostService on creation and title changes.
- Fulltext search via `posts.search_vector` tsvector (GIN index). `PostRepository.searchPostIds(query, pageable)` queries via the index.
- Sealed exception hierarchy: `ApplicationException` permits 11 specific exceptions. New ones must be `final`, extend `ApplicationException`, carry an `HttpStatus`, and join the `permits` clause.
- Error responses are RFC 7807 `ProblemDetail` (`spring.mvc.problemdetails.enabled=true`). Don't write custom JSON bodies.
- Services: `@Service @RequiredArgsConstructor @Transactional(readOnly = true)` at class level, `@Transactional` on writes. Mutate loaded entities and rely on dirty checking — no explicit `save()` on updates. Ownership checks receive `username` string from controller and compare with resource owner's field directly, throwing `ForbiddenOperationException` on mismatch. Handwritten `@Component` mappers (no MapStruct).
- Events (`event/`): `UserRegisteredEvent` and `PasswordResetRequestedEvent` are Spring `record`s published via `ApplicationEventPublisher`; `EmailNotificationListener` handles them with `@Async @TransactionalEventListener(phase = AFTER_COMMIT)` — only fires after the publishing tx commits.
- New code under `com.quill.*` only.

## Production profile (`application-prod.yaml`)

- Disables Swagger UI and API docs.
- Switches health check to `show-details: when-authorized`, enables Kubernetes probes.
- Enables structured ECS JSON logging.
- Configures mail SMTP auth/starttls, HikariCP connection pool.
- Sets `app.base-url` and `app.cors.allowed-origins` to the production domain.

## Tests

- JUnit 5 + AssertJ + Mockito.
- **Service tests**: `@ExtendWith(MockitoExtension.class)`, `@Nested` + `@DisplayName`.
- **Repository tests**: `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@Import({TestcontainersConfiguration.class, JpaConfig.class})`.
- **Controller tests**: `@WebMvcTest(SomeController.class)` + `@Import({TestSecurityConfig.class, CacheControlFilter.class})`. Uses AssertJ `MockMvcTester` (`@Autowired`) and `@MockitoBean` (Boot 4.x replacement for `@MockBean`). `TestSecurityConfig` mirrors security rules with HTTP Basic instead of JWT.
- **Mapper tests**: Plain JUnit 5 (no `@ExtendWith`), instantiate mapper with `new` directly.
- **JWT/service tests**: Plain JUnit 5, instantiate `JwtService` with `new JwtService(new JwtProperties(secret, duration))`.
