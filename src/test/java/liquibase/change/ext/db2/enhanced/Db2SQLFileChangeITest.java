package liquibase.change.ext.db2.enhanced;

import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.change.ChangeFactory;
import liquibase.change.ChangeMetaData;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.OfflineConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

class Db2SQLFileChangeITest extends AbstractTest {

    private static final File OFFLINE_DB = new File("./target/databasechangelog.csv");
    private static final String OFFLINE_URL = "offline:db2i?changeLogFile=" + OFFLINE_DB.getPath();
    private static final String ONLINE_URL = "jdbc:db2://localhost:50000/DB2TEST:retrieveMessagesFromServerOnGetMessage=true;";

    @BeforeEach
    void beforeEach() {
        if (OFFLINE_DB.exists() && !OFFLINE_DB.delete()) {
            throw new RuntimeException("Could not delete " + OFFLINE_DB);
        }
    }

    @Test
    void testMetaDataChange() {
        ChangeMetaData changeMetaData = Scope.getCurrentScope().getSingleton(ChangeFactory.class).getChangeMetaData(new Db2SQLFileChange());

        Assertions.assertThat(changeMetaData.getName()).isEqualTo("db2SqlFile");
        Assertions.assertThat(changeMetaData.getDescription()).contains("The 'db2SqlFile' is an extension");
    }

    @Test
    void checkPluginGetsInjectedIntoLiquibaseFramework() throws LiquibaseException {

        String changeLogFile = "dbchanges-test-1.xml";

        OfflineConnection offline = new OfflineConnection(OFFLINE_URL, Scope.getCurrentScope().getResourceAccessor());
        Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(offline);
        Liquibase liquibase = new Liquibase(changeLogFile, Scope.getCurrentScope().getResourceAccessor(), db);

        Writer writer = new StringWriter();
        liquibase.update("", writer);
        liquibase.close();

        liquibase.validate();

        String sqlExecuted = writer.toString();
        logger.debug("SQL executed: {}", sqlExecuted);

        Assertions.assertThat(sqlExecuted)
                .contains(changeLogFile)
                .contains("CREATE TABLE \"TEST\"");
    }

    @Test
    void checkPluginGetsInjectedIntoLiquibaseFrameworkAndRollback() throws LiquibaseException {

        String changeLogFile = "dbchanges-test-1.xml";

        OfflineConnection offline = new OfflineConnection(OFFLINE_URL, Scope.getCurrentScope().getResourceAccessor());
        Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(offline);
        Liquibase liquibase = new Liquibase(changeLogFile, Scope.getCurrentScope().getResourceAccessor(), db);

        Writer writer = new StringWriter();
        liquibase.update("", writer);
        liquibase.validate();
        liquibase.futureRollbackSQL(writer);
        liquibase.close();

        String sqlExecuted = writer.toString();
        logger.debug("SQL executed: {}", sqlExecuted);

        Assertions.assertThat(sqlExecuted)
                .contains(changeLogFile)
                .contains("CREATE TABLE \"TEST\"")
                .contains("DROP TABLE");
    }

    @Test
    void checkPluginParamsGetsInjectedIntoLiquibaseFramework() throws LiquibaseException {

        String changeLogFile = "dbchanges-test-2.xml";

        OfflineConnection offline = new OfflineConnection(OFFLINE_URL, Scope.getCurrentScope().getResourceAccessor());
        Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(offline);
        Liquibase liquibase = new Liquibase(changeLogFile, Scope.getCurrentScope().getResourceAccessor(), db);

        Writer writer = new StringWriter();
        liquibase.update("", writer);
        liquibase.validate();
        liquibase.close();

        String sqlExecuted = writer.toString();
        logger.debug("SQL executed: {}", sqlExecuted);

        Assertions.assertThat(sqlExecuted)
                .contains(changeLogFile)
                .contains("CREATE FUNCTION TEST_FUNC1")
                .contains("END @")
                .doesNotContain("SYSPROC.ADMIN_CMD")
                .doesNotContain("COMMIT")
                ;
    }

    @Test
    void onlineTest() throws SQLException, LiquibaseException {
        String changeLogFile = "dbchanges-test-1.xml";

        Assumptions.assumeTrue(isDb2StartedLocally(), "DB2 is not running locally (see docker-compose.yml). Skipping online test");

        try (Connection connection = DriverManager.getConnection(ONLINE_URL, "DB2INST1", "TESTPA55")) {
            Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(changeLogFile, Scope.getCurrentScope().getResourceAccessor(), db);
            liquibase.update("");
            liquibase.validate();
            liquibase.rollback(Integer.MAX_VALUE, "");
        }
    }

    private static boolean isDb2StartedLocally() {
        try (Socket ignored = new Socket("localhost", 50000)) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

}
