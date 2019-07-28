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

import com.fg.util.FLoader;
import com.fg.util.FadingFilter;
import com.fg.xmleditor.FXDocumentModel;
import com.fg.xmleditor.FXDocumentModelImpl;
import com.fg.xmleditor.FXDoubleView;
import com.fg.xmleditor.FXModelException;
import com.fg.xmleditor.FXModelStatusListener;
import com.fg.xmleditor.FXStatusEvent;
import com.fg.xmleditor.NewDocumentDialog;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.xample.custnodes.FFileDialog;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FontUIResource;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.ItemSelectable;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/*
 * Copyright (c) 2003 Felix Golubov
 */

/**
 * A simple application demonstrates usage of the Swing-based XML editing
 * component
 * {@link FXDoubleView}. The component generates
 * document-specific GUI for each particular
 * type of the XML document, specified by the XML Schema. A User-Manual
 * document, which is included into
 * XAmple XML Editor distribution package, provides detailed description of how
 * to customize XML Schema
 * documents with the editor-specific features, such as custom field editors,
 * additional labeling,
 * node-specific messages, etc.
 * <p>
 * </p>
 * The application creares an instance of the {@link FFileDialog}
 * and registers it
 * with the {@link FXDoubleView} under "FileDialog" id, so it
 * can be used as a global field
 * editor. (Actually there is no particular sense in doing it since the dialog
 * is not initialized with any
 * specific data. It is just an example).
 *
 * @author Felix Golubov
 * @version 2.0
 */

