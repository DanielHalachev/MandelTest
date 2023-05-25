package balancing.constant;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class StaticMandelTest {

    protected static int WIDTH = 2160;
    protected static int HEIGHT = 2160;
    protected static int ROWS = 40;
    protected static int COLS = 1;
    protected static int NUMBER_OF_THREADS = 16;
    protected static int NUMBER_OF_TASKS = ROWS * COLS;
    protected static double[] DIMENSIONS = {-1.8, 0.45, -1.1, 1.1};
    protected static int MAX_ITERATIONS = 1024;
    protected static int[] PALETTE = new int[MAX_ITERATIONS];
    protected static int[][] PIXEL_ARRAY = new int[WIDTH][HEIGHT];
    protected static boolean IS_QUIET = false;
    protected static boolean BY_COLS = false;

    protected static String PATH = "StaticMandel.png";


    protected static Thread[] workers;
    protected static int TASK_WIDTH = WIDTH / COLS;
    protected static int TASK_HEIGHT = HEIGHT / ROWS;

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
            int granularity = Integer.parseInt(cmd.getOptionValue("g"));
            if (BY_COLS) {
                COLS = granularity * NUMBER_OF_THREADS;
                ROWS = 1;
            } else {
                ROWS = granularity * NUMBER_OF_THREADS;
                COLS = 1;
            }
        }
        PALETTE = new int[MAX_ITERATIONS];
        PIXEL_ARRAY = new int[WIDTH][HEIGHT];
        NUMBER_OF_TASKS = ROWS * COLS;
        TASK_WIDTH = WIDTH / COLS;
        TASK_HEIGHT = HEIGHT / ROWS;
    }

    public static void main(String[] args) {
        try {
            addOptions(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            PALETTE[i] = Color.HSBtoRGB(((94 + 1.2f * (float) Math.log(i) * (float) Math.sqrt(i)) / 256f), 0.65f, i / (i + 3.5f));
        }

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphic = image.createGraphics();
        graphic.fillRect(0, 0, WIDTH, HEIGHT);

        long startTime = System.currentTimeMillis();

        workers = new Thread[NUMBER_OF_THREADS];
        for (int workerIndex = 1; workerIndex < NUMBER_OF_THREADS; workerIndex++) {
            Runnable r = new StaticWorker(workerIndex);
            Thread t = new Thread(r);
            t.start();
            workers[workerIndex] = t;
        }

        new StaticWorker(0).run();

        for (int workerIndex = 1; workerIndex < NUMBER_OF_THREADS; workerIndex++) {
            try {
                workers[workerIndex].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long endTime = System.currentTimeMillis();

        for (int x = 0; x < WIDTH; ++x) {
            for (int y = 0; y < HEIGHT; ++y) {
                if (PIXEL_ARRAY[x][y] < MAX_ITERATIONS) {
                    image.setRGB(x, y, PALETTE[PIXEL_ARRAY[x][y]]);
                    continue;
                }
                image.setRGB(x, y, Color.WHITE.getRGB());
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
