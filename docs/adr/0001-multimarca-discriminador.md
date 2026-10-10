# ADR 0001 — Aislamiento multimarca por columna discriminadora

- Estado: aceptado
- Fecha: 2026-10-10
- Issue: #7 (P03)

## Contexto
NFR-2 exige que una instancia sirva a varias Marcas con aislamiento de datos. El brief de arquitectura fija que toda entidad de negocio lleva `brand_id` y que el filtro de Marca es obligatorio en repositorios.

Opciones consideradas:
1. **Esquema o base de datos por Marca**: aislamiento fuerte, pero multiplica migraciones y conexiones y complica consultas transversales (usuarios globales, informes).
2. **Columna `brand_id` filtrada a mano en cada consulta**: simple, pero un olvido filtra datos entre Marcas.
3. **Columna `brand_id` con multi-tenancy por discriminador de Hibernate (`@TenantId`)**: Hibernate añade el filtro a toda consulta JPQL/Criteria/derivada y asigna la Marca al insertar.

## Decisión
Opción 3, con estas piezas:
- `TenantContext` (ThreadLocal) en `crm-common`, resuelto por petición en `TenantFilter`: portal por prefijo `/portal/{código}` o dominio; administración por cabecera `X-Brand` o parámetro `brand`.
- `BrandScopedEntity` (`@MappedSuperclass` con `@TenantId brand_id`) como base de toda entidad JPA de negocio.
- `BrandTenantIdentifierResolver`: sin Marca en contexto devuelve un identificador inexistente (las consultas no devuelven nada y las inserciones fallan por FK): **fallo cerrado**. Un ámbito raíz explícito (`TenantContext.callAsRoot`) desactiva el filtro para procesos de plataforma.
- `TenantAwareJpaRepository` como clase base de los repositorios Spring Data: filtra también `findById`/`existsById`, que Hibernate resuelve por clave primaria sin aplicar el discriminador.

## Consecuencias
- Las consultas SQL nativas (p. ej. segmentación) **no** pasan por el filtro de Hibernate: deben incluir `brand_id = :brandId` explícitamente.
- El uso de `callAsRoot` debe limitarse a infraestructura revisada.
- Hibernate asocia la Marca a la **sesión** al abrirla: el `TenantContext` debe fijarse **antes** de iniciar la transacción (lo hacen `TenantFilter` en peticiones web y `OutboxRelay` antes de la transacción de cada evento). Cambiar de Marca dentro de una transacción abierta no tiene efecto sobre la sesión ya abierta (detectado en P11).
- La tabla `brand` y las tablas de plataforma (usuarios internos) no son entidades con Marca.
