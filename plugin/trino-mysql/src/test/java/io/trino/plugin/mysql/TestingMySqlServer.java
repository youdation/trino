/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.plugin.mysql;

import org.testcontainers.containers.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static java.lang.String.format;
import static org.testcontainers.containers.MySQLContainer.MYSQL_PORT;

public class TestingMySqlServer
        implements AutoCloseable
{
    private final MySQLContainer<?> container;

    public TestingMySqlServer()
    {
        this(false);
    }

    public TestingMySqlServer(boolean globalTransactionEnable)
    {
        this("mysql:8.0.12", globalTransactionEnable);
    }

    public TestingMySqlServer(String dockerImageName, boolean globalTransactionEnable)
    {
        MySQLContainer<?> container = new MySQLContainer<>(dockerImageName);
        container = container.withDatabaseName("tpch");
        if (globalTransactionEnable) {
            container = container.withCommand("--gtid-mode=ON", "--enforce-gtid-consistency=ON");
        }
        this.container = container;
        container.start();
        execute(format("GRANT ALL PRIVILEGES ON *.* TO '%s'", container.getUsername()), "root", container.getPassword());
    }

    public void execute(String sql)
    {
        execute(sql, getUsername(), getPassword());
    }

    public void execute(String sql, String user, String password)
    {
        try (Connection connection = DriverManager.getConnection(getJdbcUrl(), user, password);
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUsername()
    {
        return container.getUsername();
    }

    public String getPassword()
    {
        return container.getPassword();
    }

    public String getDatabaseName()
    {
        return container.getDatabaseName();
    }

    public String getJdbcUrl()
    {
        return format("jdbc:mysql://%s:%s?useSSL=false&allowPublicKeyRetrieval=true", container.getContainerIpAddress(), container.getMappedPort(MYSQL_PORT));
    }

    @Override
    public void close()
    {
        container.close();
    }
}
