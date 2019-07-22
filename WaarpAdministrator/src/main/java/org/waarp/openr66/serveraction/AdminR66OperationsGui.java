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
package org.waarp.openr66.serveraction;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.SoftBevelBorder;

import org.waarp.administrator.AdminGui;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.client.DirectTransfer;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.BlockRequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.ShutdownPacket;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.BandwidthJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ConfigExportResponseJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.LogResponseJsonPacket;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.r66gui.R66Environment;
import org.waarp.openr66.server.ChangeBandwidthLimits;
import org.waarp.openr66.server.ConfigExport;
import org.waarp.openr66.server.ConfigImport;
import org.waarp.openr66.server.LogExport;
import org.waarp.openr66.server.LogExtendedExport;

import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import java.awt.Dimension;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.SocketAddress;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import java.awt.Rectangle;
import java.awt.Cursor;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;

/**
 * @author "Frederic Bregier"
 * 
 */
public class AdminR66OperationsGui extends JFrame {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(AdminR66OperationsGui.class);

    private static AdminR66OperationsGui window;

    private static final long serialVersionUID = -7289307852740863337L;
    private JFrame adminGui;
    private AdminR66OperationsGui myself = this;
    private JSplitPane mainPanel;
    JProgressBar progressBarTransfer;
    R66Dialog dialog;

