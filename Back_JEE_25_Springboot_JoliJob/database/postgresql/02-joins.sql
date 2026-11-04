-- DBeaver: run on jolijob_db. These are read-only reporting queries.
-- LEFT JOIN preserves applications even if a remote profile/offer has been deleted.
SELECT a.id AS application_id,
       a.status,
       a.submitted_at,
       c.id AS candidate_id,
       c.first_name,
       c.last_name,
       a.candidate_email,
       a.job_offer_id,
       COALESCE(o.title, a.job_title) AS job_title,
       a.company_id,
       co.name AS company_name,
       a.company_email
FROM applications.applications a
LEFT JOIN profiles.candidates c ON c.id = a.candidate_id
LEFT JOIN joboffers.job_offers o ON o.id = a.job_offer_id
LEFT JOIN profiles.companies co ON co.id = a.company_id
ORDER BY a.submitted_at DESC;

-- Offers, their company and application counts (including offers with no applications).
SELECT o.id AS job_offer_id,
       o.title,
       o.contract_type,
       o.active,
       COALESCE(co.name, o.company_name) AS company_name,
       count(a.id) AS application_count
FROM joboffers.job_offers o
LEFT JOIN profiles.companies co ON co.id = o.company_id
LEFT JOIN applications.applications a ON a.job_offer_id = o.id
GROUP BY o.id, co.name
ORDER BY o.created_at DESC;

-- Pending outbox events; no join to the aggregate because it may have been deleted.
SELECT 'joboffers' AS service, count(*) AS pending_events
FROM joboffers.job_offer_outbox
WHERE published_at IS NULL
UNION ALL
SELECT 'applications', count(*)
FROM applications.application_outbox
WHERE published_at IS NULL;
