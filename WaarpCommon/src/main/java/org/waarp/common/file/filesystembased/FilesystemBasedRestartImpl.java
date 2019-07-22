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
package org.waarp.common.file.filesystembased;

import org.waarp.common.exception.NoRestartException;
import org.waarp.common.file.Restart;
import org.waarp.common.file.SessionInterface;

/**
 * Restart implementation for Filesystem Based
 * 
 * @author Frederic Bregier
 * 
 */
public abstract class FilesystemBasedRestartImpl extends Restart {
    /**
     * Valid Position for the next current file
     */
    protected long position = -1;
    protected int limit = -1;

    /**
     * @param session
     */
    public FilesystemBasedRestartImpl(SessionInterface session) {
        super(session);
    }

    @Override
    public long getPosition() throws NoRestartException {
        if (isSet()) {
            setSet(false);
            return position;
        }
        throw new NoRestartException("Restart is not set");
    }

    @Override
    public int getMaxSize(int nextBlock) {
        if (limit > 0) {
            if (nextBlock > limit) {
                nextBlock = limit;
            }
            limit -= nextBlock;
            return nextBlock;
        } else if (limit == 0) {
            limit = -1;
            return 0;
        }
        return nextBlock;
    }

    @Override
    public void setSet(boolean isSet) {
        super.setSet(isSet);
        limit = -1;
    }

}
