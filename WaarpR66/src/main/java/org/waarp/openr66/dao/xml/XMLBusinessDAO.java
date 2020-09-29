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
import org.waarp.openr66.dao.BusinessDAO;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Business;
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
public class XMLBusinessDAO implements BusinessDAO {

  /**
   * HashTable in case of lack of database
   */
  private static final ConcurrentHashMap<String, Business>
      dbR66BusinessHashMap = new ConcurrentHashMap<String, Business>();

  public static final String HOSTID_FIELD = "hostid";

  private static final String XML_SELECT = "/authent/entry[hostid=$hostid]";
  private static final String XML_GET_ALL = "/authent/entry";

  private final File file;

  public XMLBusinessDAO(final String filePath) {
    file = new File(filePath);
  }

  @Override
  public void close() {
    // ignore
  }

  @Override
  public void delete(final Business business) {
    dbR66BusinessHashMap.remove(business.getHostid());
  }

  @Override
  public void deleteAll() {
    dbR66BusinessHashMap.clear();
  }

  @Override
  public List<Business> getAll() throws DAOConnectionException {
    if (!file.exists()) {
      throw new DAOConnectionException("File doesn't exist");
    }
    try {
      final DocumentBuilderFactory dbf = getDocumentBuilderFactory();
      final Document document = dbf.newDocumentBuilder().parse(file);
      // Setup XPath query
      final XPath xPath = XPathFactory.newInstance().newXPath();
      final XPathExpression xpe = xPath.compile(XML_GET_ALL);
      final NodeList listNode =
          (NodeList) xpe.evaluate(document, XPathConstants.NODESET);
      // Iterate through all found nodes
      final List<Business> res = new ArrayList<Business>(listNode.getLength());
      for (int i = 0; i < listNode.getLength(); i++) {
        final Node node = listNode.item(i);
        final Business business = getFromNode(node);
        res.add(business);
        dbR66BusinessHashMap.put(business.getHostid(), business);
      }
      return res;
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
  public boolean exist(final String hostid) throws DAOConnectionException {
    if (dbR66BusinessHashMap.containsKey(hostid)) {
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
  public List<Business> find(final List<Filter> fitlers)
      throws DAOConnectionException {
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  @Override
  public void insert(final Business business) {
    dbR66BusinessHashMap.put(business.getHostid(), business);
  }

  @Override
  public Business select(final String hostid)
      throws DAOConnectionException, DAONoDataException {
    Business business = dbR66BusinessHashMap.get(hostid);
    if (business != null) {
      return business;
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
        business = getFromNode(node);
        dbR66BusinessHashMap.put(business.getHostid(), business);
        return business;
      }
      throw new DAONoDataException("Business not found");
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
  public void update(final Business business) {
    dbR66BusinessHashMap.put(business.getHostid(), business);
  }

  private Business getFromNode(final Node parent) {
    final Business res = new Business();

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
