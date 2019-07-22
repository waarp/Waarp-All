/*******************************************************************************
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 *  individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at your
 *  option) any later version.
 *
 *  Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 *  A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  Waarp . If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

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

public class FXBasicView extends JComponent
    implements FXModelStatusListener {
  FXModel model;
  FTree tree;
  JTextField mouseInfo;
  JTextArea errorInfoArea;
  JTextArea nodesInfoArea;
  Vector viewListeners;
  InnerListener innerListener;
  boolean _insert;
  boolean _remove;
  boolean _moveUp;
  boolean _moveDown;
  JPopupMenu popup;
  JMenuItem mSelect;
  JMenuItem mUnselect;
  JMenuItem mInsBefore;
  JMenuItem mInsAfter;
  JMenuItem mRemove;
  JMenuItem mMoveUp;
  JMenuItem mMoveDown;
  JMenuItem mFindInvalid;
  JMenuItem mFindNode;
  JMenuItem mNS;
  SearchDialog searchDialog;

  public FXBasicView(FXModel model) {
    this.model = null;
    viewListeners = new Vector();
    innerListener = new InnerListener();
    _insert = false;
    _remove = false;
    _moveUp = false;
    _moveDown = false;
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
    JSplitPane mainSplitPane = new JSplitPane(0);
    mainSplitPane.setResizeWeight(0.80000000000000004D);
    mainSplitPane.setDividerSize(8);
    mainSplitPane.setDoubleBuffered(false);
    add(mainSplitPane, "Center");
    JScrollPane sp = new JScrollPane();
    tree = new GgFTree();
    setFXModel(model);
    Insets insets = tree.getInsets();
    insets.top = 20;
    tree.setInsets(insets);
    sp.getViewport().add(tree, null);
    mainSplitPane.add(sp, "left");
    JPanel infoPanel = new JPanel(new BorderLayout());
    JSplitPane infoSplitPane = new JSplitPane(1);
    infoSplitPane.setResizeWeight(0.5D);
    infoSplitPane.setDividerSize(8);
    JScrollPane nodesInfoAreaSP = new JScrollPane();
    nodesInfoAreaSP.setHorizontalScrollBarPolicy(31);
    nodesInfoArea = new JTextArea();
    nodesInfoArea.setLineWrap(true);
    nodesInfoArea.setWrapStyleWord(true);
    nodesInfoArea.setEditable(false);
    nodesInfoAreaSP.getViewport().add(nodesInfoArea, null);
    infoSplitPane.add(nodesInfoAreaSP, "left");
    JScrollPane errorInfoAreaSP = new JScrollPane();
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

  public static void stopCellEditing(JComponent editor) {
    FTree.stopCellEditing(editor);
  }

  public static void cancelCellEditing(JComponent editor) {
    FTree.cancelCellEditing(editor);
  }

  public static void cellEditorValueChanged(JComponent editor, Object event) {
    FTree.cellEditorValueChanged(editor, event);
  }

  static boolean hasValue(FToggleNode node) {
    XSRef ref = (XSRef) node.getAssociate();
    return ref.hasValue();
  }

  public void newDocumentLoaded(FXStatusEvent e) {
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

  void setInsert(boolean insert) {
    if (insert == _insert) {
      return;
    }
    _insert = insert;
    FXStatusEvent e = new FXStatusEvent(this, _insert);
    for (int i = 0; i < viewListeners.size(); i++) {
      FXViewStatusListener fxl = (FXViewStatusListener) viewListeners.get(i);
      fxl.canInsertStatusChanged(e);
    }

  }

  void setRemove(boolean remove) {
    if (remove == _remove) {
      return;
    }
    _remove = remove;
    FXStatusEvent e = new FXStatusEvent(this, _remove);
    for (int i = 0; i < viewListeners.size(); i++) {
      FXViewStatusListener fxl = (FXViewStatusListener) viewListeners.get(i);
      fxl.canRemoveStatusChanged(e);
    }

  }

  void setMoveUp(boolean moveUp) {
    if (moveUp == _moveUp) {
      return;
    }
    _moveUp = moveUp;
    FXStatusEvent e = new FXStatusEvent(this, _moveUp);
    for (int i = 0; i < viewListeners.size(); i++) {
      FXViewStatusListener fxl = (FXViewStatusListener) viewListeners.get(i);
      fxl.canMoveUpStatusChanged(e);
    }

  }

  void setMoveDown(boolean moveDown) {
    if (moveDown == _moveDown) {
      return;
    }
    _moveDown = moveDown;
    FXStatusEvent e = new FXStatusEvent(this, _moveDown);
    for (int i = 0; i < viewListeners.size(); i++) {
      FXViewStatusListener fxl = (FXViewStatusListener) viewListeners.get(i);
      fxl.canMoveDownStatusChanged(e);
    }

  }

  public void docValidityStatusChanged(FXStatusEvent fxstatusevent) {
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

                                public void actionPerformed(ActionEvent e) {
                                  FToggleNode node = innerListener.treeNode;
                                  if (innerListener.treeNode != null) {
                                    tree.setSelectedPath(innerListener.treeNode.getPath());
                                  }
                                }

                              }
    );
    popup.add(mSelect);
    mUnselect.setIcon(FLoader.getIcon(this, "UnselectNode.gif"));
    mUnselect.addActionListener(new ActionListener() {

                                  public void actionPerformed(ActionEvent e) {
                                    tree.setSelectedPath(null);
                                  }

                                }
    );
    popup.add(mUnselect);
    popup.addSeparator();
    mInsBefore.setIcon(FLoader.getIcon(this, "InsNodeBefore.gif"));
    mInsBefore.addActionListener(new ActionListener() {

                                   public void actionPerformed(ActionEvent e) {
                                     insertNodeBefore();
                                   }

                                 }
    );
    popup.add(mInsBefore);
    mInsAfter.setIcon(FLoader.getIcon(this, "InsNodeAfter.gif"));
    mInsAfter.addActionListener(new ActionListener() {

                                  public void actionPerformed(ActionEvent e) {
                                    insertNodeAfter();
                                  }

                                }
    );
    popup.add(mInsAfter);
    popup.addSeparator();
    mRemove.setIcon(FLoader.getIcon(this, "RemoveNode.gif"));
    mRemove.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent e) {
                                  removeNode();
                                }

                              }
    );
    popup.add(mRemove);
    popup.addSeparator();
    mMoveUp.setIcon(FLoader.getIcon(this, "MoveNodeUp.gif"));
    mMoveUp.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent e) {
                                  moveNodeUp();
                                }

                              }
    );
    popup.add(mMoveUp);
    mMoveDown.setIcon(FLoader.getIcon(this, "MoveNodeDown.gif"));
    mMoveDown.addActionListener(new ActionListener() {

                                  public void actionPerformed(ActionEvent e) {
                                    moveNodeDown();
                                  }

                                }
    );
    popup.add(mMoveDown);
    popup.addSeparator();
    mNS.setIcon(FLoader.getIcon(this, "NSQualifiers.gif"));
    mNS.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent e) {
                              showNSQualifiersDialog();
                            }

                          }
    );
    popup.add(mNS);
    popup.addSeparator();
    mFindInvalid.setIcon(FLoader.getIcon(this, "FindInvalidNode.gif"));
    mFindInvalid.addActionListener(new ActionListener() {

                                     public void actionPerformed(ActionEvent e) {
                                       showInvalidNode();
                                     }

                                   }
    );
    popup.add(mFindInvalid);
    mFindNode.setIcon(FLoader.getIcon(this, "FindNode.gif"));
    mFindNode.addActionListener(new ActionListener() {

                                  public void actionPerformed(ActionEvent e) {
                                    showSearchDialog();
                                  }

                                }
    );
    popup.add(mFindNode);
  }

  public void addExternalDialog(String id, JDialog dialog) {
    tree.addDialog(id, dialog);
  }

  public void removeExternalDialog(String id) {
    tree.removeDialog(id);
  }

  public void removeAllExternalDialogs() {
    tree.removeAllDialogs();
  }

  public void setBackground(Color color) {
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

  public void setReducedView(boolean reduced) {
    tree.setReducedView(reduced);
    FToggleNode node = (FToggleNode) tree.getSelectedNode();
    if (node == null) {
      return;
    }
    FToggleNode parent = (FToggleNode) node.getParent();
    if (parent == null) {
      return;
    } else {
      setEditorFlags(parent, node);
      return;
    }
  }

  void setEditorFlags(FToggleNode parent, FToggleNode node) {
    int count = parent.getRealChildCount();
    XSRef ref = (XSRef) parent.getAssociate();
    boolean b = !tree.isReducedView();
    if (ref.isArray()) {
      XSParticle particle = ref.getParticle();
      int minOccurs = Math.max(particle.getMinOccurs(), 1);
      int maxOccurs = particle.getMaxOccursUnbounded()? 0x7fffffff :
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

  public void showInfoMessage(String message) {
    nodesInfoArea.setText(message);
  }

  public void showErrorMessage(String message) {
    errorInfoArea.setText(message);
  }

  public void addViewStatusListener(FXViewStatusListener l) {
    if (!viewListeners.contains(l)) {
      viewListeners.addElement(l);
    }
  }

  public void removeViewStatusListener(FXViewStatusListener l) {
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
    return _insert;
  }

  public boolean canRemove() {
    return _remove;
  }

  public boolean canMoveUp() {
    return _moveUp;
  }

  public boolean canMoveDown() {
    return _moveDown;
  }

  public void insertNodeBefore() {
    if (!canInsert()) {
      return;
    }
    FToggleNode node = (FToggleNode) tree.getSelectedNode();
    if (node == null) {
      return;
    }
    FToggleNode parent = (FToggleNode) node.getParent();
    if (parent == null) {
      return;
    }
    int index = parent.getIndex(node);
    if (model.insertInstance(parent, index) != null) {
      tree.setSelectedPath(node.getPath());
      setEditorFlags(parent, node);
    }
  }

  public void insertNodeAfter() {
    if (!canInsert()) {
      return;
    }
    FToggleNode node = (FToggleNode) tree.getSelectedNode();
    if (node == null) {
      return;
    }
    FToggleNode parent = (FToggleNode) node.getParent();
    if (parent == null) {
      return;
    }
    int index = parent.getIndex(node);
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
    FToggleNode parent = (FToggleNode) node.getParent();
    if (parent == null) {
      return;
    }
    int index = model.removeInstance(node);
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
    FToggleNode node = (FToggleNode) tree.getSelectedNode();
    if (node == null) {
      return;
    }
    FToggleNode parent = (FToggleNode) node.getParent();
    if (parent == null) {
      return;
    } else {
      int index = parent.getIndex(node);
      parent.remove(index);
      parent.insert(node, index - 1);
      model.fireTreeModelDataChanged(true);
      tree.setSelectedPath(node.getPath());
      setEditorFlags(parent, node);
      return;
    }
  }

  public void moveNodeDown() {
    if (!canMoveDown()) {
      return;
    }
    FToggleNode node = (FToggleNode) tree.getSelectedNode();
    if (node == null) {
      return;
    }
    FToggleNode parent = (FToggleNode) node.getParent();
    if (parent == null) {
      return;
    } else {
      int index = parent.getIndex(node);
      parent.remove(index);
      parent.insert(node, index + 1);
      model.fireTreeModelDataChanged(true);
      tree.setSelectedPath(node.getPath());
      setEditorFlags(parent, node);
      return;
    }
  }

  void setDialogLocation(JDialog dlg) {
    Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension size = getSize();
    Dimension dlgSize = dlg.getSize();
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
    Frame frame =
        (Frame) SwingUtilities.getAncestorOfClass(java.awt.Frame.class, this);
    NSQualifiersDialog dlg = new NSQualifiersDialog(frame, model);
    setDialogLocation(dlg);
    dlg.setVisible(true);
  }

  int[] createPath(FBasicNode treeNode) {
    Vector v = new Vector();
    for (FBasicNode node = treeNode; node != null;
         node = (FBasicNode) node.getParent()) {
      v.add(0, node);
    }

    int path[] = new int[v.size()];
    FBasicNode parent = (FBasicNode) v.get(0);
    for (int i = 1; i < v.size(); i++) {
      FBasicNode child = (FBasicNode) v.get(i);
      path[i - 1] = parent.getIndex(child);
      parent = child;
    }

    path[path.length - 1] = 0;
    return path;
  }

  FToggleNode getInvalidNode(FToggleNode parent, int startPath[], int level) {
    int startIndex = startPath[level];
    FToggleNode node = null;
    for (int i = startIndex; i < parent.getRealChildCount(); i++) {
      FToggleNode child = (FToggleNode) parent.getRealChildAt(i);
      if (child.isToggleSelected()
          && (!child.getChildrenValidity() || !child.getNodeValidity())) {
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
    if (!parent.getNodeValidity()) {
      return parent;
    }
    FToggleNode node = null;
    for (int i = 0; i < parent.getRealChildCount(); i++) {
      FToggleNode child = (FToggleNode) parent.getRealChildAt(i);
      if (child.isToggleSelected()
          && (!child.getChildrenValidity() || !child.getNodeValidity())) {
        return getInvalidNode(child);
      }
    }

    return parent;
  }

  public void showInvalidNode() {
    if (!hasDocument() || isDocValid()) {
      return;
    }
    FToggleNode node = null;
    FToggleNode rootNode = (FToggleNode) tree.getRoot();
    FToggleNode startNode = (FToggleNode) tree.getSelectedNode();
    if (startNode != null) {
      startNode = (FToggleNode) startNode.getSubstituteNode();
    }
    if (startNode != null) {
      int startPath[] = createPath(startNode);
      node = getInvalidNode(rootNode, startPath, 0);
    }
    if (node == null) {
      node = getInvalidNode(rootNode);
    }
    if (node != null) {
      if (node.getParent() instanceof FToggleSwitchNode) {
        node = (FToggleNode) node.getParent();
      }
      Object path[] = node.getPath();
      tree.makeVisible(path);
      tree.setSelectedPath(path);
    }
  }

  public void showSearchDialog() {
    if (searchDialog == null) {
      Frame frame =
          (Frame) SwingUtilities.getAncestorOfClass(java.awt.Frame.class, this);
      searchDialog = new SearchDialog(frame, this);
      searchDialog.pack();
      setDialogLocation(searchDialog);
    }
    searchDialog.setVisible(true);
  }

  class InnerListener
      implements FTreeExpansionListener, FTreeExpansBarListener,
                 FTreeEditorListener,
                 FTreeActionListener, FTreeSelectionListener, MouseListener {

    FToggleNode treeNode;

    InnerListener() {
      treeNode = null;
    }

    public void nodeWillExpand(FTreeNodeEvent e) {
      FToggleNode node = (FToggleNode) e.getTreeNode();
      node = (FToggleNode) node.getSubstituteNode();
      if (node != null && model.populateNode(node)) {
        Object selPath[] = tree.getSelectedPath();
        model.fireTreeModelDataChanged(true);
        tree.setSelectedPath(selPath);
      }
    }

    public void nodeWillCollapse(FTreeNodeEvent ftreenodeevent) {
    }

    public void nodeExpanded(FTreeNodeEvent e) {
      if (isSelectedNodeShowingChanged(e)) {
        selectedNodeShown();
      }
    }

    boolean isSelectedNodeShowingChanged(FTreeNodeEvent expansionEvent) {
      FToggleNode selNode = (FToggleNode) tree.getSelectedNode();
      if (selNode == null) {
        return false;
      }
      FToggleNode node = (FToggleNode) expansionEvent.getTreeNode();
      FToggleNode curr;
      for (curr = (FToggleNode) selNode.getParent();
           curr != node && curr != null; curr = (FToggleNode) curr
          .getParent()) {
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
      FToggleNode parent = (FToggleNode) node.getParent();
      if (parent != null) {
        setEditorFlags(parent, node);
      }
      node = (FToggleNode) node.getSubstituteNode();
      if (node == null) {
        return;
      }
      nodesInfoArea.setText(model.getNodeMessage(node));
      if (FXBasicView.hasValue(node)) {
        String errMessage = model.getValidityMessage(node, node.getValue());
        if (errMessage != null) {
          errorInfoArea.setText("Error: " + errMessage);
        } else {
          errorInfoArea.setText("OK");
        }
      } else {
        errorInfoArea.setText("");
      }
    }

    public void nodeCollapsed(FTreeNodeEvent e) {
      if (isSelectedNodeShowingChanged(e)) {
        selectedNodeHidden();
      }
    }

    void selectedNodeHidden() {
      clearEditorFlags();
      nodesInfoArea.setText("");
      errorInfoArea.setText("");
    }

    public void enteredExpansBar(FTreeNodeEvent e) {
      FToggleNode node = (FToggleNode) e.getTreeNode();
      FToggleNode parent = (FToggleNode) node.getParent();
      String text = "  Folder: ";
      if (parent != null) {
        XSRef ref = (XSRef) parent.getAssociate();
        if (ref.isArray()) {
          text = "  Folder: #" + (parent.getIndex(node) + 1) + " ";
        }
      }
      text = text + node.getLabelText();
      FAbstractToggleNode subNode = node.getSubstituteNode();
      if (subNode != null && subNode != node) {
        text = text + " " + subNode.getLabelText();
      }
      if (node.getValue() != null) {
        text = text + "     " + node.getValue().toString();
      }
      mouseInfo.setText(text);
      mouseInfo.setCaretPosition(0);
    }

    public void exitedExpansBar(FTreeNodeEvent e) {
      mouseInfo.setText("  Folder:");
    }

    public void cellEditingStarted(FTreeEditorEvent ftreeeditorevent) {
    }

    public void cellEditingWillStop(FTreeEditorEvent e) {
      if (e.isCanceled()) {
        return;
      }
      FToggleNode node = (FToggleNode) e.getTreeNode();
      node = (FToggleNode) node.getSubstituteNode();
      if (node == null || !FXBasicView.hasValue(node) || !node.isEditable()) {
        return;
      }
      JComponent editor = e.getEditor();
      if (editor == null) {
        return;
      }
      JComponent extraControl = ((FToggleControl) editor).getExtraControl();
      if (extraControl instanceof ICellControl) {
        Object newValue = ((ICellControl) extraControl).getData();
        Object oldValue = node.getValue();
        if (!equal(oldValue, newValue)) {
          model.setNodeValue(node, newValue);
        }
      }
    }

    boolean equal(Object a, Object b) {
      return a == null && b == null || a != null && a.equals(b);
    }

    public void cellEditingStopped(FTreeEditorEvent e) {
      if (e.isCanceled()) {
        selectedNodeShown();
      }
    }

    public void cellEditorValueChanged(FTreeEditorEvent e) {
      FToggleNode node = (FToggleNode) e.getTreeNode();
      node = (FToggleNode) node.getSubstituteNode();
      if (node == null) {
        return;
      }
      Object editor = e.getEditor();
      if (!(editor instanceof ICellControl)) {
        return;
      }
      Object obj = ((ICellControl) editor).getData();
      String editorValue = obj != null? obj.toString() : "";
      String errMessage = model.getValidityMessage(node, editorValue);
      if (errMessage != null) {
        errorInfoArea.setText("Error: " + errMessage);
      } else {
        errorInfoArea.setText("OK");
      }
    }

    public void treeActionPerformed(FTreeActionEvent e) {
      if (e.containsAction(1)) {
        FToggleNode node = (FToggleNode) e.getTreeNode();
        model.toggleSelectionChanged(node);
      }
    }

    public void nodeSelected(FTreeNodeEvent e) {
      selectedNodeShown();
    }

    public void nodeUnselected(FTreeNodeEvent e) {
      selectedNodeHidden();
    }

    public void mousePressed(MouseEvent e) {
      showPopup(e);
    }

    void showPopup(MouseEvent e) {
      if (!e.isPopupTrigger()) {
        return;
      } else {
        treeNode = (FToggleNode) tree.getNodeAt(e.getX(), e.getY());
        boolean b = treeNode != null && treeNode == tree.getSelectedNode();
        mSelect
            .setEnabled(treeNode != null && treeNode != tree.getSelectedNode()
                        && treeNode.isToggleSelected() &&
                        treeNode.isPathSelected());
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
        return;
      }
    }

    public void mouseClicked(MouseEvent mouseevent) {
    }

    public void mouseReleased(MouseEvent e) {
      showPopup(e);
    }

    public void mouseEntered(MouseEvent mouseevent) {
    }

    public void mouseExited(MouseEvent mouseevent) {
    }
  }
}
