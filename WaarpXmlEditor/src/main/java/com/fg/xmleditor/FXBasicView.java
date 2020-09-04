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

/*
 * Copyright (c) 2002 Felix Golubov
 */
// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3)
// Source File Name: FXBasicView.java

package com.fg.xmleditor;

import com.fg.ftree.FBasicNode;
import com.fg.ftree.FTree;
import com.fg.ftree.FTreeActionEvent;
import com.fg.ftree.FTreeActionListener;
import com.fg.ftree.FTreeEditorEvent;
import com.fg.ftree.FTreeEditorListener;
import com.fg.ftree.FTreeExpansBarListener;
import com.fg.ftree.FTreeExpansionListener;
import com.fg.ftree.FTreeNodeEvent;
import com.fg.ftree.FTreeSelectionListener;
import com.fg.ftree.GgFTree;
import com.fg.ftreenodes.FAbstractToggleNode;
import com.fg.ftreenodes.FToggleControl;
import com.fg.ftreenodes.FToggleNode;
import com.fg.ftreenodes.FToggleSwitchNode;
import com.fg.ftreenodes.ICellControl;
import com.fg.util.FLoader;
import org.apache.xerces.impl.xs.psvi.XSParticle;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

/*
 * @author Felix Golubov
 * @version 1.0 Fix to get a different FTree implementation
 */
// Referenced classes of package com.fg.xmleditor:
// FXModelStatusListener, FXModel, FXDocumentModelImpl, FXStatusEvent,
// FXViewStatusListener, XSRef, NSQualifiersDialog, SearchDialog

public class FXBasicView extends JComponent implements FXModelStatusListener {
  /**
   *
   */
  private static final long serialVersionUID = 4353436426114132650L;
  transient FXModel model;
  final FTree tree;
  final JTextField mouseInfo;
  final JTextArea errorInfoArea;
  final JTextArea nodesInfoArea;
  transient final Vector<FXViewStatusListener> viewListeners;
  transient final InnerListener innerListener;
  boolean insert;
  boolean remove;
  boolean moveUp;
  boolean moveDown;
  final JPopupMenu popup;
  final JMenuItem mSelect;
  final JMenuItem mUnselect;
  final JMenuItem mInsBefore;
  final JMenuItem mInsAfter;
  final JMenuItem mRemove;
  final JMenuItem mMoveUp;
  final JMenuItem mMoveDown;
  final JMenuItem mFindInvalid;
  final JMenuItem mFindNode;
  final JMenuItem mNS;
  SearchDialog searchDialog;

