# Configuración por Marca

Parámetros de negocio de cada Marca, modificables en caliente (sin redespliegue) con la API `GET/PUT /api/brand-settings` (cabecera `X-Brand`; solo ADMIN o GLOBAL_USER). Se guardan en `brand_settings` (JSON validado por la aplicación) y se leen en cada uso. Una Marca sin ajustes guardados usa los valores por defecto (`GET /api/brand-settings/defaults`).

| Ajuste | Valor por defecto | Validación |
|---|---|---|
| `notifications.<TIPO>.senderName` | nombre de la Marca | obligatorio, ≤ 120 |
| `notifications.<TIPO>.senderAddress` | `no-reply@<dominio de la Marca>` (`no-reply@example.invalid` si no tiene dominio) | email válido |
| `notifications.<TIPO>.subject` | `REGISTRATION_VERIFICATION`: "Confirma tu registro"; `PASSWORD_RESET`: "Restablece tu contraseña"; `ACCOUNT_LOCKED`: "Tu cuenta se ha bloqueado"; `UNSUBSCRIBE_CONFIRMATION`: "Confirmación de baja" | obligatorio, ≤ 200 |
| `languages` | `[<idioma por defecto de la Marca>]` | 1–20 idiomas BCP-47 sin repetir; debe incluir el idioma por defecto de la Marca; el primero es el idioma por defecto del programa |
| `maxRetries.<CANAL>` | EMAIL 3, SMS 3, MMS 3, PUSH 2, INBOX 0 | 0–10 |
| `loginLockThreshold` | 5 intentos fallidos | 1–20 |
| `activeProviders.<CANAL>` | `[]` (ninguno) | códigos `[a-z0-9_-]` únicos, ≤ 10 por canal, en orden de preferencia |

Canales: `EMAIL, SMS, MMS, PUSH, INBOX`. Tipos de notificación: `REGISTRATION_VERIFICATION, PASSWORD_RESET, ACCOUNT_LOCKED, UNSUBSCRIBE_CONFIRMATION`.

La plantilla HTML de cada notificación (§4.2) pertenece a las plantillas de mensaje (C02); aquí solo se configuran remitente y asunto.
