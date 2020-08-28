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
package org.waarp.openr66.protocol.configuration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.protocol.utils.R66Versions;
import org.waarp.openr66.protocol.utils.Version;

import java.util.Map;

/**
 * Partner Configuration
 */
public class PartnerConfiguration {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(PartnerConfiguration.class);

  /**
   * Uses as separator in field
   */
  public static final String BAR_JSON_FIELD = "{";
  /**
   * Uses as separator in field
   */
  public static final String BAR_SEPARATOR_FIELD = ";";
  /**
   * Uses as separator in field
   */
  public static final String BLANK_SEPARATOR_FIELD = " ";
  /**
   * Uses as separator in field
   */
  private static String SEPARATOR_FIELD = BAR_SEPARATOR_FIELD;

  /**
   * JSON Fields
   */
  public enum FIELDS {
    HOSTID("nohostid"), VERSION(R66Versions.V2_4_12.getVersion()),
    DIGESTALGO(DigestAlgo.MD5.algoName), FILESIZE(false), FINALHASH(false),
    PROXIFIED(false), SEPARATOR(BLANK_SEPARATOR_FIELD);

    final String name;
    final Object defaultValue;

    FIELDS(Object def) {
      name = name();
      defaultValue = def;
    }
  }

  private final String id;
  private final ObjectNode root = JsonHandler.createObjectNode();
  private final boolean useJson;
  private boolean changeFileInfoEnabled;
  private DigestAlgo digestAlgo;

  /**
   * Constructor for an external HostId
   *
   * @param id
   * @param json mainly the version information
   */
  public PartnerConfiguration(String id, String json) {
    this.id = id;
    JsonHandler.setValue(root, FIELDS.HOSTID, id);
    final int pos = json.lastIndexOf('{');
    String version;
    if (pos > 1) {
      version = json.substring(0, pos - 1);
    } else {
      version = json;
    }
    JsonHandler.setValue(root, FIELDS.VERSION, version);
    if (isVersion2GEQVersion1(R66Versions.V2_4_12.getVersion(), version)) {
      JsonHandler.setValue(root, FIELDS.FILESIZE, true);
      JsonHandler.setValue(root, FIELDS.FINALHASH, true);
    } else {
      JsonHandler.setValue(root, FIELDS.FILESIZE,
                           (Boolean) FIELDS.FILESIZE.defaultValue);
      JsonHandler.setValue(root, FIELDS.FINALHASH,
                           (Boolean) FIELDS.FINALHASH.defaultValue);
    }
    JsonHandler.setValue(root, FIELDS.DIGESTALGO,
                         Configuration.configuration.getDigest().algoName);
    JsonHandler.setValue(root, FIELDS.PROXIFIED,
                         (Boolean) FIELDS.PROXIFIED.defaultValue);
    String sep = getSEPARATOR_FIELD();
    if (!isVersion2GEQVersion1(R66Versions.V2_4_13.getVersion(), version)) {
      sep = BLANK_SEPARATOR_FIELD;
    }
    if (isVersion2GEQVersion1(R66Versions.V2_4_17.getVersion(), version)) {
      logger.debug("UseJson for " + id + ':' + json);
      useJson = true;
    } else {
      logger.debug("NOT UseJson for " + id + ':' + json);
      useJson = false;
    }
    if (isVersion2GEQVersion1(R66Versions.V3_0_4.getVersion(), version)) {
      changeFileInfoEnabled = true;
    }
    JsonHandler.setValue(root, FIELDS.SEPARATOR, sep);

    if (json != null && pos > 1) {
      final String realjson = json.substring(pos);
      final ObjectNode info = JsonHandler.getFromString(realjson);
      if (info != null) {
        root.setAll(info);
      }
    }
    if (isProxified()) {
      Configuration.configuration.setBlacklistBadAuthent(false);
    }
    digestAlgo = getDigestAlgoInternal();
    logger.debug("Info on HostId: " + root);
  }

  /**
   * Self constructor
   *
   * @param id
   */
  public PartnerConfiguration(String id) {
    this.id = id;
    JsonHandler.setValue(root, FIELDS.HOSTID, id);
    JsonHandler.setValue(root, FIELDS.VERSION, Version.ID);
    JsonHandler.setValue(root, FIELDS.FILESIZE, true);
    JsonHandler.setValue(root, FIELDS.FINALHASH,
                         Configuration.configuration.isGlobalDigest());
    JsonHandler.setValue(root, FIELDS.DIGESTALGO,
                         Configuration.configuration.getDigest().algoName);
    JsonHandler.setValue(root, FIELDS.PROXIFIED,
                         Configuration.configuration.isHostProxyfied());
    JsonHandler.setValue(root, FIELDS.SEPARATOR, getSEPARATOR_FIELD());
    useJson = true;
    digestAlgo = getDigestAlgoInternal();
    logger.debug("Info on HostId: " + root);
  }

  /**
   * @return the associated HostId
   */
  public String getId() {
    return id;
  }

  /**
   * @return the version for this Host
   */
  public String getVersion() {
    return root.path(FIELDS.VERSION.name).asText();
  }

  /**
   * @return True if this Host returns FileSize
   */
  public boolean useFileSize() {
    return root.path(FIELDS.FILESIZE.name)
               .asBoolean((Boolean) FIELDS.FILESIZE.defaultValue);
  }

  /**
   * @return True if this Host returns a final hash
   */
  public boolean useFinalHash() {
    return root.path(FIELDS.FINALHASH.name)
               .asBoolean((Boolean) FIELDS.FINALHASH.defaultValue);
  }

