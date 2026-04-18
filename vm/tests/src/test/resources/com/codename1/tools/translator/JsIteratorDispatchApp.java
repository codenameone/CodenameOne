public class JsIteratorDispatchApp {
    interface SegmentIterator {
        boolean isDone();
        void next();
        int currentSegment(float[] coords);
    }

    static final class Shape {
        static final int SEG_MOVETO = 0;
        static final int SEG_LINETO = 1;
        static final int SEG_QUADTO = 2;
        static final int SEG_CUBICTO = 3;
        static final int SEG_CLOSE = 4;
        static final int[] POINT_SHIFT = {2, 2, 4, 6, 0};

        final byte[] types = new byte[] {
                SEG_MOVETO,
                SEG_LINETO,
                SEG_QUADTO,
                SEG_CUBICTO,
                SEG_CLOSE
        };
        final float[] points = new float[] {
                1, 2,
                3, 4,
                5, 6, 7, 8,
                9, 10, 11, 12, 13, 14
        };

        final class Iterator implements SegmentIterator {
            int typeIndex;
            int pointIndex;

            @Override
            public boolean isDone() {
                return typeIndex >= types.length;
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

    public static void main(String[] args) {
        Shape path = new Shape();
        SegmentIterator it = path.getPathIterator();
        float[] coords = new float[6];
        int score = 0;
        int index = 0;
        while (!it.isDone()) {
            int type = it.currentSegment(coords);
            switch (index) {
                case 0:
                    if (type == Shape.SEG_MOVETO && coords[0] == 1f && coords[1] == 2f) {
                        score |= 1;
                    }
                    break;
                case 1:
                    if (type == Shape.SEG_LINETO && coords[0] == 3f && coords[1] == 4f) {
                        score |= 2;
                    }
                    break;
                case 2:
                    if (type == Shape.SEG_QUADTO
                            && coords[0] == 5f && coords[1] == 6f
                            && coords[2] == 7f && coords[3] == 8f) {
                        score |= 4;
                    }
                    break;
                case 3:
                    if (type == Shape.SEG_CUBICTO
                            && coords[0] == 9f && coords[1] == 10f
                            && coords[2] == 11f && coords[3] == 12f
                            && coords[4] == 13f && coords[5] == 14f) {
                        score |= 8;
                    }
                    break;
                case 4:
                    if (type == Shape.SEG_CLOSE) {
                        score |= 16;
                    }
                    break;
                default:
                    break;
            }
            index++;
            it.next();
        }
        if (index == 5) {
            score |= 32;
        }
        result = score;
        System.exit(score);
    }
}
