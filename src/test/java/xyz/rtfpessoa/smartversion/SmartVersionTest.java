package xyz.rtfpessoa.smartversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import xyz.rtfpessoa.smartversion.Item.ComparisonMode;

@SuppressWarnings("unchecked")
class SmartVersionTest {
  private SmartVersion newComparable(String version) {
    SmartVersion ret = new SmartVersion(version);
    String canonical = ret.getCanonical();
    String parsedCanonical = new SmartVersion(canonical).getCanonical();

    assertEquals(
        canonical,
        parsedCanonical,
        "canonical( " + version + " ) = " + canonical + " -> canonical: " + parsedCanonical);

    return ret;
  }

  private static final String[] VERSIONS_QUALIFIER = {
    "1-alpha2snapshot",
    "1-alpha2",
    "1-alpha-123",
    "1-beta-2",
    "1-beta123",
    "1-m2",
    "1-m11",
    "1-rc",
    "1-cr2",
    "1-rc123",
    "1-SNAPSHOT",
    "1",
    "1-sp",
    "1-sp2",
    "1-sp123",
    "1-abc",
    "1-def",
    "1-pom-1",
    "1-1-snapshot",
    "1-1",
    "1-2",
    "1-123"
  };

  private static final String[] VERSIONS_NUMBER = {
    "2.0", "2.0.a", "2-1", "2.0.2", "2.0.123", "2.1.0", "2.1-a", "2.1b", "2.1-c", "2.1-1",
    "2.1.0.1", "2.2", "2.123", "11.a2", "11.a11", "11.b2", "11.b11", "11.m2", "11.m11", "11",
    "11.a", "11b", "11c", "11m"
  };

  private void checkVersionsOrder(String[] versions) {
    var c = new Comparable[versions.length];
    for (int i = 0; i < versions.length; i++) {
      c[i] = newComparable(versions[i]);
    }

    for (int i = 1; i < versions.length; i++) {
      var low = c[i - 1];
      for (int j = i; j < versions.length; j++) {
        var high = c[j];
        assertTrue(low.compareTo(high) < 0, "expected " + low + " < " + high);
        assertTrue(high.compareTo(low) > 0, "expected " + high + " > " + low);
      }
    }
  }

  private void checkVersionsEqual(String v1, String v2) {
    var c1 = newComparable(v1);
    var c2 = newComparable(v2);
    assertEquals(0, c1.compareTo(c2), "expected " + v1 + " == " + v2);
    assertEquals(0, c2.compareTo(c1), "expected " + v2 + " == " + v1);
    assertEquals(c1.hashCode(), c2.hashCode(), "expected same hashcode for " + v1 + " and " + v2);
    assertEquals(c1, c2, "expected " + v1 + ".equals( " + v2 + " )");
    assertEquals(c2, c1, "expected " + v2 + ".equals( " + v1 + " )");
  }

  private void checkVersionsHaveSameOrder(String v1, String v2) {
    SmartVersion c1 = new SmartVersion(v1);
    SmartVersion c2 = new SmartVersion(v2);
    assertEquals(0, c1.compareTo(c2), "expected " + v1 + " == " + v2);
    assertEquals(0, c2.compareTo(c1), "expected " + v2 + " == " + v1);
  }

  private void checkVersionsArrayEqual(String[] array) {
    // compare against each other (including itself)
    for (int i = 0; i < array.length; ++i)
      for (int j = i; j < array.length; ++j) checkVersionsEqual(array[i], array[j]);
  }

  private void checkVersionsOrder(String v1, String v2) {
    var c1 = newComparable(v1);
    var c2 = newComparable(v2);
    assertTrue(c1.compareTo(c2) < 0, "expected " + v1 + " < " + v2);
    assertTrue(c2.compareTo(c1) > 0, "expected " + v2 + " > " + v1);
  }

  @Test
  void testVersionsQualifier() {
    checkVersionsOrder(VERSIONS_QUALIFIER);
  }

  @Test
  void testVersionsNumber() {
    checkVersionsOrder(VERSIONS_NUMBER);
  }

