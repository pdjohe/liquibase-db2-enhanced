---------------------------------------------------------------------------------
-- Test reorg table
---------------------------------------------------------------------------------
SET CURRENT SCHEMA = 'DB2TEST';
SET CURRENT PATH = 'DB2TEST';

CREATE TABLE "TEST_REORG" (
			  "OBJ_ID" 			BIGINT NOT NULL,
			  "TEXT"		    VARCHAR(30),
			  "IS_TEXT_NULL" 	INTEGER NOT NULL DEFAULT 1);

COMMENT ON TABLE "TEST_REORG" IS 'Test table';

ALTER TABLE TEST_REORG ALTER COLUMN TEXT SET DATA TYPE VARCHAR(50);

REORG TABLE TEST_REORG;
