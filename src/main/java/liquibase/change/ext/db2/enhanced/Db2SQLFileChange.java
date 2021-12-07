package liquibase.change.ext.db2.enhanced;

import liquibase.Scope;
import liquibase.change.ChangeMetaData;
import liquibase.change.DatabaseChange;
import liquibase.change.DatabaseChangeProperty;
import liquibase.change.core.SQLFileChange;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.logging.Logger;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.RawSqlStatement;
import liquibase.util.SqlParser;
import liquibase.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The 'db2SqlFile' is an extension that parses DB2 SQL files a bit better.
 * (For example '--#SET TERMINATOR @' which will switch the delimiter within the script)
 */
@DatabaseChange(name = "db2SqlFile",
        description = "The 'db2SqlFile' is an extension that parses DB2 SQL files a bit better\n" +
                "(For example '--#SET TERMINATOR @' which will switch the delimiter within the script)\n",
        priority = ChangeMetaData.PRIORITY_DEFAULT)
public class Db2SQLFileChange extends SQLFileChange {

    private static final String DB2_DELIMITER_COMMAND = "--#SET TERMINATOR ";

    private static final String REORG_TABLE_COMMAND = "REORG TABLE ";

    private boolean useSetTerminatorComments = true;

    private boolean rewriteReorgTableStatements = true;

    private boolean commitBeforeTruncate = true;

    /**
     * If true, 'REORG TABLE X' will be re-written to be a ADMIN_CMD so that the JDBC driver can execute it.
     *
     * @return Boolean, Liquibase requires a Boolean, but this will never be null
     */
    @DatabaseChangeProperty(description = "If true, 'REORG TABLE X' will be re-written to be a ADMIN_CMD " +
            "so that the JDBC driver can execute it. " +
            "Default is true.")
    public Boolean isRewriteReorgTableStatements() {
        return rewriteReorgTableStatements;
    }

    /**
     * @see #isRewriteReorgTableStatements()
     * @param rewriteReorgTableStatements if null, this defaults to true
     */
    public void setRewriteReorgTableStatements(Boolean rewriteReorgTableStatements) {
        this.rewriteReorgTableStatements = Optional.ofNullable(rewriteReorgTableStatements).orElse(true);
    }

    /**
     * If true, when 'TRUNCATE TABLE ...' is found but missing a COMMIT just before it, COMMIT will be automatically
     * added just before the truncate table command.
     *
     * @return Boolean, Liquibase requires a Boolean, but this will never be null
     */
    @DatabaseChangeProperty(description = "If true, when 'TRUNCATE TABLE ...' is found but missing a COMMIT " +
            "just before it, COMMIT will be automatically added just before the truncate table command. " +
            "Default is true.")
    public Boolean isCommitBeforeTruncate() {
        return commitBeforeTruncate;
    }

    /**
     * @see #isCommitBeforeTruncate()
     * @param commitBeforeTruncate if null, this defaults to true
     */
    public void setCommitBeforeTruncate(Boolean commitBeforeTruncate) {
        this.commitBeforeTruncate = Optional.ofNullable(commitBeforeTruncate).orElse(true);
    }

    /**
     * If true, --#SET TERMINATOR comments will be properly executed.
     *
     * @return Boolean, Liquibase requires a Boolean, but this will never be null
     */
    @DatabaseChangeProperty(description = "If true, --#SET TERMINATOR comments will be properly executed." +
            "Default is true.")
    public Boolean isUseSetTerminatorComments() {
        return useSetTerminatorComments;
    }

    /**
     * @see #isUseSetTerminatorComments()
     * @param useSetTerminatorComments if null, this defaults to true
     */
    public void setUseSetTerminatorComments(Boolean useSetTerminatorComments) {
        this.useSetTerminatorComments = Optional.ofNullable(useSetTerminatorComments).orElse(true);
    }

    public String getSerializedObjectNamespace() {
        return GENERIC_CHANGELOG_EXTENSION_NAMESPACE;
    }

