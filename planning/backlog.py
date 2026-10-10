"""Fuente única del backlog. Genera planning/issues.json y docs/backlog.md.

Uso: python3 planning/backlog.py
"""
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent

MILESTONES = {
    "M0": ("M0 Fundaciones", "Monorepo, multimarca, consumidores, consentimientos, reglas, seguridad, CI"),
    "M1": ("M1 Campañas básicas", "Poblaciones, plantillas, email/SMS, workflow lineal, control, reintentos, recuperación"),
    "M2": ("M2 Portal y CMS", "Portal del consumidor y CMS con renderizado personalizado"),
    "M3": ("M3 Campañas avanzadas", "Condiciones, triggered, MGM, coleccionables, push, inbox, pruebas"),
    "M4": ("M4 Medición", "Tracking de contenidos, analítica web, KPIs, BI"),
}

EPICS = {
    "E0": ("EPIC 0 · Plataforma base", "01-architecture.md", "§1 y requisitos no funcionales"),
    "E1": ("EPIC 1 · Gestión de campañas", "10-campaigns.md", "§2"),
    "E2": ("EPIC 2 · Gestión de contenidos (CMS)", "20-cms.md", "§3"),
    "E3": ("EPIC 3 · Interfaz web / portal", "30-web.md", "§4"),
}

LABELS = {
    "epic": "5319e7", "story": "0e8a16", "infra": "c5def5", "security": "d93f0b",
    "privacy": "b60205", "campaigns": "1d76db", "cms": "fbca04", "web": "0052cc",
    "tracking": "006b75", "modernized": "bfd4f2", "needs-decision": "e99695", "follow-up": "ededed",
}

# key, epic, milestone, title, req, labels, description, acceptance criteria, deps
I = []
def issue(key, epic, ms, title, req, labels, desc, ac, deps=()):
    I.append(dict(key=key, epic=epic, milestone=ms, title=title, req=req,
                  labels=labels, desc=desc, ac=ac, deps=list(deps)))

# ---------------- EPIC 0 · Plataforma base ----------------
issue("P01", "E0", "M0", "Esqueleto del monorepo Maven, aplicación y CI", "§1", ["infra"],
      "Crear el parent POM y los módulos vacíos definidos en el brief de arquitectura, la aplicación Spring Boot arrancable, docker compose para desarrollo y el workflow de GitHub Actions.",
      ["Módulos `crm-common, crm-consumer, crm-rules, crm-campaign, crm-messaging, crm-tracking, crm-cms, crm-web, crm-admin-api, crm-app` compilan con Java 21",
       "`docker compose up` levanta PostgreSQL 16, MailHog y un mock HTTP de pasarela SMS",
       "`crm-app` arranca, aplica Flyway y expone `/actuator/health` en UP",
       "GitHub Actions ejecuta `mvn -B verify` en cada PR",
       "springdoc-openapi expone `/v3/api-docs`"])
issue("P02", "E0", "M0", "Reglas de arquitectura y calidad (ArchUnit, Spotless, JaCoCo)", "§1 NFR-1", ["infra"],
      "Garantizar automáticamente la arquitectura hexagonal y los estándares de calidad.",
      ["Test ArchUnit: paquetes `..domain..` sin dependencias de Spring, JPA ni `..infrastructure..`",
       "Test ArchUnit: ningún módulo accede a repositorios/entidades JPA de otro módulo",
       "Spotless (google-java-format) falla el build si hay código sin formatear",
       "JaCoCo exige ≥ 80 % de líneas en paquetes `domain`"], ["P01"])
issue("P03", "E0", "M0", "Soporte multimarca: entidad Marca y contexto de tenant", "NFR-2", ["infra"],
      "Toda entidad de negocio pertenece a una Marca. El contexto de Marca se resuelve por petición y se aplica a todas las consultas.",
      ["Entidad `Brand` (id, código, nombre, dominio, idioma por defecto, estado) con migración",
       "`TenantContext` resuelto por dominio/prefijo de ruta (portal) o cabecera/selector (admin)",
       "Filtro de Marca aplicado en repositorios; test que demuestra que la Marca A no ve datos de la Marca B",
       "Datos semilla de desarrollo: `Marca A` y `Marca B`"], ["P01"])
