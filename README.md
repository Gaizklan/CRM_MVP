# CRM Web Platform

Plataforma CRM web multimarca: gestión de campañas de comunicación, gestión de contenidos personalizados y portal del consumidor.

## Documentación
- [Requisitos funcionales](docs/requirements.md)
- Briefs: [Visión](docs/briefs/00-vision.md) · [Arquitectura](docs/briefs/01-architecture.md) · [Campañas](docs/briefs/10-campaigns.md) · [CMS](docs/briefs/20-cms.md) · [Portal web](docs/briefs/30-web.md)
- [Guía para el agente de desarrollo](CLAUDE.md)

## Planificación
El trabajo está en GitHub Issues, agrupado en 4 epics y 5 milestones (M0 Fundaciones → M4 Medición). Cada issue enlaza su brief y la sección de requisitos correspondiente.

## Stack
Java 21 · Spring Boot 3 · Maven multimódulo · PostgreSQL · Flyway · Thymeleaf + HTMX · Testcontainers

## Desarrollo local
```bash
docker compose up -d                  # PostgreSQL 16 (5432), MailHog (SMTP 1025, UI 8025), mock SMS (8089)
mvn -B verify                         # build + tests (los tests con Testcontainers se saltan sin Docker)
mvn -pl crm-app spring-boot:run       # http://localhost:8080/actuator/health · /v3/api-docs · /swagger-ui.html
```
Variables de entorno de la aplicación: `CRM_DB_URL`, `CRM_DB_USER`, `CRM_DB_PASSWORD`.
El mock SMS acepta `POST http://localhost:8089/sms/send` y responde `202` con un `messageId`.

## Calidad
- `mvn spotless:apply` formatea el código (google-java-format); `mvn -B verify` falla si hay código sin formatear.
- ArchUnit (`crm-app/src/test/java/org/example/crm/architecture`) verifica la arquitectura hexagonal de todos los módulos.
- JaCoCo exige ≥ 80 % de líneas en los paquetes `org.example.crm.*.domain` (los tests de dominio deben vivir en el propio módulo).
