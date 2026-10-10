# Brief 20 — Gestión de contenidos (EPIC 2)

Referencia funcional: [requirements.md §3](../requirements.md#3-sistema-de-gestión-de-contenidos-cms)

## Modelo
- **Content**: `id, brandId, type, title, locale, body/assetRef, status (DRAFT|IN_REVIEW|APPROVED|PUBLISHED|ARCHIVED), version, tags, audienceRule (opcional)`.
- **Asset**: binario almacenado (filesystem local en dev; interfaz `AssetStorage` para S3-compatible), con validación de tipo MIME y tamaño.
- **PageTemplate**: estructura (cabecera, pie, menú, slots) y, por slot, tipos de componente permitidos y regla de selección de contenido.
- **Component** catálogo: miniatura, imagen, vídeo corto, vídeo, noticia breve (±imagen, ±vídeo), noticia (±imagen, ±vídeo), banner/modal promocional, cabecera multimedia, pie multimedia, hero. Extensible.
- **NavigationMenu**: ítems con regla de visibilidad.

## Workflow editorial
Productor crea → Revisor revisa → Administrador/aprobador publica. Cada transición notifica a los implicados. Solo contenidos `PUBLISHED` se sirven en el portal. Historial de versiones con posibilidad de restaurar.

## Seguridad
- Niveles: `ADMIN`, `GLOBAL_USER` (todas las Marcas), `LOCAL_USER` (una Marca).
- Roles editoriales: `CONTENT_REVIEWER`, `PRODUCER`, `DEVELOPER` (gestiona plantillas y componentes).
- Permisos por categoría/tipo de contenido.

## Renderizado personalizado
Al pedir una página: resolver plantilla → para cada slot evaluar reglas (crm-rules) con el perfil y preferencias del consumidor → elegir contenido, posición y variante visual → renderizar en el idioma del usuario con fallback al idioma por defecto de la Marca.

## Previsualización
Endpoint de preview que renderiza una página simulando un perfil arbitrario (sin registrar tracking).

## Tracking
Cada acceso a contenido y acción sobre él se publica como evento hacia `crm-tracking`.
