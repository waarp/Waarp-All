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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.LimitDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Limit;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.waarp.openr66.dao.DAOFactory.*;

//TODO
public class XMLLimitDAO implements LimitDAO {

  /**
   * HashTable in case of lack of database
   */
  private static final ConcurrentHashMap<String, Limit>
      dbR66ConfigurationHashMap = new ConcurrentHashMap<String, Limit>();

  public static final String HOSTID_FIELD = "hostid";
  public static final String SESSION_LIMIT_FILED = "sessionlimit";
  public static final String GOLBAL_LIMIT_FILED = "globallimit";
  public static final String DELAY_LIMIT_FILED = "delaylimit";
  public static final String RUN_LIMIT_FILED = "runlimit";
  public static final String DELAY_COMMAND_LIMIT_FILED = "delaycommand";
  public static final String DELAY_RETRY_LIMIT_FILED = "delayretry";

  private static final String XML_SELECT = "/config/identity[hostid=$hostid]";

  private final File file;

  public XMLLimitDAO(String filePath) throws DAOConnectionException {
    file = new File(filePath);
  }

  @Override
  public void close() {
    // ignore
  }

  @Override
  public void delete(Limit limit) throws DAOConnectionException {
    dbR66ConfigurationHashMap.remove(limit.getHostid());
  }

  @Override
  public void deleteAll() throws DAOConnectionException {
    dbR66ConfigurationHashMap.clear();
  }

  @Override
  public List<Limit> getAll() throws DAOConnectionException {
    if (!file.exists()) {
      throw new DAOConnectionException("File doesn't exist");
    }
    try {
      final DocumentBuilderFactory dbf = getDocumentBuilderFactory();
      final Document document = dbf.newDocumentBuilder().parse(file);
      // File contains only 1 entry
      final List<Limit> res = new ArrayList<Limit>(1);
      final Limit limit = getFromNode(document.getDocumentElement());
      res.add(limit);
      dbR66ConfigurationHashMap.put(limit.getHostid(), limit);
      // Iterate through all found nodes
      return res;
    } catch (final SAXException e) {
      throw new DAOConnectionException(e);
    } catch (final ParserConfigurationException e) {
      throw new DAOConnectionException(e);
    } catch (final IOException e) {
      throw new DAOConnectionException(e);
    }
  }

  @Override
  public boolean exist(String hostid) throws DAOConnectionException {
    if (dbR66ConfigurationHashMap.containsKey(hostid)) {
      return true;
    }
    if (!file.exists()) {
      throw new DAOConnectionException("File doesn't exist");
    }
    try {
      final DocumentBuilderFactory dbf = getDocumentBuilderFactory();
      final Document document = dbf.newDocumentBuilder().parse(file);
      // Setup XPath variable
      final SimpleVariableResolver resolver = new SimpleVariableResolver();
      resolver.addVariable(new QName(null, HOSTID_FIELD), hostid);
      // Setup XPath query
      final XPath xPath = XPathFactory.newInstance().newXPath();
      xPath.setXPathVariableResolver(resolver);
      final XPathExpression xpe = xPath.compile(XML_SELECT);
      // Query will return "" if nothing is found
      return xpe.evaluate(document) != null &&
             !xpe.evaluate(document).isEmpty();
    } catch (final SAXException e) {
      throw new DAOConnectionException(e);
    } catch (final XPathExpressionException e) {
      throw new DAOConnectionException(e);
    } catch (final ParserConfigurationException e) {
      throw new DAOConnectionException(e);
    } catch (final IOException e) {
      throw new DAOConnectionException(e);
    }
  }

  @Override
  public List<Limit> find(List<Filter> fitlers) throws DAOConnectionException {
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  @Override
  public void insert(Limit limit) throws DAOConnectionException {
    dbR66ConfigurationHashMap.put(limit.getHostid(), limit);
  }

  @Override
  public Limit select(String hostid)
      throws DAOConnectionException, DAONoDataException {
    Limit limit = dbR66ConfigurationHashMap.get(hostid);
    if (limit != null) {
      return limit;
    }
    if (!file.exists()) {
      throw new DAOConnectionException("File doesn't exist");
    }
    try {
      final DocumentBuilderFactory dbf = getDocumentBuilderFactory();
      final Document document = dbf.newDocumentBuilder().parse(file);
      // Setup XPath variable
      final SimpleVariableResolver resolver = new SimpleVariableResolver();
      resolver.addVariable(new QName(null, HOSTID_FIELD), hostid);
      // Setup XPath query
      final XPath xPath = XPathFactory.newInstance().newXPath();
      xPath.setXPathVariableResolver(resolver);
      final XPathExpression xpe = xPath.compile(XML_SELECT);
      // Retrieve node and instantiate object
      final Node node = (Node) xpe.evaluate(document, XPathConstants.NODE);
      if (node != null) {
        limit = getFromNode(document.getDocumentElement());
        dbR66ConfigurationHashMap.put(limit.getHostid(), limit);
        return limit;
      }
      throw new DAONoDataException("Limit not found");
    } catch (final SAXException e) {
      throw new DAOConnectionException(e);
    } catch (final XPathExpressionException e) {
      throw new DAOConnectionException(e);
    } catch (final ParserConfigurationException e) {
      throw new DAOConnectionException(e);
    } catch (final IOException e) {
      throw new DAOConnectionException(e);
    }
  }

  @Override
  public void update(Limit limit) throws DAOConnectionException {
    dbR66ConfigurationHashMap.put(limit.getHostid(), limit);
  }

  private Limit getFromNode(Node parent) {
    final Limit res = new Limit();

    final NodeList children = parent.getChildNodes();
    for (int j = 0; j < children.getLength(); j++) {
      final Node node = children.item(j);
      if (node.getNodeName().equals(HOSTID_FIELD)) {
        res.setHostid(node.getTextContent());
      }
    }
    return res;
  }
}
