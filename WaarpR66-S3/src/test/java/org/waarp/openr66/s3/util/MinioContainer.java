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
package org.waarp.openr66.s3.util;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.Base58;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

/**
 * Inspired from https://github.com/alexkirnsu/minio-testcontainer
 */
public class MinioContainer extends GenericContainer<MinioContainer> {

  private static final int DEFAULT_PORT = 9000;
  private static final String DEFAULT_IMAGE = "minio/minio";

  private static final String MINIO_ACCESS_KEY = "MINIO_ACCESS_KEY";
  private static final String MINIO_SECRET_KEY = "MINIO_SECRET_KEY";

  private static final String DEFAULT_STORAGE_DIRECTORY = "/data";
  private static final String HEALTH_ENDPOINT = "/minio/health/ready";

  public MinioContainer(final CredentialsProvider credentials) {
    this(DEFAULT_IMAGE, credentials);
  }

  public MinioContainer(final String image,
                        final CredentialsProvider credentials) {
    super(image == null? DEFAULT_IMAGE : image);
    withNetworkAliases("minio-" + Base58.randomString(6));
    addExposedPort(DEFAULT_PORT);
    if (credentials != null) {
      withEnv(MINIO_ACCESS_KEY, credentials.getAccessKey());
      withEnv(MINIO_SECRET_KEY, credentials.getSecretKey());
    }
    withCommand("server", DEFAULT_STORAGE_DIRECTORY);
    setWaitStrategy(
        new HttpWaitStrategy().forPort(DEFAULT_PORT).forPath(HEALTH_ENDPOINT)
                              .withStartupTimeout(Duration.ofMinutes(2)));
  }

  public URL getURL() throws MalformedURLException {
    return new URL("http://" + getContainerIpAddress() + ":" +
                   getMappedPort(DEFAULT_PORT));
  }

  public static class CredentialsProvider {
    private final String accessKey;
    private final String secretKey;

    public CredentialsProvider(final String accessKey, final String secretKey) {
      this.accessKey = accessKey;
      this.secretKey = secretKey;
    }

    public String getAccessKey() {
      return accessKey;
    }

    public String getSecretKey() {
      return secretKey;
    }
  }
}
