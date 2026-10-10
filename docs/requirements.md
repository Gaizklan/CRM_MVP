# Plataforma CRM Web — Requisitos funcionales

> Versión genérica y modernizada de un documento de requisitos de negocio.
> Toda referencia a cliente, marca, sector, país, autor u organización ha sido sustituida por términos neutros.
> Los requisitos tecnológicos obsoletos se han modernizado; cada caso se indica con **[Modernizado]**.

## Glosario

| Término | Significado |
|---|---|
| **Cliente** | Organización que opera la plataforma (tenant). |
| **Marca** | Línea de producto del Cliente que ejecuta programas CRM. La plataforma es multimarca. |
| **Programa CRM** | Conjunto de campañas, contenidos y reglas de una Marca orientado a una relación con consumidores. |
| **Consumidor** | Usuario final que recibe comunicaciones y navega por el sitio. |
| **Gestor CRM / Producción** | Usuarios internos que diseñan campañas y contenidos. |
| **Regla de negocio** | Condición evaluable sobre el perfil, preferencias o comportamiento del consumidor. |

## 1. Introducción

La plataforma da soporte a actividades CRM basadas en web: gestión de campañas de comunicación, gestión y publicación de contenidos, y reglas de negocio que dirigen la relación con los consumidores.

### Componentes

1. **Sistema de gestión de campañas (CMP-CAMP)** — construye y envía comunicaciones directas a consumidores y gestiona sus respuestas para medir rendimiento.
2. **Sistema de gestión de contenidos (CMP-CMS)** — permite crear, editar, probar, personalizar (según reglas de negocio) y publicar contenidos en los canales activos (web, SMS, MMS, email, push).
3. **Interfaz web (CMP-WEB)** — canal principal: interacción con consumidores y funciones de administración para gestores CRM.
4. **Base de datos CRM** — información de consumidores, repositorio de contenidos, reglas de negocio, histórico de campañas.

### Requisitos no funcionales generales

- NFR-1 Reutilización de desarrollos para futuros programas.
- NFR-2 Soporte multimarca: una instancia sirve programas de varias Marcas, con aislamiento de datos y configuración.
- NFR-3 Adaptable a la infraestructura existente: base de datos de consumidores externa y herramienta de reporting/BI.
- NFR-4 Flexible ante cambios de mercado (configuración antes que código).
- NFR-5 Extensible hacia esquemas de oferta más sofisticados.
- NFR-6 Reducir el time-to-market de nuevas campañas.
- NFR-7 Marco de medición de rendimiento (KPIs).

## 2. Sistema de gestión de campañas

Software CRM orientado a marketing que combina CRM operacional (comportamiento) y analítico (KPIs, planes) para campañas inbound y outbound:

- Seleccionar destinatarios según criterios de segmentación.
- Asignar ofertas específicas a los destinatarios.
- Enviar automáticamente por los canales seleccionados (directo, o generando listas para canales externos).
- Recoger, almacenar y analizar resultados (respuestas, propensiones).

La plataforma debe permitir adaptar estas funciones a las particularidades de cada Cliente y Marca; los servicios nuevos quedan disponibles para su reutilización.

### 2.1 Definición y workflow de campaña
Una campaña = **población objetivo** + **mensaje y contenidos referenciados** + **canales** + **workflow** (eventos, acciones y resultados que expresan las reglas de negocio). Las lógicas se definen mediante plantillas de workflow reutilizables.

Ejemplo ilustrativo:
- Población: consumidores del segmento objetivo *S1* registrados en la *Marca A*.
- Mensaje: invitación a un evento. Canales: SMS y email (según opt-ins).
- Contenido: enlace a la sección web de confirmación.
- Workflow: (1) enviar invitación por los canales con opt-in; (2) si abre → si hace clic, confirma asistencia y recibe un mensaje con número de reserva; si no hace clic, recibe un mensaje de refuerzo; (3) si no abre en 2 días, se reintenta el mismo mensaje.

