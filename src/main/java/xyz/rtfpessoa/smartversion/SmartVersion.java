package xyz.rtfpessoa.smartversion;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 *
 * <h2>Generic implementation of version comparison.</h2>
 *
 * <p>Comparison modes:
 *
 * <ul>
 *   <li>MAVEN (DEFAULT): Equal to `org.apache.maven.artifact.versioning.ComparableVersion`, certain
 *       pre-release qualifiers are considered before the final release while the rest is considered
 *       after the release. (all details explained in features section bellow)
 *   <li>SEMVER: All versions with pre-release information are considered before the final release,
 *       and it always follows alphabetical order for the pre-release qualifiers.
 *   <li>MIXED: All versions with pre-release information are considered before the final release,
 *       but it follows maven pre-release qualifiers ordering, alias and short names resolution.
 * </ul>
 *
 * <p>Features:
 *
 * <ul>
 *   <li>mixing of '<code>-</code>' (hyphen) and '<code>.</code>' (dot) separators,
 *   <li>transition between characters and digits also constitutes a separator: <code>
 *       1.0alpha1 =&gt; [1, [alpha, 1]]</code>
 *   <li>unlimited number of version components,
 *   <li>version components in the text can be digits or strings,
 *   <li>strings are checked for well-known qualifiers and the qualifier ordering is used for
 *       version ordering. Well-known qualifiers (case insensitive) are:
 *       <ul>
 *         <li><code>alpha</code> or <code>a</code>
 *         <li><code>beta</code> or <code>b</code>
 *         <li><code>milestone</code> or <code>m</code>
 *         <li><code>rc</code> or <code>cr</code>
 *         <li><code>snapshot</code>
 *         <li><code>(the empty string)</code> or <code>ga</code> or <code>final</code>
 *         <li><code>sp</code>
 *       </ul>
 *       Unknown qualifiers are considered after known qualifiers, with lexical order (always case
 *       insensitive),
 *   <li>a hyphen usually precedes a qualifier, and is always less important than digits/number, for
 *       example {@code 1.0.RC2 < 1.0-RC3 < 1.0.1}; but prefer {@code 1.0.0-RC1} over {@code
 *       1.0.0.RC1}, and more generally: {@code 1.0.X2 < 1.0-X3 < 1.0.1} for any string {@code X};
 *       but prefer {@code 1.0.0-X1} over {@code 1.0.0.X1}.
 * </ul>
 *
 * @see <a href="https://maven.apache.org/pom.html#version-order-specification">"Versioning" in the
 *     POM reference</a>
 */
public class SmartVersion implements Comparable<SmartVersion> {

  private static final int MAX_LONGITEM_LENGTH = 18;

  private String value;

  private Item.ComparisonMode mode;

  private String canonical;

  private ListItem items;

  public SmartVersion(String version) {
    this(version, Item.ComparisonMode.MAVEN);
  }

  public SmartVersion(String version, Item.ComparisonMode mode) {
    if (Item.ComparisonMode.MAVEN.equals(mode)) {
      parseVersion(version);
    } else {
      parseSemVerVersion(version, mode);
    }
  }

  @SuppressWarnings("checkstyle:innerassignment")
  public final void parseVersion(String version) {
    this.value = version;
    this.mode = Item.ComparisonMode.MAVEN;
    version = version.toLowerCase(Locale.ENGLISH);
    items = new ListItem(mode);

    ListItem list = items;

    Deque<Item> stack = new ArrayDeque<>();
    stack.push(list);

    boolean isDigit = false;

    boolean isCombination = false;

    int startIndex = 0;

    for (int i = 0; i < version.length(); i++) {
      char c = version.charAt(i);

      if (c == '.') {
        if (i == startIndex) {
          list.add(LongItem.ZERO);
        } else {
          list.add(parseItem(isCombination, isDigit, version.substring(startIndex, i), mode));
        }
        isCombination = false;
        startIndex = i + 1;
      } else if (c == '-') {
        if (i == startIndex) {
          list.add(LongItem.ZERO);
        } else {
          // X-1 is going to be treated as X1
          if (!isDigit && i != version.length() - 1) {
            char c1 = version.charAt(i + 1);
            if (Character.isDigit(c1)) {
              isCombination = true;
              continue;
            }
          }
          list.add(parseItem(isCombination, isDigit, version.substring(startIndex, i), mode));
        }
        startIndex = i + 1;

        list.add(list = new ListItem(mode));
        stack.push(list);
        isCombination = false;
      } else if (Character.isDigit(c)) {
        if (!isDigit && i > startIndex) {
          // X1
          isCombination = true;

          if (!list.isEmpty()) {
            list.add(list = new ListItem(mode));
            stack.push(list);
          }
        }

        isDigit = true;
      } else {
        if (isDigit && i > startIndex) {
          list.add(parseItem(isCombination, true, version.substring(startIndex, i), mode));
          startIndex = i;

          list.add(list = new ListItem(mode));
          stack.push(list);
          isCombination = false;
        }

        isDigit = false;
      }
    }

    if (version.length() > startIndex) {
      // 1.0.0.X1 < 1.0.0-X2
      // treat .X as -X for any string qualifier X
      if (!isDigit && !list.isEmpty()) {
        list.add(list = new ListItem(mode));
        stack.push(list);
      }

      list.add(parseItem(isCombination, isDigit, version.substring(startIndex), mode));
    }

    while (!stack.isEmpty()) {
      list = (ListItem) stack.pop();
      if (Item.ComparisonMode.MAVEN.equals(mode)) {
        list.normalize();
      }
    }
  }

