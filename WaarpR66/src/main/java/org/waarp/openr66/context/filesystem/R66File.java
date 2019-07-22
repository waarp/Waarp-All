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
package org.waarp.openr66.context.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.channel.ChannelFuture;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.exception.FileEndOfTransferException;
import org.waarp.common.exception.FileTransferException;
import org.waarp.common.file.DataBlock;
import org.waarp.common.file.filesystembased.FilesystemBasedDirImpl;
import org.waarp.common.file.filesystembased.FilesystemBasedFileImpl;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.RetrieveRunner;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.FileUtils;

/**
 * File representation
 * 
 * @author frederic bregier
 * 
 */
public class R66File extends FilesystemBasedFileImpl {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(R66File.class);

    /**
     * Does the current file is external (i.e. out of R66 base directory)
     */
    private boolean isExternal = false;

    /**
     * @param session
     * @param dir
     * @param path
     * @param append
     * @throws CommandAbstractException
     */
    public R66File(R66Session session, R66Dir dir, String path, boolean append)
            throws CommandAbstractException {
        super(session, dir, path, append);
    }

    /**
     * This constructor is for External file
     * 
     * @param session
     * @param dir
     * @param path
     */
    public R66File(R66Session session, R66Dir dir, String path) {
        super(session, dir, path);
        isExternal = true;
    }

    /**
     * Start the retrieve (send to the remote host the local file)
     * 
     * @param running
     *            When false, should stop the runner
     * @throws OpenR66RunnerErrorException
     * @throws OpenR66ProtocolSystemException
     */
    public void retrieveBlocking(AtomicBoolean running) throws OpenR66RunnerErrorException,
            OpenR66ProtocolSystemException {
        boolean retrieveDone = false;
        LocalChannelReference localChannelReference = getSession()
                .getLocalChannelReference();
        FilesystemBasedDigest digest = null;
        logger.debug("File to retrieve: " + this.toString());
        try {
            if (!isReady) {
                return;
            }
            DataBlock block = null;
            try {
                block = readDataBlock();
            } catch (FileEndOfTransferException e) {
                // Last block (in fact, no data to read)
                retrieveDone = true;
                return;
            }
            if (block == null) {
                // Last block (in fact, no data to read)
                retrieveDone = true;
                return;
            }
            if (Configuration.configuration.isGlobalDigest()) {
                try {
                    digest = new FilesystemBasedDigest(Configuration.configuration.getDigest());
                } catch (NoSuchAlgorithmException e2) {
                    // ignore
                }
            }
            ChannelFuture future1 = null, future2 = null;
            if ((block != null && (running.get()))) {
                block.getBlock().retain();
                future1 = RetrieveRunner.writeWhenPossible(
                        block, localChannelReference);
                if (Configuration.configuration.isGlobalDigest()) {
                    FileUtils.computeGlobalHash(digest, block.getBlock());
                }
            }
            // While not last block
            while (block != null && (!block.isEOF())) {
                try {
                    future1.await();
                } catch (InterruptedException e) {
                }
                if (!future1.isSuccess()) {
                    return;
                }
                if (!running.get()) {
                    return;
                }
                try {
                    block = readDataBlock();
                } catch (FileEndOfTransferException e) {
                    // Wait for last write
                    try {
                        future1.await();
                    } catch (InterruptedException e1) {
                    }
                    if (future1.isSuccess()) {
                        retrieveDone = true;
                    }
                    return;
                }
                block.getBlock().retain();
                future2 = RetrieveRunner.writeWhenPossible(
                        block, localChannelReference);
                if (Configuration.configuration.isGlobalDigest()) {
                    FileUtils.computeGlobalHash(digest, block.getBlock());
                }
                future1 = future2;
            }
            if (!running.get()) {
                // stopped
                return;
            }
            // Wait for last write
            if (future1 != null) {
                try {
                    future1.await();
                } catch (InterruptedException e) {
                }
                if (!future1.isSuccess()) {
                    return;
                }
            }
            if (block != null) {
                block.getBlock().release();
                block.clear();
            }
            retrieveDone = true;
            return;
        } catch (FileTransferException e) {
            // An error occurs!
            getSession()
                    .setFinalizeTransfer(
                            false,
                            new R66Result(new OpenR66ProtocolSystemException(e),
                                    getSession(), false, ErrorCode.TransferError, getSession()
                                            .getRunner()));
        } catch (OpenR66ProtocolPacketException e) {
            // An error occurs!
            getSession()
                    .setFinalizeTransfer(
                            false,
                            new R66Result(e, getSession(), false,
                                    ErrorCode.Internal, getSession().getRunner()));
        } finally {
            if (retrieveDone) {
                String hash = null;
                if (digest != null) {
                    hash = FilesystemBasedDigest.getHex(digest.Final());
                }
                try {
                    if (hash == null) {
                        ChannelUtils.writeEndTransfer(localChannelReference);
                    } else {
                        ChannelUtils.writeEndTransfer(localChannelReference, hash);
                    }
                } catch (OpenR66ProtocolPacketException e) {
                    // An error occurs!
                    getSession().setFinalizeTransfer(
                            false,
                            new R66Result(e, getSession(), false,
                                    ErrorCode.Internal, getSession().getRunner()));
                }
            } else {
                // An error occurs!
                getSession().setFinalizeTransfer(
                        false,
                        new R66Result(new OpenR66ProtocolSystemException("Transfer in error"),
                                getSession(), false, ErrorCode.TransferError, getSession()
                                        .getRunner()));
            }
        }
    }

