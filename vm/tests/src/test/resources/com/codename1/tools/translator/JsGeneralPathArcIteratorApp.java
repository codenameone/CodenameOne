public class JsGeneralPathArcIteratorApp {
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

        byte[] types = new byte[32];
        float[] points = new float[128];
        int typeSize;
        int pointSize;

        void moveTo(double x, double y) {
            types[typeSize++] = (byte)SEG_MOVETO;
            points[pointSize++] = (float)x;
            points[pointSize++] = (float)y;
        }

        void lineTo(double x, double y) {
            types[typeSize++] = (byte)SEG_LINETO;
            points[pointSize++] = (float)x;
            points[pointSize++] = (float)y;
        }

        void quadTo(double x1, double y1, double x2, double y2) {
            types[typeSize++] = (byte)SEG_QUADTO;
            points[pointSize++] = (float)x1;
            points[pointSize++] = (float)y1;
            points[pointSize++] = (float)x2;
            points[pointSize++] = (float)y2;
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

    static final class Ellipse {
        double a;
        double b;
        double cx;
        double cy;

        void initWithBounds(double x, double y, double w, double h) {
            cx = x + w / 2.0;
            cy = y + h / 2.0;
            a = w / 2.0;
            b = h / 2.0;
        }

        void getPointAtAngle(double theta, Point out) {
            double tanTheta = Math.tan(theta);
            double tanThetaSq = tanTheta * tanTheta;
            double bs = b * b;
            double as = a * a;
            double x = a * b / Math.sqrt(bs + as * tanThetaSq);
            if (Math.cos(theta) < 0) {
                x = -x;
            }
            double y = a * b / Math.sqrt(as + bs / tanThetaSq);
            if (Math.sin(theta) < 0) {
                y = -y;
            }
            out.x = x + cx;
            out.y = y + cy;
        }

        void addToPath(Path p, double startAngle, double sweepAngle, boolean join) {
            Point start = new Point();
            getPointAtAngle(startAngle, start);
            if (join) {
                p.lineTo(start.x, start.y);
            } else {
                p.moveTo(start.x, start.y);
            }
            addArcSegments(p, startAngle, sweepAngle);
        }

        void addArcSegments(Path p, double startAngle, double sweepAngle) {
            double absSweep = Math.abs(sweepAngle);
            if (absSweep < 0.0001) {
                return;
            }
            if (absSweep > Math.PI / 4.0) {
                double diff = sweepAngle < 0 ? -Math.PI / 4.0 : Math.PI / 4.0;
                addArcSegments(p, startAngle, diff);
                addArcSegments(p, startAngle + diff, sweepAngle - diff);
                return;
            }
            Point end = new Point();
            getPointAtAngle(startAngle + sweepAngle, end);
            Point control = new Point();
            calculateBezierControlPoint(startAngle, sweepAngle, control);
            p.quadTo(control.x, control.y, end.x, end.y);
        }

        void calculateBezierControlPoint(double startAngle, double sweepAngle, Point out) {
            Point p1 = new Point();
            getPointAtAngle(startAngle, p1);
            p1.x -= cx;
            p1.y -= cy;

            Point p2 = new Point();
            getPointAtAngle(startAngle + sweepAngle, p2);
            p2.x -= cx;
            p2.y -= cy;

            double x1s = p1.x * p1.x;
            double y1s = p1.y * p1.y;
            double x2s = p2.x * p2.x;
            double y2s = p2.y * p2.y;
            double as = a * a;
            double bs = b * b;

            out.x = -(p1.y * (-as * y2s - bs * x2s) + as * y1s * p2.y + bs * x1s * p2.y)
                    / (bs * p2.x * p1.y - bs * p1.x * p2.y);
            out.y = (p1.x * (-as * y2s - bs * x2s) + as * p2.x * y1s + bs * x1s * p2.x)
                    / (as * p2.x * p1.y - as * p1.x * p2.y);
            out.x += cx;
            out.y += cy;
        }
    }

    static final class Point {
        double x;
        double y;
    }

    static int result;

    private static boolean approx(float actual, float expected) {
        return Math.abs(actual - expected) < 0.25f;
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    public static void main(String[] args) {
        Path path = new Path();
        Ellipse ellipse = new Ellipse();
        ellipse.initWithBounds(0.0, 0.0, 100.0, 50.0);
        ellipse.addToPath(path, 0.0, -Math.PI, false);

        SegmentIterator it = path.getPathIterator();
        float[] coords = new float[6];
        int score = 0;
        int index = 0;
        int quadCount = 0;
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
                            && approx(coords[0], 100f)
                            && approx(coords[1], 25f)) {
                        score |= 1;
                    }
                    break;
                case 1:
                    if (type == Path.SEG_QUADTO
                            && approx(coords[2], 72.36068f)
                            && approx(coords[3], 2.63932f)) {
                        score |= 2;
                    }
                    break;
                case 2:
                    if (type == Path.SEG_QUADTO
                            && approx(coords[2], 50f)
                            && approx(coords[3], 0f)) {
                        score |= 4;
                    }
                    break;
                case 3:
                    if (type == Path.SEG_QUADTO
                            && approx(coords[2], 27.63932f)
                            && approx(coords[3], 2.63932f)) {
                        score |= 8;
                    }
                    break;
                case 4:
                    if (type == Path.SEG_QUADTO
                            && approx(coords[2], 0f)
                            && approx(coords[3], 25f)) {
                        score |= 16;
                    }
                    break;
                default:
                    break;
            }
            if (type == Path.SEG_QUADTO) {
                quadCount++;
            }
            if (isFinite(coords[0]) && isFinite(coords[1]) && isFinite(coords[2]) && isFinite(coords[3])) {
                score |= 32;
            }
            index++;
            it.next();
        }
        if (index == 5) {
            score |= 64;
        }
        if (quadCount == 4) {
            score |= 128;
        }
        if (path.typeSize == 5) {
            score |= 256;
        }
        result = score;
        System.exit(score);
    }
}
