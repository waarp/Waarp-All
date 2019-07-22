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
package org.waarp.openr66.protocol.localhandler.packet.json;

import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;

/**
 * Exporting Configuration response JSON packet
 * 
 * @author "Frederic Bregier"
 *
 */
public class ConfigExportResponseJsonPacket extends ConfigExportJsonPacket {

    protected byte command;
    protected String filehost, filerule, filebusiness, filealias, fileroles;

    /**
     * @return the command
     */
    public byte getCommand() {
        return command;
    }

    /**
     * @param command
     *            the command to set
     */
    public void setCommand(byte command) {
        this.command = command;
    }

    /**
     * @return the filehost
     */
    public String getFilehost() {
        return filehost;
    }

    /**
     * @param filehost
     *            the filehost to set
     */
    public void setFilehost(String filehost) {
        this.filehost = filehost;
    }

    /**
     * @return the filerule
     */
    public String getFilerule() {
        return filerule;
    }

    /**
     * @param filerule
     *            the filerule to set
     */
    public void setFilerule(String filerule) {
        this.filerule = filerule;
    }

    /**
     * @return the filebusiness
     */
    public String getFilebusiness() {
        return filebusiness;
    }

    /**
     * @param filebusiness
     *            the filebusiness to set
     */
    public void setFilebusiness(String filebusiness) {
        this.filebusiness = filebusiness;
    }

    /**
     * @return the filealias
     */
    public String getFilealias() {
        return filealias;
    }

    /**
     * @param filealias
     *            the filealias to set
     */
    public void setFilealias(String filealias) {
        this.filealias = filealias;
    }

    /**
     * @return the fileroles
     */
    public String getFileroles() {
        return fileroles;
    }

    /**
     * @param fileroles
     *            the fileroles to set
     */
    public void setFileroles(String fileroles) {
        this.fileroles = fileroles;
    }

    public void setRequestUserPacket() {
        super.setRequestUserPacket(LocalPacketFactory.CONFEXPORTPACKET);
    }
}
