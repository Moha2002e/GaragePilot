-- GaragePilot - jeu de donnees fictives multi-garages pour la vue super-admin.
--
-- Jeu idempotent : 5 garages, 10 utilisateurs et 100 lignes pour chaque
-- entite metier principale, reparties de janvier a decembre de l'annee en cours.
-- Les UUID deterministes permettent de relancer le script sans doublons.
--
-- Prerequis : demarrer le backend une fois pour que Liquibase cree le schema,
-- puis executer ce fichier dans Supabase > SQL Editor.

DO $$
DECLARE
    v_owner_email text := 'demo.20260929151433@garagepilot.local';
    v_primary_garage_id uuid;
    v_owner_password_hash text;
    v_year integer := extract(year from current_date)::integer;
    v_year_start date := make_date(extract(year from current_date)::integer, 1, 1);
BEGIN
    SELECT garage_id, password_hash
      INTO v_primary_garage_id, v_owner_password_hash
      FROM users
     WHERE lower(email) = lower(v_owner_email) AND active = true
     LIMIT 1;

    IF v_primary_garage_id IS NULL THEN
        RAISE EXCEPTION 'Aucun garage trouve pour le compte %. Modifiez v_owner_email.', v_owner_email;
    END IF;

    INSERT INTO garages (
        id, name, vat_number, email, phone, street, city, postal_code,
        country, active, created_at, updated_at
    )
    SELECT
        md5('gp-demo-garage-' || g)::uuid,
        (ARRAY['Atelier du Centre','Garage Meuse Auto',
               'Mecanique des Hautes Fagnes','Auto Service Hesbaye'])[g - 1],
        'BE0' || lpad((760000000 + g)::text, 9, '0'),
        'contact.garage' || g || '@demo.garagepilot.be',
        '+32 4 220 ' || lpad((10 + g)::text, 2, '0') || ' ' || lpad((20 + g)::text, 2, '0'),
        (10 + g) || ' rue de l''Atelier',
        (ARRAY['Liege','Seraing','Verviers','Waremme'])[g - 1],
        (ARRAY['4000','4100','4800','4300'])[g - 1],
        'BE', true, v_year_start + ((g - 1) * 21), now()
    FROM generate_series(2, 5) AS g
    ON CONFLICT (id) DO UPDATE SET
        name = EXCLUDED.name, vat_number = EXCLUDED.vat_number,
        email = EXCLUDED.email, phone = EXCLUDED.phone,
        street = EXCLUDED.street, city = EXCLUDED.city,
        postal_code = EXCLUDED.postal_code, active = true, updated_at = now();

    INSERT INTO users (
        id, garage_id, first_name, last_name, email, password_hash, phone,
        role, active, created_at, updated_at
    )
    SELECT
        md5('gp-demo-user-' || g || '-' || u)::uuid,
        CASE WHEN g = 1 THEN v_primary_garage_id
             ELSE md5('gp-demo-garage-' || g)::uuid END,
        (ARRAY['Camille','Alex','Nora','Hugo','Lea','Yanis','Julie','Tom','Sarah','Louis'])[(g - 1) * 2 + u],
        (ARRAY['Leroy','Martin','Lambert','Simon','Dubois','Henry','Renard','Petit','Michel','Robert'])[(g - 1) * 2 + u],
        'equipe' || g || '.' || u || '@demo.garagepilot.be',
        v_owner_password_hash,
        '+32 470 ' || lpad(g::text, 2, '0') || ' ' || lpad(u::text, 2, '0') || ' 00',
        CASE WHEN u = 1 THEN 'OWNER' ELSE 'MECHANIC' END,
        true, v_year_start + (((g - 1) * 2 + u) * 5), now()
    FROM generate_series(1, 5) AS g
    CROSS JOIN generate_series(1, 2) AS u
    ON CONFLICT (id) DO UPDATE SET
        garage_id = EXCLUDED.garage_id, first_name = EXCLUDED.first_name,
        last_name = EXCLUDED.last_name, email = EXCLUDED.email,
        phone = EXCLUDED.phone, role = EXCLUDED.role,
        active = true, updated_at = now();

    INSERT INTO clients (
        id, garage_id, type, first_name, last_name, company_name, vat_number,
        email, phone, street, postal_code, city, country, notes, active,
        created_at, updated_at
    )
    SELECT
        md5('gp-demo-client-' || n)::uuid,
        CASE WHEN ((n - 1) % 5) + 1 = 1 THEN v_primary_garage_id
             ELSE md5('gp-demo-garage-' || (((n - 1) % 5) + 1))::uuid END,
        CASE WHEN n % 10 = 0 THEN 'COMPANY' ELSE 'INDIVIDUAL' END,
        CASE WHEN n % 10 = 0 THEN NULL ELSE (ARRAY[
            'Sophie','Thomas','Nadia','Julien','Emma','Lucas','Sarah','Mehdi','Claire','Nicolas',
            'Julie','David','Amina','Laurent','Chloe','Samuel','Ines','Olivier','Laura','Karim'
        ])[1 + ((n - 1) % 20)] END,
        CASE WHEN n % 10 = 0 THEN NULL ELSE (ARRAY[
            'Dubois','Lambert','Benali','Simon','Leclercq','Martin','Jacobs','El Amrani','Renard','Michel',
            'Leroy','Robert','Boulahrouz','Moreau','Petit','Henry','Diallo','Denis','Francois','Ait Ali'
        ])[1 + (((n * 7) - 1) % 20)] END,
        CASE WHEN n % 10 = 0 THEN
            (ARRAY['Boulangerie du Centre','Electricite Lambert','Taxi Mosan',
                   'Jardin et Fils','Construction Horizon'])[1 + (((n / 10) - 1) % 5)]
            || ' SRL ' || n ELSE NULL END,
        CASE WHEN n % 10 = 0 THEN 'BE0' || lpad((740000000 + n)::text, 9, '0') ELSE NULL END,
        'client' || lpad(n::text, 3, '0') || '@demo.garagepilot.be',
        '+32 470 ' || lpad((10 + n % 80)::text, 2, '0') || ' ' || lpad((20 + n % 70)::text, 2, '0'),
        n || ' rue de la Station',
        (ARRAY['4000','4020','4430','4800','4600','4100','4300','4500'])[1 + ((n - 1) % 8)],
        (ARRAY['Liege','Liege','Ans','Verviers','Vise','Seraing','Waremme','Huy'])[1 + ((n - 1) % 8)],
        'BE', 'Donnee fictive GaragePilot - client de demonstration ' || n,
        true, v_year_start + floor((n - 1) * 364.0 / 99)::integer, now()
    FROM generate_series(1, 100) AS n
    ON CONFLICT (id) DO UPDATE SET
        garage_id = EXCLUDED.garage_id, type = EXCLUDED.type,
        first_name = EXCLUDED.first_name, last_name = EXCLUDED.last_name,
        company_name = EXCLUDED.company_name, vat_number = EXCLUDED.vat_number,
        email = EXCLUDED.email, phone = EXCLUDED.phone, street = EXCLUDED.street,
        postal_code = EXCLUDED.postal_code, city = EXCLUDED.city,
        notes = EXCLUDED.notes, active = true,
        created_at = EXCLUDED.created_at, updated_at = now();

    INSERT INTO vehicles (
        id, garage_id, client_id, license_plate, vin, brand, model, version,
        fuel_type, first_registration_date, mileage, color, notes, active,
        created_at, updated_at
    )
    SELECT
        md5('gp-demo-vehicle-' || n)::uuid,
        CASE WHEN ((n - 1) % 5) + 1 = 1 THEN v_primary_garage_id
             ELSE md5('gp-demo-garage-' || (((n - 1) % 5) + 1))::uuid END,
        md5('gp-demo-client-' || n)::uuid,
        'GP-' || lpad(n::text, 3, '0') || '-D',
        upper(substr(md5('garagepilot-vin-' || n), 1, 17)),
        (ARRAY['Peugeot','Renault','Volkswagen','BMW','Mercedes','Toyota','Citroen','Opel','Ford','Volvo'])[1 + ((n - 1) % 10)],
        (ARRAY['308','Clio','Golf','Serie 1','Classe A','Corolla','C3','Astra','Focus','XC40'])[1 + ((n - 1) % 10)],
        (ARRAY['1.2 PureTech','1.0 TCe','1.5 TSI','118i','A180','1.8 Hybrid','1.2 Turbo','1.4 Turbo','1.0 EcoBoost','B3'])[1 + ((n - 1) % 10)],
        (ARRAY['PETROL','DIESEL','HYBRID','PETROL','DIESEL','HYBRID','PETROL','ELECTRIC','DIESEL','ELECTRIC'])[1 + ((n - 1) % 10)],
        date '2014-01-01' + ((n * 31) % 3650), 18000 + n * 1450,
        (ARRAY['Gris','Noir','Blanc','Bleu','Rouge','Vert'])[1 + ((n - 1) % 6)],
        'Donnee fictive GaragePilot - vehicule de demonstration ' || n,
        true, v_year_start + floor((n - 1) * 364.0 / 99)::integer, now()
    FROM generate_series(1, 100) AS n
    ON CONFLICT (id) DO UPDATE SET
        garage_id = EXCLUDED.garage_id, client_id = EXCLUDED.client_id,
        license_plate = EXCLUDED.license_plate, vin = EXCLUDED.vin,
        brand = EXCLUDED.brand, model = EXCLUDED.model, version = EXCLUDED.version,
        fuel_type = EXCLUDED.fuel_type,
        first_registration_date = EXCLUDED.first_registration_date,
        mileage = EXCLUDED.mileage, color = EXCLUDED.color,
        notes = EXCLUDED.notes, active = true,
        created_at = EXCLUDED.created_at, updated_at = now();

    INSERT INTO appointments (
        id, garage_id, client_id, vehicle_id, start_date_time, end_date_time,
        reason, notes, status, created_at, updated_at
    )
    SELECT
        md5('gp-demo-appointment-' || n)::uuid,
        CASE WHEN ((n - 1) % 5) + 1 = 1 THEN v_primary_garage_id
             ELSE md5('gp-demo-garage-' || (((n - 1) % 5) + 1))::uuid END,
        md5('gp-demo-client-' || n)::uuid, md5('gp-demo-vehicle-' || n)::uuid,
        v_year_start + floor((n - 1) * 364.0 / 99)::integer
            + time '08:00' + ((n - 1) % 8) * interval '1 hour',
        v_year_start + floor((n - 1) * 364.0 / 99)::integer
            + time '09:30' + ((n - 1) % 8) * interval '1 hour',
        (ARRAY['Entretien annuel','Diagnostic moteur','Remplacement des freins',
               'Montage pneus','Controle technique','Vidange et filtres',
               'Climatisation','Batterie'])[1 + ((n - 1) % 8)],
        'Rendez-vous fictif du seed annuel GaragePilot.',
        CASE
            WHEN v_year_start + floor((n - 1) * 364.0 / 99)::integer < current_date THEN 'COMPLETED'
            WHEN v_year_start + floor((n - 1) * 364.0 / 99)::integer = current_date THEN 'IN_PROGRESS'
            WHEN n % 3 = 0 THEN 'CONFIRMED' ELSE 'PLANNED' END,
        v_year_start + floor((n - 1) * 364.0 / 99)::integer - 14, now()
    FROM generate_series(1, 100) AS n
    ON CONFLICT (id) DO UPDATE SET
        garage_id = EXCLUDED.garage_id, client_id = EXCLUDED.client_id,
        vehicle_id = EXCLUDED.vehicle_id,
        start_date_time = EXCLUDED.start_date_time,
        end_date_time = EXCLUDED.end_date_time, reason = EXCLUDED.reason,
        notes = EXCLUDED.notes, status = EXCLUDED.status,
        created_at = EXCLUDED.created_at, updated_at = now();

    INSERT INTO work_orders (
        id, garage_id, client_id, vehicle_id, appointment_id, status,
        mileage_in, mileage_out, description, technician_notes, customer_notes,
        started_at, completed_at, created_at, updated_at
    )
    SELECT
        md5('gp-demo-work-order-' || n)::uuid,
        CASE WHEN ((n - 1) % 5) + 1 = 1 THEN v_primary_garage_id
             ELSE md5('gp-demo-garage-' || (((n - 1) % 5) + 1))::uuid END,
        md5('gp-demo-client-' || n)::uuid, md5('gp-demo-vehicle-' || n)::uuid,
        md5('gp-demo-appointment-' || n)::uuid,
        CASE
            WHEN v_year_start + floor((n - 1) * 364.0 / 99)::integer > current_date THEN 'DRAFT'
            WHEN n % 4 = 0 THEN 'INVOICED' ELSE 'COMPLETED' END,
        18000 + n * 1450,
        CASE WHEN v_year_start + floor((n - 1) * 364.0 / 99)::integer <= current_date
             THEN 18020 + n * 1450 ELSE NULL END,
        (ARRAY['Entretien complet','Freinage avant','Diagnostic electronique',
               'Embrayage','Distribution','Pneumatiques','Vidange moteur',
               'Suspension avant','Batterie et alternateur','Climatisation'])[1 + ((n - 1) % 10)],
        'Controles effectues. Pieces homologuees uniquement.',
        CASE WHEN n % 3 = 0 THEN 'Prevenir le client avant tout supplement.' ELSE NULL END,
        v_year_start + floor((n - 1) * 364.0 / 99)::integer + time '08:00',
        CASE WHEN v_year_start + floor((n - 1) * 364.0 / 99)::integer <= current_date
             THEN v_year_start + floor((n - 1) * 364.0 / 99)::integer + time '16:00'
             ELSE NULL END,
        v_year_start + floor((n - 1) * 364.0 / 99)::integer - 14, now()
    FROM generate_series(1, 100) AS n
    ON CONFLICT (id) DO UPDATE SET
        garage_id = EXCLUDED.garage_id, client_id = EXCLUDED.client_id,
        vehicle_id = EXCLUDED.vehicle_id, appointment_id = EXCLUDED.appointment_id,
        status = EXCLUDED.status, mileage_in = EXCLUDED.mileage_in,
        mileage_out = EXCLUDED.mileage_out, description = EXCLUDED.description,
        technician_notes = EXCLUDED.technician_notes,
        customer_notes = EXCLUDED.customer_notes,
        started_at = EXCLUDED.started_at, completed_at = EXCLUDED.completed_at,
        created_at = EXCLUDED.created_at, updated_at = now();

    INSERT INTO work_order_items (
        id, garage_id, work_order_id, type, description, quantity, unit_price,
        vat_rate, total_excluding_vat, created_at, updated_at
    )
    SELECT
        md5('gp-demo-work-item-' || w || '-' || i)::uuid,
        CASE WHEN ((w - 1) % 5) + 1 = 1 THEN v_primary_garage_id
             ELSE md5('gp-demo-garage-' || (((w - 1) % 5) + 1))::uuid END,
        md5('gp-demo-work-order-' || w)::uuid,
        (ARRAY['SERVICE','PART','LABOR'])[i],
        (ARRAY['Entretien et diagnostic','Pieces et consommables','Main-d''oeuvre atelier'])[i],
        CASE WHEN i = 3 THEN 2.5 ELSE 1 END,
        CASE WHEN i = 1 THEN 65 + (w % 15) * 3
             WHEN i = 2 THEN 85 + (w % 12) * 7 ELSE 72 END,
        21,
        CASE WHEN i = 1 THEN 65 + (w % 15) * 3
             WHEN i = 2 THEN 85 + (w % 12) * 7 ELSE 180 END,
        v_year_start + floor((w - 1) * 364.0 / 99)::integer - 14, now()
    FROM generate_series(1, 100) AS w
    CROSS JOIN generate_series(1, 3) AS i
    ON CONFLICT (id) DO UPDATE SET
        garage_id = EXCLUDED.garage_id, work_order_id = EXCLUDED.work_order_id,
        type = EXCLUDED.type, description = EXCLUDED.description,
        quantity = EXCLUDED.quantity, unit_price = EXCLUDED.unit_price,
        vat_rate = EXCLUDED.vat_rate,
        total_excluding_vat = EXCLUDED.total_excluding_vat,
        created_at = EXCLUDED.created_at, updated_at = now();

    INSERT INTO quotes (
        id, garage_id, client_id, vehicle_id, work_order_id, quote_number,
        status, issue_date, valid_until, subtotal, vat_amount, total,
        customer_comment, created_at, updated_at
    )
    SELECT
        md5('gp-demo-quote-' || n)::uuid,
        CASE WHEN ((n - 1) % 5) + 1 = 1 THEN v_primary_garage_id
             ELSE md5('gp-demo-garage-' || (((n - 1) % 5) + 1))::uuid END,
        md5('gp-demo-client-' || n)::uuid, md5('gp-demo-vehicle-' || n)::uuid,
        md5('gp-demo-work-order-' || n)::uuid,
        'DEV-' || v_year || '-' || lpad(n::text, 4, '0'),
        CASE
            WHEN v_year_start + floor((n - 1) * 364.0 / 99)::integer > current_date THEN 'DRAFT'
            WHEN n % 10 = 0 THEN 'REJECTED'
            WHEN n % 3 = 0 THEN 'SENT' ELSE 'ACCEPTED' END,
        v_year_start + floor((n - 1) * 364.0 / 99)::integer,
        v_year_start + floor((n - 1) * 364.0 / 99)::integer + 30,
        220 + (n % 18) * 55,
        round((220 + (n % 18) * 55) * 0.21, 2),
        round((220 + (n % 18) * 55) * 1.21, 2),
        CASE WHEN n % 10 = 0 THEN 'Le client souhaite reporter les travaux.' ELSE NULL END,
        v_year_start + floor((n - 1) * 364.0 / 99)::integer - 2, now()
    FROM generate_series(1, 100) AS n
    ON CONFLICT (id) DO UPDATE SET
        garage_id = EXCLUDED.garage_id, client_id = EXCLUDED.client_id,
        vehicle_id = EXCLUDED.vehicle_id, work_order_id = EXCLUDED.work_order_id,
        quote_number = EXCLUDED.quote_number, status = EXCLUDED.status,
        issue_date = EXCLUDED.issue_date, valid_until = EXCLUDED.valid_until,
        subtotal = EXCLUDED.subtotal, vat_amount = EXCLUDED.vat_amount,
        total = EXCLUDED.total, customer_comment = EXCLUDED.customer_comment,
        created_at = EXCLUDED.created_at, updated_at = now();

    INSERT INTO quote_items (
        id, garage_id, quote_id, type, description, quantity, unit_price,
        vat_rate, total_excluding_vat, created_at, updated_at
    )
    SELECT
        md5('gp-demo-quote-item-' || q || '-' || i)::uuid,
        CASE WHEN ((q - 1) % 5) + 1 = 1 THEN v_primary_garage_id
             ELSE md5('gp-demo-garage-' || (((q - 1) % 5) + 1))::uuid END,
        md5('gp-demo-quote-' || q)::uuid,
        (ARRAY['SERVICE','PART','LABOR'])[i],
        (ARRAY['Diagnostic et entretien','Pieces prevues','Main-d''oeuvre estimee'])[i],
        1, round((220 + (q % 18) * 55) / 3.0, 2), 21,
        round((220 + (q % 18) * 55) / 3.0, 2),
        v_year_start + floor((q - 1) * 364.0 / 99)::integer - 2, now()
    FROM generate_series(1, 100) AS q
    CROSS JOIN generate_series(1, 3) AS i
    ON CONFLICT (id) DO UPDATE SET
        garage_id = EXCLUDED.garage_id, quote_id = EXCLUDED.quote_id,
        type = EXCLUDED.type, description = EXCLUDED.description,
        quantity = EXCLUDED.quantity, unit_price = EXCLUDED.unit_price,
        vat_rate = EXCLUDED.vat_rate,
        total_excluding_vat = EXCLUDED.total_excluding_vat,
        created_at = EXCLUDED.created_at, updated_at = now();

    INSERT INTO invoices (
        id, garage_id, client_id, vehicle_id, work_order_id, quote_id,
        invoice_number, status, issue_date, due_date, subtotal, vat_amount,
        total, amount_paid, balance_due, created_at, updated_at
    )
    SELECT
        md5('gp-demo-invoice-' || n)::uuid,
        CASE WHEN ((n - 1) % 5) + 1 = 1 THEN v_primary_garage_id
             ELSE md5('gp-demo-garage-' || (((n - 1) % 5) + 1))::uuid END,
        md5('gp-demo-client-' || n)::uuid, md5('gp-demo-vehicle-' || n)::uuid,
        md5('gp-demo-work-order-' || n)::uuid, md5('gp-demo-quote-' || n)::uuid,
        'FAC-' || v_year || '-' || lpad(n::text, 4, '0'),
        CASE
            WHEN v_year_start + floor((n - 1) * 364.0 / 99)::integer > current_date THEN 'DRAFT'
            WHEN n % 9 = 0 THEN 'OVERDUE'
            WHEN n % 7 = 0 THEN 'PARTIALLY_PAID'
            WHEN n % 5 = 0 THEN 'ISSUED' ELSE 'PAID' END,
        v_year_start + floor((n - 1) * 364.0 / 99)::integer,
        v_year_start + floor((n - 1) * 364.0 / 99)::integer + 30,
        240 + (n % 20) * 60,
        round((240 + (n % 20) * 60) * 0.21, 2),
        round((240 + (n % 20) * 60) * 1.21, 2),
        CASE
            WHEN v_year_start + floor((n - 1) * 364.0 / 99)::integer > current_date THEN 0
            WHEN n % 9 = 0 OR n % 5 = 0 THEN 0
            WHEN n % 7 = 0 THEN round((240 + (n % 20) * 60) * 1.21 * 0.5, 2)
            ELSE round((240 + (n % 20) * 60) * 1.21, 2) END,
        CASE
            WHEN v_year_start + floor((n - 1) * 364.0 / 99)::integer > current_date
                THEN round((240 + (n % 20) * 60) * 1.21, 2)
            WHEN n % 9 = 0 OR n % 5 = 0
                THEN round((240 + (n % 20) * 60) * 1.21, 2)
            WHEN n % 7 = 0
                THEN round((240 + (n % 20) * 60) * 1.21 * 0.5, 2)
            ELSE 0 END,
        v_year_start + floor((n - 1) * 364.0 / 99)::integer - 1, now()
    FROM generate_series(1, 100) AS n
    ON CONFLICT (id) DO UPDATE SET
        garage_id = EXCLUDED.garage_id, client_id = EXCLUDED.client_id,
        vehicle_id = EXCLUDED.vehicle_id, work_order_id = EXCLUDED.work_order_id,
        quote_id = EXCLUDED.quote_id, invoice_number = EXCLUDED.invoice_number,
        status = EXCLUDED.status, issue_date = EXCLUDED.issue_date,
        due_date = EXCLUDED.due_date, subtotal = EXCLUDED.subtotal,
        vat_amount = EXCLUDED.vat_amount, total = EXCLUDED.total,
        amount_paid = EXCLUDED.amount_paid, balance_due = EXCLUDED.balance_due,
        created_at = EXCLUDED.created_at, updated_at = now();

    INSERT INTO invoice_items (
        id, garage_id, invoice_id, type, description, quantity, unit_price,
        vat_rate, total_excluding_vat, created_at, updated_at
    )
    SELECT
        md5('gp-demo-invoice-item-' || inv || '-' || i)::uuid,
        CASE WHEN ((inv - 1) % 5) + 1 = 1 THEN v_primary_garage_id
             ELSE md5('gp-demo-garage-' || (((inv - 1) % 5) + 1))::uuid END,
        md5('gp-demo-invoice-' || inv)::uuid,
        (ARRAY['SERVICE','PART','LABOR'])[i],
        (ARRAY['Travaux realises','Pieces utilisees','Main-d''oeuvre'])[i],
        1, round((240 + (inv % 20) * 60) / 3.0, 2), 21,
        round((240 + (inv % 20) * 60) / 3.0, 2),
        v_year_start + floor((inv - 1) * 364.0 / 99)::integer - 1, now()
    FROM generate_series(1, 100) AS inv
    CROSS JOIN generate_series(1, 3) AS i
    ON CONFLICT (id) DO UPDATE SET
        garage_id = EXCLUDED.garage_id, invoice_id = EXCLUDED.invoice_id,
        type = EXCLUDED.type, description = EXCLUDED.description,
        quantity = EXCLUDED.quantity, unit_price = EXCLUDED.unit_price,
        vat_rate = EXCLUDED.vat_rate,
        total_excluding_vat = EXCLUDED.total_excluding_vat,
        created_at = EXCLUDED.created_at, updated_at = now();

    INSERT INTO payments (
        id, garage_id, invoice_id, amount, payment_date, method, reference,
        notes, created_at, updated_at
    )
    SELECT
        md5('gp-demo-payment-' || n)::uuid,
        CASE WHEN ((n - 1) % 5) + 1 = 1 THEN v_primary_garage_id
             ELSE md5('gp-demo-garage-' || (((n - 1) % 5) + 1))::uuid END,
        md5('gp-demo-invoice-' || n)::uuid,
        CASE WHEN n % 7 = 0
             THEN round((240 + (n % 20) * 60) * 1.21 * 0.5, 2)
             ELSE round((240 + (n % 20) * 60) * 1.21, 2) END,
        v_year_start + floor((n - 1) * 364.0 / 99)::integer + 7,
        (ARRAY['CARD','BANK_TRANSFER','CASH'])[1 + ((n - 1) % 3)],
        'PAY-' || v_year || '-' || lpad(n::text, 4, '0'),
        'Paiement fictif GaragePilot.',
        v_year_start + floor((n - 1) * 364.0 / 99)::integer + 7, now()
    FROM generate_series(1, 100) AS n
    WHERE v_year_start + floor((n - 1) * 364.0 / 99)::integer <= current_date
      AND n % 9 <> 0 AND n % 5 <> 0
    ON CONFLICT (id) DO UPDATE SET
        garage_id = EXCLUDED.garage_id, invoice_id = EXCLUDED.invoice_id,
        amount = EXCLUDED.amount, payment_date = EXCLUDED.payment_date,
        method = EXCLUDED.method, reference = EXCLUDED.reference,
        notes = EXCLUDED.notes, created_at = EXCLUDED.created_at,
        updated_at = now();

    INSERT INTO garage_services (
        id, garage_id, name, category, description, duration_minutes,
        price, vat_rate, color, active, created_at, updated_at
    )
    SELECT
        md5('gp-demo-service-' || g || '-' || s)::uuid,
        CASE WHEN g = 1 THEN v_primary_garage_id
             ELSE md5('gp-demo-garage-' || g)::uuid END,
        (ARRAY['Entretien annuel','Vidange moteur',
               'Controle et remplacement des freins','Montage de quatre pneus',
               'Diagnostic electronique','Controle batterie et alternateur',
               'Entretien climatisation','Geometrie et parallelisme'])[s],
        (ARRAY['MAINTENANCE','MAINTENANCE','MECHANICAL','TIRES',
               'DIAGNOSTIC','ELECTRICAL','MAINTENANCE','TIRES'])[s],
        'Prestation fictive du catalogue GaragePilot.',
        (ARRAY[90,45,120,75,60,30,60,60])[s],
        (ARRAY[149.00,89.00,219.00,96.00,79.00,45.00,129.00,85.00])[s],
        21,
        (ARRAY['#087a55','#12b76a','#f79009','#2e90fa',
               '#6172f3','#7f56d9','#06aed4','#475467'])[s],
        true, v_year_start, now()
    FROM generate_series(1, 5) AS g
    CROSS JOIN generate_series(1, 8) AS s
    ON CONFLICT (garage_id, name) DO UPDATE SET
        garage_id = EXCLUDED.garage_id, name = EXCLUDED.name,
        category = EXCLUDED.category, description = EXCLUDED.description,
        duration_minutes = EXCLUDED.duration_minutes, price = EXCLUDED.price,
        vat_rate = EXCLUDED.vat_rate, color = EXCLUDED.color,
        active = true, updated_at = now();

    UPDATE users SET role = 'SUPER_ADMIN', updated_at = now()
     WHERE lower(email) = lower(v_owner_email);

    RAISE NOTICE 'Jeu annuel GaragePilot ajoute : 5 garages et 100 lignes par entite principale.';
END $$;
