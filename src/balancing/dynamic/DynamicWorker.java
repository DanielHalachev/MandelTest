package balancing.dynamic;

import java.util.concurrent.BlockingQueue;

public class DynamicWorker implements Runnable {

    private Thread thread = null;
    private final BlockingQueue<Runnable> taskQueue;
    private boolean isStopped;
    private int countTasks;

    public void inter() {
        this.thread.interrupt();
    }

    public DynamicWorker(BlockingQueue<Runnable> queue) {
        this.taskQueue = queue;
        this.isStopped = false;
        this.countTasks = 0;
    }

    public void run() {
        this.thread = Thread.currentThread();
        while (!isStopped()) {
            try {
                Runnable runnable = taskQueue.take();
                countTasks++;
                runnable.run();
            } catch (Exception e) {
                //we can log or note the exception in other way, but we should keep the thread alive.
            }
        }
    }

    public synchronized void doStop(boolean quiet) {
        isStopped = true;
        //make current thread stop calling taskQueue
        if (!quiet) {
            System.out.println(countTasks + " tasks have been done by this thread.");
        }
        // this.thread.interrupt();
    }

    public synchronized boolean isStopped() {
        return isStopped;
    }
}