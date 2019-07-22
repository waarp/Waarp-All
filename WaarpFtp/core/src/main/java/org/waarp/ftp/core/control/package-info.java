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
 * Classes implementing Control connections.
 *
 * <br>
 * <br>
 * The internal logic is the following:<br>
 * <ul>
 * <li>When a connection is opened for control network:</li> It first creates
 * the default startup command
 * (ConnectionCommand), then it answers it is ok to accept identification (which
 * is implied by
 * ConnectionCommand).
 * <li>Each time a command is received:</li>
 * <ul>
 * <li>Parsing the command</li> in order to find the corresponding class that
 * implements it.
 * <li>Checking if the command is legal now</li> such that no transfer is
 * currently running except if is a
 * special command (like QUIT or ABORT).
 * <li>Checking if the command is legal from workflow</li> that is to say the
 * previous command allows the
 * usage of the current command (for instance, no transfer command is allowed if
 * the authentication is not
 * finished).
 * <li>Running the command</li> with executing a pre and post operation on
 * business handler.
 * <li>Making the final answer of the command</li> (in some cases this is a
 * partial answer like ready to
 * transfer)
 * </ul>
 * <li>When an exception occurs</li> the connection will be closed.
 * <li>When the connection is closed</li> all attributes are cleaned.
 * </ul>
 */
package org.waarp.ftp.core.control;
