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
package org.waarp.administrator.guipwd;

import java.awt.event.ActionEvent;
import java.util.List;

import org.waarp.uip.WaarpUiPassword;

/**
 * Password Gui helper
 * 
 * @author Frederic Bregier
 *
 */
public class AdminUiPassword extends WaarpUiPassword {

    private static final long serialVersionUID = -7864989527339637852L;
    private List<AdminUiPassword> list;

    public AdminUiPassword(List<AdminUiPassword> list) throws Exception {
        super(true);
        this.list = list;
        this.list.add(this);
        myself = this;
    }

    @Override
    public void exit(ActionEvent evt) {
        ((AdminUiPassword) myself).list.remove(myself);
        super.exit(evt);
    }
}
