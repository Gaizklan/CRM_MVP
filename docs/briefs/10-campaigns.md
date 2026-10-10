# Brief 10 — Gestión de campañas (EPIC 1)

Referencia funcional: [requirements.md §2](../requirements.md#2-sistema-de-gestión-de-campañas)

## Modelo
- **Campaign**: `id, brandId, name, type (BROADCAST|STANDARD|TRIGGERED|MGM|COLLECTIBLE), populationId, workflowDefinition, channels, status`.
- **Population**: `id, brandId, name, criteria (DSL de crm-rules)`; **PopulationInstance**: snapshot inmutable de consumer IDs con fecha.
- **WorkflowDefinition** (JSON/YAML versionado): grafo de nodos.
  - `ACTION` (enviar mensaje por canal, generar código único, mostrar banner, dejar mensaje en inbox)
  - `WAIT` (duración o hasta evento)
  - `CONDITION` (regla sobre evento: abrió, hizo clic en zona X, cambió perfil…)
  - `END`
- **CampaignExecution**: estado `CREATED → RUNNING ⇄ PAUSED → STOPPED | COMPLETED`; estado por consumidor y nodo persistido.
- **MessageTemplate**: tipos `EMAIL_TEXT, EMAIL_HTML, SMS, PUSH, INBOX, FILE_EXPORT, SQL_EXPORT`; variables de personalización; multiidioma.

## Flujo de ejecución
1. Se materializa una `PopulationInstance` a partir del criterio.
2. Cada consumidor entra en el nodo inicial.
3. El motor avanza por ticks planificados y por eventos recibidos de `crm-tracking`.
4. Las acciones generan registros outbox; `crm-messaging` los despacha verificando opt-in del canal.
5. Resultados (entregado, rechazado, fallido, abierto, clic) vuelven como eventos y pueden disparar transiciones.

## Workflow de referencia (debe existir como plantilla y como test de integración)
Invitación a evento → si abre y hace clic: confirmación + mensaje con número de reserva; si abre y no hace clic: refuerzo; si no abre en 2 días: reenvío.

## Criterios transversales
- Nunca enviar sin opt-in vigente del canal (test obligatorio).
- Pausa conserva estado; parada es definitiva.
- Reinicio de la aplicación a mitad de campaña: se completan los envíos pendientes sin duplicados (test con Testcontainers).
- Reintentos configurables por Marca y canal; tras agotarlos, estado `FAILED`.
- Varios proveedores por canal con estrategia de selección (manual, round-robin, menor carga).
- Cada comunicación enviada se guarda renderizada para poder verla tal como la recibió el consumidor.