issue("P04", "E0", "M0", "Usuarios internos, niveles de seguridad y roles", "§3.1", ["security"],
      "Usuarios internos (gestores, productores…) con nivel ADMIN / GLOBAL_USER / LOCAL_USER y roles funcionales.",
      ["Entidad usuario interno con contraseña Argon2/BCrypt, nivel y roles (`CRM_MANAGER, PRODUCER, CONTENT_REVIEWER, DEVELOPER`)",
       "LOCAL_USER solo opera sobre su Marca; GLOBAL_USER sobre todas; ADMIN además gestiona usuarios y Marcas",
       "Login de administración (Spring Security) y API para gestionar usuarios internos (solo ADMIN)",
       "Tests de autorización por nivel y rol"], ["P03"])
issue("P05", "E0", "M0", "Modelo de consumidor y preferencias", "§4.6", ["story"],
      "Perfil del consumidor por Marca con datos personales, idioma, segmento y preferencias clave-valor utilizables por las reglas.",
      ["Entidad `Consumer` (brandId, email, teléfono, idioma, fecha alta, estado ACTIVE/LOCKED/WITHDRAWN, atributos de perfil)",
       "Preferencias tipadas clave-valor con historial de cambios",
       "Email único por Marca",
       "Cambio de perfil/preferencias publica evento de dominio `ConsumerProfileChanged`",
       "API admin de consulta de consumidores paginada y filtrable"], ["P03"])
issue("P06", "E0", "M0", "Consentimientos opt-in/opt-out auditables", "§2.12", ["privacy", "modernized"],
      "Gestión de permisos por canal (email, SMS, MMS, push, suscripciones, programa CRM) con registro auditable.",
      ["Cada cambio registra canal, valor, fecha, origen (web, email, admin, importación) y actor",
       "Servicio `ConsentChecker.isAllowed(consumer, channel)` usado por mensajería",
       "Opt-out del programa CRM implica opt-out de todos los canales",
       "Tests de cada canal y del opt-out global"], ["P05"])
issue("P07", "E0", "M0", "Suscripciones a servicios del programa", "§2.11", ["story"],
      "Catálogo de servicios suscribibles por Marca (boletín, novedades, comunicaciones periódicas) y suscripciones del consumidor.",
      ["Entidades `SubscriptionService` y `Subscription` con alta/baja y fechas",
       "Suscribirse requiere opt-in de servicios de suscripción",
       "Las suscripciones son utilizables como criterio en reglas (`subscribedTo`)"], ["P06"])
issue("P08", "E0", "M0", "Motor de reglas de negocio: DSL y evaluación en memoria", "§2.1, §3.2", ["story"],
      "DSL JSON único para segmentación, condiciones de workflow, personalización de contenidos y menús.",
      ["Operadores: `and, or, not, eq, ne, in, gt, lt, between, exists, contains`; campos de perfil, preferencias, consentimientos, suscripciones y eventos recientes",
       "Validación de la regla con mensajes de error claros",
       "Evaluación en memoria contra un `RuleContext` del consumidor",
       "Tests de propiedad/tabla con ≥ 20 casos"], ["P05"])
issue("P09", "E0", "M0", "Traducción del DSL de reglas a SQL para segmentación", "§2.10", ["story"],
      "Las poblaciones grandes no pueden evaluarse en memoria: traducir la regla a una consulta parametrizada.",
      ["Traducción a SQL parametrizado (sin concatenar valores) para los operadores soportados",
       "Test de equivalencia: mismo resultado en memoria y en SQL sobre un conjunto semilla (Testcontainers)",
       "Reglas no traducibles se rechazan con error explícito"], ["P08"])
issue("P10", "E0", "M0", "Cifrado de datos personales y comunicaciones", "§2.16", ["security"],
      "Cifrado en reposo de PII y en tránsito.",
      ["Email y teléfono cifrados en columna con AES-GCM; clave por configuración externa, nunca en el repo",
       "Búsqueda por email mediante hash determinista (blind index)",
       "TLS obligatorio con proveedores externos (SMTP STARTTLS, HTTPS)",
       "ADR documentando gestión y rotación de claves"], ["P05"])
issue("P11", "E0", "M0", "Eventos de dominio y registro de eventos de usuario", "§2.2", ["tracking"],
      "Infraestructura de eventos interna y almacenamiento de eventos de usuario en `crm-tracking`.",
      ["Publicación de eventos de dominio (Spring events + outbox transaccional)",
       "Tipos de evento de usuario: login, logout, clic en servicio, cambio de experiencia, email enviado, email abierto, clic en email, cambio de preferencias",
       "Tabla de eventos consultable por consumidor, tipo y rango de fechas",
       "Los eventos están disponibles para reglas (`eventOccurred(type, within)`)"], ["P05"])
