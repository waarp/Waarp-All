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
package org.waarp.openr66.context.task;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.filemonitor.FileMonitor.FileItem;
import org.waarp.common.filemonitor.FileMonitor.FileMonitorInformation;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.client.SpooledDirectoryTransfer;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.packet.BusinessRequestPacket;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TreeMap;

import static org.waarp.common.database.DbConstant.*;

/**
 * Java Task for SpooledDirectory information to the Waarp Server
 */
public class SpooledInformTask extends AbstractExecJavaTask {

  private static final String TD_TD = "</TD><TD>";

  private static final String TH_TH = "</TH><TH>";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(SpooledInformTask.class);

  static final TreeMap<String, SpooledInformation> spooledInformationMap =
      new TreeMap<String, SpooledInformation>();

  public static class SpooledInformation {
    public final String host;
    public FileMonitorInformation fileMonitorInformation;
    public Date lastUpdate = new Date();

    /**
     * @param host
     * @param fileMonitorInformation
     */
    private SpooledInformation(final String host,
                               final FileMonitorInformation fileMonitorInformation) {
      this.host = host;
      this.fileMonitorInformation = fileMonitorInformation;
    }
  }

  @Override
  public void run() {
    if (callFromBusiness) {
      // Business Request to validate?
      String validated = SpooledDirectoryTransfer.PARTIALOK;
      if (isToValidate) {
        try {
          final FileMonitorInformation fileMonitorInformation =
              JsonHandler.mapper
                  .readValue(fullarg, FileMonitorInformation.class);
          logger.info(
              "Receive SpooledInform of size: " + fullarg.length() + " (" +
              fileMonitorInformation.fileItems.size() + ", " +
              (fileMonitorInformation.removedFileItems != null?
                  fileMonitorInformation.removedFileItems.size() : -1) + ')');
          final String host = session.getAuth().getUser();
          synchronized (spooledInformationMap) {
            if (fileMonitorInformation.removedFileItems == null ||
                fileMonitorInformation.removedFileItems.isEmpty()) {
              final SpooledInformation old = spooledInformationMap
                  .put(fileMonitorInformation.name,
                       new SpooledInformation(host, fileMonitorInformation));
              if (old != null && old.fileMonitorInformation != null) {
                if (old.fileMonitorInformation.directories != null) {
                  old.fileMonitorInformation.directories.clear();
                }
                if (old.fileMonitorInformation.fileItems != null) {
                  old.fileMonitorInformation.fileItems.clear();
                }
                old.fileMonitorInformation = null;
              }
            } else {
              // partial update
              final SpooledInformation update =
                  spooledInformationMap.get(fileMonitorInformation.name);
              if (update == null) {
                // Issue since update is not existing so full update is needed next time
                spooledInformationMap.put(fileMonitorInformation.name,
                                          new SpooledInformation(host,
                                                                 fileMonitorInformation));
                validated = SpooledDirectoryTransfer.NEEDFULL;
              } else {
                for (final String item : fileMonitorInformation.removedFileItems) {
                  update.fileMonitorInformation.fileItems.remove(item);
                }
                update.fileMonitorInformation.fileItems
                    .putAll(fileMonitorInformation.fileItems);
                update.lastUpdate = new Date();
              }
            }
          }
        } catch (final JsonParseException e1) {
          logger.warn("Cannot parse SpooledInformation: " + fullarg + ' ' +
                      e1.getMessage());
        } catch (final JsonMappingException e1) {
          logger.warn("Cannot parse SpooledInformation: " + fullarg + ' ' +
                      e1.getMessage());
        } catch (final IOException e1) {
          logger.warn("Cannot parse SpooledInformation: " + e1.getMessage());
        }
        final BusinessRequestPacket packet =
            new BusinessRequestPacket(getClass().getName() + " informed", 0);
        if (!validate(packet)) {
          // No Validation therefore send the new packet back
          try {
            ChannelUtils
                .writeAbstractLocalPacket(session.getLocalChannelReference(),
                                          packet, true);
          } catch (final OpenR66ProtocolPacketException ignored) {
            // nothing
          }
        }
        status = 0;
      }
      finalValidate(validated);
    } else {
      // unallowed
      logger.warn("SpooledInformTask not allowed as Java Task: " + fullarg);
      invalid();
    }
  }

