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
package org.waarp.gateway.ftp.service;

import org.waarp.common.service.EngineAbstract;
import org.waarp.common.service.ServiceLauncher;

/**
 * @author Frederic Bregier
 *
 */
public class FtpServiceLauncher extends ServiceLauncher {

    public static void main(String[] args) {
        _main(args);
    }

    public static void windowsService(String args[]) throws Exception {
        _windowsService(args);
    }

    public static void windowsStart(String args[]) throws Exception {
        _windowsStart(args);
    }

    public static void windowsStop(String args[]) {
        _windowsStop(args);
    }

    /**
	 * 
	 */
    public FtpServiceLauncher() {
        super();
    }

    @Override
    protected EngineAbstract getNewEngineAbstract() {
        return new FtpEngine();
    }

}
