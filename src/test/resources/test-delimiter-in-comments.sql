-- 1 start
SET CURRENT SCHEMA = 'DB2TEST';
-- 2 start
SET CURRENT PATH = 'DB2TEST';

-- 3 start
--<ScriptOptions statementTerminator=";/>  Dummy delimiter that is not a part of the last statement

--#SET TERMINATOR @
CREATE FUNCTION TEST_FUNC(PARAM1 INTEGER, PARAM2 INTEGER)
  RETURNS INTEGER
  BEGIN
    IF (PARAM1 < PARAM2) THEN
       return 1;
    ELSE
       RETURN 0;
    END IF;
  END @

-- 4 start
--#SET TERMINATOR ;

SET CURRENT SCHEMA = 'DEFAULT';

-- 5 start
--<ScriptOptions statementTerminator=";/> -- Dummy delimiter and dummy comment that is not a part of the last statement

--#SET TERMINATOR @
CREATE FUNCTION TEST_FUNC2(PARAM1 INTEGER, PARAM2 INTEGER)
  RETURNS INTEGER
  BEGIN
    IF (PARAM1 < PARAM2) THEN
       return 1;
    ELSE
       RETURN 0;
    END IF;
  END @

-- 6 start

--#SET TERMINATOR ;
/* multiline
 * ; @
 */
SET CURRENT SCHEMA = 'DEFAULT';

-- 7 start

--#SET TERMINATOR @
CREATE FUNCTION TEST_FUNC2(PARAM1 INTEGER, PARAM2 INTEGER)
  RETURNS INTEGER
  BEGIN
    IF (PARAM1 < PARAM2) THEN
       return 1;
    ELSE
       RETURN 0;
    END IF;
  END @

/* multiline
 * @ ;
 */

-- Final comment