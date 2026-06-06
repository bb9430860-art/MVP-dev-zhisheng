# Zhisheng Backend

This backend is a Spring Boot 3 + Java 17 Maven multi-module application.

## Local Test

Run process backend tests from the backend root:

```bash
mvn -pl zhisheng-app -am test
```

## Local Install

Build and install the app module with required upstream modules:

```bash
mvn -pl zhisheng-app -am install
```

## Local Dev Startup

Use the `dev` profile for quick frontend integration. It uses an H2 in-memory database and runs Flyway migrations automatically.

```bash
mvn -pl zhisheng-app spring-boot:run -Dspring-boot.run.profiles=dev
```

The dev profile database is intentionally local-only:

```text
jdbc:h2:mem:zhisheng_dev;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1
```

Flyway runs existing migrations from:

```text
classpath:db/migration
```

## MariaDB Configuration

MariaDB remains the formal database target for production and later shared environments. Add a dedicated profile such as `application-prod.yml` or environment-based datasource settings when the deployment database is ready.

Do not treat H2 as the production database design. It is only for local dev startup and fast admin-web integration.
