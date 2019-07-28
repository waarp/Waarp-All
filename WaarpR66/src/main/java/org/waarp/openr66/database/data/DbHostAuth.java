/*
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 * Waarp . If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.database.data;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.data.DbValue;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.role.RoleDefault;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.HostDAO;
import org.waarp.openr66.dao.database.DBHostDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Host;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Host Authentication Table object
 */
public class DbHostAuth extends AbstractDbData {
  private static final String CHECKED = "checked";

  private static final String CANNOT_FIND_HOST = "Cannot find host";

  public static final String DEFAULT_CLIENT_ADDRESS = "0.0.0.0";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DbHostAuth.class);
  private static final byte[] VALUE_0_BYTE = new byte[0];
  private static final DbHostAuth[] DBHOSTAUTH_0_SIZE = new DbHostAuth[0];
  private static final Pattern BACKSLASH =
      Pattern.compile("\"", Pattern.LITERAL);
  private static final Pattern COMMA = Pattern.compile(",", Pattern.LITERAL);

  public enum Columns {
    ADDRESS, PORT, ISSSL, HOSTKEY, ADMINROLE, ISCLIENT, ISACTIVE, ISPROXIFIED,
    UPDATEDINFO, HOSTID
  }

  public static final int[] dbTypes = {
      Types.VARCHAR, Types.INTEGER, Types.BIT, Types.VARBINARY, Types.BIT,
      Types.BIT, Types.BIT, Types.BIT, Types.INTEGER, Types.NVARCHAR
  };

  public static final String table = " HOSTS ";

  private Host host;

  // ALL TABLE SHOULD IMPLEMENT THIS
  public static final int NBPRKEY = 1;

  protected static final String selectAllFields =
      Columns.ADDRESS.name() + ',' + Columns.PORT.name() + ',' +
      Columns.ISSSL.name() + ',' + Columns.HOSTKEY.name() + ',' +
      Columns.ADMINROLE.name() + ',' + Columns.ISCLIENT.name() + ',' +
      Columns.ISACTIVE.name() + ',' + Columns.ISPROXIFIED.name() + ',' +
      Columns.UPDATEDINFO.name() + ',' + Columns.HOSTID.name();

  protected static final String updateAllFields =
      Columns.ADDRESS.name() + "=?," + Columns.PORT.name() + "=?," +
      Columns.ISSSL.name() + "=?," + Columns.HOSTKEY.name() + "=?," +
      Columns.ADMINROLE.name() + "=?," + Columns.ISCLIENT.name() + "=?," +
      Columns.ISACTIVE.name() + "=?," + Columns.ISPROXIFIED.name() + "=?," +
      Columns.UPDATEDINFO.name() + "=?";

  protected static final String insertAllValues = " (?,?,?,?,?,?,?,?,?,?) ";

  @Override
  protected void initObject() {
    primaryKey = new DbValue[] { new DbValue("", Columns.HOSTID.name()) };
    otherFields = new DbValue[] {
        new DbValue("", Columns.ADDRESS.name()),
        new DbValue(0, Columns.PORT.name()),
        new DbValue(false, Columns.ISSSL.name()),
        new DbValue(VALUE_0_BYTE, Columns.HOSTKEY.name()),
        new DbValue(false, Columns.ADMINROLE.name()),
        new DbValue(false, Columns.ISCLIENT.name()),
        new DbValue(false, Columns.ISACTIVE.name()),
        new DbValue(false, Columns.ISPROXIFIED.name()),
        new DbValue(0, Columns.UPDATEDINFO.name())
    };
    allFields = new DbValue[] {
        otherFields[0], otherFields[1], otherFields[2], otherFields[3],
        otherFields[4], otherFields[5], otherFields[6], otherFields[7],
        otherFields[8], primaryKey[0]
    };
  }

  @Override
  protected String getSelectAllFields() {
    return selectAllFields;
  }

  @Override
  protected String getTable() {
    return table;
  }

  @Override
  protected String getInsertAllValues() {
    return insertAllValues;
  }

  @Override
  protected String getUpdateAllFields() {
    return updateAllFields;
  }

  @Override
  protected void setToArray() {
    allFields[Columns.ADDRESS.ordinal()].setValue(host.getAddress());
    allFields[Columns.PORT.ordinal()].setValue(host.getPort());
    allFields[Columns.ISSSL.ordinal()].setValue(host.isSSL());
    allFields[Columns.HOSTKEY.ordinal()].setValue(host.getHostkey());
    allFields[Columns.ADMINROLE.ordinal()].setValue(host.isAdmin());
    allFields[Columns.ISCLIENT.ordinal()].setValue(host.isClient());
    allFields[Columns.ISACTIVE.ordinal()].setValue(host.isActive());
    allFields[Columns.ISPROXIFIED.ordinal()].setValue(host.isProxified());
    allFields[Columns.UPDATEDINFO.ordinal()]
        .setValue(host.getUpdatedInfo().ordinal());
    allFields[Columns.HOSTID.ordinal()].setValue(host.getHostid());
  }

  @Override
  protected void setFromArray() throws WaarpDatabaseSqlException {
    host.setAddress((String) allFields[Columns.ADDRESS.ordinal()].getValue());
    host.setPort((Integer) allFields[Columns.PORT.ordinal()].getValue());
    host.setSSL((Boolean) allFields[Columns.ISSSL.ordinal()].getValue());
    host.setHostkey((byte[]) allFields[Columns.HOSTKEY.ordinal()].getValue());
    host.setAdmin((Boolean) allFields[Columns.ADMINROLE.ordinal()].getValue());
    host.setClient((Boolean) allFields[Columns.ISCLIENT.ordinal()].getValue());
    host.setActive((Boolean) allFields[Columns.ISACTIVE.ordinal()].getValue());
    host.setProxified(
        (Boolean) allFields[Columns.ISPROXIFIED.ordinal()].getValue());
    host.setUpdatedInfo(org.waarp.openr66.pojo.UpdatedInfo.valueOf(
        (Integer) allFields[Columns.UPDATEDINFO.ordinal()].getValue()));
    host.setHostid((String) allFields[Columns.HOSTID.ordinal()].getValue());
  }

  @Override
  protected String getWherePrimaryKey() {
    return primaryKey[0].getColumn() + " = ? ";
  }

  @Override
  protected void setPrimaryKey() {
    primaryKey[0].setValue(host.getHostid());
  }

  /**
   * @param hostid
   * @param address
   * @param port
   * @param isSSL
   * @param hostkey
   * @param adminrole
   * @param isClient
   */
  public DbHostAuth(String hostid, String address, int port, boolean isSSL,
                    byte[] hostkey, boolean adminrole, boolean isClient) {
    host = new Host(hostid, address, port, hostkey, isSSL, isClient, false,
                    adminrole);
    if (hostkey != null) {
      try {
        // Save as crypted with the local Key and HEX
        host.setHostkey(
            Configuration.configuration.getCryptoKey().cryptToHex(hostkey)
                                       .getBytes(WaarpStringUtils.UTF8));
      } catch (final Exception e) {
        logger.warn("Error while cyphering hostkey", e);
        host.setHostkey(VALUE_0_BYTE);
      }
    }
    if (port < 0) {
      host.setClient(true);
      host.setAddress(DEFAULT_CLIENT_ADDRESS);
    }
    setToArray();
    isSaved = false;
  }

  private DbHostAuth(Host host) {
    if (host == null) {
      throw new IllegalArgumentException(
          "Argument in constructor cannot be null");
    }
    this.host = host;
    setToArray();
  }

  public DbHostAuth(ObjectNode source) throws WaarpDatabaseSqlException {
    host = new Host();
    setFromJson(source, false);
    setToArray();
  }

  @Override
  public void setFromJson(ObjectNode node, boolean ignorePrimaryKey)
      throws WaarpDatabaseSqlException {
    super.setFromJson(node, ignorePrimaryKey);
    if (host.getHostkey() == null || host.getHostkey().length == 0 ||
        host.getAddress() == null || host.getAddress().isEmpty() ||
        host.getHostid() == null || host.getHostid().isEmpty()) {
      throw new WaarpDatabaseSqlException(
          "Not enough argument to create the object");
    }
    if (host.getHostkey() != null) {
      try {
        // Save as crypted with the local Key and Base64
        host.setHostkey(Configuration.configuration.getCryptoKey().cryptToHex(
            host.getHostkey()).getBytes(WaarpStringUtils.UTF8));
      } catch (final Exception e) {
        host.setHostkey(VALUE_0_BYTE);
      }
    }
    if (host.getPort() < 0) {
      host.setClient(true);
      host.setAddress(DEFAULT_CLIENT_ADDRESS);
    }
  }

  /**
   * @param hostid
   *
   * @throws WaarpDatabaseException
   */
  public DbHostAuth(String hostid) throws WaarpDatabaseException {
    if (hostid == null) {
      throw new WaarpDatabaseException("No host id passed");
    }
    HostDAO hostAccess = null;
    try {
      hostAccess = DAOFactory.getInstance().getHostDAO();
      host = hostAccess.select(hostid);
      setToArray();
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException(CANNOT_FIND_HOST, e);
    } finally {
      if (hostAccess != null) {
        hostAccess.close();
      }
    }
  }

  /**
   * Delete all entries (used when purge and reload)
   *
   * @return the previous existing array of DbRule
   *
   * @throws WaarpDatabaseException
   */
  public static DbHostAuth[] deleteAll() throws WaarpDatabaseException {
    HostDAO hostAccess = null;
    final List<DbHostAuth> res = new ArrayList<DbHostAuth>();
    List<Host> hosts;
    try {
      hostAccess = DAOFactory.getInstance().getHostDAO();
      hosts = hostAccess.getAll();
      hostAccess.deleteAll();
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } finally {
      if (hostAccess != null) {
        hostAccess.close();
      }
    }
    for (final Host host : hosts) {
      res.add(new DbHostAuth(host));
    }
    return res.toArray(new DbHostAuth[0]);
  }

  @Override
  public void delete() throws WaarpDatabaseException {
    HostDAO hostAccess = null;
    try {
      hostAccess = DAOFactory.getInstance().getHostDAO();
      hostAccess.delete(host);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException(CANNOT_FIND_HOST, e);
    } finally {
      if (hostAccess != null) {
        hostAccess.close();
      }
    }
  }

  @Override
  public void insert() throws WaarpDatabaseException {
    HostDAO hostAccess = null;
    try {
      hostAccess = DAOFactory.getInstance().getHostDAO();
      hostAccess.insert(host);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } finally {
      if (hostAccess != null) {
        hostAccess.close();
      }
    }
  }

  @Override
  public boolean exist() throws WaarpDatabaseException {
    HostDAO hostAccess = null;
    try {
      hostAccess = DAOFactory.getInstance().getHostDAO();
      return hostAccess.exist(host.getHostid());
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } finally {
      if (hostAccess != null) {
        hostAccess.close();
      }
    }
  }

  @Override
  public void select() throws WaarpDatabaseException {
    HostDAO hostAccess = null;
    try {
      hostAccess = DAOFactory.getInstance().getHostDAO();
      host = hostAccess.select(host.getHostid());
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException(CANNOT_FIND_HOST, e);
    } finally {
      if (hostAccess != null) {
        hostAccess.close();
      }
    }
  }

  @Override
  public void update() throws WaarpDatabaseException {
    HostDAO hostAccess = null;
    try {
      hostAccess = DAOFactory.getInstance().getHostDAO();
      hostAccess.update(host);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException(CANNOT_FIND_HOST, e);
    } finally {
      if (hostAccess != null) {
        hostAccess.close();
      }
    }
  }

  /**
   * Private constructor for Commander only
   */
  private DbHostAuth() {
    host = new Host();
  }

  /**
   * Get All DbHostAuth from database or from internal hashMap in case of no
   * database support
   *
   * @return the array of DbHostAuth
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbHostAuth[] getAllHosts()
      throws WaarpDatabaseNoConnectionException {
    HostDAO hostAccess = null;
    final List<DbHostAuth> res = new ArrayList<DbHostAuth>();
    List<Host> hosts;
    try {
      hostAccess = DAOFactory.getInstance().getHostDAO();
      hosts = hostAccess.getAll();
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } finally {
      if (hostAccess != null) {
        hostAccess.close();
      }
    }
    for (final Host host : hosts) {
      res.add(new DbHostAuth(host));
    }
    return res.toArray(DBHOSTAUTH_0_SIZE);
  }

  /**
   * For instance from Commander when getting updated information
   *
   * @param preparedStatement
   *
   * @return the next updated DbHostAuth
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbHostAuth getFromStatement(
      DbPreparedStatement preparedStatement)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbHostAuth dbHostAuth = new DbHostAuth();
    dbHostAuth.getValues(preparedStatement, dbHostAuth.allFields);
    dbHostAuth.setFromArray();
    dbHostAuth.isSaved = true;
    return dbHostAuth;
  }

  public static DbHostAuth[] getUpdatedPreparedStatement()
      throws WaarpDatabaseNoConnectionException {
    final List<Filter> filters = new ArrayList<Filter>(1);
    filters.add(new Filter(DBHostDAO.UPDATED_INFO_FIELD, "=",
                           org.waarp.openr66.pojo.UpdatedInfo
                               .fromLegacy(UpdatedInfo.TOSUBMIT).ordinal()));
    HostDAO hostAccess = null;
    List<Host> hosts;
    try {
      hostAccess = DAOFactory.getInstance().getHostDAO();
      hosts = hostAccess.find(filters);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } finally {
      if (hostAccess != null) {
        hostAccess.close();
      }
    }
    final DbHostAuth[] res = new DbHostAuth[hosts.size()];
    int i = 0;
    for (final Host host : hosts) {
      res[i] = new DbHostAuth(host);
      i++;
    }
    return res;
  }

  /**
   * @param session
   * @param host
   * @param addr
   * @param ssl
   * @param active
   *
   * @return the DbPreparedStatement according to the filter
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getFilterPrepareStament(DbSession session,
                                                            String host,
                                                            String addr,
                                                            boolean ssl,
                                                            boolean active)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(session);
    final String request =
        "SELECT " + selectAllFields + " FROM " + table + " WHERE ";
    String condition = null;
    if (host != null) {
      condition = Columns.HOSTID.name() + " LIKE '%" + host + "%' ";
    }
    if (addr != null) {
      if (condition != null) {
        condition += " AND ";
      } else {
        condition = "";
      }
      condition += Columns.ADDRESS.name() + " LIKE '%" + addr + "%' ";
    }
    if (condition != null) {
      condition += " AND ";
    } else {
      condition = "";
    }
    condition += Columns.ISSSL.name() + " = ? AND ";
    condition += Columns.ISACTIVE.name() + " = ? ";
    preparedStatement.createPrepareStatement(
        request + condition + " ORDER BY " + Columns.HOSTID.name());
    try {
      preparedStatement.getPreparedStatement().setBoolean(1, ssl);
      preparedStatement.getPreparedStatement().setBoolean(2, active);
    } catch (final SQLException e) {
      preparedStatement.realClose();
      throw new WaarpDatabaseSqlException(e);
    }
    return preparedStatement;
  }

  /**
   * @param session
   * @param host
   * @param addr
   *
   * @return the DbPreparedStatement according to the filter
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getFilterPrepareStament(DbSession session,
                                                            String host,
                                                            String addr)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(session);
    final String request = "SELECT " + selectAllFields + " FROM " + table;
    String condition = null;
    if (host != null) {
      condition = Columns.HOSTID.name() + " LIKE '%" + host + "%' ";
    }
    if (addr != null) {
      if (condition != null) {
        condition += " AND ";
      } else {
        condition = "";
      }
      condition += Columns.ADDRESS.name() + " LIKE '%" + addr + "%' ";
    }
    if (condition != null) {
      condition = " WHERE " + condition;
    } else {
      condition = "";
    }
    preparedStatement.createPrepareStatement(
        request + condition + " ORDER BY " + Columns.HOSTID.name());
    return preparedStatement;
  }

  @Override
  public void changeUpdatedInfo(UpdatedInfo info) {
    host.setUpdatedInfo(org.waarp.openr66.pojo.UpdatedInfo.fromLegacy(info));
  }

  /**
   * @return the isActive
   */
  public boolean isActive() {
    return host.isActive();
  }

  /**
   * @param isActive the isActive to set
   */
  public void setActive(boolean isActive) {
    host.setActive(isActive);
  }

  /**
   * @return the isProxified
   */
  public boolean isProxified() {
    return host.isProxified();
  }

  /**
   * @param isProxified the isProxified to set
   */
  public void setProxified(boolean isProxified) {
    host.setProxified(isProxified);
  }

  /**
   * Is the given key a valid one
   *
   * @param newkey
   *
   * @return True if the key is valid (or any key is valid)
   */
  public boolean isKeyValid(byte[] newkey) {
    // It is valid to not have a key
    // Check before if any key is passed or if account is active
    if (host.getHostkey() == null) {
      return true;
    }
    // Check before if any key is passed or if account is active
    if (newkey == null || !isActive()) {
      return false;
    }
    try {
      return FilesystemBasedDigest.equalPasswd(
          Configuration.configuration.getCryptoKey()
                                     .decryptHexInBytes(host.getHostkey()),
          newkey);
    } catch (final Exception e) {
      logger.debug("Error while checking key", e);
      return false;
    }
  }

  /**
   * @return the hostkey
   */
  public byte[] getHostkey() {
    if (host.getHostkey() == null) {
      return null;
    }
    try {
      return Configuration.configuration.getCryptoKey()
                                        .decryptHexInBytes(host.getHostkey());
    } catch (final Exception e) {
      logger.debug("Error while checking key", e);
      return VALUE_0_BYTE;
    }
  }

  /**
   * @return the adminrole
   */
  public boolean isAdminrole() {
    return host.isAdmin();
  }

  /**
   * Test if the address is 0.0.0.0 for a client or isClient
   *
   * @return True if the address is a client address (0.0.0.0) or isClient
   */
  public boolean isClient() {
    return host.isClient() || isNoAddress();
  }

  /**
   * True if the address is a client address (0.0.0.0) or if the port is < 0
   *
   * @return True if the address is a client address (0.0.0.0) or if the port
   *     is < 0
   */
  public boolean isNoAddress() {
    return host.getAddress().equals(DEFAULT_CLIENT_ADDRESS) ||
           host.getPort() < 0;
  }

  /**
   * @return the SocketAddress from the address and port
   *
   * @throws IllegalArgumentException when the address is for a Client
   *     and
   *     therefore cannot be checked
   */
  public SocketAddress getSocketAddress() throws IllegalArgumentException {
    if (isNoAddress()) {
      throw new IllegalArgumentException("Not a server");
    }
    return new InetSocketAddress(host.getAddress(), host.getPort());
  }

  /**
   * @return True if this Host ref is with SSL support
   */
  public boolean isSsl() {
    return host.isSSL();
  }

  /**
   * @return the hostid
   */
  public String getHostid() {
    return host.getHostid();
  }

  /**
   * @return the address
   */
  public String getAddress() {
    return host.getAddress();
  }

  /**
   * @return the port
   */
  public int getPort() {
    return host.getPort();
  }

  private static String getVersion(String host) {
    String remoteHost = host;
    String alias = "";
    if (Configuration.configuration.getAliases().containsKey(remoteHost)) {
      remoteHost = Configuration.configuration.getAliases().get(remoteHost);
      alias += "(Alias: " + remoteHost + ") ";
    }
    if (Configuration.configuration.getReverseAliases()
                                   .containsKey(remoteHost)) {
      StringBuilder alias2 = new StringBuilder("(ReverseAlias: ");
      final String[] list =
          Configuration.configuration.getReverseAliases().get(remoteHost);
      boolean found = false;
      for (final String string : list) {
        if (string.equals(host)) {
          continue;
        }
        found = true;
        alias2.append(string).append(' ');
      }
      if (found) {
        alias += alias2 + ") ";
      }
    }
    if (Configuration.configuration.getBusinessWhiteSet()
                                   .contains(remoteHost)) {
      alias += "(Business: Allowed) ";
    }
    if (Configuration.configuration.getRoles().containsKey(remoteHost)) {
      final RoleDefault item =
          Configuration.configuration.getRoles().get(remoteHost);
      alias += "(Role: " + item + ") ";
    }
    return alias +
           (Configuration.configuration.getVersions().containsKey(remoteHost)?
               Configuration.configuration.getVersions().get(remoteHost)
                                          .toString() : "Version Unknown");
  }

  @Override
  public String toString() {
    return "HostAuth: " + getHostid() + " address: " + getAddress() + ':' +
           getPort() + " isSSL: " + isSsl() + " admin: " + isAdminrole() +
           " isClient: " + isClient() + " isActive: " + isActive() +
           " isProxified: " + isProxified() + " (" +
           (host.getHostkey() != null? host.getHostkey().length : 0) +
           ") Version: " + getVersion(getHostid());
  }

  /**
   * Write selected DbHostAuth to a Json String
   *
   * @param preparedStatement
   *
   * @return the associated Json String
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   * @throws OpenR66ProtocolBusinessException
   */
  public static String getJson(DbPreparedStatement preparedStatement, int limit)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             OpenR66ProtocolBusinessException {
    final ArrayNode arrayNode = JsonHandler.createArrayNode();
    try {
      preparedStatement.executeQuery();
      int nb = 0;
      while (preparedStatement.getNext()) {
        final DbHostAuth host = getFromStatement(preparedStatement);
        final ObjectNode node = host.getInternalJson();
        arrayNode.add(node);
        nb++;
        if (nb >= limit) {
          break;
        }
      }
    } finally {
      preparedStatement.realClose();
    }
    return JsonHandler.writeAsString(arrayNode);
  }

  private ObjectNode getInternalJson() {
    final ObjectNode node = getJson();
    try {
      node.put(Columns.HOSTKEY.name(),
               new String(getHostkey(), WaarpStringUtils.UTF8));
    } catch (final Exception e1) {
      node.put(Columns.HOSTKEY.name(), "");
    }
    int nb;
    try {
      nb = NetworkTransaction
          .nbAttachedConnection(getSocketAddress(), getHostid());
    } catch (final Exception e) {
      nb = -1;
    }
    node.put("Connection", nb);
    node.put("Version", COMMA.matcher(BACKSLASH.matcher(getVersion(getHostid()))
                                               .replaceAll(Matcher
                                                               .quoteReplacement(
                                                                   "")))
                             .replaceAll(Matcher.quoteReplacement(", ")));
    return node;
  }

  /**
   * @return the Json string for this
   */
  public String getJsonAsString() {
    final ObjectNode node = getInternalJson();
    return JsonHandler.writeAsString(node);
  }

  /**
   * @param session
   * @param body
   * @param crypted True if the Key is kept crypted, False it will be
   *     in
   *     clear form
   *
   * @return the runner in Html format specified by body by replacing all
   *     instance of fields
   */
  public String toSpecializedHtml(R66Session session, String body,
                                  boolean crypted) {
    final StringBuilder builder = new StringBuilder(body);
    WaarpStringUtils.replace(builder, "XXXHOSTXXX", getHostid());
    WaarpStringUtils.replace(builder, "XXXADDRXXX", getAddress());
    WaarpStringUtils
        .replace(builder, "XXXPORTXXX", Integer.toString(getPort()));
    if (crypted) {
      WaarpStringUtils.replace(builder, "XXXKEYXXX",
                               new String(getHostkey(), WaarpStringUtils.UTF8));
    } else {
      try {
        WaarpStringUtils.replace(builder, "XXXKEYXXX",
                                 Configuration.configuration.getCryptoKey()
                                                            .decryptHexInString(
                                                                new String(
                                                                    getHostkey(),
                                                                    WaarpStringUtils.UTF8)));
      } catch (final Exception e) {
        WaarpStringUtils.replace(builder, "XXXKEYXXX", "BAD DECRYPT");
      }
    }
    WaarpStringUtils.replace(builder, "XXXSSLXXX", isSsl()? CHECKED : "");
    WaarpStringUtils.replace(builder, "XXXADMXXX", isAdminrole()? CHECKED : "");
    WaarpStringUtils.replace(builder, "XXXISCXXX", isClient()? CHECKED : "");
    WaarpStringUtils.replace(builder, "XXXISAXXX", isActive()? CHECKED : "");
    WaarpStringUtils.replace(builder, "XXXISPXXX", isProxified()? CHECKED : "");
    WaarpStringUtils.replace(builder, "XXXVERSIONXXX",
                             COMMA.matcher(getVersion(getHostid()))
                                  .replaceAll(Matcher.quoteReplacement(", ")));
    int nb;
    try {
      nb = NetworkTransaction
          .nbAttachedConnection(getSocketAddress(), getHostid());
    } catch (final Exception e) {
      nb = -1;
    }
    WaarpStringUtils
        .replace(builder, "XXXCONNXXX", nb > 0? "(" + nb + " Connected) " : "");
    return builder.toString();
  }

  /**
   * @return True if any of the server has the isProxified property
   */
  public static boolean hasProxifiedHosts() {
    final List<Filter> filters = new ArrayList<Filter>();
    filters.add(new Filter(DBHostDAO.IS_PROXIFIED_FIELD, "=", true));

    HostDAO hostAccess = null;
    try {
      hostAccess = DAOFactory.getInstance().getHostDAO();
      return !hostAccess.find(filters).isEmpty();
    } catch (final DAOConnectionException e) {
      logger.error("DAO Access error", e);
      return false;
    } finally {
      if (hostAccess != null) {
        hostAccess.close();
      }
    }
  }

  /**
   * @return the DbValue associated with this table
   */
  public static DbValue[] getAllType() {
    final DbHostAuth item = new DbHostAuth();
    return item.allFields;
  }
}
