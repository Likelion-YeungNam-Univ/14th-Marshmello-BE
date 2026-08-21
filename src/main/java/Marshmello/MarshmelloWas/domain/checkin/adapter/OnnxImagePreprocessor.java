package Marshmello.MarshmelloWas.domain.checkin.adapter;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

final class OnnxImagePreprocessor {

    private static final float[] IMAGENET_MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] IMAGENET_STANDARD_DEVIATION = {0.229f, 0.224f, 0.225f};

    private final int imageSize;

    OnnxImagePreprocessor(int imageSize) {
        if (imageSize <= 0) {
            throw new IllegalArgumentException("ONNX image size must be positive");
        }
        this.imageSize = imageSize;
    }

    PreparedImage prepare(byte[] imageData) throws IOException {
        if (imageData == null || imageData.length == 0) {
            throw new IOException("Image data is empty");
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        if (image == null) {
            throw new IOException("Unsupported image data");
        }

        double scale = Math.min(
                (double) imageSize / image.getWidth(),
                (double) imageSize / image.getHeight());
        Size resizedSize = new Size(
                Math.max(1, (int) Math.round(image.getWidth() * scale)),
                Math.max(1, (int) Math.round(image.getHeight() * scale)));
        Offset offset = new Offset(
                (imageSize - resizedSize.width()) / 2,
                (imageSize - resizedSize.height()) / 2);

        BufferedImage square = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = square.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(
                    image,
                    offset.left(),
                    offset.top(),
                    resizedSize.width(),
                    resizedSize.height(),
                    null);
        } finally {
            graphics.dispose();
        }

        LetterboxTransform transform = new LetterboxTransform(
                new Size(image.getWidth(), image.getHeight()),
                resizedSize,
                offset);
        return new PreparedImage(image, normalize(square), transform);
    }

    private float[][][][] normalize(BufferedImage image) {
        float[][][][] tensor = new float[1][3][imageSize][imageSize];
        for (int y = 0; y < imageSize; y++) {
            for (int x = 0; x < imageSize; x++) {
                int rgb = image.getRGB(x, y);
                float red = ((rgb >> 16) & 0xFF) / 255.0f;
                float green = ((rgb >> 8) & 0xFF) / 255.0f;
                float blue = (rgb & 0xFF) / 255.0f;
                tensor[0][0][y][x] = normalized(red, 0);
                tensor[0][1][y][x] = normalized(green, 1);
                tensor[0][2][y][x] = normalized(blue, 2);
            }
        }
        return tensor;
    }

    private float normalized(float value, int channel) {
        return (value - IMAGENET_MEAN[channel]) / IMAGENET_STANDARD_DEVIATION[channel];
    }

    record PreparedImage(
            BufferedImage image,
            float[][][][] tensor,
            LetterboxTransform transform
    ) {
        float[][] restoreProbability(float[][] logits) {
            return transform.restoreProbability(logits);
        }
    }

    private record LetterboxTransform(Size original, Size resized, Offset offset) {

        private float[][] restoreProbability(float[][] logits) {
            if (logits.length == 0 || logits.length != logits[0].length) {
                throw new IllegalArgumentException("ONNX probability map must be a non-empty square");
            }
            float[][] probability = new float[original.height()][original.width()];
            for (int y = 0; y < original.height(); y++) {
                double sourceY = offset.top()
                        + ((y + 0.5) * resized.height() / original.height()) - 0.5;
                sourceY = clamp(sourceY, offset.top(), offset.top() + resized.height() - 1);
                for (int x = 0; x < original.width(); x++) {
                    double sourceX = offset.left()
                            + ((x + 0.5) * resized.width() / original.width()) - 0.5;
                    sourceX = clamp(sourceX, offset.left(), offset.left() + resized.width() - 1);
                    probability[y][x] = sigmoid(bilinear(logits, sourceX, sourceY));
                }
            }
            return probability;
        }

        private float bilinear(float[][] values, double x, double y) {
            int x0 = (int) Math.floor(x);
            int y0 = (int) Math.floor(y);
            int x1 = Math.min(x0 + 1, values[0].length - 1);
            int y1 = Math.min(y0 + 1, values.length - 1);
            double weightX = x - x0;
            double weightY = y - y0;
            double top = values[y0][x0] * (1.0 - weightX) + values[y0][x1] * weightX;
            double bottom = values[y1][x0] * (1.0 - weightX) + values[y1][x1] * weightX;
            return (float) (top * (1.0 - weightY) + bottom * weightY);
        }

        private float sigmoid(float value) {
            double clipped = clamp(value, -80.0, 80.0);
            return (float) (1.0 / (1.0 + Math.exp(-clipped)));
        }

        private double clamp(double value, double minimum, double maximum) {
            return Math.max(minimum, Math.min(value, maximum));
        }
    }

    private record Size(int width, int height) {}

    private record Offset(int left, int top) {}
}
