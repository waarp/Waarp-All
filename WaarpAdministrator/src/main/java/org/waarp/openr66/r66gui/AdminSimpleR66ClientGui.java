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
package org.waarp.openr66.r66gui;

import org.waarp.administrator.AdminGui;

/**
 * R66 Simple client GUI
 * 
 * @author Frederic Bregier
 *
 */
public class AdminSimpleR66ClientGui extends R66ClientGui {

    /**
     * Create the application.
     */
    public AdminSimpleR66ClientGui() {
        super();
        getEnvironment().networkTransaction = AdminGui.getEnvironnement().networkTransaction;
        extended = true;
        initialize();
    }

}
