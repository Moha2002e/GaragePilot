--liquibase formatted sql

--changeset garagepilot:007-default-garage-services dbms:postgresql
INSERT INTO garage_services (
    id, garage_id, name, category, description, duration_minutes,
    price, vat_rate, color, active, created_at, updated_at
)
SELECT
    md5(g.id::text || defaults.slug)::uuid,
    g.id,
    defaults.name,
    defaults.category,
    defaults.description,
    defaults.duration_minutes,
    defaults.price,
    21.00,
    defaults.color,
    true,
    now(),
    now()
FROM garages g
CROSS JOIN (VALUES
    ('entretien-annuel', 'Entretien annuel', 'MAINTENANCE', 'Vidange, filtres et contrôles de sécurité.', 90, 149.00, '#087a55'),
    ('vidange-moteur', 'Vidange moteur', 'MAINTENANCE', 'Huile moteur et remplacement du filtre à huile.', 45, 89.00, '#12b76a'),
    ('freins', 'Contrôle et remplacement des freins', 'MECHANICAL', 'Diagnostic du freinage et remplacement des pièces après accord.', 120, 219.00, '#f79009'),
    ('pneus', 'Montage de quatre pneus', 'TIRES', 'Montage, équilibrage et contrôle des pressions.', 75, 96.00, '#2e90fa'),
    ('diagnostic', 'Diagnostic électronique', 'DIAGNOSTIC', 'Lecture des défauts et rapport de diagnostic.', 60, 79.00, '#6172f3'),
    ('batterie', 'Contrôle batterie et alternateur', 'ELECTRICAL', 'Test du circuit de charge et de la capacité de démarrage.', 30, 45.00, '#7f56d9'),
    ('climatisation', 'Entretien climatisation', 'MAINTENANCE', 'Contrôle, recharge et désinfection du circuit.', 60, 129.00, '#06aed4'),
    ('geometrie', 'Géométrie et parallélisme', 'TIRES', 'Mesure et réglage de la géométrie des trains roulants.', 60, 85.00, '#475467')
) AS defaults(slug, name, category, description, duration_minutes, price, color)
ON CONFLICT (garage_id, name) DO NOTHING;

--rollback DELETE FROM garage_services WHERE name IN ('Entretien annuel', 'Vidange moteur', 'Contrôle et remplacement des freins', 'Montage de quatre pneus', 'Diagnostic électronique', 'Contrôle batterie et alternateur', 'Entretien climatisation', 'Géométrie et parallélisme');
