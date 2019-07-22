package org.waarp.openr66.dao.database;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

abstract class StatementExecutor {

    private static final WaarpLogger logger = WaarpLoggerFactory.getLogger(
            StatementExecutor.class);

    public void setParameters(PreparedStatement stm, Object... values)
            throws SQLException {
            for (int i = 0; i < values.length; i++) {
                stm.setObject(i+1, values[i]);
            }
    }

    public void executeUpdate(PreparedStatement stm) throws SQLException {
        int res = 0;
        res = stm.executeUpdate();
        if (res < 1) {
            logger.warn("Update failed, no record updated.");
        } else {
            logger.debug(res + " records updated.");
        }
    }

    public ResultSet executeQuery(PreparedStatement stm) throws SQLException {
        return stm.executeQuery();
    }

    public void closeStatement(Statement stm) {
        if (stm == null) {
            return;
        }
        try {
            stm.close();
        } catch (SQLException e) {
            logger.warn("An error occurs while closing the statement.", e);
        }
    }

    public void closeResultSet(ResultSet rs) {
        if (rs == null) {
            return;
        }
        try {
            rs.close();
        } catch (SQLException e) {
            logger.warn("An error occurs while closing the resultSet.", e);
        }
    }
}

