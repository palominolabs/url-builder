package com.palominolabs.http.url

import org.junit.Before
import org.junit.Test

import static java.nio.charset.StandardCharsets.UTF_8
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
  public void testRandomStrings() {
    PercentEncoder encoder = UrlPercentEncoders.getQueryEncoder()
    Random rand = new Random(44)
    char[] charBuf = new char[2]
    List<Integer> codePoints = []
    StringBuilder orig = new StringBuilder()

    10000.times {
      orig.setLength(0)
      codePoints.clear()

      while (orig.length() < 1 + rand.nextInt(1000)) {
        // pick something in the range of all 17 unicode planes
        int codePoint = rand.nextInt(17 * 65536)
        if (Character.isDefined(codePoint)) {
          int res = Character.toChars(codePoint, charBuf, 0)

          if (res == CODE_POINT_IN_BMP && Character.isSurrogate(charBuf[0])) {
            // isDefined is true even if it's a standalone surrogate in the D800-DFFF range, but those are not legal
            // single unicode code units (that is, a single char)
            continue;
          }

          orig.append(charBuf[0])
          // whether it's a pair or not, we want the only char (or high surrogate)
          codePoints.add codePoint
          if (res == CODE_POINT_IN_SUPPLEMENTARY) {
            // it's a surrogate pair, so we care about the second char
            orig.append(charBuf[1])
          }
        }
      }


      byte[] origBytes = orig.toString().getBytes(UTF_8)
      byte[] decodedBytes
      def codePointsHex = codePoints.collect({ int i -> Integer.toHexString(i) })

      try {
        decodedBytes = decoder.decode(encoder.encode(orig.toString())).getBytes(UTF_8)
      } catch (IllegalArgumentException e) {
        List<String> charHex = new ArrayList<String>();
        for (int i = 0; i < orig.toString().length(); i++) {
          charHex.add(Integer.toHexString((int) orig.toString().charAt(i)));
        }
        fail("code points: " + codePoints + codePointsHex + ' chars ' + charHex + ' ' + e.message)
      }

      assertEquals("Code points: " + codePointsHex, toHex(origBytes),
          toHex(decodedBytes))
    }
  }

  static List<Integer> toHex(byte[] bytes) {
    def list = []

    for (byte b in bytes) {
      list.add Integer.toHexString((int) b & 0xFF)
    }

    return list
  }
}
