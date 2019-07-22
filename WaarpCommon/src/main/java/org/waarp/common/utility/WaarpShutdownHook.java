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
package org.waarp.common.utility;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

/**
 * @author "Frederic Bregier"
 *
 */
public abstract class WaarpShutdownHook extends Thread {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger = WaarpLoggerFactory
      .getLogger(WaarpShutdownHook.class);
  /**
   * Sun property pointing the main class and its arguments. Might not be
   * defined on non Hotspot VM implementations.
   */
  private static final String SUN_JAVA_COMMAND = "sun.java.command";
  /**
   * Thread for ShutdownHook
   */
  public static WaarpShutdownHook shutdownHook = null;
  /**
   * Set if the program is in shutdown
   */
  private static volatile boolean shutdown = false;
  /**
   * Set if the program will start shutdown process
   */
  private static volatile boolean shutdownStarted = false;
  /**
   * Set if the program is in shutdown
   */
  private static volatile boolean immediate = false;
  /**
   * Set if the Handler is initialized
   */
  private static boolean initialized = false;
  /**
   * Is the shutdown finished
   */
  private static boolean isShutdownOver = false;
  private static String applArgs = null;
  private static volatile boolean shouldRestart = false;
  private ShutdownConfiguration shutdownConfiguration = null;

  public WaarpShutdownHook(ShutdownConfiguration configuration) {
    if (initialized) {
      shutdownHook.shutdownConfiguration = configuration;
      this.setName("WaarpShutdownHook");
      this.setDaemon(true);
      shutdownHook = this;
      this.shutdownConfiguration = configuration;
      return;
    }
    this.shutdownConfiguration = configuration;
    this.setName("WaarpShutdownHook");
    this.setDaemon(true);
    shutdownHook = this;
    initialized = true;
  }