  public FXBasicView(final FXModel model) {
    this.model = null;
    viewListeners = new Vector<FXViewStatusListener>();
    innerListener = new InnerListener();
    insert = false;
    remove = false;
    moveUp = false;
    moveDown = false;
    popup = new JPopupMenu();
    mSelect = new JMenuItem("Select Node");
    mUnselect = new JMenuItem("Unselect Node");
    mInsBefore = new JMenuItem("Insert Node Before");
    mInsAfter = new JMenuItem("Insert Node After");
    mRemove = new JMenuItem("Remove Node");
    mMoveUp = new JMenuItem("Move Node Up");
    mMoveDown = new JMenuItem("Move Node Down");
    mFindInvalid = new JMenuItem("Find Invalid Node");
    mFindNode = new JMenuItem("Find Node");
    mNS = new JMenuItem("Edit NS Qualifiers");
    searchDialog = null;
    setLayout(new BorderLayout());
    final JSplitPane mainSplitPane = new JSplitPane(0);
    mainSplitPane.setResizeWeight(0.80000000000000004D);
    mainSplitPane.setDividerSize(8);
    mainSplitPane.setDoubleBuffered(false);
    add(mainSplitPane, "Center");
    final JScrollPane sp = new JScrollPane();
    tree = new GgFTree();
    setFXModel(model);
    final Insets insets = tree.getInsets();
    insets.top = 20;
    tree.setInsets(insets);
    sp.getViewport().add(tree, null);
    mainSplitPane.add(sp, "left");
    final JPanel infoPanel = new JPanel(new BorderLayout());
    final JSplitPane infoSplitPane = new JSplitPane(1);
    infoSplitPane.setResizeWeight(0.5D);
    infoSplitPane.setDividerSize(8);
    final JScrollPane nodesInfoAreaSP = new JScrollPane();
    nodesInfoAreaSP.setHorizontalScrollBarPolicy(31);
    nodesInfoArea = new JTextArea();
    nodesInfoArea.setLineWrap(true);
    nodesInfoArea.setWrapStyleWord(true);
    nodesInfoArea.setEditable(false);
    nodesInfoAreaSP.getViewport().add(nodesInfoArea, null);
    infoSplitPane.add(nodesInfoAreaSP, "left");
    final JScrollPane errorInfoAreaSP = new JScrollPane();
    errorInfoAreaSP.setHorizontalScrollBarPolicy(31);
    errorInfoArea = new JTextArea();
    errorInfoArea.setLineWrap(true);
    errorInfoArea.setWrapStyleWord(true);
    errorInfoArea.setEditable(false);
    errorInfoAreaSP.getViewport().add(errorInfoArea, null);
    infoSplitPane.add(errorInfoAreaSP, "right");
    infoPanel.add(infoSplitPane, "Center");
    mouseInfo = new JTextField("  Folder:");
    mouseInfo.setEditable(false);
    mouseInfo.setBorder(BorderFactory.createLineBorder(Color.gray));
    infoPanel.add(mouseInfo, "South");
    mainSplitPane.add(infoPanel, "right");
    setBackground(new Color(230, 230, 230));
    tree.setCellsLeftInset(5);
    tree.addFTreeExpansionListener(innerListener);
    tree.addFTreeExpansBarListener(innerListener);
    tree.addFTreeEditorListener(innerListener);
    tree.addFTreeActionListener(innerListener);
    tree.addFTreeSelectionListener(innerListener);
    tree.addMouseListener(innerListener);
    createPopupMenu();
  }

  public FXBasicView() {
    this(null);
  }

  public static void stopCellEditing(final JComponent editor) {
    FTree.stopCellEditing(editor);
  }

  public static void cancelCellEditing(final JComponent editor) {
    FTree.cancelCellEditing(editor);
  }

  public static void cellEditorValueChanged(final JComponent editor,
                                            final Object event) {
    FTree.cellEditorValueChanged(editor, event);
  }

  static boolean hasValue(final FToggleNode node) {
    final XSRef ref = (XSRef) node.getAssociate();
    return ref.hasValue();
  }

  @Override
  public void newDocumentLoaded(final FXStatusEvent e) {
    if (e.getStatus()) {
      tree.setNodeExpanded(model.getRoot(), true);
    }
    clearEditorFlags();
    nodesInfoArea.setText("");
  }

  void clearEditorFlags() {
    setInsert(false);
    setRemove(false);
    setMoveUp(false);
    setMoveDown(false);
  }

  void setInsert(final boolean insert) {
    if (insert == this.insert) {
      return;
    }
    this.insert = insert;
    final FXStatusEvent e = new FXStatusEvent(this, this.insert);
    for (int i = 0; i < viewListeners.size(); i++) {
      final FXViewStatusListener fxl = viewListeners.get(i);
      fxl.canInsertStatusChanged(e);
    }

  }

  void setRemove(final boolean remove) {
    if (remove == this.remove) {
      return;
    }
    this.remove = remove;
    final FXStatusEvent e = new FXStatusEvent(this, this.remove);
    for (int i = 0; i < viewListeners.size(); i++) {
      final FXViewStatusListener fxl = viewListeners.get(i);
      fxl.canRemoveStatusChanged(e);
    }

  }

  void setMoveUp(final boolean moveUp) {
    if (moveUp == this.moveUp) {
      return;
    }
    this.moveUp = moveUp;
    final FXStatusEvent e = new FXStatusEvent(this, this.moveUp);
    for (int i = 0; i < viewListeners.size(); i++) {
      final FXViewStatusListener fxl = viewListeners.get(i);
      fxl.canMoveUpStatusChanged(e);
    }

  }

