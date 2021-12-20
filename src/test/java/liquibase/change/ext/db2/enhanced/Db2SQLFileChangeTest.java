package liquibase.change.ext.db2.enhanced;

import liquibase.exception.SetupException;
import liquibase.ext.db2i.database.DB2iDatabase;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.RawSqlStatement;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class Db2SQLFileChangeTest extends AbstractTest {

    @Test
    void testBasicParameters() throws SetupException {

        String path = "test-basic-end-terminator-1.sql";
        Db2SQLFileChange db2SQLFileChange = new Db2SQLFileChange();
        db2SQLFileChange.setPath(path);

        Assertions.assertThat(db2SQLFileChange).extracting(
                Db2SQLFileChange::isUseSetTerminatorComments,
                Db2SQLFileChange::isRewriteReorgTableStatements,
                Db2SQLFileChange::isCommitBeforeTruncate,
                Db2SQLFileChange::isDisableAllDbmsOutput,
                Db2SQLFileChange::getPath
        ).containsExactly(
                        true,
                        true,
                        true,
                        false,
                        path
        );

        db2SQLFileChange.setUseSetTerminatorComments(null);
        db2SQLFileChange.setCommitBeforeTruncate(null);
        db2SQLFileChange.setRewriteReorgTableStatements(null);
        db2SQLFileChange.setDisableAllDbmsOutput(null);
        Assertions.assertThat(db2SQLFileChange).extracting(
                Db2SQLFileChange::isUseSetTerminatorComments,
                Db2SQLFileChange::isRewriteReorgTableStatements,
                Db2SQLFileChange::isCommitBeforeTruncate,
                Db2SQLFileChange::isDisableAllDbmsOutput,
                Db2SQLFileChange::getPath
        ).containsExactly(
                true,
                true,
                true,
                false,
                path
        );
        db2SQLFileChange.finishInitialization();
    }

    @Test
    void testDb2ParseTestBasicEndTerminatorSimpleExample() throws SetupException {
        Db2SQLFileChange db2SQLFileChange = new Db2SQLFileChange();
        db2SQLFileChange.setPath("test-basic-end-terminator-1.sql");
        db2SQLFileChange.finishInitialization();
        SqlStatement[] statements = db2SQLFileChange.generateStatements(new DB2iDatabase());
        Assertions.assertThat(statements).hasSize(5);
    }

    @Test
    void testBasicEndTerminatorMoreComplexExample() throws SetupException {
        Db2SQLFileChange db2SQLFileChange = new Db2SQLFileChange();
        db2SQLFileChange.setPath("test-basic-end-terminator-2.sql");
        db2SQLFileChange.finishInitialization();
        SqlStatement[] statements = db2SQLFileChange.generateStatements(new DB2iDatabase());
        Assertions.assertThat(statements).hasSize(15);
    }

    @Test
    void testDelimiterInComments() throws SetupException {
        Db2SQLFileChange db2SQLFileChange = new Db2SQLFileChange();
        db2SQLFileChange.setPath("test-delimiter-in-comments.sql");
        db2SQLFileChange.finishInitialization();
        SqlStatement[] statements = db2SQLFileChange.generateStatements(new DB2iDatabase());
        Assertions.assertThat(statements)
                .filteredOn(RawSqlStatement.class::isInstance)
                .extracting(RawSqlStatement.class::cast)
                .extracting(RawSqlStatement::getSql)
                .extracting(this::getFirstLine)
                .containsExactly(
                        "-- 1 start", "-- 2 start", "-- 3 start", "-- 4 start", "-- 5 start", "-- 6 start", "-- 7 start"
                );
    }

    @Test
    void testTruncateTable() throws SetupException {
        Db2SQLFileChange db2SQLFileChange = new Db2SQLFileChange();
        db2SQLFileChange.setPath("test-truncate-table.sql");
        db2SQLFileChange.finishInitialization();
        SqlStatement[] statements = db2SQLFileChange.generateStatements(new DB2iDatabase());
        Assertions.assertThat(statements)
                .filteredOn(RawSqlStatement.class::isInstance)
                .extracting(RawSqlStatement.class::cast)
                .extracting(RawSqlStatement::getSql)
                .extracting(this::getFirstLine)
                .contains("COMMIT");
    }

    @Test
    void testReorgTable() throws SetupException {
        Db2SQLFileChange db2SQLFileChange = new Db2SQLFileChange();
        db2SQLFileChange.setPath("test-reorg-table.sql");
        db2SQLFileChange.finishInitialization();
        Assertions.assertThat(db2SQLFileChange.isRewriteReorgTableStatements()).isTrue();
        SqlStatement[] statements = db2SQLFileChange.generateStatements(new DB2iDatabase());
        Assertions.assertThat(statements)
                .filteredOn(RawSqlStatement.class::isInstance)
                .extracting(RawSqlStatement.class::cast)
                .extracting(RawSqlStatement::getSql)
                .extracting(this::getFirstLine)
                .contains("CALL SYSPROC.ADMIN_CMD ('REORG TABLE TEST_REORG')");
    }

    @Test
    void testReorgTableDisabled() throws SetupException {
        Db2SQLFileChange db2SQLFileChange = new Db2SQLFileChange();
        db2SQLFileChange.setPath("test-reorg-table.sql");
        db2SQLFileChange.setRewriteReorgTableStatements(false);
        db2SQLFileChange.finishInitialization();
        SqlStatement[] statements = db2SQLFileChange.generateStatements(new DB2iDatabase());
        Assertions.assertThat(statements)
                .filteredOn(RawSqlStatement.class::isInstance)
                .extracting(RawSqlStatement.class::cast)
                .extracting(RawSqlStatement::getSql)
                .extracting(this::getFirstLine)
                .doesNotContain("CALL SYSPROC.ADMIN_CMD ('REORG TABLE TEST_REORG')");
    }

    @Test
    void testDisabledAllDbmsOutput() throws SetupException {
        Db2SQLFileChange db2SQLFileChange = new Db2SQLFileChange();
        db2SQLFileChange.setPath("dbchanges-test-2.xml");
        db2SQLFileChange.setRewriteReorgTableStatements(false);
        db2SQLFileChange.finishInitialization();
        SqlStatement[] statements = db2SQLFileChange.generateStatements(new DB2iDatabase());
        Assertions.assertThat(statements)
                .filteredOn(RawSqlStatement.class::isInstance)
                .extracting(RawSqlStatement.class::cast)
                .extracting(RawSqlStatement::getSql)
                .extracting(this::getFirstLine)
                .doesNotContain("CALL SYSIBMADM.DBMS_OUTPUT.ENABLE(NULL)");
    }

    private String getFirstLine(String s) {
        return s.contains("\n") ? s.substring(0, s.indexOf("\n")) : s;
    }
}
