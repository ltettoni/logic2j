package org.logic2j.contrib.library.config;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.logic2j.contrib.rdb.RDBClauseProvider;
import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.impl.LibraryBase;
import org.logic2j.core.library.mgmt.Primitive;

/**
 * Preliminary - should be reviewed FIXME This class should not know of RDBClauseProvider!
 */
public class ConfigLibrary extends LibraryBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConfigLibrary.class);

    public ConfigLibrary(PrologImplementation theProlog) {
        super(theProlog);
    }

    @Primitive
    public Continuation rdb_config(SolutionListener theListener, Bindings theBindings, Object... theArguments) throws SQLException {
        final String driver = ((Struct) theArguments[0]).getName();
        final String connectionString = ((Struct) theArguments[1]).getName();
        final String username = ((Struct) theArguments[2]).getName();
        final String password = ((Struct) theArguments[3]).getName();
        final String prefix = ((Struct) theArguments[4]).getName();
        final Set<String> tablesToMap = new HashSet<String>();
        if (theArguments.length > 5 && TermApi.isList(theArguments[5])) {
            for (final Struct struct : ((Struct) theArguments[5]).javaListFromPList(new ArrayList<Struct>(), Struct.class)) {
                tablesToMap.add(struct.getName().toLowerCase());
            }
        }

        try {
            Class.forName(driver);
        } catch (final ClassNotFoundException exception) {
            exception.printStackTrace();
        }

        final DataSource dataSource = new DataSource() {

            @SuppressWarnings("unchecked")
            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                if (iface == null) {
                    throw new PrologNonSpecificError("Interface argument must not be null");
                }
                if (!DataSource.class.equals(iface)) {
                    throw new SQLException("DataSource of type [" + getClass().getName() + "] can only be unwrapped as [javax.sql.DataSource], not as [" + iface.getName());
                }
                return (T) this;
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return DataSource.class.equals(iface);
            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {
                throw new UnsupportedOperationException("setLoginTimeout");
            }

            @Override
            public void setLogWriter(PrintWriter out) throws SQLException {
                throw new UnsupportedOperationException("setLogWriter");
            }

            @Override
            // Note: this requires a Java 1.7; this method did not exist in Java 1.6
            public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                throw new UnsupportedOperationException("getParentLogger");
            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return 0;
            }

            @Override
            public PrintWriter getLogWriter() throws SQLException {
                throw new UnsupportedOperationException("getLogWriter");
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return DriverManager.getConnection(connectionString, username, password);
            }

            @Override
            public Connection getConnection() throws SQLException {
                return DriverManager.getConnection(connectionString);
            }
        };

        // This is dubious - we instantiate a new ClauseProvider just to save the table metamodel
        // but we won't have it when needed!!!
        // This generates a NPE see RDBClauseProviderTest
        final RDBClauseProvider clauseProvider = new RDBClauseProvider(getProlog(), dataSource, prefix);

        final Connection connection = dataSource.getConnection(username, password);
        try {
            final DatabaseMetaData dmd = connection.getMetaData();
            final ResultSet tables = dmd.getTables(null, null, "%", null);
            while (tables.next()) {
                final String tableName = tables.getString(3);
                logger.debug("DB introspection found table \"{}\"", tableName);
                final String tableNameLc = tableName.toLowerCase();
                if (!tablesToMap.contains(tableNameLc)) {
                    continue;
                }
                final ResultSet tableColumns = dmd.getColumns(null, null, tableName, null);
                final List<String> columnDescription = new ArrayList<String>();
                while (tableColumns.next()) {
                    columnDescription.add(tableColumns.getString(4));
                }
                clauseProvider.saveTableInfo(tableName, columnDescription.toArray(new String[columnDescription.size()]));
                tableColumns.close();
            }
            tables.close();
        } finally {
            connection.close();
        }
        return Continuation.CONTINUE;
    }
}