  @Test
  void testVersionsEqual() {
    newComparable("1.0-alpha");
    checkVersionsEqual("1", "1");
    checkVersionsEqual("1", "1.0");
    checkVersionsEqual("1", "1.0.0");
    checkVersionsEqual("1.0", "1.0.0");
    checkVersionsEqual("1", "1-0");
    checkVersionsEqual("1", "1.0-0");
    checkVersionsEqual("1.0", "1.0-0");
    // no separator between number and character
    checkVersionsEqual("1a", "1-a");
    checkVersionsEqual("1a", "1.0-a");
    checkVersionsEqual("1a", "1.0.0-a");
    checkVersionsEqual("1.0a", "1-a");
    checkVersionsEqual("1.0.0a", "1-a");
    checkVersionsEqual("1x", "1-x");
    checkVersionsEqual("1x", "1.0-x");
    checkVersionsEqual("1x", "1.0.0-x");
    checkVersionsEqual("1.0x", "1-x");
    checkVersionsEqual("1.0.0x", "1-x");
    checkVersionsEqual("1cr", "1rc");

    // special "aliases" a, b and m for alpha, beta and milestone
    checkVersionsEqual("1a1", "1-alpha-1");
    checkVersionsEqual("1b2", "1-beta-2");
    checkVersionsEqual("1m3", "1-milestone-3");

    // case insensitive
    checkVersionsEqual("1X", "1x");
    checkVersionsEqual("1A", "1a");
    checkVersionsEqual("1B", "1b");
    checkVersionsEqual("1M", "1m");
    checkVersionsEqual("1Cr", "1Rc");
    checkVersionsEqual("1cR", "1rC");
    checkVersionsEqual("1m3", "1Milestone3");
    checkVersionsEqual("1m3", "1MileStone3");
    checkVersionsEqual("1m3", "1MILESTONE3");
  }

  @Test
  void testVersionsHaveSameOrderButAreNotEqual() {
    checkVersionsHaveSameOrder("1ga", "1");
    checkVersionsHaveSameOrder("1release", "1");
    checkVersionsHaveSameOrder("1final", "1");
    checkVersionsHaveSameOrder("1Ga", "1");
    checkVersionsHaveSameOrder("1GA", "1");
    checkVersionsHaveSameOrder("1RELEASE", "1");
    checkVersionsHaveSameOrder("1release", "1");
    checkVersionsHaveSameOrder("1RELeaSE", "1");
    checkVersionsHaveSameOrder("1Final", "1");
    checkVersionsHaveSameOrder("1FinaL", "1");
    checkVersionsHaveSameOrder("1FINAL", "1");
  }

  @Test
  void testVersionComparing() {
    checkVersionsOrder("1", "2");
    checkVersionsOrder("1.5", "2");
    checkVersionsOrder("1", "2.5");
    checkVersionsOrder("1.0", "1.1");
    checkVersionsOrder("1.1", "1.2");
    checkVersionsOrder("1.0.0", "1.1");
    checkVersionsOrder("1.0.1", "1.1");
    checkVersionsOrder("1.1", "1.2.0");

    checkVersionsOrder("1.0-alpha-1", "1.0");
    checkVersionsOrder("1.0-alpha-1", "1.0-alpha-2");
    checkVersionsOrder("1.0-alpha-1", "1.0-beta-1");

    checkVersionsOrder("1.0-beta-1", "1.0-SNAPSHOT");
    checkVersionsOrder("1.0-SNAPSHOT", "1.0");
    checkVersionsOrder("1.0-alpha-1-SNAPSHOT", "1.0-alpha-1");

    checkVersionsOrder("1.0", "1.0-1");
    checkVersionsOrder("1.0-1", "1.0-2");
    checkVersionsOrder("1.0.0", "1.0-1");

    checkVersionsOrder("2.0-1", "2.0.1");
    checkVersionsOrder("2.0.1-klm", "2.0.1-lmn");
    checkVersionsOrder("2.0.1", "2.0.1-xyz");

    checkVersionsOrder("2.0.1", "2.0.1-123");
    checkVersionsOrder("2.0.1-xyz", "2.0.1-123");
  }

  @Test
  void testLeadingZeroes() {
    checkVersionsOrder("0.7", "2");
    checkVersionsOrder("0.2", "1.0.7");
  }

