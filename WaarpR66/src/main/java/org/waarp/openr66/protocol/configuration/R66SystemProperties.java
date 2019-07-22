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

/**
 * This class is only intend to concentrate all SystemProperty definitions
 *
 *
 */
public class R66SystemProperties {

  /**
   * Used to set en/fr or other locale defined (default en)
   */
  public static final String OPENR66_LOCALE = "openr66.locale";
  /**
   * True if this host is proxyfied for other partners (default false)
   */
  public static final String OPENR66_ISHOSTPROXYFIED =
      "openr66.ishostproxyfied";
  /**
   * Shall Waarp use space as separator (buggy, should be false)
   */
  public static final String OPENR66_USESPACESEPARATOR =
      "openr66.usespaceseparator";
  /**
   * Shall Waarp allow to execute error actions in case no transfer at all
   * occurs but an error in "pre-task"
   * occurs: default true
   */
  public static final String OPENR66_EXECUTEBEFORETRANSFERRED =
      "openr66.executebeforetransferred";
  /**
   * Use in Windows Service mode to specify the location of the configuration
   * file
   */
  public static final String OPENR66_CONFIGFILE = "org.waarp.r66.config.file";
  /**
   * Shall we print info of startup in Warning mode (true, default) or Info
   * mode
   * (false)
   */
  public static final String OPENR66_STARTUP_WARNING =
      "openr66.startup.warning";
  /**
   * Shall the database being check at startup (default is false)
   */
  @Deprecated
  public static final String OPENR66_STARTUP_DATABASE_CHECK =
      "openr66.startup.dbcheck";
  /**
   * Shall the database being check at startup (default is false)
   */
  public static final String OPENR66_STARTUP_DATABASE_AUTOUPGRADE =
      "openr66.startup.autoUpgrade";
  /**
   * Shall we allow or not a request as RECV that will access to file outside
   * Rule Out directory (default =
   * false)
   */
  public static final String OPENR66_CHROOT_CHECKED = "openr66.chroot.checked";
  /**
   * Shall we blacklist badly authenticated servers (default = true, if
   * OPENR66_ISHOSTPROXYFIED is true, then is
   * mandatory false). Note that this must not be true if several partners
   * might
   * have the same IP (proxyfied for
   * instance), since they will all be banned.
   */
  public static final String OPENR66_BLACKLIST_BADAUTHENT =
      "openr66.blacklist.badauthent";
  /**
   * Filename max length (default = 255), only for Basename (not full path).
   * to
   * choose the default max filename
   * length used when receiving a file (for the temporary filename and final
   * filename). This does not prevent to
   * change the filename after (and #ORIGINALFILENAME# does still contain the
   * full filename, not truncated).
   */
  public static final String OPENR66_FILENAME_MAXLENGTH =
      "openr66.filename.maxlength";
  /**
   * Debug by making a trace of consumption every x s, where x is specify as
   * value in the definition. If 0 or
   * less, means not activated.
   */
  public static final String OPENR66_TRACE_STATS = "openr66.trace.stats";
  /**
   * Maximum number of DbTaskRunners to keep in LRU cache (used in self
   * request
   * and for instance without
   * database). Minimal value is 100.
   */
  public static final String OPENR66_CACHE_LIMIT = "openr66.cache.limit";
  /**
   * Maximum time in ms of valid element once created, used or updated (used
   * in
   * self request and for instance
   * without database). Minimal value is 1000 ms (1s). If set to 1000, the
   * value
   * will not be regularly deleted.
   */
  public static final String OPENR66_CACHE_TIMELIMIT =
      "openr66.cache.timelimit";

}
