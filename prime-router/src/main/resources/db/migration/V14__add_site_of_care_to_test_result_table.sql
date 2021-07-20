/*
This SQL creates the tables of the DB. The Flyway tool applies this migration to create the database
Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema
Copy a version of this comment into the next migration
*/


/*
 * This SQL adds 'site_of_care' to the covid_result_metadata table
 */

ALTER TABLE covid_result_metadata ADD COLUMN site_of_care VARCHAR(128) NULL;

UPDATE
    covid_result_metadata
SET
    site_of_care = null
WHERE
    TRIM(site_of_care) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT ck_site_of_care
        CHECK (site_of_care IS NULL OR LENGTH(site_of_care) > 0);