  @Test
  void testGetOriginal() {
    SmartVersion version = new SmartVersion("0.x");
    assertEquals("0.x", version.getOriginal());
    SmartVersion version2 = new SmartVersion("0.2");
    assertEquals("0.2", version2.getOriginal());
  }

  @Test
  void testGetCanonical() {
    // MNG-7700
    newComparable("0.x");
    newComparable("0-x");
    newComparable("0.rc");
    newComparable("0-1");

    SmartVersion version = new SmartVersion("0.x");
    assertEquals("x", version.getCanonical());
    SmartVersion version2 = new SmartVersion("0.2");
    assertEquals("0.2", version2.getCanonical());
  }

  /**
   * Test <a href="https://issues.apache.org/jira/browse/MNG-5568">MNG-5568</a> edge case which was
   * showing transitive inconsistency: since A &gt; B and B &gt; C then we should have A &gt; C
   * otherwise sorting a list of SmartVersions() will in some cases throw runtime exception; see
   * Netbeans issues <a href="https://netbeans.org/bugzilla/show_bug.cgi?id=240845">240845</a> and
   * <a href="https://netbeans.org/bugzilla/show_bug.cgi?id=226100">226100</a>
   */
  @Test
  void testMng5568() {
    String a = "6.1.0";
    String b = "6.1.0rc3";
    String c = "6.1H.5-beta"; // this is the unusual version string, with 'H' in the middle

    checkVersionsOrder(b, a); // classical
    checkVersionsOrder(b, c); // now b < c, but before MNG-5568, we had b > c
    checkVersionsOrder(a, c);
  }

  /** Test <a href="https://jira.apache.org/jira/browse/MNG-6572">MNG-6572</a> optimization. */
  @Test
  void testMng6572() {
    String a = "20190126.230843"; // resembles a SNAPSHOT
    String b = "1234567890.12345"; // 10 digit number
    String c = "123456789012345.1H.5-beta"; // 15 digit number
    String d = "12345678901234567890.1H.5-beta"; // 20 digit number

    checkVersionsOrder(a, b);
    checkVersionsOrder(b, c);
    checkVersionsOrder(a, c);
    checkVersionsOrder(c, d);
    checkVersionsOrder(b, d);
    checkVersionsOrder(a, d);
  }

  /**
   * Test all versions are equal when starting with many leading zeroes regardless of string length
   * (related to MNG-6572 optimization)
   */
  @Test
  void testVersionEqualWithLeadingZeroes() {
    // versions with string lengths from 1 to 19
    String[] arr =
        new String[] {
          "0000000000000000001",
          "000000000000000001",
          "00000000000000001",
          "0000000000000001",
          "000000000000001",
          "00000000000001",
          "0000000000001",
          "000000000001",
          "00000000001",
          "0000000001",
          "000000001",
          "00000001",
          "0000001",
          "000001",
          "00001",
          "0001",
          "001",
          "01",
          "1"
        };

    checkVersionsArrayEqual(arr);
  }

  /**
   * Test all "0" versions are equal when starting with many leading zeroes regardless of string
   * length (related to MNG-6572 optimization)
   */
  @Test
  void testVersionZeroEqualWithLeadingZeroes() {
    // versions with string lengths from 1 to 19
    String[] arr =
        new String[] {
          "0000000000000000000",
          "000000000000000000",
          "00000000000000000",
          "0000000000000000",
          "000000000000000",
          "00000000000000",
          "0000000000000",
          "000000000000",
          "00000000000",
          "0000000000",
          "000000000",
          "00000000",
          "0000000",
          "000000",
          "00000",
          "0000",
          "000",
          "00",
          "0"
        };

    checkVersionsArrayEqual(arr);
  }

  /**
   * Test <a href="https://issues.apache.org/jira/browse/MNG-6964">MNG-6964</a> edge cases for
   * qualifiers that start with "-0.", which was showing A == C and B == C but A &lt; B.
   */
  @Test
  void testMng6964() {
    String a = "1-0.alpha";
    String b = "1-0.beta";
    String c = "1";

    checkVersionsOrder(a, c); // Now a < c, but before MNG-6964 they were equal
    checkVersionsOrder(b, c); // Now b < c, but before MNG-6964 they were equal
    checkVersionsOrder(a, b); // Should still be true
  }