issue("P12", "E0", "M0", "Configuración por Marca", "NFR-4", ["infra"],
      "Parámetros de negocio configurables por Marca sin redespliegue.",
      ["Ajustes: remitente y asunto por tipo de notificación, idiomas, nº de reintentos por canal, umbral de bloqueo de login, proveedores activos",
       "API admin para leer/modificar ajustes (ADMIN o GLOBAL_USER)",
       "Valores por defecto razonables documentados"], ["P03"])

# ---------------- EPIC 1 · Campañas ----------------
issue("C01", "E1", "M1", "Poblaciones: criterio de segmentación e instancias", "§2.10", ["campaigns"],
      "Definir poblaciones reutilizables y materializar instancias inmutables.",
      ["CRUD de `Population` con criterio en DSL validado",
       "Materializar `PopulationInstance` (snapshot de consumer IDs y fecha) mediante SQL generado",
       "Contar tamaño estimado sin materializar",
       "Histórico: consultar qué instancias/campañas incluyeron a un consumidor"], ["P09"])
issue("C02", "E1", "M1", "Plantillas de mensaje (email texto, email HTML, SMS)", "§2.3", ["campaigns"],
      "Plantillas con variables de personalización y variantes por idioma.",
      ["Tipos EMAIL_TEXT, EMAIL_HTML, SMS con motor de plantillas (Thymeleaf texto/HTML)",
       "Variables del perfil y de la campaña; error si falta una variable obligatoria",
       "Variantes por idioma con fallback al idioma por defecto de la Marca",
       "Validación de longitud de SMS y segmentación",
       "Enlace de baja obligatorio en emails"], ["P05", "P12"])
issue("C03", "E1", "M1", "Canal email con varios servidores SMTP", "§2.9", ["campaigns"],
      "Adaptador de email con varios servidores en paralelo y estrategia de selección.",
      ["Varios servidores SMTP por Marca configurables",
       "Estrategias: manual, round-robin, menor carga",
       "Envío concurrente con límite configurable por servidor",
       "Test de integración con GreenMail (2 servidores)"], ["P12"])
issue("C04", "E1", "M1", "Canal SMS con varios proveedores HTTP", "§2.9", ["campaigns"],
      "Puerto `SmsGateway` y adaptador HTTP genérico configurable; varios proveedores con selección.",
      ["Adaptador HTTP configurable (URL, auth, mapeo de respuesta) para proveedores genéricos",
       "Selección de proveedor como en email",
       "Recepción de acuses de entrega (webhook) que actualizan el estado",
       "Tests con WireMock"], ["P12"])
issue("C05", "E1", "M1", "Outbox de comunicaciones con verificación de opt-in e idempotencia", "§2.12, §2.14", ["campaigns", "privacy"],
      "Toda comunicación se encola en un outbox persistente y se despacha por el canal tras verificar consentimiento.",
      ["Registro outbox con clave idempotente `(executionId, nodeId, consumerId, attemptGroup)` única",
       "El despachador verifica opt-in del canal justo antes de enviar; si no hay, estado `SKIPPED_NO_CONSENT`",
       "Despachador con ShedLock, seguro con varias instancias",
       "Test: un consumidor sin opt-in nunca recibe envío"], ["P06", "C03", "C04"])
issue("C06", "E1", "M1", "Reintentos configurables y estado de fallo", "§2.15", ["campaigns"],
      "Reintentos ante error del proveedor con backoff.",
      ["Nº de reintentos y backoff por Marca y canal",
       "Errores permanentes (destinatario inválido) no se reintentan",
       "Tras agotar reintentos, estado `FAILED` con motivo",
       "Métricas de reintentos y fallos"], ["C05"])
issue("C07", "E1", "M1", "Modelo de campaña y definición de workflow", "§2.1", ["campaigns"],
      "Entidad campaña y definición de workflow como grafo JSON/YAML versionado.",
      ["Campaña: población, canales, tipo, plantilla(s), workflow, estado",
       "Esquema JSON de workflow con nodos ACTION, WAIT, CONDITION, END y validación (sin ciclos no controlados, nodos alcanzables)",
       "Versionado: una ejecución queda ligada a la versión del workflow con que arrancó",
       "API admin CRUD de campañas"], ["C01", "C02"])
