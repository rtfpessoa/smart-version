package xyz.rtfpessoa.smartversion;

import static xyz.rtfpessoa.smartversion.SmartVersion.parseItem;

import java.util.Objects;

/**
 * Represents a combination in the version item list. It is usually a combination of a string and a
 * number, with the string first and the number second.
 */
public class CombinationItem implements Item {

  StringItem stringPart;

  Item digitPart;

  CombinationItem(String value, ComparisonMode mode) {
    int index = 0;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (Character.isDigit(c)) {
        index = i;
        break;
      }
    }

    stringPart = StringItem.newStringItem(value.substring(0, index), true, mode);
    digitPart = parseItem(false, true, value.substring(index), mode);
  }

  @Override
  public int compareTo(Item item) {
    if (item == null) {
      // 1-rc1 < 1, 1-ga1 > 1
      return stringPart.compareTo(item);
    }
    int result = 0;
    switch (item.getType()) {
      case LONG:
      case BIGINTEGER, LIST:
        return -1;

      case STRING:
        result = stringPart.compareTo(item);
        if (result == 0) {
          // X1 > X
          return 1;
        }
        return result;

      case COMBINATION:
        result = stringPart.compareTo(((CombinationItem) item).getStringPart());
        if (result == 0) {
          return digitPart.compareTo(((CombinationItem) item).getDigitPart());
        }
        return result;
      default:
        return 0;
    }
  }

  public StringItem getStringPart() {
    return stringPart;
  }

  public Item getDigitPart() {
    return digitPart;
  }

  @Override
  public Item.Type getType() {
    return Item.Type.COMBINATION;
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CombinationItem that = (CombinationItem) o;
    return Objects.equals(stringPart, that.stringPart) && Objects.equals(digitPart, that.digitPart);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stringPart, digitPart);
  }

  @Override
  public String toString() {
    return stringPart.toString() + digitPart.toString();
  }
}
