package balancing.dynamic.pool;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DynamicMandelTest {

    protected static int WIDTH = 3840;
    protected static int HEIGHT = 2160;
    protected static int MAX_ITERATIONS = 1024;
    protected static int[] PALETTE = new int[MAX_ITERATIONS];
    protected static double[] DIMENSIONS = {-1.8, 0.45, -1.1, 1.1};
    protected static byte[][] PIXEL_ARRAY;
    protected static int NUMBER_OF_THREADS = 1;
    protected static int GRANULARITY = 1;
    protected static int NUMBER_OF_TASKS;
    protected static int TASK_WIDTH;
    protected static String PATH = "Mandelbrot.png";

    protected static long getTimeInMillis() {
        return System.currentTimeMillis();
    }

    static private void addOptions(String[] args) throws Exception {
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

        if (cmd.hasOption("i")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("./runMe.sh [OPTIONS]", opt);
        }

        if (cmd.hasOption("g")) {
            GRANULARITY = Integer.parseInt(cmd.getOptionValue("g"));
        }
        NUMBER_OF_TASKS = GRANULARITY * NUMBER_OF_THREADS;
        PALETTE = new int[MAX_ITERATIONS];
        TASK_WIDTH = (int) Math.ceil((float) HEIGHT / NUMBER_OF_TASKS);
    }


    public static void main(String[] args) {
        try {
            addOptions(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            PALETTE[i] = Color.HSBtoRGB((200f + i * 2f) / 256f, 1, i / (i + 8f));
        }

        PALETTE[127] = Color.BLACK.getRGB();

        BufferedImage bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bi.createGraphics();
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        long startTime = getTimeInMillis();

        ExecutorService pool = Executors.newFixedThreadPool(NUMBER_OF_THREADS - 1);

        DynamicWorker[] tasks = new DynamicWorker[NUMBER_OF_TASKS];
        for (int i = 0; i < NUMBER_OF_TASKS; i++) {
            tasks[i] = new DynamicWorker(i);
        }

        for (int i = 0; i < NUMBER_OF_TASKS; i++) {
            pool.execute(tasks[i]);
        }

        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = getTimeInMillis();

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (PIXEL_ARRAY[x][y] < 0) {
                    PIXEL_ARRAY[x][y] += 128;
                }
                bi.setRGB(x, y, PALETTE[PIXEL_ARRAY[x][y]]);
            }
        }

        try {
            ImageIO.write(bi, "PNG", new File(PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Total execution time: " + (endTime - startTime) + " ms.");
    }
}