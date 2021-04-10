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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.configuration.ExtensionFilter;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.RuleDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Rule;
import org.waarp.openr66.pojo.RuleTask;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.waarp.openr66.dao.DAOFactory.*;

public class XMLRuleDAO implements RuleDAO {

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(XMLRuleDAO.class);

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

  private static final String XML_GET_ALL = "//rule";
  private static final File[] FILE_0_LENGTH = new File[0];

  public XMLRuleDAO(final String filePath) {
    // Nothing to do
  }

  @Override
  public void close() {
    // ignore
  }

  public static final String EXT_RULE = ".rule.xml";
  public static final String EXT_RULES = ".rules.xml";

  private File[] getRuleFiles() {
    final File ruleDir = new File(
        Configuration.configuration.getBaseDirectory() +
        Configuration.configuration.getConfigPath());
    final List<File> res = new ArrayList<File>();
    if (ruleDir.isDirectory()) {
      res.addAll(
          Arrays.asList(ruleDir.listFiles(new ExtensionFilter(EXT_RULE))));
      res.addAll(
          Arrays.asList(ruleDir.listFiles(new ExtensionFilter(EXT_RULES))));
    }
    return res.toArray(FILE_0_LENGTH);
  }

  @Override
  public void delete(final Rule rule) {
    dbR66RuleHashMap.remove(rule.getName());
  }

  @Override
  public void deleteAll() {
    dbR66RuleHashMap.clear();
  }

  @Override
  public List<Rule> getAll() throws DAOConnectionException {
    final List<Rule> res = new ArrayList<Rule>();

    final File[] files = getRuleFiles();
    for (final File ruleFile : files) {
      try {
        final DocumentBuilderFactory dbf = getDocumentBuilderFactory();
        final Document document = dbf.newDocumentBuilder().parse(ruleFile);
        // Setup XPath query
        final XPath xPath = XPathFactory.newInstance().newXPath();
        final XPathExpression xpe = xPath.compile(XML_GET_ALL);
        final NodeList listNode =
            (NodeList) xpe.evaluate(document, XPathConstants.NODESET);
        // Iterate through all found nodes

        for (int i = 0; i < listNode.getLength(); i++) {
          final Node node = listNode.item(i);
          final Rule rule = getFromNode(node);
          res.add(rule);
          dbR66RuleHashMap.put(rule.getName(), rule);
        }
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
    return res;
  }

  @Override
  public boolean exist(final String rulename) {
    return dbR66RuleHashMap.containsKey(rulename);
  }

  @Override
  public List<Rule> find(final List<Filter> fitlers)
      throws DAOConnectionException {
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  @Override
  public void insert(final Rule rule) {
    dbR66RuleHashMap.put(rule.getName(), rule);
  }

  @Override
  public Rule select(final String rulename)
      throws DAOConnectionException, DAONoDataException {
    if (exist(rulename)) {
      return dbR66RuleHashMap.get(rulename);
    }
    throw new DAONoDataException("Rule cannot be found");
  }

  @Override
  public void update(final Rule rule) {
    dbR66RuleHashMap.put(rule.getName(), rule);
  }

  private Rule getFromNode(final Node parent) throws DAOConnectionException {
    final Rule res = new Rule();

    final NodeList children = parent.getChildNodes();
    for (int j = 0; j < children.getLength(); j++) {
      final Node node = children.item(j);
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

  public static final String HOSTID_FIELD = "hostid";

  private List<String> retrieveHostids(final String xml)
      throws DAOConnectionException {
    final ArrayList<String> res = new ArrayList<String>();
    if (xml == null || xml.isEmpty()) {
      return res;
    }
    Document document;
    InputStream stream = null;
    try {
      stream = new ByteArrayInputStream(xml.getBytes(WaarpStringUtils.UTF8));
      final DocumentBuilderFactory dbf = getDocumentBuilderFactory();
      document = dbf.newDocumentBuilder().parse(stream);
    } catch (final IOException e) {
      throw new DAOConnectionException(e);
    } catch (final ParserConfigurationException e) {
      throw new DAOConnectionException(e);
    } catch (final SAXException e) {
      throw new DAOConnectionException(e);
    } finally {
      if (stream != null) {
        FileUtils.close(stream);
      }
    }
    document.getDocumentElement().normalize();

    final NodeList hostsList = document.getElementsByTagName(HOSTID_FIELD);
    for (int i = 0; i < hostsList.getLength(); i++) {
      res.add(hostsList.item(i).getTextContent());
    }
    return res;
  }

  public static final String TASK_NODE = "task";
  public static final String TYPE_FIELD = "type";
  public static final String PATH_FIELD = "path";
  public static final String DELAY_FIELD = "delay";

  private List<RuleTask> retrieveTasks(final Node src) {
    final List<RuleTask> res = new ArrayList<RuleTask>();
    final NodeList feed = src.getChildNodes();
    for (int i = 0; i < feed.getLength(); i++) {
      final Node mainnode = feed.item(i);
      if (mainnode.getNodeType() == Node.ELEMENT_NODE) {
        final Element e = (Element) mainnode;
        final NodeList tasksList = e.getElementsByTagName(TASK_NODE);
        for (int j = 0; j < tasksList.getLength(); j++) {
          final Node taskNode = tasksList.item(j);
          if (taskNode.getNodeType() == Node.ELEMENT_NODE) {
            final Element task = (Element) taskNode;
            NodeList nodeList = task.getElementsByTagName(TYPE_FIELD);
            if (nodeList == null || nodeList.getLength() == 0) {
              logger.error("Field not found in Rule: " + TYPE_FIELD);
              continue;
            }
            final String type = nodeList.item(0).getTextContent();
            nodeList = task.getElementsByTagName(PATH_FIELD);
            final String path;
            if (nodeList != null && nodeList.getLength() > 0) {
              path = nodeList.item(0).getTextContent();
            } else {
              path = "";
            }
            nodeList = task.getElementsByTagName(DELAY_FIELD);
            final int delay;
            if (nodeList != null && nodeList.getLength() > 0) {
              int tmpDelay = 0;
              try {
                tmpDelay = Integer.parseInt(nodeList.item(0).getTextContent());
              } catch (NumberFormatException ignored) {
                // ignore
              }
              delay = tmpDelay;
            } else {
              delay = 0;
            }
            res.add(new RuleTask(type, path, delay));
          }
        }
      }
    }
    return res;
  }
}
