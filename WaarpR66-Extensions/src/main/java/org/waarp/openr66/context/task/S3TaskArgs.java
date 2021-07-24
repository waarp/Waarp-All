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
package org.waarp.openr66.context.task;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.openr66.s3.taskfactory.S3TaskFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * S3 GET Task<br>
 * <p>
 * Result of arguments will be as a S3 GET command.<br>
 * Format is the following:<br>
 * "-URL url of S3 service <br>
 * -accessKey access Key for S3 service <br>
 * -secretKey secret Key for S3 service <br>
 * -bucketName bucket Name where to retrieve the object <br>
 * -sourceName source Name from the bucket to select the final Object <br>
 * -file final File path (absolute or relative from IN path) <br>
 * [-getTags [* or list of tag names comma separated without space]]" <br>
 * -targetName source Name from the bucket to select the final Object <br>
 * [-setTags 'name:value,...' without space]" <br>
 */
public class S3TaskArgs {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(S3TaskArgs.class);

  public static final String ARG_URL = "URL";
  public static final String ARG_ACCESS_KEY = "accessKey";
  public static final String ARG_SECRET_KEY = "secretKey";
  public static final String ARG_BUCKET_NAME = "bucketName";
  public static final String ARG_SOURCE_NAME = "sourceName";
  public static final String ARG_TARGET_NAME = "targetName";
  public static final String ARG_FILE = "file";
  public static final String ARG_GET_TAGS = "getTags";
  public static final String ARG_SET_TAGS = "setTags";

  private static final Option OPTION_URL =
      Option.builder(ARG_URL).required(true).hasArg(true)
            .desc("Specify the URL to operate on").build();
  private static final Option OPTION_ACCESS_KEY =
      Option.builder(ARG_ACCESS_KEY).required(true).hasArg(true)
            .desc("Specify the Access Key to operate with").build();
  private static final Option OPTION_SECRET_KEY =
      Option.builder(ARG_SECRET_KEY).required(true).hasArg(true)
            .desc("Specify the Secret Key to operate with").build();
  private static final Option OPTION_BUCKET_NAME =
      Option.builder(ARG_BUCKET_NAME).required(true).hasArg(true)
            .desc("Specify the Bucket Name to operate with").build();
  private static final Option OPTION_SOURCE_NAME =
      Option.builder(ARG_SOURCE_NAME).required(true).hasArg(true)
            .desc("Specify the Source Name to operate on").build();
  private static final Option OPTION_TARGET_NAME =
      Option.builder(ARG_TARGET_NAME).required(true).hasArg(true)
            .desc("Specify the Target Name to operate on").build();
  private static final Option OPTION_FILE =
      Option.builder(ARG_FILE).required(true).hasArg(true)
            .desc("Specify the File to operate on").build();
  private static final Option OPTION_GET_TAGS =
      Option.builder(ARG_GET_TAGS).required(false).hasArg(true).desc(
                "Specify the Tags to retrieve: * or comma separated list without space")
            .build();
  private static final Option OPTION_SET_TAGS =
      Option.builder(ARG_SET_TAGS).required(false).hasArg(true).desc(
                "Specify the Tags to set: key1:value1,key2:value2 comma separated list without space")
            .build();

  private static final Options S3_GET_OPTIONS =
      new Options().addOption(OPTION_URL).addOption(OPTION_ACCESS_KEY)
                   .addOption(OPTION_SECRET_KEY).addOption(OPTION_BUCKET_NAME)
                   .addOption(OPTION_SOURCE_NAME).addOption(OPTION_FILE)
                   .addOption(OPTION_GET_TAGS);
  private static final Options S3_PUT_OPTIONS =
      new Options().addOption(OPTION_URL).addOption(OPTION_ACCESS_KEY)
                   .addOption(OPTION_SECRET_KEY).addOption(OPTION_BUCKET_NAME)
                   .addOption(OPTION_TARGET_NAME).addOption(OPTION_SET_TAGS);
  private static final Options S3_DELETE_OPTIONS =
      new Options().addOption(OPTION_URL).addOption(OPTION_ACCESS_KEY)
                   .addOption(OPTION_SECRET_KEY).addOption(OPTION_BUCKET_NAME)
                   .addOption(OPTION_SOURCE_NAME);
  protected static final String EMPTY_STRING_NOT_ALLOWED =
      "Empty String not allowed";

