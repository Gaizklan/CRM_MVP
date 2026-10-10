# Brief 01 — Arquitectura y plataforma base

## Stack
- Java 21, Spring Boot 3.x, Maven multimódulo (parent POM con `dependencyManagement`).
- PostgreSQL 16 + Flyway; Spring Data JPA (Hibernate).
- Spring Security (usuarios internos: formulario + roles; consumidores: sesión en el portal).
- Portal del consumidor: Thymeleaf + HTMX (SSR, responsive, WCAG 2.1 AA).
- APIs de administración REST documentadas con springdoc-openapi.
- Tests: JUnit 5, AssertJ, Testcontainers (PostgreSQL), GreenMail (SMTP), WireMock (pasarelas SMS/push).
- Calidad: ArchUnit para reglas de arquitectura, Spotless (formato), JaCoCo (cobertura ≥ 80 % en dominio).
- CI: GitHub Actions (`mvn -B verify`).
- Local: `docker compose` con PostgreSQL, MailHog y un mock de pasarela SMS.

## Estilo arquitectónico
Arquitectura hexagonal por módulo:
```
<modulo>/
  domain/        modelo y reglas puras, sin Spring ni JPA
  application/   casos de uso (puertos de entrada) y puertos de salida
  infrastructure/ adaptadores: persistencia JPA, REST, proveedores externos
```
Reglas (verificadas con ArchUnit):
- `domain` no depende de `application` ni de `infrastructure`, ni de Spring/JPA.
- Los módulos se comunican por puertos o eventos de dominio, nunca accediendo a tablas ajenas.

## Módulos Maven
| Módulo | Responsabilidad |
|---|---|
| `crm-common` | Kernel compartido: `BrandId`, `TenantContext`, errores, eventos base, utilidades de tiempo/IDs |
| `crm-consumer` | Perfil del consumidor, preferencias, consentimientos (opt-in/out), suscripciones |
| `crm-rules` | Motor de reglas de negocio: DSL de criterios en JSON evaluable en memoria y traducible a SQL |
| `crm-campaign` | Campañas, poblaciones e instancias, motor de workflow, control de ejecución |
| `crm-messaging` | Plantillas de mensaje, adaptadores de canal (email, SMS, push, inbox), enrutado de proveedores, reintentos, outbox |
| `crm-tracking` | Eventos de usuario, tracking de comunicaciones (entrega, apertura, clic) y de contenidos |
| `crm-cms` | Contenidos, assets, versionado, workflow editorial, plantillas de página, renderizado personalizado |
| `crm-web` | Portal del consumidor (registro, login, preferencias, baja) |
| `crm-admin-api` | APIs REST de administración |
| `crm-app` | Arranque Spring Boot, configuración, migraciones Flyway |

## Decisiones transversales
- **Multimarca**: toda entidad de negocio lleva `brand_id`; filtro obligatorio en repositorios; usuarios internos *globales* o *locales* (una Marca).
- **Idempotencia y recuperación**: los envíos se generan como registros *outbox* con clave idempotente `(campaign_execution_id, node_id, consumer_id, attempt_group)`; un procesador reanuda tras reinicio sin duplicar.
- **Planificación**: `@Scheduled` + ShedLock para ejecución en varias instancias.
- **Reglas de negocio**: un único DSL reutilizado por poblaciones, condiciones de workflow, personalización de contenidos y menús.
- **Cifrado**: TLS en tránsito; cifrado de columnas PII (email, teléfono) con clave externa (AES-GCM); contraseñas con BCrypt/Argon2.
- **Privacidad (RGPD)**: consentimiento auditable (quién, cuándo, canal, origen); supresión por anonimización.
- **Configuración por Marca**: remitentes, plantillas, proveedores, nº de reintentos, umbral de bloqueo de login, idiomas.
- **Datos de ejemplo**: solo nombres genéricos (`Marca A`, `Marca B`, segmento `S1`). No usar nombres de empresas reales.
