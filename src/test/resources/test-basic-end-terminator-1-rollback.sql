-- Some comment at the top

SET CURRENT SCHEMA = 'DB2TEST';
SET CURRENT PATH = 'DB2TEST';

--#SET TERMINATOR @
BEGIN
  IF EXISTS (SELECT ROUTINENAME FROM SYSIBM.SYSROUTINES
    WHERE routineschema = 'DB2TEST' AND ROUTINENAME = 'TEST_FUNC1') THEN
      PREPARE stmt FROM 'DROP FUNCTION DB2TEST.TEST_FUNC1';
      EXECUTE stmt;
  END if;
END
@

BEGIN
  IF EXISTS (SELECT ROUTINENAME FROM SYSIBM.SYSROUTINES
    WHERE routineschema = 'DB2TEST' AND ROUTINENAME = 'TEST_FUNC2') THEN
      PREPARE stmt FROM 'DROP FUNCTION DB2TEST.TEST_FUNC2';
      EXECUTE stmt;
  END if;
END
@

BEGIN
  IF EXISTS (SELECT ROUTINENAME FROM SYSIBM.SYSROUTINES
    WHERE routineschema = 'DB2TEST' AND ROUTINENAME = 'TEST_FUNC3') THEN
      PREPARE stmt FROM 'DROP FUNCTION DB2TEST.TEST_FUNC3';
      EXECUTE stmt;
  END if;
END
@

--- End ---
