package org.waarp.openr66.dao.database;

import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.dao.exception.DAOConnectionException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DbTransferDAOStatistic extends DBTransferDAO {
  protected static WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DbTransferDAOStatistic.class);
  private final static String PG_EXCLUDE = "pg_";
  private final DBTransferDAO dbTransferDAO;

  public DbTransferDAOStatistic(final DBTransferDAO transferDAO) {
    super(transferDAO.connection);
    dbTransferDAO = transferDAO;
  }

  @Override
  protected long getNextId() throws DAOConnectionException {
    return 0;
  }

  public void initStatistics() throws SQLException {
    try {
      final Statement statement = connection.createStatement();
      statement.execute("CREATE EXTENSION IF NOT EXISTS pg_stat_statements");
      statement.close();
    } catch (final SQLException e) {
      logger.warn(e);
    }
  }

  public void executeStatistics() throws SQLException {
    final String hotQueries = "select \n" + "\tquery, \n" + "\tcalls,\n" +
                              "\ttotal_exec_time::integer, \n" +
                              "\t(calls/total_exec_time)::integer as " +
                              "ms_per_call, \n" + "\tshared_blks_hit, \n" +
                              "\tshared_blks_read \n" +
                              "from pg_stat_statements pss\n" +
                              "order by calls desc\n" + "limit 100";
    final String slowQueries =
        "select query, calls, (total_exec_time/calls)::integer as avg_time_ms" +
        " \n" + "from pg_stat_statements\n" + "where calls > 1000\n" +
        "order by avg_time_ms desc\n" + "limit 100";
    final String slowLowQueries =
        "select query, calls, (total_exec_time/calls)::integer as avg_time_ms" +
        " \n" + "from pg_stat_statements\n" + "where calls > 1000\n" +
        "and rows < 50\n" + "order by avg_time_ms desc\n" + "limit 100";
    final String topQueries = "SELECT query,\n" + "      calls,\n" +
                              "      round(total_exec_time::numeric, 2) AS total_time,\n" +
                              "      round(mean_exec_time::numeric, 2) AS mean_time,\n" +
                              "      round((100 * total_exec_time / sum(total_exec_time) OVER ())::numeric, 2) AS percentage\n" +
                              "FROM pg_stat_statements\n" +
                              "ORDER BY total_exec_time DESC\n" + "LIMIT 10";
    try {
      final Statement statement = connection.createStatement();
      final ResultSet resultSet = statement.executeQuery(slowQueries);
      SysErrLogger.FAKE_LOGGER.sysout("Calls\tAvgTime\tAll Query");
      while (resultSet.next()) {
        final String query = resultSet.getString(1);
        if (!query.contains(PG_EXCLUDE)) {
          SysErrLogger.FAKE_LOGGER.sysout(
              resultSet.getLong(2) + "\t" + resultSet.getLong(3) + "\t" +
              query);
        }
      }
      resultSet.close();
      final ResultSet resultSet2 = statement.executeQuery(slowLowQueries);
      SysErrLogger.FAKE_LOGGER.sysout("Calls\tAvgTime\tSmall Query");
      while (resultSet2.next()) {
        final String query = resultSet2.getString(1);
        if (!query.contains(PG_EXCLUDE)) {
          SysErrLogger.FAKE_LOGGER.sysout(
              resultSet2.getLong(2) + "\t" + resultSet2.getLong(3) + "\t" +
              query);
        }
      }
      resultSet2.close();
      final ResultSet resultSet4 = statement.executeQuery(topQueries);
      SysErrLogger.FAKE_LOGGER.sysout(
          "Calls\tTotalTime\tAverage\tPercent\tTop Query");
      while (resultSet4.next()) {
        final String query = resultSet4.getString(1);
        if (!query.contains(PG_EXCLUDE)) {
          SysErrLogger.FAKE_LOGGER.sysout(
              resultSet4.getLong(2) + "\t" + resultSet4.getLong(3) + "\t" +
              resultSet4.getLong(4) + "\t" + resultSet4.getLong(5) + "\t" +
              query);
        }
      }
      resultSet4.close();
      final ResultSet resultSet3 = statement.executeQuery(hotQueries);
      SysErrLogger.FAKE_LOGGER.sysout(
          "Calls\tTotalTime\tMsPerCall\tHit\tRead\tHot Query");
      while (resultSet3.next()) {
        final String query = resultSet3.getString(1);
        if (!query.contains(PG_EXCLUDE)) {
          SysErrLogger.FAKE_LOGGER.sysout(
              resultSet3.getLong(2) + "\t" + resultSet3.getLong(3) + "\t" +
              resultSet3.getLong(4) + "\t" + resultSet3.getLong(5) + "\t" +
              resultSet3.getLong(6) + "\t" + query);
        }
      }
      resultSet3.close();
      statement.close();
    } catch (final SQLException e) {
      e.printStackTrace();
    }
  }
}
