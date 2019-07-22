package org.waarp.openr66.dao.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.dao.BusinessDAO;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Business;
import org.waarp.openr66.pojo.Host;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

//TODO
public class XMLBusinessDAO implements BusinessDAO {

    private static final WaarpLogger logger = WaarpLoggerFactory.getLogger(XMLBusinessDAO.class);

    /**
     * HashTable in case of lack of database
     */
    private static final ConcurrentHashMap<String, Business>
        dbR66BusinessHashMap =
        new ConcurrentHashMap<String, Business>();

    public static final String HOSTID_FIELD = "hostid";

    private static final String XML_SELECT = "/authent/entry[hostid=$hostid]";
    private static final String XML_GET_ALL= "/authent/entry";

    private File file;

    public XMLBusinessDAO(String filePath) throws DAOConnectionException {
        this.file = new File(filePath);
    }

    public void close() {}

    public void delete(Business business) throws DAOConnectionException {
        dbR66BusinessHashMap.remove(business.getHostid());
    }

    public void deleteAll() throws DAOConnectionException {
        dbR66BusinessHashMap.clear();
    }

    public List<Business> getAll() throws DAOConnectionException {
        if (!file.exists()) {
            throw new DAOConnectionException("File doesn't exist");
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document document = dbf.newDocumentBuilder().parse(file);
            // Setup XPath query
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression xpe = xPath.compile(XML_GET_ALL);
            NodeList listNode = (NodeList) xpe.evaluate(document,
                    XPathConstants.NODESET);
            // Iterate through all found nodes
            List<Business> res = new ArrayList<Business>(listNode.getLength());
            for (int i = 0; i < listNode.getLength(); i++) {
                Node node = listNode.item(i);
                Business business = getFromNode(node);
                res.add(business);
                dbR66BusinessHashMap.put(business.getHostid(), business);
            }
            return res;
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

    public boolean exist(String hostid) throws DAOConnectionException {
        if (dbR66BusinessHashMap.containsKey(hostid)) {
            return true;
        }
        if (!file.exists()) {
            throw new DAOConnectionException("File doesn't exist");
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document document = dbf.newDocumentBuilder().parse(file);
            // Setup XPath variable
            SimpleVariableResolver resolver = new SimpleVariableResolver();
            resolver.addVariable(new QName(null, "hostid"), hostid);
            // Setup XPath query
            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setXPathVariableResolver(resolver);
            XPathExpression xpe = xPath.compile(XML_SELECT);
            // Query will return "" if nothing is found
            return(!"".equals(xpe.evaluate(document)));
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

    public List<Business> find(List<Filter> fitlers) throws
                                                     DAOConnectionException {
        throw new DAOConnectionException("Operation not supported on XML DAO");
    }

    public void insert(Business business) throws DAOConnectionException {
        dbR66BusinessHashMap.put(business.getHostid(), business);
    }

    public Business select(String hostid)
        throws DAOConnectionException, DAONoDataException {
        Business business = dbR66BusinessHashMap.get(hostid);
        if (business != null) {
            return business;
        }
        if (!file.exists()) {
            throw new DAOConnectionException("File doesn't exist");
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document document = dbf.newDocumentBuilder().parse(file);
            // Setup XPath variable
            SimpleVariableResolver resolver = new SimpleVariableResolver();
            resolver.addVariable(new QName(null, "hostid"), hostid);
            // Setup XPath query
            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setXPathVariableResolver(resolver);
            XPathExpression xpe = xPath.compile(XML_SELECT);
            // Retrieve node and instantiate object
            Node node = (Node) xpe.evaluate(document, XPathConstants.NODE);
            if (node != null) {
                business = getFromNode(node);
                dbR66BusinessHashMap.put(business.getHostid(), business);
                return business;
            }
            throw new DAONoDataException("Business not found");
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

    public void update(Business business) throws DAOConnectionException {
        dbR66BusinessHashMap.put(business.getHostid(), business);
    }

    private Business getFromNode(Node parent) {
        Business res = new Business();

        NodeList children = parent.getChildNodes();
        for (int j = 0; j < children.getLength(); j++) {
            Node node = children.item(j);
            if (node.getNodeName().equals(HOSTID_FIELD)) {
                res.setHostid(node.getTextContent());
            }
        }
        return res;
    }

    private Node getNode(Document doc, Business business) {
        Node res = doc.createElement("entry");
        return res;
    }
}
