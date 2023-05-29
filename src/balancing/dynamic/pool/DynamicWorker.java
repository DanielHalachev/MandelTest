package balancing.dynamic.pool;

import org.apache.commons.math3.complex.Complex;

public class DynamicWorker implements java.lang.Runnable {


    protected static final int CONVERGENCE_LIMIT = 2;
    private static final int X_MIN_POS = 0;
    private static final int X_MAX_POS = 1;
    private static final int Y_MIN_POS = 2;
    private static final int Y_MAX_POS = 3;
    private final int taskLocation;

    public DynamicWorker(int taskLocation) {
        this.taskLocation = taskLocation;
    }

    private Complex calculateNextIteration(Complex z, Complex c) {
        return z.multiply(z).add(c);
    }

    private int getIteration(final Complex c) {
        Complex z = new Complex(0, 0);
        int currentIteration = 0;
        while (z.abs() <= CONVERGENCE_LIMIT && currentIteration < DynamicMandelTest.MAX_ITERATIONS) {
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
//        while (z.abs() <= CONVERGENCE_LIMIT && currentIteration < DynamicMandelTest.MAX_ITERATIONS) {
//            z = calcNewValue(z, c);
//            currentIteration++;
//        }
//
//        return currentIteration;
//    }

    @Override
    public void run() {
        int startFrom = taskLocation * DynamicMandelTest.TASK_WIDTH;
        int endTo = taskLocation * DynamicMandelTest.TASK_WIDTH + DynamicMandelTest.TASK_WIDTH;
        for (int y = startFrom; y < endTo && y < DynamicMandelTest.HEIGHT; y++) {
            for (int x = 0; x < DynamicMandelTest.WIDTH; x++) {
                double pixel_x = DynamicMandelTest.DIMENSIONS[X_MIN_POS] + ((double) x / DynamicMandelTest.WIDTH) * (DynamicMandelTest.DIMENSIONS[X_MAX_POS] - DynamicMandelTest.DIMENSIONS[X_MIN_POS]);
                double pixel_y = DynamicMandelTest.DIMENSIONS[Y_MIN_POS] + ((double) y / (DynamicMandelTest.HEIGHT)) * (DynamicMandelTest.DIMENSIONS[Y_MAX_POS] - DynamicMandelTest.DIMENSIONS[Y_MIN_POS]);
                Complex c = new Complex(pixel_x, pixel_y);

                DynamicMandelTest.PIXEL_ARRAY[x][y] = getIteration(c);
            }
        }
    }
}