### 2.2 Eventos y acciones predefinidos
- **Eventos**: login, logout, clic en servicio, cambio de experiencia de usuario, email enviado, email abierto, clic en contenido del email, cambio de interfaz/preferencias.
- **Acciones**: enviar email, enviar SMS, notificación push **[Modernizado: sustituye a WAP push]**, mensaje en buzón del portal, banner/modal web, generación de código único (respuesta/canje).

### 2.3 Tipos de mensaje soportados
Plantilla de fichero, plantilla de consulta SQL (extracción de listas), email texto, email HTML, SMS, notificación push **[Modernizado]**, mensaje en buzón del portal.

### 2.4 Campañas de difusión (broadcast)
Comunicación de novedades y actividades a toda la comunidad o a un segmento.

### 2.5 Campañas interactivas — Member get Member (MGM)
El sistema recoge una acción de un consumidor (recomendación), enruta una campaña a un potencial consumidor y espera su respuesta. Los detalles se adaptan mediante la definición de actividades.

### 2.6 Campañas disparadas por eventos (triggered)
Captura de eventos de usuario en tiempo real; el workflow se ejecuta según los eventos ocurridos en el grupo seleccionado (p. ej., solo para quienes cambiaron su perfil). Distintos usuarios pueden seguir workflows distintos según reglas de negocio.

### 2.7 Campañas de coleccionables
Los consumidores acumulan puntos/códigos y los usan dentro del sitio. Workflows específicos concretan el servicio genérico.

### 2.8 Pruebas de campaña
Antes del lanzamiento, el equipo de marketing analiza los resultados esperados, ajusta la definición y ejecuta.

### 2.9 Integración con otros sistemas
- **Servidores SMTP / proveedores de email**: varios simultáneamente para acelerar envíos.
- **Pasarelas SMS/MMS**: varios proveedores (normalmente vía HTTP/REST).
- **Base de datos CRM**: selección de poblaciones y registro de toda la información de campaña.
- **Selección de proveedor**: cuando hay alternativas, se elige manualmente o el sistema elige la más adecuada (p. ej., por carga).

### 2.10 Listas de población
Cada población guarda:
- **Criterio de segmentación**: filtros y condiciones aplicadas.
- **Instancia**: la lista materializada en un momento dado.

Beneficios: reutilizar definiciones creando nuevas instancias; pre-testear sin ejecutar; trazar históricamente qué campañas recibió cada consumidor.

### 2.11 Gestión de suscripciones
Suscripción de consumidores a servicios del programa (comunicaciones periódicas, novedades, noticias…).

### 2.12 Gestión de opt-in/opt-out
Permisos por canal: email, SMS, MMS, push **[Modernizado]**, servicios de suscripción y programa CRM. Antes de cualquier acción se verifica el opt-in del canal correspondiente. **[Modernizado]**: registro de consentimiento auditable conforme a normativa de protección de datos (RGPD).

### 2.13 Control de ejecución
- **Arranque**: se ejecutan acciones sobre los elementos de cada nodo.
- **Pausa**: se detiene temporalmente persistiendo el estado de nodos para reanudar.
- **Parada**: detención definitiva, no recuperable.

### 2.14 Recuperación ante caídas
Tras una caída, las campañas en curso continúan; se envían las comunicaciones pendientes **sin duplicados**.

### 2.15 Reintentos de comunicación
Número configurable de reintentos ante error del proveedor; tras agotarlos, estado *fallido*. Resultados almacenados para evaluación.

### 2.16 Cifrado
Cifrado de datos (en reposo) y de comunicaciones (en tránsito).

### 2.17 Tracking de comunicaciones
Cada comunicación enviada se guarda para visualizarla como la vio el consumidor (y previsualizar antes de enviar), con su resultado (entregado, rechazado…). Para email además:
- **Apertura**: detectable y utilizable como condición de transición de workflow.
- **Clic**: traza de cada clic, con códigos de respuesta distintos por zona clicable; utilizable como condición de transición.

## 3. Sistema de gestión de contenidos (CMS)

Permite construir, editar, gestionar, versionar y publicar contenidos (ficheros, imágenes, audio, vídeo, documentos, contenido web) de forma organizada. Debe ofrecer:
- Acceso privado con roles y responsabilidades por categoría/tipo de contenido, y workflow editorial colaborativo con notificaciones (p. ej., creador envía → revisor revisa → responsable aprueba → publicado).
- Personalización según criterios de segmentación y preferencias (colores, tipografía, énfasis según perfil).

