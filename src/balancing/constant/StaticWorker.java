package balancing.constant;

import org.apache.commons.math3.complex.Complex;

public final class StaticWorker implements Runnable {

    private static final int CONVERGENCE_LIMIT = 2;
    private static final int X_MIN_POS = 0;
    private static final int X_MAX_POS = 1;
    private static final int Y_MIN_POS = 2;
    private static final int Y_MAX_POS = 3;

    private final int index;
    private final boolean quiet;
    private final boolean byCols;
    private final int maxIterations;
    private final int width;
    private final int height;
    private final double[] dimensions;
    private final int rows;
    private final int cols;
    private final int taskWidth;
    private final int taskHeight;
    private final int numberOfTasks;
    private final int threads;

    public StaticWorker(final int index) {
        this.index = index;
        this.quiet = StaticMandelTest.IS_QUIET;
        this.maxIterations = StaticMandelTest.MAX_ITERATIONS;
        this.width = StaticMandelTest.WIDTH;
        this.height = StaticMandelTest.HEIGHT;
        this.dimensions = StaticMandelTest.DIMENSIONS;
        this.rows = StaticMandelTest.ROWS;
        this.cols = StaticMandelTest.COLS;
        this.taskWidth = StaticMandelTest.TASK_WIDTH;
        this.taskHeight = StaticMandelTest.TASK_HEIGHT;
        this.numberOfTasks = StaticMandelTest.NUMBER_OF_TASKS;
        this.byCols = StaticMandelTest.BY_COLS;
        this.threads = StaticMandelTest.NUMBER_OF_THREADS;
    }

    private Complex calculateNextIteration(final Complex z, final Complex c) {
        return z.multiply(z).add(c);
    }

    private int getIteration(final Complex c) {
        Complex z = new Complex(0, 0);
        int currentIteration = 0;
        while (z.abs() <= CONVERGENCE_LIMIT && currentIteration < maxIterations) {
            z = calculateNextIteration(z, c);
            currentIteration++;
        }
        return currentIteration;
    }

    public void calculateByRows() {
        for (int task = index; task < numberOfTasks; task += threads) {
            int p = (task % rows) * taskHeight;
            for (int x = p; x < p + taskHeight; x++) {
                int q = (task / rows) * taskWidth;
                for (int y = q; y < q + taskWidth; y++) {
                    double pixelX = dimensions[Y_MIN_POS]
                            + ((double) x / height) * (dimensions[Y_MAX_POS] - dimensions[Y_MIN_POS]);
                    double pixelY = dimensions[X_MIN_POS]
                            + ((double) y / width) * (dimensions[X_MAX_POS] - dimensions[X_MIN_POS]);

                    Complex c = new Complex(pixelY, pixelX);

                    StaticMandelTest.PIXEL_ARRAY[y][x] = getIteration(c);
                }
            }
        }
    }

    public void calculateByCols() {
        for (int task = index; task < numberOfTasks; task += threads) {
            int p = (task % cols) * taskWidth;
            for (int x = p; x < p + taskWidth; x++) {
                int q = (task / cols) * taskHeight;
                for (int y = q; y < q + taskHeight; y++) {
                    double pixelX = dimensions[X_MIN_POS]
                            + ((double) x / width) * (dimensions[X_MAX_POS] - dimensions[X_MIN_POS]);
                    double pixelY = dimensions[Y_MIN_POS]
                            + ((double) y / height) * (dimensions[Y_MAX_POS] - dimensions[Y_MIN_POS]);

                    Complex c = new Complex(pixelX, pixelY);

                    StaticMandelTest.PIXEL_ARRAY[x][y] = getIteration(c);
                }
            }
        }
    }

    @Override
    public void run() {
        if (!quiet) {
            System.out.println("Thread-" + index + " started.");
        }
        long startTime = System.currentTimeMillis();

        if (byCols) {
            calculateByCols();
        } else {
            calculateByRows();
        }

        if (!quiet) {
            long endTime = System.currentTimeMillis();
            System.out.println("Thread-" + index + " finished. Execution time was (millis): " + (endTime - startTime));
        }
    }
}
