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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.joda.time.DateTime;
import org.waarp.common.database.DbConstant;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.role.RoleDefault;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.common.xml.XmlDecl;
import org.waarp.common.xml.XmlHash;
import org.waarp.common.xml.XmlType;
import org.waarp.common.xml.XmlUtil;
import org.waarp.common.xml.XmlValue;
import org.waarp.openr66.dao.AbstractDAO;
import org.waarp.openr66.dao.BusinessDAO;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.database.DBBusinessDAO;
import org.waarp.openr66.dao.database.StatementExecutor;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Business;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.utils.R66Versions;
import org.waarp.openr66.protocol.utils.Version;

import java.io.StringReader;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Configuration Table object
 */
public class DbHostConfiguration extends AbstractDbDataDao<Business> {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DbHostConfiguration.class);
  private static final Pattern WHITESPACES = WaarpStringUtils.BLANK;
  private static final Pattern SPACE_BACKSLASH = Pattern.compile("\\s|\\|");
  private static final Pattern COMMA = Pattern.compile(",");

  public enum Columns {
    BUSINESS, ROLES, ALIASES, OTHERS, UPDATEDINFO, HOSTID
  }

  public static final int[] dbTypes = {
      Types.LONGVARCHAR, Types.LONGVARCHAR, Types.LONGVARCHAR,
      Types.LONGVARCHAR, Types.INTEGER, Types.NVARCHAR
  };

  public static final Columns[] indexes = {
      Columns.HOSTID, Columns.UPDATEDINFO
  };

  public static final String table = " HOSTCONFIG ";

  public static final String XML_ALIASES = "aliases";

  public static final String XML_ROLES = "roles";

  public static final String XML_BUSINESS = "business";

  /**
   * Alias Id
   */
  public static final String XML_ALIASID = "aliasid";

  /**
   * Main ID in alias
   */
  public static final String XML_REALID = "realid";

  /**
   * Alias
   */
  public static final String XML_ALIAS = "alias";

  /**
   * Role set
   */
  public static final String XML_ROLESET = "roleset";

  /**
   * ID in role
   */
  public static final String XML_ROLEID = "roleid";

  /**
   * Role Main entry
   */
  public static final String XML_ROLE = "role";

  /**
   * Check version in protocol
   */
  public static final String XML_BUSINESSID = "businessid";
  private static final XmlDecl[] businessDecl = {
      new XmlDecl(XML_BUSINESS, XmlType.STRING,
                  XML_BUSINESS + '/' + XML_BUSINESSID, true)
  };

  /**
   * Structure of the Configuration file
   */
  public static final XmlDecl[] configRoleDecls = {
      // roles
      new XmlDecl(XmlType.STRING, XML_ROLEID),
      new XmlDecl(XmlType.STRING, XML_ROLESET)
  };
  private static final XmlDecl[] roleDecl = {
      new XmlDecl(XML_ROLES, XmlType.XVAL, XML_ROLES + '/' + XML_ROLE,
                  configRoleDecls, true)
  };
  /**
   * Structure of the Configuration file
   */
  public static final XmlDecl[] configAliasDecls = {
      // alias
      new XmlDecl(XmlType.STRING, XML_REALID),
      new XmlDecl(XmlType.STRING, XML_ALIASID)
  };

  private static final XmlDecl[] aliasDecl = {
      new XmlDecl(XML_ALIASES, XmlType.XVAL, XML_ALIASES + '/' + XML_ALIAS,
                  configAliasDecls, true)
  };

  public enum OtherFields {
    root, version, seeallid, lastMonitoringDateTime
  }

  protected static final String selectAllFields =
      Columns.BUSINESS.name() + ',' + Columns.ROLES.name() + ',' +
      Columns.ALIASES.name() + ',' + Columns.OTHERS.name() + ',' +
      Columns.UPDATEDINFO.name() + ',' + Columns.HOSTID.name();

  @Override
  protected final void initObject() {
    //nothing
  }

  @Override
  protected final String getTable() {
    return table;
  }

  @Override
  protected final AbstractDAO<Business> getDao(final boolean isCacheable)
      throws DAOConnectionException {
    return DAOFactory.getInstance().getBusinessDAO(isCacheable);
  }

  @Override
  protected final String getPrimaryKey() {
    if (pojo != null) {
      return pojo.getHostid();
    }
    throw new IllegalArgumentException("pojo is null");
  }

  @Override
  protected final String getPrimaryField() {
    return Columns.HOSTID.name();
  }


  /**
   * @param hostid
   * @param business Business configuration
   * @param roles Roles configuration
   * @param aliases Aliases configuration
   * @param others Other configuration
   */
  public DbHostConfiguration(final String hostid, final String business,
                             final String roles, final String aliases,
                             final String others)
      throws WaarpDatabaseSqlException {
    this.pojo = new Business(hostid, business, roles, aliases, others);
  }

  public DbHostConfiguration(final Business business) {
    if (business == null) {
      throw new IllegalArgumentException(
          "Argument in constructor cannot be null");
    }
    this.pojo = business;
  }

  /**
   * Constructor from Json
   *
   * @param source
   *
   * @throws WaarpDatabaseSqlException
   */
  public DbHostConfiguration(final ObjectNode source)
      throws WaarpDatabaseSqlException {
    pojo = new Business();
    setFromJson(source, false);
    if (ParametersChecker.isEmpty(pojo.getHostid())) {
      throw new WaarpDatabaseSqlException(
          "Not enough argument to create the object");
    }
    isSaved = false;
  }

  /**
   * @param hostid
   *
   * @throws WaarpDatabaseException
   */
  public DbHostConfiguration(final String hostid)
      throws WaarpDatabaseException {
    BusinessDAO businessAccess = null;
    try {
      businessAccess = DAOFactory.getInstance().getBusinessDAO(true);
      pojo = businessAccess.select(hostid);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException("DbHostConfiguration not found",
                                             e);
    } finally {
      DAOFactory.closeDAO(businessAccess);
    }
  }

  @Override
  protected final void checkValues() throws WaarpDatabaseSqlException {
    pojo.checkValues();
  }

  /**
   * @return the hostid
   */
  public final String getHostid() {
    return pojo.getHostid();
  }

  /**
   * @return the business
   */
  public final String getBusiness() {
    return pojo.getBusiness();
  }

  /**
   * @param business the business to set
   */
  public final void setBusiness(final String business) {
    this.pojo.setBusiness(business == null? "" : business);
    int len;
    do {
      len = this.pojo.getBusiness().length();
      this.pojo.setBusiness(
          WHITESPACES.matcher(this.pojo.getBusiness()).replaceAll(" "));
    } while (len != this.pojo.getBusiness().length());
    Configuration.configuration.getBusinessWhiteSet().clear();
    if (!this.pojo.getBusiness().isEmpty()) {
      readValuesFromXml(this.pojo.getBusiness(), businessDecl);
    }
    isSaved = false;
  }

  /**
   * @return the roles
   */
  public final String getRoles() {
    return pojo.getRoles();
  }

  /**
   * @param roles the roles to set
   */
  public final void setRoles(final String roles) {
    pojo.setRoles(roles == null? "" : roles);
    int len;
    do {
      len = getRoles().length();
      pojo.setRoles(WHITESPACES.matcher(pojo.getRoles()).replaceAll(" "));
    } while (len != pojo.getRoles().length());
    Configuration.configuration.getRoles().clear();
    if (!pojo.getRoles().isEmpty()) {
      readValuesFromXml(pojo.getRoles(), roleDecl);
    }
    isSaved = false;
  }

  /**
   * @return the aliases
   */
  public final String getAliases() {
    return pojo.getAliases();
  }

  /**
   * @param aliases the aliases to set
   */
  public final void setAliases(final String aliases) {
    pojo.setAliases(aliases == null? "" : aliases);
    int len;
    do {
      len = pojo.getAliases().length();
      pojo.setAliases(WHITESPACES.matcher(pojo.getAliases()).replaceAll(" "));
    } while (len != pojo.getAliases().length());
    Configuration.configuration.getAliases().clear();
    Configuration.configuration.getReverseAliases().clear();
    if (!pojo.getAliases().isEmpty()) {
      readValuesFromXml(pojo.getAliases(), aliasDecl);
    }
    isSaved = false;
  }

  @SuppressWarnings("unchecked")
  private void readValuesFromXml(final String input, final XmlDecl[] config) {
    if (ParametersChecker.isEmpty(input)) {
      logger.error(Messages.getString(
          "FileBasedConfiguration.CannotReadXml")); //$NON-NLS-1$
      return;
    }
    final Document document;
    final StringReader reader = new StringReader(input);
    // Open config file
    try {
      document = XmlUtil.getNewSaxReader().read(reader);
    } catch (final DocumentException e) {
      logger.error(
          Messages.getString("FileBasedConfiguration.CannotReadXml") + input +
          ": {}", e.getMessage()); //$NON-NLS-1$
      return;
    }
    if (document == null) {
      logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") +
                   input); //$NON-NLS-1$
      return;
    }
    final XmlValue[] configuration = XmlUtil.read(document, config);
    final XmlHash hashConfig = new XmlHash(configuration);
    XmlValue value = hashConfig.get(XML_BUSINESS);
    if (value != null && value.getList() != null) {
      final List<String> ids = (List<String>) value.getList();
      if (ids != null) {
        for (final String sval : ids) {
          if (sval.isEmpty()) {
            continue;
          }
          logger.info("Business Allow: {}", sval);
          Configuration.configuration.getBusinessWhiteSet().add(sval.trim());
        }
        ids.clear();
      }
    }
    value = hashConfig.get(XML_ALIASES);
    if (value != null && value.getList() != null) {
      for (final XmlValue[] xml : (Iterable<XmlValue[]>) value.getList()) {
        final XmlHash subHash = new XmlHash(xml);
        value = subHash.get(XML_REALID);
        if (value == null || value.isEmpty()) {
          continue;
        }
        final String refHostId = value.getString();
        value = subHash.get(XML_ALIASID);
        if (value == null || value.isEmpty()) {
          continue;
        }
        final String aliasset = value.getString();
        final String[] alias = SPACE_BACKSLASH.split(aliasset);
        for (final String namealias : alias) {
          Configuration.configuration.getAliases().put(namealias, refHostId);
        }
        Configuration.configuration.getReverseAliases().put(refHostId, alias);
        logger.info("Aliases for: {} = {}", refHostId, aliasset);
      }
    }
    value = hashConfig.get(XML_ROLES);
    if (value != null && value.getList() != null) {
      for (final XmlValue[] xml : (Iterable<XmlValue[]>) value.getList()) {
        final XmlHash subHash = new XmlHash(xml);
        value = subHash.get(XML_ROLEID);
        if (value == null || value.isEmpty()) {
          continue;
        }
        final String refHostId = value.getString();
        value = subHash.get(XML_ROLESET);
        if (value == null || value.isEmpty()) {
          continue;
        }
        final String roleset = value.getString();
        final String[] roles = SPACE_BACKSLASH.split(roleset);
        final RoleDefault newrole = new RoleDefault();
        for (final String role : roles) {
          try {
            final ROLE roletype = ROLE.valueOf(role.toUpperCase());
            if (roletype == ROLE.NOACCESS) {
              // reset
              newrole.setRole(roletype);
            } else {
              newrole.addRole(roletype);
            }
          } catch (final IllegalArgumentException e) {
            // ignore
          }
        }
        logger.info("New Role: {}:{}", refHostId, newrole);
        Configuration.configuration.getRoles().put(refHostId, newrole);
      }
    }
    hashConfig.clear();
  }

  /**
   * @return the others
   */
  public final String getOthers() {
    return pojo.getOthers();
  }

  /**
   * @param others the others to set
   */
  public final void setOthers(final String others) {
    pojo.setOthers(others == null? "" : others);
    int len;
    do {
      len = pojo.getOthers().length();
      pojo.setOthers(WHITESPACES.matcher(pojo.getOthers()).replaceAll(" "));
    } while (len != pojo.getOthers().length());
    isSaved = false;
  }

  /**
   * @return the element for the content of the other part
   */
  public final Element getOtherElement() {
    if (ParametersChecker.isNotEmpty(pojo.getOthers())) {
      final Document document;
      try {
        document = DocumentHelper.parseText(pojo.getOthers());
      } catch (final DocumentException e) {
        return DocumentHelper.createElement(OtherFields.root.name());
      }
      return document.getRootElement();
    } else {
      return DocumentHelper.createElement(OtherFields.root.name());
    }
  }

  /**
   * @param element the element to set as XML string to other part
   */
  public final void setOtherElement(final Element element) {
    setOthers(element.asXML());
  }

  @Override
  protected final void setFromJson(final String field, final JsonNode value) {
    if (value == null) {
      return;
    }
    for (final Columns column : Columns.values()) {
      if (column.name().equalsIgnoreCase(field)) {
        int len;
        switch (column) {
          case ALIASES:
            String aliases = value.asText();
            do {
              len = aliases.length();
              aliases = WHITESPACES.matcher(aliases).replaceAll(" ");
            } while (len != aliases.length());
            pojo.setAliases(aliases);
            break;
          case BUSINESS:
            String business = value.asText();
            do {
              len = business.length();
              business = WHITESPACES.matcher(business).replaceAll(" ");
            } while (len != business.length());
            pojo.setBusiness(business);
            break;
          case OTHERS:
            String others = value.asText();
            do {
              len = others.length();
              others = WHITESPACES.matcher(others).replaceAll(" ");
            } while (len != others.length());
            pojo.setOthers(others);
            break;
          case ROLES:
            String roles = value.asText();
            do {
              len = roles.length();
              roles = WHITESPACES.matcher(roles).replaceAll(" ");
            } while (len != roles.length());
            pojo.setRoles(roles);
            break;
          case UPDATEDINFO:
            pojo.setUpdatedInfo(
                org.waarp.openr66.pojo.UpdatedInfo.valueOf(value.asInt()));
            break;
          case HOSTID:
            pojo.setHostid(value.asText());
            break;
        }
      }
    }
  }

  /**
   * Private constructor for Commander only
   */
  private DbHostConfiguration() {
    pojo = new Business();
  }

  /**
   * For instance from Commander when getting updated information
   *
   * @param preparedStatement
   *
   * @return the next updated Configuration
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbHostConfiguration getFromStatement(
      final DbPreparedStatement preparedStatement)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbHostConfiguration dbHostConfiguration = new DbHostConfiguration();
    AbstractDAO<Business> businessDAO = null;
    try {
      businessDAO = dbHostConfiguration.getDao(false);
      dbHostConfiguration.pojo =
          ((StatementExecutor<Business>) businessDAO).getFromResultSet(
              preparedStatement.getResultSet());
      return dbHostConfiguration;
    } catch (final SQLException e) {
      DbConstant.error(e);
      throw new WaarpDatabaseSqlException("Getting values in error", e);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseSqlException("Getting values in error", e);
    } finally {
      DAOFactory.closeDAO(businessDAO);
    }
  }

  /**
   * @return the DbPreparedStatement for getting Updated Object
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbHostConfiguration[] getUpdatedPrepareStament()
      throws WaarpDatabaseNoConnectionException {
    final List<Filter> filters = new ArrayList<Filter>();
    filters.add(new Filter(DBBusinessDAO.HOSTID_FIELD, "=",
                           Configuration.configuration.getHostId()));
    filters.add(new Filter(DBBusinessDAO.UPDATED_INFO_FIELD, "=",
                           UpdatedInfo.TOSUBMIT.ordinal()));

    BusinessDAO businessAccess = null;
    List<Business> businesses;
    try {
      businessAccess = DAOFactory.getInstance().getBusinessDAO(false);
      businesses = businessAccess.find(filters);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } finally {
      DAOFactory.closeDAO(businessAccess);
    }
    final DbHostConfiguration[] res =
        new DbHostConfiguration[businesses.size()];
    int i = 0;
    for (final Business business : businesses) {
      res[i] = new DbHostConfiguration(business);
      res[i].isSaved = true;
      i++;
    }
    return res;
  }

  /**
   * @param session
   * @param hostid
   * @param business
   * @param role
   * @param alias
   * @param other
   *
   * @return a preparedStatement with the filter set
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getFilterPrepareStament(
      final DbSession session, final String hostid, final String business,
      final String role, final String alias, final String other)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(session);
    final String request = "SELECT " + selectAllFields + " FROM " + table;
    String condition = null;
    if (ParametersChecker.isNotEmpty(hostid)) {
      condition = " WHERE " + Columns.HOSTID.name() + " = '" + hostid + "' ";
    }
    if (ParametersChecker.isNotEmpty(business)) {
      if (condition != null) {
        condition +=
            " AND " + Columns.BUSINESS.name() + " LIKE '%" + business + "%' ";
      } else {
        condition =
            " WHERE " + Columns.BUSINESS.name() + " LIKE '%" + business + "%' ";
      }
    }
    if (ParametersChecker.isNotEmpty(role)) {
      if (condition != null) {
        condition += " AND " + Columns.ROLES.name() + " LIKE '%" + role + "%' ";
      } else {
        condition =
            " WHERE " + Columns.ROLES.name() + " LIKE '%" + role + "%' ";
      }
    }
    if (ParametersChecker.isNotEmpty(alias)) {
      if (condition != null) {
        condition +=
            " AND " + Columns.ALIASES.name() + " LIKE '%" + alias + "%' ";
      } else {
        condition =
            " WHERE " + Columns.ALIASES.name() + " LIKE '%" + alias + "%' ";
      }
    }
    if (ParametersChecker.isNotEmpty(other)) {
      if (condition != null) {
        condition +=
            " AND " + Columns.OTHERS.name() + " LIKE '%" + other + "%' ";
      } else {
        condition =
            " WHERE " + Columns.OTHERS.name() + " LIKE '%" + other + "%' ";
      }
    }
    if (condition != null) {
      preparedStatement.createPrepareStatement(
          request + condition + " ORDER BY " + Columns.HOSTID.name());
    } else {
      preparedStatement.createPrepareStatement(
          request + " ORDER BY " + Columns.HOSTID.name());
    }
    return preparedStatement;
  }

  @Override
  public final void changeUpdatedInfo(final UpdatedInfo info) {
    isSaved = false;
    pojo.setUpdatedInfo(org.waarp.openr66.pojo.UpdatedInfo.fromLegacy(info));
  }

  /**
   * Update configuration according to new values
   */
  public final void updateConfiguration() {
    updateHostConfiguration(Configuration.configuration, this);
  }

  /**
   * @return True if this Configuration refers to the current host
   */
  public final boolean isOwnConfiguration() {
    return pojo.getHostid().equals(Configuration.configuration.getHostId());
  }

  /**
   * Shortcut to add all paths element from source into set
   *
   * @param source
   * @param path
   * @param set
   *
   * @return True if ok
   */
  private boolean updateSet(final String source, final String path,
                            final HashSet<String> set) {
    if (ParametersChecker.isNotEmpty(source)) {
      final Document document;
      StringReader reader = null;
      try {
        reader = new StringReader(source);
        document = XmlUtil.getNewSaxReader().read(reader);
      } catch (final DocumentException e) {
        logger.error(
            "Unable to read the XML Config " + path + " string: " + source +
            ": {}", e.getMessage());
        FileUtils.close(reader);
        return false;
      }
      if (document == null) {
        logger.error(
            "Unable to read the XML Config " + path + " string: " + source);
        FileUtils.close(reader);
        return false;
      }
      final List<Node> list = document.selectNodes(path);
      for (final Node element : list) {
        final String sval = element.getText().trim();
        if (sval.isEmpty()) {
          continue;
        }
        set.add(sval.trim());
      }
      list.clear();
      document.clearContent();
      FileUtils.close(reader);
    }
    return true;
  }

  /**
   * update Business with possible purge and new or added content, and
   * updating
   * in memory information
   *
   * @param config
   * @param newbusiness
   * @param purged
   *
   * @return True if updated
   */
  public final boolean updateBusiness(final Configuration config,
                                      final String newbusiness,
                                      final boolean purged) {
    final HashSet<String> set = new HashSet<String>();
    if (!updateSet(newbusiness, XML_BUSINESS + '/' + XML_BUSINESSID, set)) {
      return false;
    }
    if (purged) {
      config.getBusinessWhiteSet().clear();
      isSaved = false;
    } else {
      final String businessStr = getBusiness();
      if (!updateSet(businessStr, XML_BUSINESS + '/' + XML_BUSINESSID, set)) {
        return false;
      }
    }
    config.getBusinessWhiteSet().addAll(set);
    if (ParametersChecker.isNotEmpty(newbusiness) || purged) {
      final Document document = DocumentHelper.createDocument(
          DocumentHelper.createElement(XML_BUSINESS));
      final Element root = document.getRootElement();
      for (final String sval : set) {
        root.addElement(XML_BUSINESSID).setText(sval);
        logger.info("Business Allow: {}", sval);
      }
      setBusiness(root.asXML());
      try {
        update();
      } catch (final WaarpDatabaseException e) {
        document.clearContent();
        return false;
      }
      document.clearContent();
    }
    set.clear();
    return true;
  }

  /**
   * Update the DbHostConfiguration from Configuration
   *
   * @param configuration
   */
  public final void updateFromConfiguration(final Configuration configuration) {
    // Business
    if (!configuration.getBusinessWhiteSet().isEmpty()) {
      final Document document = DocumentHelper.createDocument(
          DocumentHelper.createElement(XML_BUSINESS));
      final Element root = document.getRootElement();
      for (final String sval : configuration.getBusinessWhiteSet()) {
        root.addElement(XML_BUSINESSID).setText(sval);
        logger.info("Business Allow: {}", sval);
      }
      final String xml = root.asXML();
      this.pojo.setBusiness(xml);
      isSaved = false;
      document.clearContent();
    }
    // Aliases
    if (!configuration.getAliases().isEmpty()) {
      final Document document = DocumentHelper.createDocument(
          DocumentHelper.createElement(XML_ALIASES));
      final Element root = document.getRootElement();
      for (final Entry<String, String[]> entry : configuration.getReverseAliases()
                                                              .entrySet()) {
        final Element elt = root.addElement(XML_ALIAS);
        elt.addElement(XML_REALID).setText(entry.getKey());
        StringBuilder cumul = null;
        for (final String namealias : entry.getValue()) {
          if (cumul == null) {
            cumul = new StringBuilder(namealias);
          } else {
            cumul.append(' ').append(namealias);
          }
        }
        if (cumul == null) {
          cumul = new StringBuilder();
        }
        elt.addElement(XML_ALIASID).setText(cumul.toString());
      }
      final String xml = root.asXML();
      this.pojo.setAliases(xml);
      isSaved = false;
      document.clearContent();
    }

    // Role
    if (!configuration.getRoles().isEmpty()) {
      final Document document = DocumentHelper.createDocument(
          DocumentHelper.createElement(XML_ROLES));
      final Element root = document.getRootElement();
      for (final Entry<String, RoleDefault> entry : configuration.getRoles()
                                                                 .entrySet()) {
        final Element elt = root.addElement(XML_ROLE);
        elt.addElement(XML_ROLEID).setText(entry.getKey());
        StringBuilder cumul = null;
        final RoleDefault roleDefault = entry.getValue();
        if (roleDefault.hasNoAccess()) {
          cumul = new StringBuilder(ROLE.NOACCESS.name());
        } else {
          for (final ROLE role : ROLE.values()) {
            if (role == ROLE.NOACCESS) {
              continue;
            }
            if (roleDefault.isContaining(role)) {
              if (cumul == null) {
                cumul = new StringBuilder(role.name().toUpperCase());
              } else {
                cumul.append(' ').append(role.name().toUpperCase());
              }
            }
          }
        }
        if (cumul == null) {
          cumul = new StringBuilder();
        }
        logger.info("New Role: {}:{}", entry.getKey(), cumul);
        elt.addElement(XML_ROLESET).setText(cumul.toString());
      }
      final String xml = root.asXML();
      this.pojo.setRoles(xml);
      isSaved = false;
      document.clearContent();
    }

    // Now save (insert or update)
    try {
      update();
    } catch (final WaarpDatabaseException e) {
      try {
        insert();
      } catch (final WaarpDatabaseException waarpDatabaseException) {
        // Real issue there
        logger.error("Cannot update neither save DbHostConfiguration for ",
                     this.pojo.getHostid());
      }
    }
  }

  /**
   * Shortcut to add all paths element with key and value from source into map
   *
   * @param source
   * @param path
   * @param keypath
   * @param valpath
   * @param split
   * @param map
   *
   * @return True if ok
   */
  private boolean updateMap(final String source, final String path,
                            final String keypath, final String valpath,
                            final String split,
                            final HashMap<String, HashSet<String>> map) {
    if (ParametersChecker.isNotEmpty(source)) {
      final Document document;
      StringReader reader = null;
      try {
        reader = new StringReader(source);
        document = XmlUtil.getNewSaxReader().read(reader);
      } catch (final DocumentException e) {
        logger.error(
            "Unable to read the XML Config " + path + " string: " + source +
            ": {}", e.getMessage());
        FileUtils.close(reader);
        return false;
      }
      if (document == null) {
        logger.error(
            "Unable to read the XML Config " + path + " string: " + source);
        FileUtils.close(reader);
        return false;
      }
      final List<Node> list = document.selectNodes(path);
      for (final Node element : list) {
        final Element nodeid = (Element) element.selectSingleNode(keypath);
        if (nodeid == null) {
          continue;
        }
        final Element nodeset = (Element) element.selectSingleNode(valpath);
        if (nodeset == null) {
          continue;
        }
        final String refHostId = nodeid.getText();
        final String aliasesid = nodeset.getText();
        final String[] aliasid = aliasesid.split(split);
        final HashSet<String> set;
        if (map.containsKey(refHostId)) {
          set = map.get(refHostId);
        } else {
          set = new HashSet<String>();
        }
        set.addAll(Arrays.asList(aliasid));
        map.put(refHostId, set);
      }
      list.clear();
      document.clearContent();
      FileUtils.close(reader);
    }
    return true;
  }

  /**
   * update Alias with possible purge and new or added content, and updating
   * in
   * memory information
   *
   * @param config
   * @param newalias
   * @param purged
   *
   * @return True if updated
   */
  public final boolean updateAlias(final Configuration config,
                                   final String newalias,
                                   final boolean purged) {
    final HashMap<String, HashSet<String>> map =
        new HashMap<String, HashSet<String>>();
    if (!updateMap(newalias, XML_ALIASES + '/' + XML_ALIAS, XML_REALID,
                   XML_ALIASID, " |\\|", map)) {
      return false;
    }
    if (purged) {
      config.getReverseAliases().clear();
      config.getAliases().clear();
      isSaved = false;
    } else {
      final String alias = getAliases();
      if (!updateMap(alias, XML_ALIASES + '/' + XML_ALIAS, XML_REALID,
                     XML_ALIASID, " |\\|", map)) {
        return false;
      }
    }
    if (ParametersChecker.isNotEmpty(newalias) || purged) {
      final Document document = DocumentHelper.createDocument(
          DocumentHelper.createElement(XML_ALIASES));
      final Element root = document.getRootElement();
      for (final Entry<String, HashSet<String>> entry : map.entrySet()) {
        final Element elt = root.addElement(XML_ALIAS);
        elt.addElement(XML_REALID).setText(entry.getKey());
        StringBuilder cumul = null;
        final String[] oldAlias =
            config.getReverseAliases().get(entry.getKey());
        final int size = oldAlias == null? 0 : oldAlias.length;
        final String[] alias = new String[entry.getValue().size() + size];
        int i = 0;
        if (oldAlias != null) {
          System.arraycopy(oldAlias, 0, alias, 0, size);
        }
        for (final String namealias : entry.getValue()) {
          config.getAliases().put(namealias, entry.getKey());
          if (cumul == null) {
            cumul = new StringBuilder(namealias);
          } else {
            cumul.append(' ').append(namealias);
          }
          alias[i] = namealias;
          i++;
        }
        if (cumul == null) {
          cumul = new StringBuilder();
        }
        elt.addElement(XML_ALIASID).setText(cumul.toString());
        config.getReverseAliases().put(entry.getKey(), alias);
      }
      setAliases(root.asXML());
      try {
        update();
      } catch (final WaarpDatabaseException e) {
        document.clearContent();
        return false;
      }
      document.clearContent();
    } else {
      for (final Entry<String, HashSet<String>> entry : map.entrySet()) {
        final String[] oldAlias =
            config.getReverseAliases().get(entry.getKey());
        final int size = oldAlias == null? 0 : oldAlias.length;
        final String[] alias = new String[entry.getValue().size() + size];
        int i = 0;
        if (oldAlias != null) {
          System.arraycopy(oldAlias, 0, alias, 0, size);
        }
        for (final String namealias : entry.getValue()) {
          config.getAliases().put(namealias, entry.getKey());
          alias[i] = namealias;
          i++;
        }
        config.getReverseAliases().put(entry.getKey(), alias);
      }
    }
    map.clear();
    return true;
  }

  /**
   * update Roles with possible purge and new or added content, and updating
   * in
   * memory information
   *
   * @param config
   * @param newroles
   * @param purged
   *
   * @return True if ok
   */
  public final boolean updateRoles(final Configuration config,
                                   final String newroles,
                                   final boolean purged) {
    final HashMap<String, HashSet<String>> map =
        new HashMap<String, HashSet<String>>();
    if (!updateMap(newroles, XML_ROLES + '/' + XML_ROLE, XML_ROLEID,
                   XML_ROLESET, " |\\|", map)) {
      return false;
    }
    if (purged) {
      config.getRoles().clear();
      isSaved = false;
    } else {
      final String roles = getRoles();
      if (!updateMap(roles, XML_ROLES + '/' + XML_ROLE, XML_ROLEID, XML_ROLESET,
                     " |\\|", map)) {
        return false;
      }
    }
    if (ParametersChecker.isNotEmpty(newroles) || purged) {
      final Document document = DocumentHelper.createDocument(
          DocumentHelper.createElement(XML_ROLES));
      final Element root = document.getRootElement();
      for (final Entry<String, HashSet<String>> entry : map.entrySet()) {
        final RoleDefault newrole = new RoleDefault();
        final Element elt = root.addElement(XML_ROLE);
        elt.addElement(XML_ROLEID).setText(entry.getKey());
        StringBuilder cumul = null;
        if (entry.getValue().contains(ROLE.NOACCESS.name())) {
          newrole.setRole(ROLE.NOACCESS);
          cumul = new StringBuilder(ROLE.NOACCESS.name());
        }
        for (final String namerole : entry.getValue()) {
          try {
            final ROLE roletype = ROLE.valueOf(namerole.toUpperCase());
            if (roletype != ROLE.NOACCESS) {
              newrole.addRole(roletype);
              if (cumul == null) {
                cumul = new StringBuilder(namerole.toUpperCase());
              } else {
                cumul.append(' ').append(namerole.toUpperCase());
              }
            }
          } catch (final IllegalArgumentException e) {
            // ignore
          }
        }
        if (cumul == null) {
          cumul = new StringBuilder();
        }
        logger.info("New Role: {}:{}", entry.getKey(), newrole);
        config.getRoles().put(entry.getKey(), newrole);
        elt.addElement(XML_ROLESET).setText(cumul.toString());
      }
      setRoles(root.asXML());
      try {
        update();
      } catch (final WaarpDatabaseException e) {
        document.clearContent();
        return false;
      }
      document.clearContent();
    } else {
      for (final Entry<String, HashSet<String>> entry : map.entrySet()) {
        final RoleDefault newrole = new RoleDefault();
        if (entry.getValue().contains(ROLE.NOACCESS.name())) {
          newrole.setRole(ROLE.NOACCESS);
        }
        for (final String namerole : entry.getValue()) {
          try {
            final ROLE roletype = ROLE.valueOf(namerole.toUpperCase());
            if (roletype != ROLE.NOACCESS) {
              newrole.addRole(roletype);
            }
          } catch (final IllegalArgumentException e) {
            // ignore
          }
        }
        logger.info("New Role: {}:{}", entry.getKey(), newrole);
        config.getRoles().put(entry.getKey(), newrole);
      }
    }
    map.clear();
    return true;
  }

  public static void updateHostConfiguration(final Configuration config,
                                             final DbHostConfiguration hostConfiguration) {
    hostConfiguration.updateBusiness(config, null, false);
    hostConfiguration.updateAlias(config, null, false);
    hostConfiguration.updateRoles(config, null, false);
  }

  /**
   * @param hostid
   *
   * @return the version of the database from HostConfiguration table
   */
  public static String getVersionDb(final String hostid) {
    final DbHostConfiguration hostConfiguration;
    try {
      hostConfiguration = new DbHostConfiguration(hostid);
    } catch (final WaarpDatabaseException e) {
      // ignore and return
      return "1.1.0";
    }
    final Element others = hostConfiguration.getOtherElement();
    if (others != null) {
      final Element version =
          (Element) others.selectSingleNode(OtherFields.version.name());
      if (version != null) {
        return version.getText();
      }
    }
    return "1.1.0";
  }

  public final boolean isSeeAllId(final String id) {
    final Element others = getOtherElement();
    if (others != null) {
      final Element seeallids =
          (Element) others.selectSingleNode(OtherFields.seeallid.name());
      if (seeallids != null) {
        final String[] split = COMMA.split(seeallids.getText());
        for (final String string : split) {
          if (string.equals(id)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Update the version for this HostId
   *
   * @param hostid
   * @param version
   *
   * @return the version updated
   */
  public static String updateVersionDb(final String hostid,
                                       final String version) {
    DbHostConfiguration hostConfiguration;
    try {
      hostConfiguration = new DbHostConfiguration(hostid);
    } catch (final WaarpDatabaseNoDataException e) {
      try {
        hostConfiguration = new DbHostConfiguration(hostid, "", "", "", "");
        hostConfiguration.insert();
      } catch (final WaarpDatabaseException e1) {
        logger.debug("Not inserted?", e1);
        // ignore and return
        return R66Versions.V2_4_12.name();
      }
    } catch (final WaarpDatabaseException e) {
      logger.debug("Not found?", e);
      // ignore and return
      return R66Versions.V2_4_12.name();
    }
    Element others = hostConfiguration.getOtherElement();
    if (others != null) {
      final Element eversion =
          (Element) others.selectSingleNode(OtherFields.version.name());
      if (eversion != null) {
        final String currentVersion = eversion.getText();
        if (PartnerConfiguration.isVersion2GTVersion1(currentVersion,
                                                      version)) {
          eversion.setText(version);
        } else {
          return currentVersion;
        }
      } else {
        others.addElement(OtherFields.version.name()).addText(Version.ID);
      }
    } else {
      others = DocumentHelper.createElement(OtherFields.root.name());
      others.addElement(OtherFields.version.name()).addText(Version.ID);
    }
    hostConfiguration.setOtherElement(others);
    try {
      hostConfiguration.update();
    } catch (final WaarpDatabaseException e) {
      logger.debug("Not update?", e);
      // ignore
    }
    return version;
  }

  /**
   * @return the last DateTime for Monitoring
   */
  public final DateTime getLastDateTimeMonitoring() {
    final Element others = getOtherElement();
    if (others != null) {
      final Element lastMonitoringDT = (Element) others.selectSingleNode(
          OtherFields.lastMonitoringDateTime.name());
      if (lastMonitoringDT != null) {
        final String dateTime = lastMonitoringDT.getText();
        if (ParametersChecker.isNotEmpty(dateTime)) {
          try {
            return DateTime.parse(lastMonitoringDT.getText());
          } catch (final Exception ignored) {
            return null;
          }
        }
      }
    }
    return null;
  }

  /**
   * Update the last DateTime for Monitoring for this HostId
   *
   * @param lastDateTime
   */
  public final void updateLastDateTimeMonitoring(final DateTime lastDateTime) {
    Element others = getOtherElement();
    if (others != null) {
      final Element lastMonitoringDT = (Element) others.selectSingleNode(
          OtherFields.lastMonitoringDateTime.name());
      if (lastMonitoringDT != null) {
        lastMonitoringDT.setText(lastDateTime.toString());
      } else {
        others.addElement(OtherFields.lastMonitoringDateTime.name())
              .addText(lastDateTime.toString());
      }
    } else {
      others = DocumentHelper.createElement(OtherFields.root.name());
      others.addElement(OtherFields.lastMonitoringDateTime.name())
            .addText(lastDateTime.toString());
    }
    setOtherElement(others);
    try {
      update();
    } catch (final WaarpDatabaseException e) {
      logger.debug("Not update?", e);
      // ignore
    }
  }

  /**
   * @param hostid
   *
   * @return the last DateTime for Monitoring
   */
  public static DateTime getLastDateTimeMonitoring(final String hostid) {
    DbHostConfiguration hostConfiguration;
    try {
      hostConfiguration = new DbHostConfiguration(hostid);
    } catch (final WaarpDatabaseNoDataException e) {
      try {
        hostConfiguration = new DbHostConfiguration(hostid, "", "", "", "");
        hostConfiguration.insert();
      } catch (final WaarpDatabaseException e1) {
        logger.debug("Not inserted?", e1);
        // ignore and return
        return null;
      }
    } catch (final WaarpDatabaseException e) {
      logger.debug("Not found?", e);
      // ignore and return
      return null;
    }
    return hostConfiguration.getLastDateTimeMonitoring();
  }

  /**
   * Update the last DateTime for Monitoring for this HostId
   *
   * @param hostid
   * @param lastDateTime
   */
  public static void updateLastDateTimeMonitoring(final String hostid,
                                                  final DateTime lastDateTime) {
    DbHostConfiguration hostConfiguration;
    try {
      hostConfiguration = new DbHostConfiguration(hostid);
    } catch (final WaarpDatabaseNoDataException e) {
      try {
        hostConfiguration = new DbHostConfiguration(hostid, "", "", "", "");
        hostConfiguration.insert();
      } catch (final WaarpDatabaseException e1) {
        logger.debug("Not inserted?", e1);
        // ignore and return
        return;
      }
    } catch (final WaarpDatabaseException e) {
      logger.debug("Not found?", e);
      // ignore and return
      return;
    }
    hostConfiguration.updateLastDateTimeMonitoring(lastDateTime);
  }
}
