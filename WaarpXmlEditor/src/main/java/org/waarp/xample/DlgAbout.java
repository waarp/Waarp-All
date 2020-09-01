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

import org.waarp.common.logging.SysErrLogger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

/*
 * Copyright (c) 2002 Felix Golubov
 */

/**
 * "About" dialog for the XAmple application.
 *
 * @author Felix Golubov
 * @version 1.0
 */

public class DlgAbout extends JDialog implements ActionListener {
  /**
   *
   */
  private static final long serialVersionUID = -2724699673221265107L;
  final JPanel panel1 = new JPanel();
  final JPanel panel2 = new JPanel();
  final JPanel insetsPanel1 = new JPanel();
  final JPanel insetsPanel3 = new JPanel();
  final JButton button1 = new JButton();
  final JLabel label1 = new JLabel();
  final JLabel label2 = new JLabel();
  final JLabel label3 = new JLabel();
  final JLabel label4 = new JLabel();
  final BorderLayout borderLayout1 = new BorderLayout();
  final BorderLayout borderLayout2 = new BorderLayout();
  final GridLayout gridLayout1 = new GridLayout();
  final String product = "XAmple-Waarp XML Configuration Editor";
  final String version = "Version 1.0";
  final String copyright =
      "Copyright (c) 2002 Felix Golubov & 2010 Frederic Bregier";
  final String comments = "Helper for XML Configuration editing";
  transient Border border1;

  public DlgAbout(final Frame parent) {
    super(parent);
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    try {
      jbInit();
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
    }
    pack();
  }

  private void jbInit() throws Exception {
    border1 = BorderFactory.createCompoundBorder(
        BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
        BorderFactory.createEmptyBorder(20, 20, 20, 20));
    setTitle("About");
    setResizable(false);
    panel1.setLayout(borderLayout1);
    panel2.setLayout(borderLayout2);
    gridLayout1.setRows(4);
    gridLayout1.setColumns(1);
    label1.setFont(new Font("Dialog", 1, 18));
    label1.setText(product);
    label2.setText(version);
    label3.setText(copyright);
    label4.setText(comments);
    insetsPanel3.setLayout(gridLayout1);
    insetsPanel3.setBorder(border1);
    button1.setText("Ok");
    button1.addActionListener(this);
    getContentPane().add(panel1, null);
    insetsPanel3.add(label1, null);
    insetsPanel3.add(label2, null);
    insetsPanel3.add(label3, null);
    insetsPanel3.add(label4, null);
    panel2.add(insetsPanel3, BorderLayout.CENTER);
    insetsPanel1.add(button1, null);
    panel1.add(insetsPanel1, BorderLayout.SOUTH);
    panel1.add(panel2, BorderLayout.NORTH);
  }

  @Override
  protected void processWindowEvent(final WindowEvent e) {
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      dispose();
    }
    super.processWindowEvent(e);
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    if (e.getSource() == button1) {
      dispose();
    }
  }
}
