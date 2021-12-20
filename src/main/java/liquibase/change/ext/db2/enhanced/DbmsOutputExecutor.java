package liquibase.change.ext.db2.enhanced;

import liquibase.Scope;
import liquibase.database.DatabaseConnection;
import liquibase.database.OfflineConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.executor.jvm.JdbcExecutor;
import liquibase.executor.jvm.RowMapper;
import liquibase.logging.Logger;
import liquibase.sql.visitor.SqlVisitor;
import liquibase.statement.SqlStatement;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class DbmsOutputExecutor extends JdbcExecutor {

    private boolean isDbmsOutputEnabled = true;

    public boolean isDbmsOutputEnabled() {
        return isDbmsOutputEnabled;
    }

    public void setDbmsOutputEnabled(boolean isDbmsOutputEnabled) {
        this.isDbmsOutputEnabled = isDbmsOutputEnabled;
    }

    @Override
    public void execute(final SqlStatement sql) throws DatabaseException {
        super.execute(sql);
        logDbmsOutput();
    }

    @Override
    public void execute(final SqlStatement sql, final List<SqlVisitor> sqlVisitors) throws DatabaseException {
        super.execute(sql, sqlVisitors);
        logDbmsOutput();
    }

    @Override
    public List query(SqlStatement sql, RowMapper rowMapper, List<SqlVisitor> sqlVisitors) throws DatabaseException {
        List<?> ret = super.query(sql, rowMapper, sqlVisitors);
        logDbmsOutput();
        return ret;
    }

    private void logDbmsOutput() throws DatabaseException {
        if (isDbmsOutputEnabled()) {
            dbmsOutputGetLines().stream().forEach(getLogger()::info);
        }
    }

    private Logger getLogger() {
        return Scope.getCurrentScope().getLog(getClass());
    }

    private List<String> dbmsOutputGetLines() throws DatabaseException {
        DatabaseConnection con = database.getConnection();

        if (con instanceof OfflineConnection) {
            throw new DatabaseException("Cannot execute commands against an offline database");
        }

        List<String> ret = new ArrayList<>();

        while(true) {
            try (CallableStatement stmt = ((JdbcConnection) con).getUnderlyingConnection().prepareCall("{ CALL SYSIBMADM.DBMS_OUTPUT.GET_LINE(?,?) }")) {
                stmt.registerOutParameter(1, Types.VARCHAR);
                stmt.registerOutParameter(2, Types.INTEGER);
                stmt.execute();

                if (stmt.getInt(2) == 0) {
                    ret.add(stmt.getString(1));
                } else {
                    return ret;
                }
            } catch (SQLException ex) {
                throw new DatabaseException("Error executing SQL CALL SYSIBMADM.DBMS_OUTPUT.GET_LINES(?,?): " + ex.getMessage(), ex);
            }
        }
    }


}