issue("C08", "E1", "M1", "Motor de workflow: ejecución de nodos ACTION, WAIT y END", "§2.1", ["campaigns"],
      "Motor que avanza a cada consumidor por el grafo.",
      ["Estado por consumidor y nodo persistido",
       "ACTION genera registros outbox; WAIT por duración; END cierra",
       "Avance por ticks planificados con lotes configurables",
       "Test de un workflow lineal enviar → esperar → enviar"], ["C07", "C05"])
issue("C09", "E1", "M1", "Control de ejecución: arrancar, pausar, reanudar, parar", "§2.13", ["campaigns"],
      "Control de ejecución en cualquier fase.",
      ["Transiciones `CREATED→RUNNING⇄PAUSED→STOPPED|COMPLETED`; transiciones inválidas rechazadas",
       "Pausa: no se generan ni despachan nuevos envíos; el estado de nodos persiste",
       "Parada: definitiva; los pendientes del outbox se cancelan",
       "API admin y tests de cada transición"], ["C08"])
issue("C10", "E1", "M1", "Recuperación ante caídas sin duplicados", "§2.14", ["campaigns"],
      "Tras reinicio, las campañas en curso continúan sin duplicar envíos.",
      ["Test de integración que interrumpe el despachador a mitad de lote y reinicia el contexto",
       "Todos los pendientes se envían exactamente una vez (verificado en GreenMail)",
       "Envíos en estado `SENDING` huérfanos se reconcilian con el proveedor o se reintentan de forma idempotente"], ["C08", "C06"])
issue("C11", "E1", "M1", "Tracking de envíos y archivo de comunicaciones", "§2.17", ["campaigns", "tracking"],
      "Guardar cada comunicación tal como la recibió el consumidor y su resultado.",
      ["Se guarda el contenido renderizado de cada envío",
       "Estados: QUEUED, SENT, DELIVERED, BOUNCED/REJECTED, FAILED, SKIPPED_NO_CONSENT",
       "API para ver el historial de comunicaciones de un consumidor y de una campaña",
       "Previsualización de un mensaje para un consumidor antes de enviar"], ["C05"])
issue("C12", "E1", "M1", "Campañas de difusión (broadcast)", "§2.4", ["campaigns"],
      "Envío de novedades a toda la comunidad o a un segmento con un workflow de un solo paso.",
      ["Plantilla de workflow `broadcast` lista para usar",
       "Envío respetando opt-ins y con throttling configurable",
       "Test de extremo a extremo con población de 1.000 consumidores semilla"], ["C09", "C11"])
issue("C13", "E1", "M1", "Exportaciones por plantilla de fichero y consulta SQL", "§2.3", ["campaigns"],
      "Generar listas para canales externos (p. ej. correo postal).",
      ["Tipo FILE_EXPORT: genera CSV con columnas configurables a partir de una instancia",
       "Tipo SQL_EXPORT: plantilla de consulta parametrizada de solo lectura, ejecutada con usuario de BD sin permisos de escritura",
       "Ficheros descargables vía API admin con auditoría"], ["C01"])
issue("C14", "E1", "M3", "Tracking de apertura de email", "§2.17", ["campaigns", "tracking"],
      "Detectar aperturas mediante píxel de seguimiento.",
      ["Píxel único por envío; endpoint que registra `EMAIL_OPENED` una vez por envío (ignora repeticiones para el estado, cuenta totales)",
       "Respeta preferencia de no seguimiento si el consumidor la tiene",
       "Evento disponible para condiciones de workflow"], ["C11", "P11"])
issue("C15", "E1", "M3", "Tracking de clics con códigos por zona", "§2.17", ["campaigns", "tracking"],
      "Reescritura de enlaces con código de respuesta distinto por zona clicable.",
      ["Reescritura de enlaces en emails HTML con token firmado y código de zona",
       "Endpoint de redirección que registra `EMAIL_CLICKED{zone}` y redirige solo a destinos permitidos (sin open redirect)",
       "Evento disponible para condiciones de workflow"], ["C11", "P11"])
