/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.common.file.passthrough;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply550Exception;
import org.waarp.common.command.exception.Reply553Exception;
import org.waarp.common.file.AbstractDir;
import org.waarp.common.file.FileInterface;
import org.waarp.common.file.OptsMLSxInterface;
import org.waarp.common.file.SessionInterface;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

/**
 * Directory implementation for Passthrough Based. It is just an empty shell since in pass through
 * mode, no directories or files really exist.
 * 
 * If one wants to implement special actions, he/she just has to extend this class and override the
 * default empty implementation.
 * 
 * @author Frederic Bregier
 * 
 */
public abstract class PassthroughBasedDirImpl extends AbstractDir {
    /**
     * Factory for PassthroughFile
     */
    protected static PassthroughFileFactory factory = null;

    /**
     * Passthrough object
     */
    protected PassthroughFile pdir = null;
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(PassthroughBasedDirImpl.class);

    /**
     * @param session
     * @param optsMLSx
     */
    public PassthroughBasedDirImpl(SessionInterface session,
            OptsMLSxInterface optsMLSx) {
        this.session = session;
        this.optsMLSx = optsMLSx;
        this.optsMLSx.setOptsModify((byte) -1);
        this.optsMLSx.setOptsPerm((byte) -1);
        this.optsMLSx.setOptsSize((byte) 1);
        this.optsMLSx.setOptsType((byte) 1);
        try {
            pdir = factory.create(null, "/");
        } catch (PassthroughException e) {
        }
    }

    /**
     * Finds all files matching a wildcard expression (based on '?', '~' or '*').
     * 
     * @param pathWithWildcard
     *            The wildcard expression with a business path.
     * @return List of String as relative paths matching the wildcard expression. Those files are
     *         tested as valid from business point of view. If Wildcard support is not active, if
     *         the path contains any wildcards, it will throw an error.
     * @throws CommandAbstractException
     */
    protected List<String> wildcardFiles(String pathWithWildcard)
            throws CommandAbstractException {
        List<String> resultPaths = new ArrayList<String>();
        // First check if pathWithWildcard contains wildcards
        if (!(pathWithWildcard.contains("*") || pathWithWildcard.contains("?") || pathWithWildcard
                .contains("~"))) {
            // No so simply return the list containing this path after
            // validating it
            if (getSession().getAuth().isBusinessPathValid(pathWithWildcard)) {
                resultPaths.add(pathWithWildcard);
            }
            return resultPaths;
        }
        // FIXME could support Wildcard path
        PassthroughFile file;
        try {
            file = factory.create(null, pathWithWildcard);
        } catch (PassthroughException e) {
            throw new Reply553Exception("Error while creating a wildcard PassthroughFile: " +
                    e.getMessage());
        }
        try {
            return file.wildcard(null);
        } catch (PassthroughException e) {
            throw new Reply553Exception("Error while getting a wildcard PassthroughFile: " +
                    e.getMessage());
        }

    }

    /**
     * Get the File from this path, checking first its validity
     * 
     * @param path
     * @return the FileInterface
     * @throws CommandAbstractException
     */
    protected File getFileFromPath(String path) throws CommandAbstractException {
        String newdir = validatePath(path);
        if (isAbsolute(newdir)) {
            return new File(newdir);
        }
        String truedir = ((PassthroughBasedAuthImpl) getSession().getAuth())
                .getAbsolutePath(newdir);
        return new File(truedir);
    }

    /**
     * Get the true file from the path
     * 
     * @param path
     * @return the true File from the path
     * @throws CommandAbstractException
     */
    protected File getTrueFile(String path) throws CommandAbstractException {
        checkIdentify();
        String newpath = consolidatePath(path);
        List<String> paths = wildcardFiles(normalizePath(newpath));
        if (paths.size() != 1) {
            throw new Reply550Exception("File not found: " + paths.size() +
                    " founds");
        }
        String extDir = paths.get(0);
        extDir = this.validatePath(extDir);
        File file = getFileFromPath(extDir);
        return file;
    }