  /**
   * @param detailed
   * @param status 1 for ok, -1 for ko, 0 for all
   * @param uri
   *
   * @return the StringBuilder containing the HTML format as a Table of the
   *     current Spooled information
   */
  public static StringBuilder buildSpooledTable(final boolean detailed,
                                                final int status,
                                                final String uri) {
    final StringBuilder builder = beginSpooledTable(detailed, uri);
    // get current information
    synchronized (spooledInformationMap) {
      final Set<String> names = spooledInformationMap.keySet();
      for (final String name : names) {
        // per Name
        buildSpooledTableElement(detailed, status, builder, name);
      }
    }
    endSpooledTable(builder);
    return builder;
  }

  /**
   * @param name
   * @param uri
   *
   * @return the StringBuilder containing the HTML format as a Table of the
   *     current Spooled information
   */
  public static StringBuilder buildSpooledUniqueTable(final String uri,
                                                      final String name) {
    final StringBuilder builder = beginSpooledTable(false, uri);
    // get current information
    synchronized (spooledInformationMap) {
      // per Name
      final SpooledInformation inform =
          buildSpooledTableElement(false, 0, builder, name);
      endSpooledTable(builder);
      builder.append("<BR>");
      if (inform != null) {
        buildSpooledTableFiles(builder, inform);
      }
    }
    return builder;
  }

  /**
   * @param builder
   */
  private static void endSpooledTable(final StringBuilder builder) {
    builder.append("</TBODY></TABLE></small>");
  }

  /**
   * @param detailed
   * @param uri
   *
   * @return the associated StringBuilder as temporary result
   */
  private static StringBuilder beginSpooledTable(final boolean detailed,
                                                 final String uri) {
    final StringBuilder builder = new StringBuilder();
    builder.append(
        "<small><TABLE class='table table-condensed table-bordered' BORDER=1><CAPTION><A HREF=");
    builder.append(uri);
    if (detailed) {
      builder.append(
          Messages.getString("SpooledInformTask.TitleDetailed")); //$NON-NLS-1$
    } else {
      builder.append(
          Messages.getString("SpooledInformTask.TitleNormal")); //$NON-NLS-1$
    }
    // title first
    builder.append("<THEAD><TR><TH>")
           .append(Messages.getString("SpooledInformTask.0")) //$NON-NLS-1$
           .append(TH_TH)
           .append(Messages.getString("SpooledInformTask.1")) //$NON-NLS-1$
           .append(TH_TH)
           .append(Messages.getString("SpooledInformTask.2")) //$NON-NLS-1$
           .append(TH_TH)
           .append(Messages.getString("SpooledInformTask.3")) //$NON-NLS-1$
           .append(TH_TH)
           .append(Messages.getString("SpooledInformTask.4")) //$NON-NLS-1$
           .append(TH_TH)
           .append(Messages.getString("SpooledInformTask.5")) //$NON-NLS-1$
           .append(TH_TH)
           .append(Messages.getString("SpooledInformTask.6")) //$NON-NLS-1$
           .append(TH_TH)
           .append(Messages.getString("SpooledInformTask.7")) //$NON-NLS-1$
           .append(TH_TH)
           .append(Messages.getString("SpooledInformTask.8")) //$NON-NLS-1$
           .append(TH_TH)
           .append(Messages.getString("SpooledInformTask.9")) //$NON-NLS-1$
           .append("</TH></TR></THEAD><TBODY>");
    return builder;
  }