issue("C16", "E1", "M3", "Condiciones y esperas por evento en el workflow; workflow de referencia", "§2.1", ["campaigns"],
      "Nodos CONDITION sobre eventos y WAIT con timeout, con el ejemplo de invitación implementado.",
      ["CONDITION evalúa reglas sobre eventos del consumidor (abrió, clic en zona X…)",
       "WAIT-hasta-evento con timeout y rama alternativa",
       "Workflow de referencia de invitación (ver brief 10) como plantilla y test de integración que recorre las tres ramas"], ["C14", "C15", "C08"])
issue("C17", "E1", "M3", "Campañas disparadas por eventos (triggered)", "§2.6", ["campaigns"],
      "Entrada de consumidores en un workflow al producirse un evento.",
      ["Disparador configurable: tipo de evento + regla adicional",
       "Ejemplo: consumidores que cambian su perfil reciben un mensaje en < 1 min",
       "Protección contra re-entrada configurable (una vez, una vez por periodo, siempre)"], ["C16"])
issue("C18", "E1", "M3", "Generación de códigos únicos de respuesta y canje", "§2.2", ["campaigns"],
      "Acción de workflow que genera códigos únicos por consumidor.",
      ["Códigos no adivinables, únicos por campaña, con caducidad opcional",
       "Variable disponible en plantillas",
       "API de validación/canje de código idempotente"], ["C08"])
issue("C19", "E1", "M3", "Campañas Member get Member (MGM)", "§2.5", ["campaigns", "privacy"],
      "Un consumidor recomienda a un contacto; se envía la invitación y se espera respuesta.",
      ["Formulario/endpoint de recomendación con límite por consumidor y antispam",
       "El invitado recibe una única comunicación transaccional con enlace de registro trazable; sus datos no se usan para otras campañas sin opt-in propio",
       "Si el invitado se registra, el emisor avanza en su workflow (p. ej. recompensa)",
       "Test del ciclo completo"], ["C16", "C18"])
issue("C20", "E1", "M3", "Campañas de coleccionables (puntos/códigos)", "§2.7", ["campaigns"],
      "Acumulación y uso de puntos o códigos dentro del sitio.",
      ["Saldo de puntos por consumidor con libro de movimientos (alta, canje, caducidad)",
       "Acciones de workflow: otorgar y canjear puntos; condición sobre saldo",
       "Endpoint en el portal para ver saldo y canjear",
       "Operaciones concurrentes consistentes (test)"], ["C18"])
issue("C21", "E1", "M3", "Canal de notificaciones push", "§2.2, §2.3", ["campaigns", "modernized"],
      "Sustituye a WAP push del documento original: notificaciones Web Push.",
      ["Registro de suscripciones Web Push (VAPID) desde el portal con opt-in de canal push",
       "Plantillas PUSH y adaptador de envío",
       "Integrado con outbox, reintentos y tracking"], ["C05", "W01"])
issue("C22", "E1", "M3", "Buzón del portal y banners/modales", "§2.2", ["campaigns", "web"],
      "Acciones que muestran mensajes dentro del portal.",
      ["Mensajes de inbox por consumidor con leído/no leído",
       "Banners/modales dirigidos por campaña mostrados en la siguiente visita",
       "Eventos de visualización y clic registrados"], ["C08", "W01"])
issue("C23", "E1", "M3", "Pruebas de campaña (dry-run)", "§2.8", ["campaigns"],
      "Analizar resultados esperados antes del lanzamiento.",
      ["Dry-run sobre una instancia: nº de consumidores por rama y canal, excluidos por falta de consentimiento",
       "Envío de prueba a una lista de destinatarios internos",
       "Ningún dry-run genera envíos reales ni eventos de tracking"], ["C16"])
issue("C24", "E1", "M3", "Catálogo de plantillas de workflow reutilizables", "§2.1", ["campaigns"],
      "Las lógicas de campaña se definen a partir de plantillas parametrizables.",
      ["Catálogo con al menos: broadcast, invitación a evento, bienvenida, reactivación, MGM",
       "Parámetros declarados (plantillas de mensaje, esperas, canales) y validación al instanciar",
       "Crear campaña desde plantilla sin escribir JSON a mano"], ["C16", "C19"])

