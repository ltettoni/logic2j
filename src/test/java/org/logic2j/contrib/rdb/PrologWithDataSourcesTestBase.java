/*
 * logic2j - "Bring Logic to your Java" - Copyright (c) 2017 Laurent.Tettoni@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.logic2j.contrib.rdb;

import org.h2.jdbcx.JdbcDataSource;
import org.logic2j.core.PrologTestBase;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Common base class for testing the Prolog engine with data sources. Although it would be formally cleaner to instantiate data sources once
 * per test case (using a @Before), let's go for a slightly faster approach: one connection per test class, since we are only reading from our
 * reference databases.
 */
public abstract class PrologWithDataSourcesTestBase extends PrologTestBase {
    private Connection zipcodesConnection = null;

    /**
     * @param h2LoadScriptResourcePath Relative path to the H2 initial load script.
     * @return A new H2 in-memory {@link DataSource}
     */
    protected DataSource h2DataSource(String h2LoadScriptResourcePath) {
        // Ensure proper exception message in case resource does not exist
        try (final InputStream inputStream = getClass().getResourceAsStream(h2LoadScriptResourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("No classloadable resource at path \"" + h2LoadScriptResourcePath + '"');
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not autoclose classloadable resource");
        }

        String uniqueDbName = "testdb_" + UUID.randomUUID();

        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + uniqueDbName + ";INIT=RUNSCRIPT FROM 'classpath:" + h2LoadScriptResourcePath + "'");
        ds.setUser("");
        ds.setPassword("");
        return ds;
    }

    /**
     * @return A {@link javax.sql.DataSource} to the "zipcodes" reference database.
     */
    protected DataSource zipcodesDataSource() {
        return h2DataSource("/db/zipcodes1/sql/h2_init.sql");
    }

    /**
     * @return A (previously obtained and reused) {@link java.sql.Connection} to the "zipcodes" reference database.
     * @throws java.sql.SQLException
     */
    protected Connection zipcodesConnection() throws SQLException {
        if (this.zipcodesConnection == null) {
            this.zipcodesConnection = zipcodesDataSource().getConnection();
            // logger.debug("Instantiated new connection to zipcodes DB");
        }
        return this.zipcodesConnection;
    }

}
