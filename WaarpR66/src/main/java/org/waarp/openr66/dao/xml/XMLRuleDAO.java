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
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.xml.XmlUtil;
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.waarp.openr66.dao.DAOFactory.*;
import static org.waarp.openr66.dao.database.DBRuleDAO.*;

public class XMLRuleDAO implements RuleDAO {

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(XMLRuleDAO.class);

  /**
   * HashTable in case of lack of database
   */
  private static final ConcurrentHashMap<String, Rule> dbR66RuleHashMap =
      new ConcurrentHashMap<String, Rule>();

  public static final String ROOT_LIST = "rules";

  private static final String XML_GET_ALL = "//rule";
  private static final File[] FILE_0_LENGTH = new File[0];

  public XMLRuleDAO() {
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
      logger.debug("Load file {}", ruleFile.getAbsolutePath());
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
      final String content = XmlUtil.getExtraTrimed(node.getTextContent());
      if (node.getNodeName().equals(ID_FIELD)) {
        res.setName(content);
      } else if (node.getNodeName().equals(HOSTIDS_FIELD)) {
        res.setHostids(retrieveHostids(node));
      } else if (node.getNodeName().equals(MODE_TRANS_FIELD)) {
        res.setMode(Integer.parseInt(content));
      } else if (node.getNodeName().equals(SEND_PATH_FIELD)) {
        res.setSendPath(content);
      } else if (node.getNodeName().equals(RECV_PATH_FIELD)) {
        res.setRecvPath(content);
      } else if (node.getNodeName().equals(ARCHIVE_PATH_FIELD)) {
        res.setArchivePath(content);
      } else if (node.getNodeName().equals(WORK_PATH_FIELD)) {
        res.setWorkPath(content);
      } else if (node.getNodeName().equals(R_PRE_TASKS_FIELD)) {
        res.setRPreTasks(retrieveTasks(node));
      } else if (node.getNodeName().equals(R_POST_TASKS_FIELD)) {
        res.setRPostTasks(retrieveTasks(node));
      } else if (node.getNodeName().equals(R_ERROR_TASKS_FIELD)) {
        res.setRErrorTasks(retrieveTasks(node));
      } else if (node.getNodeName().equals(S_PRE_TASKS_FIELD)) {
        res.setSPreTasks(retrieveTasks(node));
      } else if (node.getNodeName().equals(S_POST_TASKS_FIELD)) {
        res.setSPostTasks(retrieveTasks(node));
      } else if (node.getNodeName().equals(S_ERROR_TASKS_FIELD)) {
        res.setSErrorTasks(retrieveTasks(node));
      }
    }
    return res;
  }

  private List<String> retrieveHostids(final Node xml)
      throws DAOConnectionException {
    final ArrayList<String> res = new ArrayList<String>();
    if (xml == null || !xml.hasChildNodes()) {
      return res;
    }
    final NodeList hostsList = xml.getChildNodes();
    for (int i = 0; i < hostsList.getLength(); i++) {
      res.add(XmlUtil.getExtraTrimed(hostsList.item(i).getTextContent()));
    }
    return res;
  }

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
            final String type =
                XmlUtil.getExtraTrimed(nodeList.item(0).getTextContent());
            nodeList = task.getElementsByTagName(PATH_FIELD);
            final String path;
            if (nodeList != null && nodeList.getLength() > 0) {
              path = XmlUtil.getExtraTrimed(nodeList.item(0).getTextContent());
            } else {
              path = "";
            }
            nodeList = task.getElementsByTagName(DELAY_FIELD);
            final int delay;
            if (nodeList != null && nodeList.getLength() > 0) {
              int tmpDelay = 0;
              try {
                tmpDelay = Integer.parseInt(
                    XmlUtil.getExtraTrimed(nodeList.item(0).getTextContent()));
              } catch (NumberFormatException ignored) {
                // ignore
              }
              delay = tmpDelay;
            } else {
              delay = 0;
            }
            RuleTask ruleTask = new RuleTask(type, path, delay);
            logger.debug("Task {}", ruleTask);
            res.add(ruleTask);
          }
        }
      }
    }
    return res;
  }
}
