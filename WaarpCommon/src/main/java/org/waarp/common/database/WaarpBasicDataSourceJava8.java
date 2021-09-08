package org.waarp.common.database;

import org.apache.commons.dbcp2.BasicDataSource;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class WaarpBasicDataSourceJava8 implements WaarpBasicDataSource {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpBasicDataSourceJava8.class);
  private final BasicDataSource dataSource;

  public WaarpBasicDataSourceJava8() {
    dataSource = new BasicDataSource();
  }

  @Override
  public void setDriverClassName(final String driverClassName) {
    dataSource.setDriverClassName(driverClassName);
  }

  @Override
  public void setUrl(final String url) {
    dataSource.setUrl(url);
  }

  @Override
  public void setUsername(final String user) {
    dataSource.setUsername(user);
  }

  @Override
  public void setPassword(final String password) {
    dataSource.setPassword(password);
  }

  @Override
  public void setDefaultAutoCommit(final boolean autoCommit) {
    dataSource.setDefaultAutoCommit(autoCommit);
  }

  @Override
  public void setDefaultReadOnly(final boolean readOnly) {
    dataSource.setDefaultReadOnly(readOnly);
  }

  @Override
  public void setValidationQuery(final String validationQuery) {
    dataSource.setValidationQuery(validationQuery);
  }

  @Override
  public void setMaxActive(final int maxConnections) {
    dataSource.setMaxTotal(maxConnections);
  }

  @Override
  public void setMaxIdle(final int maxIdle) {
    dataSource.setMaxIdle(maxIdle);
  }

  @Override
  public void setInitialSize(final int maxIdleDefault) {
    dataSource.setInitialSize(maxIdleDefault);
  }

  @Override
  public void close() {
    try {
      dataSource.close();
    } catch (final SQLException e) {
      logger.debug("Cannot close properly the connection pool", e);
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }
}
