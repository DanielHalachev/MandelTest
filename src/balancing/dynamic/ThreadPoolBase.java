package balancing.dynamic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ThreadPoolBase {

    private final BlockingQueue<Runnable> taskQueue;
    private final List<DynamicWorker> workers = new ArrayList<>();
    private boolean isStopped;

    public ThreadPoolBase(int allThreadsCount, int allTasksCount) {
        this.taskQueue = new ArrayBlockingQueue<>(allTasksCount);
        this.isStopped = false;

        for (int i = 0; i < allThreadsCount; ++i) {
            this.workers.add(new DynamicWorker(this.taskQueue));
        }
        for (DynamicWorker runnable : this.workers) {
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
        for (DynamicWorker runnable : this.workers) {
            runnable.doStop(quiet);
        }
    }

    public synchronized void killThreads() {
        for (DynamicWorker runnable : this.workers) {
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
