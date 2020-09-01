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
package org.waarp.xample;

import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.SysErrLogger;

import javax.swing.UIManager;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * XML configuration edition GUI helper
 */
public class AdminXample extends XAmple {

  private static final long serialVersionUID = 6020872788819087355L;

  private boolean stillLaunched;
  private final List<AdminXample> list;

  public AdminXample(final List<AdminXample> list) {
    this.list = list;
    setStillLaunched(true);
    this.list.add(this);
  }

  @Override
  public void exit() {
    if (!confirmation()) {
      return;
    }
    saveRuntimeProperties();
    setStillLaunched(false);
    list.remove(this);
    dispose();
  }

  @Override
  protected void processWindowEvent(final WindowEvent e) {
    if (e.getID() == WindowEvent.WINDOW_CLOSING && confirmation()) {
      saveRuntimeProperties();
      setStillLaunched(false);
      list.remove(this);
      dispose();
    }
  }

  public static AdminXample start(final List<AdminXample> list) {
    assignDefaultFont();
    final Properties props = new Properties();
    InputStream in = null;
    try {
      final File file = new File(FILE_RUNTIME);
      if (file.exists()) {
        in = new FileInputStream(file);
        props.load(in);
      }
      final String lfName = props.getProperty(LOOK_AND_FEEL);
      String lfClassName = null;
      final UIManager.LookAndFeelInfo[] lfi =
          UIManager.getInstalledLookAndFeels();
      for (int i = 0; i < lfi.length && lfClassName == null; i++) {
        if (lfi[i].getName().equals(lfName)) {
          lfClassName = lfi[i].getClassName();
        }
      }
      if (lfClassName != null) {
        UIManager.setLookAndFeel(lfClassName);
      } else {
        UIManager
            .setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      }
    } catch (final Exception ex) {
      try {
        UIManager
            .setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      } catch (final Exception ex1) {
        SysErrLogger.FAKE_LOGGER.syserr(ex1);
      }
    } finally {
      FileUtils.close(in);
    }
    final AdminXample frame = new AdminXample(list);
    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    try {
      final Rectangle r = new Rectangle();
      r.x = Integer.parseInt(props.getProperty(BOUNDS_LEFT));
      r.y = Integer.parseInt(props.getProperty(BOUNDS_TOP));
      r.width = Integer.parseInt(props.getProperty(BOUNDS_WIDTH));
      r.height = Integer.parseInt(props.getProperty(BOUNDS_HEIGHT));
      if (r.width > screenSize.width) {
        r.width = screenSize.width;
      }
      if (r.height > screenSize.height) {
        r.height = screenSize.height;
      }
      if (r.x + r.width > screenSize.width) {
        r.x = screenSize.width - r.width;
      }
      if (r.y + r.height > screenSize.height) {
        r.y = screenSize.height - r.height;
      }
      frame.setBounds(r.x, r.y, r.width, r.height);
      frame.validate();
    } catch (final Exception ex) {
      frame.pack();
      final Dimension d = frame.getSize();
      if (d.height > screenSize.height) {
        d.height = screenSize.height;
      }
      if (d.width > screenSize.width) {
        d.width = screenSize.width;
      }
      frame.setLocation((screenSize.width - d.width) / 2,
                        (screenSize.height - d.height) / 2);
    }
    frame.setVisible(true);
    return frame;
  }

  /**
   * @return the stillLaunched
   */
  public boolean isStillLaunched() {
    return stillLaunched;
  }

  /**
   * @param stillLaunched the stillLaunched to set
   */
  private void setStillLaunched(final boolean stillLaunched) {
    this.stillLaunched = stillLaunched;
  }

}
