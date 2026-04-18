public class JsIteratorPointShiftDispatchApp {
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

        final class Iterator implements SegmentIterator {
            int typeIndex;

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
                return POINT_SHIFT[types[typeIndex]];
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
        int score = 0;
        int index = 0;
        while (!it.isDone()) {
            int count = it.currentSegment(null);
            switch (index) {
                case 0:
                    if (count == 2) {
                        score |= 1;
                    }
                    break;
                case 1:
                    if (count == 2) {
                        score |= 2;
                    }
                    break;
                case 2:
                    if (count == 4) {
                        score |= 4;
                    }
                    break;
                case 3:
                    if (count == 6) {
                        score |= 8;
                    }
                    break;
                case 4:
                    if (count == 0) {
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
