# ADR 0002 — Cifrado de datos personales y gestión de claves

- Estado: aceptado
- Fecha: 2026-10-10
- Issue: #14 (P10)

## Contexto
§2.16 y el brief de arquitectura exigen cifrado en reposo de PII (email, teléfono) con clave externa (AES-GCM) y TLS en tránsito. El email debe seguir siendo buscable y único por Marca.

## Decisión

### Cifrado en reposo
- **AES-256-GCM** (IV aleatorio de 96 bits, etiqueta de 128 bits) a nivel de aplicación para `consumer.email` y `consumer.phone` (`PiiCipher` en `crm-common`, `AesGcmPiiCipher` en infraestructura).
- Formato almacenado: `<idClave>:<base64(iv‖cifrado‖etiqueta)>`. El identificador de clave se autentica como AAD.
- **Índice ciego** del email: HMAC-SHA256 del email normalizado con una **clave distinta** (`email_hash`). Permite búsqueda exacta y `UNIQUE (brand_id, email_hash)` sin descifrar. El teléfono no tiene índice ciego (no se busca por teléfono); en segmentación SQL solo admite `exists`.
- Los datos existentes se cifran con una migración Flyway Java (`V8__EncryptConsumerPii`) entre dos migraciones SQL (V7 añade columnas, V9 elimina las columnas en claro).

### Gestión de claves
- Las claves **nunca** están en el repositorio ni en ficheros de configuración versionados. Se inyectan por entorno o gestor de secretos (Vault, KMS, secretos del orquestador):

  | Variable | Contenido |
  |---|---|
  | `CRM_PII_ENCRYPTION_KEY_ID` | id de la clave activa (p. ej. `k1`) |
  | `CRM_PII_ENCRYPTION_KEY` | clave AES-256 activa, Base64 (32 bytes) |
  | `CRM_PII_PREVIOUS_KEYS` | claves anteriores para descifrar: `k0:base64,…` |
  | `CRM_PII_BLIND_INDEX_KEY` | clave HMAC del índice ciego, Base64 (≥ 32 bytes) |

- Generación: `openssl rand -base64 32`.
- Sin claves válidas la aplicación **no arranca** (fallo explícito).
- Tests: claves aleatorias generadas en memoria por JVM (`TestPiiKeys`).

### Rotación
1. Generar una clave nueva `k2`; desplegar con `CRM_PII_ENCRYPTION_KEY_ID=k2`, `CRM_PII_ENCRYPTION_KEY=<k2>` y `CRM_PII_PREVIOUS_KEYS=k1:<k1>`.
2. Las escrituras usan `k2`; las lecturas descifran `k1` y `k2`. Un registro con clave antigua se recifra al guardarse (`needsReencryption`).
3. Recifrado masivo de los registros restantes (proceso batch; issue follow-up).
4. Cuando no queden datos con `k1`, retirar `k1` de `CRM_PII_PREVIOUS_KEYS`.

La clave del índice ciego **no rota** con el mismo procedimiento: cambiarla exige recalcular todos los `email_hash` en una ventana de mantenimiento. Se recomienda custodiarla en KMS con acceso restringido.

### TLS en tránsito
- Entrada: la aplicación se despliega tras un proxy/balanceador que termina TLS (`server.forward-headers-strategy=framework`).
- Salida: `OutboundTlsPolicy` exige **HTTPS** para pasarelas HTTP y **STARTTLS obligatorio** con verificación de identidad para SMTP. Solo el perfil `dev` permite texto plano a `localhost` (MailHog y mock SMS). Los adaptadores de canal (C03, C04, C21) deben usar esta política.
- PostgreSQL: en producción, `CRM_DB_URL` debe incluir `sslmode=verify-full`.

## Consecuencias
- No es posible buscar por subcadena de email ni teléfono, ni ordenar por ellos. La consulta admin filtra por email exacto.
- Un volcado de la base de datos no expone email ni teléfono sin las claves.
- La pérdida de las claves implica la pérdida de esos datos: deben respaldarse en el gestor de secretos.
