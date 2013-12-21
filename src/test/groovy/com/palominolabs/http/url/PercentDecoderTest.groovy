package com.palominolabs.http.url

import groovy.transform.CompileStatic
import org.junit.Before
import org.junit.Test

import static com.google.common.base.Charsets.UTF_8
import static java.lang.Character.isHighSurrogate
import static java.lang.Character.isLowSurrogate
import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail

class PercentDecoderTest {
  static final int CODE_POINT_IN_SUPPLEMENTARY = 2
  static final int CODE_POINT_IN_BMP = 1
  PercentDecoder decoder

  @Before
  public void setUp() {
    decoder = new PercentDecoder(UTF_8.newDecoder())
  }

  @Test
  public void testDecodesWithoutPercents() {
    assert 'asdf' == decoder.decode('asdf')
  }

  @Test
  public void testDecodeSingleByte() {
    assert '#' == decoder.decode('%23')
  }

  @Test
  public void testIncompletePercentPairNoNumbers() {
    try {
      decoder.decode('%')
      fail()
    } catch (IllegalArgumentException e) {
      assert 'Could not percent decode <%>: incomplete %-pair at position 0' == e.message
    }
  }

  @Test
  public void testIncompletePercentPairOneNumber() {
    try {
      decoder.decode('%2')
      fail()
    } catch (IllegalArgumentException e) {
      assert 'Could not percent decode <%2>: incomplete %-pair at position 0' == e.message
    }
  }

  @Test
  public void testInvalidHex() {
    try {
      decoder.decode('%xz')
      fail()
    } catch (IllegalArgumentException e) {
      assert 'Invalid %-tuple <%xz>' == e.message
    }
  }

  @Test
  @CompileStatic
  public void testRandomStrings() {
    PercentEncoder encoder = UrlPercentEncoders.getQueryEncoder()
    Random rand = new Random()

    def seed = rand.nextLong()
    rand.setSeed(seed)

    char[] charBuf = new char[2]
    List<Integer> codePoints = []
    StringBuilder buf = new StringBuilder()

    10000.times {
      buf.setLength(0)
      codePoints.clear()

      randString(buf, codePoints, charBuf, rand, 1 + rand.nextInt(1000))

      byte[] origBytes = buf.toString().getBytes(UTF_8)
      byte[] decodedBytes
      def codePointsHex = codePoints.collect({ int i -> Integer.toHexString(i) })

      try {
        decodedBytes = decoder.decode(encoder.encode(buf.toString())).getBytes(UTF_8)
      } catch (IllegalArgumentException e) {
        List<String> charHex = new ArrayList<String>();
        for (int i = 0; i < buf.toString().length(); i++) {
          charHex.add(Integer.toHexString((int) buf.toString().charAt(i)));
        }
        fail("seed: $seed code points: $codePointsHex chars $charHex $e.message")
      }

      assertEquals("Seed: $seed Code points: $codePointsHex", toHex(origBytes),
          toHex(decodedBytes))
    }
  }

  /**
   * Generate a random string
   * @param buf buffer to write into
   * @param codePoints list of code points to write into
   * @param charBuf char buf for temporary char wrangling (size 2)
   * @param rand random source
   * @param maxStrLength max string length
   */
  @CompileStatic
  private static void randString(StringBuilder buf, List<Integer> codePoints, char[] charBuf, Random rand,
                                 int length) {
    while (buf.length() < length) {
      // pick something in the range of all 17 unicode planes
      int codePoint = rand.nextInt(17 * 65536)
      if (Character.isDefined(codePoint)) {
        int res = Character.toChars(codePoint, charBuf, 0)

        if (res == CODE_POINT_IN_BMP && (isHighSurrogate(charBuf[0]) || isLowSurrogate(charBuf[0]))) {
          // isDefined is true even if it's a standalone surrogate in the D800-DFFF range, but those are not legal
          // single unicode code units (that is, a single char)
          continue;
        }

        buf.append(charBuf[0])
        // whether it's a pair or not, we want the only char (or high surrogate)
        codePoints.add codePoint
        if (res == CODE_POINT_IN_SUPPLEMENTARY) {
          // it's a surrogate pair, so we care about the second char
          buf.append(charBuf[1])
        }
      }
    }
  }

  /**
   *
   * @param bytes
   * @return list of hex strings
   */
  @CompileStatic
  static List<String> toHex(byte[] bytes) {
    def list = []

    for (byte b in bytes) {
      list.add Integer.toHexString((int) b & 0xFF)
    }

    return list
  }
}