  /**
   * @param detailed
   * @param status
   * @param builder
   * @param name
   */
  private static SpooledInformation buildSpooledTableElement(
      final boolean detailed, final int status, final StringBuilder builder,
      final String name) {
    final SpooledInformation inform = spooledInformationMap.get(name);
    if (inform == null) {
      return null;
    }
    final long time = inform.lastUpdate.getTime() +
                      Configuration.configuration.getTimeoutCon();
    final long curtime = System.currentTimeMillis();
    if (time + Configuration.configuration.getTimeoutCon() < curtime) {
      if (status > 0) {
        return inform;
      }
    } else {
      if (status < 0) {
        return inform;
      }
    }
    builder.append("<TR><TH>").append(name.replace(',', ' '))
           .append("</TH><TD>").append(inform.host).append("</TD>");
    if (time + Configuration.configuration.getTimeoutCon() < curtime) {
      builder.append("<TD bgcolor=Red>");
    } else if (time < curtime) {
      builder.append("<TD bgcolor=Orange>");
    } else {
      builder.append("<TD bgcolor=LightGreen>");
    }
    final DateFormat dateFormat =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
    builder.append(dateFormat.format(inform.lastUpdate)).append("</TD>");
    if (inform.fileMonitorInformation != null) {
      builder
          .append(Messages.getString("SpooledInformTask.AllOk")) //$NON-NLS-1$
          .append(inform.fileMonitorInformation.globalok).append(
          Messages.getString("SpooledInformTask.AllError")) //$NON-NLS-1$
          .append(inform.fileMonitorInformation.globalerror)
          .append(Messages.getString("SpooledInformTask.TodayOk")) //$NON-NLS-1$
          .append(inform.fileMonitorInformation.todayok).append(
          Messages.getString("SpooledInformTask.TodayError")) //$NON-NLS-1$
          .append(inform.fileMonitorInformation.todayerror).append(TD_TD)
          .append(inform.fileMonitorInformation.elapseTime).append(TD_TD)
          .append(inform.fileMonitorInformation.stopFile).append(TD_TD)
          .append(inform.fileMonitorInformation.statusFile).append(TD_TD)
          .append(inform.fileMonitorInformation.scanSubDir).append("</TD>");
      final StringBuilder dirs =
          new StringBuilder("<ul class='list-unstyled'>");
      for (final File dir : inform.fileMonitorInformation.directories) {
        dirs.append("<li>").append(dir).append("</li>");
      }
      dirs.append("</ul>");
      builder.append("<TD>").append(dirs).append(TD_TD);
      if (detailed && inform.fileMonitorInformation.fileItems != null) {
        buildSpooledTableFiles(builder, inform);
      } else {
        // simply print number of files
        if (inform.fileMonitorInformation.fileItems != null) {
          builder.append(inform.fileMonitorInformation.fileItems.size());
        } else {
          builder.append(0);
        }
        // Form GET to ensure encoding
        builder.append(
            "<FORM class='form-inline' name='DETAIL' method='GET' action='/SpooledDetailed.html'><input type=hidden name='name' value='")
               .append(name).append(
            "'/><INPUT type='submit' class='btn btn-info btn-sm' value='DETAIL'/></FORM>");
      }
    }
    builder.append("</TD></TR>");
    return inform;
  }

  /**
   * @param builder
   * @param inform
   */
  private static void buildSpooledTableFiles(final StringBuilder builder,
                                             final SpooledInformation inform) {
    builder.append(
        "<small><TABLE class='table table-condensed table-bordered' BORDER=1><THEAD><TR><TH>") //$NON-NLS-1$
           .append(Messages.getString("SpooledInformTask.10")).append(TH_TH)
           .append(Messages.getString("SpooledInformTask.11")) //$NON-NLS-2$
           .append(TH_TH)
           .append(Messages.getString("SpooledInformTask.12")) //$NON-NLS-1$
           .append(TH_TH)
           .append(Messages.getString("SpooledInformTask.13")) //$NON-NLS-1$
           .append(TH_TH)
           .append(Messages.getString("SpooledInformTask.14")) //$NON-NLS-1$
           .append(TH_TH)
           .append(Messages.getString("SpooledInformTask.15")) //$NON-NLS-1$
           .append("</TH></TR></THEAD><TBODY>");
    final DateFormat dateFormat =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
    for (final FileItem fileItem : inform.fileMonitorInformation.fileItems
        .values()) {
      builder.append("<TR><TD>").append(fileItem.file).append(TD_TD);
      if (fileItem.hash != null) {
        builder.append(FilesystemBasedDigest.getHex(fileItem.hash));
      }
      builder.append(TD_TD);
      if (fileItem.lastTime > 0) {
        builder.append(dateFormat.format(new Date(fileItem.lastTime)));
      }
      builder.append(TD_TD);
      if (fileItem.timeUsed > 0) {
        builder.append(dateFormat.format(new Date(fileItem.timeUsed)));
      }
      builder.append(TD_TD).append(fileItem.used).append(TD_TD)
             .append(fileItem.specialId).append("</TD></TR>");
    }
    builder.append("</TBODY></TABLE></small>");
  }

