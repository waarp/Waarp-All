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
package org.waarp.openr66.r66gui;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 */
class R66Dialog extends JDialog {

  /**
   *
   */
  private static final long serialVersionUID = -6105635300084413738L;
  private final JPanel contentPanel = new JPanel();
  JEditorPane textPaneDialog;

  /**
   * Create the dialog.
   */
  public R66Dialog() {
    setTitle("R66 Gui Dialog");
    setBounds(100, 100, 426, 298);
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(new GridLayout(1, 0, 0, 0));
    {
      final JScrollPane scrollPane = new JScrollPane();
      contentPanel.add(scrollPane);
      {
        textPaneDialog = new JEditorPane();
        scrollPane.setViewportView(textPaneDialog);
        textPaneDialog.setEditable(false);
        textPaneDialog.setContentType("text/html");
      }
    }
    {
      final JPanel buttonPane = new JPanel();
      buttonPane.setLayout(new FlowLayout(FlowLayout.CENTER));
      getContentPane().add(buttonPane, BorderLayout.SOUTH);
      {
        final JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            R66ClientGui.window.enableAllButtons();
            dispose();
          }
        });
        okButton.setActionCommand("OK");
        buttonPane.add(okButton);
        getRootPane().setDefaultButton(okButton);
      }
    }
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
  }

}
