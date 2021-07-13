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

package org.waarp.openr66.dao;

import org.waarp.common.database.ConnectionFactory;
import org.waarp.openr66.dao.database.DBDAOFactory;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.xml.XMLDAOFactory;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;

/**
 * Abstract class to create DAOFactory
 */
public abstract class DAOFactory {

  private static DAOFactory instance;

  public static void initialize() {
    if (instance == null) {
      instance = new XMLDAOFactory();
    }
  }

  public static void initialize(final ConnectionFactory factory) {
    if (instance == null) {
      instance = new DBDAOFactory(factory);
    }
  }

  public static DAOFactory getInstance() {
    return instance;
  }

  /**
   * OWASP security
   *
   * @return the {@link DocumentBuilderFactory} ready
   */
  public static DocumentBuilderFactory getDocumentBuilderFactory() {
    final DocumentBuilderFactory factory = // NOSONAR
        DocumentBuilderFactory.newInstance(); // NOSONAR
    // disable external entities
    try {
      factory.setFeature(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
                         Boolean.FALSE);
    } catch (final ParserConfigurationException ignored) {
      // nothing
    } catch (final AbstractMethodError ignored) {
      // nothing
    }
    try {
      factory.setFeature(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    } catch (final ParserConfigurationException ignored) {
      // nothing
    } catch (final AbstractMethodError ignored) {
      // nothing
    }
    try {
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
    } catch (final ParserConfigurationException ignored) {
      // nothing
    } catch (final AbstractMethodError ignored) {
      // nothing
    }
    return factory;
  }

  /**
   * Close the DAO
   *
   * @param dao
   */
  public static void closeDAO(final AbstractDAO<?> dao) {
    if (dao != null) {
      dao.close();
    }
  }

  /**
   * @return the current configuration of the database maximum connections
   */
  public int getMaxConnections() {
    return 1000;
  }

  /**
   * Return a BusinessDAO
   *
   * @return a ready to use BusinessDAO
   *
   * @throws DAOConnectionException if cannot create the DAO
   * @param isCacheable
   */
  public abstract BusinessDAO getBusinessDAO(final boolean isCacheable) throws DAOConnectionException;

  /**
   * Return a HostDAO
   *
   * @return a ready to use HostDAO
   *
   * @throws DAOConnectionException if cannot create the DAO
   * @param isCacheable
   */
  public abstract HostDAO getHostDAO(final boolean isCacheable) throws DAOConnectionException;

  /**
   * Return a LimitDAO
   *
   * @return a ready to use LimitDAO
   *
   * @throws DAOConnectionException if cannot create the DAO
   * @param isCacheable
   */
  public abstract LimitDAO getLimitDAO(final boolean isCacheable) throws DAOConnectionException;

  /**
   * Return a MultipleMonitorDAO
   *
   * @return a ready to use MultipleMonitorDAO
   *
   * @throws DAOConnectionException if cannot create the DAO
   * @param isCacheable
   */
  public abstract MultipleMonitorDAO getMultipleMonitorDAO(
      final boolean isCacheable)
      throws DAOConnectionException;

  /**
   * Return a RuleDAO
   *
   * @return a ready to use RuleDAO
   *
   * @throws DAOConnectionException if cannot create the DAO
   * @param isCacheable
   */
  public abstract RuleDAO getRuleDAO(final boolean isCacheable) throws DAOConnectionException;

  /**
   * Return a TransferDAO
   *
   * @return a ready to use TramsferDAO
   *
   * @throws DAOConnectionException if cannot create the DAO
   */
  public abstract TransferDAO getTransferDAO() throws DAOConnectionException;
}