    /**
     * @throws HeadlessException
     */
    public AdminR66OperationsGui(JFrame adminGui) throws HeadlessException {
        super(Messages.getString("AdminR66OperationsGui.0") + Configuration.configuration.getHOST_ID()); //$NON-NLS-1$
        setMinimumSize(new Dimension(1100, 700));
        setPreferredSize(new Dimension(1100, 800));
        this.adminGui = adminGui;
        mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        mainPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent arg0) {
                mainPanel.setDividerLocation(mainPanel.getHeight() / 3);
            }
        });
        mainPanel.setDividerSize(2);
        mainPanel.setAutoscrolls(true);
        mainPanel.setBorder(new CompoundBorder());
        mainPanel.setOneTouchExpandable(true);
        setContentPane(mainPanel);
        initializePanel();
    }

    private void initializePanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setPreferredSize(new Dimension(50, 250));
        buttonPanel.setMinimumSize(new Dimension(50, 250));
        GridBagLayout buttons = new GridBagLayout();
        buttons.columnWidths = new int[] { 194, 124, 0, 0, 0 };
        buttons.rowHeights = new int[] { 0, 0, 0, 0 };
        buttons.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
        buttons.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0 };
        buttonPanel.setLayout(buttons);
        mainPanel.setBottomComponent(buttonPanel);

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setMinimumSize(new Dimension(5, 300));
        tabbedPane.setPreferredSize(new Dimension(5, 300));
        mainPanel.setTopComponent(tabbedPane);

        scrollPane_1 = new JScrollPane();
        scrollPane_1
                .setViewportBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
        gbc_scrollPane_1.insets = new Insets(0, 0, 5, 0);
        gbc_scrollPane_1.weighty = 1.0;
        gbc_scrollPane_1.weightx = 1.0;
        gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
        gbc_scrollPane_1.gridwidth = 5;
        // gbc_scrollPane_1.insets = new Insets(0, 0, 5, 0);
        gbc_scrollPane_1.gridx = 0;
        gbc_scrollPane_1.gridy = 0;
        buttonPanel.add(scrollPane_1, gbc_scrollPane_1);

        textFieldStatus = new JEditorPane();
        textFieldStatus.setToolTipText(Messages.getString("R66ClientGui.21")); //$NON-NLS-1$
        scrollPane_1.setViewportView(textFieldStatus);
        textFieldStatus.setForeground(Color.GRAY);
        textFieldStatus.setBackground(new Color(255, 255, 153));
        textFieldStatus.setContentType("text/html");
        textFieldStatus.setEditable(false);

        scrollPane = new JScrollPane();
        scrollPane.setViewportBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null,
                null));
        GridBagConstraints gbc_scrollPane = new GridBagConstraints();
        gbc_scrollPane.insets = new Insets(0, 0, 5, 0);
        gbc_scrollPane.weighty = 1.0;
        gbc_scrollPane.weightx = 1.0;
        gbc_scrollPane.fill = GridBagConstraints.BOTH;
        gbc_scrollPane.gridwidth = 5;
        gbc_scrollPane.gridx = 0;
        gbc_scrollPane.gridy = 1;
        buttonPanel.add(scrollPane, gbc_scrollPane);

        textPaneLog = new JTextArea();
        scrollPane.setViewportView(textPaneLog);
        textPaneLog.setToolTipText(Messages.getString("R66ClientGui.23")); //$NON-NLS-1$
        textPaneLog.setEditable(false);

        System.setOut(new PrintStream(new JTextAreaOutputStream(textPaneLog)));
        DbSession session = DbConstant.admin != null ? DbConstant.admin.getSession() : null;
        try {
            comboBoxServer = new JComboBox(DbHostAuth.getAllHosts());
        } catch (WaarpDatabaseNoConnectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        comboBoxServer.setMinimumSize(new Dimension(28, 22));
        GridBagConstraints gbc_comboBoxServer = new GridBagConstraints();
        gbc_comboBoxServer.gridwidth = 2;
        gbc_comboBoxServer.insets = new Insets(0, 0, 5, 5);
        gbc_comboBoxServer.fill = GridBagConstraints.HORIZONTAL;
        gbc_comboBoxServer.gridx = 0;
        gbc_comboBoxServer.gridy = 2;
        buttonPanel.add(comboBoxServer, gbc_comboBoxServer);

        progressBarTransfer = new JProgressBar();
        progressBarTransfer.setPreferredSize(new Dimension(500, 14));
        GridBagConstraints gbc_pb = new GridBagConstraints();
        gbc_pb.gridwidth = 3;
        gbc_pb.insets = new Insets(0, 0, 0, 5);
        gbc_pb.fill = GridBagConstraints.BOTH;
        gbc_pb.gridx = 0;
        gbc_pb.gridy = 3;
        buttonPanel.add(progressBarTransfer, gbc_pb);

        JButton btnCancel = new JButton(Messages.getString("AdminR66OperationsGui.Close")); //$NON-NLS-1$
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        GridBagConstraints gbc_btnCancel = new GridBagConstraints();
        gbc_btnCancel.insets = new Insets(0, 0, 0, 5);
        gbc_btnCancel.gridx = 3;
        gbc_btnCancel.gridy = 3;
        buttonPanel.add(btnCancel, gbc_btnCancel);
        progressBarTransfer.setVisible(false);

        initBandwidth(tabbedPane);
        initConfig(tabbedPane);
        initLog(tabbedPane);
        initShutdown(tabbedPane);
        mainPanel.setDividerLocation(200);
    }

    JButton btnGetBandwidthCurrent, btnGetConfigCurrent;
    JButton btnSetBandwidthConfiguration, btnSetConfigConfiguration;
    JFormattedTextField globWriteLimit;
    JFormattedTextField globReadLimit;
    JFormattedTextField sessionWriteLimit;
    JFormattedTextField sessionReadLimit;
    JScrollPane scrollPane_1;
    JEditorPane textFieldStatus;
    JScrollPane scrollPane;
    JTextArea textPaneLog;
    private JCheckBox chckbxHosts;
    private JCheckBox chckbxRules;
    private JTextField textFieldHosts;
    private JTextField textFieldRules;
    private JCheckBox chckbxPurgeHosts;
    private JCheckBox chckbxPurgeRules;
    private JButton btnHostsFile;
    private JButton btnRulesFile;
    private JButton btnShutdown;
    private JPasswordField passwordField;
    private JLabel lblPassword;
    private JCheckBox chckbxPurge;
    private JCheckBox chckbxClean;
    private JTextField textFieldStart;
    private JTextField textFieldStop;
    private JButton btnExportLogs;
    private JTextField textFieldResult;
    private JComboBox textRuleUsedToGet;
    private JComboBox textRuleToPut;
    private JLabel lblDates;
    private JComboBox textRuleToExportLog;
    private JSeparator separator;
    private JLabel lblRuleToGet;
    private JLabel lblRuleToPut;
    private JLabel lblRuleToExport;
    private JLabel lblNbIfNo;
    private JComboBox comboBoxServer;
    private JCheckBox chckbxPending;
    private JCheckBox chckbxRunning;
    private JCheckBox chckbxInError;
    private JCheckBox chckbxDone;
    private JCheckBox chckbxBusiness;
    private JCheckBox chckbxAlias;
    private JCheckBox chckbxRoles;
    private JButton btnBusinessFile;
    private JButton btnAliasFile;
    private JButton btnRolesFile;
    private JTextField textFieldBusiness;
    private JTextField textFieldAlias;
    private JTextField textFieldRoles;
    private JCheckBox chckbxPurgeBusiness;
    private JCheckBox chckbxPurgeAlias;
    private JCheckBox chckbxPurgeRoles;
    private JTextField textFieldLogRule;
    private JTextField textFieldLogHost;
    private JLabel lblRuleUsedIn;
    private JLabel lblHostUsedIn;
    private JCheckBox chckbxBlockUnblock;
    private JRadioButton rdbtnShutdown;
    private JRadioButton rdbtnBlock;
    private JCheckBox chckbxRestart;
    private final ButtonGroup buttonGroup = new ButtonGroup();

    private void initBandwidth(JTabbedPane tabbedPane) {
        JPanel bandwidthPanel = new JPanel();
        tabbedPane.addTab(Messages.getString("AdminR66OperationsGui.5"), null, bandwidthPanel, null); //$NON-NLS-1$
        GridBagLayout gbl_xmlFilePanel = new GridBagLayout();
        gbl_xmlFilePanel.columnWidths = new int[] { 0, 0, 0, 0, 0, 0 };
        gbl_xmlFilePanel.rowHeights = new int[] { 1, 1, 0, 0 };
        gbl_xmlFilePanel.columnWeights = new double[] { 0.0, 0.0, 1.0, 1.0, 1.0, 1.0 };
        gbl_xmlFilePanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0 };
        bandwidthPanel.setLayout(gbl_xmlFilePanel);
        {
            btnGetBandwidthCurrent = new JButton(Messages.getString("AdminR66OperationsGui.6")); //$NON-NLS-1$
            btnGetBandwidthCurrent.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    R66AdminGuiActions action = new R66AdminGuiActions(
                            R66AdminGuiActions.BANDWIDTHGET);
                    action.execute();
                }
            });
            GridBagConstraints gbc_btnGetBandwidthCurrent = new GridBagConstraints();
            gbc_btnGetBandwidthCurrent.gridwidth = 2;
            gbc_btnGetBandwidthCurrent.insets = new Insets(0, 0, 5, 5);
            gbc_btnGetBandwidthCurrent.gridx = 4;
            gbc_btnGetBandwidthCurrent.gridy = 0;
            bandwidthPanel.add(btnGetBandwidthCurrent, gbc_btnGetBandwidthCurrent);
        }
        {
            JLabel lblGlobalLimit = new JLabel(Messages.getString("AdminR66OperationsGui.7")); //$NON-NLS-1$
            GridBagConstraints gbc_lblGlobalLimit = new GridBagConstraints();
            gbc_lblGlobalLimit.insets = new Insets(0, 0, 5, 5);
            gbc_lblGlobalLimit.gridx = 0;
            gbc_lblGlobalLimit.gridy = 2;
            bandwidthPanel.add(lblGlobalLimit, gbc_lblGlobalLimit);
        }
        {
            JLabel lblWrite = new JLabel(Messages.getString("AdminR66OperationsGui.Write")); //$NON-NLS-1$
            GridBagConstraints gbc_lblWrite = new GridBagConstraints();
            gbc_lblWrite.insets = new Insets(0, 0, 5, 5);
            gbc_lblWrite.anchor = GridBagConstraints.EAST;
            gbc_lblWrite.gridx = 1;
            gbc_lblWrite.gridy = 2;
            bandwidthPanel.add(lblWrite, gbc_lblWrite);
        }
        {
            globWriteLimit = new JFormattedTextField();
            globWriteLimit.setMinimumSize(new Dimension(100, 20));
            globWriteLimit.setValue(new Long(0));
            GridBagConstraints gbc_globWriteLimit = new GridBagConstraints();
            gbc_globWriteLimit.insets = new Insets(0, 0, 5, 5);
            gbc_globWriteLimit.fill = GridBagConstraints.HORIZONTAL;
            gbc_globWriteLimit.gridx = 2;
            gbc_globWriteLimit.gridy = 2;
            bandwidthPanel.add(globWriteLimit, gbc_globWriteLimit);
        }
        {
            JLabel lblRead = new JLabel(Messages.getString("AdminR66OperationsGui.Read")); //$NON-NLS-1$
            GridBagConstraints gbc_lblRead = new GridBagConstraints();
            gbc_lblRead.insets = new Insets(0, 0, 5, 5);
            gbc_lblRead.anchor = GridBagConstraints.EAST;
            gbc_lblRead.gridx = 3;
            gbc_lblRead.gridy = 2;
            bandwidthPanel.add(lblRead, gbc_lblRead);
        }
        {
            globReadLimit = new JFormattedTextField();
            globReadLimit.setMinimumSize(new Dimension(100, 20));
            globReadLimit.setValue(new Long(0));
            GridBagConstraints gbc_globReadLimit = new GridBagConstraints();
            gbc_globReadLimit.insets = new Insets(0, 0, 5, 5);
            gbc_globReadLimit.fill = GridBagConstraints.HORIZONTAL;
            gbc_globReadLimit.gridx = 4;
            gbc_globReadLimit.gridy = 2;
            bandwidthPanel.add(globReadLimit, gbc_globReadLimit);
        }
        {
            JLabel lblSessionLimit = new JLabel(Messages.getString("AdminR66OperationsGui.10")); //$NON-NLS-1$
            GridBagConstraints gbc_lblSessionLimit = new GridBagConstraints();
            gbc_lblSessionLimit.insets = new Insets(0, 0, 5, 5);
            gbc_lblSessionLimit.gridx = 0;
            gbc_lblSessionLimit.gridy = 3;
            bandwidthPanel.add(lblSessionLimit, gbc_lblSessionLimit);
        }
        {
            JLabel lblWrite_1 = new JLabel(Messages.getString("AdminR66OperationsGui.Write")); //$NON-NLS-1$
            GridBagConstraints gbc_lblWrite_1 = new GridBagConstraints();
            gbc_lblWrite_1.anchor = GridBagConstraints.EAST;
            gbc_lblWrite_1.insets = new Insets(0, 0, 5, 5);
            gbc_lblWrite_1.gridx = 1;
            gbc_lblWrite_1.gridy = 3;
            bandwidthPanel.add(lblWrite_1, gbc_lblWrite_1);
        }
        {
            sessionWriteLimit = new JFormattedTextField();
            sessionWriteLimit.setMinimumSize(new Dimension(100, 20));
            sessionWriteLimit.setValue(new Long(0));
            GridBagConstraints gbc_sessionWriteLimit = new GridBagConstraints();
            gbc_sessionWriteLimit.insets = new Insets(0, 0, 5, 5);
            gbc_sessionWriteLimit.fill = GridBagConstraints.HORIZONTAL;
            gbc_sessionWriteLimit.gridx = 2;
            gbc_sessionWriteLimit.gridy = 3;
            bandwidthPanel.add(sessionWriteLimit, gbc_sessionWriteLimit);
        }
        {
            JLabel lblRead_1 = new JLabel(Messages.getString("AdminR66OperationsGui.Read")); //$NON-NLS-1$
            GridBagConstraints gbc_lblRead_1 = new GridBagConstraints();
            gbc_lblRead_1.anchor = GridBagConstraints.EAST;
            gbc_lblRead_1.insets = new Insets(0, 0, 5, 5);
            gbc_lblRead_1.gridx = 3;
            gbc_lblRead_1.gridy = 3;
            bandwidthPanel.add(lblRead_1, gbc_lblRead_1);
        }
        {
            sessionReadLimit = new JFormattedTextField();
            sessionReadLimit.setMinimumSize(new Dimension(100, 20));
            sessionReadLimit.setValue(new Long(0));
            GridBagConstraints gbc_sessionReadLimit = new GridBagConstraints();
            gbc_sessionReadLimit.insets = new Insets(0, 0, 5, 5);
            gbc_sessionReadLimit.fill = GridBagConstraints.HORIZONTAL;
            gbc_sessionReadLimit.gridx = 4;
            gbc_sessionReadLimit.gridy = 3;
            bandwidthPanel.add(sessionReadLimit, gbc_sessionReadLimit);
        }
        {
            btnSetBandwidthConfiguration = new JButton(Messages.getString("AdminR66OperationsGui.13")); //$NON-NLS-1$
            btnSetBandwidthConfiguration.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    R66AdminGuiActions action = new R66AdminGuiActions(
                            R66AdminGuiActions.BANDWIDTHSET);
                    action.execute();
                }
            });
            GridBagConstraints gbc_btnSetBandwidthConfiguration = new GridBagConstraints();
            gbc_btnSetBandwidthConfiguration.gridwidth = 2;
            gbc_btnSetBandwidthConfiguration.insets = new Insets(0, 0, 5, 5);
            gbc_btnSetBandwidthConfiguration.gridx = 4;
            gbc_btnSetBandwidthConfiguration.gridy = 4;
            bandwidthPanel.add(btnSetBandwidthConfiguration, gbc_btnSetBandwidthConfiguration);
        }

    }

    private void initConfig(JTabbedPane tabbedPane) {
        String[] srulesSend = R66Environment.getRules(true);
        String[] srulesRecv = R66Environment.getRules(false);

        JPanel configPanel = new JPanel();
        tabbedPane.addTab(Messages.getString("AdminR66OperationsGui.14"), null, configPanel, null); //$NON-NLS-1$
        GridBagLayout gbl_toolsPanel = new GridBagLayout();
        gbl_toolsPanel.columnWidths = new int[] { 0, 0, 0, 0, 0, 0, 0 };
        gbl_toolsPanel.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        gbl_toolsPanel.columnWeights = new double[] { 0.0, 1.0, 1.0, 1.0, 1.0, 1.0 };
        gbl_toolsPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
        configPanel.setLayout(gbl_toolsPanel);
        {
            {
                lblRuleToGet = new JLabel(Messages.getString("AdminR66OperationsGui.15")); //$NON-NLS-1$
                GridBagConstraints gbc_lblRuleToGet = new GridBagConstraints();
                gbc_lblRuleToGet.insets = new Insets(0, 0, 5, 5);
                gbc_lblRuleToGet.anchor = GridBagConstraints.EAST;
                gbc_lblRuleToGet.gridx = 0;
                gbc_lblRuleToGet.gridy = 2;
                configPanel.add(lblRuleToGet, gbc_lblRuleToGet);
            }
            {
                textRuleUsedToGet = new JComboBox(srulesRecv);
                GridBagConstraints gbc_textRuleUsedToGet = new GridBagConstraints();
                gbc_textRuleUsedToGet.fill = GridBagConstraints.HORIZONTAL;
                gbc_textRuleUsedToGet.insets = new Insets(0, 0, 5, 5);
                gbc_textRuleUsedToGet.gridx = 1;
                gbc_textRuleUsedToGet.gridy = 2;
                configPanel.add(textRuleUsedToGet, gbc_textRuleUsedToGet);
            }
            {
                chckbxHosts = new JCheckBox(Messages.getString("AdminR66OperationsGui.Hosts")); //$NON-NLS-1$
                GridBagConstraints gbc_chckbxHosts = new GridBagConstraints();
                gbc_chckbxHosts.insets = new Insets(0, 0, 5, 5);
                gbc_chckbxHosts.gridx = 2;
                gbc_chckbxHosts.gridy = 2;
                configPanel.add(chckbxHosts, gbc_chckbxHosts);
            }
        }
        {
            {
                chckbxRules = new JCheckBox(Messages.getString("AdminR66OperationsGui.Rules")); //$NON-NLS-1$
                GridBagConstraints gbc_chckbxRules = new GridBagConstraints();
                gbc_chckbxRules.insets = new Insets(0, 0, 5, 5);
                gbc_chckbxRules.gridx = 3;
                gbc_chckbxRules.gridy = 2;
                configPanel.add(chckbxRules, gbc_chckbxRules);
            }
            {
                chckbxBusiness = new JCheckBox(Messages.getString("AdminR66OperationsGui.Business")); //$NON-NLS-1$
                GridBagConstraints gbc_chckbxBusiness = new GridBagConstraints();
                gbc_chckbxBusiness.insets = new Insets(0, 0, 5, 5);
                gbc_chckbxBusiness.gridx = 1;
                gbc_chckbxBusiness.gridy = 3;
                configPanel.add(chckbxBusiness, gbc_chckbxBusiness);
            }
            {
                chckbxAlias = new JCheckBox(Messages.getString("AdminR66OperationsGui.Alias")); //$NON-NLS-1$
                GridBagConstraints gbc_chckbxAlias = new GridBagConstraints();
                gbc_chckbxAlias.insets = new Insets(0, 0, 5, 5);
                gbc_chckbxAlias.gridx = 2;
                gbc_chckbxAlias.gridy = 3;
                configPanel.add(chckbxAlias, gbc_chckbxAlias);
            }
            {
                chckbxRoles = new JCheckBox(Messages.getString("AdminR66OperationsGui.Roles")); //$NON-NLS-1$
                GridBagConstraints gbc_chckbxRoles = new GridBagConstraints();
                gbc_chckbxRoles.insets = new Insets(0, 0, 5, 5);
                gbc_chckbxRoles.gridx = 3;
                gbc_chckbxRoles.gridy = 3;
                configPanel.add(chckbxRoles, gbc_chckbxRoles);
            }
            btnGetConfigCurrent = new JButton(Messages.getString("AdminR66OperationsGui.21")); //$NON-NLS-1$
            btnGetConfigCurrent.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    R66AdminGuiActions action = new R66AdminGuiActions(
                            R66AdminGuiActions.CONFIGEXPORT);
                    action.execute();
                }
            });
            GridBagConstraints gbc_btnGetBandwidthCurrent = new GridBagConstraints();
            gbc_btnGetBandwidthCurrent.gridwidth = 2;
            gbc_btnGetBandwidthCurrent.insets = new Insets(0, 0, 5, 5);
            gbc_btnGetBandwidthCurrent.gridx = 4;
            gbc_btnGetBandwidthCurrent.gridy = 3;
            configPanel.add(btnGetConfigCurrent, gbc_btnGetBandwidthCurrent);
        }
        {
            separator = new JSeparator();
            separator.setOpaque(true);
            separator.setForeground(Color.BLACK);
            separator.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            separator.setBounds(new Rectangle(0, 0, 2, 2));
            separator.setBackground(Color.DARK_GRAY);
            separator.setMinimumSize(new Dimension(800, 2));
            separator.setRequestFocusEnabled(false);
            separator.setSize(new Dimension(800, 2));
            separator.setPreferredSize(new Dimension(800, 2));
            separator.setFocusTraversalKeysEnabled(false);
            GridBagConstraints gbc_separator = new GridBagConstraints();
            gbc_separator.gridwidth = 6;
            gbc_separator.insets = new Insets(0, 0, 5, 5);
            gbc_separator.gridx = 0;
            gbc_separator.gridy = 4;
            configPanel.add(separator, gbc_separator);
        }
        btnHostsFile = new JButton(Messages.getString("AdminR66OperationsGui.22")); //$NON-NLS-1$
        btnHostsFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File result = openFile(
                        textFieldHosts.getText(),
                        Messages.getString("AdminR66OperationsGui.Choose") + Messages.getString("AdminR66OperationsGui.22"), "xml"); //$NON-NLS-1$
                if (result != null) {
                    textFieldHosts.setText(result.getAbsolutePath());
                }
            }
        });
        GridBagConstraints gbc_btnHostsFile = new GridBagConstraints();
        gbc_btnHostsFile.insets = new Insets(0, 0, 5, 5);
        gbc_btnHostsFile.gridx = 0;
        gbc_btnHostsFile.gridy = 5;
        configPanel.add(btnHostsFile, gbc_btnHostsFile);
        {
            textFieldHosts = new JTextField();
            GridBagConstraints gbc_textFieldHosts = new GridBagConstraints();
            gbc_textFieldHosts.insets = new Insets(0, 0, 5, 5);
            gbc_textFieldHosts.fill = GridBagConstraints.HORIZONTAL;
            gbc_textFieldHosts.gridx = 1;
            gbc_textFieldHosts.gridy = 5;
            configPanel.add(textFieldHosts, gbc_textFieldHosts);
            textFieldHosts.setColumns(10);
        }
        btnRulesFile = new JButton(Messages.getString("AdminR66OperationsGui.25")); //$NON-NLS-1$
        btnRulesFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File result = openFile(
                        textFieldRules.getText(),
                        Messages.getString("AdminR66OperationsGui.Choose") + Messages.getString("AdminR66OperationsGui.25"), "xml"); //$NON-NLS-1$
                if (result != null) {
                    textFieldRules.setText(result.getAbsolutePath());
                }
            }
        });
        GridBagConstraints gbc_btnRulesFile = new GridBagConstraints();
        gbc_btnRulesFile.insets = new Insets(0, 0, 5, 5);
        gbc_btnRulesFile.gridx = 2;
        gbc_btnRulesFile.gridy = 5;
        configPanel.add(btnRulesFile, gbc_btnRulesFile);
        {
            textFieldRules = new JTextField();
            GridBagConstraints gbc_textFieldRules = new GridBagConstraints();
            gbc_textFieldRules.insets = new Insets(0, 0, 5, 5);
            gbc_textFieldRules.fill = GridBagConstraints.HORIZONTAL;
            gbc_textFieldRules.gridx = 3;
            gbc_textFieldRules.gridy = 5;
            configPanel.add(textFieldRules, gbc_textFieldRules);
            textFieldRules.setColumns(10);
        }
        {
            btnBusinessFile = new JButton(Messages.getString("AdminR66OperationsGui.28")); //$NON-NLS-1$
            btnBusinessFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    File result = openFile(
                            textFieldBusiness.getText(),
                            Messages.getString("AdminR66OperationsGui.Choose") + Messages.getString("AdminR66OperationsGui.28"), "xml"); //$NON-NLS-1$
                    if (result != null) {
                        textFieldBusiness.setText(result.getAbsolutePath());
                    }
                }
            });
            GridBagConstraints gbc_btnBusinessFile = new GridBagConstraints();
            gbc_btnBusinessFile.insets = new Insets(0, 0, 5, 5);
            gbc_btnBusinessFile.gridx = 0;
            gbc_btnBusinessFile.gridy = 6;
            configPanel.add(btnBusinessFile, gbc_btnBusinessFile);
        }
        {
            {
                textFieldBusiness = new JTextField();
                GridBagConstraints gbc_textFieldBusiness = new GridBagConstraints();
                gbc_textFieldBusiness.insets = new Insets(0, 0, 5, 5);
                gbc_textFieldBusiness.fill = GridBagConstraints.HORIZONTAL;
                gbc_textFieldBusiness.gridx = 1;
                gbc_textFieldBusiness.gridy = 6;
                configPanel.add(textFieldBusiness, gbc_textFieldBusiness);
                textFieldBusiness.setColumns(10);
            }
        }
        {
            btnAliasFile = new JButton(Messages.getString("AdminR66OperationsGui.31")); //$NON-NLS-1$
            btnAliasFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    File result = openFile(
                            textFieldAlias.getText(),
                            Messages.getString("AdminR66OperationsGui.Choose") + Messages.getString("AdminR66OperationsGui.31"), "xml"); //$NON-NLS-1$
                    if (result != null) {
                        textFieldAlias.setText(result.getAbsolutePath());
                    }
                }
            });
            GridBagConstraints gbc_btnAliasFile = new GridBagConstraints();
            gbc_btnAliasFile.insets = new Insets(0, 0, 5, 5);
            gbc_btnAliasFile.gridx = 2;
            gbc_btnAliasFile.gridy = 6;
            configPanel.add(btnAliasFile, gbc_btnAliasFile);
        }
        {
            textFieldAlias = new JTextField();
            GridBagConstraints gbc_textFieldAlias = new GridBagConstraints();
            gbc_textFieldAlias.insets = new Insets(0, 0, 5, 5);
            gbc_textFieldAlias.fill = GridBagConstraints.HORIZONTAL;
            gbc_textFieldAlias.gridx = 3;
            gbc_textFieldAlias.gridy = 6;
            configPanel.add(textFieldAlias, gbc_textFieldAlias);
            textFieldAlias.setColumns(10);
        }
        {
            btnRolesFile = new JButton(Messages.getString("AdminR66OperationsGui.34")); //$NON-NLS-1$
            btnRolesFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    File result = openFile(
                            textFieldRoles.getText(),
                            Messages.getString("AdminR66OperationsGui.Choose") + Messages.getString("AdminR66OperationsGui.34"), "xml"); //$NON-NLS-1$
                    if (result != null) {
                        textFieldRoles.setText(result.getAbsolutePath());
                    }
                }
            });
            GridBagConstraints gbc_btnRolesFile = new GridBagConstraints();
            gbc_btnRolesFile.insets = new Insets(0, 0, 5, 5);
            gbc_btnRolesFile.gridx = 4;
            gbc_btnRolesFile.gridy = 6;
            configPanel.add(btnRolesFile, gbc_btnRolesFile);
        }
        {
            textFieldRoles = new JTextField();
            GridBagConstraints gbc_textFieldRoles = new GridBagConstraints();
            gbc_textFieldRoles.insets = new Insets(0, 0, 5, 5);
            gbc_textFieldRoles.fill = GridBagConstraints.HORIZONTAL;
            gbc_textFieldRoles.gridx = 5;
            gbc_textFieldRoles.gridy = 6;
            configPanel.add(textFieldRoles, gbc_textFieldRoles);
            textFieldRoles.setColumns(10);
        }
        {
            lblRuleToPut = new JLabel(Messages.getString("AdminR66OperationsGui.37")); //$NON-NLS-1$
            GridBagConstraints gbc_lblRuleToPut = new GridBagConstraints();
            gbc_lblRuleToPut.insets = new Insets(0, 0, 5, 5);
            gbc_lblRuleToPut.anchor = GridBagConstraints.EAST;
            gbc_lblRuleToPut.gridx = 0;
            gbc_lblRuleToPut.gridy = 7;
            configPanel.add(lblRuleToPut, gbc_lblRuleToPut);
        }
        {
            textRuleToPut = new JComboBox(srulesSend);
            GridBagConstraints gbc_textRuleToPut = new GridBagConstraints();
            gbc_textRuleToPut.insets = new Insets(0, 0, 5, 5);
            gbc_textRuleToPut.fill = GridBagConstraints.HORIZONTAL;
            gbc_textRuleToPut.gridx = 1;
            gbc_textRuleToPut.gridy = 7;
            configPanel.add(textRuleToPut, gbc_textRuleToPut);
        }
        {
            chckbxPurgeHosts = new JCheckBox(
                    Messages.getString("AdminR66OperationsGui.Purge") + Messages.getString("AdminR66OperationsGui.Hosts")); //$NON-NLS-1$
            GridBagConstraints gbc_chckbxPurgeHosts = new GridBagConstraints();
            gbc_chckbxPurgeHosts.insets = new Insets(0, 0, 5, 5);
            gbc_chckbxPurgeHosts.gridx = 2;
            gbc_chckbxPurgeHosts.gridy = 7;
            configPanel.add(chckbxPurgeHosts, gbc_chckbxPurgeHosts);
        }
        {
            chckbxPurgeRules = new JCheckBox(
                    Messages.getString("AdminR66OperationsGui.Purge") + Messages.getString("AdminR66OperationsGui.Rules")); //$NON-NLS-1$
            GridBagConstraints gbc_chckbxPurgeRules = new GridBagConstraints();
            gbc_chckbxPurgeRules.insets = new Insets(0, 0, 5, 5);
            gbc_chckbxPurgeRules.gridx = 3;
            gbc_chckbxPurgeRules.gridy = 7;
            configPanel.add(chckbxPurgeRules, gbc_chckbxPurgeRules);
        }
        {
            chckbxPurgeBusiness = new JCheckBox(
                    Messages.getString("AdminR66OperationsGui.Purge") + Messages.getString("AdminR66OperationsGui.Business")); //$NON-NLS-1$
            GridBagConstraints gbc_chckbxPurgeBusiness = new GridBagConstraints();
            gbc_chckbxPurgeBusiness.insets = new Insets(0, 0, 5, 5);
            gbc_chckbxPurgeBusiness.gridx = 1;
            gbc_chckbxPurgeBusiness.gridy = 8;
            configPanel.add(chckbxPurgeBusiness, gbc_chckbxPurgeBusiness);
        }
        {
            chckbxPurgeAlias = new JCheckBox(
                    Messages.getString("AdminR66OperationsGui.Purge") + Messages.getString("AdminR66OperationsGui.Alias")); //$NON-NLS-1$
            GridBagConstraints gbc_chckbxPurgeAlias = new GridBagConstraints();
            gbc_chckbxPurgeAlias.insets = new Insets(0, 0, 5, 5);
            gbc_chckbxPurgeAlias.gridx = 2;
            gbc_chckbxPurgeAlias.gridy = 8;
            configPanel.add(chckbxPurgeAlias, gbc_chckbxPurgeAlias);
        }
        {
            chckbxPurgeRoles = new JCheckBox(
                    Messages.getString("AdminR66OperationsGui.Purge") + Messages.getString("AdminR66OperationsGui.Roles")); //$NON-NLS-1$
            GridBagConstraints gbc_chckbxPurgeRoles = new GridBagConstraints();
            gbc_chckbxPurgeRoles.insets = new Insets(0, 0, 5, 5);
            gbc_chckbxPurgeRoles.gridx = 3;
            gbc_chckbxPurgeRoles.gridy = 8;
            configPanel.add(chckbxPurgeRoles, gbc_chckbxPurgeRoles);
        }

        btnSetConfigConfiguration = new JButton(Messages.getString("AdminR66OperationsGui.43")); //$NON-NLS-1$
        btnSetConfigConfiguration.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                R66AdminGuiActions action = new R66AdminGuiActions(
                        R66AdminGuiActions.CONFIGIMPORT);
                action.execute();
            }
        });
        GridBagConstraints gbc_btnSetBandwidthConfiguration = new GridBagConstraints();
        gbc_btnSetBandwidthConfiguration.gridwidth = 2;
        gbc_btnSetBandwidthConfiguration.insets = new Insets(0, 0, 5, 5);
        gbc_btnSetBandwidthConfiguration.gridx = 4;
        gbc_btnSetBandwidthConfiguration.gridy = 8;
        configPanel.add(btnSetConfigConfiguration, gbc_btnSetBandwidthConfiguration);

    }

    private void initLog(JTabbedPane tabbedPane) {
        String[] srulesRecv = R66Environment.getRules(false);
        JPanel logPanel = new JPanel();
        tabbedPane.addTab(Messages.getString("AdminR66OperationsGui.44"), null, logPanel, null); //$NON-NLS-1$
        GridBagLayout gbl_toolsPanel = new GridBagLayout();
        gbl_toolsPanel.columnWidths = new int[] { 0, 0, 0, 0, 0, 0, 0 };
        gbl_toolsPanel.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        gbl_toolsPanel.columnWeights = new double[] { 0.0, 0.0, 1.0, 1.0, 1.0, 1.0 };
        gbl_toolsPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
        logPanel.setLayout(gbl_toolsPanel);

        {
            chckbxPurge = new JCheckBox(Messages.getString("AdminR66OperationsGui.Purge")); //$NON-NLS-1$
            GridBagConstraints gbc_chckbxPurge = new GridBagConstraints();
            gbc_chckbxPurge.insets = new Insets(0, 0, 5, 5);
            gbc_chckbxPurge.gridx = 1;
            gbc_chckbxPurge.gridy = 2;
            logPanel.add(chckbxPurge, gbc_chckbxPurge);
        }
        {
            chckbxPending = new JCheckBox(Messages.getString("AdminR66OperationsGui.Pending")); //$NON-NLS-1$
            GridBagConstraints gbc_chckbxPending = new GridBagConstraints();
            gbc_chckbxPending.insets = new Insets(0, 0, 5, 5);
            gbc_chckbxPending.gridx = 2;
            gbc_chckbxPending.gridy = 2;
            logPanel.add(chckbxPending, gbc_chckbxPending);
        }
        {
            chckbxRunning = new JCheckBox(Messages.getString("AdminR66OperationsGui.Running")); //$NON-NLS-1$
            GridBagConstraints gbc_chckbxRunning = new GridBagConstraints();
            gbc_chckbxRunning.insets = new Insets(0, 0, 5, 5);
            gbc_chckbxRunning.gridx = 3;
            gbc_chckbxRunning.gridy = 2;
            logPanel.add(chckbxRunning, gbc_chckbxRunning);
        }
        {
            chckbxInError = new JCheckBox(Messages.getString("HttpSslHandler.12")); //$NON-NLS-1$
            GridBagConstraints gbc_chckbxInError = new GridBagConstraints();
            gbc_chckbxInError.insets = new Insets(0, 0, 5, 5);
            gbc_chckbxInError.gridx = 4;
            gbc_chckbxInError.gridy = 2;
            logPanel.add(chckbxInError, gbc_chckbxInError);
        }
        {
            chckbxDone = new JCheckBox(Messages.getString("AdminR66OperationsGui.Done")); //$NON-NLS-1$
            GridBagConstraints gbc_chckbxDone = new GridBagConstraints();
            gbc_chckbxDone.insets = new Insets(0, 0, 5, 5);
            gbc_chckbxDone.gridx = 5;
            gbc_chckbxDone.gridy = 2;
            logPanel.add(chckbxDone, gbc_chckbxDone);
        }
        {
            chckbxClean = new JCheckBox(Messages.getString("AdminR66OperationsGui.Clean")); //$NON-NLS-1$
            GridBagConstraints gbc_chckbxClean = new GridBagConstraints();
            gbc_chckbxClean.insets = new Insets(0, 0, 5, 5);
            gbc_chckbxClean.gridx = 1;
            gbc_chckbxClean.gridy = 3;
            logPanel.add(chckbxClean, gbc_chckbxClean);
        }
        {
            lblRuleUsedIn = new JLabel(Messages.getString("AdminR66OperationsGui.51")); //$NON-NLS-1$
            GridBagConstraints gbc_lblRuleUsedIn = new GridBagConstraints();
            gbc_lblRuleUsedIn.insets = new Insets(0, 0, 5, 5);
            gbc_lblRuleUsedIn.anchor = GridBagConstraints.EAST;
            gbc_lblRuleUsedIn.gridx = 2;
            gbc_lblRuleUsedIn.gridy = 3;
            logPanel.add(lblRuleUsedIn, gbc_lblRuleUsedIn);
        }
        {
            textFieldLogRule = new JTextField();
            GridBagConstraints gbc_textFieldLogRule = new GridBagConstraints();
            gbc_textFieldLogRule.insets = new Insets(0, 0, 5, 5);
            gbc_textFieldLogRule.fill = GridBagConstraints.HORIZONTAL;
            gbc_textFieldLogRule.gridx = 3;
            gbc_textFieldLogRule.gridy = 3;
            logPanel.add(textFieldLogRule, gbc_textFieldLogRule);
            textFieldLogRule.setColumns(10);
        }
        {
            lblHostUsedIn = new JLabel(Messages.getString("AdminR66OperationsGui.52")); //$NON-NLS-1$
            GridBagConstraints gbc_lblHostUsedIn = new GridBagConstraints();
            gbc_lblHostUsedIn.anchor = GridBagConstraints.EAST;
            gbc_lblHostUsedIn.insets = new Insets(0, 0, 5, 5);
            gbc_lblHostUsedIn.gridx = 4;
            gbc_lblHostUsedIn.gridy = 3;
            logPanel.add(lblHostUsedIn, gbc_lblHostUsedIn);
        }
        {
            textFieldLogHost = new JTextField();
            GridBagConstraints gbc_textFieldLogHost = new GridBagConstraints();
            gbc_textFieldLogHost.insets = new Insets(0, 0, 5, 5);
            gbc_textFieldLogHost.fill = GridBagConstraints.HORIZONTAL;
            gbc_textFieldLogHost.gridx = 5;
            gbc_textFieldLogHost.gridy = 3;
            logPanel.add(textFieldLogHost, gbc_textFieldLogHost);
            textFieldLogHost.setColumns(10);
        }
        {
            lblNbIfNo = new JLabel(
                    Messages.getString("AdminR66OperationsGui.53")); //$NON-NLS-1$
            lblNbIfNo.setMinimumSize(new Dimension(900, 14));
            lblNbIfNo.setMaximumSize(new Dimension(900, 14));
            lblNbIfNo.setPreferredSize(new Dimension(900, 30));
            lblNbIfNo.setFocusTraversalKeysEnabled(false);
            lblNbIfNo.setFocusable(false);
            lblNbIfNo.setAutoscrolls(true);
            GridBagConstraints gbc_lblNbIfNo = new GridBagConstraints();
            gbc_lblNbIfNo.gridwidth = 5;
            gbc_lblNbIfNo.insets = new Insets(0, 0, 5, 5);
            gbc_lblNbIfNo.gridx = 1;
            gbc_lblNbIfNo.gridy = 4;
            logPanel.add(lblNbIfNo, gbc_lblNbIfNo);
        }
        {
            lblDates = new JLabel(Messages.getString("AdminR66OperationsGui.Dates")); //$NON-NLS-1$
            GridBagConstraints gbc_lblDates = new GridBagConstraints();
            gbc_lblDates.insets = new Insets(0, 0, 5, 5);
            gbc_lblDates.anchor = GridBagConstraints.EAST;
            gbc_lblDates.gridx = 1;
            gbc_lblDates.gridy = 5;
            logPanel.add(lblDates, gbc_lblDates);
        }
        {
            textFieldStart = new JTextField();
            GridBagConstraints gbc_textFieldStart = new GridBagConstraints();
            gbc_textFieldStart.insets = new Insets(0, 0, 5, 5);
            gbc_textFieldStart.fill = GridBagConstraints.HORIZONTAL;
            gbc_textFieldStart.gridx = 2;
            gbc_textFieldStart.gridy = 5;
            logPanel.add(textFieldStart, gbc_textFieldStart);
            textFieldStart.setColumns(10);
        }
        {
            textFieldStop = new JTextField();
            GridBagConstraints gbc_textFieldStop = new GridBagConstraints();
            gbc_textFieldStop.insets = new Insets(0, 0, 5, 5);
            gbc_textFieldStop.fill = GridBagConstraints.HORIZONTAL;
            gbc_textFieldStop.gridx = 3;
            gbc_textFieldStop.gridy = 5;
            logPanel.add(textFieldStop, gbc_textFieldStop);
            textFieldStop.setColumns(10);
        }
        {
            btnExportLogs = new JButton(Messages.getString("AdminR66OperationsGui.55")); //$NON-NLS-1$
            btnExportLogs.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    R66AdminGuiActions action = new R66AdminGuiActions(
                            R66AdminGuiActions.LOGEXPORT);
                    action.execute();
                }
            });
            {
                lblRuleToExport = new JLabel(Messages.getString("AdminR66OperationsGui.56")); //$NON-NLS-1$
                GridBagConstraints gbc_lblRuleToExport = new GridBagConstraints();
                gbc_lblRuleToExport.insets = new Insets(0, 0, 5, 5);
                gbc_lblRuleToExport.anchor = GridBagConstraints.EAST;
                gbc_lblRuleToExport.gridx = 1;
                gbc_lblRuleToExport.gridy = 6;
                logPanel.add(lblRuleToExport, gbc_lblRuleToExport);
            }
            {
                textRuleToExportLog = new JComboBox(srulesRecv);
                GridBagConstraints gbc_textRuleToExportLog = new GridBagConstraints();
                gbc_textRuleToExportLog.insets = new Insets(0, 0, 5, 5);
                gbc_textRuleToExportLog.fill = GridBagConstraints.HORIZONTAL;
                gbc_textRuleToExportLog.gridx = 2;
                gbc_textRuleToExportLog.gridy = 6;
                logPanel.add(textRuleToExportLog, gbc_textRuleToExportLog);
            }
            GridBagConstraints gbc_btnExportLogs = new GridBagConstraints();
            gbc_btnExportLogs.insets = new Insets(0, 0, 5, 5);
            gbc_btnExportLogs.gridx = 4;
            gbc_btnExportLogs.gridy = 6;
            logPanel.add(btnExportLogs, gbc_btnExportLogs);
        }
        {
            textFieldResult = new JTextField();
            textFieldResult.setEditable(false);
            GridBagConstraints gbc_textFieldResult = new GridBagConstraints();
            gbc_textFieldResult.gridwidth = 3;
            gbc_textFieldResult.insets = new Insets(0, 0, 5, 5);
            gbc_textFieldResult.fill = GridBagConstraints.HORIZONTAL;
            gbc_textFieldResult.gridx = 2;
            gbc_textFieldResult.gridy = 7;
            logPanel.add(textFieldResult, gbc_textFieldResult);
            textFieldResult.setColumns(10);
        }

    }

    private void initShutdown(JTabbedPane tabbedPane) {
        JPanel shutdownPanel = new JPanel();
        tabbedPane.addTab(Messages.getString("AdminR66OperationsGui.57"), null, shutdownPanel, null); //$NON-NLS-1$
        GridBagLayout gbl_toolsPanel = new GridBagLayout();
        gbl_toolsPanel.columnWidths = new int[] { 0, 0, 0, 0, 0, 0, 0 };
        gbl_toolsPanel.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        gbl_toolsPanel.columnWeights = new double[] { 0.0, 0.0, 1.0, 1.0 };
        gbl_toolsPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
        shutdownPanel.setLayout(gbl_toolsPanel);

        {
            btnShutdown = new JButton(Messages.getString("AdminR66OperationsGui.Shutdown")); //$NON-NLS-1$
            btnShutdown.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    R66AdminGuiActions action = new R66AdminGuiActions(
                            R66AdminGuiActions.SHUTDOWN);
                    action.execute();
                }
            });
            {
                lblPassword = new JLabel(Messages.getString("AdminR66OperationsGui.Password")); //$NON-NLS-1$
                lblPassword.setMinimumSize(new Dimension(60, 14));
                lblPassword.setMaximumSize(new Dimension(60, 14));
                GridBagConstraints gbc_lblPassword = new GridBagConstraints();
                gbc_lblPassword.anchor = GridBagConstraints.EAST;
                gbc_lblPassword.insets = new Insets(0, 0, 5, 5);
                gbc_lblPassword.gridx = 1;
                gbc_lblPassword.gridy = 4;
                shutdownPanel.add(lblPassword, gbc_lblPassword);
            }
            {
                passwordField = new JPasswordField();
                GridBagConstraints gbc_passwordField = new GridBagConstraints();
                gbc_passwordField.insets = new Insets(0, 0, 5, 5);
                gbc_passwordField.fill = GridBagConstraints.HORIZONTAL;
                gbc_passwordField.gridx = 2;
                gbc_passwordField.gridy = 4;
                shutdownPanel.add(passwordField, gbc_passwordField);
            }
            GridBagConstraints gbc_btnShutdown = new GridBagConstraints();
            gbc_btnShutdown.insets = new Insets(0, 0, 5, 5);
            gbc_btnShutdown.gridx = 3;
            gbc_btnShutdown.gridy = 4;
            shutdownPanel.add(btnShutdown, gbc_btnShutdown);
        }
        {
            rdbtnShutdown = new JRadioButton(Messages.getString("AdminR66OperationsGui.Shutdown")); //$NON-NLS-1$
            rdbtnShutdown.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    if (rdbtnShutdown.isSelected()) {
                        chckbxRestart.setEnabled(true);
                        chckbxBlockUnblock.setEnabled(false);
                    } else {
                        chckbxRestart.setEnabled(false);
                        chckbxBlockUnblock.setEnabled(true);
                    }
                }
            });
            buttonGroup.add(rdbtnShutdown);
            GridBagConstraints gbc_rdbtnShutdown = new GridBagConstraints();
            gbc_rdbtnShutdown.insets = new Insets(0, 0, 5, 5);
            gbc_rdbtnShutdown.gridx = 2;
            gbc_rdbtnShutdown.gridy = 6;
            shutdownPanel.add(rdbtnShutdown, gbc_rdbtnShutdown);
            rdbtnShutdown.setSelected(true);
        }
        {
            chckbxRestart = new JCheckBox(Messages.getString("AdminR66OperationsGui.Restart")); //$NON-NLS-1$
            GridBagConstraints gbc_chckbxRestart = new GridBagConstraints();
            gbc_chckbxRestart.insets = new Insets(0, 0, 5, 5);
            gbc_chckbxRestart.gridx = 3;
            gbc_chckbxRestart.gridy = 6;
            shutdownPanel.add(chckbxRestart, gbc_chckbxRestart);
        }
        {
            rdbtnBlock = new JRadioButton(Messages.getString("AdminR66OperationsGui.Block")); //$NON-NLS-1$
            rdbtnBlock.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    if (rdbtnBlock.isSelected()) {
                        chckbxRestart.setEnabled(false);
                        chckbxBlockUnblock.setEnabled(true);
                    } else {
                        chckbxRestart.setEnabled(true);
                        chckbxBlockUnblock.setEnabled(false);
                    }
                }
            });
            buttonGroup.add(rdbtnBlock);
            GridBagConstraints gbc_rdbtnBlock = new GridBagConstraints();
            gbc_rdbtnBlock.insets = new Insets(0, 0, 5, 5);
            gbc_rdbtnBlock.gridx = 2;
            gbc_rdbtnBlock.gridy = 7;
            shutdownPanel.add(rdbtnBlock, gbc_rdbtnBlock);
        }
        {
            chckbxBlockUnblock = new JCheckBox(Messages.getString("AdminR66OperationsGui.63")); //$NON-NLS-1$
            GridBagConstraints gbc_chckbxBlockUnblock = new GridBagConstraints();
            gbc_chckbxBlockUnblock.insets = new Insets(0, 0, 5, 5);
            gbc_chckbxBlockUnblock.gridx = 3;
            gbc_chckbxBlockUnblock.gridy = 7;
            shutdownPanel.add(chckbxBlockUnblock, gbc_chckbxBlockUnblock);
            chckbxBlockUnblock.setEnabled(false);
        }

    }

    private void close() {
        this.adminGui.setEnabled(true);
        this.adminGui.requestFocus();
        this.setVisible(false);
    }

    /**
     * @author Frederic Bregier
     * 
     */
    public class R66AdminGuiActions extends SwingWorker<String, Integer> {
        static final int BANDWIDTHGET = 1;
        static final int BANDWIDTHSET = 2;
        static final int CONFIGEXPORT = 3;
        static final int CONFIGIMPORT = 4;
        static final int LOGEXPORT = 5;
        static final int SHUTDOWN = 6;

        int method;

        R66AdminGuiActions(int method) {
            this.method = method;
        }

        @Override
        protected String doInBackground() throws Exception {
            disableAllButtons();
            startRequest();
            switch (method) {
                case BANDWIDTHGET:
                    getBandwidth();
                    break;
                case BANDWIDTHSET:
                    setBandwidth();
                    break;
                case CONFIGEXPORT:
                    getConfig();
                    break;
                case CONFIGIMPORT:
                    setConfig();
                    break;
                case LOGEXPORT:
                    exportLog();
                    break;
                case SHUTDOWN:
                    shutdown();
                    break;
                default:
                    AdminGui.getEnvironnement().GuiResultat = Messages.getString("R66ClientGui.24"); //$NON-NLS-1$
            }
            setStatus(AdminGui.getEnvironnement().GuiResultat);
            showDialog();
            stopRequest();
            return AdminGui.getEnvironnement().GuiResultat;
        }
    }

    private File openFile(String currentValue, String text, String extension) {
        JFileChooser chooser = null;
        if (currentValue != null) {
            String file = currentValue;
            File ffile = new File(file).getParentFile();
            chooser = new JFileChooser(ffile);
        }
        if (chooser == null) {
            chooser = new JFileChooser(System.getProperty("user.dir"));
        }
        if (extension != null) {
            FileExtensionFilter filter = new FileExtensionFilter(extension, text);
            chooser.setFileFilter(filter);
        }
        chooser.setDialogTitle(text);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    private void showDialog() {
        disableAllButtons();
        if (dialog != null) {
            dialog.dispose();
            dialog = null;
        }
        if (dialog == null) {
            dialog = new R66Dialog();
            dialog.setLocationRelativeTo(myself);
            if (dialog.isAlwaysOnTopSupported()) {
                dialog.setAlwaysOnTop(true);
            } else {
                dialog.toFront();
            }
        }
        dialog.getTextPaneDialog().setText(AdminGui.getEnvironnement().GuiResultat);
        dialog.setVisible(true);
        dialog.requestFocus();
    }

    private void setStatus(String mesg) {
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
        myself.toFront();
        myself.requestFocus();
    }

    public void disableAllButtons() {
        // frmRClientGui.setEnabled(false);
        comboBoxServer.setEnabled(false);
        btnGetBandwidthCurrent.setEnabled(false);
        btnSetBandwidthConfiguration.setEnabled(false);
    }

    public void enableAllButtons() {
        // frmRClientGui.setEnabled(true);
        //System.err.println("Versions: "+Configuration.configuration.versions);
        try {
            int idx = comboBoxServer.getSelectedIndex();
            comboBoxServer.removeAllItems();
            DbSession session = DbConstant.admin != null ? DbConstant.admin.getSession() : null;
            for (DbHostAuth auth : DbHostAuth.getAllHosts()) {
                //System.err.println("Add: "+auth.toString());
                comboBoxServer.addItem(auth);
            }
            comboBoxServer.setSelectedIndex(idx);
        } catch (WaarpDatabaseNoConnectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        comboBoxServer.setEnabled(true);
        btnGetBandwidthCurrent.setEnabled(true);
        btnSetBandwidthConfiguration.setEnabled(true);
        myself.toFront();
    }

    public void getBandwidth() {
        DbHostAuth host = (DbHostAuth) comboBoxServer.getSelectedItem();
        if (host == null) {
            AdminGui.getEnvironnement().GuiResultat = Messages.getString("AdminR66OperationsGui.164"); //$NON-NLS-1$
            return;
        }
        long time1 = System.currentTimeMillis();
        R66Future future = new R66Future(true);
        ChangeBandwidthLimits bandwidthLimits =
                new ChangeBandwidthLimits(future, -1, -1, -1, -1,
                        AdminGui.getEnvironnement().networkTransaction);
        bandwidthLimits.setHost(host);
        bandwidthLimits.run();
        future.awaitUninterruptibly();
        long time2 = System.currentTimeMillis();
        long delay = time2 - time1;
        R66Result result = future.getResult();
        String message = null;
        boolean useJson = PartnerConfiguration.useJson(host.getHostid());
        logger.debug("UseJson: " + useJson);
        if (future.isSuccess()) {
            String sresult = null;
            if (result.getOther() != null) {
                if (useJson) {
                    JsonCommandPacket packet = (JsonCommandPacket) result.getOther();
                    sresult = packet.getRequest();
                    BandwidthJsonPacket node = (BandwidthJsonPacket) packet.getJsonRequest();
                    globWriteLimit.setValue(node.getWriteglobal());
                    globReadLimit.setValue(node.getReadglobal());
                    sessionWriteLimit.setValue(node.getWritesession());
                    sessionReadLimit.setValue(node.getReadsession());
                } else {
                    ValidPacket packet = (ValidPacket) result.getOther();
                    sresult = packet.getSheader();
                    String[] values = sresult.split(" ");
                    Long gw = Long.parseLong(values[0]);
                    Long gr = Long.parseLong(values[1]);
                    Long sw = Long.parseLong(values[2]);
                    Long sr = Long.parseLong(values[3]);
                    globWriteLimit.setValue(gw);
                    globReadLimit.setValue(gr);
                    sessionWriteLimit.setValue(sw);
                    sessionReadLimit.setValue(sr);
                }
            }
            if (result.getCode() == ErrorCode.Warning) {
                message = Messages.getString("RequestInformation.Warned") + Messages.getString("AdminR66OperationsGui.70") + //$NON-NLS-1$
                        (result.getOther() != null ? sresult :
                                Messages.getString("AdminR66OperationsGui.71")) //$NON-NLS-1$
                        + Messages.getString("AdminR66OperationsGui.72") + delay; //$NON-NLS-1$
            } else {
                message = Messages.getString("RequestInformation.Success") + Messages.getString("AdminR66OperationsGui.70") + //$NON-NLS-1$
                        (result.getOther() != null ? sresult :
                                Messages.getString("AdminR66OperationsGui.71")) //$NON-NLS-1$
                        + Messages.getString("AdminR66OperationsGui.72") + delay; //$NON-NLS-1$
            }
        } else {
            if (result.getCode() == ErrorCode.Warning) {
                message = Messages.getString("RequestInformation.Warned") + Messages.getString("AdminR66OperationsGui.70") + future.getCause(); //$NON-NLS-1$
            } else {
                message = Messages.getString("RequestInformation.Failure") + Messages.getString("AdminR66OperationsGui.70") + future.getCause(); //$NON-NLS-1$
            }
        }
        AdminGui.getEnvironnement().GuiResultat = message;
    }

    public void setBandwidth() {
        DbHostAuth host = (DbHostAuth) comboBoxServer.getSelectedItem();
        if (host == null) {
            AdminGui.getEnvironnement().GuiResultat = Messages.getString("AdminR66OperationsGui.164"); //$NON-NLS-1$
            return;
        }
        long time1 = System.currentTimeMillis();
        R66Future future = new R66Future(true);
        ChangeBandwidthLimits bandwidthLimits =
                new ChangeBandwidthLimits(future, (Long) globWriteLimit.getValue(),
                        (Long) globReadLimit.getValue(),
                        (Long) sessionWriteLimit.getValue(),
                        (Long) sessionReadLimit.getValue(),
                        AdminGui.getEnvironnement().networkTransaction);
        bandwidthLimits.setHost(host);
        bandwidthLimits.run();
        future.awaitUninterruptibly();
        long time2 = System.currentTimeMillis();
        long delay = time2 - time1;
        R66Result result = future.getResult();
        String message = null;
        boolean useJson = PartnerConfiguration.useJson(host.getHostid());
        logger.debug("UseJson: " + useJson);
        if (future.isSuccess()) {
            String sresult = null;
            if (result.getOther() != null) {
                if (useJson) {
                    JsonCommandPacket packet = (JsonCommandPacket) result.getOther();
                    sresult = packet.getRequest();

                } else {
                    ValidPacket packet = (ValidPacket) result.getOther();
                    sresult = packet.getSheader();
                }
            }
            if (result.getCode() == ErrorCode.Warning) {
                message = Messages.getString("RequestInformation.Warned") + Messages.getString("AdminR66OperationsGui.70") + //$NON-NLS-1$
                        (result.getOther() != null ? sresult :
                                Messages.getString("AdminR66OperationsGui.71")) //$NON-NLS-1$
                        + Messages.getString("AdminR66OperationsGui.72") + delay; //$NON-NLS-1$
            } else {
                message = Messages.getString("RequestInformation.Success") + Messages.getString("AdminR66OperationsGui.70") + //$NON-NLS-1$
                        (result.getOther() != null ? sresult :
                                Messages.getString("AdminR66OperationsGui.71")) //$NON-NLS-1$
                        + Messages.getString("AdminR66OperationsGui.72") + delay; //$NON-NLS-1$
            }
        } else {
            if (result.getCode() == ErrorCode.Warning) {
                message = Messages.getString("RequestInformation.Warned") + Messages.getString("AdminR66OperationsGui.70") + future.getCause(); //$NON-NLS-1$
            } else {
                message = Messages.getString("RequestInformation.Failure") + Messages.getString("AdminR66OperationsGui.70") + future.getCause(); //$NON-NLS-1$
            }
        }
        AdminGui.getEnvironnement().GuiResultat = message;
    }

    public void getConfig() {
        DbHostAuth host = (DbHostAuth) comboBoxServer.getSelectedItem();
        if (host == null) {
            AdminGui.getEnvironnement().GuiResultat = Messages.getString("AdminR66OperationsGui.164"); //$NON-NLS-1$
            return;
        }
        long time1 = System.currentTimeMillis();
        R66Future future = new R66Future(true);
        boolean getHost = chckbxHosts.isSelected();
        boolean getRule = chckbxRules.isSelected();
        boolean getBusiness = chckbxBusiness.isSelected();
        boolean getAlias = chckbxAlias.isSelected();
        boolean getRoles = chckbxRoles.isSelected();
        String ruleToGet = (String) textRuleUsedToGet.getSelectedItem();
        ConfigExport export = new ConfigExport(future, getHost, getRule, getBusiness, getAlias, getRoles,
                AdminGui.getEnvironnement().networkTransaction);
        export.setHost(host);
        export.run();
        future.awaitUninterruptibly();
        long time2 = System.currentTimeMillis();
        long delay = time2 - time1;
        R66Result result = future.getResult();
        String message = "";
        boolean useJson = PartnerConfiguration.useJson(host.getHostid());
        logger.debug("UseJson: " + useJson);
        if (future.isSuccess()) {
            String resume = "";
            if (useJson) {
                JsonCommandPacket packet = (JsonCommandPacket) result.getOther();
                if (packet != null) {
                    resume = packet.getRequest();
                    ConfigExportResponseJsonPacket node = (ConfigExportResponseJsonPacket) packet.getJsonRequest();
                    logger.debug(node.toString());
                    String shost = node.getFilehost();
                    String srule = node.getFilerule();
                    String sbusiness = node.getFilebusiness();
                    String salias = node.getFilealias();
                    String srole = node.getFileroles();
                    if (ruleToGet == null || ruleToGet.isEmpty()) {
                        message = Messages.getString("AdminR66OperationsGui.92"); //$NON-NLS-1$
                    }
                    if (message.length() > 1) {
                        // error
                        message = Messages.getString("RequestInformation.Failure") + Messages.getString("AdminR66OperationsGui.123") + message; //$NON-NLS-1$
                    } else {
                        if (getHost && shost != null && !shost.isEmpty()) {
                            future = new R66Future(true);
                            DirectTransfer transfer = new DirectTransfer(
                                    future,
                                    host.getHostid(),
                                    shost,
                                    ruleToGet,
                                    Messages.getString("AdminR66OperationsGui.Hosts") + Messages.getString("AdminR66OperationsGui.GetConfig") //$NON-NLS-1$
                                            + AdminGui.getEnvironnement().hostId,
                                    AdminGui.getEnvironnement().isMD5, Configuration.configuration.getBLOCKSIZE(),
                                    DbConstant.ILLEGALVALUE,
                                    AdminGui.getEnvironnement().networkTransaction);
                            transfer.run();
                            if (future.isSuccess()) {
                                R66Result resultHost = future.getResult();
                                shost = resultHost.getFile().getTrueFile().getAbsolutePath();
                                message += Messages.getString("AdminR66OperationsGui.Hosts") + Messages.getString("AdminR66OperationsGui.FileInto") + shost + "\n"; //$NON-NLS-1$
                            } else {
                                getHost = false;
                                shost = Messages.getString("AdminR66OperationsGui.CantGetFile") + Messages.getString("AdminR66OperationsGui.Hosts") + ": " + shost; //$NON-NLS-1$
                                message += shost;
                            }
                        }
                        if (getRule && srule != null && !srule.isEmpty()) {
                            future = new R66Future(true);
                            DirectTransfer transfer = new DirectTransfer(
                                    future,
                                    host.getHostid(),
                                    srule,
                                    ruleToGet,
                                    Messages.getString("AdminR66OperationsGui.Rules") + Messages.getString("AdminR66OperationsGui.GetConfig") //$NON-NLS-1$
                                            + AdminGui.getEnvironnement().hostId,
                                    AdminGui.getEnvironnement().isMD5, Configuration.configuration.getBLOCKSIZE(),
                                    DbConstant.ILLEGALVALUE,
                                    AdminGui.getEnvironnement().networkTransaction);
                            transfer.run();
                            if (future.isSuccess()) {
                                R66Result resultRule = future.getResult();
                                srule = resultRule.getFile().getTrueFile().getAbsolutePath();
                                message += Messages.getString("AdminR66OperationsGui.Rules") + Messages.getString("AdminR66OperationsGui.FileInto") + srule + "\n"; //$NON-NLS-1$
                            } else {
                                getRule = false;
                                srule = Messages.getString("AdminR66OperationsGui.CantGetFile") + Messages.getString("AdminR66OperationsGui.Rules") + ": " + srule; //$NON-NLS-1$
                                message += srule;
                            }
                        }
                        if (getBusiness && sbusiness != null && !sbusiness.isEmpty()) {
                            future = new R66Future(true);
                            DirectTransfer transfer = new DirectTransfer(
                                    future,
                                    host.getHostid(),
                                    sbusiness,
                                    ruleToGet,
                                    Messages.getString("AdminR66OperationsGui.Business") + Messages.getString("AdminR66OperationsGui.GetConfig") //$NON-NLS-1$
                                            + AdminGui.getEnvironnement().hostId,
                                    AdminGui.getEnvironnement().isMD5, Configuration.configuration.getBLOCKSIZE(),
                                    DbConstant.ILLEGALVALUE,
                                    AdminGui.getEnvironnement().networkTransaction);
                            transfer.run();
                            if (future.isSuccess()) {
                                R66Result resultRule = future.getResult();
                                sbusiness = resultRule.getFile().getTrueFile().getAbsolutePath();
                                message += Messages.getString("AdminR66OperationsGui.Business") + Messages.getString("AdminR66OperationsGui.FileInto") + sbusiness + "\n"; //$NON-NLS-1$
                            } else {
                                getBusiness = false;
                                sbusiness = Messages.getString("AdminR66OperationsGui.CantGetFile") + Messages.getString("AdminR66OperationsGui.Business") + ": " + sbusiness; //$NON-NLS-1$
                                message += sbusiness;
                            }
                        }
                        if (getAlias && salias != null && !salias.isEmpty()) {
                            future = new R66Future(true);
                            DirectTransfer transfer = new DirectTransfer(
                                    future,
                                    host.getHostid(),
                                    salias,
                                    ruleToGet,
                                    Messages.getString("AdminR66OperationsGui.Alias") + Messages.getString("AdminR66OperationsGui.GetConfig") //$NON-NLS-1$
                                            + AdminGui.getEnvironnement().hostId,
                                    AdminGui.getEnvironnement().isMD5, Configuration.configuration.getBLOCKSIZE(),
                                    DbConstant.ILLEGALVALUE,
                                    AdminGui.getEnvironnement().networkTransaction);
                            transfer.run();
                            if (future.isSuccess()) {
                                R66Result resultRule = future.getResult();
                                salias = resultRule.getFile().getTrueFile().getAbsolutePath();
                                message += Messages.getString("AdminR66OperationsGui.Alias") + Messages.getString("AdminR66OperationsGui.FileInto") + salias + "\n"; //$NON-NLS-1$
                            } else {
                                getAlias = false;
                                salias = Messages.getString("AdminR66OperationsGui.CantGetFile") + Messages.getString("AdminR66OperationsGui.Alias") + ": " + salias; //$NON-NLS-1$
                                message += salias;
                            }
                        }
                        if (getRoles && srole != null && !srole.isEmpty()) {
                            future = new R66Future(true);
                            DirectTransfer transfer = new DirectTransfer(
                                    future,
                                    host.getHostid(),
                                    srole,
                                    ruleToGet,
                                    Messages.getString("AdminR66OperationsGui.Roles") + Messages.getString("AdminR66OperationsGui.GetConfig") //$NON-NLS-1$
                                            + AdminGui.getEnvironnement().hostId,
                                    AdminGui.getEnvironnement().isMD5, Configuration.configuration.getBLOCKSIZE(),
                                    DbConstant.ILLEGALVALUE,
                                    AdminGui.getEnvironnement().networkTransaction);
                            transfer.run();
                            if (future.isSuccess()) {
                                R66Result resultRule = future.getResult();
                                srole = resultRule.getFile().getTrueFile().getAbsolutePath();
                                message += Messages.getString("AdminR66OperationsGui.Roles") + Messages.getString("AdminR66OperationsGui.FileInto") + srole; //$NON-NLS-1$
                            } else {
                                getRoles = false;
                                srole = Messages.getString("AdminR66OperationsGui.CantGetFile") + Messages.getString("AdminR66OperationsGui.Roles") + ": " + srole; //$NON-NLS-1$
                                message += srole;
                            }
                        }
                        if (getHost) {
                            textFieldHosts.setText(shost);
                        }
                        if (getRule) {
                            textFieldRules.setText(srule);
                        }
                        if (getBusiness) {
                            textFieldBusiness.setText(sbusiness);
                        }
                        if (getAlias) {
                            textFieldAlias.setText(salias);
                        }
                        if (getRoles) {
                            textFieldRoles.setText(srole);
                        }
                    }
                }
            } else {
                ValidPacket packet = (ValidPacket) result.getOther();
                if (packet != null) {
                    resume = packet.getSheader();
                    String[] values = resume.split(" ");
                    String shost = values[0];
                    String srule = null;
                    if (values.length > 1) {
                        srule = values[1];
                    }
                    if (ruleToGet == null || ruleToGet.isEmpty()) {
                        message = Messages.getString("AdminR66OperationsGui.92"); //$NON-NLS-1$
                    }
                    if (message.length() > 1) {
                        // error
                        message = Messages.getString("RequestInformation.Failure") + Messages.getString("AdminR66OperationsGui.123") + message; //$NON-NLS-1$
                    } else {
                        // get config files
                        if (getHost && shost != null && !shost.isEmpty()) {
                            future = new R66Future(true);
                            DirectTransfer transfer = new DirectTransfer(
                                    future,
                                    host.getHostid(),
                                    shost,
                                    ruleToGet,
                                    Messages.getString("AdminR66OperationsGui.Hosts") + Messages.getString("AdminR66OperationsGui.GetConfig") //$NON-NLS-1$
                                            + AdminGui.getEnvironnement().hostId,
                                    AdminGui.getEnvironnement().isMD5, Configuration.configuration.getBLOCKSIZE(),
                                    DbConstant.ILLEGALVALUE,
                                    AdminGui.getEnvironnement().networkTransaction);
                            transfer.run();
                            if (future.isSuccess()) {
                                R66Result resultHost = future.getResult();
                                shost = resultHost.getFile().getTrueFile().getAbsolutePath();
                                message += Messages.getString("AdminR66OperationsGui.Hosts") + Messages.getString("AdminR66OperationsGui.FileInto") + shost + "\n"; //$NON-NLS-1$
                            } else {
                                getHost = false;
                                shost = Messages.getString("AdminR66OperationsGui.CantGetFile") + Messages.getString("AdminR66OperationsGui.Hosts") + ": " + shost; //$NON-NLS-1$
                                message += shost;
                            }
                        }
                        if (getRule && srule != null && !srule.isEmpty()) {
                            future = new R66Future(true);
                            DirectTransfer transfer = new DirectTransfer(
                                    future,
                                    host.getHostid(),
                                    srule,
                                    ruleToGet,
                                    Messages.getString("AdminR66OperationsGui.Rules") + Messages.getString("AdminR66OperationsGui.GetConfig") //$NON-NLS-1$
                                            + AdminGui.getEnvironnement().hostId,
                                    AdminGui.getEnvironnement().isMD5, Configuration.configuration.getBLOCKSIZE(),
                                    DbConstant.ILLEGALVALUE,
                                    AdminGui.getEnvironnement().networkTransaction);
                            transfer.run();
                            if (future.isSuccess()) {
                                R66Result resultRule = future.getResult();
                                srule = resultRule.getFile().getTrueFile().getAbsolutePath();
                                message += Messages.getString("AdminR66OperationsGui.Rules") + Messages.getString("AdminR66OperationsGui.FileInto") + srule; //$NON-NLS-1$
                            } else {
                                getRule = false;
                                srule = Messages.getString("AdminR66OperationsGui.CantGetFile") + Messages.getString("AdminR66OperationsGui.Rules") + ": " + srule; //$NON-NLS-1$
                                message += srule;
                            }
                        }
                        // then set downloaded file to text fields
                        if (getHost) {
                            textFieldHosts.setText(shost);
                        }
                        if (getRule) {
                            textFieldRules.setText(srule);
                        }
                    }
                }
            }
            if (result.getCode() == ErrorCode.Warning) {
                message = Messages.getString("RequestInformation.Warned") + Messages.getString("AdminR66OperationsGui.123") //$NON-NLS-1$
                        +
                        (result.getOther() != null ? resume + message
                                :
                                Messages.getString("AdminR66OperationsGui.71")) //$NON-NLS-1$
                        + Messages.getString("AdminR66OperationsGui.72") + delay; //$NON-NLS-1$
            } else {
                message = Messages.getString("RequestInformation.Success") + Messages.getString("AdminR66OperationsGui.123") //$NON-NLS-1$
                        +
                        (result.getOther() != null ? resume + message
                                :
                                Messages.getString("AdminR66OperationsGui.71")) //$NON-NLS-1$
                        + Messages.getString("AdminR66OperationsGui.72") + delay; //$NON-NLS-1$
            }
        } else {
            if (result.getCode() == ErrorCode.Warning) {
                message = Messages.getString("RequestInformation.Warned") + Messages.getString("AdminR66OperationsGui.123") + future.getCause(); //$NON-NLS-1$
            } else {
                message = Messages.getString("RequestInformation.Failure") + Messages.getString("AdminR66OperationsGui.123") + future.getCause(); //$NON-NLS-1$
            }
        }
        AdminGui.getEnvironnement().GuiResultat = message;
    }

    public void setConfig() {
        DbHostAuth host = (DbHostAuth) comboBoxServer.getSelectedItem();
        if (host == null) {
            AdminGui.getEnvironnement().GuiResultat = Messages.getString("AdminR66OperationsGui.164"); //$NON-NLS-1$
            return;
        }
        long time1 = System.currentTimeMillis();
        R66Future future = null;
        String hostfile = textFieldHosts.getText();
        String rulefile = textFieldRules.getText();
        String businessfile = textFieldBusiness.getText();
        String aliasfile = textFieldAlias.getText();
        String rolefile = textFieldRoles.getText();
        long hostid = DbConstant.ILLEGALVALUE, ruleid = DbConstant.ILLEGALVALUE, businessid = DbConstant.ILLEGALVALUE, aliasid = DbConstant.ILLEGALVALUE, roleid = DbConstant.ILLEGALVALUE;
        boolean erazeHost = chckbxPurgeHosts.isSelected();
        boolean erazeRule = chckbxPurgeRules.isSelected();
        boolean erazeBusiness = chckbxPurgeBusiness.isSelected();
        boolean erazeAlias = chckbxPurgeAlias.isSelected();
        boolean erazeRole = chckbxPurgeRoles.isSelected();
        String ruleToPut = (String) textRuleToPut.getSelectedItem();
        String error = "";
        String msg = "";
        // should send config files first
        if ((hostfile == null || hostfile.isEmpty()) &&
                (rulefile == null || rulefile.isEmpty()) &&
                (businessfile == null || businessfile.isEmpty()) &&
                (aliasfile == null || aliasfile.isEmpty()) &&
                (rolefile == null || rolefile.isEmpty())) {
            error = Messages.getString("AdminR66OperationsGui.134"); //$NON-NLS-1$
        }
        if (ruleToPut == null || ruleToPut.isEmpty()) {
            error = Messages.getString("AdminR66OperationsGui.135"); //$NON-NLS-1$
        }
        String message = null;
        if (error.length() > 1) {
            // error
            message = Messages.getString("RequestInformation.Failure") + Messages.getString("AdminR66OperationsGui.123") + error; //$NON-NLS-1$
            AdminGui.getEnvironnement().GuiResultat = message;
            return;
        }
        if (hostfile != null && hostfile.length() > 1) {
            future = new R66Future(true);
            DirectTransfer transfer = new DirectTransfer(
                    future,
                    host.getHostid(),
                    hostfile,
                    ruleToPut,
                    Messages.getString("AdminR66OperationsGui.Hosts") + Messages.getString("AdminR66OperationsGui.SetConfig") //$NON-NLS-1$
                            + AdminGui.getEnvironnement().hostId,
                    AdminGui.getEnvironnement().isMD5, Configuration.configuration.getBLOCKSIZE(),
                    DbConstant.ILLEGALVALUE,
                    AdminGui.getEnvironnement().networkTransaction);
            transfer.run();
            if (!future.isSuccess()) {
                error = Messages.getString("AdminR66OperationsGui.138") + hostfile; //$NON-NLS-1$
            } else {
                msg += " " + Messages.getString("AdminR66OperationsGui.Hosts") + Messages.getString("AdminR66OperationsGui.ConfigTransmitted"); //$NON-NLS-1$
                hostid = future.getRunner().getSpecialId();
            }
        }
        if (error.length() > 1) {
            // error
            message = Messages.getString("RequestInformation.Failure") + Messages.getString("AdminR66OperationsGui.158") + error; //$NON-NLS-1$
            AdminGui.getEnvironnement().GuiResultat = message;
            return;
        }
        if (rulefile != null && rulefile.length() > 1) {
            future = new R66Future(true);
            DirectTransfer transfer = new DirectTransfer(
                    future,
                    host.getHostid(),
                    rulefile,
                    ruleToPut,
                    Messages.getString("AdminR66OperationsGui.Rules") + Messages.getString("AdminR66OperationsGui.SetConfig") //$NON-NLS-1$
                            + AdminGui.getEnvironnement().hostId,
                    AdminGui.getEnvironnement().isMD5, Configuration.configuration.getBLOCKSIZE(),
                    DbConstant.ILLEGALVALUE,
                    AdminGui.getEnvironnement().networkTransaction);
            transfer.run();
            if (!future.isSuccess()) {
                error += Messages.getString("AdminR66OperationsGui.142") + rulefile; //$NON-NLS-1$
            } else {
                msg += " " + Messages.getString("AdminR66OperationsGui.Rules") + Messages.getString("AdminR66OperationsGui.ConfigTransmitted"); //$NON-NLS-1$
                ruleid = future.getRunner().getSpecialId();
            }
        }
        if (error.length() > 1) {
            // error
            message = Messages.getString("RequestInformation.Failure") + Messages.getString("AdminR66OperationsGui.158") + error; //$NON-NLS-1$
            AdminGui.getEnvironnement().GuiResultat = message;
            return;
        }
        boolean useJson = PartnerConfiguration.useJson(host.getHostid());
        logger.debug("UseJson: " + useJson); //$NON-NLS-1$
        if (useJson) {
            if (businessfile != null && businessfile.length() > 1) {
                future = new R66Future(true);
                DirectTransfer transfer = new DirectTransfer(
                        future,
                        host.getHostid(),
                        businessfile,
                        ruleToPut,
                        Messages.getString("AdminR66OperationsGui.Business") + Messages.getString("AdminR66OperationsGui.SetConfig") //$NON-NLS-1$
                                + AdminGui.getEnvironnement().hostId,
                        AdminGui.getEnvironnement().isMD5, Configuration.configuration.getBLOCKSIZE(),
                        DbConstant.ILLEGALVALUE,
                        AdminGui.getEnvironnement().networkTransaction);
                transfer.run();
                if (!future.isSuccess()) {
                    error += Messages.getString("AdminR66OperationsGui.142") + businessfile; //$NON-NLS-1$
                } else {
                    msg += " " + Messages.getString("AdminR66OperationsGui.Business") + Messages.getString("AdminR66OperationsGui.ConfigTransmitted"); //$NON-NLS-1$
                    businessid = future.getRunner().getSpecialId();
                }
            }
            if (error.length() > 1) {
                // error
                message = Messages.getString("RequestInformation.Failure") + Messages.getString("AdminR66OperationsGui.158") + error; //$NON-NLS-1$
                AdminGui.getEnvironnement().GuiResultat = message;
                return;
            }
            if (aliasfile != null && aliasfile.length() > 1) {
                future = new R66Future(true);
                DirectTransfer transfer = new DirectTransfer(
                        future,
                        host.getHostid(),
                        aliasfile,
                        ruleToPut,
                        Messages.getString("AdminR66OperationsGui.Alias") + Messages.getString("AdminR66OperationsGui.SetConfig") //$NON-NLS-1$
                                + AdminGui.getEnvironnement().hostId,
                        AdminGui.getEnvironnement().isMD5, Configuration.configuration.getBLOCKSIZE(),
                        DbConstant.ILLEGALVALUE,
                        AdminGui.getEnvironnement().networkTransaction);
                transfer.run();
                if (!future.isSuccess()) {
                    error += Messages.getString("AdminR66OperationsGui.142") + aliasfile; //$NON-NLS-1$
                } else {
                    msg += " " + Messages.getString("AdminR66OperationsGui.Alias") + Messages.getString("AdminR66OperationsGui.ConfigTransmitted"); //$NON-NLS-1$
                    aliasid = future.getRunner().getSpecialId();
                }
            }
            if (error.length() > 1) {
                // error
                message = Messages.getString("RequestInformation.Failure") + Messages.getString("AdminR66OperationsGui.158") + error; //$NON-NLS-1$
                AdminGui.getEnvironnement().GuiResultat = message;
                return;
            }
            if (rolefile != null && rolefile.length() > 1) {
                future = new R66Future(true);
                DirectTransfer transfer = new DirectTransfer(
                        future,
                        host.getHostid(),
                        rolefile,
                        ruleToPut,
                        Messages.getString("AdminR66OperationsGui.Roles") + Messages.getString("AdminR66OperationsGui.SetConfig") //$NON-NLS-1$
                                + AdminGui.getEnvironnement().hostId,
                        AdminGui.getEnvironnement().isMD5, Configuration.configuration.getBLOCKSIZE(),
                        DbConstant.ILLEGALVALUE,
                        AdminGui.getEnvironnement().networkTransaction);
                transfer.run();
                if (!future.isSuccess()) {
                    error += Messages.getString("AdminR66OperationsGui.142") + rolefile; //$NON-NLS-1$
                } else {
                    msg += " " + Messages.getString("AdminR66OperationsGui.Roles") + Messages.getString("AdminR66OperationsGui.ConfigTransmitted"); //$NON-NLS-1$
                    roleid = future.getRunner().getSpecialId();
                }
            }
            if (error.length() > 1) {
                // error
                message = Messages.getString("RequestInformation.Failure") + Messages.getString("AdminR66OperationsGui.158") + error; //$NON-NLS-1$
                AdminGui.getEnvironnement().GuiResultat = message;
                return;
            }
        }
        future = new R66Future(true);
        ConfigImport importCmd = new ConfigImport(future, erazeHost, erazeRule, erazeBusiness, erazeAlias, erazeRole,
                hostfile, rulefile, businessfile, aliasfile, rolefile,
                AdminGui.getEnvironnement().networkTransaction);
        importCmd.setSpecialIds(hostid, ruleid, businessid, aliasid, roleid);
        importCmd.setHost(host);
        importCmd.run();
        future.awaitUninterruptibly();
        long time2 = System.currentTimeMillis();
        long delay = time2 - time1;
        R66Result result = future.getResult();
        if (future.isSuccess()) {
            String resume = msg;
            if (result.getOther() != null) {
                if (useJson) {
                    resume = ((JsonCommandPacket) result.getOther()).getRequest();
                } else {
                    resume = ((ValidPacket) result.getOther()).getSheader();
                }
                resume += msg;
            }
            if (result.getCode() == ErrorCode.Warning) {
                message = Messages.getString("RequestInformation.Warned") + Messages.getString("AdminR66OperationsGui.158") + //$NON-NLS-1$
                        resume
                        + Messages.getString("AdminR66OperationsGui.72") + delay; //$NON-NLS-1$
            } else {
                message = Messages.getString("RequestInformation.Success") + Messages.getString("AdminR66OperationsGui.158") + //$NON-NLS-1$
                        resume
                        + Messages.getString("AdminR66OperationsGui.72") + delay; //$NON-NLS-1$
            }
        } else {
            if (result.getCode() == ErrorCode.Warning) {
                message = Messages.getString("RequestInformation.Warned") + Messages.getString("AdminR66OperationsGui.158") + future.getCause() + msg; //$NON-NLS-1$
            } else {
                message = Messages.getString("RequestInformation.Failure") + Messages.getString("AdminR66OperationsGui.158") + future.getCause(); //$NON-NLS-1$
            }
        }
        AdminGui.getEnvironnement().GuiResultat = message;
    }

    public void exportLog() {
        DbHostAuth host = (DbHostAuth) comboBoxServer.getSelectedItem();
        if (host == null) {
            AdminGui.getEnvironnement().GuiResultat = Messages.getString("AdminR66OperationsGui.164"); //$NON-NLS-1$
            return;
        }
        long time1 = System.currentTimeMillis();
        boolean purgeLog = chckbxPurge.isSelected();
        boolean clean = chckbxClean.isSelected();
        Timestamp start = null;
        Timestamp stop = null;
        String sstart = textFieldStart.getText();
        String sstop = textFieldStop.getText();
        if (sstart != null) {
            start = WaarpStringUtils.fixDate(sstart);
        }
        if (sstop != null) {
            stop = WaarpStringUtils.fixDate(sstop, start);
        }
        if (start == null && stop == null) {
            stop = WaarpStringUtils.getTodayMidnight();
        }
        if (start != null) {
            System.err
                    .println(Messages.getString("AdminR66OperationsGui.Start") + (new Date(start.getTime())).toString()); //$NON-NLS-1$
        }
        if (stop != null) {
            System.err
                    .println(Messages.getString("AdminR66OperationsGui.Stop") + (new Date(stop.getTime())).toString()); //$NON-NLS-1$
        }
        String rulefilter = textFieldLogRule.getText();
        if (rulefilter != null && rulefilter.isEmpty()) {
            rulefilter = null;
        }
        String hostfilter = textFieldLogHost.getText();
        if (hostfilter != null && hostfilter.isEmpty()) {
            hostfilter = null;
        }

        R66Future future = new R66Future(true);
        boolean useJson = PartnerConfiguration.useJson(host.getHostid());
        logger.debug("UseJson: " + useJson); //$NON-NLS-1$
        if (useJson) {
            LogExtendedExport export = new LogExtendedExport(future, clean, purgeLog, start,
                    stop, null, null, rulefilter, hostfilter,
                    chckbxPending.isSelected(), chckbxRunning.isSelected(), chckbxDone.isSelected(),
                    chckbxInError.isSelected(),
                    AdminGui.getEnvironnement().networkTransaction, host);
            export.run();
        } else {
            LogExport export = new LogExport(future, purgeLog, clean, start, stop,
                    AdminGui.getEnvironnement().networkTransaction);
            export.setHost(host);
            export.run();
        }
        future.awaitUninterruptibly();
        long time2 = System.currentTimeMillis();
        long delay = time2 - time1;
        R66Result result = future.getResult();
        String message = "";
        if (future.isSuccess()) {
            AbstractLocalPacket packet = (AbstractLocalPacket) result.getOther();
            String value = null;
            if (packet != null) {
                String fileExported = null;
                if (useJson) {
                    value = ((JsonCommandPacket) packet).getRequest();
                    LogResponseJsonPacket node = (LogResponseJsonPacket) ((JsonCommandPacket) packet).getJsonRequest();
                    fileExported = node.getFilename();
                } else {
                    value = ((ValidPacket) packet).getSheader();
                    String[] values = value.split(" ");
                    fileExported = values[0];
                }
                textFieldResult.setText(fileExported);
                // download logs
                if (fileExported != null && !fileExported.isEmpty()) {
                    String ruleToExport = (String) textRuleToExportLog.getSelectedItem();
                    if (ruleToExport == null || ruleToExport.isEmpty()) {
                        message = Messages.getString("AdminR66OperationsGui.170") + fileExported + Messages.getString("AdminR66OperationsGui.171"); //$NON-NLS-1$ //$NON-NLS-2$
                    } else {
                        future = new R66Future(true);
                        DirectTransfer transfer = new DirectTransfer(future, host.getHostid(),
                                fileExported,
                                ruleToExport, Messages.getString("AdminR66OperationsGui.172") //$NON-NLS-1$
                                        + AdminGui.getEnvironnement().hostId,
                                AdminGui.getEnvironnement().isMD5, Configuration.configuration.getBLOCKSIZE(),
                                DbConstant.ILLEGALVALUE,
                                AdminGui.getEnvironnement().networkTransaction);
                        transfer.run();
                        if (!future.isSuccess()) {
                            message = Messages.getString("AdminR66OperationsGui.170") + fileExported + "\n"; //$NON-NLS-1$
                        } else {
                            textFieldResult.setText(future.getResult().getFile().getTrueFile()
                                    .getAbsolutePath());
                        }
                    }
                }
            }
            if (result.getCode() == ErrorCode.Warning) {
                message += Messages.getString(
                        "AdminR66OperationsGui.175", Messages.getString("RequestInformation.Warned")) + //$NON-NLS-1$
                        (result.getOther() != null ? value :
                                Messages.getString("AdminR66OperationsGui.71")) //$NON-NLS-1$
                        + Messages.getString("AdminR66OperationsGui.72") + delay + "\n"; //$NON-NLS-1$
            } else {
                message += Messages.getString(
                        "AdminR66OperationsGui.175", Messages.getString("RequestInformation.Success")) + //$NON-NLS-1$
                        (result.getOther() != null ? value :
                                Messages.getString("AdminR66OperationsGui.71")) //$NON-NLS-1$
                        + Messages.getString("AdminR66OperationsGui.72") + delay; //$NON-NLS-1$
            }
        } else {
            if (result.getCode() == ErrorCode.Warning) {
                message += Messages.getString(
                        "AdminR66OperationsGui.175", Messages.getString("RequestInformation.Warned")) + future.getCause(); //$NON-NLS-1$
            } else {
                message += Messages.getString(
                        "AdminR66OperationsGui.175", Messages.getString("RequestInformation.Failure")) + future.getCause(); //$NON-NLS-1$
            }
        }
        AdminGui.getEnvironnement().GuiResultat = message;
    }

    public void shutdown() {
        DbHostAuth host = (DbHostAuth) comboBoxServer.getSelectedItem();
        if (host == null) {
            AdminGui.getEnvironnement().GuiResultat = Messages.getString("AdminR66OperationsGui.164"); //$NON-NLS-1$
            return;
        }
        String skey = null;
        try {
            char[] pwd = passwordField.getPassword();
            if (pwd == null || pwd.length == 0) {
                AdminGui.getEnvironnement().GuiResultat = Messages.getString("AdminR66OperationsGui.186"); //$NON-NLS-1$
                return;
            }
            skey = new String(pwd);
        } catch (NullPointerException e) {
            AdminGui.getEnvironnement().GuiResultat = Messages.getString("AdminR66OperationsGui.186"); //$NON-NLS-1$
            return;
        }
        long time1 = System.currentTimeMillis();
        byte[] key;
        key = FilesystemBasedDigest.passwdCrypt(skey.getBytes(WaarpStringUtils.UTF8));
        AbstractLocalPacket packet = null;
        if (rdbtnShutdown.isSelected()) {
            if (chckbxRestart.isSelected()) {
                packet = new ShutdownPacket(key, (byte) 1);
            } else {
                packet = new ShutdownPacket(key);
            }
        } else {
            packet = new BlockRequestPacket(chckbxBlockUnblock.isSelected(), key);
        }
        String message = null;
        final SocketAddress socketServerAddress;
        try {
            socketServerAddress = host.getSocketAddress();
        } catch (IllegalArgumentException e) {
            message = Messages.getString("AdminR66OperationsGui.187") + Messages.getString("AdminR66OperationsGui.188") + host.getHostid(); //$NON-NLS-1$ //$NON-NLS-2$
            AdminGui.getEnvironnement().GuiResultat = message;
            return;
        }
        LocalChannelReference localChannelReference = null;
        localChannelReference = AdminGui.getEnvironnement().networkTransaction
                .createConnectionWithRetry(socketServerAddress, host.isSsl(), null);
        if (localChannelReference == null) {
            message = Messages.getString("AdminR66OperationsGui.187") + Messages.getString("AdminR66OperationsGui.188") + host.getHostid(); //$NON-NLS-1$ //$NON-NLS-2$
            AdminGui.getEnvironnement().GuiResultat = message;
            return;
        }
        if (rdbtnShutdown.isSelected()) {
            localChannelReference.sessionNewState(R66FiniteDualStates.SHUTDOWN);
        } else {
            localChannelReference.sessionNewState(R66FiniteDualStates.BUSINESSR);
        }
        try {
            ChannelUtils.writeAbstractLocalPacket(localChannelReference, packet, false);
        } catch (OpenR66ProtocolPacketException e) {
            message = Messages.getString("AdminR66OperationsGui.187") + Messages.getString("AdminR66OperationsGui.190") + host.getHostid() //$NON-NLS-1$ //$NON-NLS-2$
                    + "[" + e.getMessage() + "]";
            AdminGui.getEnvironnement().GuiResultat = message;
            return;
        }
        localChannelReference.getFutureRequest().awaitUninterruptibly(180, TimeUnit.SECONDS);
        if (localChannelReference.getFutureRequest().isDone()) {
            R66Result result = localChannelReference.getFutureRequest()
                    .getResult();
            if (localChannelReference.getFutureRequest().isSuccess()) {
                message = Messages.getString("AdminR66OperationsGui.193"); //$NON-NLS-1$
            } else {
                if (result.getOther() instanceof ValidPacket
                        &&
                        ((ValidPacket) result.getOther()).getTypeValid() == LocalPacketFactory.SHUTDOWNPACKET) {
                    message = Messages.getString("AdminR66OperationsGui.194"); //$NON-NLS-1$
                } else if (result.getCode() == ErrorCode.Shutdown) {
                    message = Messages.getString("AdminR66OperationsGui.195"); //$NON-NLS-1$
                } else {
                    message = Messages.getString("AdminR66OperationsGui.196") + result.toString() + "[" + localChannelReference //$NON-NLS-1$
                                    .getFutureRequest().getCause() + "]";
                }
            }
        } else {
            message = Messages.getString("AdminR66OperationsGui.199"); //$NON-NLS-1$
            localChannelReference.getLocalChannel().close();
        }
        long time2 = System.currentTimeMillis();
        long delay = time2 - time1;
        message += Messages.getString("AdminR66OperationsGui.72") + delay; //$NON-NLS-1$
        AdminGui.getEnvironnement().GuiResultat = message;
    }

    /**
     * @return the window
     */
    public static AdminR66OperationsGui getWindow() {
        return window;
    }

    /**
     * @param window the window to set
     */
    public static void setWindow(AdminR66OperationsGui window) {
        AdminR66OperationsGui.window = window;
    }

    public static class JTextAreaOutputStream extends OutputStream {
        JTextArea ta;

        public JTextAreaOutputStream(JTextArea t) {
            super();
            ta = t;
        }

        public void write(int i) {
            ta.append(Character.toString((char) i));
        }

        public void write(char[] buf, int off, int len) {
            String s = new String(buf, off, len);
            ta.append(s);
        }

    }

}
