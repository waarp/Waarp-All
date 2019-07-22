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

/**
 * Classes implementing Data and Transfer status
 *
 * <br>
 * <br>
 * When a transfer should occur, here are the steps:<br>
 * <ul>
 * <li>The connection is prepared</li> through a PORT or PASV command. The PASV
 * command implies the creation
 * of a new binded address (connection from the client to the server for PASSIVE
 * mode) (or to reuse an already
 * opened binded address). Then it stores the session that will be used by this
 * new data session.<br>
 * The PORT command implies the preparation of the future data connection from
 * the server to the client
 * (ACTIVE mode). In this mode, the session does not need to be setup before
 * since it will be setup before the
 * real connection process.<br>
 * In both commands, if a previously data connection is already opened, it is
 * closed before any new startup.
 * <li>The transfer command is received</li> and is followed by several steps:
 * <ul>
 * <li>First it prepares the FtpFile object</li>
 * <li>It opens the data connection</li>. Two possibilities:<br>
 * 1) PASSIVE mode where the server waits for the client to initiate the real
 * connection.<br>
 * 2) ACTIVE mode where the server will initiate the data connection.<br>
 * The DataNetworkHandler will inform back the FtpTransferControl that the
 * connection is ready. Then the
 * FtpDataAsyncConn is also informed of this status.
 * <li>The transfer is initiated</li> by calling setNewFtpTransfer method of the
 * FtpTransferControl object.
 * This starts a new thread (FtpTransferExecutor) to execute the transfer.<br>
 * For STORE like operations, it waits for the end of the transfer from the data
 * network handler (when the
 * last block is received or the data connection is over).<br>
 * For LIST like commands, it immediately sends the result as wanted.<br>
 * For RETRIEVE like commands, it starts the sending of the file (trueRetrieve
 * method of the FtpFile
 * object).<br>
 * For STORE and RETRIEVE cases, it is waiting for the FtpTransferControl to
 * inform back that everything is
 * really over (waitForCommand). For each cases, when it is over, it informs
 * back the FtpTransferControl the
 * transfer is finished (setEndOfTransfer method). This enables to send early
 * end message (success or error)
 * and also final message (again success or error) after a check is
 * completed.<br>
 * </ul>
 * When the transfer command is received, it does not wait for the real transfer
 * to be done (since it is done
 * in a new thread), so to allow an immediate feedback as a partial success
 * status as specified in the RFCs.
 * </ul>
 */
package org.waarp.ftp.core.data;
