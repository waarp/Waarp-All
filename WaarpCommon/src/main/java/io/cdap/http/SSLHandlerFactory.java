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

/*
 * Copyright © 2014-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.http;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

/**
 * A class that encapsulates SSL Certificate Information.
 */
public class SSLHandlerFactory {

  private final SslContext sslContext;
  private boolean needClientAuth;

  public SSLHandlerFactory(SSLConfig sslConfig) {
    String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
    if (algorithm == null) {
      algorithm = "“X509";
      try {
        if (KeyManagerFactory.getInstance(algorithm) == null) {
          algorithm = "SunX509";
        }
      } catch (NoSuchAlgorithmException e) {
        algorithm = "SunX509";
      }
    }
    try {
      final KeyStore ks =
          getKeyStore(sslConfig.getKeyStore(), sslConfig.getKeyStorePassword());
      // Set up key manager factory to use our key store
      final KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
      kmf.init(ks, sslConfig.getCertificatePassword() != null?
          sslConfig.getCertificatePassword().toCharArray() :
          sslConfig.getKeyStorePassword().toCharArray());

      final SslContextBuilder builder = SslContextBuilder.forServer(kmf);
      if (sslConfig.getTrustKeyStore() != null) {
        needClientAuth = true;
        final KeyStore tks = getKeyStore(sslConfig.getTrustKeyStore(),
                                         sslConfig.getTrustKeyStorePassword());
        final TrustManagerFactory tmf =
            TrustManagerFactory.getInstance(algorithm);
        tmf.init(tks);
        builder.trustManager(tmf);
      }

      sslContext = builder.build();
    } catch (final Exception e) {
      throw new IllegalArgumentException(
          "Failed to initialize the server-side SSLContext", e);
    }
  }

  public SSLHandlerFactory(SslContext sslContext) {
    this.sslContext = sslContext;
  }

  private static KeyStore getKeyStore(File keyStore, String keyStorePassword)
      throws Exception {
    InputStream is = null;
    try {
      is = new FileInputStream(keyStore);
      final KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(is, keyStorePassword.toCharArray());
      return ks;
    } finally {
      if (is != null) {
        is.close();
      }
    }
  }

  /**
   * Creates an SslHandler
   *
   * @param bufferAllocator the buffer allocator
   *
   * @return instance of {@code SslHandler}
   */
  public SslHandler create(ByteBufAllocator bufferAllocator) {
    final SSLEngine engine = sslContext.newEngine(bufferAllocator);
    engine.setNeedClientAuth(needClientAuth);
    engine.setUseClientMode(false);
    return new SslHandler(engine);
  }
}