  @Test
  void testLocaleIndependent() {
    Locale orig = Locale.getDefault();
    Locale[] locales = {Locale.ENGLISH, new Locale("tr"), Locale.getDefault()};
    try {
      for (Locale locale : locales) {
        Locale.setDefault(locale);
        checkVersionsEqual("1-abcdefghijklmnopqrstuvwxyz", "1-ABCDEFGHIJKLMNOPQRSTUVWXYZ");
      }
    } finally {
      Locale.setDefault(orig);
    }
  }

  @Test
  void testReuse() {
    SmartVersion c1 = new SmartVersion("1");
    c1.parseVersion("2");

    Comparable<?> c2 = newComparable("2");

    assertEquals(c1, c2, "reused instance should be equivalent to new instance");
  }

  /**
   * Test <a href="https://issues.apache.org/jira/browse/MNG-7644">MNG-7644</a> edge cases 1.0.0.RC1
   * &lt; 1.0.0-RC2 and more generally: 1.0.0.X1 &lt; 1.0.0-X2 for any string X
   */
  @Test
  void testMng7644() {
    for (String x :
        new String[] {"abc", "alpha", "a", "beta", "b", "def", "milestone", "m", "RC"}) {
      // 1.0.0.X1 < 1.0.0-X2 for any string x
      checkVersionsOrder("1.0.0." + x + "1", "1.0.0-" + x + "2");
      // 2.0.X == 2-X == 2.0.0.X for any string x
      checkVersionsEqual("2-" + x, "2.0." + x); // previously ordered, now equals
      checkVersionsEqual("2-" + x, "2.0.0." + x); // previously ordered, now equals
      checkVersionsEqual("2.0." + x, "2.0.0." + x); // previously ordered, now equals
    }
  }

  @Test
  public void testMng7714() {
    SmartVersion f = new SmartVersion("1.0.final-redhat");
    SmartVersion sp1 = new SmartVersion("1.0-sp1-redhat");
    SmartVersion sp2 = new SmartVersion("1.0-sp-1-redhat");
    SmartVersion sp3 = new SmartVersion("1.0-sp.1-redhat");
    assertTrue(f.compareTo(sp1) < 0, "expected " + f + " < " + sp1);
    assertTrue(f.compareTo(sp1) < 0, "expected " + f + " < " + sp2);
    assertTrue(f.compareTo(sp1) < 0, "expected " + f + " < " + sp3);
  }

  @Test
  public void testSemVerMode() {
    SmartVersion v1 = new SmartVersion("1.0.0.foo", ComparisonMode.SEMVER);
    SmartVersion v2 = new SmartVersion("1.0.0-foo", ComparisonMode.SEMVER);
    SmartVersion v3 = new SmartVersion("1.0.0-bar", ComparisonMode.SEMVER);

    assertTrue(v1.compareTo(v2) > 0, "expected " + v1 + " > " + v2);
    assertTrue(v2.compareTo(v3) == "foo".compareTo("bar"), "expected " + v2 + " > " + v3);
  }

  @Test
  public void testMixedMode() {
    SmartVersion v1 = new SmartVersion("1.0.0.alpha2", ComparisonMode.MIXED);
    SmartVersion v2 = new SmartVersion("1.0.0-alpha2", ComparisonMode.MIXED);
    SmartVersion v3 = new SmartVersion("1.0.0-beta1", ComparisonMode.MIXED);
    SmartVersion v4 = new SmartVersion("1.0.0-foo1", ComparisonMode.MIXED);
    SmartVersion v5 = new SmartVersion("1.0.0", ComparisonMode.MIXED);

    assertTrue(v1.compareTo(v2) > 0, "expected " + v1 + " > " + v2);
    assertTrue(v2.compareTo(v3) < 0, "expected " + v2 + " < " + v3);
    assertTrue(v2.compareTo(v4) < 0, "expected " + v2 + " < " + v4);
    assertTrue(v2.compareTo(v5) < 0, "expected " + v2 + " < " + v5);
    assertTrue(v3.compareTo(v4) < 0, "expected " + v3 + " < " + v4);
    assertTrue(v3.compareTo(v5) < 0, "expected " + v3 + " < " + v5);
    assertTrue(v4.compareTo(v5) < 0, "expected " + v4 + " < " + v5);
  }

