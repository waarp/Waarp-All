/*******************************************************************************
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 *  individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at your
 *  option) any later version.
 *
 *  Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 *  A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  Waarp . If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.waarp.openr66.protocol.junit;

import io.netty.channel.Channel;
import org.junit.Test;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;

import java.net.InetSocketAddress;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NetworkTransactionTest {

    @Test
    public void testisBlacklistedPreventNPE() {
        Channel chan = mock(Channel.class);
        when(chan.remoteAddress()).thenReturn(null);
        NetworkTransaction.isBlacklisted(chan);

        reset(chan);

        InetSocketAddress addr =
                new InetSocketAddress("cannotberesolved", 6666);
        assertNull(addr.getAddress());
        doReturn(addr).when(chan).remoteAddress();
        NetworkTransaction.isBlacklisted(chan);
    }
}