# ---------------- EPIC 2 · CMS ----------------
issue("S01", "E2", "M2", "Modelo de contenido, tipos y assets", "§3.3", ["cms", "modernized"],
      "Contenidos y ficheros multimedia por Marca.",
      ["Tipos: imagen (JPG/PNG/GIF/WebP/SVG), PDF, ofimática, vídeo (MP4/WebM), ZIP, texto, HTML/XML/Markdown; extensible",
       "Validación de tipo MIME real y tamaño máximo; SVG/HTML saneados",
       "Puerto `AssetStorage` con implementación en filesystem",
       "API admin de subida, metadatos y etiquetas"], ["P04"])
issue("S02", "E2", "M2", "Versionado de contenidos", "§3", ["cms"],
      "Historial de versiones con restauración.",
      ["Cada edición crea versión nueva; consulta del historial y diff de metadatos",
       "Restaurar una versión anterior crea una nueva versión",
       "Solo una versión publicada a la vez"], ["S01"])
issue("S03", "E2", "M2", "Workflow editorial con notificaciones", "§3", ["cms"],
      "Productor crea → Revisor revisa → aprobador publica.",
      ["Estados DRAFT, IN_REVIEW, APPROVED, PUBLISHED, ARCHIVED con transiciones por rol",
       "Notificación por email a los implicados en cada transición",
       "Comentarios de revisión",
       "Solo PUBLISHED se sirve en el portal"], ["S02"])
issue("S04", "E2", "M2", "Permisos por categoría y tipo de contenido", "§3.1", ["cms", "security"],
      "Asignar responsabilidades por categoría/tipo.",
      ["Categorías de contenido por Marca",
       "Permisos (ver, editar, revisar, publicar) por rol y categoría",
       "Tests de autorización"], ["S03"])
issue("S05", "E2", "M2", "Plantillas de página y catálogo de componentes", "§3.5", ["cms", "modernized"],
      "Las páginas se construyen con plantillas compuestas por slots y componentes.",
      ["Plantilla: cabecera, pie, menú y slots con tipos de componente permitidos",
       "Componentes base: miniatura, imagen, vídeo corto, vídeo, noticia breve (±imagen, ±vídeo), noticia (±imagen, ±vídeo), banner/modal, cabecera multimedia, pie multimedia, hero (HTML5, sin Flash)",
       "Fragmentos Thymeleaf por componente; añadir uno nuevo no requiere tocar el motor",
       "Gestión de plantillas restringida al rol DEVELOPER"], ["S01"])
issue("S06", "E2", "M2", "Renderizado personalizado según reglas", "§3.2, §3.5", ["cms"],
      "Composición en tiempo real de la página según perfil y preferencias.",
      ["Cada slot elige contenido, posición y variante visual por reglas del DSL",
       "Contenido por defecto cuando ninguna regla aplica",
       "Contenidos con regla de audiencia no se muestran a quien no la cumple",
       "Caché de fragmentos no personalizados; p95 de renderizado < 200 ms con datos semilla"], ["S05", "P08"])
issue("S07", "E2", "M2", "Menú de navegación dinámico", "§3.4", ["cms"],
      "Menús con visibilidad gobernada por reglas.",
      ["Ítems jerárquicos con enlace, orden y regla de visibilidad",
       "Ítems restringidos ocultos para anónimos",
       "Tests con perfiles distintos"], ["S06"])
issue("S08", "E2", "M2", "Contenidos multiidioma", "§3.8", ["cms"],
      "Variantes de contenido por idioma.",
      ["Idiomas habilitados por Marca",
       "Variante por idioma con fallback al idioma por defecto",
       "Selección por preferencia del consumidor o cabecera `Accept-Language` para anónimos"], ["S01"])
issue("S09", "E2", "M2", "Previsualización con perfil simulado", "§3.7", ["cms"],
      "Ver una página como la vería un perfil concreto antes de publicar.",
      ["Endpoint de preview con perfil simulado editable y fecha simulada",
       "Incluye contenidos en DRAFT/APPROVED marcados visualmente",
       "No registra tracking ni eventos"], ["S06"])
issue("S10", "E2", "M4", "Tracking de acceso y acciones sobre contenidos", "§3.6", ["cms", "tracking"],
      "Registrar contenidos vistos y acciones realizadas.",
      ["Evento por contenido mostrado y por acción (clic, descarga, reproducción)",
       "Agregados por contenido, Marca y periodo",
       "Eventos utilizables en reglas"], ["S06", "P11"])