  @Test
  public void testMavenMode() {
    SmartVersion v1 = new SmartVersion("1.0.0.alpha2", ComparisonMode.MAVEN);
    SmartVersion v2 = new SmartVersion("1.0.0-alpha2", ComparisonMode.MAVEN);
    SmartVersion v3 = new SmartVersion("1.0.0-beta1", ComparisonMode.MAVEN);
    SmartVersion v4 = new SmartVersion("1.0.0-foo1", ComparisonMode.MAVEN);
    SmartVersion v5 = new SmartVersion("1.0.0", ComparisonMode.MAVEN);

    assertTrue(v1.compareTo(v2) == 0, "expected " + v1 + " = " + v2);
    assertTrue(v2.compareTo(v3) < 0, "expected " + v2 + " < " + v3);
    assertTrue(v2.compareTo(v4) < 0, "expected " + v2 + " < " + v4);
    assertTrue(v2.compareTo(v5) < 0, "expected " + v2 + " < " + v5);
    assertTrue(v3.compareTo(v4) < 0, "expected " + v3 + " < " + v4);
    assertTrue(v3.compareTo(v5) < 0, "expected " + v3 + " < " + v5);
    assertTrue(v4.compareTo(v5) > 0, "expected " + v4 + " > " + v5);
  }

  @Test
  public void testFailToCompareDifferentModes() {
    SmartVersion v1 = new SmartVersion("1", ComparisonMode.MAVEN);
    SmartVersion v2 = new SmartVersion("2", ComparisonMode.MIXED);
    SmartVersion v3 = new SmartVersion("3", ComparisonMode.SEMVER);

    assertThrows(IllegalArgumentException.class, () -> v1.compareTo(v2));
    assertThrows(IllegalArgumentException.class, () -> v1.compareTo(v3));
    assertThrows(IllegalArgumentException.class, () -> v2.compareTo(v3));
  }

  @Test
  void testFollowsDot() {
    assertThat(List.of("0.0.0", "0.0.0.dev1", "0.0.0-pre-alpha-build-1"))
        .isSortedAccordingTo(Comparator.comparing(v -> new SmartVersion(v, ComparisonMode.MAVEN)));
    assertThat(List.of("0.0.0-pre-alpha-build-1", "0.0.0", "0.0.0.dev1"))
        .isSortedAccordingTo(Comparator.comparing(v -> new SmartVersion(v, ComparisonMode.MIXED)));
    assertThat(List.of("0.0.0-pre-alpha-build-1", "0.0.0", "0.0.0.dev1"))
        .isSortedAccordingTo(Comparator.comparing(v -> new SmartVersion(v, ComparisonMode.SEMVER)));
  }

