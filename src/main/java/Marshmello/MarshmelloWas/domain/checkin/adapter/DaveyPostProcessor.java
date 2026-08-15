package Marshmello.MarshmelloWas.domain.checkin.adapter;

import Marshmello.MarshmelloWas.domain.checkin.adapter.BinaryMaskOperations.ComponentCriteria;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageAnalyzer.AnalysisResult;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

final class DaveyPostProcessor {

    private static final float PROBABILITY_THRESHOLD = 0.5f;
    private static final double MINIMUM_ABDOMEN_AREA_RATIO = 0.08;
    private static final double MAXIMUM_ABDOMEN_AREA_RATIO = 0.92;
    private static final double MINIMUM_NAVEL_CONFIDENCE = 0.20;
    private static final double MINIMUM_ABDOMEN_CONFIDENCE = 0.55;
    private static final int MINIMUM_IMAGE_SIZE = 128;
    private static final double MINIMUM_BRIGHTNESS = 18.0;
    private static final double MAXIMUM_BRIGHTNESS = 242.0;
    private static final double MINIMUM_CONTRAST = 8.0;
    private static final double NAVEL_EDGE_MARGIN_RATIO = 0.04;
    private static final ComponentCriteria COMPONENT_CRITERIA = new ComponentCriteria(25, 20);

    AnalysisResult evaluate(BufferedImage image, PredictionMaps maps) {
        validateDimensions(image, maps);
        boolean[][] abdomenMask = postprocessAbdomen(maps.abdomenProbability());
        double areaRatio = areaRatio(abdomenMask);
        if (areaRatio < MINIMUM_ABDOMEN_AREA_RATIO || areaRatio > MAXIMUM_ABDOMEN_AREA_RATIO) {
            return AnalysisResult.notDetected();
        }

        NavelPrediction navel = locateNavel(maps.navelProbability(), abdomenMask);
        double abdomenConfidence = meanProbability(maps.abdomenProbability(), abdomenMask);
        InputAssessment assessment = new InputAssessment(navel, abdomenConfidence);
        if (requiresReview(image, assessment)) {
            return AnalysisResult.notDetected();
        }

        boolean[][] abdomenRoi = BinaryMaskOperations.erode(abdomenMask, 3);
        boolean[][] stretchMask = BinaryMaskOperations.threshold(
                maps.stretchProbability(), PROBABILITY_THRESHOLD);
        boolean[][] cleaned = BinaryMaskOperations.intersect(stretchMask, abdomenRoi);
        cleaned = BinaryMaskOperations.close(cleaned, 5, 1);
        cleaned = BinaryMaskOperations.open(cleaned, 1);
        cleaned = BinaryMaskOperations.intersect(cleaned, abdomenRoi);

        int score = calculateScore(cleaned, navel.location());
        return score == 0
                ? AnalysisResult.notDetected()
                : AnalysisResult.detected((short) score);
    }

