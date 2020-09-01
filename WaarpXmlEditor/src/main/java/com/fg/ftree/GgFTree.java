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
package com.fg.ftree;

import javax.swing.tree.TreeNode;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Extension of the work of Felix Golubov
 */
public class GgFTree extends FTree {

  /**
   *
   */
  private static final long serialVersionUID = -208048327893735151L;

  /**
   *
   */
  public GgFTree() {
    this(null);
  }

  /**
   * @param root
   */
  public GgFTree(final TreeNode root) {
    super(root);
  }

  // Change print on main window
  @Override
  void drawTree(final Graphics g) {
    Rectangle r = g.getClipBounds();
    if (r == null) {
      r = new Rectangle(getSize());
    }
    g.setColor(bgColor);
    g.fillRect(r.x, r.y, r.width, r.height);
    if (r.y < 20) {
      final String s = "XML GoldenGate Editor: F Golubov & F Bregier 2010";
      g.setColor(Color.white);
      g.drawString(s, 11, 14);
      g.setColor(Color.black);
      g.drawString(s, 10, 13);
    }
    if (itemsCount != 0) {
      firstIndex = findItemIndex(firstIndex, r.y)[1];
      final int lastIndex = findItemIndex(firstIndex, r.y + r.height - 1)[1];
      drawArea(g, firstIndex, lastIndex, r);
      repainted = true;
    }
  }

  private int[] findItemIndex(int index, final int y) {
    if (index >= itemsCount) {
      index = itemsCount - 1;
    }
    Item item = (Item) items.elementAt(index);
    if (y >= item.y) {
      for (; index < itemsCount; index++) {
        item = (Item) items.elementAt(index);
        if (y < item.y + cellGUI.getRowHeight(this, item.node)) {
          if (y >= item.y) {
            return new int[] { index, index };
          } else {
            return new int[] { index - 1, index };
          }
        }
      }

      return new int[] { itemsCount - 1, itemsCount };
    }
    for (; index > 0; index--) {
      item = (Item) items.elementAt(index);
      if (y >= item.y) {
        if (y < item.y + cellGUI.getRowHeight(this, item.node)) {
          return new int[] { index, index };
        } else {
          return new int[] { index, index + 1 };
        }
      }
    }

    return new int[2];
  }
}