  private static Stream<Arguments> semVerComparisonInput() {
    return Stream.of(
        Arguments.of("1.2.3", "1.5.1", -1),
        Arguments.of("2.2.3", "1.5.1", 1),
        Arguments.of("2.2.3", "2.2.2", 1),
        Arguments.of("3.2.0-beta", "3.2.0-beta", 0),
        Arguments.of("1.3", "1.1.4", 1),
        Arguments.of("4.2.0", "4.2.0-beta", 1),
        Arguments.of("4.2.0-beta", "4.2.0", -1),
        Arguments.of("4.2.0-alpha", "4.2.0-beta", -1),
        Arguments.of("4.2.0-alpha", "4.2.0-alpha", 0),
        Arguments.of("4.2.0-beta.2", "4.2.0-beta.1", 1),
        Arguments.of("4.2.0-beta2", "4.2.0-beta1", 1),
        Arguments.of("4.2.0-beta", "4.2.0-beta.2", -1),
        Arguments.of("4.2.0-beta", "4.2.0-beta.foo", -1),
        Arguments.of("4.2.0-beta.2", "4.2.0-beta", 1),
        Arguments.of("4.2.0-beta.foo", "4.2.0-beta", 1),
        Arguments.of("1.2.0+bar", "1.2.0+baz", 0),
        Arguments.of("1.0.0-beta.-2", "1.0.0-beta.-3", -1),
        // Different from SemVer
        // .4 > .-2
        Arguments.of("1.0.0-beta.4", "1.0.0-beta.-2", 1),
        // Different from SemVer
        // .-3 < .5
        Arguments.of("1.0.0-beta.-3", "1.0.0-beta.5", -1),
        Arguments.of("1.2.3-alpha1", "1.2.3-a1", 1),
        Arguments.of("1.2.3-alpha1", "1.2.3-beta1", -1),
        Arguments.of("1.2.3-beta1", "1.2.3-b1", 1),
        Arguments.of("1.2.3-beta1", "1.2.3-milestone1", -1),
        Arguments.of("1.2.3-milestone1", "1.2.3-m1", 1),
        Arguments.of("1.2.3-milestone1", "1.2.3-rc1", -1),
        Arguments.of("1.2.3-rc1", "1.2.3-cr1", 1),
        Arguments.of("1.2.3-rc1", "1.2.3-snapshot", -1),
        Arguments.of("1.2.3-snapshot", "1.2.3", -1),
        Arguments.of("1.2.3", "1.2.3-ga", 1),
        Arguments.of("1.2.3", "1.2.3-final", 1),
        Arguments.of("1.2.3", "1.2.3-sp", 1),
        Arguments.of("1.2.3-sp", "1.2.3-foo", 1));
  }

  @ParameterizedTest(name = "{0} comparison to {1} is {2}")
  @MethodSource("semVerComparisonInput")
  void testSemVerComparison(String version, String versionToCompare, int result) {
    var parsedVersion = new SmartVersion(version, ComparisonMode.SEMVER);
    var parsedVersionToCompare = new SmartVersion(versionToCompare, ComparisonMode.SEMVER);
    assertThat(parsedVersion.greaterThan(parsedVersionToCompare)).isEqualTo(result > 0);
    assertThat(parsedVersion.lessThan(parsedVersionToCompare)).isEqualTo(result < 0);
    assertThat(parsedVersion.equalTo(parsedVersionToCompare)).isEqualTo(result == 0);
  }

  private static Stream<Arguments> mixedComparisonInput() {
    return Stream.of(
        Arguments.of("1.2.3", "1.5.1", -1),
        Arguments.of("2.2.3", "1.5.1", 1),
        Arguments.of("2.2.3", "2.2.2", 1),
        Arguments.of("3.2.0-beta", "3.2.0-beta", 0),
        Arguments.of("1.3", "1.1.4", 1),
        Arguments.of("4.2.0", "4.2.0-beta", 1),
        Arguments.of("4.2.0-beta", "4.2.0", -1),
        Arguments.of("4.2.0-alpha", "4.2.0-beta", -1),
        Arguments.of("4.2.0-alpha", "4.2.0-alpha", 0),
        Arguments.of("4.2.0-beta.2", "4.2.0-beta.1", 1),
        Arguments.of("4.2.0-beta2", "4.2.0-beta1", 1),
        Arguments.of("4.2.0-beta", "4.2.0-beta.2", -1),
        Arguments.of("4.2.0-beta", "4.2.0-beta.foo", -1),
        Arguments.of("4.2.0-beta.2", "4.2.0-beta", 1),
        Arguments.of("4.2.0-beta.foo", "4.2.0-beta", 1),
        Arguments.of("1.2.0+bar", "1.2.0+baz", 0),
        Arguments.of("1.0.0-beta.-2", "1.0.0-beta.-3", -1),
        // Different from SemVer
        // .4 > .-2
        Arguments.of("1.0.0-beta.4", "1.0.0-beta.-2", 1),
        // Different from SemVer
        // .-3 < .5
        Arguments.of("1.0.0-beta.-3", "1.0.0-beta.5", -1),
        Arguments.of("1.2.3-alpha1", "1.2.3-a1", 0),
        Arguments.of("1.2.3-alpha1", "1.2.3-beta1", -1),
        Arguments.of("1.2.3-beta1", "1.2.3-b1", 0),
        Arguments.of("1.2.3-beta1", "1.2.3-milestone1", -1),
        Arguments.of("1.2.3-milestone1", "1.2.3-m1", 0),
        Arguments.of("1.2.3-milestone1", "1.2.3-rc1", -1),
        Arguments.of("1.2.3-rc1", "1.2.3-cr1", 0),
        Arguments.of("1.2.3-rc1", "1.2.3-snapshot", -1),
        Arguments.of("1.2.3-snapshot", "1.2.3", -1),
        Arguments.of("1.2.3", "1.2.3-ga", 1),
        Arguments.of("1.2.3", "1.2.3-final", 1),
        Arguments.of("1.2.3", "1.2.3-sp", 1),
        Arguments.of("1.2.3-sp", "1.2.3-foo", -1));
  }