  void setMoveDown(final boolean moveDown) {
    if (moveDown == this.moveDown) {
      return;
    }
    this.moveDown = moveDown;
    final FXStatusEvent e = new FXStatusEvent(this, this.moveDown);
    for (int i = 0; i < viewListeners.size(); i++) {
      final FXViewStatusListener fxl = viewListeners.get(i);
      fxl.canMoveDownStatusChanged(e);
    }

  }

  @Override
  public void docValidityStatusChanged(final FXStatusEvent fxstatusevent) {
    // nothing
  }

  public FXModel getFXModel() {
    return model;
  }

  public void setFXModel(FXModel newModel) {
    if (model != null) {
      model.removeModelStatusListener(this);
    }
    if (newModel == null) {
      newModel = new FXDocumentModelImpl();
    }
    model = newModel;
    model.addModelStatusListener(this);
    tree.setTreeModel(model);
  }

  public FTree getTree() {
    return tree;
  }

  @Override
  public void updateUI() {
    super.updateUI();
    SwingUtilities.updateComponentTreeUI(popup);
    if (searchDialog != null) {
      SwingUtilities.updateComponentTreeUI(searchDialog);
    }
  }

  void createPopupMenu() {
    mSelect.setIcon(FLoader.getIcon(this, "SelectNode.gif"));
    mSelect.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(final ActionEvent e) {
        if (innerListener.treeNode != null) {
          tree.setSelectedPath(innerListener.treeNode.getPath());
        }
      }

    });
    popup.add(mSelect);
    mUnselect.setIcon(FLoader.getIcon(this, "UnselectNode.gif"));
    mUnselect.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(final ActionEvent e) {
        tree.setSelectedPath(null);
      }

    });
    popup.add(mUnselect);
    popup.addSeparator();
    mInsBefore.setIcon(FLoader.getIcon(this, "InsNodeBefore.gif"));
    mInsBefore.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(final ActionEvent e) {
        insertNodeBefore();
      }

    });
    popup.add(mInsBefore);
    mInsAfter.setIcon(FLoader.getIcon(this, "InsNodeAfter.gif"));
    mInsAfter.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(final ActionEvent e) {
        insertNodeAfter();
      }

    });
    popup.add(mInsAfter);
    popup.addSeparator();
    mRemove.setIcon(FLoader.getIcon(this, "RemoveNode.gif"));
    mRemove.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(final ActionEvent e) {
        removeNode();
      }

    });
    popup.add(mRemove);
    popup.addSeparator();
    mMoveUp.setIcon(FLoader.getIcon(this, "MoveNodeUp.gif"));
    mMoveUp.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(final ActionEvent e) {
        moveNodeUp();
      }

    });
    popup.add(mMoveUp);
    mMoveDown.setIcon(FLoader.getIcon(this, "MoveNodeDown.gif"));
    mMoveDown.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(final ActionEvent e) {
        moveNodeDown();
      }

    });
    popup.add(mMoveDown);
    popup.addSeparator();
    mNS.setIcon(FLoader.getIcon(this, "NSQualifiers.gif"));
    mNS.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(final ActionEvent e) {
        showNSQualifiersDialog();
      }

    });
    popup.add(mNS);
    popup.addSeparator();
    mFindInvalid.setIcon(FLoader.getIcon(this, "FindInvalidNode.gif"));
    mFindInvalid.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(final ActionEvent e) {
        showInvalidNode();
      }

    });
    popup.add(mFindInvalid);
    mFindNode.setIcon(FLoader.getIcon(this, "FindNode.gif"));
    mFindNode.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(final ActionEvent e) {
        showSearchDialog();
      }

    });
    popup.add(mFindNode);
  }

  public void addExternalDialog(final String id, final JDialog dialog) {
    tree.addDialog(id, dialog);
  }

  public void removeExternalDialog(final String id) {
    tree.removeDialog(id);
  }

  public void removeAllExternalDialogs() {
    tree.removeAllDialogs();
  }

  @Override
  public void setBackground(final Color color) {
    super.setBackground(color);
    if (tree != null) {
      tree.setBackground(color);
      if (tree.isShowing()) {
        tree.repaint();
      }
    }
  }

  public boolean isReducedView() {
    return tree.isReducedView();
  }

  public void setReducedView(final boolean reduced) {
    tree.setReducedView(reduced);
    final FToggleNode node = (FToggleNode) tree.getSelectedNode();
    if (node == null) {
      return;
    }
    final FToggleNode parent = (FToggleNode) node.getParent();
    if (parent != null) {
      setEditorFlags(parent, node);
    }
  }

  void setEditorFlags(final FToggleNode parent, final FToggleNode node) {
    final int count = parent.getRealChildCount();
    final XSRef ref = (XSRef) parent.getAssociate();
    final boolean b = !tree.isReducedView();
    if (ref.isArray()) {
      final XSParticle particle = ref.getParticle();
      final int minOccurs = Math.max(particle.getMinOccurs(), 1);
      final int maxOccurs = particle.getMaxOccursUnbounded()? 0x7fffffff :
          particle.getMaxOccurs();
      setInsert(b && count < maxOccurs);
      setRemove(b && count > minOccurs);
      setMoveUp(b && node != parent.getRealChildAt(0));
      setMoveDown(b && node != parent.getRealChildAt(count - 1));
    } else if (ref.in(16)) {
      setMoveUp(b && node != parent.getRealChildAt(0));
      setMoveDown(b && node != parent.getRealChildAt(count - 1));
    }
  }

  public boolean stopEditing() {
    return tree.stopEditing();
  }

  public boolean cancelEditing() {
    return tree.cancelEditing();
  }

  public void showInfoMessage(final String message) {
    nodesInfoArea.setText(message);
  }

  public void showErrorMessage(final String message) {
    errorInfoArea.setText(message);
  }

  public void addViewStatusListener(final FXViewStatusListener l) {
    if (!viewListeners.contains(l)) {
      viewListeners.addElement(l);
    }
  }

  public void removeViewStatusListener(final FXViewStatusListener l) {
    viewListeners.removeElement(l);
  }

  public boolean hasDocument() {
    return model.getRoot() != null;
  }

  public boolean isDocChanged() {
    return model.isDocumentChanged();
  }

  public boolean isDocValid() {
    return model.isDocumentValid();
  }

  public boolean canInsert() {
    return insert;
  }

  public boolean canRemove() {
    return remove;
  }

  public boolean canMoveUp() {
    return moveUp;
  }

  public boolean canMoveDown() {
    return moveDown;
  }

  public void insertNodeBefore() {
    if (!canInsert()) {
      return;
    }
    final FToggleNode node = (FToggleNode) tree.getSelectedNode();
    if (node == null) {
      return;
    }
    final FToggleNode parent = (FToggleNode) node.getParent();
    if (parent == null) {
      return;
    }
    final int index = parent.getIndex(node);
    if (model.insertInstance(parent, index) != null) {
      tree.setSelectedPath(node.getPath());
      setEditorFlags(parent, node);
    }
  }

  public void insertNodeAfter() {
    if (!canInsert()) {
      return;
    }
    final FToggleNode node = (FToggleNode) tree.getSelectedNode();
    if (node == null) {
      return;
    }
    final FToggleNode parent = (FToggleNode) node.getParent();
    if (parent == null) {
      return;
    }
    final int index = parent.getIndex(node);
    if (model.insertInstance(parent, index + 1) != null) {
      tree.setSelectedPath(node.getPath());
      setEditorFlags(parent, node);
    }
  }

  public void removeNode() {
    if (!canRemove()) {
      return;
    }
    FToggleNode node = (FToggleNode) tree.getSelectedNode();
    if (node == null) {
      return;
    }
    final FToggleNode parent = (FToggleNode) node.getParent();
    if (parent == null) {
      return;
    }
    final int index = model.removeInstance(node);
    if (index < 0) {
      return;
    }
    if (index >= parent.getRealChildCount()) {
      node = (FToggleNode) parent.getRealChildAt(index - 1);
    } else {
      node = (FToggleNode) parent.getRealChildAt(index);
    }
    tree.setSelectedPath(node.getPath());
    setEditorFlags(parent, node);
  }

  public void moveNodeUp() {
    if (!canMoveUp()) {
      return;
    }
    final FToggleNode node = (FToggleNode) tree.getSelectedNode();
    if (node == null) {
      return;
    }
    final FToggleNode parent = (FToggleNode) node.getParent();
    if (parent != null) {
      final int index = parent.getIndex(node);
      parent.remove(index);
      parent.insert(node, index - 1);
      model.fireTreeModelDataChanged(true);
      tree.setSelectedPath(node.getPath());
      setEditorFlags(parent, node);
    }
  }

  public void moveNodeDown() {
    if (!canMoveDown()) {
      return;
    }
    final FToggleNode node = (FToggleNode) tree.getSelectedNode();
    if (node == null) {
      return;
    }
    final FToggleNode parent = (FToggleNode) node.getParent();
    if (parent != null) {
      final int index = parent.getIndex(node);
      parent.remove(index);
      parent.insert(node, index + 1);
      model.fireTreeModelDataChanged(true);
      tree.setSelectedPath(node.getPath());
      setEditorFlags(parent, node);
    }
  }

  void setDialogLocation(final JDialog dlg) {
    final Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();
    final Dimension size = getSize();
    final Dimension dlgSize = dlg.getSize();
    int x = getLocationOnScreen().x + (size.width - dlgSize.width) / 2;
    if (x < 0) {
      x = 0;
    }
    if (x + dlgSize.width > scrSize.width) {
      x = scrSize.width - dlgSize.width;
    }
    int y = getLocationOnScreen().y + (size.height - dlgSize.height) / 2;
    if (y < 0) {
      y = 0;
    }
    if (y + dlgSize.height > scrSize.height) {
      y = scrSize.height - dlgSize.height;
    }
    dlg.setLocation(x, y);
  }

  public void showNSQualifiersDialog() {
    final Frame frame =
        (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    final NSQualifiersDialog dlg = new NSQualifiersDialog(frame, model);
    setDialogLocation(dlg);
    dlg.setVisible(true);
  }

  int[] createPath(final FBasicNode treeNode) {
    final Vector<FBasicNode> v = new Vector<FBasicNode>();
    for (FBasicNode node = treeNode; node != null;
         node = (FBasicNode) node.getParent()) {
      v.add(0, node);
    }

    final int[] path = new int[v.size()];
    FBasicNode parent = v.get(0);
    for (int i = 1; i < v.size(); i++) {
      final FBasicNode child = v.get(i);
      path[i - 1] = parent.getIndex(child);
      parent = child;
    }

    path[path.length - 1] = 0;
    return path;
  }

  FToggleNode getInvalidNode(final FToggleNode parent, final int[] startPath,
                             final int level) {
    final int startIndex = startPath[level];
    FToggleNode node;
    for (int i = startIndex; i < parent.getRealChildCount(); i++) {
      final FToggleNode child = (FToggleNode) parent.getRealChildAt(i);
      if (child.isToggleSelected() &&
          (!child.getChildrenValidity() || !child.getNodeValidity())) {
        if (i == startIndex && level + 1 < startPath.length) {
          node = getInvalidNode(child, startPath, level + 1);
        } else {
          node = getInvalidNode(child);
        }
        if (node != null) {
          return node;
        }
      }
    }

    return null;
  }

  FToggleNode getInvalidNode(FToggleNode parent) {
    getInvalidNode:
    while (true) {
      if (!parent.getNodeValidity()) {
        return parent;
      }
      for (int i = 0; i < parent.getRealChildCount(); i++) {
        final FToggleNode child = (FToggleNode) parent.getRealChildAt(i);
        if (child.isToggleSelected() &&
            (!child.getChildrenValidity() || !child.getNodeValidity())) {
          parent = child;
          continue getInvalidNode;
        }
      }

      return parent;
    }
  }

  public void showInvalidNode() {
    if (!hasDocument() || isDocValid()) {
      return;
    }
    FToggleNode node = null;
    final FToggleNode rootNode = (FToggleNode) tree.getRoot();
    FToggleNode startNode = (FToggleNode) tree.getSelectedNode();
    if (startNode != null) {
      startNode = (FToggleNode) startNode.getSubstituteNode();
    }
    if (startNode != null) {
      final int[] startPath = createPath(startNode);
      node = getInvalidNode(rootNode, startPath, 0);
    }
    if (node == null) {
      node = getInvalidNode(rootNode);
    }
    if (node != null) {
      if (node.getParent() instanceof FToggleSwitchNode) {
        node = (FToggleNode) node.getParent();
      }
      final Object[] path = node.getPath();
      tree.makeVisible(path);
      tree.setSelectedPath(path);
    }
  }

  public void showSearchDialog() {
    if (searchDialog == null) {
      final Frame frame =
          (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
      searchDialog = new SearchDialog(frame, this);
      searchDialog.pack();
      setDialogLocation(searchDialog);
    }
    searchDialog.setVisible(true);
  }

  class InnerListener implements FTreeExpansionListener, FTreeExpansBarListener,
                                 FTreeEditorListener, FTreeActionListener,
                                 FTreeSelectionListener, MouseListener {

    FToggleNode treeNode;

    InnerListener() {
      treeNode = null;
    }

    @Override
    public void nodeWillExpand(final FTreeNodeEvent e) {
      FToggleNode node = (FToggleNode) e.getTreeNode();
      node = (FToggleNode) node.getSubstituteNode();
      if (node != null && model.populateNode(node)) {
        final Object[] selPath = tree.getSelectedPath();
        model.fireTreeModelDataChanged(true);
        tree.setSelectedPath(selPath);
      }
    }

    @Override
    public void nodeWillCollapse(final FTreeNodeEvent ftreenodeevent) {
      // nothing
    }

    @Override
    public void nodeExpanded(final FTreeNodeEvent e) {
      if (isSelectedNodeShowingChanged(e)) {
        selectedNodeShown();
      }
    }

    boolean isSelectedNodeShowingChanged(final FTreeNodeEvent expansionEvent) {
      final FToggleNode selNode = (FToggleNode) tree.getSelectedNode();
      if (selNode == null) {
        return false;
      }
      final FToggleNode node = (FToggleNode) expansionEvent.getTreeNode();
      FToggleNode curr;
      for (curr = (FToggleNode) selNode.getParent();
           curr != node && curr != null;
           curr = (FToggleNode) curr.getParent()) {
        if (!tree.isNodeExpanded(curr)) {
          return false;
        }
      }

      return curr != null;
    }

    void selectedNodeShown() {
      FToggleNode node = (FToggleNode) tree.getSelectedNode();
      if (node == null) {
        return;
      }
      final FToggleNode parent = (FToggleNode) node.getParent();
      if (parent != null) {
        setEditorFlags(parent, node);
      }
      node = (FToggleNode) node.getSubstituteNode();
      if (node == null) {
        return;
      }
      nodesInfoArea.setText(model.getNodeMessage(node));
      if (hasValue(node)) {
        final String errMessage =
            model.getValidityMessage(node, node.getValue());
        if (errMessage != null) {
          errorInfoArea.setText("Error: " + errMessage);
        } else {
          errorInfoArea.setText("OK");
        }
      } else {
        errorInfoArea.setText("");
      }
    }

    @Override
    public void nodeCollapsed(final FTreeNodeEvent e) {
      if (isSelectedNodeShowingChanged(e)) {
        selectedNodeHidden();
      }
    }

    void selectedNodeHidden() {
      clearEditorFlags();
      nodesInfoArea.setText("");
      errorInfoArea.setText("");
    }

    @Override
    public void enteredExpansBar(final FTreeNodeEvent e) {
      final FToggleNode node = (FToggleNode) e.getTreeNode();
      final FToggleNode parent = (FToggleNode) node.getParent();
      String text = "  Folder: ";
      if (parent != null) {
        final XSRef ref = (XSRef) parent.getAssociate();
        if (ref.isArray()) {
          text = "  Folder: #" + (parent.getIndex(node) + 1) + ' ';
        }
      }
      text += node.getLabelText();
      final FAbstractToggleNode subNode = node.getSubstituteNode();
      if (subNode != null && subNode != node) {
        text = text + ' ' + subNode.getLabelText();
      }
      if (node.getValue() != null) {
        text = text + "     " + node.getValue();
      }
      mouseInfo.setText(text);
      mouseInfo.setCaretPosition(0);
    }

    @Override
    public void exitedExpansBar(final FTreeNodeEvent e) {
      mouseInfo.setText("  Folder:");
    }

    @Override
    public void cellEditingStarted(final FTreeEditorEvent ftreeeditorevent) {
      // nothing
    }

    @Override
    public void cellEditingWillStop(final FTreeEditorEvent e) {
      if (e.isCanceled()) {
        return;
      }
      FToggleNode node = (FToggleNode) e.getTreeNode();
      node = (FToggleNode) node.getSubstituteNode();
      if (node == null || !hasValue(node) || !node.isEditable()) {
        return;
      }
      final JComponent editor = e.getEditor();
      if (editor == null) {
        return;
      }
      final JComponent extraControl =
          ((FToggleControl) editor).getExtraControl();
      if (extraControl instanceof ICellControl) {
        final Object newValue = ((ICellControl) extraControl).getData();
        final Object oldValue = node.getValue();
        if (!equal(oldValue, newValue)) {
          model.setNodeValue(node, newValue);
        }
      }
    }

    boolean equal(final Object a, final Object b) {
      return a == null && b == null || a != null && a.equals(b);
    }

    @Override
    public void cellEditingStopped(final FTreeEditorEvent e) {
      if (e.isCanceled()) {
        selectedNodeShown();
      }
    }

    @Override
    public void cellEditorValueChanged(final FTreeEditorEvent e) {
      FToggleNode node = (FToggleNode) e.getTreeNode();
      node = (FToggleNode) node.getSubstituteNode();
      if (node == null) {
        return;
      }
      final Object editor = e.getEditor();
      if (!(editor instanceof ICellControl)) {
        return;
      }
      final Object obj = ((ICellControl) editor).getData();
      final String editorValue = obj != null? obj.toString() : "";
      final String errMessage = model.getValidityMessage(node, editorValue);
      if (errMessage != null) {
        errorInfoArea.setText("Error: " + errMessage);
      } else {
        errorInfoArea.setText("OK");
      }
    }

    @Override
    public void treeActionPerformed(final FTreeActionEvent e) {
      if (e.containsAction(1)) {
        final FToggleNode node = (FToggleNode) e.getTreeNode();
        model.toggleSelectionChanged(node);
      }
    }

    @Override
    public void nodeSelected(final FTreeNodeEvent e) {
      selectedNodeShown();
    }

    @Override
    public void nodeUnselected(final FTreeNodeEvent e) {
      selectedNodeHidden();
    }

    @Override
    public void mousePressed(final MouseEvent e) {
      showPopup(e);
    }

    void showPopup(final MouseEvent e) {
      if (e.isPopupTrigger()) {
        treeNode = (FToggleNode) tree.getNodeAt(e.getX(), e.getY());
        final boolean b =
            treeNode != null && treeNode == tree.getSelectedNode();
        mSelect.setEnabled(
            treeNode != null && treeNode != tree.getSelectedNode() &&
            treeNode.isToggleSelected() && treeNode.isPathSelected());
        mUnselect.setEnabled(b);
        mInsBefore.setEnabled(b && canInsert());
        mInsAfter.setEnabled(b && canInsert());
        mRemove.setEnabled(b && canRemove());
        mMoveUp.setEnabled(b && canMoveUp());
        mMoveDown.setEnabled(b && canMoveDown());
        mNS.setEnabled(hasDocument());
        mFindInvalid.setEnabled(hasDocument() && !isDocValid());
        mFindNode.setEnabled(hasDocument());
        popup.show(tree, e.getX(), e.getY());
      }
    }

    @Override
    public void mouseClicked(final MouseEvent mouseevent) {
      // nothing
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
      showPopup(e);
    }

    @Override
    public void mouseEntered(final MouseEvent mouseevent) {
      // nothing
    }

    @Override
    public void mouseExited(final MouseEvent mouseevent) {
      // nothing
    }
  }
}
