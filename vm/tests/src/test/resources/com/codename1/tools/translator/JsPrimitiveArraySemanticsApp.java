public class JsPrimitiveArraySemanticsApp {
    static int result;

    public static void main(String[] args) {
        int score = 0;

        byte[] types = new byte[] {0, 1, 2, 3, 4};
        if (types[0] == 0) {
            score |= 1;
        }
        if (types[1] == 1) {
            score |= 2;
        }
        if (types[2] == 2) {
            score |= 4;
        }
        if (types[3] == 3) {
            score |= 8;
        }
        if (types[4] == 4) {
            score |= 16;
        }

        float[] points = new float[] {1, 2, 3, 4, 5, 6};
        if (points[0] == 1f && points[5] == 6f) {
            score |= 32;
        }

        float[] copy = new float[6];
        System.arraycopy(points, 0, copy, 0, points.length);
        if (copy[1] == 2f && copy[4] == 5f) {
            score |= 64;
        }

        float[] partial = new float[] {99, 99, 99, 99, 99, 99};
        System.arraycopy(points, 2, partial, 1, 3);
        if (partial[0] == 99f && partial[1] == 3f && partial[2] == 4f && partial[3] == 5f && partial[4] == 99f) {
            score |= 128;
        }

        result = score;
        System.exit(score);
    }
}
