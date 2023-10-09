package xyz.rtfpessoa.smartversion;

import org.jetbrains.annotations.Nullable;

public interface Item extends Comparable<Item> {

  enum Type {
    LONG,
    BIGINTEGER,
    STRING,
    LIST,
    COMBINATION
  }

  int compareTo(@Nullable Item item);

  Type getType();

  boolean isNull();

  enum ComparisonMode {
    // All versions with pre-release information are considered before the final release,
    // but it follows maven ordering, alias and short names for the qualifiers.
    MIXED,
    // Similar to ComparableVersion, certain pre-release qualifiers are considered before the final
    // release while the rest is considered after the release.
    MAVEN,
    // All versions with pre-release information are considered before the final release,
    // and it always follows alphabetical order for the qualifiers
    SEMVER
  }
}
