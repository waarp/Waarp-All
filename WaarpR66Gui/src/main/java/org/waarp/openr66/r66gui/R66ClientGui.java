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

import com.swtdesigner.FocusTraversalOnArray;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * R66 Client GUI to show how to use the API and also to enable to test the
 * connectivity to R66 servers and
 * the validity of a transfer through a rule.
 */
public class R66ClientGui {
  /**
   * Internal Logger
   */
  private static WaarpLogger logger;

  static String[] staticArgs;
  public static R66ClientGui window;

  public JFrame frmRClientGui;
  private JTextField textFieldInformation;
  private JTextField textFieldFile;
  private final R66Environment environnement = new R66Environment();
  private JEditorPane textFieldStatus;
  private JComboBox<String> comboBoxHosts;
  private JComboBox<String> comboBoxRules;
  private JCheckBox checkBoxMD5;
  private JProgressBar progressBarTransfer;
  private JButton buttonTransferStart;
  private JMenu menu;
  private JButton buttonCheckConnection;
  private JButton buttonFileFind;
  private R66Dialog dialog;
  private JTextArea textPaneLog;
  private JScrollPane scrollPane;
  private JScrollPane scrollPane_1;
  private JCheckBox checkBoxDebug;
  protected boolean extended;

  /**
   * Launch the application.
   */
  public static void main(final String[] args) {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(R66ClientGui.class);
    }
    staticArgs = args;
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          window = new R66ClientGui(staticArgs);
          window.frmRClientGui.setVisible(true);
        } catch (final Exception e) {
          SysErrLogger.FAKE_LOGGER.syserr(e);
        }
      }
    });
  }

  /**
   * Used by extended class
   */
  protected R66ClientGui() {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(R66ClientGui.class);
      environnement.initLog();
    }
  }

  public R66Environment getEnvironment() {
    return environnement;
  }

  /**
   * Create the application.
   */
  public R66ClientGui(final String[] args) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(R66ClientGui.class);
      environnement.initLog();
    }
    environnement.initialize(args);
    initialize();
  }

  /**
   * Initialize the contents of the frame.
   */
  protected void initialize() {
    final String[] shosts = R66Environment.getHostIds();
    final String[] srules = R66Environment.getRules();

    frmRClientGui = new JFrame();
    frmRClientGui.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        if (extended) {
          frmRClientGui.dispose();
        } else {
          environnement.exit();
          System.exit(0);//NOSONAR
        }
      }
    });
    frmRClientGui
        .setTitle("R66 Client Gui: " + Configuration.configuration.getHostId());
    frmRClientGui.setBounds(100, 100, 724, 546);
    if (extended) {
      frmRClientGui.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    } else {
      frmRClientGui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    final JMenuBar menuBar = new JMenuBar();
    frmRClientGui.setJMenuBar(menuBar);

    menu = new JMenu(Messages.getString("R66ClientGui.1")); //$NON-NLS-1$
    menuBar.add(menu);

    final JMenuItem menuItemExit =
        new JMenuItem(Messages.getString("R66ClientGui.2")); //$NON-NLS-1$
    menuItemExit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (extended) {
          frmRClientGui.dispose();
        } else {
          environnement.exit();
          System.exit(0);//NOSONAR
        }
      }
    });
    menu.add(menuItemExit);

    final JSeparator separator = new JSeparator();
    menu.add(separator);

    final JMenuItem menuItemHelp =
        new JMenuItem(Messages.getString("R66ClientGui.3")); //$NON-NLS-1$
    menuItemHelp.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        environnement.about();
        showDialog();
      }
    });
    menu.add(menuItemHelp);
    final GridBagLayout gridBagLayout = new GridBagLayout();
    gridBagLayout.columnWidths = new int[] { 24, 130, 80, 369, 99, 0 };
    gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 27, 179, 162 };
    gridBagLayout.columnWeights =
        new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
    gridBagLayout.rowWeights =
        new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0 };
    frmRClientGui.getContentPane().setLayout(gridBagLayout);

    buttonCheckConnection =
        new JButton(Messages.getString("R66ClientGui.4")); //$NON-NLS-1$
    buttonCheckConnection
        .setToolTipText(Messages.getString("R66ClientGui.5")); //$NON-NLS-1$
    buttonCheckConnection.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        final R66ClientGuiActions action =
            new R66ClientGuiActions(R66ClientGuiActions.CHECKCONNECTION);
        action.execute();
      }
    });
    final GridBagConstraints gbc_buttonCheckConnection =
        new GridBagConstraints();
    gbc_buttonCheckConnection.insets = new Insets(0, 0, 5, 5);
    gbc_buttonCheckConnection.gridx = 1;
    gbc_buttonCheckConnection.gridy = 0;
    frmRClientGui.getContentPane()
                 .add(buttonCheckConnection, gbc_buttonCheckConnection);

    final JLabel label =
        new JLabel(Messages.getString("R66ClientGui.6")); //$NON-NLS-1$
    final GridBagConstraints gbc_label = new GridBagConstraints();
    gbc_label.insets = new Insets(0, 0, 5, 5);
    gbc_label.gridx = 2;
    gbc_label.gridy = 0;
    frmRClientGui.getContentPane().add(label, gbc_label);

    comboBoxHosts = new JComboBox<String>(shosts);
    comboBoxHosts.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        environnement.guiResultat =
            R66Environment.getHost((String) comboBoxHosts.getSelectedItem());
        setStatus(environnement.guiResultat);
      }
    });
    label.setLabelFor(comboBoxHosts);
    comboBoxHosts
        .setToolTipText(Messages.getString("R66ClientGui.7")); //$NON-NLS-1$
    final GridBagConstraints gbc_comboBoxHosts = new GridBagConstraints();
    gbc_comboBoxHosts.fill = GridBagConstraints.HORIZONTAL;
    gbc_comboBoxHosts.insets = new Insets(0, 0, 5, 5);
    gbc_comboBoxHosts.gridx = 3;
    gbc_comboBoxHosts.gridy = 0;
    frmRClientGui.getContentPane().add(comboBoxHosts, gbc_comboBoxHosts);

    final JLabel label_1 =
        new JLabel(Messages.getString("R66ClientGui.8")); //$NON-NLS-1$
    final GridBagConstraints gbc_label_1 = new GridBagConstraints();
    gbc_label_1.insets = new Insets(0, 0, 5, 5);
    gbc_label_1.gridx = 2;
    gbc_label_1.gridy = 1;
    frmRClientGui.getContentPane().add(label_1, gbc_label_1);

    comboBoxRules = new JComboBox<String>(srules);
    comboBoxRules.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        environnement.guiResultat =
            R66Environment.getRule((String) comboBoxRules.getSelectedItem());
        setStatus(environnement.guiResultat);
      }
    });
    label_1.setLabelFor(comboBoxRules);
    comboBoxRules
        .setToolTipText(Messages.getString("R66ClientGui.9")); //$NON-NLS-1$
    final GridBagConstraints gbc_comboBoxRules = new GridBagConstraints();
    gbc_comboBoxRules.fill = GridBagConstraints.HORIZONTAL;
    gbc_comboBoxRules.insets = new Insets(0, 0, 5, 5);
    gbc_comboBoxRules.gridx = 3;
    gbc_comboBoxRules.gridy = 1;
    frmRClientGui.getContentPane().add(comboBoxRules, gbc_comboBoxRules);

    checkBoxMD5 =
        new JCheckBox(Messages.getString("R66ClientGui.10")); //$NON-NLS-1$
    checkBoxMD5
        .setToolTipText(Messages.getString("R66ClientGui.11")); //$NON-NLS-1$
    final GridBagConstraints gbc_checkBoxMD5 = new GridBagConstraints();
    gbc_checkBoxMD5.insets = new Insets(0, 0, 5, 0);
    gbc_checkBoxMD5.gridx = 4;
    gbc_checkBoxMD5.gridy = 1;
    frmRClientGui.getContentPane().add(checkBoxMD5, gbc_checkBoxMD5);

    final JLabel label_2 =
        new JLabel(Messages.getString("R66ClientGui.0")); //$NON-NLS-1$
    final GridBagConstraints gbc_label_2 = new GridBagConstraints();
    gbc_label_2.insets = new Insets(0, 0, 5, 5);
    gbc_label_2.gridx = 2;
    gbc_label_2.gridy = 2;
    frmRClientGui.getContentPane().add(label_2, gbc_label_2);

    textFieldInformation = new JTextField();
    label_2.setLabelFor(textFieldInformation);
    textFieldInformation
        .setToolTipText(Messages.getString("R66ClientGui.13")); //$NON-NLS-1$
    final GridBagConstraints gbc_textFieldInformation =
        new GridBagConstraints();
    gbc_textFieldInformation.weightx = 1.0;
    gbc_textFieldInformation.fill = GridBagConstraints.HORIZONTAL;
    gbc_textFieldInformation.gridwidth = 2;
    gbc_textFieldInformation.insets = new Insets(0, 0, 5, 0);
    gbc_textFieldInformation.gridx = 3;
    gbc_textFieldInformation.gridy = 2;
    frmRClientGui.getContentPane()
                 .add(textFieldInformation, gbc_textFieldInformation);
    textFieldInformation.setColumns(10);

    final JLabel label_3 =
        new JLabel(Messages.getString("R66ClientGui.14")); //$NON-NLS-1$
    final GridBagConstraints gbc_label_3 = new GridBagConstraints();
    gbc_label_3.insets = new Insets(0, 0, 5, 5);
    gbc_label_3.gridx = 2;
    gbc_label_3.gridy = 3;
    frmRClientGui.getContentPane().add(label_3, gbc_label_3);

    textFieldFile = new JTextField();
    textFieldFile.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(final FocusEvent e) {
        setFindFile();
      }
    });
    label_3.setLabelFor(textFieldFile);
    textFieldFile
        .setToolTipText(Messages.getString("R66ClientGui.15")); //$NON-NLS-1$
    final GridBagConstraints gbc_textFieldFile = new GridBagConstraints();
    gbc_textFieldFile.fill = GridBagConstraints.HORIZONTAL;
    gbc_textFieldFile.insets = new Insets(0, 0, 5, 5);
    gbc_textFieldFile.gridx = 3;
    gbc_textFieldFile.gridy = 3;
    frmRClientGui.getContentPane().add(textFieldFile, gbc_textFieldFile);
    textFieldFile.setColumns(10);

    buttonFileFind =
        new JButton(Messages.getString("R66ClientGui.16")); //$NON-NLS-1$
    buttonFileFind
        .setToolTipText(Messages.getString("R66ClientGui.17")); //$NON-NLS-1$
    buttonFileFind.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        final R66ClientGuiActions action =
            new R66ClientGuiActions(R66ClientGuiActions.FILESELECT);
        action.execute();
      }
    });
    final GridBagConstraints gbc_buttonFileFind = new GridBagConstraints();
    gbc_buttonFileFind.insets = new Insets(0, 0, 5, 0);
    gbc_buttonFileFind.gridx = 4;
    gbc_buttonFileFind.gridy = 3;
    frmRClientGui.getContentPane().add(buttonFileFind, gbc_buttonFileFind);

    buttonTransferStart =
        new JButton(Messages.getString("R66ClientGui.18")); //$NON-NLS-1$
    buttonTransferStart
        .setToolTipText(Messages.getString("R66ClientGui.19")); //$NON-NLS-1$
    buttonTransferStart.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        final R66ClientGuiActions action =
            new R66ClientGuiActions(R66ClientGuiActions.STARTTRANSFER);
        action.execute();
      }
    });
    final GridBagConstraints gbc_buttonTransferStart = new GridBagConstraints();
    gbc_buttonTransferStart.insets = new Insets(0, 0, 5, 5);
    gbc_buttonTransferStart.gridx = 3;
    gbc_buttonTransferStart.gridy = 4;
    frmRClientGui.getContentPane()
                 .add(buttonTransferStart, gbc_buttonTransferStart);

    checkBoxDebug =
        new JCheckBox(Messages.getString("R66ClientGui.20")); //$NON-NLS-1$
    checkBoxDebug.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        environnement.debug(checkBoxDebug.isSelected());
      }
    });
    final GridBagConstraints gbc_checkBoxDebug = new GridBagConstraints();
    gbc_checkBoxDebug.insets = new Insets(0, 0, 5, 5);
    gbc_checkBoxDebug.gridx = 1;
    gbc_checkBoxDebug.gridy = 5;
    environnement.debug(checkBoxDebug.isSelected());
    frmRClientGui.getContentPane().add(checkBoxDebug, gbc_checkBoxDebug);

    progressBarTransfer = new JProgressBar();
    final GridBagConstraints gbc_progressBarTransfer = new GridBagConstraints();
    gbc_progressBarTransfer.weightx = 1.0;
    gbc_progressBarTransfer.fill = GridBagConstraints.HORIZONTAL;
    gbc_progressBarTransfer.insets = new Insets(0, 0, 5, 0);
    gbc_progressBarTransfer.gridwidth = 3;
    gbc_progressBarTransfer.gridx = 2;
    gbc_progressBarTransfer.gridy = 5;
    frmRClientGui.getContentPane()
                 .add(progressBarTransfer, gbc_progressBarTransfer);
    progressBarTransfer.setVisible(false);

    scrollPane_1 = new JScrollPane();
    scrollPane_1.setViewportBorder(
        new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
    final GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
    gbc_scrollPane_1.weighty = 1.0;
    gbc_scrollPane_1.weightx = 1.0;
    gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
    gbc_scrollPane_1.gridwidth = 5;
    gbc_scrollPane_1.insets = new Insets(0, 0, 5, 0);
    gbc_scrollPane_1.gridx = 0;
    gbc_scrollPane_1.gridy = 6;
    frmRClientGui.getContentPane().add(scrollPane_1, gbc_scrollPane_1);

    textFieldStatus = new JEditorPane();
    textFieldStatus
        .setToolTipText(Messages.getString("R66ClientGui.21")); //$NON-NLS-1$
    scrollPane_1.setViewportView(textFieldStatus);
    textFieldStatus.setForeground(Color.GRAY);
    textFieldStatus.setBackground(new Color(255, 255, 153));
    textFieldStatus.setContentType("text/html");
    textFieldStatus.setEditable(false);

    scrollPane = new JScrollPane();
    scrollPane.setViewportBorder(
        new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
    final GridBagConstraints gbc_scrollPane = new GridBagConstraints();
    gbc_scrollPane.weighty = 1.0;
    gbc_scrollPane.weightx = 1.0;
    gbc_scrollPane.fill = GridBagConstraints.BOTH;
    gbc_scrollPane.gridwidth = 5;
    gbc_scrollPane.gridx = 0;
    gbc_scrollPane.gridy = 7;
    frmRClientGui.getContentPane().add(scrollPane, gbc_scrollPane);

    textPaneLog = new JTextArea();
    scrollPane.setViewportView(textPaneLog);
    textPaneLog
        .setToolTipText(Messages.getString("R66ClientGui.23")); //$NON-NLS-1$
    textPaneLog.setEditable(false);

    System.setOut(new PrintStream(new JTextAreaOutputStream(textPaneLog)));
    frmRClientGui.getContentPane().setFocusTraversalPolicy(
        new FocusTraversalOnArray(new Component[] {
            buttonCheckConnection, comboBoxHosts, comboBoxRules, checkBoxMD5,
            textFieldInformation, textFieldFile, buttonFileFind,
            buttonTransferStart
        }));
  }

  /**
   *
   */
  public class R66ClientGuiActions extends SwingWorker<String, Integer> {
    static final int CHECKCONNECTION = 1;
    static final int STARTTRANSFER = 2;
    static final int FILESELECT = 3;
    final int method;

    R66ClientGuiActions(final int method) {
      this.method = method;
      if (logger == null) {
        logger = WaarpLoggerFactory.getLogger(R66ClientGui.class);
      }
    }

    @Override
    protected String doInBackground() throws Exception {
      disableAllButtons();
      startRequest();
      switch (method) {
        case CHECKCONNECTION:
          checkConnection();
          break;
        case STARTTRANSFER:
          startTransfer();
          break;
        case FILESELECT:
          findFile();
          break;
        default:
          environnement.guiResultat =
              Messages.getString("R66ClientGui.24"); //$NON-NLS-1$
      }
      setStatus(environnement.guiResultat);
      if (method != FILESELECT) {
        showDialog();
      } else {
        enableAllButtons();
      }
      stopRequest();
      return environnement.guiResultat;
    }
  }

  private void showDialog() {
    disableAllButtons();
    if (dialog != null) {
      dialog.dispose();
      dialog = null;
    }
    dialog = new R66Dialog();
    dialog.setLocationRelativeTo(frmRClientGui);
    if (dialog.isAlwaysOnTopSupported()) {
      dialog.setAlwaysOnTop(true);
    } else {
      dialog.toFront();
    }
    dialog.textPaneDialog.setText(environnement.guiResultat);
    dialog.setVisible(true);
    dialog.requestFocus();
  }

  private void setStatus(final String mesg) {
    textFieldStatus.setText(mesg);
  }

  private void startRequest() {
    progressBarTransfer.setIndeterminate(true);
    progressBarTransfer.setValue(0);
    progressBarTransfer.setVisible(true);
    textPaneLog.setText("");
  }

  private void stopRequest() {
    progressBarTransfer.setIndeterminate(true);
    progressBarTransfer.setValue(0);
    progressBarTransfer.setVisible(false);
    frmRClientGui.toFront();
    frmRClientGui.requestFocus();
  }

  private void checkConnection() {
    startRequest();
    disableAllButtons();
    environnement.hostId = (String) comboBoxHosts.getSelectedItem();
    if (ParametersChecker.isEmpty(environnement.hostId)) {
      environnement.hostId =
          Messages.getString("R66ClientGui.26"); //$NON-NLS-1$
      environnement.guiResultat =
          Messages.getString("R66ClientGui.27"); //$NON-NLS-1$
    } else {
      environnement.hostId = environnement.hostId.trim();
      environnement.checkConnection();
    }
    setStatus(environnement.guiResultat);
    showDialog();
    stopRequest();
  }

  private void startTransfer() {
    logger.debug("start startTransfer");
    disableAllButtons();
    environnement.hostId = (String) comboBoxHosts.getSelectedItem();
    environnement.ruleId = (String) comboBoxRules.getSelectedItem();
    environnement.filePath = textFieldFile.getText();
    environnement.information = textFieldInformation.getText();
    environnement.isMD5 = checkBoxMD5.isSelected();

    boolean ok = true;
    if (ParametersChecker.isEmpty(environnement.hostId)) {
      environnement.hostId =
          Messages.getString("R66ClientGui.26"); //$NON-NLS-1$
      ok = false;
    } else {
      environnement.hostId = environnement.hostId.trim();
    }
    if (ParametersChecker.isEmpty(environnement.filePath)) {
      environnement.filePath =
          Messages.getString("R66ClientGui.30"); //$NON-NLS-1$
      ok = false;
    } else {
      environnement.filePath = environnement.filePath.trim();
    }
    if (ParametersChecker.isEmpty(environnement.information)) {
      environnement.information = "";
    } else {
      environnement.information = environnement.information.trim();
    }
    if (ParametersChecker.isEmpty(environnement.ruleId)) {
      environnement.ruleId =
          Messages.getString("R66ClientGui.32"); //$NON-NLS-1$
      ok = false;
    } else {
      environnement.ruleId = environnement.ruleId.trim();
    }
    if (ok) {
      logger.debug("start startTransfer: " + environnement);
      environnement.startsTransfer(progressBarTransfer, textFieldStatus);
    } else {
      environnement.guiResultat =
          Messages.getString("R66ClientGui.34") + //$NON-NLS-1$
          Messages.getString("R66ClientGui.35") + environnement.hostId +
          Messages.getString("R66ClientGui.36")
          //$NON-NLS-1$ //$NON-NLS-2$
          + environnement.ruleId + Messages.getString("R66ClientGui.37") +
          environnement.filePath; //$NON-NLS-1$
    }
    setStatus(environnement.guiResultat);
    showDialog();
  }

  private void findFile() {
    startRequest();
    disableAllButtons();
    try {
      final JFileChooser fc = new JFileChooser();
      final int returnVal = fc.showOpenDialog(frmRClientGui);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        final File file = fc.getSelectedFile();
        try {
          textFieldFile.setText(file.getCanonicalPath());
        } catch (final IOException ignored) {
          // nothing
        }
        setFindFile();
        environnement.guiResultat =
            Messages.getString("R66ClientGui.38"); //$NON-NLS-1$
      }
    } finally {
      enableAllButtons();
      stopRequest();
    }
  }

  private void setFindFile() {
    String text;
    try {
      text = textFieldFile.getText();
    } catch (final NullPointerException e1) {
      text = null;
    }
    if (text != null) {
      final File file = new File(text);
      if (file.exists()) {
        text = file.toURI().toString();
      } else {
        // nothing
      }
      textFieldFile.setText(text);
    }
  }

  public void disableAllButtons() {
    // frmRClientGui.setEnabled(false)
    buttonCheckConnection.setEnabled(false);
    buttonFileFind.setEnabled(false);
    buttonTransferStart.setEnabled(false);
    menu.setEnabled(false);
    textFieldInformation.setEnabled(false);
    textFieldFile.setEnabled(false);
    comboBoxHosts.setEnabled(false);
    comboBoxRules.setEnabled(false);
    checkBoxMD5.setEnabled(false);
    checkBoxDebug.setEnabled(false);
  }

  public void enableAllButtons() {
    // frmRClientGui.setEnabled(true)
    buttonCheckConnection.setEnabled(true);
    buttonFileFind.setEnabled(true);
    buttonTransferStart.setEnabled(true);
    menu.setEnabled(true);
    textFieldInformation.setEnabled(true);
    textFieldFile.setEnabled(true);
    comboBoxHosts.setEnabled(true);
    comboBoxRules.setEnabled(true);
    checkBoxMD5.setEnabled(true);
    checkBoxDebug.setEnabled(true);
    frmRClientGui.toFront();
  }

  public static class JTextAreaOutputStream extends OutputStream {
    final JTextArea ta;

    public JTextAreaOutputStream(final JTextArea t) {
      ta = t;
    }

    @Override
    public void write(final int i) {
      ta.append(Character.toString((char) i));
    }

    public void write(final char[] buf, final int off, final int len) {
      final String s = new String(buf, off, len);
      ta.append(s);
    }

  }
}
