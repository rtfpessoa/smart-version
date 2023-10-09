package xyz.rtfpessoa.smartversion;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Represents a version list item. This class is used both for the global item list and for
 * sub-lists (which start with '-(number)' in the version specification).
 */
public class ListItem extends ArrayList<Item> implements Item {

  private final ComparisonMode mode;

  public ListItem(ComparisonMode mode) {
    super();
    this.mode = mode;
  }

  @Override
  public Item.Type getType() {
    return Item.Type.LIST;
  }

  @Override
  public boolean isNull() {
    return (size() == 0);
  }

  void normalize() {
    for (int i = size() - 1; i >= 0; i--) {
      Item lastItem = get(i);

      if (lastItem.isNull()) {
        if (i == size() - 1 || get(i + 1).getType() == Item.Type.STRING) {
          remove(i);
        } else if (get(i + 1).getType() == Item.Type.LIST) {
          Item item = ((ListItem) get(i + 1)).get(0);
          if (item.getType() == Item.Type.COMBINATION || item.getType() == Item.Type.STRING) {
            remove(i);
          }
        }
      }
    }
  }

  @Override
  public int compareTo(Item item) {
    if (item == null) {
      if (!ComparisonMode.MAVEN.equals(mode)) {
        return -1;
      }

      if (size() == 0) {
        return 0; // 1-0 = 1- (normalize) = 1
      }
      // Compare the entire list of items with null - not just the first one, MNG-6964
      for (Item i : this) {
        int result = i.compareTo(null);
        if (result != 0) {
          return result;
        }
      }
      return 0;
    }
    switch (item.getType()) {
      case LONG:
      case BIGINTEGER:
        return -1; // 1-1 < 1.0.x

      case STRING:
        return 1;
      case COMBINATION:
        return 1; // 1-1 > 1-sp

      case LIST:
        Iterator<Item> left = iterator();
        Iterator<Item> right = ((ListItem) item).iterator();

        while (left.hasNext() || right.hasNext()) {
          Item l = left.hasNext() ? left.next() : null;
          Item r = right.hasNext() ? right.next() : null;

          // if this is shorter, then invert the compare and mul with -1
          int result = l == null ? (r == null ? 0 : -1 * r.compareTo(l)) : l.compareTo(r);

          if (result != 0) {
            return result;
          }
        }

        return 0;

      default:
        throw new IllegalStateException("invalid item: " + item.getClass());
    }
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    for (Item item : this) {
      if (!buffer.isEmpty()) {
        buffer.append((item instanceof ListItem) ? '-' : '.');
      }
      buffer.append(item);
    }
    return buffer.toString();
  }
}