    /**
     * This method is a good to have in a true FileInterface implementation.
     * 
     * @return the File associated with the current FileInterface operation
     */
    public File getTrueFile() {
        if (isExternal) {
            return new File(currentFile);
        }
        try {
            return getFileFromPath(getFile());
        } catch (CommandAbstractException e) {
            logger.warn("Exception while getting file: " + this, e);
            return null;
        }
    }

    /**
     * 
     * @return the basename of the current file
     */
    public String getBasename() {
        return getBasename(currentFile);
    }

    /**
     * 
     * @param path
     * @return the basename from the given path
     */
    public static String getBasename(String path) {
        int pos = path.lastIndexOf('/');
        int pos2 = path.lastIndexOf('\\');
        if (pos2 > pos) {
            pos = pos2;
        }
        if (pos > 0) {
            return path.substring(pos + 1);
        }
        return path;
    }

    @Override
    public R66Session getSession() {
        return (R66Session) session;
    }

    @Override
    public boolean canRead() throws CommandAbstractException {
        if (isExternal) {
            File file = new File(currentFile);
            logger.debug("Final File: " + file + " CanRead: " + file.canRead());
            return file.canRead();
        }
        return super.canRead();
    }

    @Override
    public boolean canWrite() throws CommandAbstractException {
        if (isExternal) {
            File file = new File(currentFile);
            if (file.exists()) {
                return file.canWrite();
            }
            return file.getParentFile().canWrite();
        }
        return super.canWrite();
    }

    @Override
    public boolean delete() throws CommandAbstractException {
        if (isExternal) {
            File file = new File(currentFile);
            checkIdentify();
            if (!isReady) {
                return false;
            }
            if (!file.exists()) {
                return true;
            }
            closeFile();
            return file.delete();
        }
        return super.delete();
    }

    @Override
    public boolean exists() throws CommandAbstractException {
        if (isExternal) {
            File file = new File(currentFile);
            return file.exists();
        }
        return super.exists();
    }

