package org.waarp.openr66.dao.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.configuration.ExtensionFilter;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.RuleDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.pojo.Rule;
import org.waarp.openr66.pojo.RuleTask;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class XMLRuleDAO implements RuleDAO {

    private static final WaarpLogger logger = WaarpLoggerFactory.getLogger(XMLRuleDAO.class);

    /**
     * HashTable in case of lack of database
     */
    private static final ConcurrentHashMap<String, Rule> dbR66RuleHashMap =
        new ConcurrentHashMap<String, Rule>();

    public static final String ROOT_LIST = "rules";
    public static final String ROOT_ELEMENT = "rule";

    public static final String RULENAME_FIELD = "idrule";
    public static final String HOSTIDS_LIST = "hostids";
    public static final String MODE_FIELD = "mode";
    public static final String SEND_PATH_FIELD = "sendpath";
    public static final String RECV_PATH_FIELD = "recvpath";
    public static final String ARCH_PATH_FIELD = "archivepath";
    public static final String WORK_PATH_FIELD = "workpath";
    public static final String RPRE_TASKS_FIELD = "rpretasks";
    public static final String RPOST_TASKS_FIELD = "rposttasks";
    public static final String RERROR_TASKS_FIELD = "rerrortasks";
    public static final String SPRE_TASKS_FIELD = "spretasks";
    public static final String SPOST_TASKS_FIELD = "sposttasks";
    public static final String SERROR_TASKS_FIELD = "serrortasks";

    private static final String XML_SELECT = "//rule[idrule=$idrule]";
    private static final String XML_GET_ALL= "//rule";

    private File file;

    public XMLRuleDAO(String filePath) {
        this.file = new File(filePath);
    }

    public void close() {}

    public static final String EXT_RULE = ".rule.xml";
    public static final String EXT_RULES = ".rules.xml";

    private File[] getRuleFiles() {
        File ruleDir = new File(Configuration.configuration.getBaseDirectory() +
                Configuration.configuration.getConfigPath());
        List<File> res = new ArrayList<File>();
        if(ruleDir.isDirectory()) {
            res.addAll(Arrays.asList(
                    ruleDir.listFiles(new ExtensionFilter(EXT_RULE))));
            res.addAll(Arrays.asList(
                    ruleDir.listFiles(new ExtensionFilter(EXT_RULES))));
        }
        return res.toArray(new File[0]);
    }

    public void delete(Rule rule) throws DAOConnectionException {
        dbR66RuleHashMap.remove(rule.getName());
    }

    public void deleteAll() throws DAOConnectionException {
        dbR66RuleHashMap.clear();
    }

    public List<Rule> getAll() throws DAOConnectionException {
        List<Rule> res = new ArrayList<Rule>();

        File[] files = getRuleFiles();
        for (File ruleFile : files) {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                Document document = dbf.newDocumentBuilder().parse(ruleFile);
                // Setup XPath query
                XPath xPath = XPathFactory.newInstance().newXPath();
                XPathExpression xpe = xPath.compile(XML_GET_ALL);
                NodeList listNode = (NodeList) xpe.evaluate(document,
                        XPathConstants.NODESET);
                // Iterate through all found nodes

                for (int i = 0; i < listNode.getLength(); i++) {
                    Node node = listNode.item(i);
                    Rule rule = getFromNode(node);
                    res.add(rule);
                    dbR66RuleHashMap.put(rule.getName(), rule);
                }
            } catch (SAXException e) {
                throw new DAOConnectionException(e);
            } catch (XPathExpressionException e) {
                throw new DAOConnectionException(e);
            } catch (ParserConfigurationException e) {
                throw new DAOConnectionException(e);
            } catch (IOException e) {
                throw new DAOConnectionException(e);
            }
        }
        return res;
    }

    public boolean exist(String rulename) throws DAOConnectionException {
        return dbR66RuleHashMap.containsKey(rulename);
    }

    public List<Rule> find(List<Filter> fitlers) throws DAOConnectionException {
        throw new DAOConnectionException("Operation not supported on XML DAO");
    }

    public void insert(Rule rule) throws DAOConnectionException {
        dbR66RuleHashMap.put(rule.getName(), rule);
    }

    public Rule select(String rulename)
        throws DAOConnectionException, DAONoDataException {
        if (exist(rulename)) {
            return dbR66RuleHashMap.get(rulename);
        }
        throw new DAONoDataException("Rule cannot be found");
    }

    public void update(Rule rule) throws DAOConnectionException {
        dbR66RuleHashMap.put(rule.getName(), rule);
    }

    private Rule getFromNode(Node parent) throws DAOConnectionException {
        Rule res = new Rule();

        NodeList children = parent.getChildNodes();
        for (int j = 0; j < children.getLength(); j++) {
            Node node = children.item(j);
            if (node.getNodeName().equals(RULENAME_FIELD)) {
                res.setName(node.getTextContent());
            } else if (node.getNodeName().equals(HOSTIDS_LIST)) {
                res.setHostids(retrieveHostids(node.getTextContent()));
            } else if (node.getNodeName().equals(MODE_FIELD)) {
                res.setMode(Integer.parseInt(node.getTextContent()));
            } else if (node.getNodeName().equals(SEND_PATH_FIELD)) {
                res.setSendPath(node.getTextContent());
            } else if (node.getNodeName().equals(RECV_PATH_FIELD)) {
                res.setRecvPath(node.getTextContent());
            } else if (node.getNodeName().equals(ARCH_PATH_FIELD)) {
                res.setArchivePath(node.getTextContent());
            } else if (node.getNodeName().equals(WORK_PATH_FIELD)) {
                res.setWorkPath(node.getTextContent());
            } else if (node.getNodeName().equals(RPRE_TASKS_FIELD)) {
                res.setRPreTasks(retrieveTasks(node));
            } else if (node.getNodeName().equals(RPOST_TASKS_FIELD)) {
                res.setRPostTasks(retrieveTasks(node));
            } else if (node.getNodeName().equals(RERROR_TASKS_FIELD)) {
                res.setRErrorTasks(retrieveTasks(node));
            } else if (node.getNodeName().equals(SPRE_TASKS_FIELD)) {
                res.setSPreTasks(retrieveTasks(node));
            } else if (node.getNodeName().equals(SPOST_TASKS_FIELD)) {
                res.setSPostTasks(retrieveTasks(node));
            } else if (node.getNodeName().equals(SERROR_TASKS_FIELD)) {
                res.setSErrorTasks(retrieveTasks(node));
            }
        }
        return res;
    }

    private Node getNode(Document doc, Rule rule) {
        Node res = doc.createElement(ROOT_ELEMENT);
        res.appendChild(XMLUtils.createNode(doc, RULENAME_FIELD,
                rule.getName()));
        res.appendChild(XMLUtils.createNode(doc, HOSTIDS_LIST,
                rule.getXMLHostids()));
        res.appendChild(XMLUtils.createNode(doc, MODE_FIELD,
                Integer.toString(rule.getMode())));
        res.appendChild(XMLUtils.createNode(doc, SEND_PATH_FIELD,
                rule.getSendPath()));
        res.appendChild(XMLUtils.createNode(doc, RECV_PATH_FIELD,
                rule.getRecvPath()));
        res.appendChild(XMLUtils.createNode(doc, ARCH_PATH_FIELD,
                rule.getArchivePath()));
        res.appendChild(XMLUtils.createNode(doc, WORK_PATH_FIELD,
                rule.getWorkPath()));
        res.appendChild(XMLUtils.createNode(doc, RPRE_TASKS_FIELD,
                rule.getXMLRPreTasks()));
        res.appendChild(XMLUtils.createNode(doc, RPOST_TASKS_FIELD,
                rule.getXMLRPostTasks()));
        res.appendChild(XMLUtils.createNode(doc, RERROR_TASKS_FIELD,
                rule.getXMLRErrorTasks()));
        res.appendChild(XMLUtils.createNode(doc, SPRE_TASKS_FIELD,
                rule.getXMLSPreTasks()));
        res.appendChild(XMLUtils.createNode(doc, SPOST_TASKS_FIELD,
                rule.getXMLSPostTasks()));
        res.appendChild(XMLUtils.createNode(doc, SERROR_TASKS_FIELD,
                rule.getXMLSErrorTasks()));

        return res;
    }

    public static final String HOSTID_FIELD = "hostid";

    private List<String> retrieveHostids(String xml) throws
                                                     DAOConnectionException {
        ArrayList<String> res = new ArrayList<String>();
        if ((xml == null) || xml.equals("")) {
            return res;
        }
        Document document = null;
        InputStream stream = null;
        try {
            stream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            document = DocumentBuilderFactory.newInstance().
                    newDocumentBuilder().parse(stream);
        } catch (IOException e) {
            throw new DAOConnectionException(e);
        } catch (ParserConfigurationException e) {
            throw new DAOConnectionException(e);
        } catch (SAXException e) {
            throw new DAOConnectionException(e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    logger.warn("Cannot properly close input stream", e);
                }
            }
        }
        document.getDocumentElement().normalize();

        NodeList hostsList = document.getElementsByTagName(HOSTID_FIELD);
        for (int i = 0; i < hostsList.getLength(); i++) {
            res.add(hostsList.item(i).getTextContent());
        }
        return res;
    }

    public static final String TASK_NODE = "task";
    public static final String TYPE_FIELD= "type";
    public static final String PATH_FIELD = "path";
    public static final String DELAY_FIELD = "delay";

    private List<RuleTask> retrieveTasks(Node src) throws
                                                   DAOConnectionException {
        List<RuleTask> res = new ArrayList<RuleTask>();
        NodeList feed = src.getChildNodes();
        for (int i = 0; i < feed.getLength(); i++) {
            Node mainnode = feed.item(i);
            if(mainnode.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) mainnode;
                NodeList tasksList = e.getElementsByTagName(TASK_NODE);
                for (int j = 0; j < tasksList.getLength(); j++) {
                    Node taskNode = tasksList.item(j);
                    if (taskNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element task = (Element) taskNode;
                        String type = task.getElementsByTagName(TYPE_FIELD)
                                .item(0).getTextContent();
                        String path = task.getElementsByTagName(PATH_FIELD)
                                .item(0).getTextContent();
                        int delay = Integer.parseInt(
                                task.getElementsByTagName(DELAY_FIELD)
                                .item(0).getTextContent());
                        res.add(new RuleTask(type, path, delay));
                    }
                }
            }
        }
        return res;
    }
}
