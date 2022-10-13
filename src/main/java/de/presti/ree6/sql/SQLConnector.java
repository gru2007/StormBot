package de.presti.ree6.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.presti.ree6.main.Main;
import de.presti.ree6.sql.migrations.MigrationUtil;
import de.presti.ree6.sql.seed.SeedManager;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A "Connector" Class which connect with the used Database Server.
 * Used to manage the connection between Server and Client.
 */
@Slf4j
@SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve"})
public class SQLConnector {

    // Various String that keep connection information to use for a connection.
    private final String databaseUser,
            databaseName,
            databasePassword,
            databaseServerIP;

    // The port of the Server.
    private final int databaseServerPort;

    // An Instance of the actual Java SQL Connection.
    private HikariDataSource dataSource;

    // An Instance of the SQL-Worker which works with the Data in the Database.
    private final SQLWorker sqlWorker;

    // A boolean to keep track if there was at least one valid connection.
    private boolean connectedOnce = false;

    /**
     * Constructor with the needed data to open an SQL connection.
     *
     * @param databaseUser       the Database Username
     * @param databaseName       the Database name
     * @param databasePassword   the Database User password
     * @param databaseServerIP   the Address of the Database Server.
     * @param databaseServerPort the Port of the Database Server.
     */
    public SQLConnector(String databaseUser, String databaseName, String databasePassword, String databaseServerIP, int databaseServerPort) {
        this.databaseUser = databaseUser;
        this.databaseName = databaseName;
        this.databasePassword = databasePassword;
        this.databaseServerIP = databaseServerIP;
        this.databaseServerPort = databaseServerPort;

        sqlWorker = new SQLWorker(this);

        SQLSession.setJdbcURL(buildConnectionURL());
        SQLSession.setMaxPoolSize(Main.getInstance().getConfig().getConfiguration().getInt("hikari.misc.poolSize"));

        connectToSQLServer();
        createTables();
        try {
            MigrationUtil.runAllMigrations(this);
        } catch (Exception exception) {
            log.error("Error while running Migrations!", exception);
        }

        SeedManager.runAllSeeds(this);
    }

    /**
     * Try to open a connection to the SQL Server with the given data.
     */
    public void connectToSQLServer() {
        log.info("Connecting to SQl-Service (SQL).");
        // Check if there is already an open Connection.
        if (isConnected()) {
            try {
                // Close if there is and notify.
                getDataSource().close();
                log.info("Service (SQL) has been stopped.");
            } catch (Exception ignore) {
                // Notify if there was an error.
                log.error("Service (SQL) couldn't be stopped.");
            }
        }

        try {
            HikariConfig hConfig = new HikariConfig();

            hConfig.setJdbcUrl(SQLSession.getJdbcURL());
            hConfig.setMaximumPoolSize(SQLSession.getMaxPoolSize());
            dataSource = new HikariDataSource(hConfig);
            log.info("Service (SQL) has been started. Connection was successful.");
            connectedOnce = true;
        } catch (Exception exception) {
            // Notify if there was an error.
            log.error("Service (SQL) couldn't be started. Connection was unsuccessful.", exception);
        }
    }

    /**
     * Create Tables in the Database if they aren't already set.
     */
    public void createTables() {

        // Check if there is an open Connection if not, skip.
        if (!isConnected()) return;

        try (InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("sql/schema.sql")) {
            List<String> queries = Arrays.stream(new String(inputStream.readAllBytes()).split(";")).filter(s -> !s.isEmpty()).toList();
            for (String query : queries) {
                log.debug("\t\t[*] Executing query {}/{}", queries.indexOf(query) + 1, queries.size());
                log.debug("\t\t[*] Executing query: {}", query);
                querySQL(query);
            }
        } catch (Exception exception) {
            log.error("Couldn't create Tables!", exception);
        }
    }

    //region Utility

    /**
     * Build the Connection URL with the given data.
     *
     * @return the Connection URL.
     */
    public String buildConnectionURL() {
        String jdbcUrl;

        switch (Main.getInstance().getConfig().getConfiguration().getString("hikari.misc.storage").toLowerCase()) {
            case "mariadb" -> {
                jdbcUrl = "jdbc:mariadb://%s:%s/%s?user=%s&password=%s";
                jdbcUrl = jdbcUrl.formatted(databaseServerIP,
                        databaseServerPort,
                        databaseName,
                        databaseUser,
                        databasePassword);
            }

            default -> {
                jdbcUrl = "jdbc:sqlite:%s";
                jdbcUrl = jdbcUrl.formatted("storage/Ree6.db");
            }
        }
        return jdbcUrl;
    }

    /**
     * Query basic SQL Statements, without using the ORM-System.
     *
     * @param sqlQuery   The SQL Query.
     * @param parameters The Parameters for the Query.
     * @return Either a {@link Integer} or the result object of the ResultSet.
     */
    public Object querySQL(String sqlQuery, Object... parameters) {
        try (Connection connection = getDataSource().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            for (int i = 0; i < parameters.length; i++) {
                preparedStatement.setObject(i + 1, parameters[i]);
            }
            if (sqlQuery.startsWith("SELECT")) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    return resultSet.next();
                }
            } else {
                return preparedStatement.executeUpdate();
            }
        } catch (Exception ignore) {
        }

        return null;
    }

    /**
     * Send an SQL-Query to SQL-Server and get the response.
     *
     * @param sqlQuery   the SQL-Query.
     * @param parameters a list with all parameters that should be considered.
     * @return The Result from the SQL-Server.
     */
    public <R> Query<R> querySQL(@NotNull R r, @NotNull String sqlQuery, @Nullable Map<String, Object> parameters) {

        if (!isConnected()) {
            if (connectedOnce()) {
                connectToSQLServer();
                return querySQL(r, sqlQuery, parameters);
            } else {
                return null;
            }
        }

        try (Session session = SQLSession.getSessionFactory().openSession()) {

            session.beginTransaction();

            Query<R> query = (Query<R>) session.createQuery(sqlQuery, r.getClass());

            if (parameters != null) {
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    query.setParameter(entry.getKey(), entry.getValue());
                }
            }

            session.getTransaction().commit();

            return query;
        }
    }

    //endregion

    /**
     * Check if there is an open connection to the Database Server.
     *
     * @return boolean If the connection is opened.
     */
    public boolean isConnected() {
        try {
            return getDataSource() != null && !getDataSource().isClosed();
        } catch (Exception ignore) {
        }

        return false;
    }

    /**
     * Call to close the current Connection.
     */
    public void close() {
        // Check if there is already an open Connection.
        if (isConnected()) {
            try {
                // Close if there is and notify.
                getDataSource().close();
                log.info("Service (SQL) has been stopped.");
            } catch (Exception ignore) {
                // Notify if there was an error.
                log.error("Service (SQL) couldn't be stopped.");
            }
        }
    }

    /**
     * Retrieve an Instance of the SQL-Connection.
     *
     * @return DataSource Instance of te SQL-Connection.
     */
    public HikariDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Retrieve an Instance of the SQL-Worker to work with the Data.
     *
     * @return {@link SQLWorker} the Instance saved in this SQL-Connector.
     */
    public SQLWorker getSqlWorker() {
        return sqlWorker;
    }

    /**
     * Check if there was at least one successful Connection to the Database Server.
     *
     * @return boolean If there was at least one successful Connection.
     */
    public boolean connectedOnce() {
        return connectedOnce;
    }
}