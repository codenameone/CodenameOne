import java.io.UnsupportedEncodingException;

public class Utf8PerfApp {
    private static final int PAYLOAD_BYTES = 8192;
    private static final int ITERATIONS = 4000;

    public static void main(String[] args) throws Exception {
        // ASCII payload: stresses the NEON-accelerated ASCII prefix scan and
        // u8 -> u16 widen on ParparVM. JavaSE uses its own native decoder so
        // absolute timings differ, but RESULT signatures must match.
        byte[] asciiPayload = buildAsciiPayload();
        // Mixed payload: ASCII filler with 2/3/4-byte UTF-8 sequences sprinkled
        // in so decoding falls through to the DFA tail after the ASCII prefix.
        byte[] mixedPayload = buildMixedPayload();

        for (int i = 0; i < 40; i++) {
            warmup(asciiPayload);
            warmup(mixedPayload);
        }

        long asciiDecodeStart = System.currentTimeMillis();
        String asciiDecoded = null;
        for (int i = 0; i < ITERATIONS; i++) {
            asciiDecoded = new String(asciiPayload, 0, asciiPayload.length, "UTF-8");
        }
        long asciiDecodeMs = System.currentTimeMillis() - asciiDecodeStart;

        long asciiEncodeStart = System.currentTimeMillis();
        byte[] asciiReEncoded = null;
        for (int i = 0; i < ITERATIONS; i++) {
            asciiReEncoded = asciiDecoded.getBytes("UTF-8");
        }
        long asciiEncodeMs = System.currentTimeMillis() - asciiEncodeStart;

        long mixedDecodeStart = System.currentTimeMillis();
        String mixedDecoded = null;
        for (int i = 0; i < ITERATIONS; i++) {
            mixedDecoded = new String(mixedPayload, 0, mixedPayload.length, "UTF-8");
        }
        long mixedDecodeMs = System.currentTimeMillis() - mixedDecodeStart;

        long mixedEncodeStart = System.currentTimeMillis();
        byte[] mixedReEncoded = null;
        for (int i = 0; i < ITERATIONS; i++) {
            mixedReEncoded = mixedDecoded.getBytes("UTF-8");
        }
        long mixedEncodeMs = System.currentTimeMillis() - mixedEncodeStart;

        int asciiDecodedChk = checksum(asciiDecoded);
        int asciiReEncodedChk = checksum(asciiReEncoded);
        int mixedDecodedChk = checksum(mixedDecoded);
        int mixedReEncodedChk = checksum(mixedReEncoded);
        // Malformed input: a lone continuation byte, an over-long sequence,
        // and a truncated 3-byte lead. The JDK encoder emits one U+FFFD per
        // maximal subpart; the iOS decoder must agree byte-for-byte.
        byte[] malformed = new byte[] {
                (byte) 'a', (byte) 0x80, (byte) 'b',
                (byte) 0xC0, (byte) 0x80,
                (byte) 'c', (byte) 0xE2, (byte) 0x82,
        };
        String malformedDecoded = new String(malformed, 0, malformed.length, "UTF-8");
        int malformedChk = checksum(malformedDecoded);
        int signature = asciiDecodedChk
                ^ (asciiReEncodedChk * 7)
                ^ (mixedDecodedChk * 13)
                ^ (mixedReEncodedChk * 17)
                ^ (malformedChk * 23);

        System.out.println("RESULT=" + signature);
        System.out.println("ASCII_DECODE_MS=" + asciiDecodeMs);
        System.out.println("ASCII_ENCODE_MS=" + asciiEncodeMs);
        System.out.println("MIXED_DECODE_MS=" + mixedDecodeMs);
        System.out.println("MIXED_ENCODE_MS=" + mixedEncodeMs);
        System.out.println("ASCII_BYTES=" + asciiPayload.length);
        System.out.println("MIXED_BYTES=" + mixedPayload.length);
        System.out.println("MIXED_CHARS=" + mixedDecoded.length());
    }

    private static void warmup(byte[] bytes) throws UnsupportedEncodingException {
        String s = new String(bytes, 0, bytes.length, "UTF-8");
        s.getBytes("UTF-8");
    }

    private static byte[] buildAsciiPayload() {
        byte[] out = new byte[PAYLOAD_BYTES];
        // A short repeating pattern that resembles JSON/HTML keywords, so
        // compilers cannot collapse the loop into a memset.
        String seed = "{\"key\":\"value\",\"id\":12345,\"flag\":true,\"text\":\"lorem ipsum dolor sit amet\"}";
        byte[] s = seed.getBytes();
        for (int i = 0; i < out.length; i++) {
            out[i] = s[i % s.length];
        }
        return out;
    }

    private static byte[] buildMixedPayload() {
        // Build the byte stream directly so the boundary between the ASCII
        // prefix and the multi-byte sequences is well-defined and no UTF-8
        // sequence is ever truncated at the payload boundary.
        byte[] out = new byte[PAYLOAD_BYTES];
        // "the quick brown fox jumps " (26 ASCII bytes per chunk).
        String asciiChunk = "the quick brown fox jumps ";
        byte[] asciiBytes = asciiChunk.getBytes();
        // U+00E9 (Latin small e with acute) -> 0xC3 0xA9
        byte[] twoByte = new byte[] { (byte) 0xC3, (byte) 0xA9 };
        // U+20AC (euro sign) -> 0xE2 0x82 0xAC
        byte[] threeByte = new byte[] { (byte) 0xE2, (byte) 0x82, (byte) 0xAC };
        // U+1F600 (grinning face) -> 0xF0 0x9F 0x98 0x80
        byte[] fourByte = new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80 };
        int pos = 0;
        int rotation = 0;
        while (pos < PAYLOAD_BYTES) {
            int chunkLen = Math.min(asciiBytes.length, PAYLOAD_BYTES - pos);
            System.arraycopy(asciiBytes, 0, out, pos, chunkLen);
            pos += chunkLen;
            if (pos >= PAYLOAD_BYTES) break;
            byte[] mb;
            switch (rotation % 3) {
                case 0:  mb = twoByte;   break;
                case 1:  mb = threeByte; break;
                default: mb = fourByte;  break;
            }
            if (pos + mb.length > PAYLOAD_BYTES) {
                // Pad the tail with ASCII so we never truncate a UTF-8 seq.
                while (pos < PAYLOAD_BYTES) {
                    out[pos++] = (byte) 'X';
                }
                break;
            }
            System.arraycopy(mb, 0, out, pos, mb.length);
            pos += mb.length;
            rotation++;
        }
        return out;
    }

    private static int checksum(String s) {
        int result = 0;
        for (int i = 0; i < s.length(); i++) {
            result = result * 31 + s.charAt(i);
        }
        return result;
    }

    private static int checksum(byte[] data) {
        int result = 0;
        for (int i = 0; i < data.length; i++) {
            result = result * 31 + (data[i] & 0xff);
        }
        return result;
    }
}
