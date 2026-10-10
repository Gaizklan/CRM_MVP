# Guía para el agente de desarrollo

Este repositorio implementa la plataforma CRM web descrita en `docs/`. Lee, en este orden, antes de empezar cualquier issue:
1. `docs/briefs/00-vision.md`
2. `docs/briefs/01-architecture.md` (stack, módulos y reglas — son obligatorios)
3. El brief del epic al que pertenece el issue (`10-campaigns`, `20-cms`, `30-web`)
4. La sección de `docs/requirements.md` citada en el issue

## Cómo elegir trabajo
- Trabaja por milestone en orden: M0 → M1 → M2 → M3 → M4.
- Dentro del milestone, coge el issue abierto de menor número cuyas dependencias (`Depende de: #N`) estén cerradas.
- Un issue = una rama = un PR. Rama: `feat/<nº-issue>-<slug>` (o `fix/`, `chore/`).

## Definition of Done (todo PR)
- [ ] Cumple todos los criterios de aceptación del issue; marca cada uno en la descripción del PR.
- [ ] `mvn -B verify` pasa en local y en CI.
- [ ] Tests unitarios de dominio y, si toca persistencia o integración, tests con Testcontainers/GreenMail/WireMock.
- [ ] Reglas ArchUnit en verde (dominio sin Spring/JPA, sin acceso a tablas de otros módulos).
- [ ] Migraciones Flyway nuevas, nunca editando las ya publicadas.
- [ ] Endpoints nuevos documentados con OpenAPI.
- [ ] PR con descripción `Closes #<nº>` y resumen de decisiones.

## Reglas
- No inventes nombres de empresas, marcas o sectores reales; usa `Marca A`, `Marca B`, segmento `S1`.
- Nunca envíes ni almacenes contraseñas en claro; nunca envíes comunicaciones sin opt-in del canal.
- Si un requisito es ambiguo, toma la opción más simple coherente con los briefs, documéntala en el PR y, si cambia el diseño, en `docs/adr/NNNN-titulo.md`.
- Si estás bloqueado (falta una decisión de negocio), comenta en el issue con la pregunta concreta, añade la etiqueta `needs-decision` y pasa al siguiente issue disponible.
- No amplíes el alcance: lo que encuentres fuera del issue, créalo como issue nuevo con etiqueta `follow-up`.

## Comandos
```bash
docker compose up -d          # PostgreSQL, MailHog, mock SMS
mvn -B verify                 # build + tests
mvn -pl crm-app spring-boot:run
```
