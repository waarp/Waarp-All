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

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectRetentionArgs;
import io.minio.GetObjectTagsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.SetObjectRetentionArgs;
import io.minio.SetObjectTagsArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import io.minio.messages.Retention;
import io.minio.messages.RetentionMode;
import io.minio.messages.Tags;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.utility.SingletonUtils;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNetworkException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Map;

import static org.waarp.common.file.FileUtils.*;

/**
 * Waarp R66 S3 Client
 */
public class WaarpR66S3Client {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpR66S3Client.class);
  public static final String BUCKET_OR_TARGET_CANNOT_BE_NULL_OR_EMPTY =
      "Bucket or Target cannot be null or empty";
  public static final String S_3_ISSUE = "S3 issue: ";
  public static final String BUCKET_OR_SOURCE_CANNOT_BE_NULL_OR_EMPTY =
      "Bucket or Source cannot be null or empty";

  private final MinioClient minioClient;

  /**
   * Initialize context for S3 Client
   *
   * @param accessKey
   * @param secretKey
   * @param endPointS3
   */
  public WaarpR66S3Client(final String accessKey, final String secretKey,
                          final URL endPointS3) {
    ParametersChecker
        .checkParameter("Parameters cannot be null or empty", accessKey,
                        secretKey, endPointS3);
    // Create a minioClient with the MinIO server playground, its access key and secret key.
    minioClient = MinioClient.builder().endpoint(endPointS3)
                             .credentials(accessKey, secretKey).build();
  }

  /**
   * Create one file into S3 with optional Tags (null if none)
   *
   * @param bucketName
   * @param targetName
   * @param file
   * @param tags
   *
   * @return versionId
   *
   * @throws OpenR66ProtocolNetworkException
   */
  public String createFile(final String bucketName, final String targetName,
                           final File file, final Map<String, String> tags)
      throws OpenR66ProtocolNetworkException {
    ParametersChecker
        .checkParameter(BUCKET_OR_TARGET_CANNOT_BE_NULL_OR_EMPTY, bucketName,
                        targetName);
    ParametersChecker.checkParameter("File cannot be null", file);
    if (!file.canRead()) {
      throw new IllegalArgumentException(
          "File cannot be read: " + file.getAbsolutePath());
    }
    boolean uploaded = false;
    boolean error = false;
    try {
      // Make bucketName bucket if not exist.
      final boolean found = minioClient
          .bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
      if (!found) {
        // Make a new bucket called 'asiatrip'.
        minioClient
            .makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      } else {
        logger.info("Bucket {} already exists.", bucketName);
      }
      // Upload file as object name targetName to bucket bucketName
      final ObjectWriteResponse response = minioClient.uploadObject(
          UploadObjectArgs.builder().bucket(bucketName).object(targetName)
                          .filename(file.getAbsolutePath()).build());
      uploaded = true;
      logger.info("{} is successfully uploaded as object {} to bucket {}.",
                  file.getAbsolutePath(), targetName, bucketName);
      if (tags != null && !tags.isEmpty()) {
        minioClient.setObjectTags(
            SetObjectTagsArgs.builder().bucket(bucketName).object(targetName)
                             .tags(tags).build());
      }
      logger.debug("Resp: {} {} {} {} {}", response.bucket(), response.object(),
                   response.versionId(), response.etag(), response.region());
      return response.versionId();
    } catch (final MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
      logger.error(e.getMessage());
      error = true;
      throw new OpenR66ProtocolNetworkException(S_3_ISSUE + e.getMessage(), e);
    } finally {
      if (error && uploaded) {
        // Clean incompletely object creation
        try {
          deleteFile(bucketName, targetName);
        } catch (final Exception e) {
          logger.warn(
              "Error while cleaning S3 file incompletely created" + " : {}",
              e.getMessage());
        }
      }
    }
  }

  /**
   * Set a Tags to S3
   *
   * @param bucketName
   * @param targetName
   * @param tags
   *
   * @throws OpenR66ProtocolNetworkException
   */
  public void setTags(final String bucketName, final String targetName,
                      final Map<String, String> tags)
      throws OpenR66ProtocolNetworkException {
    ParametersChecker
        .checkParameter(BUCKET_OR_TARGET_CANNOT_BE_NULL_OR_EMPTY, bucketName,
                        targetName);
    try {
      if (tags != null && !tags.isEmpty()) {
        minioClient.setObjectTags(
            SetObjectTagsArgs.builder().bucket(bucketName).object(targetName)
                             .tags(tags).build());
      }
    } catch (final MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
      logger.error(e.getMessage());
      throw new OpenR66ProtocolNetworkException(S_3_ISSUE + e.getMessage(), e);
    }
  }

  /**
   * Get the ZoneDateTime when this Object will be deleted
   *
   * @param bucketName
   * @param sourceName
   *
   * @return the ZoneDateTime
   *
   * @throws OpenR66ProtocolNetworkException
   */
  public ZonedDateTime getObjectRetention(final String bucketName,
                                          final String sourceName)
      throws OpenR66ProtocolNetworkException {
    ParametersChecker
        .checkParameter(BUCKET_OR_TARGET_CANNOT_BE_NULL_OR_EMPTY, bucketName,
                        sourceName);
    try {
      final Retention retention = minioClient.getObjectRetention(
          GetObjectRetentionArgs.builder().bucket(bucketName).object(bucketName)
                                .build());
      return retention.retainUntilDate();
    } catch (final MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
      logger.error(e.getMessage());
      throw new OpenR66ProtocolNetworkException(S_3_ISSUE + e.getMessage(), e);
    }
  }

  /**
   * Bypass the governance retention and set a specific validity time in the future
   *
   * @param bucketName
   * @param targetName
   * @param retentionUntil
   *
   * @throws OpenR66ProtocolNetworkException
   */
  public void bypassObjectRetention(final String bucketName,
                                    final String targetName,
                                    final ZonedDateTime retentionUntil)
      throws OpenR66ProtocolNetworkException {
    ParametersChecker
        .checkParameter(BUCKET_OR_TARGET_CANNOT_BE_NULL_OR_EMPTY, bucketName,
                        targetName);
    ParametersChecker
        .checkParameter("Retention cannot be null", retentionUntil);
    if (retentionUntil.isBefore(ZonedDateTime.now())) {
      logger.warn("Retention Date Time is before now");
      throw new IllegalArgumentException("Retention Date Time is before now");
    }
    final Retention config =
        new Retention(RetentionMode.COMPLIANCE, retentionUntil);
    // Set object retention
    try {
      minioClient.setObjectRetention(
          SetObjectRetentionArgs.builder().bucket(bucketName).object(targetName)
                                .config(config).bypassGovernanceMode(true)
                                .build());
    } catch (final MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
      logger.error(e.getMessage());
      throw new OpenR66ProtocolNetworkException(S_3_ISSUE + e.getMessage(), e);
    }
  }

  /**
   * Get a File from S3
   *
   * @param bucketName
   * @param sourceName
   * @param file
   * @param getTags if False, will return an empty Map
   *
   * @return the Tag as Map of Strings
   *
   * @throws OpenR66ProtocolNetworkException
   */
  public Map<String, String> getFile(final String bucketName,
                                     final String sourceName, final File file,
                                     final boolean getTags)
      throws OpenR66ProtocolNetworkException {
    ParametersChecker
        .checkParameter(BUCKET_OR_SOURCE_CANNOT_BE_NULL_OR_EMPTY, bucketName,
                        sourceName);
    ParametersChecker.checkParameter("File cannot be null", file);
    boolean downloaded = false;
    boolean error = false;
    // Get input stream to have content of 'my-objectname' from 'my-bucketname'
    // Read the input stream and print to the console till EOF.
    try (final InputStream stream = minioClient.getObject(
        GetObjectArgs.builder().bucket(bucketName).object(sourceName).build());
         final FileOutputStream outputStream = new FileOutputStream(file)) {
      final byte[] buf = new byte[ZERO_COPY_CHUNK_SIZE];
      int bytesRead;
      while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
        outputStream.write(buf, 0, bytesRead);
      }
      FileUtils.close(outputStream);
      FileUtils.close(stream);
      downloaded = true;
      if (getTags) {
        final Tags tags = minioClient.getObjectTags(
            GetObjectTagsArgs.builder().bucket(bucketName).object(sourceName)
                             .build());
        return tags.get();
      } else {
        return SingletonUtils.singletonMap();
      }
    } catch (final MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
      logger.info(e);
      error = true;
      throw new OpenR66ProtocolNetworkException(S_3_ISSUE + e.getMessage(), e);
    } finally {
      if (error && downloaded) {
        // Delete wrongly deleted file
        file.delete();
      }
    }
  }

  /**
   * Get a Tags from S3
   *
   * @param bucketName
   * @param sourceName
   *
   * @return the Tag as Map of Strings
   *
   * @throws OpenR66ProtocolNetworkException
   */
  public Map<String, String> getTags(final String bucketName,
                                     final String sourceName)
      throws OpenR66ProtocolNetworkException {
    ParametersChecker
        .checkParameter(BUCKET_OR_SOURCE_CANNOT_BE_NULL_OR_EMPTY, bucketName,
                        sourceName);
    try {
      final Tags tags = minioClient.getObjectTags(
          GetObjectTagsArgs.builder().bucket(bucketName).object(sourceName)
                           .build());

      return tags.get();
    } catch (final MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
      logger.error(e.getMessage());
      throw new OpenR66ProtocolNetworkException(S_3_ISSUE + e.getMessage(), e);
    }
  }

  /**
   * Delete S3 source
   *
   * @param bucketName
   * @param sourceName
   *
   * @throws OpenR66ProtocolNetworkException
   */
  public void deleteFile(final String bucketName, final String sourceName)
      throws OpenR66ProtocolNetworkException {
    ParametersChecker
        .checkParameter(BUCKET_OR_SOURCE_CANNOT_BE_NULL_OR_EMPTY, bucketName,
                        sourceName);
    try {
      // Remove object.
      minioClient.removeObject(
          RemoveObjectArgs.builder().bucket(bucketName).object(sourceName)
                          .build());
    } catch (final MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
      logger.error(e.getMessage());
      throw new OpenR66ProtocolNetworkException(S_3_ISSUE + e.getMessage(), e);
    }
  }

  /**
   * Get sourceNames from S3 from the bucketName specified, names optionally starting with a String, recursively or not,
   * and possibly unlimited (if limit is <= 0)
   *
   * @param bucketName
   * @param optionalNameStartWith could be null or empty
   * @param recursively False only from main directory, True scanning also subdirectories
   * @param limit if <= 0, unlimited
   *
   * @return the Iterator on found sourceNames
   *
   * @throws OpenR66ProtocolNetworkException
   */
  public Iterator<String> listObjectsFromBucket(final String bucketName,
                                                final String optionalNameStartWith,
                                                final boolean recursively,
                                                final int limit)
      throws OpenR66ProtocolNetworkException {
    ParametersChecker
        .checkParameter("Bucket cannot be null or empty", bucketName);
    try {
      // Remove object.
      final ListObjectsArgs.Builder builder =
          ListObjectsArgs.builder().bucket(bucketName).recursive(recursively);
      if (ParametersChecker.isNotEmpty(optionalNameStartWith)) {
        builder.prefix(optionalNameStartWith);
      }
      if (limit > 0) {
        builder.maxKeys(limit);
      }
      final ListObjectsArgs args = builder.build();
      final Iterable<Result<Item>> iterable = minioClient.listObjects(args);
      return Iterators
          .transform(iterable.iterator(), new Function<Result<Item>, String>() {
            @Override
            public @Nullable String apply(
                @Nullable final Result<Item> itemResult) {
              try {
                if (itemResult != null) {
                  final Item item = itemResult.get();
                  if (item != null) {
                    return item.objectName();
                  }
                }
                return null;
              } catch (final MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
                logger.error(e.getMessage());
                return null;
              }
            }
          });
    } catch (final Exception e) {
      logger.error(e.getMessage());
      throw new OpenR66ProtocolNetworkException(S_3_ISSUE + e.getMessage(), e);
    }
  }
}
