---------------------------------------------------------------------------------
-- Test truncate table
---------------------------------------------------------------------------------
SET CURRENT SCHEMA = 'DB2TEST';
SET CURRENT PATH = 'DB2TEST';

CREATE TABLE "TEST_TRUNCATE" ("OBJ_ID" BIGINT NOT NULL, "TEXT"VARCHAR(30));

INSERT INTO "TEST_TRUNCATE" ("OBJ_ID", "TEXT") VALUES (1, "TEST");

-- truncate table
TRUNCATE TABLE TEST_TRUNCATE reuse storage immediate;
