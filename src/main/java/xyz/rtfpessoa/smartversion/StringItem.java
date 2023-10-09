package xyz.rtfpessoa.smartversion;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/** Represents a string in the version item list, usually a qualifier. */
public record StringItem(String value, ComparisonMode mode) implements Item {
  private static final List<String> QUALIFIERS =
      Arrays.asList("alpha", "beta", "milestone", "rc", "snapshot", "", "sp");
  private static final List<String> RELEASE_QUALIFIERS = Arrays.asList("ga", "final", "release");

  private static final Properties ALIASES = new Properties();

  static {
    ALIASES.put("cr", "rc");
  }

  /**
   * A comparable value for the empty-string qualifier. This one is used to determine if a given
   * qualifier makes the version older than one without a qualifier, or more recent.
   */
  private static final String RELEASE_VERSION_INDEX = String.valueOf(QUALIFIERS.indexOf(""));

  /**
   * A comparable value for the empty-string qualifier. This one is used to determine if a given
   * qualifier makes the version older than one without a qualifier, or more recent.
   */
  private static final String MAX_VERSION_INDEX = String.valueOf(Integer.MAX_VALUE);

  public static StringItem newStringItem(
      String value, boolean followedByDigit, ComparisonMode mode) {
    if (ComparisonMode.SEMVER.equals(mode)) {
      return new StringItem(value, mode);
    }

    if (followedByDigit && value.length() == 1) {
      // a1 = alpha-1, b1 = beta-1, m1 = milestone-1
      switch (value.charAt(0)) {
        case 'a':
          value = "alpha";
          break;
        case 'b':
          value = "beta";
          break;
        case 'm':
          value = "milestone";
          break;
        default:
      }
    }
    return new StringItem(ALIASES.getProperty(value, value), mode);
  }

  @Override
  public Item.Type getType() {
    return Item.Type.STRING;
  }

  @Override
  public boolean isNull() {
    return value == null || value.isEmpty();
  }

  /**
   * Returns a comparable value for a qualifier.
   *
   * <p>This method takes into account the ordering of known qualifiers then unknown qualifiers with
   * lexical ordering.
   *
   * @param qualifier value
   * @return an equivalent value that can be used with lexical comparison
   */
  public String comparableQualifier(String qualifier) {
    if (ComparisonMode.SEMVER.equals(mode)) {
      return qualifier;
    }

    if (RELEASE_QUALIFIERS.contains(qualifier)) {
      return String.valueOf(QUALIFIERS.indexOf(""));
    }

    int i = QUALIFIERS.indexOf(qualifier);

    // Just returning an Integer with the index here is faster, but requires a lot of if/then/else
    // to check for
    // -1
    //  or QUALIFIERS.size and then resort to lexical ordering. Most comparisons are decided by
    // the first
    // character,
    // so this is still fast. If more characters are needed then it requires a lexical sort
    // anyway.
    return i == -1 ? (QUALIFIERS.size() + "-" + qualifier) : String.valueOf(i);
  }

  @Override
  public int compareTo(Item item) {
    if (item == null) {
      return ComparisonMode.MAVEN.equals(mode)
          ? comparableQualifier(value).compareTo(RELEASE_VERSION_INDEX) // 1-rc < 1, 1-ga > 1
          : comparableQualifier(value)
              .compareTo(MAX_VERSION_INDEX); // 1-rc < 1, 1-ga < 1, 1-foo < 1
    }
    switch (item.getType()) {
      case LONG:
      case BIGINTEGER:
        return -1; // 1.any < 1.1 ?

      case STRING:
        var stringItemToCompare = (StringItem) item;
        return comparableQualifier(value).compareTo(comparableQualifier(stringItemToCompare.value));

      case COMBINATION:
        var combinationItemToCompare = (CombinationItem) item;
        int result = this.compareTo(combinationItemToCompare.getStringPart());
        if (result == 0) {
          return -1;
        }
        return result;

      case LIST:
        return ComparisonMode.MAVEN.equals(mode) ? -1 : 1; // 1.any < 1-1

      default:
        throw new IllegalStateException("invalid item: " + item.getClass());
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
