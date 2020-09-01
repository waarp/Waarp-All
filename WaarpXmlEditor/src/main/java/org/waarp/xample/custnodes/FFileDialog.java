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
package org.waarp.xample.custnodes;

import com.fg.ftreenodes.ICellControl;
import com.fg.ftreenodes.Params;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

/*
 * Copyright (c) 2003 Felix Golubov
 */

/**
 * FFileDialog is a file dialog, which can be used as a custom field editor for
 * the
 * {@link com.fg.xmleditor.FXBasicView}. Since FFileDialog is a JDialog, it can
 * be used as both custom field
 * editor and a global editor.
 *
 * @author Felix Golubov
 * @version 2.0
 *     <p>
 *     Add support for Directory and different options on File Dialog
 */

/*
 * Implementation notices: Well, it would much better to make buttons of the JFileChooser invisible and create
 * our own "OK" and "Cancel" buttons. The problem is that CDE/Motif UI doesn't allow to make buttons
 * invisible. Hence all the complexities with addBtnListener(Component c) recursive method. Besides, when UI
 * changes, all the child components of the JFileChooser are discarded, so addBtnListener has to be called
 * from the updateUI() method.
 */

public class FFileDialog extends JDialog
    implements ICellControl, ActionListener {
  /**
   *
   */
  private static final long serialVersionUID = -8775488630211100329L;
  JPanel panel = new JPanel();
  final FFileChooser fileChooser = new FFileChooser();
  int mode = JFileChooser.FILES_ONLY;
  transient Object data;
  transient List masks;

  public FFileDialog() {
    getContentPane().setLayout(new BorderLayout());
    fileChooser.setCurrentDirectory(new File("."));
    fileChooser.addActionListener(this);
    fileChooser.setApproveButtonText("OK");
    getContentPane().add(fileChooser, BorderLayout.CENTER);
  }

  @Override
  protected void processWindowEvent(final WindowEvent e) {
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      cancel();
    }
    super.processWindowEvent(e);
  }

  void cancel() {
    dispose();
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    if (!(e.getSource() instanceof JButton)) {
      return;
    }
    final JButton btn = (JButton) e.getSource();
    if ("OK".equals(btn.getText())) {
      final File file = fileChooser.getSelectedFile();
      data = file == null? "" : file.getAbsolutePath();
      cancel();
    } else if ("Cancel".equals(btn.getText())) {
      cancel();
    } else if ("Annuler".equals(btn.getText())) {
      cancel();
    }
  }

  @Override
  public void initCellControl(final boolean isEditor) {
    fileChooser.addBtnListener(fileChooser);
  }

  @Override
  public void updateCellControl(final boolean isEditor, final boolean enabled,
                                final boolean editable, final Object data,
                                final Params params) {
    pack();
    this.data = data;
    if (data != null) {
      final String path = data.toString().trim();
      if (path.length() > 0) {
        try {
          final File file = new File(path);
          fileChooser.setSelectedFile(file);
        } catch (final Exception ex) {
          fileChooser.setSelectedFile(null);
        }
      } else {
        fileChooser.setSelectedFile(null);
      }
    } else {
      fileChooser.setSelectedFile(null);
    }
    final List list = params.getList();
    boolean sameMasks = true;
    if (masks == null || list.size() != masks.size()) {
      sameMasks = false;
    } else {
      for (int i = 0; i < list.size() && sameMasks; i++) {
        if (!list.get(i).equals(masks.get(i))) {
          sameMasks = false;
          break;
        }
      }
    }
    if (sameMasks) {
      return;
    }
    masks = list;
    fileChooser.resetChoosableFileFilters();
    for (int i = 0; i < masks.size(); i++) {
      fileChooser
          .addChoosableFileFilter(new FileExtFilter((String) masks.get(i)));
    }
  }

  @Override
  public Object getData() {
    return data;
  }

  class FileExtFilter extends FileFilter {
    final HashSet<String> extensions = new HashSet<String>();
    String description = "";

    FileExtFilter(final String mask) {
      final StringTokenizer st = new StringTokenizer(mask, "|");
      while (st.hasMoreElements()) {
        String token = st.nextToken().trim();
        if (st.hasMoreElements()) {
          if (token.length() == 0) {
            // no extension
          } else {
            if (token.startsWith("null")) {
              if (token.length() > 4) {
                token = token.substring(5).trim();
              } else {
                token = "";
              }
            }
            if (token.length() > 0) {
              extensions.add(token.toLowerCase());
            }
          }
        } else {
          final String type = token.substring(0, 1);
          final int val = Integer.parseInt(type);
          if (val == 0) {
            mode = JFileChooser.FILES_ONLY;
          } else if (val == 1) {
            mode = JFileChooser.DIRECTORIES_ONLY;
          } else {
            mode = JFileChooser.FILES_AND_DIRECTORIES;
          }
          description = token.substring(1);
        }
      }
      fileChooser.setFileSelectionMode(mode);
    }

    @Override
    public boolean accept(final File f) {
      if (f.isDirectory()) {
        return true;
      }
      if (f.isFile() && mode == JFileChooser.DIRECTORIES_ONLY) {
        return false;
      }
      if (extensions.isEmpty()) {
        return true;
      }
      final String ext;
      final String name = f.getName();
      final int i = name.lastIndexOf('.');
      if (i > 0 && i < name.length() - 1) {
        ext = name.substring(i + 1).toLowerCase();
        return extensions.contains(ext);
      } else {
        return false;
      }
    }

    @Override
    public String getDescription() {
      return description;
    }
  }

  class FFileChooser extends JFileChooser {
    /**
     *
     */
    private static final long serialVersionUID = -4161876854413783100L;

    public void addBtnListener(final Component c) {
      if (c instanceof JButton) {
        ((JButton) c).addActionListener(FFileDialog.this);
      }
      if (c instanceof Container) {
        final Component[] children = ((Container) c).getComponents();
        for (final Component element : children) {
          addBtnListener(element);
        }
      }
    }

    @Override
    public void updateUI() {
      setAcceptAllFileFilterUsed(getAcceptAllFileFilter() == null);
      super.updateUI();
      setAcceptAllFileFilterUsed(true);
      addBtnListener(this);
    }
  }

}
