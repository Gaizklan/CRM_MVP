# Backlog

Generado desde `planning/backlog.py`. La fuente de verdad del estado son los GitHub Issues.

## M0 Fundaciones
_Monorepo, multimarca, consumidores, consentimientos, reglas, seguridad, CI_

| Clave | Epic | Issue | Req. | Depende de |
|---|---|---|---|---|
| P01 | E0 | Esqueleto del monorepo Maven, aplicación y CI | §1 | — |
| P02 | E0 | Reglas de arquitectura y calidad (ArchUnit, Spotless, JaCoCo) | §1 NFR-1 | P01 |
| P03 | E0 | Soporte multimarca: entidad Marca y contexto de tenant | NFR-2 | P01 |
| P04 | E0 | Usuarios internos, niveles de seguridad y roles | §3.1 | P03 |
| P05 | E0 | Modelo de consumidor y preferencias | §4.6 | P03 |
| P06 | E0 | Consentimientos opt-in/opt-out auditables | §2.12 | P05 |
| P07 | E0 | Suscripciones a servicios del programa | §2.11 | P06 |
| P08 | E0 | Motor de reglas de negocio: DSL y evaluación en memoria | §2.1, §3.2 | P05 |
| P09 | E0 | Traducción del DSL de reglas a SQL para segmentación | §2.10 | P08 |
| P10 | E0 | Cifrado de datos personales y comunicaciones | §2.16 | P05 |
| P11 | E0 | Eventos de dominio y registro de eventos de usuario | §2.2 | P05 |
| P12 | E0 | Configuración por Marca | NFR-4 | P03 |

## M1 Campañas básicas
_Poblaciones, plantillas, email/SMS, workflow lineal, control, reintentos, recuperación_

| Clave | Epic | Issue | Req. | Depende de |
|---|---|---|---|---|
| C01 | E1 | Poblaciones: criterio de segmentación e instancias | §2.10 | P09 |
| C02 | E1 | Plantillas de mensaje (email texto, email HTML, SMS) | §2.3 | P05, P12 |
| C03 | E1 | Canal email con varios servidores SMTP | §2.9 | P12 |
| C04 | E1 | Canal SMS con varios proveedores HTTP | §2.9 | P12 |
| C05 | E1 | Outbox de comunicaciones con verificación de opt-in e idempotencia | §2.12, §2.14 | P06, C03, C04 |
| C06 | E1 | Reintentos configurables y estado de fallo | §2.15 | C05 |
| C07 | E1 | Modelo de campaña y definición de workflow | §2.1 | C01, C02 |
| C08 | E1 | Motor de workflow: ejecución de nodos ACTION, WAIT y END | §2.1 | C07, C05 |
| C09 | E1 | Control de ejecución: arrancar, pausar, reanudar, parar | §2.13 | C08 |
| C10 | E1 | Recuperación ante caídas sin duplicados | §2.14 | C08, C06 |
| C11 | E1 | Tracking de envíos y archivo de comunicaciones | §2.17 | C05 |
| C12 | E1 | Campañas de difusión (broadcast) | §2.4 | C09, C11 |
| C13 | E1 | Exportaciones por plantilla de fichero y consulta SQL | §2.3 | C01 |

## M2 Portal y CMS
_Portal del consumidor y CMS con renderizado personalizado_

| Clave | Epic | Issue | Req. | Depende de |
|---|---|---|---|---|
| S01 | E2 | Modelo de contenido, tipos y assets | §3.3 | P04 |
| S02 | E2 | Versionado de contenidos | §3 | S01 |
| S03 | E2 | Workflow editorial con notificaciones | §3 | S02 |
| S04 | E2 | Permisos por categoría y tipo de contenido | §3.1 | S03 |
| S05 | E2 | Plantillas de página y catálogo de componentes | §3.5 | S01 |
| S06 | E2 | Renderizado personalizado según reglas | §3.2, §3.5 | S05, P08 |
| S07 | E2 | Menú de navegación dinámico | §3.4 | S06 |
| S08 | E2 | Contenidos multiidioma | §3.8 | S01 |
| S09 | E2 | Previsualización con perfil simulado | §3.7 | S06 |
| W01 | E3 | Portal por Marca: layout, navegación anónima y registrada | §4.1 | S06, S07 |
| W02 | E3 | Registro con verificación de email y notificación configurable | §4.2 | W01, P06, C02 |
| W03 | E3 | Login y bloqueo por intentos fallidos | §4.8 | W02 |
| W04 | E3 | Recuperación de contraseña | §4.3 | W02 |
| W05 | E3 | Preferencias, suscripciones y consentimientos del consumidor | §4.5, §4.6 | W03, P07 |
| W06 | E3 | Baja del programa y supresión de datos | §4.4 | W05, C05 |
| W08 | E3 | Publicación de eventos de usuario del portal | §2.2 | W03, P11 |
| W09 | E3 | Accesibilidad, responsive, compatibilidad y seguridad web | §4.7 | W02 |

## M3 Campañas avanzadas
_Condiciones, triggered, MGM, coleccionables, push, inbox, pruebas_

| Clave | Epic | Issue | Req. | Depende de |
|---|---|---|---|---|
| C14 | E1 | Tracking de apertura de email | §2.17 | C11, P11 |
| C15 | E1 | Tracking de clics con códigos por zona | §2.17 | C11, P11 |
| C16 | E1 | Condiciones y esperas por evento en el workflow; workflow de referencia | §2.1 | C14, C15, C08 |
| C17 | E1 | Campañas disparadas por eventos (triggered) | §2.6 | C16 |
| C18 | E1 | Generación de códigos únicos de respuesta y canje | §2.2 | C08 |
| C19 | E1 | Campañas Member get Member (MGM) | §2.5 | C16, C18 |
| C20 | E1 | Campañas de coleccionables (puntos/códigos) | §2.7 | C18 |
| C21 | E1 | Canal de notificaciones push | §2.2, §2.3 | C05, W01 |
| C22 | E1 | Buzón del portal y banners/modales | §2.2 | C08, W01 |
| C23 | E1 | Pruebas de campaña (dry-run) | §2.8 | C16 |
| C24 | E1 | Catálogo de plantillas de workflow reutilizables | §2.1 | C16, C19 |
| W07 | E3 | Verificación de elegibilidad configurable por Marca | §4.2 | W02, C16 |

## M4 Medición
_Tracking de contenidos, analítica web, KPIs, BI_

| Clave | Epic | Issue | Req. | Depende de |
|---|---|---|---|---|
| S10 | E2 | Tracking de acceso y acciones sobre contenidos | §3.6 | S06, P11 |
| W10 | E3 | Analítica web con consentimiento de cookies | §4.9 | W01 |
| K01 | E1 | KPIs de campañas y API de informes | NFR-7 | C16, S10 |
| K02 | E0 | Exportación a herramienta de BI | NFR-3 | K01 |
