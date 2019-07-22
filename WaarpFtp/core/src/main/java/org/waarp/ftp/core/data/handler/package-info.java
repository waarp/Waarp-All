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
 * Classes implementing Data connections
 *
 * <br>
 * <br>
 * The internal logic is the following:<br>
 * <ul>
 * <li>When a connection is opened for data network:</li> It first tries to find
 * the corresponding session
 * setup from the control connection. Then it setups the FTP codec. Finally it
 * informs the FtpTransferControl
 * object that the data connection is opened and ready, such that the transfer
 * can start correctly.
 * <li>Each time a block is received:</li> the DataBlock is written into the
 * corresponding FtpFile according
 * to the status.
 * <li>If the operation is a retrieve</li> it writes the file to the data
 * channel (from FtpTransferControl)
 * and wake up the process of writing from channelInterestChanged in order to
 * prevent OOME.
 * <li>When an exception occurs</li> the data connection will be closed so as
 * the current transfer action
 * through the FtpTransferControl Object.
 * <li>When the connection is closed</li> the process tries to unbind if
 * necessary the parent connection (no
 * more connections will use this binded address) and then cleans all
 * attributes.
 * </ul>
 */
package org.waarp.ftp.core.data.handler;
