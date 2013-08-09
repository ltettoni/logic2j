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
import org.logic2j.core.PrologImplementor;
import org.logic2j.core.library.impl.LibraryBase;
import org.logic2j.core.library.mgmt.Primitive;
import org.logic2j.core.model.symbol.Struct;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.solve.GoalFrame;
import org.logic2j.core.solve.listener.SolutionListener;

/**
 * Preliminary - should be reviewed
 * FIXME: This class should not know of RDBClauseProvider!
 */
public class ConfigLibrary extends LibraryBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConfigLibrary.class);
    
    public ConfigLibrary(PrologImplementor theProlog) {
        super(theProlog);
    }

    @Primitive
    public void rdb_config(SolutionListener theListener,
            GoalFrame theGoalFrame, Bindings theBindings, Term... theArguments)
            throws SQLException {
        String driver = ((Struct) theArguments[0]).getName();
        final String connectionString = ((Struct) theArguments[1]).getName();
        String username = ((Struct) theArguments[2]).getName();
        String password = ((Struct) theArguments[3]).getName();
        String prefix = ((Struct) theArguments[4]).getName();
        Set<String> tablesToMap = new HashSet<String>();
        if (theArguments.length > 5 && theArguments[5].isList()) {
            for (Struct struct : ((Struct) theArguments[5]).javaListFromPList(
                    new ArrayList<Struct>(), Struct.class)) {
                tablesToMap.add(struct.getName().toLowerCase());
            }
        }

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException exception) {
            exception.printStackTrace();
        }
        
        DataSource dataSource = new DataSource() {

            @SuppressWarnings("unchecked")
            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                if (iface == null)
                    throw new IllegalArgumentException("Interface argument must not be null");
                if (!DataSource.class.equals(iface)) {
                    throw new SQLException(
                            "DataSource of type ["
                                    + getClass().getName()
                                    + "] can only be unwrapped as [javax.sql.DataSource], not as ["
                                    + iface.getName());
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
            public Logger getParentLogger()
                    throws SQLFeatureNotSupportedException {
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
            public Connection getConnection(String username, String password)
                    throws SQLException {
                return DriverManager.getConnection(connectionString, username,
                        password);
            }

            @Override
            public Connection getConnection() throws SQLException {
                return DriverManager.getConnection(connectionString);
            }
        };
        
        // This is dubious - we instantiate a new ClauseProvider just to save the table metamodel
        // but we won't have it when needed!!!
        // This generates a NPE see RDBClauseProviderTest
        RDBClauseProvider clauseProvider = new RDBClauseProvider(getProlog(),
                dataSource, prefix);

        DatabaseMetaData dmd = dataSource.getConnection(username, password).getMetaData();
        ResultSet tables = dmd.getTables(null, null, "%", null);
        while (tables.next()) {
            String tableName = tables.getString(3);
            logger.debug("DB introspection found table \"{}\"", tableName);
            String tableNameLc = tableName.toLowerCase();
            if (!tablesToMap.contains(tableNameLc)) continue;
            ResultSet tableColumns = dmd.getColumns(null, null, tableName, null);
            List<String> columnDescription = new ArrayList<String>();
            while (tableColumns.next()) {
                columnDescription.add(tableColumns.getString(4));
            }
            clauseProvider.saveTableInfo(tableName, columnDescription.toArray(new String[] {}));
            int arity = columnDescription.size();
            String predicateKey = prefix + tableNameLc + '/' + arity;
            getProlog().getClauseProviderResolver().register(predicateKey, clauseProvider);
            tableColumns.close();
        }
        tables.close();
    }
}
