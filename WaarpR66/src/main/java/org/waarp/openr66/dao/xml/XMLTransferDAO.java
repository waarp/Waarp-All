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
import org.waarp.common.guid.LongUuid;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.lru.SynchronizedLruCache;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.pojo.Transfer;
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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.waarp.openr66.dao.DAOFactory.*;
import static org.waarp.openr66.dao.database.DBTransferDAO.*;

public class XMLTransferDAO implements TransferDAO {

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(XMLTransferDAO.class);

  public static final String ROOT_LIST = "taskrunners";
  public static final String ROOT_ELEMENT = "runner";


  private static final String XML_SELECT =
      "//runner[" + ID_FIELD + "='$" + ID_FIELD + "' and " + REQUESTER_FIELD +
      "='$" + REQUESTER_FIELD + "' and " + REQUESTED_FIELD + "='$" +
      REQUESTED_FIELD + "' and " + OWNER_REQUEST_FIELD + "='$" +
      OWNER_REQUEST_FIELD + "']";
  private static final String XML_GET_ALL = "runner";

  /**
   * HashTable in case of lack of database using LRU mode with 20 000 items
   * maximum (< 200 MB?) for 180s
   */
  private static SynchronizedLruCache<Long, Transfer> dbR66TaskHashMap;

  private static boolean noFile = false;

  /**
   * @param newNoFile if True, no file will be created but only on Memory
   *     using LruCache
   */
  public static void setNoFile(final boolean newNoFile) {
    noFile = newNoFile;
  }

  /**
   * Create the LRU cache
   *
   * @param limit limit of number of entries in the cache
   * @param ttl time to leave used
   */
  public static void createLruCache(final int limit, final long ttl) {
    dbR66TaskHashMap = new SynchronizedLruCache<Long, Transfer>(limit, ttl);
  }

  public static String hashStatus() {
    return "DbTaskRunner: [dbR66TaskHashMap: " + dbR66TaskHashMap.size() + "] ";
  }

  /**
   * To enable clear of oldest entries in the cache
   *
   * @return the number of elements removed
   */
  public static int clearCache() {
    return dbR66TaskHashMap.forceClearOldest();
  }

  /**
   * To update the TTL for the cache (to 10xTIMEOUT)
   *
   * @param ttl
   */
  public static void updateLruCacheTimeout(final long ttl) {
    dbR66TaskHashMap.setNewTtl(ttl);
  }

  /**
   * To allow to remove specifically one SpecialId from MemoryHashmap
   *
   * @param specialId
   */
  public static void removeNoDbSpecialId(final long specialId) {
    dbR66TaskHashMap.remove(specialId);
  }

  /**
   * To update the usage TTL of the associated object
   *
   * @param specialId
   */
  public static void updateUsed(final long specialId) {
    dbR66TaskHashMap.updateTtl(specialId);
  }

  public XMLTransferDAO() {
    // Empty
  }

  @Override
  public void close() {
    // ignore
  }

  public static final String XMLEXTENSION = "_singlerunner.xml";

  private File getFile(final String requester, final String requested,
                       final long id) {
    return new File(Configuration.configuration.getBaseDirectory() +
                    Configuration.configuration.getArchivePath() + '/' +
                    requester + '_' + requested + '_' + id + XMLEXTENSION);
  }

  @Override
  public void delete(final Transfer transfer) {
    removeNoDbSpecialId(transfer.getId());
  }

  /**
   * {@link DAOConnectionException}
   */
  @Override
  public void deleteAll() {
    dbR66TaskHashMap.clear();
    final File arch = new File(Configuration.configuration.getBaseDirectory() +
                               Configuration.configuration.getArchivePath());
    if (arch.isDirectory()) {
      FileUtils.forceDeleteRecursiveDir(arch);
    }
  }

