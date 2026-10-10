-- Datos semilla SOLO de desarrollo (perfil "dev"): servicios suscribibles de ejemplo.
INSERT INTO subscription_service (id, brand_id, code, name, description, active) VALUES
    ('a1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-00000000000a', 'newsletter', 'Boletín', 'Boletín periódico de la Marca A', TRUE),
    ('a1000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-00000000000a', 'news', 'Novedades', 'Novedades y actividades de la Marca A', TRUE),
    ('b1000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-00000000000b', 'newsletter', 'Boletín', 'Boletín periódico de la Marca B', TRUE)
ON CONFLICT (id) DO NOTHING;
