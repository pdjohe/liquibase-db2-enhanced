package liquibase.change.ext.db2.enhanced;

/**
 * This represents a portion of SQL with the delimiter used to separate the SQL statements.
 */
public class DelimitedSegment {

    private final String delimiter;
    private final StringBuilder segmentSql;

    /**
     * Creates a new delimited segment with the current delimiter used for parsing this segment.
     *
     * @param delimiter, the delimiter of the segment, defaults to ';' if null
     * @param str The initial text for the segment.
     */
    public DelimitedSegment(String delimiter, String str) {
        this.delimiter = delimiter == null ? ";" : delimiter;
        this.segmentSql = new StringBuilder(str);
    }

    /**
     * Gets the delimiter for the current segment
     *
     * @return String, never null (defaults to ';')
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Appends some text to this delimited segment
     * @param str The string to append
     * @return 'this' for method chaining
     */
    public DelimitedSegment append(String str) {
        segmentSql.append(str);
        return this;
    }

    /**
     * Gets the SQL for the delimited segment
     *
     * @return String, never null
     */
    public String getSql() {
        String trimmed = segmentSql.toString().trim();
        if (!";".equals(delimiter) && !"go".equalsIgnoreCase(delimiter) && !"/".equals(delimiter)) {
            // Liquibase seems not to be able to properly split statements when the delimiter is not a common one
            // For example 'END@' with @ as a delimiter does not register as the end of a statement
            // If we add a space, this gets properly parsed
            trimmed = trimmed.replaceAll("(\\S)("+delimiter+")", "$1 $2");
        }
        return trimmed;
    }

    /**
     * Gets the trailing text after the last SQL delimiter and removes it from this segment
     *
     * @return The text that was removed (it should be appended to next segment).
     *         Never null, empty string if nothing was removed.
     */
    public String getAndRemoveAfterLastDelimiter() {
        int index = getIndexOfLastDelimiterInUse(new StringBuilder(segmentSql).reverse(), 0);

        if (index == -1) {
            return "";
        }
        String ret = segmentSql.substring(index);
        segmentSql.delete(index, segmentSql.length());
        return ret;
    }

    /**
     * This finds the index of the end of the actual last SQL statement considering SQL comments
     *
     * @param reversed The reversed SQL string - no modifications are made
     * @param index Start index of where to check
     * @return index of the last actual delimiter, -1 if not found
     */
    private int getIndexOfLastDelimiterInUse(StringBuilder reversed, int index) {
        int nextFound = reversed.indexOf(delimiter, index);

        if (nextFound == -1) {
            // No previous delimiter found
            return nextFound;
        }

        // "\nREAL SQL; -- comment ; skipped" -> reversed -> "deppiks ; tnemmoc -- ;LQS LAER\n"
        int lineStart = reversed.indexOf("\n", nextFound);
        int commentIndex = reversed.indexOf("--", nextFound);
        if (commentIndex != -1) {

            if (commentIndex < lineStart && nextFound < commentIndex) {
                // Delimiter is inside a single line comment, continue searching...
                return getIndexOfLastDelimiterInUse(reversed, commentIndex + 1);
            }
        }

        // "\nREAL SQL; /*\n * ; skipped\n */" -> reversed -> "/* \ndeppiks ; * \n */ ;LSQ LAER\n"
        int multiLineCommentEnd = reversed.indexOf("/*", index);
        if (multiLineCommentEnd != -1) {
            int multiLineCommentStart = reversed.indexOf("*/", multiLineCommentEnd);
            if (nextFound < multiLineCommentStart && nextFound > multiLineCommentEnd) {
                // Delimiter is inside multiline comment, continue searching...
                return getIndexOfLastDelimiterInUse(reversed, multiLineCommentStart + 1);
            }
        }

        // Found it!
        return segmentSql.length() - nextFound;
    }

    @Override
    public String toString() {
        return "DelimitedSegment{" +
                "delimiter='" + delimiter + '\'' +
                ", segmentSql=" + segmentSql +
                '}';
    }
}