  private final Pattern ITEM_PATTERN =
      Pattern.compile(
          "(?<qualifiername>[abm])(?<qualifierversion>[0-9]+)|(?<number>[0-9]+)|(?<word>[a-zA-Z]+)");

  private final String QUOTED_DOT = Pattern.quote(".");

  private final Set<Item.Type> NUMBER_TYPES = Set.of(Item.Type.LONG, Item.Type.BIGINTEGER);

  public final void parseSemVerVersion(String version, Item.ComparisonMode mode) {
    this.value = version;
    this.mode = mode;
    version = version.toLowerCase(Locale.ENGLISH);

    var plusIndex = version.indexOf("+");
    if (plusIndex != -1) {
      version = version.substring(0, plusIndex);
    }

    items = new ListItem(mode);

    ListItem list = items;

    String[] versionParts = version.split(QUOTED_DOT);
    for (String part : versionParts) {
      var matcher = ITEM_PATTERN.matcher(part);
      // TODO: can we use matcher state to track matcherIdx instead?
      for (int matcherIdx = 0; matcher.find(); matcherIdx++) {
        if (matcherIdx != 0) {
          list.add(list = new ListItem(mode));
        }

        var number = matcher.group("number");
        if (number != null) {
          list.add(parseItem(false, true, number, mode));
          continue;
        }

        if (items.size() < 3) {
          fillMissingMajorMinorPath();
        }

        var qualifierName = matcher.group("qualifiername");
        var qualifierVersion = matcher.group("qualifierversion");
        if (qualifierName != null && qualifierVersion != null) {
          list.add(StringItem.newStringItem(qualifierName, true, mode));
          list.add(list = new ListItem(mode));
          list.add(parseItem(false, true, qualifierVersion, mode));
          continue;
        }

        var word = matcher.group("word");
        if (word != null) {
          list.add(parseItem(false, false, word, mode));
          continue;
        }
      }
    }

    // 2.0 => 2.0.0
    fillMissingMajorMinorPath();

    // 2.0.0.0 => 2.0.0
    removeExtraTrailingZeros(items);
  }

  // Make sure we always start with 3 LongItems
  private void fillMissingMajorMinorPath() {
    for (int i = 3 - items.size(); i > 0; i--) {
      items.add(new LongItem(0L));
    }
  }

  private boolean removeExtraTrailingZeros(ListItem items) {
    while (items.size() > 3) {
      var lastItem = items.get(items.size() - 1);
      if (lastItem.isNull()) {
        items.remove(items.size() - 1);
        continue;
      } else if (Item.Type.LIST.equals(lastItem.getType())) {
        var listItem = (ListItem) lastItem;
        if (removeExtraTrailingZeros(listItem)) {
          continue;
        }
      }
      break;
    }

    return items.isEmpty();
  }

  static Item parseItem(
      boolean isCombination, boolean isDigit, String buf, Item.ComparisonMode mode) {
    if (isCombination) {
      return new CombinationItem(buf.replace("-", ""), mode);
    } else if (isDigit) {
      buf = stripLeadingZeroes(buf);
      if (buf.length() <= MAX_LONGITEM_LENGTH) {
        // lower than 2^63
        return new LongItem(Long.parseLong(buf));
      }
      return new BigIntegerItem(new BigInteger(buf));
    }
    return StringItem.newStringItem(buf, false, mode);
  }

  private static String stripLeadingZeroes(String buf) {
    if (buf == null || buf.isEmpty()) {
      return "0";
    }
    for (int i = 0; i < buf.length(); ++i) {
      char c = buf.charAt(i);
      if (c != '0') {
        return buf.substring(i);
      }
    }
    return buf;
  }

  @Override
  public int compareTo(SmartVersion o) {
    if (!mode.equals(o.mode)) {
      throw new IllegalArgumentException("Cannot compare versions from different modes");
    }
    return items.compareTo(o.items);
  }

  public boolean lessThan(SmartVersion versionToCompare) {
    return compareTo(versionToCompare) < 0;
  }

  public boolean greaterThan(SmartVersion versionToCompare) {
    return compareTo(versionToCompare) > 0;
  }

  public boolean equalTo(SmartVersion versionToCompare) {
    return compareTo(versionToCompare) == 0;
  }

  public String getOriginal() {
    return value;
  }

  @Override
  public String toString() {
    return getOriginal();
  }

  public String getCanonical() {
    if (canonical == null) {
      canonical = items.toString();
    }
    return canonical;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof SmartVersion) && equalTo(((SmartVersion) o));
  }

  @Override
  public int hashCode() {
    return items.hashCode();
  }
}
