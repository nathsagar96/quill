# AGENTS.md

## Stack

- Spring Boot **4.0.6**, Java **25**, single Maven module (`com.quill:quill`).
- WebMVC, Spring Data JPA, Flyway + `flyway-database-postgresql`, PostgreSQL 18.
- Lombok + `spring-boot-configuration-processor` (both wired as annotation processor paths in `maven-compiler-plugin` —
  do not move them to plain deps).
- Optional runtime dep `spring-boot-docker-compose` auto-starts services from `compose.yaml` in dev/test.

## Build / run

- Use the wrapper: `./mvnw` (system `mvn` is not assumed). Requires JDK 25.
- Common goals: `./mvnw spring-boot:run`, `./mvnw test`, `./mvnw package`.
- Final jar name is `target/quill` (not `quill-0.0.1-SNAPSHOT`) — `pom.xml` sets
  `<finalName>${project.artifactId}</finalName>`.
- `HELP.md` is gitignored Spring Initializr boilerplate; ignore it.

## Database

- No datasource in `src/main/resources/application.yaml` — connection is auto-wired by `spring-boot-docker-compose` from
  `compose.yaml` (user `myuser`, pw `secret`, db `mydatabase`, port 5432).
- Tests do **not** use `compose.yaml`; they spin up Postgres via Testcontainers (`postgres:18-alpine`) with
  `@ServiceConnection` in `src/test/java/com/quill/TestcontainersConfiguration.java`. Keep the test image in sync with
  `compose.yaml`.
- Tests require a working Docker daemon; `TestcontainersConfiguration` must be `@Import`ed (already done in
  `ApplicationTests`).
- Flyway migrations live at `src/main/resources/db/migration/` (Spring Boot default; folder exists but is empty — add
  `V1__...sql` files there when needed).

## Layout

- Main entrypoint: `src/main/java/com/quill/Application.java` (package root `com.quill`).
- `src/test/java/com/quill/TestApplication.java` boots the app with `TestcontainersConfiguration` — useful as an IDE run
  config that exercises the full context.
- No `.github/`, `.opencode/`, CI, or lint/format config exists — do not invent one unless asked.

## Conventions

- Do not add `lombok` or `spring-boot-configuration-processor` as regular `<dependency>` entries for processing — they
  are already declared as `annotationProcessorPaths` in `pom.xml`.
- Do not delete the `spring-boot-docker-compose` `<excludes>` block for Lombok in `spring-boot-maven-plugin` — it keeps
  Lombok out of the fat jar.
- New code goes under `com.quill.*`; do not introduce new top-level packages without a reason.
