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
package org.waarp.administrator;

import org.waarp.administrator.guipwd.AdminUiPassword;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.openr66.client.Message;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.localhandler.packet.TestPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.r66gui.AdminSimpleR66ClientGui;
import org.waarp.openr66.r66gui.R66ClientGui;
import org.waarp.openr66.r66gui.R66Environment;
import org.waarp.openr66.serveraction.AdminR66OperationsGui;
import org.waarp.uip.WaarpPassword;
import org.waarp.xample.AdminXample;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.waarp.openr66.database.DbConstantR66.*;

public class AdminGui {
  /**
   * Internal Logger
   */
  static volatile WaarpLogger logger;

  private JFrame frmWaarpRCentral;
  private final List<AdminXample> xamples = new ArrayList<AdminXample>();
  private final List<AdminUiPassword> passwords =
      new ArrayList<AdminUiPassword>();
  private static final R66Environment environnement = new R66Environment();

  JButton btnEditXml;
  JButton btnCheckPartners;
  JButton btnEditPassword;
  JButton btnManageConfiguration;
  JButton btnQuit;
  JButton btnManageLogs;
  JButton btnFileTransfer;

  protected static boolean getParams(String[] args) {
    ParametersChecker.checkParameter(ParametersChecker.DEFAULT_ERROR, args);
    if (args.length < 1) {
      logger
          .error(Messages.getString("Configuration.NeedConfig")); //$NON-NLS-1$
      final JFileChooser chooser = new JFileChooser();
      final int returnvval = chooser.showOpenDialog(null);
      if (returnvval == JFileChooser.APPROVE_OPTION) {
        final File file = chooser.getSelectedFile();
        args = new String[] { file.getAbsolutePath() };
      } else {
        JOptionPane.showMessageDialog(null, Messages
            .getString("Configuration.NeedConfig")); //$NON-NLS-1$
        return false;
      }
    }
    if (!FileBasedConfiguration
        .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
      logger
          .error(Messages.getString("Configuration.NeedConfig")); //$NON-NLS-1$
      return false;
    }
    Configuration.configuration.pipelineInit();
    getEnvironnement().networkTransaction = new NetworkTransaction();
    return true;
  }

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(AdminGui.class);
    }
    if (!getParams(args)) {
      logger.error(Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
      JOptionPane.showMessageDialog(null, Messages
                                        .getString("Configuration.WrongInit"), //$NON-NLS-1$
                                    "Attention", JOptionPane.WARNING_MESSAGE);
      if (admin != null) {
        admin.close();
      }
      System.exit(1);//NOSONAR
    }
    String[] args2;

    final String pwd_gpp = Configuration.configuration.getServerKeyFile();
    if (pwd_gpp == null) {
      args2 = new String[] {
          "-ki", Configuration.configuration.getCryptoFile(), "-pwd",
          "testpassword", "-clear"
      };
    } else {
      args2 = new String[] {
          "-ki", Configuration.configuration.getCryptoFile(), "-pi", pwd_gpp,
          "-pwd", "testpassword", "-clear"
      };
    }
    WaarpPassword.loadOptions(args2);
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          final AdminGui window = new AdminGui();
          window.frmWaarpRCentral.setVisible(true);
        } catch (final Throwable e) {
          SysErrLogger.FAKE_LOGGER.syserr(e);
        }
      }
    });
  }

  /**
   * Create the application.
   */
  public AdminGui() {
    initialize();
  }

  private void langReinit() {
    frmWaarpRCentral.setTitle(Messages.getString("AdminGui.title") +
                              Configuration.configuration.getHostId());
    btnEditXml.setText(Messages.getString("AdminGui.EditXml"));
    btnCheckPartners.setText(Messages.getString("AdminGui.CheckPartners"));
    btnEditPassword.setText(Messages.getString("AdminGui.EditPassword"));
    btnManageConfiguration.setText(Messages.getString("AdminGui.ManageConfig"));
    btnQuit.setText(Messages.getString("AdminGui.Quit"));
    btnManageLogs.setText(Messages.getString("AdminGui.Hypervision"));
    btnFileTransfer.setText(Messages.getString("AdminGui.FileTransfer"));
  }

  /**
   * Initialize the contents of the frame.
   */
  private void initialize() {
    frmWaarpRCentral = new JFrame();
    frmWaarpRCentral.setTitle("Waarp R66 Central Administrator: " +
                              Configuration.configuration.getHostId());
    frmWaarpRCentral.setBounds(100, 100, 850, 300);
    frmWaarpRCentral.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    final JMenuBar menuBar = new JMenuBar();
    frmWaarpRCentral.setJMenuBar(menuBar);
    final GridBagLayout gridBagLayout = new GridBagLayout();
    gridBagLayout.columnWidths = new int[] { 0, 0, 0, 0 };
    gridBagLayout.rowHeights = new int[] { 0, 0 };
    gridBagLayout.columnWeights =
        new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
    gridBagLayout.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
    frmWaarpRCentral.getContentPane().setLayout(gridBagLayout);

    final JToolBar toolBar = new JToolBar();
    final GridBagConstraints constraints = new GridBagConstraints();
    constraints.insets = new Insets(0, 0, 0, 5);
    constraints.gridy = 0;
    constraints.gridx = 0;
    frmWaarpRCentral.getContentPane().add(toolBar, constraints);

    btnEditXml = new JButton("Edit XML");
    btnEditXml.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        AdminXample.start(xamples);
      }
    });

    btnCheckPartners = new JButton("Check Partners");
    btnCheckPartners.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        // Check all Known Partners
        try {
          TestPacket packet;
          String myhost;
          try {
            myhost = getEnvironnement().hostId == null?
                InetAddress.getLocalHost().getHostName() :
                getEnvironnement().hostId;
          } catch (final UnknownHostException e) {
            myhost = Messages.getString("AdminGui.NameUnknown");
          }
          packet =
              new TestPacket("MSG", "Administrator checking from " + myhost,
                             100);
          packet.retain();
          StringBuilder result =
              new StringBuilder(Messages.getString("AdminGui.CheckedHosts"));
          for (final DbHostAuth host : DbHostAuth.getAllHosts()) {
            final R66Future future = new R66Future(true);
            final Message mesg =
                new Message(getEnvironnement().networkTransaction, future, host,
                            packet);
            mesg.run();
            packet.retain();
            future.awaitOrInterruptible();
            if (future.isSuccess()) {
              result.append("OK: ").append(host).append('\n');
            } else {
              result.append("KO: ").append(host).append('\n');
            }
          }
          packet.clear();

          JOptionPane.showMessageDialog(null, result.toString());
        } catch (final WaarpDatabaseNoConnectionException e) {
          SysErrLogger.FAKE_LOGGER.syserr(e);
        }
      }
    });
    toolBar.add(btnCheckPartners);
    toolBar.add(btnEditXml);

    btnEditPassword = new JButton("Edit Password");
    btnEditPassword.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            AdminUiPassword inst;
            try {
              inst = new AdminUiPassword(passwords);
            } catch (final Exception e) {
              SysErrLogger.FAKE_LOGGER.syserr(e);
              return;
            }
            inst.setLocationRelativeTo(null);
            inst.setVisible(true);
          }
        });
      }
    });
    toolBar.add(btnEditPassword);

    frmWaarpRCentral.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        quit();
      }
    });
    frmWaarpRCentral
        .setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    btnQuit = new JButton("QUIT");
    btnQuit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        quit();
      }
    });

    btnManageConfiguration = new JButton("Manage Configuration");
    btnManageConfiguration.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EventQueue.invokeLater(new Runnable() {
          @Override
          public void run() {
            if (AdminR66OperationsGui.getWindow() != null) {
              AdminR66OperationsGui.getWindow().dispose();
              AdminR66OperationsGui.setWindow(null);
            }
            try {
              AdminR66OperationsGui
                  .setWindow(new AdminR66OperationsGui(frmWaarpRCentral));
              AdminR66OperationsGui.getWindow().setVisible(true);
            } catch (final Exception e) {
              SysErrLogger.FAKE_LOGGER.syserr(e);
            }
          }
        });
      }
    });
    toolBar.add(btnManageConfiguration);

    btnManageLogs = new JButton("Hypervision");
    btnManageLogs.setEnabled(false);
    toolBar.add(btnManageLogs);

    btnFileTransfer = new JButton("File Transfer");
    btnFileTransfer.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        EventQueue.invokeLater(new Runnable() {
          @Override
          public void run() {
            if (R66ClientGui.window != null) {
              R66ClientGui.window.frmRClientGui.dispose();
              R66ClientGui.window = null;
            }
            try {
              R66ClientGui.window = new AdminSimpleR66ClientGui();
              R66ClientGui.window.frmRClientGui.setVisible(true);
            } catch (final Exception e) {
              SysErrLogger.FAKE_LOGGER.syserr(e);
            }
          }
        });
      }
    });
    toolBar.add(btnFileTransfer);
    toolBar.add(btnQuit);

    final JButton btnFr = new JButton("");
    btnFr.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Messages.init(new Locale("fr"));
        langReinit();
      }
    });
    btnFr.setMargin(new Insets(2, 2, 2, 2));
    final URL fr = AdminGui.class.getResource("/fr.png");
    if (fr != null) {
      btnFr.setIcon(new ImageIcon(fr));
    } else {
      btnFr.setText("FR");
    }
    btnFr.setToolTipText("FR");
    final GridBagConstraints gbcBtnFr = new GridBagConstraints();
    gbcBtnFr.insets = new Insets(0, 0, 0, 5);
    gbcBtnFr.gridx = 1;
    gbcBtnFr.gridy = 0;
    frmWaarpRCentral.getContentPane().add(btnFr, gbcBtnFr);

    final JButton btnEn = new JButton("");
    btnEn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Messages.init(new Locale("en"));
        langReinit();
      }
    });
    btnEn.setToolTipText("EN");
    final URL en = AdminGui.class.getResource("/en.png");
    if (en != null) {
      btnEn.setIcon(new ImageIcon(en));
    } else {
      btnEn.setText("EN");
    }
    btnEn.setMargin(new Insets(2, 2, 2, 2));
    final GridBagConstraints gbcBtnEn = new GridBagConstraints();
    gbcBtnEn.gridx = 2;
    gbcBtnEn.gridy = 0;
    frmWaarpRCentral.getContentPane().add(btnEn, gbcBtnEn);
  }

  /**
   * Quit application
   */
  private void quit() {
    final List<AdminXample> list = new ArrayList<AdminXample>();
    for (final AdminXample xample : xamples) {
      if (xample.isStillLaunched()) {
        list.add(xample);
      }
    }
    for (final AdminXample xample : list) {
      xample.exit();
    }
    final List<AdminUiPassword> list2 = new ArrayList<AdminUiPassword>();
    list2.addAll(passwords);
    for (final AdminUiPassword pwd : list2) {
      pwd.exit(null);
    }
    if (R66ClientGui.window != null) {
      R66ClientGui.window.frmRClientGui.dispose();
      R66ClientGui.window = null;
    }
    frmWaarpRCentral.dispose();
    System.exit(0);//NOSONAR
  }

  /**
   * @return the environnement
   */
  public static R66Environment getEnvironnement() {
    return environnement;
  }
}