# ---------------- EPIC 3 · Portal web ----------------
issue("W01", "E3", "M2", "Portal por Marca: layout, navegación anónima y registrada", "§4.1", ["web"],
      "Portal SSR que compone sus páginas con el CMS.",
      ["Resolución de Marca por dominio/prefijo",
       "Páginas públicas para anónimos; contenido restringido redirige a login",
       "Layout base con menú dinámico y páginas desde plantillas del CMS"], ["S06", "S07"])
issue("W02", "E3", "M2", "Registro con verificación de email y notificación configurable", "§4.2", ["web", "modernized"],
      "Alta de consumidores en el portal.",
      ["Formulario con validación cliente/servidor y recogida de consentimientos explícitos",
       "Email de verificación con enlace de un solo uso; cuenta activa tras verificar",
       "Remitente, asunto y plantilla HTML configurables por Marca",
       "Nunca se envía la contraseña por email"], ["W01", "P06", "C02"])
issue("W03", "E3", "M2", "Login y bloqueo por intentos fallidos", "§4.8", ["web", "security"],
      "Autenticación del consumidor con protección contra fuerza bruta.",
      ["Login con sesión segura; evento LOGIN/LOGOUT publicado",
       "Bloqueo tras N intentos (configurable por Marca), estado LOCKED",
       "Email con enlace de desbloqueo de un solo uso",
       "Rate limiting por IP"], ["W02"])
issue("W04", "E3", "M2", "Recuperación de contraseña", "§4.3", ["web", "security", "modernized"],
      "Restablecimiento seguro.",
      ["Solicitud por email o usuario; respuesta idéntica exista o no la cuenta",
       "Token de un solo uso con caducidad configurable",
       "Invalida sesiones abiertas tras el cambio"], ["W02"])
issue("W05", "E3", "M2", "Preferencias, suscripciones y consentimientos del consumidor", "§4.5, §4.6", ["web", "privacy"],
      "Área personal para gestionar datos y permisos.",
      ["Edición de datos personales, idioma y preferencias",
       "Alta/baja de suscripciones y opt-in/out por canal (auditado)",
       "Cambios disponibles inmediatamente para reglas y campañas"], ["W03", "P07"])
issue("W06", "E3", "M2", "Baja del programa y supresión de datos", "§4.4", ["web", "privacy", "modernized"],
      "Baja desde el portal y desde el enlace de cada comunicación.",
      ["Baja desde el menú (autenticado) y desde enlace firmado en emails (sin login)",
       "Opt-out global inmediato y cancelación de envíos pendientes",
       "Anonimización de PII conservando agregados estadísticos; política documentada en ADR",
       "Etiquetar `needs-decision` si falta validación legal del alcance de la supresión"], ["W05", "C05"])
issue("W07", "E3", "M3", "Verificación de elegibilidad configurable por Marca", "§4.2", ["web", "campaigns"],
      "Paso adicional de registro exigido por algunas Marcas, implementado como campaña interactiva.",
      ["Activable por Marca; mientras no se complete, el consumidor queda `PENDING_VERIFICATION` sin acceso a contenido restringido ni comunicaciones comerciales",
       "Puerto `EligibilityVerifier` con implementación de ejemplo (declaración + revisión manual)",
       "Al completarse, acceso concedido y notificación en un único mensaje"], ["W02", "C16"])
issue("W08", "E3", "M2", "Publicación de eventos de usuario del portal", "§2.2", ["web", "tracking"],
      "El portal alimenta el registro de eventos.",
      ["Eventos de login, logout, clic en servicio, cambio de experiencia y de preferencias",
       "Asíncrono: no afecta a la latencia de la página",
       "Test que verifica cada evento"], ["W03", "P11"])
issue("W09", "E3", "M2", "Accesibilidad, responsive, compatibilidad y seguridad web", "§4.7", ["web", "security", "modernized"],
      "Requisitos de calidad del portal.",
      ["Responsive; probado en las dos últimas versiones de Chrome, Firefox, Safari y Edge",
       "WCAG 2.1 AA: test automático con axe sobre páginas principales sin violaciones serias",
       "CSRF, CSP, HSTS y demás cabeceras de seguridad",
       "Validación dinámica de formularios en cliente coherente con la del servidor"], ["W02"])