    /**
     * Get the relative path (without mount point)
     * 
     * @param file
     * @return the relative path
     */
    protected String getRelativePath(File file) {
        return ((PassthroughBasedAuthImpl) getSession().getAuth())
                .getRelativePath(normalizePath(file.getAbsolutePath()));
    }

    public boolean changeDirectory(String path) throws CommandAbstractException {
        checkIdentify();
        String newpath = consolidatePath(path);
        List<String> paths = wildcardFiles(newpath);
        if (paths.size() != 1) {
            logger.warn("CD error: {}", newpath);
            throw new Reply550Exception("Directory not found: " + paths.size() +
                    " founds");
        }
        String extDir = paths.get(0);
        extDir = this.validatePath(extDir);
        if (isDirectory(extDir)) {
            try {
                pdir.changeDirectory(extDir);
            } catch (PassthroughException e) {
                throw new Reply550Exception("Directory not found");
            }
            currentDir = extDir;
            return true;
        }
        throw new Reply550Exception("Directory not found");
    }

    public String mkdir(String directory) throws CommandAbstractException {
        checkIdentify();
        String newdirectory = consolidatePath(directory);
        File dir = new File(newdirectory);
        String parent = dir.getParentFile().getPath();
        List<String> paths = wildcardFiles(normalizePath(parent));
        if (paths.size() != 1) {
            throw new Reply550Exception("Base Directory not found: " +
                    paths.size() + " founds");
        }
        String newDir = paths.get(0) + SEPARATOR + dir.getName();
        newDir = this.validatePath(newDir);
        PassthroughFile newdir;
        try {
            newdir = factory.create(null, newDir);
        } catch (PassthroughException e) {
            throw new Reply550Exception("Cannot create directory " + newDir);
        }
        try {
            if (newdir.mkdir()) {
                return newDir;
            }
        } catch (PassthroughException e) {
            throw new Reply550Exception("Cannot create directory " + newDir);
        }
        throw new Reply550Exception("Cannot create directory " + newDir);
    }

    public String rmdir(String directory) throws CommandAbstractException {
        checkIdentify();
        String newdirectory = consolidatePath(directory);
        List<String> paths = wildcardFiles(normalizePath(newdirectory));
        if (paths.size() != 1) {
            throw new Reply550Exception("Directory not found: " + paths.size() +
                    " founds");
        }
        String extDir = paths.get(0);
        extDir = this.validatePath(extDir);
        PassthroughFile dir;
        try {
            dir = factory.create(null, extDir);
        } catch (PassthroughException e) {
            throw new Reply550Exception("Cannot delete directory " + extDir);
        }
        try {
            if (dir.delete()) {
                return extDir;
            }
        } catch (PassthroughException e) {
            throw new Reply550Exception("Cannot delete directory " + extDir);
        }
        throw new Reply550Exception("Cannot delete directory " + extDir);
    }

    public boolean isDirectory(String path) throws CommandAbstractException {
        checkIdentify();
        PassthroughFile dir;
        try {
            dir = factory.create(pdir, path);
        } catch (PassthroughException e) {
            throw new Reply550Exception("Cannot get isDirectory " + path);
        }
        return dir.isDirectory();
    }

    public boolean isFile(String path) throws CommandAbstractException {
        checkIdentify();
        PassthroughFile file;
        try {
            file = factory.create(pdir, path);
        } catch (PassthroughException e) {
            throw new Reply550Exception("Cannot get File " + path);
        }
        return file.isFile();
    }

    public String getModificationTime(String path)
            throws CommandAbstractException {
        checkIdentify();
        PassthroughFile file;
        try {
            file = factory.create(pdir, path);
        } catch (PassthroughException e) {
            throw new Reply550Exception("Cannot get File " + path);
        }
        try {
            return file.getModificationTime();
        } catch (PassthroughException e) {
            throw new Reply550Exception("Cannot get ModificationTime " + path);
        }
    }

