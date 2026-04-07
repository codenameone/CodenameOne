import com.codename1.util.Base64;

public class Base64PerfApp {
    private static final int PAYLOAD_BYTES = 8192;
    private static final int ITERATIONS = 6000;

    public static void main(String[] args) throws Exception {
        byte[] payload = buildPayload();
        String encoded = Base64.encodeNoNewline(payload);
        byte[] encodedBytes = encoded.getBytes("UTF-8");

        // Warmup
        for (int i = 0; i < 50; i++) {
            Base64.encodeNoNewline(payload);
            Base64.decode(encodedBytes);
        }

        long encodeStart = System.currentTimeMillis();
        String sampleEncoded = null;
        for (int i = 0; i < ITERATIONS; i++) {
            sampleEncoded = Base64.encodeNoNewline(payload);
        }
        long encodeMs = System.currentTimeMillis() - encodeStart;

        long decodeStart = System.currentTimeMillis();
        byte[] sampleDecoded = null;
        for (int i = 0; i < ITERATIONS; i++) {
            sampleDecoded = Base64.decode(encodedBytes);
        }
        long decodeMs = System.currentTimeMillis() - decodeStart;

        int signature = checksum(sampleEncoded) ^ checksum(sampleDecoded);

        System.out.println("RESULT=" + signature);
        System.out.println("ENCODE_MS=" + encodeMs);
        System.out.println("DECODE_MS=" + decodeMs);
    }

    private static byte[] buildPayload() {
        byte[] out = new byte[PAYLOAD_BYTES];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) ('A' + (i % 26));
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
