import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ScreenshotVerifier {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected screenshot path");
        }
        BufferedImage image = ImageIO.read(new File(args[0]));
        if (image == null) {
            throw new IllegalStateException("Unable to decode image " + args[0]);
        }

        Region red = findBestRegion(image, false);
        System.out.println("red=" + red.sample.r + "," + red.sample.g + "," + red.sample.b +
                " @" + red.centerX + "," + red.centerY + " score=" + red.score);

        assertRed("red region", red.sample);
        if (red.centerX <= image.getWidth() / 2) {
            throw new IllegalStateException("Red region detected on wrong side: x=" + red.centerX);
        }
    }

    private static void assertRed(String label, Sample sample) {
        if (sample.r < 120 || sample.r <= sample.g + 25 || sample.r <= sample.b + 25) {
            throw new IllegalStateException(label + " is not red enough: " + sample.r + "," + sample.g + "," + sample.b);
        }
    }

    private static Region findBestRegion(BufferedImage image, boolean green) {
        int marginX = Math.max(10, image.getWidth() / 12);
        int marginY = Math.max(10, image.getHeight() / 8);
        int stepX = Math.max(12, image.getWidth() / 16);
        int stepY = Math.max(12, image.getHeight() / 16);
        Region best = null;
        for (int y = marginY; y < image.getHeight() - marginY; y += stepY) {
            for (int x = marginX; x < image.getWidth() - marginX; x += stepX) {
                Sample sample = sampleBox(image, x, y);
                int score = green
                        ? sample.g - Math.max(sample.r, sample.b)
                        : sample.r - Math.max(sample.g, sample.b);
                if (best == null || score > best.score) {
                    best = new Region(sample, x, y, score);
                }
            }
        }
        if (best == null) {
            throw new IllegalStateException("No candidate regions found");
        }
        return best;
    }

    private static Sample sampleBox(BufferedImage image, int centerX, int centerY) {
        int radius = Math.max(8, Math.min(image.getWidth(), image.getHeight()) / 12);
        long r = 0;
        long g = 0;
        long b = 0;
        long count = 0;
        int startX = Math.max(0, centerX - radius);
        int endX = Math.min(image.getWidth(), centerX + radius);
        int startY = Math.max(0, centerY - radius);
        int endY = Math.min(image.getHeight(), centerY + radius);
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int rgb = image.getRGB(x, y);
                r += (rgb >> 16) & 0xff;
                g += (rgb >> 8) & 0xff;
                b += rgb & 0xff;
                count++;
            }
        }
        if (count == 0) {
            throw new IllegalStateException("No pixels sampled");
        }
        return new Sample((int)(r / count), (int)(g / count), (int)(b / count));
    }

    private static final class Region {
        final Sample sample;
        final int centerX;
        final int centerY;
        final int score;

        Region(Sample sample, int centerX, int centerY, int score) {
            this.sample = sample;
            this.centerX = centerX;
            this.centerY = centerY;
            this.score = score;
        }
    }

    private static final class Sample {
        final int r;
        final int g;
        final int b;

        Sample(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }
}
