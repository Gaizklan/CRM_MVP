-- Datos semilla SOLO de desarrollo (perfil "dev"): Marca A y Marca B.
INSERT INTO brand (id, code, name, domain, default_language, status) VALUES
    ('a0000000-0000-0000-0000-00000000000a', 'marca-a', 'Marca A', 'marca-a.localhost', 'es', 'ACTIVE'),
    ('b0000000-0000-0000-0000-00000000000b', 'marca-b', 'Marca B', 'marca-b.localhost', 'es', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;