    @Override
    protected FileChannel getFileChannel() {
        if (!isExternal) {
            return super.getFileChannel();
        }
        if (!isReady) {
            return null;
        }
        File trueFile = getTrueFile();
        FileChannel fileChannel;
        try {
            @SuppressWarnings("resource")
            FileInputStream fileInputStream = new FileInputStream(trueFile);
            fileChannel = fileInputStream.getChannel();
            if (getPosition() > 0) {
                fileChannel = fileChannel.position(getPosition());
            }
        } catch (FileNotFoundException e) {
            logger.error("FileInterface not found in getFileChannel:", e);
            return null;
        } catch (IOException e) {
            logger.error("Change position in getFileChannel:", e);
            return null;
        }
        return fileChannel;
    }

    @Override
    protected RandomAccessFile getRandomFile() {
        if (!isExternal) {
            return super.getRandomFile();
        }
        if (!isReady) {
            return null;
        }
        File trueFile = getTrueFile();
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(trueFile, "rw");
            raf.seek(getPosition());
        } catch (FileNotFoundException e) {
            logger.error("File not found in getRandomFile:", e);
            return null;
        } catch (IOException e) {
            logger.error("Change position in getRandomFile:", e);
            return null;
        }
        return raf;
    }

    /**
     * Returns the FileOutputStream in Out mode associated with the current file.
     * 
     * @param append
     *            True if the FileOutputStream should be in append mode
     * @return the FileOutputStream (OUT)
     */
    protected FileOutputStream getFileOutputStream(boolean append) {
        if (!isExternal) {
            return super.getFileOutputStream(append);
        }
        if (!isReady) {
            return null;
        }
        File trueFile = getTrueFile();
        if (getPosition() > 0) {
            if (trueFile.length() < getPosition()) {
                logger.error("Cannot Change position in getFileOutputStream: file is smaller than required position");
                return null;
            }
            RandomAccessFile raf = getRandomFile();
            try {
                raf.setLength(getPosition());
                raf.close();
            } catch (IOException e) {
                logger.error("Change position in getFileOutputStream:", e);
                return null;
            }
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(trueFile, append);
        } catch (FileNotFoundException e) {
            logger.error("File not found in getRandomFile:", e);
            return null;
        }
        return fos;
    }

    @Override
    public boolean isDirectory() throws CommandAbstractException {
        if (isExternal) {
            File dir = new File(currentFile);
            return dir.isDirectory();
        }
        return super.isDirectory();
    }

    @Override
    public boolean isFile() throws CommandAbstractException {
        if (isExternal) {
            File file = new File(currentFile);
            return file.isFile();
        }
        return super.isFile();
    }

    @Override
    public long length() throws CommandAbstractException {
        if (isExternal) {
            File file = new File(currentFile);
            if (file.canRead()) {
                return file.length();
            } else {
                return -1;
            }
        }
        return super.length();
    }

    protected String getFullInDir() {
        DbTaskRunner runner = getSession().getRunner();
        if (runner != null) {
            R66Dir dir = new R66Dir(getSession());
            try {
                dir.changeDirectory(runner.getRule().getRecvPath());
                return dir.getFullPath();
            } catch (CommandAbstractException e) {
            }
        }
        return null;
    }

    @Override
    public boolean renameTo(String path) throws CommandAbstractException {
        if (!isExternal) {
            return super.renameTo(path);
        }
        checkIdentify();
        if (!isReady) {
            logger.warn("File not ready: {}", this);
            return false;
        }
        File file = getTrueFile();
        if (file.canRead()) {
            File newFile = getFileFromPath(path);
            File parentFile = newFile.getParentFile();
            if (parentFile == null) {
                String dir = getFullInDir();
                if (dir != null) {
                    newFile = new File(dir, newFile.getPath());
                    parentFile = newFile.getParentFile();
                }
            }
            if (newFile.exists()) {
                logger.warn("Target file already exists: "+newFile.getAbsolutePath());
                return false;
            }
            if (newFile.getAbsolutePath().equals(file.getAbsolutePath())) {
                // already in the right position
                isReady = true;
                return true;
            }
            if (parentFile != null && parentFile.canWrite()) {
                if (!file.renameTo(newFile)) {
                    FileOutputStream fileOutputStream = null;
                    try {
                        try {
                            fileOutputStream = new FileOutputStream(newFile);
                        } catch (FileNotFoundException e) {
                            logger
                                    .warn("Cannot find file: " + newFile.getName(),
                                            e);
                            return false;
                        }
                        FileChannel fileChannelOut = fileOutputStream.getChannel();
                        if (get(fileChannelOut)) {
                            delete();
                        } else {
                            try {
                                fileChannelOut.close();
                            } catch (IOException e) {
                            }
                            logger.error("Cannot write file: {}", newFile);
                            return false;
                        }
                    } finally {
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                }
                currentFile = getRelativePath(newFile);
                isExternal = false;
                isReady = true;
                logger.debug("File renamed to: {} and real position: {}", this, newFile);
                return true;
            }
        }
        logger.warn("Cannot read file: {}", file);
        return false;
    }

    /**
     * Move the current file to the path as destination
     * 
     * @param path
     * @param external
     *            if True, the path is outside authentication control
     * @return True if the operation is done
     * @throws CommandAbstractException
     */
    public boolean renameTo(String path, boolean external)
            throws CommandAbstractException {
        if (!external) {
            return renameTo(path);
        }
        checkIdentify();
        if (!isReady) {
            return false;
        }
        File file = getTrueFile();
        if (file.canRead()) {
            File newFile = new File(path);
            File parentFile = newFile.getParentFile();
            if (parentFile == null) {
                String dir = getFullInDir();
                if (dir != null) {
                    newFile = new File(dir, newFile.getPath());
                    parentFile = newFile.getParentFile();
                }
            }
            if (newFile.exists()) {
                logger.warn("Target file already exists: "+newFile.getAbsolutePath());
                return false;
            }
            if (newFile.getAbsolutePath().equals(file.getAbsolutePath())) {
                // already in the right position
                isReady = true;
                return true;
            }
            if (parentFile != null && parentFile.canWrite()) {
                if (!file.renameTo(newFile)) {
                    FileOutputStream fileOutputStream = null;
                    try {
                        try {
                            fileOutputStream = new FileOutputStream(newFile);
                        } catch (FileNotFoundException e) {
                            logger
                                    .warn("Cannot find file: " + newFile.getName(),
                                            e);
                            return false;
                        }
                        FileChannel fileChannelOut = fileOutputStream.getChannel();
                        if (get(fileChannelOut)) {
                            delete();
                        } else {
                            try {
                                fileChannelOut.close();
                            } catch (IOException e) {
                            }
                            logger.error("Cannot write file: {}", newFile);
                            return false;
                        }
                    } finally {
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                }
                currentFile = FilesystemBasedDirImpl.normalizePath(newFile
                        .getAbsolutePath());
                isExternal = true;
                isReady = true;
                return true;
            }
            logger.error("Cannot write to parent directory: {}", newFile.getParent());
        }
        logger.error("Cannot read file: {}", file);
        return false;
    }

    /**
     * Replace the current file with the new filename after closing the previous one.
     * 
     * @param filename
     * @param isExternal
     * @throws CommandAbstractException
     */
    public void replaceFilename(String filename, boolean isExternal)
            throws CommandAbstractException {
        closeFile();
        currentFile = filename;
        this.isExternal = isExternal;
        isReady = true;
    }

    @Override
    public boolean closeFile() throws CommandAbstractException {
        boolean status = super.closeFile();
        // FORCE re-open file
        isReady = true;
        return status;
    }

    /**
     * 
     * @return True if this file is outside OpenR66 Base directory
     */
    public boolean isExternal() {
        return isExternal;
    }

    @Override
    public String toString() {
        return "File: " + currentFile + " Ready " + isReady + " isExternal " + isExternal + " " +
                getPosition();
    }
}
