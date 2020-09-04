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
package org.waarp.common.file;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply501Exception;
import org.waarp.common.command.exception.Reply530Exception;
import org.waarp.common.command.exception.Reply550Exception;
import org.waarp.common.command.exception.Reply553Exception;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.common.utility.WaarpStringUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract Main Implementation of Directory
 */
public abstract class AbstractDir implements DirInterface {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AbstractDir.class);
  /**
   * Current Directory
   */
  protected String currentDir;
  /**
   * SessionInterface
   */
  protected SessionInterface session;

  /**
   * Opts command for MLSx. (-1) means not supported, 0 supported but not
   * active, 1 supported and active
   */
  protected OptsMLSxInterface optsMLSx;
  /**
   * Hack to say Windows or Unix (root like X:\ or /)
   */
  protected static final boolean ISUNIX;
  /**
   * Roots for Windows system
   */
  protected static final File[] roots;

  /**
   * Init Windows Support
   */
  static {
    ISUNIX = !DetectionUtils.isWindows();
    if (!ISUNIX) {
      roots = File.listRoots();
    } else {
      roots = new File[] { new File("/") };
    }
  }

  /**
   * @param file
   *
   * @return The corresponding Root file
   */
  protected File getCorrespondingRoot(final File file) {
    if (ISUNIX) {
      return new File("/");
    }
    final String path = file.getAbsolutePath();
    for (final File root : roots) {
      if (path.startsWith(root.getAbsolutePath())) {
        return root;
      }
    }
    // hack !
    logger.warn("No root found for " + file.getAbsolutePath());
    return roots[0];
  }

  /**
   * Normalize Path to Internal unique representation
   *
   * @param path
   *
   * @return the normalized path
   */
  public static String normalizePath(final String path) {
    return path.replace('\\', SEPARATORCHAR);
  }

  /**
   * Convert the URI representation of a file path to a simple path.
   * <p>
   * If the path is not an URI, this method does nothing.
   *
   * @param path
   *
   * @return the normalized path
   */
  public static String pathFromURI(String path) {

    if (path.startsWith("file://")) {
      int charToRemove = 7;

      if (path.charAt(7) == '/' && path.charAt(9) == ':') {
        charToRemove++;
      }

      path = path.substring(charToRemove);
      if (path.contains("%")) {
        try {
          path = URLDecoder.decode(path, WaarpStringUtils.UTF8.name());
        } catch (final UnsupportedEncodingException e) {
          logger.warn("Cannot convert filename to UTF-8: " + path);
        } catch (final IllegalArgumentException e) {
          // ignore: it was propably not url-encoded!
        }
      }
    }
    return path;
  }

  /**
   * @return the SessionInterface
   */
  @Override
  public SessionInterface getSession() {
    return session;
  }

  @Override
  public String validatePath(final String path)
      throws CommandAbstractException {
    String extDir;
    if (isAbsolute(path)) {
      extDir = path;
      final File newDir = new File(extDir);
      try {
        return validatePath(newDir);
      } catch (final Reply553Exception e) {
        // ignore and continue as it could be not absolute
      }
    }
    if (path.charAt(0) == SEPARATORCHAR) {
      extDir = path;
    } else {
      extDir = currentDir + SEPARATOR + path;
    }
    // Get the baseDir (mount point)
    final String baseDir = getSession().getAuth().getBaseDirectory();
    // Get the translated real file path (removing '..')
    final File newDir = new File(baseDir, extDir);
    return validatePath(newDir);
  }

  /**
   * @param path
   *
   * @return True if the given Path is an absolute one under Windows System or
   *     should be an absolute one on Unix
   */
  public boolean isAbsolute(final String path) {
    File file = new File(path);
    logger.trace(
        "isAbsolute: " + file + ':' + ISUNIX + ':' + file.isAbsolute() + ':' +
        file.getParentFile());
    if (!ISUNIX) {
      return file.isAbsolute();
    } else {
      file = file.getParentFile();
      return file != null && file.isAbsolute() && file.isDirectory() &&
             !file.getAbsolutePath().equals(File.separator);
    }
  }

  /**
   * Consolidate Path as relative or absolute path to an absolute path
   *
   * @param path
   *
   * @return the consolidated path
   *
   * @throws CommandAbstractException
   */
  protected String consolidatePath(final String path)
      throws CommandAbstractException {
    if (path == null || path.isEmpty()) {
      throw new Reply501Exception("Path must not be empty");
    }
    // First check if the path is relative or absolute
    if (isAbsolute(path)) {
      return normalizePath(path);
    }
    String extDir = normalizePath(path);
    if (extDir.charAt(0) != SEPARATORCHAR) {
      extDir = currentDir + SEPARATOR + extDir;
    }
    return extDir;
  }

  /**
   * Construct the CanonicalPath without taking into account symbolic link
   *
   * @param dir
   *
   * @return the canonicalPath
   */
  protected String getCanonicalPath(final File dir) {
    if (ISUNIX) {
      // resolve it without getting symbolic links
      final StringBuilder builder = new StringBuilder();
      // Get the path in reverse order from end to start
      final List<String> list = new ArrayList<String>();
      File newdir = dir;
      String lastdir = newdir.getName();
      list.add(lastdir);
      File parent = newdir.getParentFile();
      while (parent != null) {
        newdir = parent;
        lastdir = newdir.getName();
        list.add(lastdir);
        parent = newdir.getParentFile();
      }
      // Now filter on '..' or '.'
      for (int i = list.size() - 1; i >= 0; i--) {
        final String curdir = list.get(i);
        if (".".equals(curdir)) {
          list.remove(i);// removes '.'
        } else if ("..".equals(curdir)) {
          list.remove(i);// removes '..'
          final int len = list.size();
          if (len > 0 && i < len) {
            list.remove(i);// and removes parent dir
          }
        }
      }
      if (list.isEmpty()) {
        return "/";
      }

      for (int i = list.size() - 1; i >= 0; i--) {
        builder.append('/').append(list.get(i));
      }
      return builder.toString();
    }
    // Windows version
    // no link so just use the default version of canonical Path
    try {
      return dir.getCanonicalPath();
    } catch (final IOException e) {
      return dir.getAbsolutePath();
    }
  }

  /**
   * Same as validatePath but from a FileInterface
   *
   * @param dir
   *
   * @return the construct and validated path (could be different than the one
   *     given as argument, example: '..'
   *     are removed)
   *
   * @throws CommandAbstractException
   */
  protected String validatePath(final File dir)
      throws CommandAbstractException {
    String extDir;
    extDir = normalizePath(getCanonicalPath(dir));
    // Get the relative business path
    extDir = getSession().getAuth().getRelativePath(extDir);
    // Check if this business path is valid
    if (getSession().getAuth().isBusinessPathValid(extDir)) {
      logger.trace("final path: " + extDir);
      return extDir;
    }
    throw new Reply553Exception("Pathname not allowed");
  }

  @Override
  public boolean isPathInCurrentDir(final String path) {
    final String extDir;
    if (isAbsolute(path)) {
      extDir = path;
      final File newDir = new File(extDir);
      return isPathInCurrentDir(newDir);
    }
    if (path.charAt(0) == SEPARATORCHAR) {
      extDir = path;
    } else {
      extDir = currentDir + SEPARATOR + path;
    }
    // Get the baseDir (mount point)
    final String baseDir = getSession().getAuth().getBaseDirectory();
    // Get the translated real file path (removing '..')
    final File newDir = new File(baseDir, extDir);
    return isPathInCurrentDir(newDir);
  }

  /**
   * Validate a file according to the current Directory
   *
   * @param dir
   *
   * @return True if validated
   *
   * @throws CommandAbstractException
   */
  protected boolean isPathInCurrentDir(final File dir) {
    String extDir;
    extDir = normalizePath(getCanonicalPath(dir));
    // Get the relative business path
    extDir = getSession().getAuth().getRelativePath(extDir);
    // Check if this business path is valid
    if (extDir.startsWith(currentDir)) {
      return true;
    }
    logger.warn("File not OK: {} not in {}", extDir, currentDir);
    return false;
  }

  /**
   * Finds all files matching a wildcard expression (based on '?', '~' or
   * '*').
   *
   * @param pathWithWildcard The wildcard expression with a business
   *     path.
   *
   * @return List of String as relative paths matching the wildcard
   *     expression.
   *     Those files are tested as valid
   *     from business point of view. If Wildcard support is not active,
   *     if the
   *     path contains any wildcards,
   *     it will throw an error.
   *
   * @throws CommandAbstractException
   */
  protected abstract List<String> wildcardFiles(String pathWithWildcard)
      throws CommandAbstractException;

  @Override
  public String getPwd() throws CommandAbstractException {
    return currentDir;
  }

  @Override
  public boolean changeParentDirectory() throws CommandAbstractException {
    return changeDirectory("..");
  }

  @Override
  public FileInterface setFile(final String path, final boolean append)
      throws CommandAbstractException {
    checkIdentify();
    final String newpath = consolidatePath(path);
    final List<String> paths = wildcardFiles(newpath);
    if (paths.size() != 1) {
      throw new Reply550Exception(
          "File not found: " + paths.size() + " founds");
    }
    final String extDir = paths.get(0);
    return newFile(extDir, append);
  }

  @Override
  public void checkIdentify() throws Reply530Exception {
    if (!getSession().getAuth().isIdentified()) {
      throw new Reply530Exception("User not authentified");
    }
  }

  @Override
  public void clear() {
    currentDir = null;
  }

  @Override
  public void initAfterIdentification() {
    currentDir = getSession().getAuth().getBusinessPath();
  }

  @Override
  public OptsMLSxInterface getOptsMLSx() {
    return optsMLSx;
  }

}