public class XAmple extends JFrame
    implements ActionListener, ItemListener, FXModelStatusListener {
  /**
   *
   */
  private static final long serialVersionUID = 119665832924793058L;
  public static final String FILE_GUI = "gui.properties";
  public static final String FONT_NAME = "FONT_NAME";
  public static final String FONT_SIZE = "FONT_SIZE";
  public static final String FILE_RUNTIME = "runtime.properties";
  public static final String LOOK_AND_FEEL = "LOOK_AND_FEEL";
  public static final String BOUNDS_LEFT = "BOUNDS_LEFT";
  public static final String BOUNDS_TOP = "BOUNDS_TOP";
  public static final String BOUNDS_WIDTH = "BOUNDS_WIDTH";
  public static final String BOUNDS_HEIGHT = "BOUNDS_HEIGHT";

  final History history = new History("");

  final FHistoryButton btnOpenXSD;
  final FHistoryButton btnOpenXML;
  final JButton btnNewXML;
  final JButton btnSaveXML;
  final JToggleButton btnHorizSplit;
  final JToggleButton btnVertSplit;
  final ButtonGroup splitGroup = new ButtonGroup();
  final JToggleButton btnSync;

  final JMenuBar jMenuBar = new JMenuBar();
  final JMenu menuFile = new JMenu("File");
  final JMenuItem mOpenXSD = new JMenuItem("Open XML Configuration Schema");
  final JMenuItem mOpenXML = new JMenuItem("Open XML Configuration Document");
  final JMenuItem mNewXML = new JMenuItem("New XML Configuration Document");
  final JMenuItem mSaveXML = new JMenuItem("Save XML Configuration Document");
  final JMenuItem mExit = new JMenuItem("Exit");

  final JMenu menuLF = new JMenu("Look & Feel");

  final JMenu menuHelp = new JMenu("Help");
  final JMenuItem mAbout = new JMenuItem("About...");

  final FXDoubleView dblView;
  final FXDocumentModel model;

  final JFileChooser fileDlg;
  final TheFileFilter xsdFilter =
      new TheFileFilter("xsd", "XML Schema files *.xsd");
  final TheFileFilter xmlFilter = new TheFileFilter("xml", "XML files *.xml");

  final FFileDialog fFileDialog = new FFileDialog();

  String xsdFileName = "";
  String xmlFileName = "";

  public XAmple() {
    HistoryIO.load(history);
    setTitle("XAmple-Waarp");
    getContentPane().setLayout(new BorderLayout());
    final ImageIcon icoOpenXSD = FLoader.getIcon(this, "OpenXSD.gif");
    final ImageIcon icoOpenXML = FLoader.getIcon(this, "OpenXML.gif");
    final ImageIcon icoNewXML = FLoader.getIcon(this, "NewXML.gif");
    final ImageIcon icoSaveXML = FLoader.getIcon(this, "SaveXML.gif");
    final ImageIcon imgHorizSplit = FLoader.getIcon(this, "HorizSplit.gif");
    final ImageIcon imgVertSplit = FLoader.getIcon(this, "VertSplit.gif");
    final ImageIcon imgSync = FLoader.getIcon(this, "Sync.gif");

    final JToolBar toolBar = new JToolBar();

    btnOpenXSD = new FHistoryButton(icoOpenXSD, "Open XML Configuration Schema",
                                    "Reopen XML Configuration Schema");
    btnOpenXSD.addActionListener(this);
    btnOpenXSD.addItemListener(this);
    btnOpenXSD.setItems(history.items);
    toolBar.add(btnOpenXSD);
    toolBar.addSeparator(new Dimension(2, 1));
    btnOpenXML =
        new FHistoryButton(icoOpenXML, "Open XML Configuration Document",
                           "Reopen XML Configuration Document");
    btnOpenXML.setEnabled(false);
    btnOpenXML.addActionListener(this);
    btnOpenXML.addItemListener(this);
    toolBar.add(btnOpenXML);
    toolBar.addSeparator();
    btnNewXML = new Btn(icoNewXML, "New XML Configuration Document");
    toolBar.add(btnNewXML);
    btnSaveXML = new Btn(icoSaveXML, "Save XML Configuration Document");
    toolBar.add(btnSaveXML);
    toolBar.addSeparator();
    btnHorizSplit = new ToggleBtn(imgHorizSplit, "Horizontal Split");
    splitGroup.add(btnHorizSplit);
    toolBar.add(btnHorizSplit);
    btnVertSplit = new ToggleBtn(imgVertSplit, "Vertical Split");
    splitGroup.add(btnVertSplit);
    toolBar.add(btnVertSplit);
    toolBar.addSeparator();
    btnSync = new ToggleBtn(imgSync, "Synchronized Node Selection");
    toolBar.add(btnSync);

    mOpenXSD.addActionListener(this);
    menuFile.add(mOpenXSD);

    mOpenXML.setEnabled(false);
    mOpenXML.addActionListener(this);
    menuFile.add(mOpenXML);

    mNewXML.setEnabled(false);
    mNewXML.addActionListener(this);
    menuFile.add(mNewXML);

    menuFile.addSeparator();

    mSaveXML.setEnabled(false);
    mSaveXML.addActionListener(this);
    menuFile.add(mSaveXML);

    menuFile.addSeparator();

    mExit.addActionListener(this);
    menuFile.add(mExit);

    final UIManager.LookAndFeelInfo[] lfi =
        UIManager.getInstalledLookAndFeels();
    final LookAndFeel lf = UIManager.getLookAndFeel();
    final ButtonGroup group = new ButtonGroup();
    for (final LookAndFeelInfo element : lfi) {
      final JRadioButtonMenuItem mi =
          new JRadioButtonMenuItem(element.getName());
      group.add(mi);
      if (element.getClassName().equals(lf.getClass().getName())) {
        mi.setSelected(true);
      }
      mi.addActionListener(this);
      menuLF.add(mi);
    }

    mAbout.addActionListener(this);
    menuHelp.add(mAbout);

    jMenuBar.add(menuFile);
    jMenuBar.add(menuLF);
    jMenuBar.add(menuHelp);
    setJMenuBar(jMenuBar);

    getContentPane().add(toolBar, BorderLayout.NORTH);
    model = new FXDocumentModelImpl();
    model.addModelStatusListener(this);
    dblView = new FXDoubleView(model);
    dblView.addExternalDialog("FileDialog", fFileDialog);
    getContentPane().add(dblView, BorderLayout.CENTER);
    fileDlg = new JFileChooser(new File(".").getAbsolutePath());
    fileDlg.addChoosableFileFilter(xsdFilter);
    fileDlg.addChoosableFileFilter(xmlFilter);
  }

  public static void main(String[] args) {
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
    final XAmple frame = new XAmple();
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
  }

  static void assignDefaultFont() {
    final Properties props = new Properties();
    InputStream in = null;
    String fontName;
    int fontSize = 12;
    try {
      final File file = new File(FILE_GUI);
      if (!file.exists()) {
        return;
      }
      in = new FileInputStream(file);
      props.load(in);
      fontName = props.getProperty(FONT_NAME);
      if (fontName == null || fontName.length() == 0) {
        return;
      }
      try {
        fontSize = Integer.parseInt(props.getProperty(FONT_SIZE, "12"));
      } catch (final Exception ignore) {
        // nothing
      }
    } catch (final Exception ex) {
      SysErrLogger.FAKE_LOGGER.syserr(ex);
      return;
    } finally {
      FileUtils.close(in);
    }
    final Font font = new Font(fontName, Font.PLAIN, fontSize);
    final Enumeration<Object> keys = UIManager.getDefaults().keys();
    while (keys.hasMoreElements()) {
      final Object key = keys.nextElement();
      final Object value = UIManager.get(key);
      if (value instanceof FontUIResource) {
        UIManager.put(key, font);
      }
    }
  }

  @Override
  protected void processWindowEvent(WindowEvent e) {
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      if (confirmation()) {
        saveRuntimeProperties();
        System.exit(0);//NOSONAR
      }
    }
  }

  boolean confirmation() {
    if (dblView.isDocChanged()) {
      final int option = JOptionPane.showConfirmDialog(this,
                                                       "The XML Configuration Document has been changed. Do you like to save it?",
                                                       "information",
                                                       JOptionPane.YES_NO_CANCEL_OPTION,
                                                       JOptionPane.INFORMATION_MESSAGE);
      if (option == JOptionPane.CANCEL_OPTION) {
        return false;
      } else if (option == JOptionPane.YES_OPTION) {
        return saveXML();
      } else {
        return true;
      }
    } else {
      return true;
    }
  }

  void saveRuntimeProperties() {
    final Properties props = new Properties();
    final LookAndFeel lf = UIManager.getLookAndFeel();
    if (lf != null) {
      props.setProperty(LOOK_AND_FEEL, lf.getName());
    }
    final Rectangle r = getBounds();
    props.setProperty(BOUNDS_LEFT, String.valueOf(r.x));
    props.setProperty(BOUNDS_TOP, String.valueOf(r.y));
    props.setProperty(BOUNDS_WIDTH, String.valueOf(r.width));
    props.setProperty(BOUNDS_HEIGHT, String.valueOf(r.height));
    OutputStream out = null;
    try {
      final File file = new File(FILE_RUNTIME);
      file.createNewFile();
      out = new FileOutputStream(file);
      props.store(out, "XAmple-Waarp XML Editor runtime properties");
    } catch (final Exception ex) {
      SysErrLogger.FAKE_LOGGER.syserr(ex);
    } finally {
      FileUtils.close(out);
    }
    HistoryIO.save(history);
  }

  boolean saveXML() {
    final Document doc = model.getDocument();
    if (doc == null) {
      return false;
    }
    fileDlg.setFileFilter(xmlFilter);
    if (fileDlg.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      File file = fileDlg.getSelectedFile();
      if (file == null) {
        return false;
      }
      if (file.getName().indexOf('.') < 0) {
        file = new File(file.getAbsolutePath() + ".xml");
      }
      FileOutputStream out = null;
      Writer writer = null;
      try {
        file.createNewFile();
        out = new FileOutputStream(file);
        final OutputFormat format = new OutputFormat(doc, "UTF-8", true);
        writer = new OutputStreamWriter(out, "UTF-8");
        final XMLSerializer serial = new XMLSerializer(writer, format);
        serial.asDOMSerializer();
        serial.serialize(doc);
        model.setDocumentChanged(false);
        xmlFileName = file.getName();
        showXMLTitle();
        final History childHistory = history.getFirstChild();
        if (childHistory != null) {
          childHistory.put(file.getAbsolutePath());
          btnOpenXML.setItems(childHistory.items);
        }
        return true;
      } catch (final Exception ex) {
        dblView.showErrorMessage("Error: " + ex.getMessage());
        return false;
      } finally {
        FileUtils.close(writer);
        FileUtils.close(out);
      }
    } else {
      return false;
    }
  }

  void showXMLTitle() {
    setTitle("XAmple-GG  -  " + xsdFileName + "  :  " + xmlFileName);
  }

  @Override
  public void itemStateChanged(ItemEvent e) {
    if (!confirmation()) {
      return;
    }
    final ItemSelectable source = e.getItemSelectable();
    final String path = ((History) e.getItem()).path;
    final File file = new File(path);
    if (source == btnOpenXSD) {
      loadXMLSchema(file);
    } else if (source == btnOpenXML) {
      loadXMLDocument(file);
    }
    fileDlg.setCurrentDirectory(file.getParentFile());
  }

  boolean loadXMLSchema(File file) {
    try {
      model.newDocument(file.toURI().toURL());
      setSchemaEnabled(true);
      xsdFileName = file.getName();
      showXSDTitle();
      final History childHistory = history.put(file.getAbsolutePath());
      btnOpenXSD.setItems(history.items);
      btnOpenXML.setItems(childHistory.items);
      dblView.showInfoMessage(
          "XML Configuration Schema: " + file.getAbsolutePath());
      return true;
    } catch (final Exception ex) {
      history.remove(file.getAbsolutePath());
      btnOpenXSD.setItems(history.items);
      btnOpenXML.setItems(null);
      dblView.showErrorMessage("Error: " + ex);
      setSchemaEnabled(false);
      JOptionPane
          .showMessageDialog(this, "Can't load the XML Configuration Schema",
                             "Error", JOptionPane.ERROR_MESSAGE);
      setTitle("XAmple");
      return false;
    }
  }

  boolean loadXMLDocument(File file) {
    final History childHistory = history.getFirstChild();
    try {
      final URL url = file.toURI().toURL();
      final List<?> lostElements =
          model.openDocument(model.getSchemaURL(), url);
      if (lostElements != null) {
        final StringBuilder sb = new StringBuilder(
            "Error: The source XML Configuration document is invalid.\n" +
            "The following elements have not been loaded:");
        for (int i = 0; i < lostElements.size(); i++) {
          sb.append('\n');
          final int k = sb.length();
          final Node element = (Node) lostElements.get(i);
          sb.append(element.getNodeName());
          Node node = element.getParentNode();
          while (node != null && !(node instanceof Document)) {
            sb.insert(k, node.getNodeName() + '/');
            node = node.getParentNode();
          }
        }
        dblView.showErrorMessage(sb.toString());
      }
      setXMLEnabled(true);
      xmlFileName = file.getName();
      showXMLTitle();
      childHistory.put(file.getAbsolutePath());
      btnOpenXML.setItems(childHistory.items);
      dblView.showInfoMessage(
          "XML Configuration Document: " + file.getAbsolutePath());
      return true;
    } catch (final Exception ex) {
      childHistory.remove(file.getAbsolutePath());
      btnOpenXML.setItems(childHistory.items);
      // dblView.clear()
      setXMLEnabled(false);
      dblView.showErrorMessage("Error: " + ex.getMessage());
      JOptionPane
          .showMessageDialog(this, "Can't load the XML Configuration Document",
                             "Error", JOptionPane.ERROR_MESSAGE);
      showXSDTitle();
      return false;
    }
  }

  void setSchemaEnabled(boolean enabled) {
    btnOpenXML.setEnabled(enabled);
    mOpenXML.setEnabled(enabled);
    btnNewXML.setEnabled(enabled);
    mNewXML.setEnabled(enabled);
    setXMLEnabled(enabled);
  }

  void showXSDTitle() {
    setTitle("XAmple-GG  -  " + xsdFileName + "  :  <noname>.xml");
  }

  void setXMLEnabled(boolean enabled) {
    btnSaveXML.setEnabled(enabled);
    mSaveXML.setEnabled(enabled);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    final Object source = e.getSource();
    if (source == btnOpenXSD || source == mOpenXSD) {
      openXSD();
    } else if (source == btnOpenXML || source == mOpenXML) {
      openXML();
    } else if (source == btnNewXML || source == mNewXML) {
      newXML();
    } else if (source == btnSaveXML || source == mSaveXML) {
      saveXML();
    } else if (source == btnHorizSplit) {
      dblView.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    } else if (source == btnVertSplit) {
      dblView.setOrientation(JSplitPane.VERTICAL_SPLIT);
    } else if (source == btnSync) {
      dblView.setSyncSelectNodes(btnSync.getModel().isSelected());
    } else if (source instanceof JRadioButtonMenuItem) {
      updateLookAndFeel(((JRadioButtonMenuItem) source).getText());
    } else if (source == mAbout) {
      showDlgAbout();
    } else if (source == mExit) {
      exit();
    }
  }

  void updateLookAndFeel(String lfName) {
    final LookAndFeel lf = UIManager.getLookAndFeel();
    if (lf != null && lf.getName().equals(lfName)) {
      return;
    }
    final UIManager.LookAndFeelInfo[] lfi =
        UIManager.getInstalledLookAndFeels();
    for (final LookAndFeelInfo element : lfi) {
      if (element.getName().equals(lfName)) {
        try {
          // dblView.stopEditing()
          UIManager.setLookAndFeel(element.getClassName());
          SwingUtilities.updateComponentTreeUI(this);
          final FileFilter currFilter = fileDlg.getFileFilter();
          fileDlg.setAcceptAllFileFilterUsed(
              fileDlg.getAcceptAllFileFilter() == null);
          SwingUtilities.updateComponentTreeUI(fileDlg);
          fileDlg.setAcceptAllFileFilterUsed(true);
          fileDlg.setFileFilter(currFilter);
          SwingUtilities.updateComponentTreeUI(fFileDialog);
          return;
        } catch (final Exception ignore) {
          // nothing
        }
      }
    }
  }

  void showDlgAbout() {
    final DlgAbout dlg = new DlgAbout(this);
    final Dimension dlgSize = dlg.getPreferredSize();
    final Dimension frmSize = getSize();
    final Point loc = getLocation();
    dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x,
                    (frmSize.height - dlgSize.height) / 2 + loc.y);
    dlg.setModal(true);
    dlg.setVisible(true);
  }

  void openXSD() {
    if (!confirmation()) {
      return;
    }
    fileDlg.setFileFilter(xsdFilter);
    if (fileDlg.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      final File file = fileDlg.getSelectedFile();
      if (file == null) {
        return;
      }
      loadXMLSchema(file);
    }
  }

  void openXML() {
    if (!confirmation()) {
      return;
    }
    fileDlg.setFileFilter(xmlFilter);
    if (fileDlg.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      final File file = fileDlg.getSelectedFile();
      if (file == null) {
        return;
      }
      loadXMLDocument(file);
    }
  }

  void newXML() {
    if (!confirmation()) {
      return;
    }
    try {
      if (model.getSchemaURL() == null) {
        throw new FXModelException("No XML Configuration Schema loaded");
      }
      final NewDocumentDialog dlg = new NewDocumentDialog(this, model);
      dlg.pack();
      dlg.setLocationRelativeTo(dblView);
      dlg.setVisible(true);
      if (!dlg.isStatusOK()) {
        return;
      }
      model.newDocument(model.getSchemaURL(), dlg.getRootNamespace(),
                        dlg.getRootElementName());
      setXMLEnabled(true);
    } catch (final Exception ex) {
      SysErrLogger.FAKE_LOGGER.syserr(ex);
      setXMLEnabled(false);
      dblView.showErrorMessage("Error: " + ex.getMessage());
      JOptionPane
          .showMessageDialog(this, "Can't open new XML Configuration Document",
                             "Error", JOptionPane.ERROR_MESSAGE);
    }
    showXSDTitle();
  }

  void exit() {
    if (!confirmation()) {
      return;
    }
    saveRuntimeProperties();
    System.exit(0);//NOSONAR
  }

  @Override
  public void newDocumentLoaded(FXStatusEvent e) {
  }

  @Override
  public void docValidityStatusChanged(FXStatusEvent e) {
  }

  class TheFileFilter extends FileFilter {
    final String extension;
    final String description;

    TheFileFilter(String extension, String description) {
      this.extension = extension;
      this.description = description;
    }

    @Override
    public boolean accept(File f) {
      if (f.isDirectory()) {
        return true;
      }
      String ext;
      final String name = f.getName();
      final int i = name.lastIndexOf('.');
      if (i > 0 && i < name.length() - 1) {
        ext = name.substring(i + 1).toLowerCase();
      } else {
        ext = "";
      }
      return ext.equals(extension);
    }

    @Override
    public String getDescription() {
      return description;
    }
  }

  class Btn extends JButton {
    /**
     *
     */
    private static final long serialVersionUID = 2143658918908443792L;

    Btn(ImageIcon icon, String toolTipText) {
      super(icon);
      setFocusPainted(false);
      setDisabledIcon(FadingFilter.fade(icon));
      setToolTipText(toolTipText);
      setEnabled(false);
      setPreferredSize(new Dimension(35, 27));
      setMinimumSize(new Dimension(35, 27));
      setMaximumSize(new Dimension(35, 27));
      addActionListener(XAmple.this);
    }
  }

  class ToggleBtn extends JToggleButton {
    /**
     *
     */
    private static final long serialVersionUID = 4742196038942810167L;

    ToggleBtn(ImageIcon icon, String toolTipText) {
      super(icon);
      setFocusPainted(false);
      setDisabledIcon(FadingFilter.fade(icon));
      setToolTipText(toolTipText);
      setPreferredSize(new Dimension(35, 27));
      setMinimumSize(new Dimension(35, 27));
      setMaximumSize(new Dimension(35, 27));
      addActionListener(XAmple.this);
    }
  }

}
