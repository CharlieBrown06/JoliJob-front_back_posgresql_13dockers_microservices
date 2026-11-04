-- Execute the entire script on jolijob_db, never individual statements.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

DO $$
BEGIN
    IF current_database() <> 'jolijob_db' THEN
        RAISE EXCEPTION 'CRUD tests require a connection to jolijob_db';
    END IF;
END
$$;

CREATE TEMP TABLE crud_results (
    test_order integer GENERATED ALWAYS AS IDENTITY,
    test_name text NOT NULL,
    result text NOT NULL
) ON COMMIT DROP;

CREATE FUNCTION pg_temp.check_crud(test_name text, condition boolean)
RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    IF condition IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'FAIL: %', test_name;
    END IF;
    INSERT INTO pg_temp.crud_results (test_name, result) VALUES (test_name, 'PASS');
END
$$;

DO $$
<<fixtures>>
DECLARE
    candidate_id uuid := gen_random_uuid();
    candidate_user_id uuid := gen_random_uuid();
    company_id uuid := gen_random_uuid();
    company_user_id uuid := gen_random_uuid();
    offer_id uuid := gen_random_uuid();
    application_id uuid := gen_random_uuid();
    offer_event_id uuid := gen_random_uuid();
    application_event_id text := gen_random_uuid()::text;
    offer_event_order bigint;
    application_sequence_id bigint;
    affected integer;
    expected_time timestamptz := '2026-01-15 12:30:45.123456+00';
