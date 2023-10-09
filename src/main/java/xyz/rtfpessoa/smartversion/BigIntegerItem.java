package xyz.rtfpessoa.smartversion;

import java.math.BigInteger;

/** Represents a numeric item in the version item list. */
public record BigIntegerItem(BigInteger value) implements Item {

  @Override
  public Item.Type getType() {
    return Item.Type.BIGINTEGER;
  }

  @Override
  public boolean isNull() {
    return BigInteger.ZERO.equals(value);
  }

  @Override
  public int compareTo(Item item) {
    if (item == null) {
      return BigInteger.ZERO.equals(value) ? 0 : 1; // 1.0 == 1, 1.1 > 1
    }

    return switch (item.getType()) {
      case LONG, STRING -> 1;
      case BIGINTEGER -> value.compareTo(((BigIntegerItem) item).value);
      case COMBINATION -> 1; // 1.1 > 1-sp
      case LIST -> 1; // 1.1 > 1-1
    };
  }

  public String toString() {
    return value.toString();
  }
}
