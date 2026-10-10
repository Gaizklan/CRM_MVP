# Brief 00 — Visión del producto

## Problema
Los equipos CRM de una organización multimarca necesitan lanzar campañas de comunicación personalizadas, publicar contenidos adaptados al perfil del consumidor y medir resultados, sin depender de un desarrollo a medida por cada campaña.

## Propuesta
Una plataforma CRM web reutilizable y multimarca con tres componentes sobre una base de datos CRM común:

| Componente | Brief | Epic |
|---|---|---|
| Plataforma base (multimarca, consumidores, reglas, seguridad) | [01-architecture](01-architecture.md) | EPIC 0 |
| Gestión de campañas | [10-campaigns](10-campaigns.md) | EPIC 1 |
| Gestión de contenidos (CMS) | [20-cms](20-cms.md) | EPIC 2 |
| Interfaz web / portal | [30-web](30-web.md) | EPIC 3 |

## Usuarios
- **Consumidor**: navega, se registra, gestiona preferencias y consentimientos, recibe comunicaciones.
- **Gestor CRM**: define poblaciones, campañas y workflows; analiza resultados.
- **Productor / Revisor de contenidos / Desarrollador**: crean, revisan y publican contenidos y plantillas.
- **Administrador**: gestiona Marcas, usuarios internos, proveedores e integración.

## Objetivos medibles
- Una campaña nueva a partir de una plantilla de workflow existente se configura sin código.
- Ninguna comunicación se envía sin opt-in vigente del canal.
- Cero duplicados tras una caída a mitad de campaña.
- Cada envío, apertura y clic queda trazado y consultable por consumidor y campaña.

## Fases (milestones)
| Milestone | Alcance |
|---|---|
| **M0 Fundaciones** | Esqueleto del monorepo, multimarca, modelo de consumidor, consentimientos, reglas, seguridad interna, CI |
| **M1 Campañas básicas** | Poblaciones, plantillas de mensaje, canales email/SMS, workflow lineal, control de ejecución, reintentos, recuperación, tracking de envíos |
| **M2 Portal y CMS** | Registro/login, preferencias, baja, CMS con plantillas, roles editoriales, renderizado personalizado |
| **M3 Campañas avanzadas** | Workflows con condiciones (apertura/clic), triggered, MGM, coleccionables, push e inbox, pruebas de campaña |
| **M4 Medición** | Tracking de contenidos, analítica web, KPIs, exportación a BI |

## Fuera de alcance inicial
- Consola de administración visual (M0–M3 son API-first; se documenta con OpenAPI).
- Editor visual de workflows (los workflows se definen en JSON/YAML).