  @ParameterizedTest(name = "{0} comparison to {1} is {2}")
  @MethodSource("mixedComparisonInput")
  void testMixedComparison(String version, String versionToCompare, int result) {
    var parsedVersion = new SmartVersion(version, ComparisonMode.MIXED);
    var parsedVersionToCompare = new SmartVersion(versionToCompare, ComparisonMode.MIXED);
    assertThat(parsedVersion.greaterThan(parsedVersionToCompare)).isEqualTo(result > 0);
    assertThat(parsedVersion.lessThan(parsedVersionToCompare)).isEqualTo(result < 0);
    assertThat(parsedVersion.equalTo(parsedVersionToCompare)).isEqualTo(result == 0);
  }

  @Test
  void testNonZeroComparisonWithNull() {
    // 2.0 < 2-1

    var mode = ComparisonMode.MAVEN;
    var v1 = new SmartVersion("2.0", mode);
    var v2 = new SmartVersion("2-1", mode);
    assertThat(v1.lessThan(v2)).isTrue();

    // 2-1 < 2.0

    mode = ComparisonMode.MIXED;
    v1 = new SmartVersion("2.0", mode);
    v2 = new SmartVersion("2-1", mode);
    assertThat(v1.greaterThan(v2)).isTrue();

    mode = ComparisonMode.SEMVER;
    v1 = new SmartVersion("2.0", mode);
    v2 = new SmartVersion("2-1", mode);
    assertThat(v1.greaterThan(v2)).isTrue();
  }

  @Test
  void testZerosWithLetter() {
    // 0.0.0 == 0.0.0.0 < 0.0.0.dev1
    var mode = ComparisonMode.MAVEN;
    var v1 = new SmartVersion("0.0.0", mode);
    var v2 = new SmartVersion("0.0.0.dev1", mode);
    var v3 = new SmartVersion("0.0.0.0", mode);
    assertThat(v1.lessThan(v2)).isTrue();
    assertThat(v1.equalTo(v3)).isTrue();
    assertThat(v2.greaterThan(v1)).isTrue();
    assertThat(v2.greaterThan(v3)).isTrue();
    assertThat(v3.equals(v1)).isTrue();
    assertThat(v3.lessThan(v2)).isTrue();
  }

  @Test
  void testMoreThan3ZerosEndingWithNonZero() {
    // 0.0.0rc0.dev1 < 0.0.0 < 0.0.0.0.2
    var mode = ComparisonMode.MAVEN;
    var v1 = new SmartVersion("0.0.0rc0.dev1", mode);
    var v2 = new SmartVersion("0.0.0", mode);
    var v3 = new SmartVersion("0.0.0.0.2", mode);
    assertThat(v1.lessThan(v2)).isTrue();
    assertThat(v1.lessThan(v3)).isTrue();
    assertThat(v2.greaterThan(v1)).isTrue();
    assertThat(v2.lessThan(v3)).isTrue();
    assertThat(v3.greaterThan(v1)).isTrue();
    assertThat(v3.greaterThan(v2)).isTrue();
  }

  @Test
  void foo() {
    var v1 = new SmartVersion("1.2.3", ComparisonMode.SEMVER);
    var v2 = new SmartVersion("1.5.1", ComparisonMode.SEMVER);
    System.out.println(v1.getCanonical());
    System.out.println(v2.getCanonical());
    assertThat(v1.compareTo(v2)).isLessThan(0);
  }
}
