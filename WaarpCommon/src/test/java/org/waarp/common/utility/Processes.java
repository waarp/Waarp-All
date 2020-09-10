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

package org.waarp.common.utility;

import com.google.common.collect.ImmutableList;
import jnr.constants.platform.Errno;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import jnr.posix.POSIXHandler;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline.Argument;
import org.apache.tools.ant.types.Path;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.SysErrLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Processes {
  private static final OurPOSIXHandler posixHandler;
  private static String jvmArgsDefault = null;

  private Processes() {
  }

  static {
    OurPOSIXHandler posixHandler1 = null;
    try {
      Class.forName("jnr.posix.POSIX");
      posixHandler1 = new OurPOSIXHandler();
    } catch (ClassNotFoundException e) {
      posixHandler1 = null;
    }
    posixHandler = posixHandler1;
  }

  public static int getCurrentPid() {
    return getPosix().getpid();
  }

  public static synchronized int getPidLinux(Process p) {
    int pid = -1;

    try {
      if ("java.lang.UNIXProcess".equals(p.getClass().getName()) ||
          "java.lang.ProcessImpl".equals(p.getClass().getName())) {
        Field f = p.getClass().getDeclaredField("pid");
        f.setAccessible(true);
        pid = f.getInt(p);
        f.setAccessible(false);
      }
    } catch (Exception e) {
      throw new RuntimeException("Cannot find PID");
    }
    return pid;
  }

  public static int getPidOfRunnerCommandLinux(String filterByRunner,
                                               String filterByCommand) {
    List<String> args = Arrays
        .asList(("ps -C " + filterByRunner + " -e -o pid,args=").split(" +"));
    // Example output:
    // 22245 /opt/libreoffice5.4/program/soffice.bin --headless
    // 22250 [soffice.bin] <defunct>

    try {
      Process psAux = new ProcessBuilder(args).start();
      try {
        Thread.sleep(100);
      } catch (InterruptedException ignored) {//NOSONAR
      }
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(psAux.getInputStream(), WaarpStringUtils.UTF8));
      try {
        String line;
        while ((line = reader.readLine()) != null) {
          if (!line.contains(filterByCommand)) {
            continue;
          }
          String[] parts = line.split("[\\W]+");
          if (parts.length < 2) {
            throw new RuntimeException(
                "Unexpected format of the `ps` line, expected at least 2 " +
                "columns:\n\t" + line);
          }
          for (int i = 0; i < 2; i++) {
            String pid = parts[i];
            if (!pid.trim().isEmpty()) {
              return Integer.parseInt(pid);
            }
          }
        }
      } finally {
        FileUtils.close(reader);
      }
    } catch (IOException ex) {
      throw new RuntimeException(
          String.format("Failed executing %s: %s", args, ex.getMessage()), ex);
    }
    throw new RuntimeException("Cannot find PID");
  }

  public static int getPidOfRunnerCommandLinux(String filterByRunner,
                                               String filterByCommand,
                                               List<Integer> previousPids) {
    List<String> args = Arrays
        .asList(("ps -C " + filterByRunner + " -e -o pid,args=").split(" +"));
    // Example output:
    // 22245 /opt/libreoffice5.4/program/soffice.bin --headless
    // 22250 [soffice.bin] <defunct>

    try {
      Process psAux = new ProcessBuilder(args).start();
      try {
        Thread.sleep(100);
      } catch (InterruptedException ignored) {//NOSONAR
      }
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(psAux.getInputStream(), WaarpStringUtils.UTF8));
      try {
        String line;
        while ((line = reader.readLine()) != null) {
          if (!line.contains(filterByCommand)) {
            continue;
          }
          String[] parts = line.split("[\\W]+");
          if (parts.length < 2) {
            throw new RuntimeException(
                "Unexpected format of the `ps` line, expected at least 2 " +
                "columns:\n\t" + line);
          }
          for (int i = 0; i < 2; i++) {
            String pid = parts[i];
            if (!pid.trim().isEmpty()) {
              boolean found = false;
              int ipid = Integer.parseInt(pid);
              for (Integer oldPid : previousPids) {
                if (ipid == oldPid) {
                  found = true;
                  break;
                }
              }
              if (found) {
                break;
              }
              return Integer.parseInt(pid);
            }
          }
        }
      } finally {
        FileUtils.close(reader);
      }
    } catch (IOException ex) {
      throw new RuntimeException(
          String.format("Failed executing %s: %s", args, ex.getMessage()), ex);
    }
    throw new RuntimeException("Cannot find PID");
  }

  public static void kill(int pid, boolean graceful) {
    if (System.getProperty("os.name").startsWith("Windows")) {
      List<String> args = ImmutableList
          .of("taskkill", graceful? "/t" : "/f", "/pid", Integer.toString(pid));
      try {
        Process process = new ProcessBuilder(args).start();
        BufferedReader br = null;
        try {
          br = new BufferedReader(
              new InputStreamReader(process.getInputStream()));
          String line = null;
          while ((line = br.readLine()) != null) {
            // nothing
          }
        } finally {
          FileUtils.close(br);
        }
        process.waitFor();
      } catch (IOException ignored) {
      } catch (InterruptedException ignored) {//NOSONAR
      }
    } else {
      int signal = graceful? 15 : 9;
      killLinux(pid, signal);
    }
    try {
      Thread.sleep(100);
    } catch (InterruptedException ignored) {//NOSONAR
    }
  }

  public static boolean exists(int pid) {
    if (System.getProperty("os.name").startsWith("Windows")) {
      return existsWindows(pid);
    } else {
      return killLinux(pid, 0) == 0;
    }
  }

  static int killLinux(int pid, int signal) {
    if (pid < 100) {
      SysErrLogger.FAKE_LOGGER
          .syserr("Should never try to kill process with pid < 100");
      throw new RuntimeException(
          "Should never try to kill process with pid < 100");
    }
    if (posixHandler != null) {
      return getPosix().kill(pid, signal);
    } else {
      List<String> args = new ArrayList<String>();
      args.add("kill");
      args.add("-s");
      args.add(Integer.toString(signal));
      args.add(Integer.toString(pid));
      try {
        Process process = new ProcessBuilder(args).start();
        BufferedReader br = null;
        try {
          br = new BufferedReader(
              new InputStreamReader(process.getInputStream()));
          String line = null;
          while ((line = br.readLine()) != null) {
            // nothing
          }
        } finally {
          FileUtils.close(br);
        }
        return process.waitFor();
      } catch (IOException ignored) {
      } catch (InterruptedException ignored) {//NOSONAR
      }
    }
    throw new RuntimeException("Cannot find PID");
  }

  static boolean existsWindows(int pid) {
    final AtomicBoolean exist = new AtomicBoolean(false);
    try {
      Process psAux =
          new ProcessBuilder("tasklist.exe", "/fo", "csv", "/nh", "/fi",
                             "PID eq " + pid).redirectErrorStream(true).start();
      try {
        Thread.sleep(100);
      } catch (InterruptedException ignored) {//NOSONAR
      }
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(psAux.getInputStream(), WaarpStringUtils.UTF8));
      try {
        String line;
        String spid = Integer.toString(pid);
        while ((line = reader.readLine()) != null) {
          if (line.contains(spid)) {
            exist.set(true);
          }
        }
      } finally {
        FileUtils.close(reader);
      }
      psAux.waitFor();
    } catch (Throwable ignored) {
    }
    return exist.get();
  }

  public static void setJvmArgsDefault(String jvmArgsDefault1) {
    jvmArgsDefault = jvmArgsDefault1;
  }

  public static String contentXmx() {
    final List<String> vmArguments =
        ManagementFactory.getRuntimeMXBean().getInputArguments();
    for (final String arg : vmArguments) {
      if (arg.contains("-Xmx")) {
        return arg;
      }
    }
    return null;
  }

  public static Process launchJavaProcess(String applArgs) throws IOException {
    try {
      // java binary
      final String java = System.getProperty("java.home") + "/bin/java";
      // vm arguments
      final List<String> vmArguments =
          ManagementFactory.getRuntimeMXBean().getInputArguments();
      final StringBuilder vmArgsOneLine = new StringBuilder();
      boolean xms = false;
      boolean xmx = false;
      for (final String arg : vmArguments) {
        // if it's the agent argument : we ignore it otherwise the
        // address of the old application and the new one will be in conflict
        if (!arg.contains("-agentlib")) {
          vmArgsOneLine.append(arg).append(' ');
        }
        if (arg.contains("-Xms")) {
          xms = true;
        }
        if (arg.contains("-Xmx")) {
          xmx = true;
        }
      }
      if (jvmArgsDefault != null) {
        if (jvmArgsDefault.contains("-Xms")) {
          xms = true;
        }
        if (jvmArgsDefault.contains("-Xmx")) {
          xmx = true;
        }
      }
      if (!xms) {
        vmArgsOneLine.append("-Xms1024m ");
      }
      if (!xmx) {
        vmArgsOneLine.append("-Xmx1024m ");
      }
      if (jvmArgsDefault != null) {
        vmArgsOneLine.append(' ').append(jvmArgsDefault).append(' ');
      }
      // init the command to execute, add the vm args
      final StringBuilder cmd;
      if (DetectionUtils.isWindows()) {
        cmd = new StringBuilder('"' + java + "\" " + vmArgsOneLine);
      } else {
        cmd = new StringBuilder(java + ' ' + vmArgsOneLine);
      }
      cmd.append(applArgs);
      return Runtime.getRuntime().exec(cmd.toString());
    } catch (final Throwable e) {
      // something went wrong
      throw new IOException("Error while trying to restart the application", e);
    }
  }

  public static Project getProject(File homeDir) {
    // global ant project settings
    Project project = new Project();
    project.setBaseDir(homeDir);
    project.init();
    final WaarpLoggerListener listener = new WaarpLoggerListener();
    project.addBuildListener(listener);
    listener.setMessageOutputLevel(Project.MSG_WARN);
    project.fireBuildStarted();
    return project;
  }

  public static void finalizeProject(Project project) {
    project.log("finished");
    project.fireBuildFinished(null);
  }

  public static int executeJvm(Project project, File homeDir, Class<?> zclass,
                               String[] args, boolean longTerm) {
    int pid = 99999999;
    try {
      // initialize an java task
      final Java javaTask = new Java();
      javaTask.setNewenvironment(false);
      javaTask.setTaskName(zclass.getSimpleName());
      javaTask.setProject(project);
      javaTask.setFork(true);
      javaTask.setCloneVm(true);
      javaTask.setSpawn(longTerm);
      javaTask.setFailonerror(true);
      javaTask.setClassname(zclass.getName());

      // add some vm args
      final Argument jvmArgs = javaTask.createJvmarg();
      if (jvmArgsDefault != null) {
        if (jvmArgsDefault.contains("-Xmx")) {
          jvmArgs.setLine(jvmArgsDefault);
        } else {
          jvmArgs.setLine("-Xms1024m -Xmx1024m " + jvmArgsDefault);
        }
      } else {
        jvmArgs.setLine("-Xms1024m -Xmx1024m ");
      }

      // added some args for to class to launch
      final Argument taskArgs = javaTask.createArg();
      final StringBuilder builder = new StringBuilder();
      for (final String string : args) {
        builder.append(' ').append(string);
      }
      taskArgs.setLine(builder.toString());

      // set the class path
      final String classpath = System.getProperty("java.class.path");
      final Path classPath = javaTask.createClasspath();
      classPath.setPath(classpath);
      javaTask.setClasspath(classPath);

      javaTask.init();
      final int ret = javaTask.executeJava();
      if (longTerm) {
        pid = getPidOfRunnerCommandLinux("java", zclass.getName());
      }
      SysErrLogger.FAKE_LOGGER.syserr(
          zclass.getName() + ' ' + builder + " return code: " + ret + " pid: " +
          pid);
    } catch (final BuildException e) {
      SysErrLogger.FAKE_LOGGER.syserr("While java task", e);
    }
    return pid;
  }

  public static String getCurrentMethodName() {
    return Thread.currentThread().getStackTrace()[2].getMethodName();
  }

  private static POSIX getPosix() {
    return PosixSingletonHolder.instance;
  }

  private static final class PosixSingletonHolder {
    private static final POSIX instance =
        POSIXFactory.getNativePOSIX(posixHandler);
  }

  private static final class OurPOSIXHandler implements POSIXHandler {
    private final AtomicBoolean verbose = new AtomicBoolean(false);

    void setVerbose(boolean verbose) {
      this.verbose.set(verbose);
    }

    @Override
    public void error(Errno error, String extraData) {
      throw new RuntimeException(
          "native error " + error.description() + ' ' + extraData);
    }

    @Override
    public void error(Errno errno, String extraData1, String extraData2) {
      throw new RuntimeException(
          "native error " + errno.description() + ' ' + extraData1 + ' ' +
          extraData2);
    }

    @Override
    public void unimplementedError(String methodName) {
      throw new IllegalStateException(
          methodName + " is not implemented in jnr-posix");
    }

    @Override
    public void warn(WARNING_ID id, String message, Object... data) {
      String msg;
      try {
        msg = String.format(message, data);
      } catch (IllegalFormatException e) {
        msg = message + ' ' + Arrays.toString(data);
      }
      Logger.getLogger("jnr-posix").log(Level.WARNING, msg);
    }

    @Override
    public boolean isVerbose() {
      return verbose.get();
    }

    @Override
    public File getCurrentWorkingDirectory() {
      return new File(".");
    }

    @Override
    public String[] getEnv() {
      String[] envp = new String[System.getenv().size()];
      int i = 0;
      for (Map.Entry<String, String> pair : System.getenv().entrySet()) {
        envp[i++] = pair.getKey() + '=' + pair.getValue();
      }
      return envp;

    }

    @Override
    public InputStream getInputStream() {
      return System.in;
    }

    @Override
    public PrintStream getOutputStream() {
      return System.out;
    }

    @Override
    public int getPID() {
      throw new IllegalStateException("getPID is not implemented in jnr-posix");
    }

    @Override
    public PrintStream getErrorStream() {
      return System.err;
    }
  }
}