package balancing.dynamic;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.math3.complex.Complex;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class DynamicMandelTest {

    private static final int CONVERGENCE_LIMIT = 2;
    private static final int X_MIN_POS = 0;
    private static final int X_MAX_POS = 1;
    private static final int Y_MIN_POS = 2;
    private static final int Y_MAX_POS = 3;

    protected static int WIDTH = 3840;
    protected static int HEIGHT = 2160;
    protected static int ROWS = 40;
    protected static int COLS = 1;
    protected static int NUMBER_OF_THREADS = 1;
    protected static int NUMBER_OF_TASKS = 1;
    protected static int GRANULARITY = 1;
    protected static double[] DIMENSIONS = {-2.50, 1.30, -1.1, 1.1};
    protected static int MAX_ITERATIONS = 1024;
    protected static int[] PALETTE = new int[MAX_ITERATIONS];
    protected static int[][] PIXEL_ARRAY = new int[WIDTH][HEIGHT];
    protected static boolean IS_QUIET = false;
    protected static boolean BY_COLS = false;

    protected static String PATH = "DynamicMandel.png";
    protected static int numFinishedTasks = 0;

    protected static synchronized void count() {
        numFinishedTasks++;
    }

    protected static long getTimeInMillis() {
        return System.currentTimeMillis();
    }

    static private Complex calculateNext(final Complex z, final Complex c) {
        return z.multiply(z).add(c);
    }

    static private int getIteration(final Complex c) {
        Complex z = new Complex(0, 0);

        int currentIteration = 0;

        while (z.abs() <= CONVERGENCE_LIMIT && currentIteration < MAX_ITERATIONS) {
            z = calculateNext(z, c);
            currentIteration++;
        }
        return currentIteration;
    }

    static private void byRows(int kk, int part) {
        for (int y = kk; y < kk + part && y < HEIGHT; ++y) {
            for (int x = 0; x < WIDTH; ++x) {
                double pixelX = DIMENSIONS[X_MIN_POS] + ((double) x / WIDTH) * (DIMENSIONS[X_MAX_POS] - DIMENSIONS[X_MIN_POS]);
                double pixelY = DIMENSIONS[Y_MIN_POS] + ((double) y / HEIGHT) * (DIMENSIONS[Y_MAX_POS] - DIMENSIONS[Y_MIN_POS]);

                Complex c = new Complex(pixelX, pixelY);

                PIXEL_ARRAY[x][y] = getIteration(c);
            }
        }
    }

    static private void byCols(int kk, int part) {
        for (int x = kk; x < kk + part && x < WIDTH; ++x) {
            for (int y = 0; y < HEIGHT; ++y) {
                double pixel_x = DIMENSIONS[X_MIN_POS] + ((double) x / WIDTH) * (DIMENSIONS[X_MAX_POS] - DIMENSIONS[X_MIN_POS]);
                double pixel_y = DIMENSIONS[Y_MIN_POS] + ((double) y / HEIGHT) * (DIMENSIONS[Y_MAX_POS] - DIMENSIONS[Y_MIN_POS]);

                Complex c = new Complex(pixel_x, pixel_y);

                PIXEL_ARRAY[x][y] = getIteration(c);
            }
        }
    }

    private static void addOptions(String[] args) throws Exception {
        Options opt = new Options();
        opt.addOption("w", "width", true, "width of the image in pixels");
        opt.addOption("h", "height", true, "height of the image in pixels");
        opt.addOption("d", "dimensions", true, "dimensions of rectangle in z-plane minX:maxX:minY:maxY");
        opt.addOption("p", "parallelism", true, "number of threads");
        opt.addOption("o", "output", true, "path where the image will be stored");
        opt.addOption("q", "quiet", false, "quiet mode");
        opt.addOption("i", "info", false, "help");
        opt.addOption("c", "cols", false, "perform parallelism by columns");
        opt.addOption("g", "granularity", true, "granularity value");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(opt, args);

        if (cmd.hasOption("w")) {
            WIDTH = Integer.parseInt(cmd.getOptionValue("w"));
        }
        if (cmd.hasOption("h")) {
            HEIGHT = Integer.parseInt(cmd.getOptionValue("h"));
        }
        if (cmd.hasOption("d")) {
            String[] dim = cmd.getOptionValue("d").split(":");
            for (int j = 0; j < dim.length; ++j) {
                DIMENSIONS[j] = Float.parseFloat(dim[j]);
            }
        }
        if (cmd.hasOption("p")) {
            NUMBER_OF_THREADS = Integer.parseInt(cmd.getOptionValue("p"));
        }
        if (cmd.hasOption("o")) {
            PATH = cmd.getOptionValue("o");
        }
        IS_QUIET = cmd.hasOption("q");
        if (cmd.hasOption("i")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("./runMe.sh [OPTIONS]", opt);
        }
        BY_COLS = cmd.hasOption("c");
        if (cmd.hasOption("g")) {
            GRANULARITY = Integer.parseInt(cmd.getOptionValue("g"));
        }
        NUMBER_OF_TASKS = GRANULARITY * NUMBER_OF_THREADS;
        PALETTE = new int[MAX_ITERATIONS];
        PIXEL_ARRAY = new int[WIDTH][HEIGHT];
    }

    public static void main(String[] args) {
        try {
            addOptions(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            PALETTE[i] = Color.HSBtoRGB(((94 + 1.2f * (float) Math.log(i) * (float) Math.sqrt(i)) / 256f), 0.65f, i / (i + 3.5f));
        }

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        long startTime = getTimeInMillis();

        ThreadPoolBase threadPool = new ThreadPoolBase(NUMBER_OF_THREADS, NUMBER_OF_TASKS);

        int divisor = NUMBER_OF_TASKS;
        int partRows = HEIGHT / divisor + (HEIGHT % divisor == 0 ? 0 : 1);
        int partCols = WIDTH / divisor + (WIDTH % divisor == 0 ? 0 : 1);
        final int part = BY_COLS ? partCols : partRows;
        for (int k = 0; k < divisor; ++k) {
            final int kk = k * part;
            threadPool.execute(() -> {
                if (BY_COLS) {
                    byCols(kk, part);
                } else {
                    byRows(kk, part);
                }
                count();
            });
        }

        threadPool.waitUntilAllTasksFinished();
        threadPool.stop(IS_QUIET);

        try {
            while (numFinishedTasks < NUMBER_OF_TASKS) {
                Thread.sleep(20);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        long endTime = getTimeInMillis();

        threadPool.killThreads();

        for (int x = 0; x < WIDTH; ++x) {
            for (int y = 0; y < HEIGHT; ++y) {
                if (PIXEL_ARRAY[x][y] == MAX_ITERATIONS) {
                    image.setRGB(x, y, Color.BLACK.getRGB());
                } else {
                    image.setRGB(x, y, PALETTE[PIXEL_ARRAY[x][y]]);
                }
            }
        }

        try {
            ImageIO.write(image, "PNG", new File(PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Total time elapsed (in millis): " + (endTime - startTime) + "\n");
    }
}