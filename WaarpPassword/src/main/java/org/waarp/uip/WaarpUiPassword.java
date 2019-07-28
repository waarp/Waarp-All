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
package org.waarp.uip;

import org.waarp.common.crypto.Blowfish;
import org.waarp.common.crypto.Des;
import org.waarp.common.exception.CryptoException;
import org.waarp.common.logging.SysErrLogger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * GUI for GoldenGate Password Management for GoldenGate products.
 * <p>
 * This code was edited or generated using CloudGarden's Jigloo SWT/Swing GUI
 * Builder, which is free for
 * non-commercial use. If Jigloo is being used commercially (ie, by a
 * corporation, company or business for any
 * purpose whatever) then you should purchase a license for each developer using
 * Jigloo. Please visit
 * www.cloudgarden.com for details. Use of Jigloo implies acceptance of these
 * licensing terms. A COMMERCIAL
 * LICENSE HAS NOT BEEN PURCHASED FOR THIS MACHINE, SO JIGLOO OR THIS CODE
 * CANNOT BE USED LEGALLY FOR ANY
 * CORPORATE OR COMMERCIAL PURPOSE.
 */
public class WaarpUiPassword extends JFrame {

  /**
   *
   */
  private static final long serialVersionUID = -4276191288898099362L;
  protected WaarpUiPassword myself;
  private JMenuItem helpMenuItem;
  private JMenu jMenu5;
  private AbstractAction saveAsKeyAction;
  private AbstractAction saveKeyAction;
  private AbstractAction newKeyAction;
  private AbstractAction openKeyAction;
  private AbstractAction loadPswdAction;
  private AbstractAction exitAction;
  private JPanel jPanel1;
  private JTextField jTextFieldPasswordFile;
  private JPasswordField jPasswordField;
  private JTextField jPasswordFieldTxt;
  private JTextField jTextFieldCryptedPassword;
  private JTextField jTextFieldKeyFile;
  private JMenuItem savePasswordMenuItem;
  private AbstractAction helpAction;
  private AbstractAction savePaswdAction;
  private JMenuItem loadPasswordMenuItem;
  private JMenu jMenuPassword;
  private JMenuItem exitMenuItem;
  private JSeparator jSeparator2;
  private JMenuItem saveAsMenuItem;
  private JMenuItem saveMenuItem;
  private JMenuItem openFileMenuItem;
  private JMenuItem newFileMenuItem;
  private JMenu jMenu3;
  private JMenuBar jMenuBar1;

  private final JFileChooser chooserKeyFile;
  private final FileNameExtensionFilter filterKey;
  private final JFileChooser chooserPwdFile;
  private final FileNameExtensionFilter filterPwdKey;

  private AbstractAction closeHelpAction;
  private JTextPane jTextPaneHelp;
  private JButton jButtonHelp;
  private JDialog jDialogHelp;
  private WaarpPassword waarpPassword;
  boolean passwordModified;
  boolean extended;

