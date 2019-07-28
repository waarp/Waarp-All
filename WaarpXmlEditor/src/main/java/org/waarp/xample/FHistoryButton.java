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

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

/*
 * Copyright (c) 2002 Felix Golubov
 */

/**
 * A simple button with drop-down menu for the XAmple application.
 *
 * @author Felix Golubov
 * @version 1.0
 */

public class FHistoryButton extends JComponent
    implements ItemSelectable, ActionListener {
  /**
   *
   */
  private static final long serialVersionUID = -5176285056068997723L;
  final JToggleButton leftButton = new JToggleButton();
  final Btn rightButton = new Btn();
  final JPopupMenu popupMenu = new JPopupMenu();
  final ArrayList<History> items = new ArrayList<History>();
  final ArrayList<ItemListener> itemListeners = new ArrayList<ItemListener>(1);
  final ArrayList<ActionListener> actionListeners =
      new ArrayList<ActionListener>(1);

  public FHistoryButton(ImageIcon icon, String leftTipText,
                        String rightTipText) {
    leftButton.setFocusPainted(false);
    setIcon(icon);
    final ImageIcon icoArrow = FLoader.getIcon(this, "DropDown.gif");
    rightButton.setIcon(icoArrow);
    rightButton.setDisabledIcon(FadingFilter.fade(icoArrow));
    leftButton.setToolTipText(leftTipText);
    rightButton.setToolTipText(rightTipText);
    popupMenu.addPopupMenuListener(rightButton);
    leftButton.addActionListener(this);
    setBorder(BorderFactory.createLineBorder(Color.gray));
    setLayout(new BorderLayout());
    add(leftButton, BorderLayout.CENTER);
    add(rightButton, BorderLayout.EAST);
    setPreferredSize(new Dimension(44 + 14, 27));
    setMinimumSize(new Dimension(44 + 14, 27));
    setMaximumSize(new Dimension(44 + 14, 27));
    setItems(null);
  }

  public void setIcon(ImageIcon icon) {
    leftButton.setIcon(icon);
    if (icon != null) {
      leftButton.setDisabledIcon(FadingFilter.fade(icon));
    }
  }

  public void setItems(List<History> items) {
    popupMenu.removeAll();
    this.items.clear();
    if (items != null) {
      for (int i = 0; i < items.size(); i++) {
        final History item = items.get(i);
        if (item == null) {
          continue;
        }
        this.items.add(item);
        final JMenuItem mi = new JMenuItem(item.toString());
        mi.addActionListener(this);
        popupMenu.add(mi);
      }
    }
    if (this.items.isEmpty()) {
      final JMenuItem mi = new JMenuItem("< Empty >");
      mi.setEnabled(false);
      popupMenu.add(mi);
    }
  }

  @Override
  public Insets getInsets() {
    return new Insets(2, 2, 1, 1);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() instanceof JMenuItem) {
      final int index = popupMenu.getComponentIndex((Component) e.getSource());
      if (index >= 0 && index < items.size()) {
        final ItemEvent ie =
            new ItemEvent(this, index, items.get(index), ItemEvent.SELECTED);
        for (int i = 0; i < itemListeners.size(); i++) {
          final ItemListener l = itemListeners.get(i);
          l.itemStateChanged(ie);
        }
      }
    } else if (e.getSource() == leftButton) {
      leftButton.setSelected(false);

      for (int i = 0; i < actionListeners.size(); i++) {
        final ActionListener l = actionListeners.get(i);
        final ActionEvent ae =
            new ActionEvent(this, e.getID(), e.getActionCommand());
        l.actionPerformed(ae);
      }
    }
  }

  @Override
  public void addItemListener(ItemListener l) {
    if (!itemListeners.contains(l)) {
      itemListeners.add(l);
    }
  }

  @Override
  public Object[] getSelectedObjects() {
    return null;
  }

  @Override
  public void removeItemListener(ItemListener l) {
    itemListeners.remove(l);
  }

  public void addActionListener(ActionListener l) {
    if (!actionListeners.contains(l)) {
      actionListeners.add(l);
    }
  }

  public void removeActionListener(ActionListener l) {
    actionListeners.remove(l);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    leftButton.setEnabled(enabled);
    rightButton.setEnabled(enabled);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    SwingUtilities.updateComponentTreeUI(popupMenu);
  }

  class Btn extends JToggleButton implements MouseListener, PopupMenuListener {
    /**
     *
     */
    private static final long serialVersionUID = -1272417382610945574L;
    boolean b;

    Btn() {
      setFocusPainted(false);
      addMouseListener(this);
    }

    @Override
    public Dimension getMinimumSize() {
      super.getMinimumSize();
      return new Dimension(14, 27);
    }

    @Override
    public Dimension getPreferredSize() {
      super.getPreferredSize();
      return new Dimension(14, 27);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      // nothing
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if (!isEnabled()) {
        return;
      }
      if (popupMenu.isShowing()) {
        b = true;
      } else {
        popupMenu
            .show(FHistoryButton.this, 0, FHistoryButton.this.getSize().height);
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      // nothing
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      // nothing
    }

    @Override
    public void mouseExited(MouseEvent e) {
      // nothing
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
      // nothing
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      if (b) {
        b = false;
      } else {
        setSelected(false);
      }
    }

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      // nothing
    }
  }
}
