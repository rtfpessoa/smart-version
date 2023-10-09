package xyz.rtfpessoa.smartversion;

/** Represents a numeric item in the version item list that can be represented with a long. */
public record LongItem(long value) implements Item {

  public static final LongItem ZERO = new LongItem(0);

  @Override
  public Item.Type getType() {
    return Item.Type.LONG;
  }

  @Override
  public boolean isNull() {
    return value == 0;
  }

  @Override
  public int compareTo(Item item) {
    if (item == null) {
      return (value == 0) ? 0 : 1; // 1.0 == 1, 1.1 > 1
    }

    return switch (item.getType()) {
      case STRING -> 1;
      case LONG -> {
        long itemValue = ((LongItem) item).value;
        yield Long.compare(value, itemValue);
      }
      case BIGINTEGER -> -1;
      case COMBINATION -> 1; // 1.1 > 1-sp

      case LIST -> 1; // 1.1 > 1-1
    };
  }

  @Override
  public String toString() {
    return Long.toString(value);
  }
}
