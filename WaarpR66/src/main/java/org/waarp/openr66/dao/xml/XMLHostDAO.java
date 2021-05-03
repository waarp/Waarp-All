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
import org.waarp.common.utility.ParametersChecker;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.HostDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Host;
import org.waarp.openr66.protocol.configuration.Configuration;
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
import static org.waarp.openr66.dao.database.DBHostDAO.*;

public class XMLHostDAO implements HostDAO {

  /**
   * HashTable in case of lack of database
   */
  private static final ConcurrentHashMap<String, Host> dbR66HostAuthHashMap =
      new ConcurrentHashMap<String, Host>();

  private static final String XML_SELECT = "/authent/entry[hostid=$hostid]";
  private static final String XML_GET_ALL = "/authent/entry";

  private final File file;

  public XMLHostDAO() {
    file = new File(Configuration.configuration.getAuthFile());
  }

  @Override
  public void close() {
    // ignore
  }

  @Override
  public void delete(final Host host) {
    dbR66HostAuthHashMap.remove(host.getHostid());
  }

  @Override
  public void deleteAll() {
    dbR66HostAuthHashMap.clear();
  }

  @Override
  public List<Host> getAll() throws DAOConnectionException {
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
      final List<Host> res = new ArrayList<Host>(listNode.getLength());
      for (int i = 0; i < listNode.getLength(); i++) {
        final Node node = listNode.item(i);
        final Host host = getFromNode(node);
        res.add(host);
        dbR66HostAuthHashMap.put(host.getHostid(), host);
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
    if (dbR66HostAuthHashMap.containsKey(hostid)) {
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
      return ParametersChecker.isNotEmpty(xpe.evaluate(document));
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
  public List<Host> find(final List<Filter> fitlers)
      throws DAOConnectionException {
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  @Override
  public void insert(final Host host) {
    dbR66HostAuthHashMap.put(host.getHostid(), host);
  }

  @Override
  public Host select(final String hostid)
      throws DAOConnectionException, DAONoDataException {
    Host host = dbR66HostAuthHashMap.get(hostid);
    if (host != null) {
      return host;
    }
    if (!file.exists()) {
      throw new DAOConnectionException(
          "File " + file.getPath() + " doesn't exist");
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
        host = getFromNode(node);
        dbR66HostAuthHashMap.put(host.getHostid(), host);
        return host;
      }
      throw new DAONoDataException("Host not found");
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
  public void update(final Host host) {
    dbR66HostAuthHashMap.put(host.getHostid(), host);
  }

  private Host getFromNode(final Node parent) {
    final Host res = new Host();

    final NodeList children = parent.getChildNodes();
    for (int j = 0; j < children.getLength(); j++) {
      final Node node = children.item(j);
      if (node.getNodeName().equals(HOSTID_FIELD)) {
        res.setHostid(node.getTextContent());
      } else if (node.getNodeName().equals(ADDRESS_FIELD)) {
        res.setAddress(node.getTextContent());
      } else if (node.getNodeName().equals(PORT_FIELD)) {
        res.setPort(Integer.parseInt(node.getTextContent()));
      } else if (node.getNodeName().equals(IS_SSL_FIELD)) {
        res.setSSL(Boolean.parseBoolean(node.getTextContent()));
      } else if (node.getNodeName().equals(IS_CLIENT_FIELD)) {
        res.setClient(Boolean.parseBoolean(node.getTextContent()));
      } else if (node.getNodeName().equals(IS_PROXIFIED_FIELD)) {
        res.setProxified(Boolean.parseBoolean(node.getTextContent()));
      } else if (node.getNodeName().equals(ADMINROLE_FIELD)) {
        res.setAdmin(Boolean.parseBoolean(node.getTextContent()));
      } else if (node.getNodeName().equals(HOSTKEY_FIELD)) {
        res.setHostkey(node.getTextContent().getBytes());
      }
    }
    return res;
  }
}
