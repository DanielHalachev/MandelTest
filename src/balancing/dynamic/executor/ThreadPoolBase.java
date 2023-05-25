package balancing.dynamic.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ThreadPoolBase {

    private final BlockingQueue<Runnable> taskQueue;
    private final List<Worker> workers = new ArrayList<>();
    private boolean isStopped;

    public ThreadPoolBase(int allThreadsCount, int allTasksCount) {
        this.taskQueue = new ArrayBlockingQueue<>(allTasksCount);
        this.isStopped = false;

        for (int i = 0; i < allThreadsCount; ++i) {
            this.workers.add(new Worker(this.taskQueue));
        }
        for (Worker runnable : this.workers) {
            new Thread(runnable).start();
        }
    }

    public synchronized void execute(Runnable task) {
        if (this.isStopped) {
            throw new IllegalStateException("ThreadPoolBase has been stopped");
        }
        this.taskQueue.offer(task);
    }

    public synchronized void stop(boolean quiet) {
        this.isStopped = true;
        for (Worker runnable : this.workers) {
            runnable.doStop(quiet);
        }
    }

    public synchronized void killThreads() {
        for (Worker runnable : this.workers) {
            runnable.inter();
        }
    }

    public synchronized void waitUntilAllTasksFinished() {
        while (!this.taskQueue.isEmpty()) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