### 3.1 Niveles de seguridad y roles
- Niveles: Administrador, Usuario global (todas las Marcas), Usuario local (una Marca), niveles adicionales configurables.
- Roles editoriales: Revisor de contenidos, Productor, Desarrollador.

### 3.2 Apariencia personalizada
La visualización se adapta a preferencias y estrategia CRM (reglas de negocio); la sofisticación crece de forma incremental con nuevas reglas y plantillas.

### 3.3 Tipos de contenido soportados
Imágenes (JPG, PNG, GIF, WebP, SVG), PDF, documentos ofimáticos, vídeo (MP4/WebM), comprimidos (ZIP), texto, texto enriquecido (HTML, XML, Markdown). Extensible. **[Modernizado: se elimina Flash; formatos web actuales]**

### 3.4 Menú de navegación dinámico
Menús dependientes del perfil y preferencias, gobernados por reglas de negocio.

### 3.5 Contenidos mediante plantillas
Las páginas se construyen con plantillas (estructura, posiciones, tipos de contenido). Componentes base: miniatura, imagen, vídeo corto, vídeo, noticia breve (sola / con imagen / con vídeo), noticia (sola / con imagen / con vídeo), banner/modal promocional, cabecera multimedia, pie multimedia, hero a pantalla completa **[Modernizado: HTML5 en lugar de Flash]**, componentes adicionales configurables.
Al solicitar una página, se resuelve su plantilla y se compone en tiempo real aplicando reglas de negocio (posición, apariencia, contenidos) según el perfil.

### 3.6 Tracking de contenidos
Traza de contenidos accedidos y acciones realizadas, almacenada en el CRM operacional para análisis posterior.

### 3.7 Previsualización y pruebas
Previsualizar contenidos generados por plantilla simulando distintos perfiles antes de pasar a producción.

### 3.8 Multiidioma
Contenidos en varios idiomas, configurables.

## 4. Interfaz web

### 4.1 Navegación anónima / registrada
- Anónima: todo el contenido no restringido.
- Registrada: además, contenido ligado al perfil y ofertas personales.

### 4.2 Registro de acceso
- Formulario de login.
- Formulario de registro. Opcionalmente, un **proceso de verificación de elegibilidad** configurable por Marca (p. ej., verificación de edad u otro requisito legal) lanzado como campaña interactiva; al completarse, se concede acceso y se notifica en un único mensaje.
- Notificación automática por email al registrarse, con remitente, asunto y plantilla HTML configurables. **[Modernizado: no se envía la contraseña; se envía enlace de verificación]**

### 4.3 Recuperación de credenciales
Recuperación por email y/o nombre de usuario. **[Modernizado: enlace de restablecimiento de un solo uso con caducidad, nunca la contraseña]**. Decidir si se permiten emails repetidos (por defecto: email único por Marca).

### 4.4 Solicitud de baja
Baja del programa por todos los canales: menú web y enlace en comunicaciones. **[Modernizado]**: borrado o anonimización conforme a RGPD (derecho de supresión), con criterio validado por el área legal.

### 4.5 Suscripciones y opt-in/opt-out
El consumidor gestiona suscripciones y consentimientos desde sus preferencias.

### 4.6 Preferencias personales
El usuario registrado modifica datos y preferencias; se almacenan y quedan disponibles para las reglas de negocio.

### 4.7 Validación de formularios y compatibilidad
Validación dinámica en cliente y servidor. **[Modernizado]**: compatibilidad con las dos últimas versiones de Chrome, Firefox, Safari y Edge, diseño responsive para móvil y accesibilidad WCAG 2.1 AA.

### 4.8 Intentos de login
Bloqueo automático tras N intentos fallidos (configurable) y campaña interactiva para el desbloqueo.

### 4.9 Analítica web
Además del tracking propio, registro de rutas de navegación (entradas, secuencias, páginas más vistas) mediante integración con una herramienta de analítica web configurable, respetando el consentimiento de cookies. **[Modernizado]**