BEGIN
    INSERT INTO profiles.candidates (
        id, user_id, first_name, last_name, phone, city, country, job_title,
        summary, cv_url, linkedin_url, portfolio_url, birth_date,
        open_to_remote, open_to_relocation, created_at, updated_at
    ) VALUES (
        candidate_id, candidate_user_id, 'CRUD', 'Candidate', '0000000000',
        'Paris', 'France', 'Java developer', 'SQL test fixture',
        'https://example.invalid/cv', 'https://example.invalid/linkedin',
        'https://example.invalid/portfolio', DATE '1990-05-10',
        true, false, expected_time, expected_time
    );
    GET DIAGNOSTICS affected = ROW_COUNT;
    PERFORM pg_temp.check_crud('profiles.candidates CREATE', affected = 1);
    PERFORM pg_temp.check_crud('profiles.candidates READ', (
        SELECT first_name = 'CRUD' AND city = 'Paris' AND open_to_remote
               AND NOT open_to_relocation AND birth_date = DATE '1990-05-10'
               AND cv_url = 'https://example.invalid/cv' AND created_at = expected_time
        FROM profiles.candidates WHERE user_id = candidate_user_id AND id = candidate_id
    ));
    UPDATE profiles.candidates
    SET city = 'Lyon', open_to_relocation = true, updated_at = expected_time + interval '1 minute'
    WHERE id = candidate_id;
    GET DIAGNOSTICS affected = ROW_COUNT;
    PERFORM pg_temp.check_crud('profiles.candidates UPDATE', affected = 1 AND (
        SELECT city = 'Lyon' AND open_to_relocation
               AND updated_at > created_at FROM profiles.candidates WHERE id = candidate_id
    ));

    INSERT INTO profiles.companies (
        id, user_id, name, siret, description, website, sector, country, city,
        logo_url, contact_email, contact_phone, created_at, updated_at
    ) VALUES (
        company_id, company_user_id, 'CRUD Company', '00000000000000', 'SQL fixture',
        'https://example.invalid', 'IT', 'France', 'Paris',
        'https://example.invalid/logo', 'company@example.invalid', '0000000000',
        expected_time, expected_time
    );
    GET DIAGNOSTICS affected = ROW_COUNT;
    PERFORM pg_temp.check_crud('profiles.companies CREATE', affected = 1);
    PERFORM pg_temp.check_crud('profiles.companies READ', (
        SELECT name = 'CRUD Company' AND contact_email = 'company@example.invalid'
               AND created_at = expected_time
        FROM profiles.companies WHERE id = company_id AND user_id = company_user_id
    ));
    UPDATE profiles.companies SET name = 'CRUD Company Updated', updated_at = expected_time + interval '1 minute'
    WHERE id = company_id;
    GET DIAGNOSTICS affected = ROW_COUNT;
    PERFORM pg_temp.check_crud('profiles.companies UPDATE', affected = 1 AND (
        SELECT name = 'CRUD Company Updated' AND updated_at > created_at
        FROM profiles.companies WHERE id = company_id
    ));

    INSERT INTO joboffers.job_offers (
        id, company_id, company_name, title, description, job_category, specialty,
        contract_type, remote, country, region, department, city, salary_min,
        salary_max, start_date, expires_at, active, created_at, updated_at
    ) VALUES (
        offer_id, company_id, 'CRUD Company Updated', 'CRUD Java role',
        repeat('Long job description. ', 100), 'IT', 'Java', 'CDI', true,
        'France', 'Ile-de-France', '75', 'Paris', 42000.50, 55000.75,
        DATE '2026-02-01', DATE '2026-12-31', true, expected_time, expected_time
    );
    GET DIAGNOSTICS affected = ROW_COUNT;
    PERFORM pg_temp.check_crud('joboffers.job_offers CREATE', affected = 1);
    PERFORM pg_temp.check_crud('joboffers.job_offers READ', (
        SELECT o.company_id = fixtures.company_id AND o.title = 'CRUD Java role'
               AND salary_min = 42000.50 AND salary_max = 55000.75
               AND start_date = DATE '2026-02-01' AND length(description) > 255
               AND remote AND active AND contract_type = 'CDI'
        FROM joboffers.job_offers o WHERE id = offer_id
    ));
    UPDATE joboffers.job_offers SET active = false, salary_max = 60000.25,
        updated_at = expected_time + interval '1 minute' WHERE id = offer_id;
    GET DIAGNOSTICS affected = ROW_COUNT;
    PERFORM pg_temp.check_crud('joboffers.job_offers UPDATE', affected = 1 AND (
        SELECT NOT active AND salary_max = 60000.25 AND updated_at > created_at
        FROM joboffers.job_offers WHERE id = offer_id
    ));

    INSERT INTO applications.applications (
        id, job_offer_id, candidate_id, candidate_email, company_email, job_title,
        company_id, cover_letter, cv_url, status, submitted_at, updated_at
    ) VALUES (
        application_id, offer_id, candidate_id, 'candidate@example.invalid',
        'company@example.invalid', 'CRUD Java role', company_id,
        repeat('Cover letter. ', 100), 'https://example.invalid/cv',
        'SUBMITTED', expected_time, expected_time
    );
    GET DIAGNOSTICS affected = ROW_COUNT;
    PERFORM pg_temp.check_crud('applications.applications CREATE', affected = 1);
    PERFORM pg_temp.check_crud('applications.applications READ', (
        SELECT a.candidate_id = fixtures.candidate_id AND job_offer_id = offer_id
               AND a.company_id = fixtures.company_id AND status = 'SUBMITTED'
               AND length(cover_letter) > 255 AND submitted_at = expected_time
        FROM applications.applications a WHERE id = application_id
    ));
    UPDATE applications.applications SET status = 'WITHDRAWN',
        updated_at = expected_time + interval '1 minute' WHERE id = application_id;
    GET DIAGNOSTICS affected = ROW_COUNT;
    PERFORM pg_temp.check_crud('applications.applications UPDATE / withdrawal', affected = 1 AND (
        SELECT status = 'WITHDRAWN' AND updated_at > submitted_at
        FROM applications.applications WHERE id = application_id
    ));
    PERFORM pg_temp.check_crud('Cross-schema reporting JOIN', (
        SELECT count(*) = 1
        FROM applications.applications a
        JOIN profiles.candidates c ON c.id = a.candidate_id
        JOIN joboffers.job_offers o ON o.id = a.job_offer_id
        JOIN profiles.companies co ON co.id = a.company_id AND co.id = o.company_id
        WHERE a.id = application_id AND c.user_id = candidate_user_id
          AND co.user_id = company_user_id AND o.title = a.job_title
    ));

    INSERT INTO joboffers.job_offer_outbox (
        event_id, aggregate_id, topic, payload, created_at
    ) VALUES (offer_event_id, offer_id, 'job-offer.events',
        json_build_object('eventId', offer_event_id, 'type', 'UPDATED')::text, expected_time)
    RETURNING event_order INTO offer_event_order;
    PERFORM pg_temp.check_crud('joboffers.job_offer_outbox CREATE / identity', offer_event_order > 0);
    PERFORM pg_temp.check_crud('joboffers.job_offer_outbox READ', (
        SELECT aggregate_id = offer_id AND topic = 'job-offer.events'
               AND payload::jsonb ->> 'eventId' = offer_event_id::text AND published_at IS NULL
        FROM joboffers.job_offer_outbox WHERE event_order = offer_event_order
    ));
    UPDATE joboffers.job_offer_outbox SET published_at = expected_time + interval '1 minute'
    WHERE event_id = offer_event_id;
    GET DIAGNOSTICS affected = ROW_COUNT;
    PERFORM pg_temp.check_crud('joboffers.job_offer_outbox UPDATE / published', affected = 1 AND (
        SELECT published_at > created_at FROM joboffers.job_offer_outbox WHERE event_id = offer_event_id
    ));

    INSERT INTO applications.application_outbox (
        event_id, aggregate_id, payload, created_at, attempt_count, next_attempt_at
    ) VALUES (application_event_id, application_id::text,
        json_build_object('eventId', application_event_id, 'type', 'WITHDRAWN')::text,
        expected_time, 0, expected_time)
    RETURNING sequence_id INTO application_sequence_id;
    PERFORM pg_temp.check_crud('applications.application_outbox CREATE / identity', application_sequence_id > 0);
    PERFORM pg_temp.check_crud('applications.application_outbox READ', (
        SELECT aggregate_id = application_id::text AND attempt_count = 0
               AND payload::jsonb ->> 'eventId' = application_event_id AND published_at IS NULL
        FROM applications.application_outbox WHERE sequence_id = application_sequence_id
    ));
    UPDATE applications.application_outbox SET attempt_count = attempt_count + 1,
        last_error = 'Simulated failure', next_attempt_at = expected_time + interval '1 minute'
    WHERE event_id = application_event_id;
    GET DIAGNOSTICS affected = ROW_COUNT;
    PERFORM pg_temp.check_crud('applications.application_outbox UPDATE / retry', affected = 1 AND (
        SELECT attempt_count = 1 AND last_error = 'Simulated failure' AND next_attempt_at > created_at
        FROM applications.application_outbox WHERE event_id = application_event_id
    ));
    UPDATE applications.application_outbox SET published_at = expected_time + interval '2 minutes',
        last_error = NULL WHERE event_id = application_event_id;
    PERFORM pg_temp.check_crud('applications.application_outbox publication', (
        SELECT published_at IS NOT NULL AND last_error IS NULL
        FROM applications.application_outbox WHERE event_id = application_event_id
    ));

    -- A missing constraint must fail the test, not be swallowed as an expected SQL error.
    BEGIN
        INSERT INTO profiles.candidates (
            id, user_id, first_name, last_name, open_to_remote, open_to_relocation
        ) VALUES (gen_random_uuid(), candidate_user_id, 'Duplicate', 'Candidate', true, false);
        RAISE EXCEPTION 'FAIL: duplicate candidate user_id accepted';
    EXCEPTION WHEN unique_violation THEN
        PERFORM pg_temp.check_crud('Candidate user_id uniqueness', true);
    END;
    BEGIN
        INSERT INTO profiles.companies (id, user_id, name)
        VALUES (gen_random_uuid(), company_user_id, 'Duplicate Company');
        RAISE EXCEPTION 'FAIL: duplicate company user_id accepted';
    EXCEPTION WHEN unique_violation THEN
        PERFORM pg_temp.check_crud('Company user_id uniqueness', true);
    END;
    BEGIN
        INSERT INTO applications.applications (
            id, job_offer_id, candidate_id, status, submitted_at, updated_at
        ) VALUES (gen_random_uuid(), offer_id, candidate_id, 'SUBMITTED', expected_time, expected_time);
        RAISE EXCEPTION 'FAIL: duplicate offer/candidate pair accepted';
    EXCEPTION WHEN unique_violation THEN
        PERFORM pg_temp.check_crud('One application per candidate and offer', true);
    END;
    BEGIN
        UPDATE joboffers.job_offers SET contract_type = 'INVALID' WHERE id = offer_id;
        RAISE EXCEPTION 'FAIL: invalid contract type accepted';
    EXCEPTION WHEN check_violation THEN
        PERFORM pg_temp.check_crud('Contract type validation', true);
    END;
    BEGIN
        UPDATE applications.applications SET status = 'INVALID' WHERE id = application_id;
        RAISE EXCEPTION 'FAIL: invalid application status accepted';
    EXCEPTION WHEN check_violation THEN
        PERFORM pg_temp.check_crud('Application status validation', true);
    END;
    BEGIN
        UPDATE profiles.candidates SET first_name = NULL WHERE id = candidate_id;
        RAISE EXCEPTION 'FAIL: null candidate first_name accepted';
    EXCEPTION WHEN not_null_violation THEN
        PERFORM pg_temp.check_crud('Required candidate first_name', true);
    END;
    BEGIN
        UPDATE profiles.companies SET name = NULL WHERE id = company_id;
        RAISE EXCEPTION 'FAIL: null company name accepted';
    EXCEPTION WHEN not_null_violation THEN
        PERFORM pg_temp.check_crud('Required company name', true);
    END;
    BEGIN
        UPDATE joboffers.job_offers SET title = NULL WHERE id = offer_id;
        RAISE EXCEPTION 'FAIL: null offer title accepted';
    EXCEPTION WHEN not_null_violation THEN
        PERFORM pg_temp.check_crud('Required offer title', true);
    END;
    BEGIN
        UPDATE applications.applications SET submitted_at = NULL WHERE id = application_id;
        RAISE EXCEPTION 'FAIL: null submission timestamp accepted';
    EXCEPTION WHEN not_null_violation THEN
        PERFORM pg_temp.check_crud('Required application submitted_at', true);
    END;
    BEGIN
        INSERT INTO joboffers.job_offer_outbox (event_id, aggregate_id, topic, payload, created_at)
        VALUES (offer_event_id, offer_id, 'job-offer.events', '{}', expected_time);
        RAISE EXCEPTION 'FAIL: duplicate offer event_id accepted';
    EXCEPTION WHEN unique_violation THEN
        PERFORM pg_temp.check_crud('Offer event_id uniqueness', true);
    END;
    BEGIN
        INSERT INTO applications.application_outbox (
            event_id, aggregate_id, payload, created_at, attempt_count, next_attempt_at
        ) VALUES (application_event_id, application_id::text, '{}', expected_time, 0, expected_time);
        RAISE EXCEPTION 'FAIL: duplicate application event_id accepted';
    EXCEPTION WHEN unique_violation THEN
        PERFORM pg_temp.check_crud('Application event_id uniqueness', true);
    END;

    -- PostgreSQL subtransaction: both changes must roll back together on failure.
    BEGIN
        UPDATE joboffers.job_offers SET title = 'Must roll back' WHERE id = offer_id;
        INSERT INTO joboffers.job_offer_outbox (event_id, aggregate_id, topic, payload, created_at)
        VALUES (offer_event_id, offer_id, 'job-offer.events', '{}', expected_time);
        RAISE EXCEPTION 'FAIL: simulated outbox failure did not occur';
    EXCEPTION WHEN unique_violation THEN
        PERFORM pg_temp.check_crud('Offer + outbox SQL rollback', (
            SELECT title = 'CRUD Java role' FROM joboffers.job_offers WHERE id = offer_id
        ));
    END;
    BEGIN
        UPDATE applications.applications SET status = 'ACCEPTED' WHERE id = application_id;
        INSERT INTO applications.application_outbox (
            event_id, aggregate_id, payload, created_at, attempt_count, next_attempt_at
        ) VALUES (application_event_id, application_id::text, '{}', expected_time, 0, expected_time);
        RAISE EXCEPTION 'FAIL: simulated outbox failure did not occur';
    EXCEPTION WHEN unique_violation THEN
        PERFORM pg_temp.check_crud('Application + outbox SQL rollback', (
            SELECT status = 'WITHDRAWN' FROM applications.applications WHERE id = application_id
        ));
    END;

    DELETE FROM joboffers.job_offers WHERE id = offer_id;
    GET DIAGNOSTICS affected = ROW_COUNT;
    PERFORM pg_temp.check_crud('joboffers.job_offers DELETE',
        affected = 1 AND NOT EXISTS (SELECT 1 FROM joboffers.job_offers WHERE id = offer_id));
    DELETE FROM profiles.candidates WHERE id = candidate_id;
    GET DIAGNOSTICS affected = ROW_COUNT;
    PERFORM pg_temp.check_crud('profiles.candidates DELETE',
        affected = 1 AND NOT EXISTS (SELECT 1 FROM profiles.candidates WHERE id = candidate_id));
    DELETE FROM profiles.companies WHERE id = company_id;
    GET DIAGNOSTICS affected = ROW_COUNT;
    PERFORM pg_temp.check_crud('profiles.companies DELETE',
        affected = 1 AND NOT EXISTS (SELECT 1 FROM profiles.companies WHERE id = company_id));
    PERFORM pg_temp.check_crud('History survives deleted profiles / offers', (
        SELECT count(*) = 1
        FROM applications.applications a
        LEFT JOIN profiles.candidates c ON c.id = a.candidate_id
        LEFT JOIN profiles.companies co ON co.id = a.company_id
        LEFT JOIN joboffers.job_offers o ON o.id = a.job_offer_id
        WHERE a.id = application_id AND c.id IS NULL AND co.id IS NULL AND o.id IS NULL
          AND a.job_title = 'CRUD Java role'
    ));
    DELETE FROM applications.applications WHERE id = application_id;
    GET DIAGNOSTICS affected = ROW_COUNT;
    PERFORM pg_temp.check_crud('applications.applications DELETE',
        affected = 1 AND NOT EXISTS (SELECT 1 FROM applications.applications WHERE id = application_id));
    PERFORM pg_temp.check_crud('Outbox survives deleted aggregates',
        EXISTS (SELECT 1 FROM joboffers.job_offer_outbox WHERE event_id = offer_event_id)
        AND EXISTS (SELECT 1 FROM applications.application_outbox WHERE event_id = application_event_id));
    DELETE FROM joboffers.job_offer_outbox WHERE event_id = offer_event_id;
    GET DIAGNOSTICS affected = ROW_COUNT;
    PERFORM pg_temp.check_crud('joboffers.job_offer_outbox DELETE',
        affected = 1 AND NOT EXISTS (SELECT 1 FROM joboffers.job_offer_outbox WHERE event_id = offer_event_id));
    DELETE FROM applications.application_outbox WHERE event_id = application_event_id;
    GET DIAGNOSTICS affected = ROW_COUNT;
    PERFORM pg_temp.check_crud('applications.application_outbox DELETE',
        affected = 1 AND NOT EXISTS (SELECT 1 FROM applications.application_outbox WHERE event_id = application_event_id));
END
$$;

SELECT test_name, result FROM pg_temp.crud_results ORDER BY test_order;
SELECT count(*) AS tests_passed FROM pg_temp.crud_results;

ROLLBACK;
