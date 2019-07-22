package org.waarp.openr66.dao.database.mariadb;

import org.waarp.openr66.dao.database.DBTransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MariaDBTransferDAO extends DBTransferDAO {

    protected static String SQL_GET_ID = "SELECT seq FROM Sequences " +
            "WHERE name='RUNSEQ' FOR UPDATE";
    private static String SQL_UPDATE_ID = "UPDATE Sequences SET seq = ? " +
            "WHERE name='RUNSEQ'";

    public MariaDBTransferDAO(Connection con) throws DAOConnectionException {
        super(con);
    }

    @Override
    protected long getNextId() throws DAOConnectionException {
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        try {
            ps = connection.prepareStatement(SQL_GET_ID);
            ResultSet rs = ps.executeQuery();
            long res;
            if (rs.next()) {
                res = rs.getLong(1);
                ps2 = connection.prepareStatement(SQL_UPDATE_ID);
                ps2.setLong(1, res + 1);
                ps2.executeUpdate();
                return res;
            } else {
                throw new DAOConnectionException(
                        "Error no id available, you should purge the database.");
            }
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeStatement(ps);
            closeStatement(ps2);
        }
    }
}