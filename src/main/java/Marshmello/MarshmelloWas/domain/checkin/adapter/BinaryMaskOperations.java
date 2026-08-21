package Marshmello.MarshmelloWas.domain.checkin.adapter;

import java.awt.Rectangle;
import java.util.Arrays;

final class BinaryMaskOperations {

    private static final int[][] NEIGHBORS = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1, 0}, {1, 0},
            {-1, 1}, {0, 1}, {1, 1}
    };

    private BinaryMaskOperations() {}

    static boolean[][] threshold(float[][] probability, float threshold) {
        boolean[][] result = new boolean[probability.length][probability[0].length];
        for (int y = 0; y < probability.length; y++) {
            for (int x = 0; x < probability[y].length; x++) {
                result[y][x] = probability[y][x] >= threshold;
            }
        }
        return result;
    }

    static boolean[][] close(boolean[][] mask, int radius, int iterations) {
        boolean[][] result = copy(mask);
        for (int iteration = 0; iteration < iterations; iteration++) {
            result = erode(dilate(result, radius), radius);
        }
        return result;
    }

    static boolean[][] open(boolean[][] mask, int radius) {
        return dilate(erode(mask, radius), radius);
    }

    static boolean[][] erode(boolean[][] mask, int radius) {
        return morph(mask, radius, false);
    }

    static boolean[][] intersect(boolean[][] left, boolean[][] right) {
        boolean[][] result = new boolean[left.length][left[0].length];
        for (int y = 0; y < left.length; y++) {
            for (int x = 0; x < left[y].length; x++) {
                result[y][x] = left[y][x] && right[y][x];
            }
        }
        return result;
    }

    static boolean[][] keepLargestComponent(boolean[][] mask) {
        int height = mask.length;
        int width = mask[0].length;
        ComponentSearch search = new ComponentSearch(mask, new Rectangle(0, 0, width, height));
        int[] largest = new int[0];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!mask[y][x] || search.visited[y][x]) {
                    continue;
                }
                int count = search.collect(x, y);
                if (count > largest.length) {
                    largest = Arrays.copyOf(search.queue, count);
                }
            }
        }

        boolean[][] result = new boolean[height][width];
        for (int index : largest) {
            result[index / width][index % width] = true;
        }
        return result;
    }

    static boolean[][] fillHoles(boolean[][] mask) {
        int height = mask.length;
        int width = mask[0].length;
        BackgroundSearch search = new BackgroundSearch(mask);

        for (int x = 0; x < width; x++) {
            search.enqueue(x, 0);
            search.enqueue(x, height - 1);
        }
        for (int y = 1; y < height - 1; y++) {
            search.enqueue(0, y);
            search.enqueue(width - 1, y);
        }

        while (search.hasNext()) {
            int index = search.next();
            int x = index % width;
            int y = index / width;
            for (int[] neighbor : NEIGHBORS) {
                int nextX = x + neighbor[0];
                int nextY = y + neighbor[1];
                if (nextX >= 0 && nextX < width && nextY >= 0 && nextY < height) {
                    search.enqueue(nextX, nextY);
                }
            }
        }

        boolean[][] result = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = mask[y][x] || !search.external[y][x];
            }
        }
        return result;
    }

    static int countComponents(
            boolean[][] mask,
            Rectangle region,
            ComponentCriteria criteria
    ) {
        int height = mask.length;
        int width = mask[0].length;
        Rectangle bounds = region.intersection(new Rectangle(0, 0, width, height));
        if (bounds.isEmpty()) {
            return 0;
        }

        ComponentSearch search = new ComponentSearch(mask, bounds);
        int count = 0;
        for (int y = bounds.y; y < bounds.y + bounds.height; y++) {
            for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
                if (!mask[y][x] || search.visited[y][x]) {
                    continue;
                }
                int area = search.collect(x, y);
                int minX = width;
                int maxX = 0;
                int minY = height;
                int maxY = 0;
                for (int index = 0; index < area; index++) {
                    int componentX = search.queue[index] % width;
                    int componentY = search.queue[index] / width;
                    minX = Math.min(minX, componentX);
                    maxX = Math.max(maxX, componentX);
                    minY = Math.min(minY, componentY);
                    maxY = Math.max(maxY, componentY);
                }
                int majorLength = Math.max(maxX - minX + 1, maxY - minY + 1);
                if (area >= criteria.minimumArea() && majorLength >= criteria.minimumLength()) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean[][] dilate(boolean[][] mask, int radius) { return morph(mask, radius, true); }

    private static boolean[][] morph(boolean[][] mask, int radius, boolean dilation) {
        if (radius <= 0) {
            return copy(mask);
        }
        int height = mask.length;
        int width = mask[0].length;
        int[][] offsets = ellipseOffsets(radius);
        boolean[][] result = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean value = !dilation;
                for (int[] offset : offsets) {
                    int sampleX = x + offset[0];
                    int sampleY = y + offset[1];
                    boolean sample = sampleX >= 0 && sampleX < width
                            && sampleY >= 0 && sampleY < height
                            && mask[sampleY][sampleX];
                    if (dilation && sample) {
                        value = true;
                        break;
                    }
                    if (!dilation && !sample) {
                        value = false;
                        break;
                    }
                }
                result[y][x] = value;
            }
        }
        return result;
    }

    private static int[][] ellipseOffsets(int radius) {
        int[][] offsets = new int[(radius * 2 + 1) * (radius * 2 + 1)][2];
        int count = 0;
        double denominator = radius * radius;
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if ((x * x + y * y) / denominator <= 1.0) {
                    offsets[count][0] = x;
                    offsets[count][1] = y;
                    count++;
                }
            }
        }
        return Arrays.copyOf(offsets, count);
    }

    private static boolean[][] copy(boolean[][] mask) {
        boolean[][] result = new boolean[mask.length][];
        for (int y = 0; y < mask.length; y++) {
            result[y] = mask[y].clone();
        }
        return result;
    }

    record ComponentCriteria(int minimumArea, int minimumLength) {}

    private static final class ComponentSearch {

        private final boolean[][] mask;
        private final Rectangle bounds;
        private final boolean[][] visited;
        private final int[] queue;

        private ComponentSearch(boolean[][] mask, Rectangle bounds) {
            this.mask = mask;
            this.bounds = bounds;
            this.visited = new boolean[mask.length][mask[0].length];
            this.queue = new int[bounds.width * bounds.height];
        }

        private int collect(int startX, int startY) {
            int width = mask[0].length;
            int head = 0;
            int tail = 1;
            queue[0] = startY * width + startX;
            visited[startY][startX] = true;

            while (head < tail) {
                int index = queue[head++];
                int x = index % width;
                int y = index / width;
                for (int[] neighbor : NEIGHBORS) {
                    int nextX = x + neighbor[0];
                    int nextY = y + neighbor[1];
                    if (bounds.contains(nextX, nextY)
                            && mask[nextY][nextX]
                            && !visited[nextY][nextX]) {
                        visited[nextY][nextX] = true;
                        queue[tail++] = nextY * width + nextX;
                    }
                }
            }
            return tail;
        }
    }

    private static final class BackgroundSearch {

        private final boolean[][] mask;
        private final boolean[][] external;
        private final int[] queue;
        private int head;
        private int tail;

        private BackgroundSearch(boolean[][] mask) {
            this.mask = mask;
            this.external = new boolean[mask.length][mask[0].length];
            this.queue = new int[mask.length * mask[0].length];
        }

        private void enqueue(int x, int y) {
            if (!mask[y][x] && !external[y][x]) {
                external[y][x] = true;
                queue[tail++] = y * mask[0].length + x;
            }
        }

        private boolean hasNext() {
            return head < tail;
        }

        private int next() {
            return queue[head++];
        }
    }
}