  @Override
  public List<Transfer> getAll() throws DAOConnectionException {
    if (noFile) {
      return new ArrayList<Transfer>(dbR66TaskHashMap.values());
    }
    final File arch = new File(Configuration.configuration.getArchivePath());
    final File[] runnerFiles = arch.listFiles();
    final List<Transfer> res = new ArrayList<Transfer>();
    if (runnerFiles != null) {
      for (final File fileNew : runnerFiles) {
        try {
          final DocumentBuilderFactory dbf = getDocumentBuilderFactory();
          final Document document = dbf.newDocumentBuilder().parse(fileNew);
          // Setup XPath query
          final XPath xPath = XPathFactory.newInstance().newXPath();
          final XPathExpression xpe = xPath.compile(XML_GET_ALL);
          final NodeList listNode =
              (NodeList) xpe.evaluate(document, XPathConstants.NODESET);
          // Iterate through all found nodes
          for (int i = 0; i < listNode.getLength(); i++) {
            final Node node = listNode.item(i);
            final Transfer transfer = getFromNode(node);
            res.add(transfer);
            dbR66TaskHashMap.put(transfer.getId(), transfer);
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
    }
    synchronized (dbR66TaskHashMap) {
      for (final Transfer transfer : dbR66TaskHashMap.values()) {
        if (transfer != null && !res.contains(transfer)) {
          res.add(transfer);
        }
      }
    }
    return res;
  }

  @Override
  public boolean exist(final long id, final String requester,
                       final String requested, final String owner) {
    if (dbR66TaskHashMap.contains(id)) {
      return true;
    }
    final File file = getFile(requester, requested, id);
    return file.exists();
  }

  /**
   * {@link DAOConnectionException}
   *
   * @return never
   */
  @Override
  public List<Transfer> find(final List<Filter> fitlers)
      throws DAOConnectionException {
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  /**
   * {@link DAOConnectionException}
   *
   * @return count only if filters is empty or null
   */
  @Override
  public long count(final List<Filter> fitlers) throws DAOConnectionException {
    if (fitlers == null || fitlers.isEmpty()) {
      return dbR66TaskHashMap.size();
    }
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  /**
   * {@link UnsupportedOperationException}
   *
   * @return never
   */
  @Override
  public Transfer select(final String id) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@link UnsupportedOperationException}
   *
   * @return never
   */
  @Override
  public boolean exist(final String id) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@link DAOConnectionException}
   *
   * @return never
   */
  @Override
  public List<Transfer> find(final List<Filter> filters, final int limit)
      throws DAOConnectionException {
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  /**
   * {@link DAOConnectionException}
   *
   * @return never
   */
  @Override
  public List<Transfer> find(final List<Filter> filters, final int limit,
                             final int offset) throws DAOConnectionException {
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  /**
   * {@link DAOConnectionException}
   *
   * @return never
   */
  @Override
  public List<Transfer> find(final List<Filter> filters, final String column,
                             final boolean ascend)
      throws DAOConnectionException {
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  /**
   * {@link DAOConnectionException}
   *
   * @return never
   */
  @Override
  public List<Transfer> find(final List<Filter> filters, final String column,
                             final boolean ascend, final int limit)
      throws DAOConnectionException {
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  /**
   * {@link DAOConnectionException}
   *
   * @return never
   */
  @Override
  public List<Transfer> find(final List<Filter> filters, final String column,
                             final boolean ascend, final int limit,
                             final int offset) throws DAOConnectionException {
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  @Override
  public void insert(final Transfer transfer) throws DAOConnectionException {
    // Set unique Id
    if (transfer.getId() == DbConstantR66.ILLEGALVALUE) {
      transfer.setId(new LongUuid().getLong());
    }
    dbR66TaskHashMap.put(transfer.getId(), transfer);
    if (noFile) {
      return;
    }
    final File file = getFile(transfer.getRequester(), transfer.getRequested(),
                              transfer.getId());
    if (file.exists()) {
      throw new DAOConnectionException(
          "File already exist: " + file.getAbsolutePath());
    }
    try {
      final DocumentBuilderFactory dbf = getDocumentBuilderFactory();
      final Document document = dbf.newDocumentBuilder().newDocument();
      final Element root = document.createElement(ROOT_LIST);
      document.appendChild(root);
      root.appendChild(getNode(document, transfer));
      // Write document in file
      XMLUtils.writeToFile(file, document);
    } catch (final ParserConfigurationException e) {
      throw new DAOConnectionException(e);
    }
  }

  @Override
  public Transfer select(final long id, final String requester,
                         final String requested, final String owner)
      throws DAOConnectionException, DAONoDataException {
    if (dbR66TaskHashMap.contains(id)) {
      final Transfer value = dbR66TaskHashMap.get(id);
      if (value != null) {
        dbR66TaskHashMap.updateTtl(id);
      }
      return value;
    }
    if (noFile) {
      throw new DAONoDataException("Transfer cannot be found");
    }
    final File file = getFile(requester, requested, id);
    if (!file.exists()) {
      throw new DAONoDataException("Transfer cannot be found");
    }
    try {
      final DocumentBuilderFactory dbf = getDocumentBuilderFactory();
      final Document document = dbf.newDocumentBuilder().parse(file);
      // Setup XPath variable
      final SimpleVariableResolver resolver = new SimpleVariableResolver();
      resolver.addVariable(new QName(null, ID_FIELD), id);
      resolver.addVariable(new QName(null, REQUESTER_FIELD), requester);
      resolver.addVariable(new QName(null, REQUESTED_FIELD), requested);
      resolver.addVariable(new QName(null, "owner"), owner);
      // Setup XPath query
      final XPath xPath = XPathFactory.newInstance().newXPath();
      xPath.setXPathVariableResolver(resolver);
      final XPathExpression xpe = xPath.compile(XML_SELECT);
      // Retrieve node and instantiate object
      final Node node = (Node) xpe.evaluate(document, XPathConstants.NODE);
      if (node != null) {
        return getFromNode(node);
      }
      throw new DAONoDataException("Transfer cannot be found");
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
  public void update(final Transfer transfer) throws DAOConnectionException {
    dbR66TaskHashMap.put(transfer.getId(), transfer);
    if (noFile) {
      return;
    }
    final File file = getFile(transfer.getRequester(), transfer.getRequested(),
                              transfer.getId());
    if (!file.exists()) {
      throw new DAOConnectionException("File doesn't exist");
    }
    try {
      final DocumentBuilderFactory dbf = getDocumentBuilderFactory();
      final Document document = dbf.newDocumentBuilder().parse(file);
      final Element root = document.getDocumentElement();
      Node node = null;
      if (root.hasChildNodes()) {
        final NodeList nodeList = root.getChildNodes();
        final int nb = nodeList.getLength();
        for (int i = 0; i < nb; i++) {
          int found = 4;
          node = nodeList.item(0);
          if (node.hasChildNodes()) {
            final NodeList nodeChildList = node.getChildNodes();
            final int nbChild = nodeChildList.getLength();
            for (int j = 0; j < nbChild; j++) {
              final Node child = nodeChildList.item(j);
              if (child.getNodeName().equals(ID_FIELD) && child.getTextContent()
                                                               .equals(
                                                                   Long.toString(
                                                                       transfer
                                                                           .getId()))) {
                found++;
              } else if (child.getNodeName().equals(OWNER_REQUEST_FIELD) &&
                         child.getTextContent()
                              .equals(transfer.getOwnerRequest())) {
                found++;
              } else if (child.getNodeName().equals(REQUESTER_FIELD) &&
                         child.getTextContent()
                              .equals(transfer.getRequester())) {
                found++;
              } else if (child.getNodeName().equals(REQUESTED_FIELD) &&
                         child.getTextContent()
                              .equals(transfer.getRequested())) {
                found++;
              }
              if (found == 0) {
                break;
              }
            }
            if (found == 0) {
              break;
            }
          }
        }
      }
      if (node == null) {
        logger.warn("Entry not found cannot update for {} {} {} {}",
                    transfer.getId(), transfer.getRequester(),
                    transfer.getRequested(), transfer.getOwnerRequest());
        return;
      }
      node.getParentNode().removeChild(node);
      // Insert updated node
      root.appendChild(getNode(document, transfer));
      // Write document in file
      XMLUtils.writeToFile(file, document);
    } catch (final SAXException e) {
      throw new DAOConnectionException(e);
    } catch (final ParserConfigurationException e) {
      throw new DAOConnectionException(e);
    } catch (final IOException e) {
      throw new DAOConnectionException(e);
    }
  }

  private Transfer getFromNode(final Node parent) {
    final Transfer res = new Transfer();
    final NodeList children = parent.getChildNodes();
    for (int j = 0; j < children.getLength(); j++) {
      final Node node = children.item(j);
      if (node.getNodeName().equals(ID_FIELD)) {
        res.setId(Long.parseLong(node.getTextContent()));
      } else if (node.getNodeName().equals(OWNER_REQUEST_FIELD)) {
        res.setOwnerRequest(node.getTextContent());
      } else if (node.getNodeName().equals(REQUESTER_FIELD)) {
        res.setRequester(node.getTextContent());
      } else if (node.getNodeName().equals(REQUESTED_FIELD)) {
        res.setRequested(node.getTextContent());
      } else if (node.getNodeName().equals(ID_RULE_FIELD)) {
        res.setRule(node.getTextContent());
      } else if (node.getNodeName().equals(RETRIEVE_MODE_FIELD)) {
        res.setRetrieveMode(Boolean.parseBoolean(node.getTextContent()));
      } else if (node.getNodeName().equals(TRANSFER_MODE_FIELD)) {
        res.setTransferMode(Integer.parseInt(node.getTextContent()));
      } else if (node.getNodeName().equals(FILENAME_FIELD)) {
        res.setFilename(node.getTextContent());
      } else if (node.getNodeName().equals(ORIGINAL_NAME_FIELD)) {
        res.setOriginalName(node.getTextContent());
      } else if (node.getNodeName().equals(FILE_INFO_FIELD)) {
        res.setFileInfo(node.getTextContent());
      } else if (node.getNodeName().equals(TRANSFER_INFO_FIELD)) {
        res.setTransferInfo(node.getTextContent());
      } else if (node.getNodeName().equals(IS_MOVED_FIELD)) {
        res.setIsMoved(Boolean.parseBoolean(node.getTextContent()));
      } else if (node.getNodeName().equals(BLOCK_SIZE_FIELD)) {
        res.setBlockSize(Integer.parseInt(node.getTextContent()));
      } else if (node.getNodeName().equals(GLOBAL_STEP_FIELD)) {
        res.setGlobalStep(
            Transfer.TASKSTEP.valueOf(Integer.parseInt(node.getTextContent())));
      } else if (node.getNodeName().equals(GLOBAL_LAST_STEP_FIELD)) {
        res.setLastGlobalStep(
            Transfer.TASKSTEP.valueOf(Integer.parseInt(node.getTextContent())));
      } else if (node.getNodeName().equals(STEP_FIELD)) {
        res.setStep(Integer.parseInt(node.getTextContent()));
      } else if (node.getNodeName().equals(RANK_FIELD)) {
        res.setRank(Integer.parseInt(node.getTextContent()));
      } else if (node.getNodeName().equals(STEP_STATUS_FIELD)) {
        res.setStepStatus(ErrorCode.getFromCode(node.getTextContent()));
      } else if (node.getNodeName().equals(INFO_STATUS_FIELD)) {
        res.setInfoStatus(ErrorCode.getFromCode(node.getTextContent()));
      } else if (node.getNodeName().equals(TRANSFER_START_FIELD)) {
        res.setStart(Timestamp.valueOf(node.getTextContent()));
      } else if (node.getNodeName().equals(TRANSFER_STOP_FIELD)) {
        res.setStop(Timestamp.valueOf(node.getTextContent()));
      }
    }
    return res;
  }

  private Node getNode(final Document doc, final Transfer transfer) {
    final Node res = doc.createElement(ROOT_ELEMENT);
    res.appendChild(
        XMLUtils.createNode(doc, ID_FIELD, Long.toString(transfer.getId())));
    res.appendChild(XMLUtils.createNode(doc, OWNER_REQUEST_FIELD,
                                        transfer.getOwnerRequest()));
    res.appendChild(
        XMLUtils.createNode(doc, REQUESTER_FIELD, transfer.getRequester()));
    res.appendChild(
        XMLUtils.createNode(doc, REQUESTED_FIELD, transfer.getRequested()));
    res.appendChild(
        XMLUtils.createNode(doc, ID_RULE_FIELD, transfer.getRule()));
    res.appendChild(XMLUtils.createNode(doc, RETRIEVE_MODE_FIELD, Boolean
        .toString(transfer.getRetrieveMode())));
    res.appendChild(XMLUtils.createNode(doc, TRANSFER_MODE_FIELD, Integer
        .toString(transfer.getTransferMode())));
    res.appendChild(
        XMLUtils.createNode(doc, FILENAME_FIELD, transfer.getRequested()));
    res.appendChild(
        XMLUtils.createNode(doc, ORIGINAL_NAME_FIELD, transfer.getFilename()));
    res.appendChild(
        XMLUtils.createNode(doc, REQUESTED_FIELD, transfer.getOriginalName()));
    res.appendChild(
        XMLUtils.createNode(doc, FILE_INFO_FIELD, transfer.getFileInfo()));
    res.appendChild(XMLUtils.createNode(doc, TRANSFER_INFO_FIELD,
                                        transfer.getTransferInfo()));
    res.appendChild(XMLUtils.createNode(doc, IS_MOVED_FIELD, Boolean
        .toString(transfer.getIsMoved())));
    res.appendChild(XMLUtils.createNode(doc, BLOCK_SIZE_FIELD, Integer
        .toString(transfer.getBlockSize())));
    res.appendChild(XMLUtils.createNode(doc, GLOBAL_STEP_FIELD, Integer
        .toString(transfer.getGlobalStep().ordinal())));
    res.appendChild(XMLUtils.createNode(doc, GLOBAL_LAST_STEP_FIELD, Integer
        .toString(transfer.getLastGlobalStep().ordinal())));
    res.appendChild(XMLUtils.createNode(doc, STEP_FIELD,
                                        Integer.toString(transfer.getStep())));
    res.appendChild(XMLUtils.createNode(doc, RANK_FIELD,
                                        Integer.toString(transfer.getRank())));
    res.appendChild(XMLUtils.createNode(doc, STEP_STATUS_FIELD,
                                        transfer.getStepStatus().getCode()));
    res.appendChild(XMLUtils.createNode(doc, INFO_STATUS_FIELD,
                                        transfer.getInfoStatus().getCode()));
    res.appendChild(XMLUtils.createNode(doc, TRANSFER_START_FIELD,
                                        transfer.getStart().toString()));
    res.appendChild(XMLUtils.createNode(doc, TRANSFER_STOP_FIELD,
                                        transfer.getStop().toString()));
    return res;
  }
}
