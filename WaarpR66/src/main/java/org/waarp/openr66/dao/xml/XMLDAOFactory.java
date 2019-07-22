package org.waarp.openr66.dao.xml;

import org.waarp.openr66.dao.*;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.protocol.configuration.Configuration;

public class XMLDAOFactory extends DAOFactory {

    private String confDir = Configuration.configuration.getConfigPath();

    private String businessFile = confDir + "/business.xml";
    private String hostFile = Configuration.configuration.getAUTH_FILE();
    private String limitFile = confDir + "/limit.xml";
    private String ruleFile = confDir;
    private String transferFile = Configuration.configuration.getArchivePath();

    public XMLDAOFactory() {}


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
    public MultipleMonitorDAO getMultipleMonitorDAO() throws
                                                      DAOConnectionException {
        throw new DAOConnectionException("MultipleMonitor is not supported on XML DAO");
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
