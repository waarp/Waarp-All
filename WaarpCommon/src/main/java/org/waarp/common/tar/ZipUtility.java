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
package org.waarp.common.tar;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.SysErrLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public final class ZipUtility {

  private static final File[] FILE_0_LENGTH = {};

  private ZipUtility() {
  }

  /**
   * Create a new Zip from a root directory
   *
   * @param directory the base directory
   * @param filename the output filename
   * @param absolute store absolute filepath (from directory) or only
   *     filename
   *
   * @return True if OK
   */
  public static boolean createZipFromDirectory(String directory,
                                               String filename,
                                               boolean absolute) {
    final File rootDir = new File(directory);
    final File saveFile = new File(filename);
    // recursive call
    ZipArchiveOutputStream zaos;
    try {
      zaos = new ZipArchiveOutputStream(new FileOutputStream(saveFile));
    } catch (final FileNotFoundException e) {
      return false;
    }
    try {
      recurseFiles(rootDir, rootDir, zaos, absolute);
    } catch (final IOException e2) {
      FileUtils.close(zaos);
      return false;
    }
    try {
      zaos.finish();
    } catch (final IOException e1) {
      // ignore
    }
    FileUtils.close(zaos);
    return true;
  }

  /**
   * Recursive traversal to add files
   *
   * @param root
   * @param file
   * @param zaos
   * @param absolute
   *
   * @throws IOException
   */
  private static void recurseFiles(File root, File file,
                                   ZipArchiveOutputStream zaos,
                                   boolean absolute) throws IOException {
    if (file.isDirectory()) {
      // recursive call
      final File[] files = file.listFiles();
      for (final File file2 : files) {
        recurseFiles(root, file2, zaos, absolute);
      }
    } else if (!file.getName().endsWith(".zip") &&
               !file.getName().endsWith(".ZIP")) {
      String filename;
      if (absolute) {
        filename =
            file.getAbsolutePath().substring(root.getAbsolutePath().length());
      } else {
        filename = file.getName();
      }
      final ZipArchiveEntry zae = new ZipArchiveEntry(filename);
      zae.setSize(file.length());
      zaos.putArchiveEntry(zae);
      final FileInputStream fis = new FileInputStream(file);
      IOUtils.copy(fis, zaos);
      zaos.closeArchiveEntry();
    }
  }

  /**
   * Create a new Zip from a list of Files (only name of files will be used)
   *
   * @param files list of files to add
   * @param filename the output filename
   *
   * @return True if OK
   */
  public static boolean createZipFromFiles(List<File> files, String filename) {
    return createZipFromFiles(files.toArray(FILE_0_LENGTH), filename);
  }

  /**
   * Create a new Zip from an array of Files (only name of files will be used)
   *
   * @param files array of files to add
   * @param filename the output filename
   *
   * @return True if OK
   */
  public static boolean createZipFromFiles(File[] files, String filename) {
    final File saveFile = new File(filename);
    ZipArchiveOutputStream zaos;
    try {
      zaos = new ZipArchiveOutputStream(new FileOutputStream(saveFile));
    } catch (final FileNotFoundException e) {
      return false;
    }
    for (final File file : files) {
      try {
        addFile(file, zaos);
      } catch (final IOException e) {
        FileUtils.close(zaos);
        return false;
      }
    }
    try {
      zaos.finish();
    } catch (final IOException e1) {
      // ignore
    }
    FileUtils.close(zaos);
    return true;
  }

  /**
   * Recursive traversal to add files
   *
   * @param file
   * @param zaos
   *
   * @throws IOException
   */
  private static void addFile(File file, ZipArchiveOutputStream zaos)
      throws IOException {
    String filename;
    filename = file.getName();
    final ZipArchiveEntry zae = new ZipArchiveEntry(filename);
    zae.setSize(file.length());
    zaos.putArchiveEntry(zae);
    final FileInputStream fis = new FileInputStream(file);
    IOUtils.copy(fis, zaos);
    zaos.closeArchiveEntry();
  }

  /**
   * Extract all files from Tar into the specified directory
   *
   * @param tarFile
   * @param directory
   *
   * @return the list of extracted filenames
   *
   * @throws IOException
   */
  public static List<String> unZip(File tarFile, File directory)
      throws IOException {
    final List<String> result = new ArrayList<String>();
    final InputStream inputStream = new FileInputStream(tarFile);
    final ZipArchiveInputStream in = new ZipArchiveInputStream(inputStream);
    try {
      ZipArchiveEntry entry = in.getNextZipEntry();
      while (entry != null) {
        if (entry.isDirectory()) {
          entry = in.getNextZipEntry();
          continue;
        }
        final File curfile = new File(directory, entry.getName());
        final File parent = curfile.getParentFile();
        if (!parent.exists()) {
          parent.mkdirs();
        }
        final OutputStream out = new FileOutputStream(curfile);
        try {
          IOUtils.copy(in, out);
        } finally {
          FileUtils.close(out);
        }
        result.add(entry.getName());
        entry = in.getNextZipEntry();
      }
    } finally {
      FileUtils.close(in);
    }
    return result;
  }

  public static void main(String[] args) {
    if (args.length < 3) {
      SysErrLogger.FAKE_LOGGER.syserr("You need to provide 3 arguments:\n" +
                                      "   option filedest.tar \"source\"\n" +
                                      "   where option=1 means unzip and source is a directory\n" +
                                      "   option=2 means zip and source is a directory\n" +
                                      "   option=3 means zip and source is a list of files comma separated");
      System.exit(1);//NOSONAR
    }
    final int option = Integer.parseInt(args[0]);
    final String tarfile = args[1];
    final String tarsource = args[2];
    String[] tarfiles;
    if (option == 3) {
      tarfiles = args[2].split(",");
      final File[] files = new File[tarfiles.length];
      for (int i = 0; i < tarfiles.length; i++) {
        files[i] = new File(tarfiles[i]);
      }
      if (createZipFromFiles(files, tarfile)) {
        SysErrLogger.FAKE_LOGGER.sysout("ZIP OK from multiple files");
      } else {
        SysErrLogger.FAKE_LOGGER.syserr("ZIP KO from multiple files");
      }
    } else if (option == 2) {
      if (createZipFromDirectory(tarsource, tarfile, false)) {
        SysErrLogger.FAKE_LOGGER.sysout("ZIP OK from directory");
      } else {
        SysErrLogger.FAKE_LOGGER.syserr("ZIP KO from directory");
      }
    } else if (option == 1) {
      final File tarFile = new File(tarfile);
      final File directory = new File(tarsource);
      List<String> result = null;
      try {
        result = unZip(tarFile, directory);
      } catch (final IOException e) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
      }
      if (result == null || result.isEmpty()) {
        SysErrLogger.FAKE_LOGGER.syserr("UNZIP KO from directory");
      } else {
        for (final String string : result) {
          SysErrLogger.FAKE_LOGGER.sysout("File: " + string);
        }
      }
    }

  }
}