  /**
   * @return DigestAlgo if this Host returns Digest Algo used
   */
  public DigestAlgo getDigestAlgo() {
    return digestAlgo;
  }

  /**
   * Used to initialize the DigestAlgo for this partner
   *
   * @return the DigestAlgo
   */
  private DigestAlgo getDigestAlgoInternal() {
    final String algo = root.path(FIELDS.DIGESTALGO.name).asText();
    return getDigestAlgo(algo);
  }

  /**
   * @return True if this Host is proxified
   */
  public boolean isProxified() {
    return root.path(FIELDS.PROXIFIED.name)
               .asBoolean((Boolean) FIELDS.PROXIFIED.defaultValue);
  }

  /**
   * @return the separator for this Host
   */
  public String getSeperator() {
    return root.path(FIELDS.SEPARATOR.name).asText();
  }

  /**
   * @return the useJson
   */
  public boolean useJson() {
    return useJson;
  }

  /**
   * @return the changeFileInfoEnabled
   */
  public boolean changeFileInfoEnabled() {
    return changeFileInfoEnabled;
  }

  /**
   * @return the String representation as version.json
   */
  @Override
  public String toString() {
    return getVersion() + '.' + JsonHandler.writeAsString(root);
  }

  public static final DigestAlgo getDigestAlgo(String algo) {
    for (final DigestAlgo alg : DigestAlgo.values()) {
      if (alg.algoName.equals(algo)) {
        return alg;
      }
    }
    try {
      return DigestAlgo.valueOf(algo);
    } catch (final IllegalArgumentException ignored) {
      // ignore
    }
    return Configuration.configuration.getDigest();
  }

  /**
   * @param remoteHost
   *
   * @return the separator to be used
   */
  public static final String getSeparator(String remoteHost) {
    logger.debug("Versions: search: " + remoteHost + " in {}",
                 Configuration.configuration.getVersions());
    final PartnerConfiguration partner =
        Configuration.configuration.getVersions().get(remoteHost);
    if (partner != null) {
      return partner.getSeperator();
    }
    return BLANK_SEPARATOR_FIELD;
  }

  /**
   * Compare 2 versions
   *
   * @param version1
   * @param version2
   *
   * @return True if version2 >= version1
   */
  public static final boolean isVersion2GEQVersion1(String version1,
                                                    String version2) {
    if (version1 == null || version2 == null) {
      return false;
    }
    int major1;
    int rank1;
    int subversion1;
    String[] vals = version1.split("\\.");
    major1 = Integer.parseInt(vals[0]);
    rank1 = Integer.parseInt(vals[1]);
    subversion1 = Integer.parseInt(vals[2]);
    int major2;
    int rank2;
    int subversion2;
    vals = version2.split("\\.");
    major2 = Integer.parseInt(vals[0]);
    rank2 = Integer.parseInt(vals[1]);
    subversion2 = Integer.parseInt(vals[2]);
    final boolean b = major1 < major2 || major1 == major2 && (rank1 < rank2 ||
                                                              rank1 == rank2 &&
                                                              subversion1 <=
                                                              subversion2);
    logger.trace(
        "1: " + major1 + ':' + rank1 + ':' + subversion1 + " <=? " + major2 +
        ':' + rank2 + ':' + subversion2 + " = " + (b));
    return b;
  }

  /**
   * Compare strictly 2 versions
   *
   * @param version1
   * @param version2
   *
   * @return True if version2 > version1
   */
  public static final boolean isVersion2GTVersion1(String version1,
                                                   String version2) {
    if (version1 == null || version2 == null) {
      return false;
    }
    int major1;
    int rank1;
    int subversion1;
    String[] vals = version1.split("\\.");
    major1 = Integer.parseInt(vals[0]);
    rank1 = Integer.parseInt(vals[1]);
    subversion1 = Integer.parseInt(vals[2]);
    int major2;
    int rank2;
    int subversion2;
    vals = version2.split("\\.");
    major2 = Integer.parseInt(vals[0]);
    rank2 = Integer.parseInt(vals[1]);
    subversion2 = Integer.parseInt(vals[2]);
    final boolean b = major1 < major2 || major1 == major2 && (rank1 < rank2 ||
                                                              rank1 == rank2 &&
                                                              subversion1 <
                                                              subversion2);
    logger.debug(
        "1: " + major1 + ':' + rank1 + ':' + subversion1 + " <? " + major2 +
        ':' + rank2 + ':' + subversion2 + " = " + (b));
    return b;
  }

  /**
   * @param host
   *
   * @return True if this host is referenced as using Json
   */
  public static final boolean useJson(String host) {
    logger.debug("UseJson host: '" + host + "':" +
                 (Configuration.configuration.getVersions().containsKey(host)?
                     Configuration.configuration.getVersions().get(host)
                                                .useJson() : "no:" +
                                                             ((Map<String, PartnerConfiguration>) Configuration.configuration
                                                                 .getVersions())
                                                                 .keySet()));
    return Configuration.configuration.getVersions().containsKey(host) &&
           Configuration.configuration.getVersions().get(host).useJson();
  }

  /**
   * @return the sEPARATOR_FIELD
   */
  public static String getSEPARATOR_FIELD() {
    return SEPARATOR_FIELD;
  }

  /**
   * @param separatorFIELD the sEPARATOR_FIELD to set
   */
  public static void setSEPARATOR_FIELD(String separatorFIELD) {
    SEPARATOR_FIELD = separatorFIELD;
  }
}