  /**
   * @param detailed
   * @param status 1 for ok, -1 for ko, 0 for all
   * @param uri
   *
   * @return the String containing the JSON format of the current Spooled
   *     information
   */
  public static String buildSpooledJson(final boolean detailed,
                                        final int status, final String uri) {
    final ArrayNode array = JsonHandler.createArrayNode();
    // get current information
    synchronized (spooledInformationMap) {
      final Set<String> names = spooledInformationMap.keySet();
      for (final String name : names) {
        // per Name
        buildSpooledJsonElement(detailed, status, array, name, false);
      }
    }
    return WaarpStringUtils.cleanJsonForHtml(JsonHandler.writeAsString(array));
  }

  /**
   * @param name
   * @param uri
   *
   * @return the String containing the JSON format of the current Spooled
   *     information
   */
  public static String buildSpooledUniqueJson(final String uri,
                                              final String name) {
    final ArrayNode array = JsonHandler.createArrayNode();
    // get current information
    synchronized (spooledInformationMap) {
      // per Name
      buildSpooledJsonElement(true, 0, array, name, false);
    }
    return WaarpStringUtils.cleanJsonForHtml(JsonHandler.writeAsString(array));
  }

  /**
   * For REST V2 service
   *
   * @param array the ArrayNode to fill with the results
   * @param status if 0, get all Spooled, 1 active ones, -1 inactive ones
   * @param name if null or empty, will get all Spooled
   *
   * @return the total number of "files" from request arguments of
   *     SpooledInformation
   */
  public static int buildSpooledJson(final ArrayNode array, final int status,
                                     final String name) {
    int nb = 0;
    if (name == null || name.isEmpty()) {
      synchronized (spooledInformationMap) {
        final Set<String> names = spooledInformationMap.keySet();
        for (final String nameInternal : names) {
          // per Name
          nb +=
              buildSpooledJsonElement(true, status, array, nameInternal, true);
        }
      }
    } else {
      synchronized (spooledInformationMap) {
        // per Name
        nb += buildSpooledJsonElement(true, status, array, name, true);
      }
    }
    return nb;
  }

