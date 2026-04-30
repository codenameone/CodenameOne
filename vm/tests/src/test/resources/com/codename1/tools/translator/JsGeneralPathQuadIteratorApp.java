public class JsGeneralPathQuadIteratorApp {
    interface SegmentIterator {
        boolean isDone();
        void next();
        int currentSegment(float[] coords);
    }

    static final class Path {
        static final int SEG_MOVETO = 0;
        static final int SEG_LINETO = 1;
        static final int SEG_QUADTO = 2;
        static final int SEG_CUBICTO = 3;
        static final int SEG_CLOSE = 4;
        static final int[] POINT_SHIFT = {2, 2, 4, 6, 0};

        byte[] types = new byte[8];
        float[] points = new float[16];
        int typeSize;
        int pointSize;

        void moveTo(float x, float y) {
            types[typeSize++] = (byte)SEG_MOVETO;
            points[pointSize++] = x;
            points[pointSize++] = y;
        }

        void lineTo(float x, float y) {
            types[typeSize++] = (byte)SEG_LINETO;
            points[pointSize++] = x;
            points[pointSize++] = y;
        }

        void quadTo(float x1, float y1, float x2, float y2) {
            types[typeSize++] = (byte)SEG_QUADTO;
            points[pointSize++] = x1;
            points[pointSize++] = y1;
            points[pointSize++] = x2;
            points[pointSize++] = y2;
        }

        void closePath() {
            types[typeSize++] = (byte)SEG_CLOSE;
        }

        final class Iterator implements SegmentIterator {
            int typeIndex;
            int pointIndex;

            @Override
            public boolean isDone() {
                return typeIndex >= typeSize;
            }

            @Override
            public void next() {
                typeIndex++;
            }

            @Override
            public int currentSegment(float[] coords) {
                int type = types[typeIndex];
                int count = POINT_SHIFT[type];
                System.arraycopy(points, pointIndex, coords, 0, count);
                pointIndex += count;
                return type;
            }
        }

        SegmentIterator getPathIterator() {
            return new Iterator();
        }
    }

    static int result;

    private static boolean approx(float actual, float expected) {
        return Math.abs(actual - expected) < 0.01f;
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    public static void main(String[] args) {
        Path path = new Path();
        path.moveTo(10f, 20f);
        path.quadTo(30f, 40f, 50f, 60f);
        path.lineTo(70f, 80f);
        path.closePath();

        SegmentIterator it = path.getPathIterator();
        float[] coords = new float[6];
        int score = 0;
        int index = 0;
        StringBuilder trace = new StringBuilder();
        while (!it.isDone()) {
            int type = it.currentSegment(coords);
            trace.append(index)
                    .append(':')
                    .append(type)
                    .append('@')
                    .append(coords[0]).append(',').append(coords[1]).append(',')
                    .append(coords[2]).append(',').append(coords[3])
                    .append(';');
            switch (index) {
                case 0:
                    if (type == Path.SEG_MOVETO
                            && approx(coords[0], 10f)
                            && approx(coords[1], 20f)) {
                        score |= 1;
                    }
                    break;
                case 1:
                    if (type == Path.SEG_QUADTO
                            && approx(coords[0], 30f)
                            && approx(coords[1], 40f)
                            && approx(coords[2], 50f)
                            && approx(coords[3], 60f)) {
                        score |= 2;
                    }
                    break;
                case 2:
                    if (type == Path.SEG_LINETO
                            && approx(coords[0], 70f)
                            && approx(coords[1], 80f)) {
                        score |= 4;
                    }
                    break;
                case 3:
                    if (type == Path.SEG_CLOSE) {
                        score |= 8;
                    }
                    break;
                default:
                    break;
            }
            if (isFinite(coords[0]) && isFinite(coords[1]) && isFinite(coords[2]) && isFinite(coords[3])) {
                score |= 16;
            }
            index++;
            it.next();
        }
        if (index == 4) {
            score |= 32;
        }
        if (path.typeSize == 4) {
            score |= 64;
        }
        result = score;
        System.exit(score);
    }
}
