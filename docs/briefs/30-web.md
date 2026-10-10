# Brief 30 — Interfaz web / portal (EPIC 3)

Referencia funcional: [requirements.md §4](../requirements.md#4-interfaz-web)

## Alcance
Portal SSR (Thymeleaf + HTMX) por Marca (resuelta por dominio o prefijo de ruta), que compone sus páginas con el CMS.

## Funciones
- Navegación anónima (contenido no restringido) y registrada (contenido por perfil).
- Registro con verificación de email por enlace; paso opcional de **verificación de elegibilidad** configurable por Marca, implementado como campaña interactiva.
- Notificaciones de registro con remitente, asunto y plantilla HTML configurables. Nunca se envían contraseñas.
- Login; bloqueo tras N intentos fallidos (configurable) y flujo de desbloqueo por email.
- Recuperación de contraseña con token de un solo uso y caducidad.
- Área de preferencias: datos personales, idioma, suscripciones y consentimientos por canal.
- Baja del programa desde el menú y desde el enlace incluido en cada comunicación; anonimización de datos personales.
- Publicación de eventos de usuario (login, logout, clic en servicio, cambio de preferencias) hacia `crm-tracking`.
- Analítica web configurable (snippet por Marca) cargada solo con consentimiento de cookies.

## Requisitos de calidad
- Validación en cliente y servidor.
- Responsive; dos últimas versiones de Chrome, Firefox, Safari y Edge.
- WCAG 2.1 AA (comprobado con axe en tests de UI cuando sea viable).
- Protección CSRF, cabeceras de seguridad, rate limiting en login y recuperación.