issue("W10", "E3", "M4", "Analítica web con consentimiento de cookies", "§4.9", ["web", "tracking", "modernized"],
      "Integración configurable con una herramienta de analítica web.",
      ["Banner de consentimiento de cookies por categorías",
       "Snippet de analítica configurable por Marca, cargado solo con consentimiento",
       "Sin datos personales en la URL ni en eventos de analítica"], ["W01"])

# ---------------- Medición transversal ----------------
issue("K01", "E1", "M4", "KPIs de campañas y API de informes", "NFR-7", ["tracking"],
      "Marco de medición de rendimiento.",
      ["Por campaña y nodo: enviados, entregados, fallidos, aperturas, clics (únicos/totales), conversiones",
       "API admin con filtros por Marca, campaña y periodo",
       "Consultas sobre vistas/agregados precalculados"], ["C16", "S10"])
issue("K02", "E0", "M4", "Exportación a herramienta de BI", "NFR-3", ["infra"],
      "Adaptar la plataforma a la herramienta de reporting existente.",
      ["Vistas SQL documentadas de solo lectura para BI (sin PII en claro)",
       "Exportación programada CSV/Parquet configurable",
       "Documentación del modelo de datos analítico"], ["K01"])


def body(i):
    ep_title, brief, _ = EPICS[i["epic"]]
    lines = [
        f"**Epic:** {ep_title}  ",
        f"**Requisito:** [`docs/requirements.md` {i['req']}](../blob/main/docs/requirements.md)  ",
        f"**Brief:** [`docs/briefs/{brief}`](../blob/main/docs/briefs/{brief})",
        "", "## Descripción", i["desc"], "", "## Criterios de aceptación",
        *[f"- [ ] {a}" for a in i["ac"]], "",
    ]
    if i["deps"]:
        lines += ["## Dependencias", " ".join(f"Depende de: {{{{{d}}}}}" for d in i["deps"]), ""]
    lines += ["## Definition of Done", "Ver `CLAUDE.md`.", "", f"<!-- key:{i['key']} -->"]
    return "\n".join(lines)


def epic_body(ek):
    title, brief, req = EPICS[ek]
    children = [i for i in I if i["epic"] == ek]
    lines = [f"Brief: [`docs/briefs/{brief}`](../blob/main/docs/briefs/{brief}) · Requisitos: {req}", "",
             "## Issues", *[f"- [ ] {{{{{c['key']}}}}} {c['title']} ({c['milestone']})" for c in children],
             "", f"<!-- key:{ek} -->"]
    return "\n".join(lines)


def main():
    out = {
        "labels": LABELS,
        "milestones": [{"key": k, "title": t, "description": d} for k, (t, d) in MILESTONES.items()],
        "epics": [{"key": k, "title": v[0], "labels": ["epic"], "body": epic_body(k)} for k, v in EPICS.items()],
        "issues": [{"key": i["key"], "title": i["title"], "milestone": i["milestone"],
                    "labels": ["story", *i["labels"]] if "story" not in i["labels"] else i["labels"],
                    "deps": i["deps"], "body": body(i)} for i in I],
    }
    (ROOT / "planning/issues.json").write_text(json.dumps(out, ensure_ascii=False, indent=2))

    md = ["# Backlog", "", "Generado desde `planning/backlog.py`. La fuente de verdad del estado son los GitHub Issues.", ""]
    for mk, (mt, md_desc) in MILESTONES.items():
        md += [f"## {mt}", f"_{md_desc}_", "", "| Clave | Epic | Issue | Req. | Depende de |", "|---|---|---|---|---|"]
        for i in I:
            if i["milestone"] == mk:
                md.append(f"| {i['key']} | {i['epic']} | {i['title']} | {i['req']} | {', '.join(i['deps']) or '—'} |")
        md.append("")
    (ROOT / "docs/backlog.md").write_text("\n".join(md))

    keys = {i["key"] for i in I}
    missing = {d for i in I for d in i["deps"] if d not in keys}
    assert not missing, f"Dependencias inexistentes: {missing}"
    order = {k: n for n, k in enumerate(MILESTONES)}
    by_key = {i["key"]: i for i in I}
    bad = [(i["key"], d) for i in I for d in i["deps"] if order[by_key[d]["milestone"]] > order[i["milestone"]]]
    assert not bad, f"Dependencias hacia milestones posteriores: {bad}"
    print(f"{len(EPICS)} epics, {len(I)} issues, {len(MILESTONES)} milestones")


if __name__ == "__main__":
    main()
