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

        Sample green = sampleBox(image, image.getWidth() / 4, (image.getHeight() * 3) / 5);
        Sample red = sampleBox(image, (image.getWidth() * 3) / 4, (image.getHeight() * 3) / 5);

        System.out.println("green=" + green.r + "," + green.g + "," + green.b);
        System.out.println("red=" + red.r + "," + red.g + "," + red.b);

        assertGreen("green region", green);
        assertRed("red region", red);
    }

    private static void assertGreen(String label, Sample sample) {
        if (sample.g < 120 || sample.g <= sample.r + 25 || sample.g <= sample.b + 25) {
            throw new IllegalStateException(label + " is not green enough: " + sample.r + "," + sample.g + "," + sample.b);
        }
    }

    private static void assertRed(String label, Sample sample) {
        if (sample.r < 120 || sample.r <= sample.g + 25 || sample.r <= sample.b + 25) {
            throw new IllegalStateException(label + " is not red enough: " + sample.r + "," + sample.g + "," + sample.b);
        }
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