  /**
   * @param detailed
   * @param status
   * @param array
   * @param name
   * @param strict
   *
   * @return number of files within
   */
  private static int buildSpooledJsonElement(final boolean detailed,
                                             final int status,
                                             final ArrayNode array,
                                             final String name,
                                             final boolean strict) {
    final SpooledInformation inform = spooledInformationMap.get(name);
    if (inform == null) {
      return 0;
    }
    final long time = inform.lastUpdate.getTime() +
                      Configuration.configuration.getTimeoutCon();
    final long curtime = System.currentTimeMillis();
    if (status != 0) {
      if (time + Configuration.configuration.getTimeoutCon() < curtime) {
        if (status < 0) {
          return 0;
        }
      } else if (status > 0) {
        return 0;
      }
    }
    final ObjectNode elt = JsonHandler.createObjectNode();
    elt.put("NAME", name.replace(',', ' '));
    elt.put("HOST", inform.host);
    if (strict) {
      elt.put("LAST_UPDATE", inform.lastUpdate.getTime());
    } else {
      final String val;
      if (time + Configuration.configuration.getTimeoutCon() < curtime) {
        val = "bg-danger";
      } else if (time < curtime) {
        val = "bg-warning";
      } else {
        val = "bg-success";
      }
      elt.put("LAST_UPDATE", val + ' ' + inform.lastUpdate.getTime());
    }
    int nb = 0;
    if (inform.fileMonitorInformation != null) {
      elt.put("GLOBALOK", inform.fileMonitorInformation.globalok.get());
      elt.put("GLOBALERROR", inform.fileMonitorInformation.globalerror.get());
      elt.put("TODAYOK", inform.fileMonitorInformation.todayok.get());
      elt.put("TODAYERROR", inform.fileMonitorInformation.todayerror.get());
      elt.put("INTERVAL", inform.fileMonitorInformation.elapseTime);
      elt.put("STOPFILE", inform.fileMonitorInformation.stopFile.getPath());
      elt.put("STATUSFILE", inform.fileMonitorInformation.statusFile.getPath());
      elt.put("SUBDIRS", inform.fileMonitorInformation.scanSubDir);
      final StringBuilder dirs = new StringBuilder();
      final StringBuilder dirs2 = new StringBuilder();
      int i = 0;
      for (final File dir : inform.fileMonitorInformation.directories) {
        i++;
        dirs.append(dir).append('(').append(i).append(") ");
        dirs2.append(dir).append(' ');
      }
      elt.put("DIRECTORIES", dirs.toString());
      if (detailed && inform.fileMonitorInformation.fileItems != null) {
        buildSpooledJsonFiles(elt, inform, dirs2.toString().split(" "), strict);
      } else {
        // simply print number of files
        if (inform.fileMonitorInformation.fileItems != null) {
          elt.putArray("FILES")
             .add(inform.fileMonitorInformation.fileItems.size());
        } else {
          elt.putArray("FILES").add(0);
        }
      }
      nb = inform.fileMonitorInformation.fileItems.size();
    }
    array.add(elt);
    return nb;
  }

  /**
   * @param node
   * @param inform
   * @param dirs
   */
  private static void buildSpooledJsonFiles(final ObjectNode node,
                                            final SpooledInformation inform,
                                            final String[] dirs,
                                            final boolean strict) {
    final ArrayNode array = node.putArray("FILES");
    if (inform.fileMonitorInformation.fileItems.isEmpty()) {
      array.add(0);
      return;
    }
    if (!strict) {
      final ArrayNode header = JsonHandler.createArrayNode();
      header.add("FILE");
      header.add("HASH");
      header.add("LASTTIME");
      header.add("USEDTIME");
      header.add("USED");
      header.add("ID");
      array.add(header);
    }
    for (final FileItem fileItem : inform.fileMonitorInformation.fileItems
        .values()) {
      final ObjectNode elt = JsonHandler.createObjectNode();
      int i = 0;
      String path = fileItem.file.getPath();
      final String sep = path.lastIndexOf('/') >= 0? "/" : "\\";
      for (final String dir : dirs) {
        i++;
        if (path.startsWith(dir + sep)) {
          path = "(" + i + ')' + path.substring(dir.length());
          break;
        }
      }
      elt.put("FILE", path);
      if (fileItem.hash != null) {
        elt.put("HASH", FilesystemBasedDigest.getHex(fileItem.hash));
      } else {
        elt.putNull("HASH");
      }
      if (fileItem.lastTime > 0) {
        elt.put("LASTTIME", fileItem.lastTime);
      } else {
        elt.putNull("LASTTIME");
      }
      if (fileItem.timeUsed > 0) {
        elt.put("USEDTIME", fileItem.timeUsed);
      } else {
        elt.putNull("USEDTIME");
      }
      elt.put("USED", fileItem.used);
      if (fileItem.specialId == ILLEGALVALUE) {
        elt.put("ID", "");
      } else {
        elt.put("ID", Long.toString(fileItem.specialId));
      }
      array.add(elt);
    }
  }
}