  /**
   * Print to standard output the help of this command
   */
  public static void printHelp(final S3TaskFactory.S3TaskType type,
                               final Options options) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(type.name(), options);
  }

  public static S3TaskArgs getS3Params(final S3TaskFactory.S3TaskType type,
                                       final int rank, final String[] args) {
    if (args == null || args.length == 0) {
      logger.error("Arguments cannot be empty or null");
      return null;
    }
    final String[] realArgs = getRealArgs(rank, args);
    final CommandLineParser parser = new DefaultParser();
    final Options options;
    switch (type) {
      case S3GET:
      case S3GETDELETE:
        options = S3_GET_OPTIONS;
        break;
      case S3PUT:
      case S3PUTR66DELETE:
        options = S3_PUT_OPTIONS;
        break;
      case S3DELETE:
        options = S3_DELETE_OPTIONS;
        break;
      default:
        logger.error("Unknown Task {}", type);
        return null;
    }
    try {
      final CommandLine cmd = parser.parse(options, realArgs, true);
      final String surl = cmd.getOptionValue(ARG_URL);
      final URL url = new URL(surl);
      final String accessKey = cmd.getOptionValue(ARG_ACCESS_KEY);
      final String secretKey = cmd.getOptionValue(ARG_SECRET_KEY);
      final String bucketName =
          cmd.getOptionValue(ARG_BUCKET_NAME).toLowerCase();
      final String sourceName =
          cmd.hasOption(ARG_SOURCE_NAME)? cmd.getOptionValue(ARG_SOURCE_NAME) :
              null;
      final String targetName =
          cmd.hasOption(ARG_TARGET_NAME)? cmd.getOptionValue(ARG_TARGET_NAME) :
              null;
      final String sfile =
          cmd.hasOption(ARG_FILE)? cmd.getOptionValue(ARG_FILE) : null;
      final File file;
      if (sfile != null) {
        file = new File(sfile);
      } else {
        file = null;
      }
      final String sgetTags =
          cmd.hasOption(ARG_GET_TAGS)? cmd.getOptionValue(ARG_GET_TAGS) : null;
      Set<String> getTags = null;
      if (ParametersChecker.isNotEmpty(sgetTags)) {
        getTags = new HashSet<>();
        final String[] keys = sgetTags.split(","); // NOSONAR
        boolean star = false;
        for (final String key : keys) {
          if (key.equalsIgnoreCase("*")) {
            star = true;
            break;
          }
          if (key.isEmpty()) {
            printHelp(type, options);
            logger.error("Arguments GetTags are incorrect: {}", sgetTags);
            return null;
          }
          getTags.add(key);
        }
        if (star) {
          getTags.clear();
        }
      }
      final String ssetTags =
          cmd.hasOption(ARG_SET_TAGS)? cmd.getOptionValue(ARG_SET_TAGS) : null;
      Map<String, String> setTags = null;
      if (ParametersChecker.isNotEmpty(ssetTags)) {
        setTags = new HashMap<>();
        final String[] keyvalues = ssetTags.split(","); // NOSONAR
        for (final String key : keyvalues) {
          if (key.isEmpty()) {
            printHelp(type, options);
            logger.error("Arguments SetTags are incorrect: {}", ssetTags);
            return null;
          }
          final String[] dual = key.split(":");
          if (dual.length != 2 || dual[0].isEmpty() || dual[1].isEmpty()) {
            printHelp(type, options);
            logger.error("Arguments SetTags are incorrect: {}", ssetTags);
            return null;
          }
          setTags.put(dual[0], dual[1]);
        }
      }
      return new S3TaskArgs(type, url, accessKey, secretKey, bucketName,
                            sourceName, targetName, file, getTags, setTags);
    } catch (final ParseException | MalformedURLException e) {
      printHelp(type, options);
      logger.error("Arguments are incorrect: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Finalize the real arguments to parse
   *
   * @param rank
   * @param args
   *
   * @return the real arguments
   */
  private static String[] getRealArgs(final int rank, final String[] args) {
    return rank == 0? args : Arrays.copyOfRange(args, rank, args.length);
  }

  private final URL url;
  private final String accessKey;
  private final String secretKey;
  private final String bucketName;
  private final String sourceName;
  private final String targetName;
  private final File file;
  private final Set<String> getTags;
  private final boolean getTag;
  private final Map<String, String> setTags;


  public S3TaskArgs(final S3TaskFactory.S3TaskType type, final URL url,
                    final String accessKey, final String secretKey,
                    final String bucketName, final String sourceName,
                    final String targetName, final File file,
                    final Set<String> getTags,
                    final Map<String, String> setTags) {
    ParametersChecker.checkParameter(EMPTY_STRING_NOT_ALLOWED, accessKey,
                                     secretKey, bucketName);
    if ((type == S3TaskFactory.S3TaskType.S3GET ||
         type == S3TaskFactory.S3TaskType.S3GETDELETE ||
         type == S3TaskFactory.S3TaskType.S3DELETE) &&
        ParametersChecker.isEmpty(sourceName) && file == null) {
      throw new IllegalArgumentException(EMPTY_STRING_NOT_ALLOWED);
    }
    if ((type == S3TaskFactory.S3TaskType.S3PUT ||
         type == S3TaskFactory.S3TaskType.S3PUTR66DELETE) &&
        ParametersChecker.isEmpty(targetName)) {
      throw new IllegalArgumentException(EMPTY_STRING_NOT_ALLOWED);
    }
    this.url = url;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.bucketName = bucketName;
    this.sourceName = sourceName;
    this.targetName = targetName;
    this.file = file;
    this.getTags = getTags;
    this.setTags = setTags;
    this.getTag = getTags != null;
  }

  public final URL getUrl() {
    return url;
  }

  public final String getAccessKey() {
    return accessKey;
  }

  public final String getSecretKey() {
    return secretKey;
  }

  public final String getBucketName() {
    return bucketName;
  }

  public final String getSourceName() {
    return sourceName;
  }

  public final String getTargetName() {
    return targetName;
  }

  public final File getFile() {
    return file;
  }

  public final Set<String> getGetTags() {
    return getTags;
  }

  public final boolean getGetTag() {
    return getTag;
  }

  public final Map<String, String> getSetTags() {
    return setTags;
  }
}
