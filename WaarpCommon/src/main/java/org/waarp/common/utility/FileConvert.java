/**
   This file is part of Waarp Project.

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All Waarp Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Waarp is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Waarp .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.common.utility;

/**
 * Description: Converts Unix files to Dos and vice versa
 * @author "Frederic Bregier"
 *
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;

public class FileConvert extends Thread {
    /**
     * Internal Logger
     */
    static volatile WaarpLogger logger;

    private boolean unix2dos = false;
    private boolean recursive = false;
    private File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    private List<File> files = null;

    /**
     * 
     * @param files
     *            list of files
     * @param unix2dos
     *            True for Unix2Dos, False for Dos2Unix
     * @param recursive
     *            True for Recursive from files, else unitary files
     * @param tmpDir
     *            if not null, specific tmp directory
     */
    public FileConvert(List<File> files, boolean unix2dos, boolean recursive, File tmpDir) {
        if (logger == null) {
            logger = WaarpLoggerFactory.getLogger(FileConvert.class);
        }
        this.files = files;
        this.unix2dos = unix2dos;
        this.recursive = recursive;
        if (tmpDir != null) {
            this.tmpDir = tmpDir;
        }
    }

    /**
     * Direct call
     * 
     * @param args
     */
    public static void main(String[] args) {
        WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
        if (logger == null) {
            logger = WaarpLoggerFactory.getLogger(FileConvert.class);
        }

        ArrayList<File> files = new ArrayList<File>();
        boolean unix2dos = false, dos2unix = false, recursive = false;
        File tmpDir = null;
        for (int i = 0; i < args.length; i++) {
            if (Pattern.compile("^-(u|-unix2dos|-unixtodos)$", Pattern.CASE_INSENSITIVE).matcher(args[i]).matches()) {
                unix2dos = true;
            } else if (Pattern.compile("^-(d|-dos2unix|-dostounix)$", Pattern.CASE_INSENSITIVE).matcher(args[i])
                    .matches()) {
                dos2unix = true;
            } else if (Pattern.compile("^-(r|-recursive)$", Pattern.CASE_INSENSITIVE).matcher(args[i]).matches()) {
                recursive = true;
            } else if (Pattern.compile("^-(t|-temporary)$", Pattern.CASE_INSENSITIVE).matcher(args[i]).matches()) {
                tmpDir = new File(args[++i]);
            } else {
                files.add(new File(args[i]));
            }
        }
        if ((unix2dos == true) && (dos2unix == true)) {
            syntax();
            System.exit(1);
        }
        if ((unix2dos == false) && (dos2unix == false)) {
            syntax();
            System.exit(1);
        }
        FileConvert fileConvert = new FileConvert(files, unix2dos, recursive, tmpDir);
        fileConvert.run();
    }

    /**
     * Use parameters given in instantiation
     */
    public void run() {
        if (files == null) {
            return;
        }
        for (Iterator<File> i = files.iterator(); i.hasNext();) {
            File file = i.next();
            if (file.isDirectory()) {
                if (recursive) {
                    recursive(file);
                }
            } else {
                convert(file, unix2dos);
            }
        }
    }

    private void recursive(File directory) {
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                recursive(files[i]);
            } else {
                convert(files[i], unix2dos);
            }
        }
    }

    private boolean copyFile(File source, File destination) {
        FileInputStream in = null;
        FileChannel srcChannel = null;
        FileOutputStream out = null;
        FileChannel dstChannel = null;
        try {
            in = new FileInputStream(source);
            srcChannel = in.getChannel();
            out = new FileOutputStream(destination);
            dstChannel = out.getChannel();
            long src = srcChannel.size();
            if (src < 0) {
                src = source.length();
            }
            long dst = 0;
            while (dst < src) {
                dst += dstChannel.transferFrom(srcChannel, dst, src);
            }
            return (src == dst);
        } catch (IOException e) {
            logger.error("FileConvert copy back in error", e);
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (srcChannel != null) {
                try {
                    srcChannel.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
            if (dstChannel != null) {
                try {
                    dstChannel.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Convert Unix2Dos or Dos2Unix file in place, according to second argument
     * 
     * @param input
     *            File to change
     * @param unix2dos
     *            True for UnixToDos, False for DosToUnix
     * @return True if OK
     */
    public boolean convert(File input, boolean unix2dos) {
        if (unix2dos) {
            logger.info("unix2Dos conversion of '" + input + "'... ");
        } else {
            logger.info("dos2Unix conversion of '" + input + "'... ");
        }
        FileInputStream fis = null;
        FileOutputStream fos = null;
        File tmpFile = null;
        try {
            fis = new FileInputStream(input);
            tmpFile = File.createTempFile("FileConevrt_" + input.getName(), null, tmpDir);
            fos = new FileOutputStream(tmpFile);
            if (unix2dos) {
                byte pb = -1;
                byte b = -1;
                while ((b = (byte) fis.read()) != -1) {
                    if ((b == 10) && (pb != 13)) {
                        fos.write((byte) 13);
                    }
                    fos.write(b);
                    pb = b;
                }
            } else {
                byte b = -1;
                byte nb = -1;
                while ((b = (byte) fis.read()) != -1) {
                    if (b == 13) {
                        nb = (byte) fis.read();
                        if (nb == -1) {
                            fos.write(b);
                        } else {
                            if (nb == 10) {
                                fos.write(nb);
                            } else {
                                fos.write(b);
                                fos.write(nb);
                            }
                        }
                    } else {
                        fos.write(b);
                    }
                }
            }
            try {
                fis.close();
            } catch (IOException e) {
            }
            fis = null;
            try {
                fos.close();
            } catch (IOException e) {
            }
            fos = null;
            boolean result = copyFile(tmpFile, input);
            tmpFile.delete();
            tmpFile = null;
            if (result) {
                logger.info("done.");
            } else {
                logger.error("FileConvert in error during final copy: " + input + ":" + unix2dos);
            }
            return result;
        } catch (FileNotFoundException e) {
            logger.error("FileConvert in error: " + input + ":" + unix2dos, e);
            return false;
        } catch (IOException e) {
            logger.error("FileConvert in error: " + input + ":" + unix2dos, e);
            return false;
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static void syntax() {
        logger.error("Syntax: Covnert -(u|-nix2dos|-unixtodos) | -(d|-dos2unix|-dostounix) -(t|-temporary) directory  -(r|-recursive) file directory");
    }

}