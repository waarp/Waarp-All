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
package org.waarp.openr66.s3;

import io.netty.util.ResourceLeakDetector;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNetworkException;
import org.waarp.openr66.s3.util.MinioContainer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;

public class WaarpR66S3ClientTest {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpR66S3ClientTest.class);

  private static final String ACCESS_KEY = "accessKey";
  private static final String SECRET_KEY = "secretKey";
  private static final String BUCKET = "bucket-test";
  private static final String FILEPATH11 = "/directory1/file1";
  private static final String FILEPATH12 = "/directory1/file2";
  private static final String FILEPATH21 = "/directory2/file1";
  private static final String FILEPATH0 = "/zzz/file0";

  private WaarpR66S3Client waarpR66S3Client;

  @BeforeClass
  public static void beforeClass() {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
  }

  @After
  public void shutDown() {
    if (waarpR66S3Client != null) {
      waarpR66S3Client = null;
    }
  }

  private File createTestFile() throws IOException {
    final File file = File.createTempFile("testSend", ".txt");
    final byte[] bytes = new byte[1024];
    Arrays.fill(bytes, (byte) 1);
    try (final FileOutputStream outputStream = new FileOutputStream(file)) {
      outputStream.write(bytes);
      outputStream.flush();
    }
    return file;
  }

  @Test
  public void testWaarpR66S3Client() {
    try (final MinioContainer container = new MinioContainer(
        new MinioContainer.CredentialsProvider(ACCESS_KEY, SECRET_KEY))) {
      container.start();
      logger.warn("{} with {} : {}", container.getURL(), ACCESS_KEY,
                  SECRET_KEY);
      waarpR66S3Client =
          new WaarpR66S3Client(ACCESS_KEY, SECRET_KEY, container.getURL());
      final File test = createTestFile();
      // Create files
      final String versionId11 =
          waarpR66S3Client.createFile(BUCKET, FILEPATH11, test, null);
      logger.warn("Creation 11 {}", versionId11);
      final Map<String, String> map = new HashMap<>();
      map.put("Tag1", "Value1");
      map.put("Tag2", "Value2");
      final String versionId12 =
          waarpR66S3Client.createFile(BUCKET, FILEPATH12, test, map);
      logger.warn("Creation 12 {}", versionId12);
      final String versionId21 =
          waarpR66S3Client.createFile(BUCKET, FILEPATH21, test, map);
      logger.warn("Creation 21 {}", versionId21);
      final String versionId0 =
          waarpR66S3Client.createFile(BUCKET, FILEPATH0, test, null);
      logger.warn("Creation 0 {}", versionId0);

      // listObjects
      Iterator<String> iterator =
          waarpR66S3Client.listObjectsFromBucket(BUCKET, null, true, 0);
      int count = 0;
      while (iterator.hasNext()) {
        logger.warn("Find {}", iterator.next());
        count++;
      }
      logger.warn("List all: {}", count);
      assertEquals(4, count);
      iterator = waarpR66S3Client.listObjectsFromBucket(BUCKET, "di", true, 0);
      count = 0;
      while (iterator.hasNext()) {
        iterator.next();
        count++;
      }
      logger.warn("List from di: {}", count);
      assertEquals(3, count);
      iterator = waarpR66S3Client.listObjectsFromBucket(BUCKET, "di", true, 2);
      count = 0;
      while (iterator.hasNext()) {
        iterator.next();
        count++;
      }
      logger.warn("List from di limit 2: {} but shall be 2", count);
      // From https://github.com/minio/minio-java/issues/1057 the limit (maxKeys) seems to define how many entries to be fetched on a request.
      // From what I understand each iteration performed using the iterator trigger a new request leading to 3 results instead of 2 define in the limit.
      assertEquals(3, count);
      // Only Directory listing at root
      iterator = waarpR66S3Client.listObjectsFromBucket(BUCKET, null, false, 0);
      count = 0;
      while (iterator.hasNext()) {
        iterator.next();
        count++;
      }
      logger.warn("List limit 0=default limit: {}", count);
      assertEquals(3, count);

      // Get Files
      final File file = File.createTempFile("testRecv", ".txt");
      Map<String, String> map2 =
          waarpR66S3Client.getFile(BUCKET, FILEPATH11, file, true);
      logger.warn("Get File: {}", file.length());
      assertEquals(0, map2.size());
      assertEquals(test.length(), file.length());
      file.delete();
      map2 = waarpR66S3Client.getFile(BUCKET, FILEPATH12, file, true);
      logger.warn("Get File: {}", file.length());
      assertEquals(2, map2.size());
      assertEquals(test.length(), file.length());
      file.delete();
      map2 = waarpR66S3Client.getFile(BUCKET, FILEPATH12, file, false);
      logger.warn("Get File: {}", file.length());
      assertEquals(0, map2.size());
      assertEquals(test.length(), file.length());
      file.delete();

      // Get Tags
      map2 = waarpR66S3Client.getTags(BUCKET, FILEPATH11);
      logger.warn("Get Map: {}", map2.size());
      assertEquals(0, map2.size());
      map2 = waarpR66S3Client.getTags(BUCKET, FILEPATH12);
      logger.warn("Get Map: {}", map2.size());
      assertEquals(2, map2.size());

      // Set Tags
      map.put("Tag3", "Value3");
      waarpR66S3Client.setTags(BUCKET, FILEPATH11, map);
      map2 = waarpR66S3Client.getTags(BUCKET, FILEPATH11);
      logger.warn("Set/Get Map: {}", map2.size());
      assertEquals(3, map2.size());

      // Set Retention
      final ZonedDateTime wrong = ZonedDateTime.now().minusHours(1);
      try {
        waarpR66S3Client.bypassObjectRetention(BUCKET, FILEPATH11, wrong);
        fail("Should raised an exception");
      } catch (final IllegalArgumentException e) {
        // Nothing
      }
      int deleted = 0;
      try {
        ZonedDateTime current;
        current = waarpR66S3Client.getObjectRetention(BUCKET, FILEPATH11);
        logger.warn("Current ZoneDateTime {}", current);
        final ZonedDateTime correct = ZonedDateTime.now().plusSeconds(3);
        waarpR66S3Client.bypassObjectRetention(BUCKET, FILEPATH11, correct);
        current = waarpR66S3Client.getObjectRetention(BUCKET, FILEPATH11);
        logger.warn("New ZoneDateTime {}", current);
        assertTrue(current.isEqual(correct));
        Thread.sleep(4);
        // The file shall be deleted
        try {
          waarpR66S3Client.getFile(BUCKET, FILEPATH12, file, false);
          fail("Should raised an exception");
        } catch (final Exception e) {
          deleted++;
          logger.warn("Normal Exception {}", e.getMessage());
        }
      } catch (final Exception e) {
        // Ignore since right has not be given to S3 user
        logger.warn(
            "Ignore error since in Docker test environment Retention is not available");
      }
      // Now Delete all
      iterator = waarpR66S3Client.listObjectsFromBucket(BUCKET, null, true, 0);
      count = 0;
      // Now Delete all
      while (iterator.hasNext()) {
        final String name = iterator.next();
        waarpR66S3Client.deleteFile(BUCKET, name);
        logger.warn("Deleted {}", name);
        count++;
      }
      assertEquals(4 - deleted, count);
      iterator = waarpR66S3Client.listObjectsFromBucket(BUCKET, null, true, 0);
      count = 0;
      // Should be empty
      while (iterator.hasNext()) {
        final String name = iterator.next();
        logger.warn("Should not find {}", name);
        count++;
        fail("Should not find");
      }
      assertEquals(0, count);

      // Now start errors
      try {
        waarpR66S3Client.getFile(BUCKET, FILEPATH12, file, false);
        fail("Should raised an exception");
      } catch (final Exception e) {
        logger.warn("Normal Exception {}", e.getMessage());
      }
      // If not exists, no Exception
      waarpR66S3Client.deleteFile(BUCKET, FILEPATH12);
      try {
        waarpR66S3Client.getObjectRetention(BUCKET, FILEPATH12);
        fail("Should raised an exception");
      } catch (final Exception e) {
        logger.warn("Normal Exception {}", e.getMessage());
      }
      try {
        waarpR66S3Client.getTags(BUCKET, FILEPATH12);
        fail("Should raised an exception");
      } catch (final Exception e) {
        logger.warn("Normal Exception {}", e.getMessage());
      }
      try {
        waarpR66S3Client.setTags(BUCKET, FILEPATH12, map);
        fail("Should raised an exception");
      } catch (final Exception e) {
        logger.warn("Normal Exception {}", e.getMessage());
      }
      // No Exception when rewrite
      waarpR66S3Client.createFile(BUCKET, FILEPATH12, test, null);
      waarpR66S3Client.createFile(BUCKET, FILEPATH12, test, null);
      try {
        waarpR66S3Client.deleteFile(BUCKET, FILEPATH12);
      } catch (final Exception e) {
        fail("Should not raised an exception");
      }
      iterator = waarpR66S3Client.listObjectsFromBucket(BUCKET, null, true, 0);
      count = 0;
      // Should be empty
      while (iterator.hasNext()) {
        final String name = iterator.next();
        logger.warn("Should not find {}", name);
        count++;
        fail("Should not find");
      }
      assertEquals(0, count);

    } catch (final IOException | OpenR66ProtocolNetworkException e) {
      logger.error(e);
      fail(e.getMessage());
    }
  }

}