    public List<String> list(String path) throws CommandAbstractException {
        checkIdentify();
        PassthroughFile file;
        try {
            file = factory.create(pdir, path);
        } catch (PassthroughException e) {
            throw new Reply550Exception("Cannot get File " + path);
        }
        try {
            return file.list();
        } catch (PassthroughException e) {
            throw new Reply550Exception("List error " + e.getMessage());
        }
    }

    public List<String> listFull(String path, boolean lsFormat)
            throws CommandAbstractException {
        checkIdentify();
        PassthroughFile file;
        try {
            file = factory.create(pdir, path);
        } catch (PassthroughException e) {
            throw new Reply550Exception("Cannot get File " + path);
        }
        try {
            return file.listFull(lsFormat);
        } catch (PassthroughException e) {
            throw new Reply550Exception("List error " + e.getMessage());
        }
    }

    public String fileFull(String path, boolean lsFormat)
            throws CommandAbstractException {
        checkIdentify();
        PassthroughFile file;
        try {
            file = factory.create(pdir, path);
        } catch (PassthroughException e) {
            throw new Reply550Exception("Cannot get File " + path);
        }
        try {
            return file.fileFull(lsFormat);
        } catch (PassthroughException e) {
            throw new Reply550Exception("FileFull error " + e.getMessage());
        }
    }

    /**
     * Decide if Full time or partial time as in 'ls' command
     * 
     * @return True if Full Time, False is Default (as in 'ls' command)
     */
    protected boolean isFullTime() {
        // FIXME should be it the default ?
        return false;
    }

    public long getFreeSpace() throws CommandAbstractException {
        checkIdentify();
        try {
            return pdir.getFreeSpace();
        } catch (PassthroughException e) {
            throw new Reply550Exception("FileFull error " + e.getMessage());
        }
    }

    public FileInterface setUniqueFile()
            throws CommandAbstractException {
        checkIdentify();
        // FIXME file: create a virtual unique file
        String filename =
                getFileFromPath(currentDir) + SEPARATOR +
                        getSession().getAuth().getUser() +
                        Long.toHexString(System.currentTimeMillis()) +
                        this.session.getUniqueExtension();
        File file = new File(filename);
        String currentFile = getRelativePath(file);
        return newFile(normalizePath(currentFile), false);
    }

    public boolean canRead() throws CommandAbstractException {
        checkIdentify();
        return pdir.canRead();
    }

    public boolean canWrite() throws CommandAbstractException {
        checkIdentify();
        return pdir.canWrite();
    }

    public boolean exists() throws CommandAbstractException {
        checkIdentify();
        return pdir.exists();
    }

    public long getCRC(String path) throws CommandAbstractException {
        PassthroughFile file;
        try {
            file = factory.create(pdir, path);
        } catch (PassthroughException e) {
            throw new Reply550Exception("Cannot get File " + path);
        }
        try {
            return file.getCRC();
        } catch (PassthroughException e) {
            throw new Reply550Exception("CRC error " + e.getMessage());
        }
    }

    public byte[] getMD5(String path) throws CommandAbstractException {
        PassthroughFile file;
        try {
            file = factory.create(pdir, path);
        } catch (PassthroughException e) {
            throw new Reply550Exception("Cannot get File " + path);
        }
        try {
            return file.getMD5();
        } catch (PassthroughException e) {
            throw new Reply550Exception("CRC error " + e.getMessage());
        }
    }

    public byte[] getSHA1(String path) throws CommandAbstractException {
        PassthroughFile file;
        try {
            file = factory.create(pdir, path);
        } catch (PassthroughException e) {
            throw new Reply550Exception("Cannot get File " + path);
        }
        try {
            return file.getSHA1();
        } catch (PassthroughException e) {
            throw new Reply550Exception("CRC error " + e.getMessage());
        }
    }
}