  /**
   * For Server part
   */
  public static void addShutdownHook() {
    if (shutdownHook != null) {
      Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
  }

  /**
   * Says if the Process is currently in shutdown
   *
   * @return True if already in shutdown
   */
  public static boolean isInShutdown() {
    return shutdown;
  }

  /**
   * @return True if the Shutdown process will start soon
   */
  public static boolean isShutdownStarting() {
    return shutdownStarted;
  }

  /**
   * To specify that shutdown will soon start
   */
  public static void shutdownWillStart() {
    shutdownStarted = true;
  }

  /**
   * This function is the top function to be called when the process is to be
   * shutdown.
   *
   * @param immediateSet
   */
  public static void terminate(boolean immediateSet) {
    if (immediateSet) {
      immediate = immediateSet;
    }
    if (shutdownHook != null) {
      removeShutdownHook();
      terminate();
    } else {
      logger.error("No ShutdownHook setup");
      DetectionUtils.SystemExit(1);
    }
  }

  /**
   * For Server part
   */
  public static void removeShutdownHook() {
    if (shutdownHook != null) {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }
  }

  /**
   * Intermediary exit function
   */
  private static void terminate() {
    shutdownStarted = true;
    shutdown = true;
    if (immediate) {
      shutdownHook.exit();
      // Force exit!
      try {
        Thread.sleep(shutdownHook.shutdownConfiguration.timeout / 2);
      } catch (InterruptedException e) {
      }
      if (logger.isDebugEnabled()) {
        Map<Thread, StackTraceElement[]> map = Thread
            .getAllStackTraces();
        for (Thread thread : map.keySet()) {
          printStackTrace(thread, map.get(thread));
        }
      }
      isShutdownOver = true;
      logger.info("Should restart? " + isRestart());
      try {
        restartApplication();
      } catch (IOException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      shutdownHook.serviceStopped();
      System.err.println("Halt System");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
      DetectionUtils.SystemExit(0);
    } else {
      shutdownHook.launchFinalExit();
      immediate = true;
      shutdownHook.exit();
      isShutdownOver = true;
      shutdownHook.serviceStopped();
      logger.info("Should restart? " + isRestart());
      try {
        restartApplication();
      } catch (IOException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      logger.info("Exit System");
      System.err.println("Exit System");
      //Runtime.getRuntime().halt(0);
    }
  }

  /**
   * Real exit function
   */
  protected abstract void exit();

  /**
   * Print stack trace
   *
   * @param thread
   * @param stacks
   */
  static private void printStackTrace(Thread thread,
                                      StackTraceElement[] stacks) {
    System.err.print(thread.toString() + " : ");
    for (int i = 0; i < stacks.length - 1; i++) {
      System.err.print(stacks[i].toString() + " ");
    }
    if (stacks.length >= 1) {
      System.err.println(stacks[stacks.length - 1].toString());
    } else {
      System.err.println();
    }
  }

  /**
   * @return True if the shutdown should be followed by a restart
   */
  public static boolean isRestart() {
    return shouldRestart;
  }

  /**
   * Restart the application using the preset applArgs and computing the
   * jvmArgs. execute the command in a shutdown
   * hook, to be sure that all the resources have been disposed before
   * restarting the application
   *
   * @throws IOException
   */
  private static void restartApplication() throws IOException {
    if (shouldRestart) {
      try {
        // java binary
        String java = System.getProperty("java.home") + "/bin/java";
        // vm arguments
        List<String> vmArguments =
            ManagementFactory.getRuntimeMXBean().getInputArguments();
        StringBuilder vmArgsOneLine = new StringBuilder();
        for (String arg : vmArguments) {
          // if it's the agent argument : we ignore it otherwise the
          // address of the old application and the new one will be in conflict
          if (!arg.contains("-agentlib")) {
            vmArgsOneLine.append(arg).append(" ");
          }
        }
        // init the command to execute, add the vm args
        final StringBuilder cmd;
        if (DetectionUtils.isWindows()) {
          cmd = new StringBuilder("\"" + java + "\" " + vmArgsOneLine);
        } else {
          cmd = new StringBuilder(java + " " + vmArgsOneLine);
        }

        if (applArgs == null) {
          applArgs = getArgs();
        }
        if (applArgs == null) {
          // big issue then
          System.err.println("Cannot restart!");
        }
        cmd.append(applArgs);
        logger.debug("Should restart with:\n" + cmd.toString());
        logger.warn("Should restart");
        Runtime.getRuntime().exec(cmd.toString());
      } catch (Throwable e) {
        // something went wrong
        throw new IOException("Error while trying to restart the application",
                              e);
      }
    }
  }

  private boolean serviceStopped() {
    if (shutdownConfiguration.serviceFuture != null) {
      logger.info("Service will be stopped");
      shutdownConfiguration.serviceFuture.setSuccess();
      return true;
    }
    return false;
  }

  /**
   * Extra call to ensure exit after long delay
   */
  public void launchFinalExit() {
    if (DetectionUtils.isJunit()) {
      return;
    }
    Timer timer = new Timer("R66FinalExit", true);
    ShutdownTimerTask timerTask = new ShutdownTimerTask();
    timer.schedule(timerTask, shutdownConfiguration.timeout * 4);
  }

  /**
   * Try to return the application arguments (for Oracle VM)
   *
   * @return null if it cannot
   */
  private static String getArgs() {
    String test = System.getProperty(SUN_JAVA_COMMAND);
    if (test != null && !test.isEmpty()) {
      // compute args directly
      // program main and program arguments
      String[] mainCommand = test.split(" ");
      // program main is a jar
      StringBuilder args = new StringBuilder();
      if (mainCommand[0].endsWith(".jar")) {
        // if it's a jar, add -jar mainJar
        args.append("-jar ").append(new File(mainCommand[0]).getPath());
      } else {
        // else it's a .class, add the classpath and mainClass
        args.append("-cp \"").append(System.getProperty("java.class.path"))
            .append("\" ")
            .append(mainCommand[0]);
      }
      // finally add program arguments
      for (int i = 1; i < mainCommand.length; i++) {
        args.append(" ").append(mainCommand[i]);
      }
      return args.toString();
    }
    return null;
  }

  /**
   * Set the way software should shutdown: with (true) or without restart
   * (false)
   *
   * @param toRestart
   */
  public static void setRestart(boolean toRestart) {
    shouldRestart = toRestart;
  }

  /**
   * Called to setup main class and args to enable restart
   *
   * @param main
   * @param args
   */
  public static void registerMain(Class<?> main, String[] args) {
    if (main == null) {
      applArgs = getArgs();
      return;
    }
    applArgs = ManagementFactory.getRuntimeMXBean().getClassPath();
    if (applArgs != null && !applArgs.isEmpty()) {
      applArgs = "-cp " + applArgs;
    }
    applArgs += " " + main.getName();
    for (int i = 0; i < args.length; i++) {
      applArgs += " " + args[i];
    }
  }

  @Override
  public void run() {
    if (isShutdownOver) {
      if (shutdownHook != null && shutdownHook.serviceStopped()) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
        }
      }
      // Already stopped
      System.err.println("Halt System now - services already stopped -");
      DetectionUtils.SystemExit(0);
      return;
    }
    try {
      terminate();
    } catch (Throwable t) {
      if (shutdownHook != null && shutdownHook.serviceStopped()) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
        }
      }
    }
    System.err.println("Halt System now");
    DetectionUtils.SystemExit(0);
  }

  /**
   * Class for argument of creation of WaarpShutdownHook
   *
   * @author "Frederic Bregier"
   */
  public static class ShutdownConfiguration {
    public long timeout = 30000; // 30s per default
    public WaarpFuture serviceFuture; // no service per default
  }

  /**
   * Finalize resources attached to handlers
   *
   * @author Frederic Bregier
   */
  private static class ShutdownTimerTask extends TimerTask {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
        .getLogger(ShutdownTimerTask.class);

    /**
     * Internal constructor
     */
    private ShutdownTimerTask() {
    }

    @Override
    public void run() {
      System.err.println("Halt System now - time waiting is over");
      logger.error("System will force EXIT");
      if (shutdownHook != null && shutdownHook.serviceStopped()) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
        }
      }
      if (logger.isDebugEnabled()) {
        Map<Thread, StackTraceElement[]> map = Thread
            .getAllStackTraces();
        for (Thread thread : map.keySet()) {
          printStackTrace(thread, map.get(thread));
        }
      }
      DetectionUtils.SystemExit(0);
    }
  }
}