  /**
   * Auto-generated main method to display this JFrame
   */
  public static void main(String[] args) {
    if (!WaarpPassword.loadOptions(args)) {
      // Bad options
      System.exit(2);//NOSONAR
    }
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        final WaarpUiPassword inst = new WaarpUiPassword();
        inst.setLocationRelativeTo(null);
        inst.setVisible(true);
      }
    });
  }

  protected WaarpUiPassword(boolean extended_ignore) throws Exception {
    extended = true;
    chooserKeyFile = new JFileChooser();
    filterKey = new FileNameExtensionFilter(
        "DES or Blowfish Key (" + Des.EXTENSION + ", " + Blowfish.EXTENSION +
        ')', Des.EXTENSION, Blowfish.EXTENSION);
    chooserKeyFile.setFileFilter(filterKey);
    chooserKeyFile.setFileSelectionMode(JFileChooser.FILES_ONLY);

    chooserPwdFile = new JFileChooser();
    filterPwdKey = new FileNameExtensionFilter(
        "GoldenGate Password Files (" + WaarpPassword.GGPEXTENSION + ')',
        WaarpPassword.GGPEXTENSION);
    chooserPwdFile.setFileFilter(filterPwdKey);
    chooserPwdFile.setFileSelectionMode(JFileChooser.FILES_ONLY);

    waarpPassword = new WaarpPassword();

    initGUI();
    initFromGgPassword();
  }

  public WaarpUiPassword() {
    myself = this;
    chooserKeyFile = new JFileChooser();
    filterKey = new FileNameExtensionFilter(
        "DES or Blowfish Key (" + Des.EXTENSION + ", " + Blowfish.EXTENSION +
        ')', Des.EXTENSION, Blowfish.EXTENSION);
    chooserKeyFile.setFileFilter(filterKey);
    chooserKeyFile.setFileSelectionMode(JFileChooser.FILES_ONLY);

    chooserPwdFile = new JFileChooser();
    filterPwdKey = new FileNameExtensionFilter(
        "GoldenGate Password Files (" + WaarpPassword.GGPEXTENSION + ')',
        WaarpPassword.GGPEXTENSION);
    chooserPwdFile.setFileFilter(filterPwdKey);
    chooserPwdFile.setFileSelectionMode(JFileChooser.FILES_ONLY);

    try {
      waarpPassword = new WaarpPassword();
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      System.exit(3);//NOSONAR
    }

    initGUI();
    initFromGgPassword();
  }

  private void initFromGgPassword() {
    final File keyFile = waarpPassword.getKeyFile();
    if (keyFile != null) {
      jTextFieldKeyFile.setText(keyFile.getAbsolutePath());
      enableMenuWithKey();
    }
    final File pFile = waarpPassword.getPasswordFile();
    if (pFile != null) {
      jTextFieldPasswordFile.setText(pFile.getAbsolutePath());
    }
    String clpwd = waarpPassword.getClearPassword();
    if (clpwd != null) {
      setUncryptedPassword(clpwd);
      clpwd = waarpPassword.getCryptedPassword();
      jTextFieldCryptedPassword.setText(clpwd);
    }
  }

  public void exit(ActionEvent evt) {
    if (passwordModified) {
      getSavePaswdAction().actionPerformed(evt);
    }
    if (extended) {
      myself.dispose();
    } else {
      System.exit(1);//NOSONAR
    }
  }

  private void initGUI() {
    try {
      final GroupLayout thisLayout = new GroupLayout(getContentPane());
      getContentPane().setLayout(thisLayout);
      addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent evt) {
          myself.exit(null);
        }

        @Override
        public void windowClosed(WindowEvent evt) {
          myself.exit(null);
        }
      });
      setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      setTitle("Waarp Password GUI");
      setLocale(new Locale("en", "GB"));
      {
        jTextFieldKeyFile = new JTextField();
        jTextFieldKeyFile.setText("Key File");
        jTextFieldKeyFile.setEditable(false);
        jTextFieldKeyFile.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(0, 0, 0)),
            BorderFactory.createCompoundBorder(null, null)));
      }
      {
        jPanel1 = new JPanel();
        final GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        {
          jTextFieldPasswordFile = new JTextField();
          jTextFieldPasswordFile.setText("Password File");
          jTextFieldPasswordFile.setEditable(false);
          jTextFieldPasswordFile.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(0, 0, 0)),
              BorderFactory.createCompoundBorder(null, null)));
        }
        {
          if (WaarpPassword.clearPasswordView) {
            jPasswordFieldTxt = new JTextField();
            jPasswordFieldTxt.addFocusListener(new FocusAdapter() {
              @Override
              public void focusLost(FocusEvent evt) {
                final String paswd = jPasswordFieldTxt.getText();
                if (!paswd.equals(waarpPassword.getClearPassword())) {
                  try {
                    waarpPassword.setClearPassword(paswd);
                  } catch (final Exception e) {
                    SysErrLogger.FAKE_LOGGER.syserr(e);
                  }
                  jTextFieldCryptedPassword
                      .setText(waarpPassword.getCryptedPassword());
                  passwordModified = true;
                }
              }
            });
          } else {
            jPasswordField = new JPasswordField();
            jPasswordField.addFocusListener(new FocusAdapter() {
              @Override
              public void focusLost(FocusEvent evt) {
                final String paswd = new String(jPasswordField.getPassword());
                if (!paswd.equals(waarpPassword.getClearPassword())) {
                  try {
                    waarpPassword.setClearPassword(paswd);
                  } catch (final Exception e) {
                    SysErrLogger.FAKE_LOGGER.syserr(e);
                  }
                  jTextFieldCryptedPassword
                      .setText(waarpPassword.getCryptedPassword());
                  passwordModified = true;
                }
              }
            });
          }
        }
        {
          jTextFieldCryptedPassword = new JTextField();
          jTextFieldCryptedPassword.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent evt) {
              final String paswd = jTextFieldCryptedPassword.getText();
              if (!paswd.equals(waarpPassword.getCryptedPassword())) {
                try {
                  waarpPassword.setCryptedPassword(paswd);
                } catch (final Exception e1) {
                  SysErrLogger.FAKE_LOGGER.syserr(e1);
                }
                setUncryptedPassword(waarpPassword.getClearPassword());
                jTextFieldCryptedPassword
                    .setText(waarpPassword.getCryptedPassword());
                passwordModified = true;
              }
            }
          });
        }
        if (WaarpPassword.clearPasswordView) {
          jPanel1Layout.setHorizontalGroup(
              jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(
                  jPanel1Layout.createParallelGroup().addGroup(
                      jPanel1Layout.createSequentialGroup()
                                   .addComponent(jTextFieldPasswordFile,
                                                 GroupLayout.PREFERRED_SIZE,
                                                 340,
                                                 GroupLayout.PREFERRED_SIZE)
                                   .addGap(0, 0, Short.MAX_VALUE))
                               .addComponent(jPasswordFieldTxt,
                                             GroupLayout.Alignment.LEADING, 0,
                                             341, Short.MAX_VALUE)
                               .addComponent(jTextFieldCryptedPassword,
                                             GroupLayout.Alignment.LEADING, 0,
                                             338, Short.MAX_VALUE))
                           .addContainerGap());
          jPanel1Layout.setVerticalGroup(
              jPanel1Layout.createSequentialGroup().addContainerGap()
                           .addComponent(jTextFieldPasswordFile,
                                         GroupLayout.PREFERRED_SIZE, 28,
                                         GroupLayout.PREFERRED_SIZE).addGap(18)
                           .addComponent(jPasswordFieldTxt,
                                         GroupLayout.PREFERRED_SIZE, 26,
                                         GroupLayout.PREFERRED_SIZE)
                           .addPreferredGap(
                               LayoutStyle.ComponentPlacement.UNRELATED, 1,
                               Short.MAX_VALUE)
                           .addComponent(jTextFieldCryptedPassword,
                                         GroupLayout.PREFERRED_SIZE, 28,
                                         GroupLayout.PREFERRED_SIZE)
                           .addContainerGap());
        } else {
          jPanel1Layout.setHorizontalGroup(
              jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(
                  jPanel1Layout.createParallelGroup().addGroup(
                      jPanel1Layout.createSequentialGroup()
                                   .addComponent(jTextFieldPasswordFile,
                                                 GroupLayout.PREFERRED_SIZE,
                                                 340,
                                                 GroupLayout.PREFERRED_SIZE)
                                   .addGap(0, 0, Short.MAX_VALUE))
                               .addComponent(jPasswordField,
                                             GroupLayout.Alignment.LEADING, 0,
                                             341, Short.MAX_VALUE)
                               .addComponent(jTextFieldCryptedPassword,
                                             GroupLayout.Alignment.LEADING, 0,
                                             338, Short.MAX_VALUE))
                           .addContainerGap());
          jPanel1Layout.setVerticalGroup(
              jPanel1Layout.createSequentialGroup().addContainerGap()
                           .addComponent(jTextFieldPasswordFile,
                                         GroupLayout.PREFERRED_SIZE, 28,
                                         GroupLayout.PREFERRED_SIZE).addGap(18)
                           .addComponent(jPasswordField,
                                         GroupLayout.PREFERRED_SIZE, 26,
                                         GroupLayout.PREFERRED_SIZE)
                           .addPreferredGap(
                               LayoutStyle.ComponentPlacement.UNRELATED, 1,
                               Short.MAX_VALUE)
                           .addComponent(jTextFieldCryptedPassword,
                                         GroupLayout.PREFERRED_SIZE, 28,
                                         GroupLayout.PREFERRED_SIZE)
                           .addContainerGap());
        }
      }
      thisLayout.setVerticalGroup(
          thisLayout.createSequentialGroup().addContainerGap()
                    .addComponent(jTextFieldKeyFile, GroupLayout.PREFERRED_SIZE,
                                  28, GroupLayout.PREFERRED_SIZE).addGap(21)
                    .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, 143,
                                  GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(31, Short.MAX_VALUE));
      thisLayout.setHorizontalGroup(
          thisLayout.createSequentialGroup().addContainerGap().addGroup(
              thisLayout.createParallelGroup()
                        .addComponent(jPanel1, GroupLayout.Alignment.LEADING, 0,
                                      366, Short.MAX_VALUE)
                        .addGroup(GroupLayout.Alignment.LEADING,
                                  thisLayout.createSequentialGroup().addGap(7)
                                            .addComponent(jTextFieldKeyFile, 0,
                                                          353, Short.MAX_VALUE)
                                            .addGap(6))).addGap(6));
      setSize(400, 300);
      {
        jMenuBar1 = new JMenuBar();
        setJMenuBar(jMenuBar1);
        {
          jMenu3 = new JMenu();
          jMenuBar1.add(jMenu3);
          jMenu3.setText("File");
          {
            openFileMenuItem = new JMenuItem();
            jMenu3.add(openFileMenuItem);
            openFileMenuItem.setText("Open Key");
            openFileMenuItem.setAction(getOpenKeyAction());
          }
          {
            newFileMenuItem = new JMenuItem();
            jMenu3.add(newFileMenuItem);
            newFileMenuItem.setText("New Key");
            newFileMenuItem.setAction(getNewKeyAction());
          }
          {
            saveMenuItem = new JMenuItem();
            jMenu3.add(saveMenuItem);
            saveMenuItem.setText("Save Key");
            saveMenuItem.setAction(getSaveKeyAction());
            saveMenuItem.setEnabled(false);
          }
          {
            saveAsMenuItem = new JMenuItem();
            jMenu3.add(saveAsMenuItem);
            saveAsMenuItem.setText("Save Key As ...");
            saveAsMenuItem.setAction(getSaveAsKeyAction());
            saveAsMenuItem.setEnabled(false);
          }
          {
            jSeparator2 = new JSeparator();
            jMenu3.add(jSeparator2);
          }
          {
            exitMenuItem = new JMenuItem();
            jMenu3.add(exitMenuItem);
            exitMenuItem.setText("Exit");
            exitMenuItem.setAction(getExitAction());
          }
        }
        {
          jMenuPassword = new JMenu();
          jMenuBar1.add(jMenuPassword);
          jMenuPassword.setText("Password");
          jMenuPassword.setEnabled(false);
          {
            loadPasswordMenuItem = new JMenuItem();
            jMenuPassword.add(loadPasswordMenuItem);
            loadPasswordMenuItem.setText("Load");
            loadPasswordMenuItem.setAction(getLoadPswdAction());
          }
          {
            savePasswordMenuItem = new JMenuItem();
            jMenuPassword.add(savePasswordMenuItem);
            savePasswordMenuItem.setText("Save");
            savePasswordMenuItem.setAction(getSavePaswdAction());
          }
        }
        {
          jMenu5 = new JMenu();
          jMenuBar1.add(jMenu5);
          jMenu5.setText("Help");
          {
            helpMenuItem = new JMenuItem();
            jMenu5.add(helpMenuItem);
            helpMenuItem.setText("Help");
            helpMenuItem.setAction(getHelpAction());
          }
        }
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
    }
  }

  private AbstractAction getExitAction() {
    if (exitAction == null) {
      exitAction = new AbstractAction("Exit", null) {
        /**
         *
         */
        private static final long serialVersionUID = 7484447837851868127L;

        @Override
        public void actionPerformed(ActionEvent evt) {
          myself.exit(evt);
        }
      };
    }
    return exitAction;
  }

  private String getUncryptedPassword() {
    if (WaarpPassword.clearPasswordView) {
      return jPasswordFieldTxt.getText();
    } else {
      return new String(jPasswordField.getPassword());
    }
  }

  private void setUncryptedPassword(String passwd) {
    if (WaarpPassword.clearPasswordView) {
      jPasswordFieldTxt.setText(passwd);
    } else {
      jPasswordField.setText(passwd);
    }
  }

  private void updateCryptedPassword() {
    try {
      waarpPassword.setClearPassword(getUncryptedPassword());
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
    }
    jTextFieldCryptedPassword.setText(waarpPassword.getCryptedPassword());
    passwordModified = true;
  }

  private void enableMenuWithKey() {
    saveAsMenuItem.setEnabled(true);
    saveMenuItem.setEnabled(true);
    jMenuPassword.setEnabled(true);
  }

  private AbstractAction getOpenKeyAction() {
    if (openKeyAction == null) {
      openKeyAction = new AbstractAction("Open Key", null) {
        /**
         *
         */
        private static final long serialVersionUID = -3840578648343599999L;

        @Override
        public void actionPerformed(ActionEvent evt) {
          jTextFieldPasswordFile.setText("");
          jTextFieldCryptedPassword.setText("");
          chooserKeyFile.setDialogType(JFileChooser.OPEN_DIALOG);
          final int response = chooserKeyFile.showOpenDialog(rootPane);
          if (response == JFileChooser.APPROVE_OPTION) {
            final File file = chooserKeyFile.getSelectedFile();
            jTextFieldKeyFile.setText(file.getAbsolutePath());
            try {
              waarpPassword.loadKey(file);
              updateCryptedPassword();
              enableMenuWithKey();
            } catch (final CryptoException e) {
              SysErrLogger.FAKE_LOGGER.syserr(e);
            }
          }
        }
      };
    }
    return openKeyAction;
  }

  private AbstractAction getNewKeyAction() {
    if (newKeyAction == null) {
      newKeyAction = new AbstractAction("New Key", null) {
        /**
         *
         */
        private static final long serialVersionUID = 10107370851157816L;

        @Override
        public void actionPerformed(ActionEvent evt) {
          try {
            waarpPassword.createNewKey();
          } catch (final Exception e) {
            SysErrLogger.FAKE_LOGGER.syserr(e);
          }
          updateCryptedPassword();
          enableMenuWithKey();
        }
      };
    }
    return newKeyAction;
  }

  private AbstractAction getSaveKeyAction() {
    if (saveKeyAction == null) {
      saveKeyAction = new AbstractAction("Save Key", null) {
        /**
         *
         */
        private static final long serialVersionUID = 4400661431842092244L;

        @Override
        public void actionPerformed(ActionEvent evt) {
          try {
            waarpPassword.saveKey(null);
          } catch (final CryptoException e) {
            SysErrLogger.FAKE_LOGGER.syserr(e);
          }
          passwordModified = false;
        }
      };
    }
    return saveKeyAction;
  }

  private AbstractAction getSaveAsKeyAction() {
    if (saveAsKeyAction == null) {
      saveAsKeyAction = new AbstractAction("Save Key As...", null) {
        /**
         *
         */
        private static final long serialVersionUID = 565842888109816432L;

        @Override
        public void actionPerformed(ActionEvent evt) {
          chooserKeyFile.setDialogType(JFileChooser.SAVE_DIALOG);
          final int response = chooserKeyFile.showSaveDialog(rootPane);
          if (response == JFileChooser.APPROVE_OPTION) {
            File file = chooserKeyFile.getSelectedFile();
            if (!filterKey.accept(file)) {
              if (WaarpPassword.desModel) {
                file = new File(file.getAbsoluteFile() + "." + Des.EXTENSION);
              } else {
                file =
                    new File(file.getAbsoluteFile() + "." + Blowfish.EXTENSION);
              }
            }
            jTextFieldKeyFile.setText(file.getAbsolutePath());
            try {
              waarpPassword.saveKey(file);
            } catch (final CryptoException e) {
              SysErrLogger.FAKE_LOGGER.syserr(e);
            }
            passwordModified = false;
          }
        }
      };
    }
    return saveAsKeyAction;
  }

  private AbstractAction getLoadPswdAction() {
    if (loadPswdAction == null) {
      loadPswdAction = new AbstractAction("Load", null) {
        /**
         *
         */
        private static final long serialVersionUID = -3100726446117809610L;

        @Override
        public void actionPerformed(ActionEvent evt) {
          chooserPwdFile.setDialogType(JFileChooser.OPEN_DIALOG);
          final int response = chooserPwdFile.showOpenDialog(rootPane);
          if (response == JFileChooser.APPROVE_OPTION) {
            final File file = chooserPwdFile.getSelectedFile();
            jTextFieldPasswordFile.setText(file.getAbsolutePath());
            waarpPassword.setPasswordFile(file);
            try {
              waarpPassword.loadPasswordFile();
            } catch (final CryptoException e) {
              SysErrLogger.FAKE_LOGGER.syserr(e);
            } catch (final Exception e) {
              SysErrLogger.FAKE_LOGGER.syserr(e);
            }
            setUncryptedPassword(waarpPassword.getClearPassword());
            jTextFieldCryptedPassword
                .setText(waarpPassword.getCryptedPassword());
            passwordModified = false;
          }
        }
      };
    }
    return loadPswdAction;
  }

  private AbstractAction getSavePaswdAction() {
    if (savePaswdAction == null) {
      savePaswdAction = new AbstractAction("Save", null) {
        /**
         *
         */
        private static final long serialVersionUID = -5712120697650940845L;

        @Override
        public void actionPerformed(ActionEvent evt) {
          chooserPwdFile.setDialogType(JFileChooser.SAVE_DIALOG);
          final int response = chooserPwdFile.showSaveDialog(rootPane);
          if (response == JFileChooser.APPROVE_OPTION) {
            File file = chooserPwdFile.getSelectedFile();
            if (!filterPwdKey.accept(file)) {
              file = new File(
                  file.getAbsoluteFile() + "." + WaarpPassword.GGPEXTENSION);
            }
            jTextFieldPasswordFile.setText(file.getAbsolutePath());
            waarpPassword.setPasswordFile(file);
            try {
              waarpPassword.savePasswordFile();
            } catch (final IOException e) {
              SysErrLogger.FAKE_LOGGER.syserr(e);
            }
            passwordModified = false;
          }
        }
      };
    }
    return savePaswdAction;
  }

  private AbstractAction getHelpAction() {
    if (helpAction == null) {
      helpAction = new AbstractAction("Help", null) {
        /**
         *
         */
        private static final long serialVersionUID = -8528091873190483826L;

        @Override
        public void actionPerformed(ActionEvent evt) {
          getJDialogHelp().setVisible(true);
        }
      };
    }
    return helpAction;
  }

  private JDialog getJDialogHelp() {
    if (jDialogHelp == null) {
      jDialogHelp = new JDialog(this);
      final GroupLayout jDialogHelpLayout =
          new GroupLayout(jDialogHelp.getContentPane());
      jDialogHelp.getContentPane().setLayout(jDialogHelpLayout);
      jDialogHelp.getContentPane().setBackground(new Color(255, 255, 255));
      jDialogHelp.setPreferredSize(new Dimension(670, 479));
      jDialogHelp.setSize(670, 479);
      jDialogHelpLayout.setHorizontalGroup(
          jDialogHelpLayout.createSequentialGroup().addContainerGap(27, 27)
                           .addGroup(jDialogHelpLayout.createParallelGroup()
                                                      .addComponent(
                                                          getJTextPaneHelp(),
                                                          GroupLayout.Alignment.LEADING,
                                                          0, 604,
                                                          Short.MAX_VALUE)
                                                      .addGroup(
                                                          GroupLayout.Alignment.LEADING,
                                                          jDialogHelpLayout
                                                              .createSequentialGroup()
                                                              .addGap(254)
                                                              .addComponent(
                                                                  getJButtonHelp(),
                                                                  GroupLayout.PREFERRED_SIZE,
                                                                  111,
                                                                  GroupLayout.PREFERRED_SIZE)
                                                              .addGap(0, 239,
                                                                      Short.MAX_VALUE)))
                           .addContainerGap(23, 23));
      jDialogHelpLayout.setVerticalGroup(
          jDialogHelpLayout.createSequentialGroup().addContainerGap()
                           .addComponent(getJTextPaneHelp(), 0, 395,
                                         Short.MAX_VALUE)
                           .addComponent(getJButtonHelp(),
                                         GroupLayout.PREFERRED_SIZE,
                                         GroupLayout.PREFERRED_SIZE,
                                         GroupLayout.PREFERRED_SIZE)
                           .addContainerGap());
    }
    return jDialogHelp;
  }

  private JButton getJButtonHelp() {
    if (jButtonHelp == null) {
      jButtonHelp = new JButton();
      jButtonHelp.setText("OK");
      jButtonHelp.setAction(getCloseHelpAction());
    }
    return jButtonHelp;
  }

  private JTextPane getJTextPaneHelp() {
    if (jTextPaneHelp == null) {
      jTextPaneHelp = new JTextPane();
      jTextPaneHelp.setText("                                            " +
                            "                            GoldenGate GUI Password Tool\r\n\r\n" +
                            "A) Key for crypto support\r\n\r\n" +
                            "* First you need to open a Key File (Open) or to create a " +
                            "New Key File (New Key) and to save it (Save Key As...)\r\n" +
                            "* You can at any moment save the Key File to a new " +
                            "Key File (Save Key As...)\r\n\r\n" +
                            "B) Password with Crypto Key\r\n" +
                            "* Then you need to get a Password:\r\n" +
                            "1) Enter a password in the Password field\r\n" +
                            "2) Load a password from a Password file (compatible with " +
                            "the current Key) (Load)\r\n" +
                            "* Then you need to save this password (encrypted) to " +
                            "a File (Save)\r\n\r\n" + "C) " +
                            WaarpPassword.HELPOPTIONS);
      jTextPaneHelp.setEditable(false);
      jTextPaneHelp.setBackground(new Color(255, 255, 255));
    }
    return jTextPaneHelp;
  }

  private AbstractAction getCloseHelpAction() {
    if (closeHelpAction == null) {
      closeHelpAction = new AbstractAction("Close", null) {
        /**
         *
         */
        private static final long serialVersionUID = -2310092372166868386L;

        @Override
        public void actionPerformed(ActionEvent evt) {
          getJDialogHelp().dispose();
        }
      };
    }
    return closeHelpAction;
  }

}
