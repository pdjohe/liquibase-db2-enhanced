---------------------------------------------------------------------------------
-- Another test file
---------------------------------------------------------------------------------
SET CURRENT SCHEMA = 'DB2TEST';
SET CURRENT PATH = 'DB2TEST';

--#SET TERMINATOR @
CREATE FUNCTION TEST_FUNC1(PARAM1 INTEGER, PARAM2 INTEGER)
  RETURNS INTEGER
  BEGIN
    IF (PARAM1 < PARAM2) THEN
       return 1;
    ELSE
       RETURN 0;
    END IF;
  END @

CREATE FUNCTION TEST_FUNC2(PARAM1 INTEGER, PARAM2 INTEGER)
  RETURNS INTEGER
  BEGIN
    IF (PARAM1 < PARAM2) THEN
       return 1;
    ELSE
       RETURN 0;
    END IF;
  END@

CREATE FUNCTION TEST_FUNC3(PARAM1 INTEGER, PARAM2 INTEGER)
  RETURNS INTEGER
  BEGIN
    IF (PARAM1 < PARAM2) THEN
       return 1;
    ELSE
       RETURN 0;
    END IF;
  END
@