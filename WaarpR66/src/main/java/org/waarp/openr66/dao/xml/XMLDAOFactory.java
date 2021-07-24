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

public class XMLDAOFactory extends DAOFactory {

  public XMLDAOFactory() {
    // Empty
  }

  @Override
  public BusinessDAO getBusinessDAO(final boolean isCacheable) {
    return new XMLBusinessDAO();
  }

  @Override
  public HostDAO getHostDAO(final boolean isCacheable) {
    return new XMLHostDAO();
  }

  @Override
  public LimitDAO getLimitDAO(final boolean isCacheable)
      throws DAOConnectionException {
    return new XMLLimitDAO();
  }

  @Override
  public MultipleMonitorDAO getMultipleMonitorDAO(final boolean isCacheable)
      throws DAOConnectionException {
    throw new DAOConnectionException(
        "MultipleMonitor is not supported on XML DAO");
  }

  @Override
  public RuleDAO getRuleDAO(final boolean isCacheable) {
    return new XMLRuleDAO();
  }

  @Override
  public TransferDAO getTransferDAO() {
    return new XMLTransferDAO();
  }
}
