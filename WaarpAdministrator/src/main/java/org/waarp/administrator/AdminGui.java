/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.administrator;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import java.awt.GridBagLayout;
import javax.swing.JToolBar;
import javax.swing.JButton;

import org.waarp.administrator.guipwd.AdminUiPassword;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.openr66.client.Message;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.localhandler.packet.TestPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.r66gui.AdminSimpleR66ClientGui;
import org.waarp.openr66.r66gui.R66Environment;
import org.waarp.openr66.serveraction.AdminR66OperationsGui;
import org.waarp.uip.WaarpPassword;
import org.waarp.xample.AdminXample;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.awt.Insets;
import javax.swing.ImageIcon;

public class AdminGui {
    /**
     * Internal Logger
     */
    static volatile WaarpLogger logger;

    private JFrame frmWaarpRCentral;
    private List<AdminXample> xamples = new ArrayList<AdminXample>();
    private List<AdminUiPassword> passwords = new ArrayList<AdminUiPassword>();
    private static R66Environment environnement = new R66Environment();

    JButton btnEditXml;
    JButton btnCheckPartners;
    JButton btnEditPassword;
    JButton btnManageConfiguration;
    JButton btnQuit;
    JButton btnManageLogs;
    JButton btnFileTransfer;

    protected static boolean getParams(String[] args) {
        if (args.length < 1) {
            logger.error(Messages.getString("Configuration.NeedConfig")); //$NON-NLS-1$
            JFileChooser chooser = new JFileChooser();
            int returnvval = chooser.showOpenDialog(null);
            if (returnvval == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                args = new String[] { file.getAbsolutePath() };
            } else {
                JOptionPane.showMessageDialog(null, Messages.getString("Configuration.NeedConfig")); //$NON-NLS-1$
                return false;
            }
        }
        if (!FileBasedConfiguration
                .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
            logger.error(Messages.getString("Configuration.NeedConfig")); //$NON-NLS-1$
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
        WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
        if (logger == null) {
            logger = WaarpLoggerFactory.getLogger(AdminGui.class);
        }
        if (!getParams(args)) {
            logger.error(Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
            JOptionPane.showMessageDialog(null, Messages.getString("Configuration.WrongInit"), //$NON-NLS-1$
                    "Attention", JOptionPane.WARNING_MESSAGE);
            if (DbConstant.admin != null && DbConstant.admin.isActive()) {
                DbConstant.admin.close();
            }
            System.exit(1);
        }
        String[] args2;

        String pwd_gpp = Configuration.configuration.getServerKeyFile();
        if (pwd_gpp == null) {
            args2 = new String[] {
                    "-ki",
                    Configuration.configuration.getCryptoFile(),
                    "-pwd",
                    "testpassword",
                    "-clear"
            };
        } else {
            args2 = new String[] {
                    "-ki",
                    Configuration.configuration.getCryptoFile(),
                    "-pi",
                    pwd_gpp,
                    "-pwd",
                    "testpassword",
                    "-clear"
            };
        }
        WaarpPassword.loadOptions(args2);
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    AdminGui window = new AdminGui();
                    window.frmWaarpRCentral.setVisible(true);
                } catch (Throwable e) {
                    e.printStackTrace();
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
        frmWaarpRCentral.setTitle(Messages.getString("AdminGui.title") + Configuration.configuration.getHOST_ID());
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
        frmWaarpRCentral.setTitle("Waarp R66 Central Administrator: " + Configuration.configuration.getHOST_ID());
        frmWaarpRCentral.setBounds(100, 100, 850, 300);
        frmWaarpRCentral.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();
        frmWaarpRCentral.setJMenuBar(menuBar);
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 0, 0, 0, 0 };
        gridBagLayout.rowHeights = new int[] { 0, 0 };
        gridBagLayout.columnWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
        gridBagLayout.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
        frmWaarpRCentral.getContentPane().setLayout(gridBagLayout);

        JToolBar toolBar = new JToolBar();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 0, 5);
        constraints.gridy = 0;
        constraints.gridx = 0;
        frmWaarpRCentral.getContentPane().add(toolBar, constraints);

        btnEditXml = new JButton("Edit XML");
        btnEditXml.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                AdminXample.start(xamples);
            }
        });

        btnCheckPartners = new JButton("Check Partners");
        btnCheckPartners.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                // Check all Known Partners
                try {
                    TestPacket packet;
                    String myhost = null;
                    try {
                        myhost = (AdminGui.getEnvironnement().hostId == null ?
                                InetAddress.getLocalHost().getHostName() : AdminGui.getEnvironnement().hostId);
                    } catch (UnknownHostException e) {
                        myhost = Messages.getString("AdminGui.NameUnknown");
                    }
                    packet = new TestPacket("MSG", "Administrator checking from " + myhost
                            , 100);
                    packet.retain();
                    String result = Messages.getString("AdminGui.CheckedHosts");
                    DbSession session = DbConstant.admin != null ? DbConstant.admin.getSession() : null;
                    for (DbHostAuth host : DbHostAuth.getAllHosts()) {
                        R66Future future = new R66Future(true);
                        Message mesg = new Message(AdminGui.getEnvironnement().networkTransaction, future, host, packet);
                        mesg.run();
                        packet.retain();
                        future.awaitUninterruptibly();
                        if (future.isSuccess()) {
                            result += "OK: " + host.toString() + "\n";
                        } else {
                            result += "KO: " + host.toString() + "\n";
                        }
                    }
                    packet.clear();
                    ;
                    JOptionPane.showMessageDialog(null, result);
                } catch (WaarpDatabaseNoConnectionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        toolBar.add(btnCheckPartners);
        toolBar.add(btnEditXml);

        btnEditPassword = new JButton("Edit Password");
        btnEditPassword.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        AdminUiPassword inst;
                        try {
                            inst = new AdminUiPassword(passwords);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
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
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });
        frmWaarpRCentral.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        btnQuit = new JButton("QUIT");
        btnQuit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                quit();
            }
        });

        btnManageConfiguration = new JButton("Manage Configuration");
        btnManageConfiguration.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        if (AdminR66OperationsGui.getWindow() != null) {
                            AdminR66OperationsGui.getWindow().dispose();
                            AdminR66OperationsGui.setWindow(null);
                        }
                        try {
                            AdminR66OperationsGui.setWindow(new AdminR66OperationsGui(frmWaarpRCentral));
                            AdminR66OperationsGui.getWindow().setVisible(true);
                        } catch (Exception e) {
                            e.printStackTrace();
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
            public void actionPerformed(ActionEvent arg0) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        if (AdminSimpleR66ClientGui.window != null) {
                            AdminSimpleR66ClientGui.window.frmRClientGui.dispose();
                            AdminSimpleR66ClientGui.window = null;
                        }
                        try {
                            AdminSimpleR66ClientGui.window = new AdminSimpleR66ClientGui();
                            AdminSimpleR66ClientGui.window.frmRClientGui.setVisible(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
        toolBar.add(btnFileTransfer);
        toolBar.add(btnQuit);

        JButton btnFr = new JButton("");
        btnFr.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Messages.init(new Locale("fr"));
                langReinit();
            }
        });
        btnFr.setMargin(new Insets(2, 2, 2, 2));
        URL fr = AdminGui.class.getResource("/fr.png");
        if (fr != null) {
            btnFr.setIcon(new ImageIcon(fr));
        } else {
            btnFr.setText("FR");
        }
        btnFr.setToolTipText("FR");
        GridBagConstraints gbc_btnFr = new GridBagConstraints();
        gbc_btnFr.insets = new Insets(0, 0, 0, 5);
        gbc_btnFr.gridx = 1;
        gbc_btnFr.gridy = 0;
        frmWaarpRCentral.getContentPane().add(btnFr, gbc_btnFr);

        JButton btnEn = new JButton("");
        btnEn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Messages.init(new Locale("en"));
                langReinit();
            }
        });
        btnEn.setToolTipText("EN");
        URL en = AdminGui.class.getResource("/en.png");
        if (en != null) {
            btnEn.setIcon(new ImageIcon(en));
        } else {
            btnEn.setText("EN");
        }
        btnEn.setMargin(new Insets(2, 2, 2, 2));
        GridBagConstraints gbc_btnEn = new GridBagConstraints();
        gbc_btnEn.gridx = 2;
        gbc_btnEn.gridy = 0;
        frmWaarpRCentral.getContentPane().add(btnEn, gbc_btnEn);
    }

    /**
     * Quit application
     */
    private void quit() {
        List<AdminXample> list = new ArrayList<AdminXample>();
        for (AdminXample xample : xamples) {
            if (xample.isStillLaunched()) {
                list.add(xample);
            }
        }
        for (AdminXample xample : list) {
            xample.exit();
        }
        List<AdminUiPassword> list2 = new ArrayList<AdminUiPassword>();
        for (AdminUiPassword pwd : passwords) {
            list2.add(pwd);
        }
        for (AdminUiPassword pwd : list2) {
            pwd.exit(null);
        }
        if (AdminSimpleR66ClientGui.window != null) {
            AdminSimpleR66ClientGui.window.frmRClientGui.dispose();
            AdminSimpleR66ClientGui.window = null;
        }
        frmWaarpRCentral.dispose();
        System.exit(0);
    }

    /**
     * @return the environnement
     */
    public static R66Environment getEnvironnement() {
        return environnement;
    }
}
