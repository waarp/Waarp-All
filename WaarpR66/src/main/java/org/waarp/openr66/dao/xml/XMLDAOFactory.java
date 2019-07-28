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

package org.waarp.openr66.dao.xml;

import org.waarp.openr66.dao.BusinessDAO;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.HostDAO;
import org.waarp.openr66.dao.LimitDAO;
import org.waarp.openr66.dao.MultipleMonitorDAO;
import org.waarp.openr66.dao.RuleDAO;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.protocol.configuration.Configuration;

public class XMLDAOFactory extends DAOFactory {

  private final String confDir = Configuration.configuration.getConfigPath();

  private final String businessFile = confDir + "/business.xml";
  private final String hostFile = Configuration.configuration.getAuthFile();
  private final String limitFile = confDir + "/limit.xml";
  private final String ruleFile = confDir;
  private final String transferFile =
      Configuration.configuration.getArchivePath();

  public XMLDAOFactory() {
  }

  @Override
  public BusinessDAO getBusinessDAO() throws DAOConnectionException {
    return new XMLBusinessDAO(businessFile);
  }

  @Override
  public HostDAO getHostDAO() throws DAOConnectionException {
    return new XMLHostDAO(hostFile);
  }

  @Override
  public LimitDAO getLimitDAO() throws DAOConnectionException {
    return new XMLLimitDAO(limitFile);
  }

  @Override
  public MultipleMonitorDAO getMultipleMonitorDAO()
      throws DAOConnectionException {
    throw new DAOConnectionException(
        "MultipleMonitor is not supported on XML DAO");
  }

  @Override
  public RuleDAO getRuleDAO() throws DAOConnectionException {
    return new XMLRuleDAO(ruleFile);
  }

  @Override
  public TransferDAO getTransferDAO() throws DAOConnectionException {
    return new XMLTransferDAO(transferFile);
  }
}
