package com.palominolabs.http.url

import org.junit.Before
import org.junit.Test

import static java.nio.charset.StandardCharsets.UTF_8
import static org.junit.Assert.assertEquals

class PercentDecoderTest {
  private PercentDecoder decoder

  @Before
  public void setUp() {
    decoder = new PercentDecoder(UTF_8)
  }

  @Test
  public void testDecodesWithoutPercents() {
    assert 'asdf' == decoder.percentDecode('asdf')
  }

  @Test
  public void testDecodeSingleByte() {
    assert '#' == decoder.percentDecode('%23')
  }

  @Test
  public void testRandomStrings() {
    PercentEncoder encoder = UrlPercentEncoders.getQueryEncoder()
    Random rand = new Random()
    char[] charBuf = new char[2]
    List<Integer> codePoints = []
    StringBuilder orig = new StringBuilder()

    100.times {
      orig.setLength(0)
      codePoints.clear()

      while (orig.length() < 10) {
        // pick something in the range of all 17 unicode planes
        int codePoint = rand.nextInt(17 * 65536)
        if (Character.isDefined(codePoint)) {
          codePoints.add codePoint
          int res = Character.toChars(codePoint, charBuf, 0)
          // whether it's a pair or not, we want the only char (or high surrogate)
          orig.append(charBuf[0])
          if (res == 2) {
            // it's a surrogate pair, so we care about the second char
            orig.append(charBuf[1])
          }
        }
      }

      assertEquals("Code points: " + codePoints, orig.toString(),
          decoder.percentDecode(encoder.encode(orig.toString())))
    }
  }
}
