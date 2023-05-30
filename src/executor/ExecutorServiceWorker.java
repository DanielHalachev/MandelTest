package executor;

import org.apache.commons.math3.complex.Complex;

public class ExecutorServiceWorker implements java.lang.Runnable {


    protected static final int CONVERGENCE_LIMIT = 2;
    private static final int X_MIN_POS = 0;
    private static final int X_MAX_POS = 1;
    private static final int Y_MIN_POS = 2;
    private static final int Y_MAX_POS = 3;
    private final int taskLocation;

    public ExecutorServiceWorker(int taskLocation) {
        this.taskLocation = taskLocation;
    }

    private Complex calculateNextIteration(Complex z, Complex c) {
        return z.multiply(z).add(c);
    }

    private int getIteration(final Complex c) {
        Complex z = new Complex(0, 0);
        int currentIteration = 0;
        while (z.abs() <= CONVERGENCE_LIMIT && currentIteration < ExecutorServiceMandelTest.MAX_ITERATIONS) {
            z = calculateNextIteration(z, c);
            currentIteration++;
        }
        return currentIteration;
    }
//    private short getIndex(Complex c) {
//        Complex z = new Complex(0, 0);
//
//        short currentIteration = -128;
//
//        while (z.abs() <= CONVERGENCE_LIMIT && currentIteration < ExecutorServiceMandelTest.MAX_ITERATIONS) {
//            z = calcNewValue(z, c);
//            currentIteration++;
//        }
//
//        return currentIteration;
//    }

    @Override
    public void run() {
        int startFrom = taskLocation * ExecutorServiceMandelTest.TASK_WIDTH;
        int endTo = taskLocation * ExecutorServiceMandelTest.TASK_WIDTH + ExecutorServiceMandelTest.TASK_WIDTH;
        for (int y = startFrom; y < endTo && y < ExecutorServiceMandelTest.HEIGHT; y++) {
            for (int x = 0; x < ExecutorServiceMandelTest.WIDTH; x++) {
                double pixel_x = ExecutorServiceMandelTest.DIMENSIONS[X_MIN_POS] + ((double) x / ExecutorServiceMandelTest.WIDTH) * (ExecutorServiceMandelTest.DIMENSIONS[X_MAX_POS] - ExecutorServiceMandelTest.DIMENSIONS[X_MIN_POS]);
                double pixel_y = ExecutorServiceMandelTest.DIMENSIONS[Y_MIN_POS] + ((double) y / (ExecutorServiceMandelTest.HEIGHT)) * (ExecutorServiceMandelTest.DIMENSIONS[Y_MAX_POS] - ExecutorServiceMandelTest.DIMENSIONS[Y_MIN_POS]);
                Complex c = new Complex(pixel_x, pixel_y);

                ExecutorServiceMandelTest.PIXEL_ARRAY[x][y] = getIteration(c);
            }
        }
    }
}