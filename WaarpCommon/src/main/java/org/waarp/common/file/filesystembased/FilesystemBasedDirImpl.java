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
package org.waarp.common.file.filesystembased;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply550Exception;
import org.waarp.common.command.exception.Reply553Exception;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.file.AbstractDir;
import org.waarp.common.file.FileInterface;
import org.waarp.common.file.FileUtils;
import org.waarp.common.file.OptsMLSxInterface;
import org.waarp.common.file.SessionInterface;
import org.waarp.common.file.filesystembased.specific.FilesystemBasedCommonsIo;
import org.waarp.common.file.filesystembased.specific.FilesystemBasedDirJdk5;
import org.waarp.common.file.filesystembased.specific.FilesystemBasedDirJdk6;
import org.waarp.common.file.filesystembased.specific.FilesystemBasedDirJdkAbstract;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.DetectionUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import static org.waarp.common.file.FileUtils.*;

/**
 * Directory implementation for Filesystem Based
 */
public abstract class FilesystemBasedDirImpl extends AbstractDir {
  private static final String ERROR_WHILE_READING_FILE =
      "Error while reading file: ";

  private static final String DIRECTORY_NOT_FOUND = "Directory not found: ";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FilesystemBasedDirImpl.class);

  /**
   * Class that handles specifity of one Jdk or another
   */
  protected static FilesystemBasedDirJdkAbstract filesystemBasedFtpDirJdk;

  /**
   * Initialize the filesystem
   */
  static {
    initJdkDependent();
  }

  /**
   * Init according to internals of JDK
   */
  private static void initJdkDependent() {
    if (DetectionUtils.javaVersion() >= 6) {
      filesystemBasedFtpDirJdk = new FilesystemBasedDirJdk6();
    } else {
      filesystemBasedFtpDirJdk = new FilesystemBasedDirJdk5();
    }
  }

  /**
   * Init the dependant object according to internals of JDK
   *
   * @param filesystemBasedFtpDirJdkChoice
   *
   * @deprecated replaced by initJdkDependent()
   */
  @Deprecated
  public static void initJdkDependent(
      final FilesystemBasedDirJdkAbstract filesystemBasedFtpDirJdkChoice) {
    filesystemBasedFtpDirJdk = filesystemBasedFtpDirJdkChoice;
  }

  /**
   * @param session
   * @param optsMLSx
   */
  protected FilesystemBasedDirImpl(final SessionInterface session,
                                   final OptsMLSxInterface optsMLSx) {
    this.session = session;
    this.optsMLSx = optsMLSx;
    this.optsMLSx.setOptsModify((byte) 1);
    this.optsMLSx.setOptsPerm((byte) 1);
    this.optsMLSx.setOptsSize((byte) 1);
    this.optsMLSx.setOptsType((byte) 1);
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
  @Override
  protected List<String> wildcardFiles(final String pathWithWildcard)
      throws CommandAbstractException {
    final List<String> resultPaths = new ArrayList<String>();
    // First check if pathWithWildcard contains wildcards
    if (!(pathWithWildcard.contains("*") || pathWithWildcard.contains("?") ||
          pathWithWildcard.contains("~"))) {
      // No so simply return the list containing this path after
      // validating it
      if (getSession().getAuth().isBusinessPathValid(pathWithWildcard)) {
        resultPaths.add(pathWithWildcard);
      }
      return resultPaths;
    }
    // Do we support Wildcard path
    if (!FilesystemBasedDirJdkAbstract.ueApacheCommonsIo) {
      throw new Reply553Exception("Wildcards in pathname is not allowed");
    }
    File wildcardFile;
    final File rootFile;
    if (!ISUNIX && isAbsolute(pathWithWildcard)) {
      wildcardFile = new File(pathWithWildcard);
      rootFile = getCorrespondingRoot(wildcardFile);
    } else {
      if (isAbsolute(pathWithWildcard)) {
        rootFile = new File("/");
      } else {
        rootFile = new File(getSession().getAuth().getBaseDirectory());
      }
      wildcardFile = new File(rootFile, pathWithWildcard);
    }
    // Split wildcard path into subdirectories.
    final List<String> subdirs = new ArrayList<String>();
    while (wildcardFile != null) {
      final File parent = wildcardFile.getParentFile();
      if (parent == null) {
        subdirs.add(0, wildcardFile.getPath());
        break;
      }
      subdirs.add(0, wildcardFile.getName());
      if (parent.equals(rootFile)) {
        // End of wildcard path
        subdirs.add(0, parent.getPath());
        break;
      }
      wildcardFile = parent;
    }
    List<File> basedPaths = new ArrayList<File>();
    // First set root
    basedPaths.add(new File(subdirs.get(0)));
    int i = 1;
    // For each wilcard subdirectory
    while (i < subdirs.size()) {
      // Set current filter
      final FileFilter fileFilter =
          FilesystemBasedCommonsIo.getWildcardFileFilter(subdirs.get(i));
      final List<File> newBasedPaths = new ArrayList<File>();
      // Look for matches in all the current search paths
      for (final File dir : basedPaths) {
        if (dir.isDirectory()) {
          newBasedPaths.addAll(Arrays.asList(dir.listFiles(fileFilter)));
        }
      }
      // base Search Path changes now
      basedPaths = newBasedPaths;
      i++;
    }
    // Valid each file first
    for (final File file : basedPaths) {
      final String relativePath = getSession().getAuth().getRelativePath(
          normalizePath(file.getAbsolutePath()));
      final String newpath = validatePath(relativePath);
      resultPaths.add(newpath);
    }
    return resultPaths;
  }

  /**
   * Get the FileInterface from this path, checking first its validity
   *
   * @param path
   *
   * @return the FileInterface
   *
   * @throws CommandAbstractException
   */
  protected File getFileFromPath(final String path)
      throws CommandAbstractException {
    final String newdir = validatePath(path);
    if (isAbsolute(newdir)) {
      return new File(newdir);
    }
    final String truedir = ((FilesystemBasedAuthImpl) getSession().getAuth())
        .getAbsolutePath(newdir);
    return new File(truedir);
  }

  /**
   * Get the true file from the path
   *
   * @param path
   *
   * @return the true File from the path
   *
   * @throws CommandAbstractException
   */
  protected File getTrueFile(final String path)
      throws CommandAbstractException {
    checkIdentify();
    final String newpath = consolidatePath(path);
    final List<String> paths = wildcardFiles(normalizePath(newpath));
    if (paths.size() != 1) {
      throw new Reply550Exception(
          "File not found: " + paths.size() + " founds");
    }
    String extDir = paths.get(0);
    extDir = validatePath(extDir);
    final File file = getFileFromPath(extDir);
    if (!file.isFile()) {
      throw new Reply550Exception("Path is not a file: " + path);
    }
    return file;
  }

  /**
   * Get the relative path (without mount point)
   *
   * @param file
   *
   * @return the relative path
   */
  protected String getRelativePath(final File file) {
    return getSession().getAuth()
                       .getRelativePath(normalizePath(file.getAbsolutePath()));
  }

  @Override
  public boolean changeDirectory(final String path)
      throws CommandAbstractException {
    checkIdentify();
    final String newpath = consolidatePath(path);
    final List<String> paths = wildcardFiles(newpath);
    if (paths.size() != 1) {
      logger.warn("CD error: {}", newpath);
      throw new Reply550Exception(
          DIRECTORY_NOT_FOUND + paths.size() + " founds");
    }
    String extDir = paths.get(0);
    extDir = validatePath(extDir);
    if (isDirectory(extDir)) {
      currentDir = extDir;
      return true;
    }
    throw new Reply550Exception(DIRECTORY_NOT_FOUND + extDir);
  }

  @Override
  public boolean changeDirectoryNotChecked(final String path)
      throws CommandAbstractException {
    checkIdentify();
    final String newpath = consolidatePath(path);
    final List<String> paths = wildcardFiles(newpath);
    if (paths.size() != 1) {
      logger.warn("CD error: {}", newpath);
      throw new Reply550Exception(
          DIRECTORY_NOT_FOUND + paths.size() + " founds");
    }
    String extDir = paths.get(0);
    extDir = validatePath(extDir);
    currentDir = extDir;
    return true;
  }

  @Override
  public String mkdir(final String directory) throws CommandAbstractException {
    checkIdentify();
    final String newdirectory = consolidatePath(directory);
    final File dir = new File(newdirectory);
    final String parent = dir.getParentFile().getPath();
    final List<String> paths = wildcardFiles(normalizePath(parent));
    if (paths.size() != 1) {
      throw new Reply550Exception(
          "Base Directory not found: " + paths.size() + " founds");
    }
    String newDir = paths.get(0) + SEPARATOR + dir.getName();
    newDir = validatePath(newDir);
    final File newdir = getFileFromPath(newDir);
    if (newdir.mkdir()) {
      return newDir;
    }
    throw new Reply550Exception("Cannot create directory " + newDir);
  }

  @Override
  public String rmdir(final String directory) throws CommandAbstractException {
    checkIdentify();
    final String newdirectory = consolidatePath(directory);
    final List<String> paths = wildcardFiles(normalizePath(newdirectory));
    if (paths.size() != 1) {
      throw new Reply550Exception(
          DIRECTORY_NOT_FOUND + paths.size() + " founds");
    }
    String extDir = paths.get(0);
    extDir = validatePath(extDir);
    final File dir = getFileFromPath(extDir);
    if (dir.delete()) {
      return extDir;
    }
    throw new Reply550Exception("Cannot delete directory " + extDir);
  }

  @Override
  public boolean isDirectory(final String path)
      throws CommandAbstractException {
    checkIdentify();
    final File dir = getFileFromPath(path);
    return dir.isDirectory();
  }

  @Override
  public boolean isFile(final String path) throws CommandAbstractException {
    checkIdentify();
    return getFileFromPath(path).isFile();
  }

  @Override
  public String getModificationTime(final String path)
      throws CommandAbstractException {
    checkIdentify();
    final File file = getFileFromPath(path);
    if (file.exists()) {
      return getModificationTime(file);
    }
    throw new Reply550Exception('"' + path + "\" does not exist");
  }

  /**
   * Return the Modification time for the File
   *
   * @param file
   *
   * @return the Modification time as a String YYYYMMDDHHMMSS.sss
   */
  protected String getModificationTime(final File file) {
    final long mstime = file.lastModified();
    final Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(mstime);
    final int year = calendar.get(Calendar.YEAR);
    final int month = calendar.get(Calendar.MONTH) + 1;
    final int day = calendar.get(Calendar.DAY_OF_MONTH);
    final int hour = calendar.get(Calendar.HOUR_OF_DAY);
    final int minute = calendar.get(Calendar.MINUTE);
    final int second = calendar.get(Calendar.SECOND);
    final int ms = calendar.get(Calendar.MILLISECOND);
    final StringBuilder sb = new StringBuilder(18);
    sb.append(year);
    if (month < 10) {
      sb.append(0);
    }
    sb.append(month);
    if (day < 10) {
      sb.append(0);
    }
    sb.append(day);
    if (hour < 10) {
      sb.append(0);
    }
    sb.append(hour);
    if (minute < 10) {
      sb.append(0);
    }
    sb.append(minute);
    if (second < 10) {
      sb.append(0);
    }
    sb.append(second).append('.');
    if (ms < 10) {
      sb.append(0);
    }
    if (ms < 100) {
      sb.append(0);
    }
    sb.append(ms);
    return sb.toString();
  }

  @Override
  public List<String> list(final String path) throws CommandAbstractException {
    checkIdentify();
    // First get all base directories
    String newpath = path;
    if (newpath == null || newpath.isEmpty()) {
      newpath = currentDir;
    }
    if (newpath.startsWith("-a") || newpath.startsWith("-A")) {
      final String[] args = newpath.split(" ");
      if (args.length > 1) {
        newpath = args[1];
      } else {
        newpath = currentDir;
      }
    }
    newpath = consolidatePath(newpath);
    logger.debug("debug: {}", newpath);
    final List<String> paths = wildcardFiles(newpath);
    if (paths.isEmpty()) {
      throw new Reply550Exception("No files found");
    }
    // Now if they are directories, list inside them
    final List<String> newPaths = new ArrayList<String>();
    for (final String file : paths) {
      final File dir = getFileFromPath(file);
      if (dir.exists()) {
        if (dir.isDirectory()) {
          final String[] files = dir.list();
          for (final String finalFile : files) {
            final String relativePath =
                getSession().getAuth().getRelativePath(finalFile);
            newPaths.add(relativePath);
          }
        } else {
          newPaths.add(file);
        }
      }
    }
    return newPaths;
  }

  @Override
  public List<String> listFull(final String path, final boolean lsFormat)
      throws CommandAbstractException {
    checkIdentify();
    boolean listAllFiles = false;
    String newpath = path;
    if (newpath == null || newpath.isEmpty()) {
      newpath = currentDir;
    }
    if (newpath.startsWith("-a") || newpath.startsWith("-A")) {
      final String[] args = newpath.split(" ");
      if (args.length > 1) {
        newpath = args[1];
      } else {
        newpath = currentDir;
      }
      listAllFiles = true;
    }
    newpath = consolidatePath(newpath);
    // First get all base directories
    final List<String> paths = wildcardFiles(newpath);
    if (paths.isEmpty()) {
      throw new Reply550Exception("No files found");
    }
    // Now if they are directories, list inside them
    final List<String> newPaths = new ArrayList<String>();
    for (final String file : paths) {
      final File dir = getFileFromPath(file);
      if (dir.exists()) {
        if (dir.isDirectory()) {
          final File[] files = dir.listFiles();
          for (final File finalFile : files) {
            if (lsFormat) {
              newPaths.add(lsInfo(finalFile));
            } else {
              newPaths.add(mlsxInfo(finalFile));
            }
          }
        } else {
          if (lsFormat) {
            newPaths.add(lsInfo(dir));
          } else {
            newPaths.add(mlsxInfo(dir));
          }
        }
      }
    }
    if (listAllFiles) {
      final File dir = new File(getFileFromPath(newpath), SEPARATOR + "..");
      if (lsFormat) {
        newPaths.add(lsInfo(dir));
      } else {
        newPaths.add(mlsxInfo(dir));
      }
    }
    return newPaths;
  }

  @Override
  public String fileFull(final String path, final boolean lsFormat)
      throws CommandAbstractException {
    checkIdentify();
    final String newpath = consolidatePath(path);
    final List<String> paths = wildcardFiles(normalizePath(newpath));
    if (paths.size() != 1) {
      throw new Reply550Exception("No files found " + paths.size() + " founds");
    }
    final File file = getFileFromPath(paths.get(0));
    if (file.exists()) {
      if (lsFormat) {
        return "Listing of \"" + paths.get(0) + "\"\n" + lsInfo(file) +
               "\nEnd of listing";
      }
      return "Listing of \"" + paths.get(0) + "\"\n" + mlsxInfo(file) +
             "\nEnd of listing";
    }
    return "No file with name \"" + path + '"';
  }

  /**
   * Decide if Full time or partial time as in 'ls' command
   *
   * @return True if Full Time, False is Default (as in 'ls' command)
   */
  protected boolean isFullTime() {
    return false;
  }

  /**
   * @param file
   *
   * @return the ls format information
   */
  protected String lsInfo(final File file) {
    // Unix FileInterface type,permissions,hard
    // link(?),owner(?),group(?),size,date
    // and filename
    final StringBuilder builder =
        new StringBuilder().append(file.isDirectory()? 'd' : '-')
                           .append(file.canRead()? 'r' : '-')
                           .append(file.canWrite()? 'w' : '-');
    if (filesystemBasedFtpDirJdk != null) {
      builder.append(filesystemBasedFtpDirJdk.canExecute(file)? 'x' : '-');
    } else {
      builder.append('-');
    }
    // Group and others not supported
    builder.append("---").append("---").append(' ').append("1 ")// hard link ?
           .append("anybody\t")// owner ?
           .append("anygroup\t")// group ?
           .append(file.length())// size
           .append('\t');
    final long lastmod = file.lastModified();
    final String fmt;
    // It seems Full Time is not recognized by some FTP client
    final long currentTime = System.currentTimeMillis();
    if (currentTime > lastmod + 6L * 30L * 24L * 60L * 60L * 1000L // Old.
        || currentTime < lastmod - 60L * 60L * 1000L) { // In the
      // future.
      // The file is fairly old or in the future.
      // POSIX says the cutoff is 6 months old.
      // approximate this by 6*30 days.
      // Allow a 1 hour slop factor for what is considered "the future",
      // to allow for NFS server/client clock disagreement.
      // Show the year instead of the time of day.
      fmt = "MMM dd  yyyy";
    } else {
      fmt = "MMM dd HH:mm";
    }
    final SimpleDateFormat dateFormat = (SimpleDateFormat) DateFormat
        .getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.ENGLISH);
    dateFormat.applyPattern(fmt);
    builder.append(dateFormat.format(new Date(lastmod)))// date
           .append('\t').append(file.getName());
    return builder.toString();
  }

  /**
   * @param file
   *
   * @return the MLSx information: ' Fact=facts;...; filename'
   */
  protected String mlsxInfo(final File file) {
    // don't have create, unique, lang, media-type, charset
    final StringBuilder builder = new StringBuilder(" ");
    if (getOptsMLSx().getOptsSize() == 1) {
      builder.append("Size=").append(file.length()).append(';');
    }
    if (getOptsMLSx().getOptsModify() == 1) {
      builder.append("Modify=").append(getModificationTime(file)).append(';');
    }
    if (getOptsMLSx().getOptsType() == 1) {
      builder.append("Type=");
      try {
        if (getFileFromPath(currentDir).equals(file)) {
          builder.append("cdir");
        } else {
          if (file.isDirectory()) {
            builder.append("dir");
          } else {
            builder.append("file");
          }
        }
      } catch (final CommandAbstractException e) {
        if (file.isDirectory()) {
          builder.append("dir");
        } else {
          builder.append("file");
        }
      }
      builder.append(';');
    }
    if (getOptsMLSx().getOptsPerm() == 1) {
      builder.append("Perm=");
      if (file.isFile()) {
        if (file.canWrite()) {
          builder.append('a').append('d').append('f').append('w');
        }
        if (file.canRead()) {
          builder.append('r');
        }
      } else {
        // Directory
        if (file.canWrite()) {
          builder.append('c');
          try {
            if (validatePath(file) != null) {
              builder.append('d').append('m').append('p');
            }
          } catch (final CommandAbstractException ignored) {
            // nothing
          }
        }
        if (file.canRead()) {
          builder.append('l').append('e');
        }
      }
      builder.append(';');
    }

    builder.append(' ').append(file.getName());
    return builder.toString();
  }

  @Override
  public long getFreeSpace() throws CommandAbstractException {
    checkIdentify();
    final File directory = getFileFromPath(currentDir);
    if (filesystemBasedFtpDirJdk != null) {
      return filesystemBasedFtpDirJdk.getFreeSpace(directory);
    } else {
      return Integer.MAX_VALUE;
    }
  }

  @Override
  public FileInterface setUniqueFile() throws CommandAbstractException {
    checkIdentify();
    final File file;
    try {
      file = File.createTempFile(getSession().getAuth().getUser(),
                                 session.getUniqueExtension(),
                                 getFileFromPath(currentDir));
    } catch (final IOException e) {
      throw new Reply550Exception("Cannot create unique file");
    }
    final String currentFile = getRelativePath(file);
    return newFile(normalizePath(currentFile), false);
  }

  @Override
  public boolean canRead() throws CommandAbstractException {
    checkIdentify();
    return getFileFromPath(currentDir).canRead();
  }

  @Override
  public boolean canWrite() throws CommandAbstractException {
    checkIdentify();
    final File file = getFileFromPath(currentDir);
    return file.canWrite();
  }

  @Override
  public boolean exists() throws CommandAbstractException {
    checkIdentify();
    return getFileFromPath(currentDir).exists();
  }

  @Override
  public long getCRC(final String path) throws CommandAbstractException {
    final File file = getTrueFile(path);
    FileInputStream fis = null;
    CheckedInputStream cis = null;
    try {
      try {
        // Computer CRC32 checksum
        fis = new FileInputStream(file);
        cis = new CheckedInputStream(fis, new CRC32());
      } catch (final FileNotFoundException e) {
        throw new Reply550Exception("File not found: " + path);
      }
      final byte[] buf = new byte[ZERO_COPY_CHUNK_SIZE];
      while (cis.read(buf) >= 0) {
        // nothing
      }
      return cis.getChecksum().getValue();
    } catch (final IOException e) {
      throw new Reply550Exception(ERROR_WHILE_READING_FILE + path);
    } finally {
      FileUtils.close(cis);
      FileUtils.close(fis);
    }
  }

  @Override
  public byte[] getMD5(final String path) throws CommandAbstractException {
    return getDigest(path, DigestAlgo.MD5.name());
  }

  @Override
  public byte[] getSHA1(final String path) throws CommandAbstractException {
    return getDigest(path, DigestAlgo.SHA1.name());
  }

  @Override
  public byte[] getDigest(final String path, final String algo)
      throws CommandAbstractException {
    final DigestAlgo digestAlgo;
    try {
      digestAlgo = DigestAlgo.getFromName(algo);
    } catch (final IllegalArgumentException e) {
      throw new Reply553Exception("Algorithme unknown: " + algo);
    }
    final File file = getTrueFile(path);
    try {
      return FilesystemBasedDigest
          .getHash(file, FilesystemBasedFileParameterImpl.useNio, digestAlgo);
    } catch (final IOException e1) {
      throw new Reply550Exception(ERROR_WHILE_READING_FILE + path);
    }
  }

}