    private void validateDimensions(BufferedImage image, PredictionMaps maps) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (!matches(maps.abdomenProbability(), width, height)
                || !matches(maps.navelProbability(), width, height)
                || !matches(maps.stretchProbability(), width, height)) {
            throw new IllegalArgumentException("Prediction maps must match image dimensions");
        }
    }

    private boolean matches(float[][] map, int width, int height) {
        if (map == null || map.length != height) {
            return false;
        }
        for (float[] row : map) {
            if (row == null || row.length != width) {
                return false;
            }
        }
        return true;
    }

    private boolean[][] postprocessAbdomen(float[][] probability) {
        boolean[][] mask = BinaryMaskOperations.threshold(probability, PROBABILITY_THRESHOLD);
        mask = BinaryMaskOperations.close(mask, 5, 2);
        mask = BinaryMaskOperations.keepLargestComponent(mask);
        return BinaryMaskOperations.fillHoles(mask);
    }

    private NavelPrediction locateNavel(float[][] probability, boolean[][] abdomenMask) {
        int bestX = 0;
        int bestY = 0;
        float confidence = -1.0f;
        for (int y = 0; y < probability.length; y++) {
            for (int x = 0; x < probability[y].length; x++) {
                if (abdomenMask[y][x] && probability[y][x] > confidence) {
                    bestX = x;
                    bestY = y;
                    confidence = probability[y][x];
                }
            }
        }
        return new NavelPrediction(new Point(bestX, bestY), confidence);
    }

    private double meanProbability(float[][] probability, boolean[][] selected) {
        double sum = 0.0;
        int count = 0;
        for (int y = 0; y < probability.length; y++) {
            for (int x = 0; x < probability[y].length; x++) {
                if (selected[y][x]) {
                    sum += probability[y][x];
                    count++;
                }
            }
        }
        return count == 0 ? 0.0 : sum / count;
    }

    private double areaRatio(boolean[][] mask) {
        int selected = 0;
        for (boolean[] row : mask) {
            for (boolean value : row) {
                if (value) {
                    selected++;
                }
            }
        }
        return (double) selected / (mask.length * mask[0].length);
    }

    private boolean requiresReview(BufferedImage image, InputAssessment assessment) {
        ImageStatistics statistics = imageStatistics(image);
        Point navel = assessment.navel().location();
        double marginX = image.getWidth() * NAVEL_EDGE_MARGIN_RATIO;
        double marginY = image.getHeight() * NAVEL_EDGE_MARGIN_RATIO;
        return image.getWidth() < MINIMUM_IMAGE_SIZE
                || image.getHeight() < MINIMUM_IMAGE_SIZE
                || statistics.brightness() < MINIMUM_BRIGHTNESS
                || statistics.brightness() > MAXIMUM_BRIGHTNESS
                || statistics.contrast() < MINIMUM_CONTRAST
                || assessment.abdomenConfidence() < MINIMUM_ABDOMEN_CONFIDENCE
                || assessment.navel().confidence() < MINIMUM_NAVEL_CONFIDENCE
                || navel.x < marginX
                || navel.x > image.getWidth() - marginX
                || navel.y < marginY
                || navel.y > image.getHeight() - marginY;
    }

    private ImageStatistics imageStatistics(BufferedImage image) {
        double sum = 0.0;
        double squareSum = 0.0;
        int pixels = image.getWidth() * image.getHeight();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                double gray = 0.299 * red + 0.587 * green + 0.114 * blue;
                sum += gray;
                squareSum += gray * gray;
            }
        }
        double mean = sum / pixels;
        double variance = Math.max(0.0, squareSum / pixels - mean * mean);
        return new ImageStatistics(mean, Math.sqrt(variance));
    }

    private int calculateScore(boolean[][] mask, Point navel) {
        int width = mask[0].length;
        int height = mask.length;
        int x = Math.max(1, Math.min(navel.x, width - 1));
        int y = Math.max(1, Math.min(navel.y, height - 1));
        Rectangle[] quadrants = {
                new Rectangle(0, 0, x, y),
                new Rectangle(x, 0, width - x, y),
                new Rectangle(0, y, x, height - y),
                new Rectangle(x, y, width - x, height - y)
        };
        int score = 0;
        for (Rectangle quadrant : quadrants) {
            int count = BinaryMaskOperations.countComponents(mask, quadrant, COMPONENT_CRITERIA);
            score += count == 0 ? 0 : count <= 3 ? 1 : 2;
        }
        return score;
    }

    record PredictionMaps(
            float[][] abdomenProbability,
            float[][] navelProbability,
            float[][] stretchProbability
    ) {}

    private record NavelPrediction(Point location, double confidence) {}

    private record InputAssessment(
            NavelPrediction navel,
            double abdomenConfidence
    ) {}

    private record ImageStatistics(double brightness, double contrast) {}
}
