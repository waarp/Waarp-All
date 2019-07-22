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
 * Classes implementing SSL support for Netty
 * <p>
 * To generate the stores for Waarp for instance, you need to create 2 JKS
 * keyStore. To generate those files,
 * you can use the "keytool" command from the JDK or using the free tool
 * KeyTool
 * IUI (last known version in
 * 2.4.1).<br>
 * <br>
 * <p>
 * See Certificate-Howto.txt file<br>
 * <br>
 * <p>
 * Usage:<br>
 * In order to use the SSL support, here are the different steps.<br>
 * <br>
 *
 * <b>On Client side:</b><br>
 * <ul>
 * <li>Create the KeyStore for the Client<br>
 * <b>For no client authentication:</b><br>
 * ggSecureKeyStore = new WaarpSecureKeyStore(keyStorePasswd, keyPasswd);<br>
 * <b>For client authentication:</b><br>
 * ggSecureKeyStore = new WaarpSecureKeyStore(keyStoreFilename, keyStorePasswd,
 * keyPasswd);</li>
 * <li>Create the TrustStore for the Client<br>
 * <b>For Trusting everyone:</b><br>
 * ggSecureKeyStore.initEmptyTrustStore(keyTrustStorePasswd);<br>
 * <b>For Trusting only known Certificates:</b><br>
 * ggSecureKeyStore.initTrustStore(keyTrustStoreFilename, keyTrustStorePasswd,
 * needClientAuthent);<br>
 * Note: needClientAuthent is True if the TrustStore is used to authenticate
 * Clients, False if only to
 * authenticate Servers</li>
 * <li>Create the WaarpSslContextFactory:<br>
 * WaarpSslContextFactory ggSslContextFactory = new WaarpSslContextFactory(ggSecureKeyStore,
 * <b>false</b>);</li>
 * <li>Create your own Initializer:<br>
 * As first item in the pipeline, add:<br>
 * pipeline.addLast("ssl", ggSslContextFactory.initInitializer(<b>false</b>,
 * ggSslContextFactory.hasTrustStore(), executor));<br>
 * where executor is generally a Executors.newCachedThreadPool();<br>
 * <br>
 * <p>
 * For example, see Waarp Local Exec module using SSL:<br>
 * localExecClientInitializer = new LocalExecSslClientInitializer(ggSslContextFactory);<br>
 * bootstrap.setInitializer(localExecClientInitializer);</li>
 * <li>In the final Handler, you need to add the handshake:<br>
 * public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent
 * e)<br>
 * throws Exception {<br>
 * ...<br>
 * SslHandler sslHandler = ctx.pipeline().get(SslHandler.class);<br>
 * // Begin handshake<br>
 * ChannelFuture handshakeFuture = sslHandler.handshake();<br>
 * handshakeFuture.addListener(new ChannelFutureListener() {<br>
 * public void operationComplete(ChannelFuture future)<br>
 * throws Exception {<br>
 * if (future.isSuccess()) {<br>
 * //OK<br>
 * } else {<br>
 * future.channel().close();<br>
 * }<br>
 * }<br>
 * });<br>
 * }</li>
 * <li>At the end of your connection, you need to release the Executor passes
 * as
 * argument to
 * ggSslContextFactory.initInitializer</li>
 * </ul>
 * <br>
 * <br>
 *
 * <b>On Server side:</b><br>
 * <ul>
 * <li>Create the KeyStore for the Server<br>
 * ggSecureKeyStore = new WaarpSecureKeyStore(keyStoreFilename, keyStorePasswd,
 * keyPasswd);</li>
 * <li>Create the TrustStore for the Client<br>
 * <b>For Trusting everyone:</b><br>
 * ggSecureKeyStore.initEmptyTrustStore(keyTrustStorePasswd);<br>
 * <b>For Trusting only known Certificates:</b><br>
 * ggSecureKeyStore.initTrustStore(keyTrustStoreFilename, keyTrustStorePasswd,
 * needClientAuthent);<br>
 * Note: needClientAuthent is True if the TrustStore is used to authenticate
 * Clients, False if only to
 * authenticate Servers
 * <li>
 * <li>Create the WaarpSslContextFactory:<br>
 * WaarpSslContextFactory ggSslContextFactory = new WaarpSslContextFactory(ggSecureKeyStore,
 * <b>true</b>);</li>
 * <li>Create your own Initializer:<br>
 * As first item in the pipeline, add:<br>
 * pipeline.addLast("ssl", ggSslContextFactory.initInitializer(<b>true</b>,
 * ggSslContextFactory.hasTrustStore(), executor));<br>
 * where executor is generally a Executors.newCachedThreadPool();<br>
 * <br>
 * <p>
 * For example, see Waarp Local Exec module using SSL:<br>
 * bootstrap.setInitializer(new LocalExecSslServerInitializer(ggSslContextFactory,
 * delay));</li>
 * <li>In the final Handler, you need to add the handshake:<br>
 * public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent
 * e)<br>
 * throws Exception {<br>
 * ...<br>
 * SslHandler sslHandler = ctx.pipeline().get(SslHandler.class);<br>
 * // Begin handshake<br>
 * ChannelFuture handshakeFuture = sslHandler.handshake();<br>
 * handshakeFuture.addListener(new ChannelFutureListener() {<br>
 * public void operationComplete(ChannelFuture future)<br>
 * throws Exception {<br>
 * if (future.isSuccess()) {<br>
 * //OK<br>
 * } else {<br>
 * future.channel().close();<br>
 * }<br>
 * }<br>
 * });<br>
 * }</li>
 * <li>At the end of your connection, you need to release the Executor passes
 * as
 * argument to
 * ggSslContextFactory.initInitializer</li>
 * </ul>
 */
package org.waarp.common.crypto.ssl;