# DSL de reglas de negocio

Formato JSON único para segmentación, condiciones de workflow, personalización de contenidos y menús (brief 01). Implementación: `crm-rules` (`RuleParser`, `RuleEvaluator`, `RuleEngine`).

## Nodos
Cada nodo es un objeto con **una única clave**, el operador.

| Operador | Forma | Notas |
|---|---|---|
| `and`, `or` | `{"and": [regla, …]}` | lista no vacía |
| `not` | `{"not": regla}` | |
| `eq`, `ne` | `{"eq": {"field": f, "value": v}}` | `ne` = `not eq` |
| `gt`, `lt` | `{"gt": {"field": f, "value": v}}` | solo números y fechas |
| `in` | `{"in": {"field": f, "values": [v, …]}}` | 1–1000 valores |
| `between` | `{"between": {"field": f, "from": v, "to": v}}` | rango cerrado |
| `exists` | `{"exists": {"field": f}}` | el campo tiene valor |
| `contains` | `{"contains": {"field": f, "value": "txt"}}` | subcadena, o elemento de `subscriptions` |
| `subscribedTo` | `{"subscribedTo": {"service": "newsletter"}}` | suscripción activa |
| `eventOccurred` | `{"eventOccurred": {"type": "EMAIL_OPENED", "within": "P2D"}}` | duración ISO-8601, máx. 366 días |

## Campos
| Campo | Tipo |
|---|---|
| `email`, `phone`, `language`, `segment`, `status` | texto |
| `registeredAt` | fecha/hora (`"2026-01-01"` = inicio del día UTC, o instante ISO) |
| `attribute.<nombre>` | texto |
| `preference.<clave>` | según la preferencia: texto, número, booleano o fecha (`yyyy-MM-dd`) |
| `consent.<EMAIL\|SMS\|MMS\|PUSH\|SUBSCRIPTIONS\|CRM_PROGRAM>` | booleano: canal permitido (opt-in del canal y del programa) |
| `subscriptions` | conjunto de códigos de servicio (solo `contains`/`exists`) |

## Semántica
- Comparaciones de texto sensibles a mayúsculas; números por valor (`1` = `1.0`).
- Un campo sin valor, o de tipo distinto al literal, hace la comparación **falsa** (`ne` es verdadera).
- Límites: 20 niveles de anidamiento, 500 nodos.

## Ejemplo
Consumidores del segmento S1 con opt-in de email que abrieron un email en los últimos 2 días:
```json
{"and": [
  {"eq": {"field": "segment", "value": "S1"}},
  {"eq": {"field": "consent.EMAIL", "value": true}},
  {"eventOccurred": {"type": "EMAIL_OPENED", "within": "P2D"}}
]}
```