    @Override
    public SqlStatement[] generateStatements(Database database) {
        getLogger().fine("Running Db2SQLFileChange");

        String sql = StringUtil.trimToNull(getSql());
        if (sql == null) {
            return new SqlStatement[0];
        }

        List<DelimitedSegment> segments = getDelimitedSegments(sql);
        List<SqlStatement> returnStatements = getSqlStatements(database, segments);

        return returnStatements.toArray(new SqlStatement[0]);
    }

    private List<DelimitedSegment> getDelimitedSegments(String sql) {
        getLogger().fine("Parsing SQL in "+this.getPath()
                + ", useSetTerminatorCommentTags: " + isUseSetTerminatorComments()
                + ", rewriteOrgTable:"+isRewriteReorgTableStatements()
                + ", commitBeforeTruncate= "+ isCommitBeforeTruncate());
        String processedSQL = normalizeLineEndings(sql);
        final List<DelimitedSegment> segments = new ArrayList<>();
        DelimitedSegment currentSegment = new DelimitedSegment(this.getEndDelimiter(), "");
        for (String line : processedSQL.split("\n")) {
            if (isUseSetTerminatorComments() && line.toUpperCase().startsWith(DB2_DELIMITER_COMMAND)) {
                String previousSqlAfterLastDelimiterChange = currentSegment.getAndRemoveAfterLastDelimiter();
                String delimiter = line.substring(DB2_DELIMITER_COMMAND.length()).trim();
                segments.add(currentSegment);
                currentSegment = new DelimitedSegment(delimiter, previousSqlAfterLastDelimiterChange);
            }
            currentSegment.append(line).append("\n");
        }
        segments.add(currentSegment);
        getLogger().fine("DelimitedSegments: "+segments);
        return segments;
    }

    private List<SqlStatement> getSqlStatements(Database database, List<DelimitedSegment> segments) {
        List<SqlStatement> sqlStatements = new ArrayList<>();
        for (DelimitedSegment segment : segments) {
            for (String statement : StringUtil.processMultiLineSQL(segment.getSql(), isStripComments(), isSplitStatements(), segment.getDelimiter())) {
                String escapedStatement = statement;
                try {
                    if (database.getConnection() != null) {
                        escapedStatement = database.getConnection().nativeSQL(statement);
                    }
                } catch (DatabaseException e) {
                    escapedStatement = statement;
                }
                if (hasSql(escapedStatement)) {
                    escapedStatement = refactorForJdbc(escapedStatement, sqlStatements);
                    sqlStatements.add(new RawSqlStatement(escapedStatement, getEndDelimiter()));
                }
            }
        }
        getLogger().fine("SqlStatements: "+sqlStatements);
        return sqlStatements;
    }

    private String refactorForJdbc(String statement, List<SqlStatement> sqlStatements) {
        if (isRewriteReorgTableStatements() && statement.startsWith(REORG_TABLE_COMMAND)) {
            return "CALL SYSPROC.ADMIN_CMD ('REORG TABLE "+statement.substring(REORG_TABLE_COMMAND.length())+"')";
        }
        int size = sqlStatements.size();
        if (isCommitBeforeTruncate() && size > 0 && statement.contains("TRUNCATE TABLE ")) {
            RawSqlStatement previous = (RawSqlStatement)sqlStatements.get(size - 1);
            if (!previous.getSql().equalsIgnoreCase("COMMIT")) {
                getLogger().warning("TRUNCATE TABLE should be at the start of a unit of work, so committing before truncate");
                // DB2 Truncate table must start a unit of work
                sqlStatements.add(new RawSqlStatement("COMMIT"));
            }
        }
        return statement;
    }

    private boolean hasSql(String statement) {
        return !Arrays.stream(SqlParser.parse(statement, false, false).toArray(true))
                .map(Objects::toString)
                .collect(Collectors.joining())
                .isEmpty();
    }

    private Logger getLogger() {
        return Scope.getCurrentScope().getLog(getClass());
    }

}
