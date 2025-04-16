import java.util.*;
import java.util.concurrent.locks.*;
import java.io.*;

public class Main {

    // Shared task queue
    static class TaskQueue {
        private Queue<String> queue = new LinkedList<>();
        private final ReentrantLock lock = new ReentrantLock();

        public void addTask(String task) {
            lock.lock();
            try {
                queue.add(task);
            } finally {
                lock.unlock();
            }
        }

        public String getTask() {
            lock.lock();
            try {
                return queue.poll();
            } finally {
                lock.unlock();
            }
        }

        public boolean isEmpty() {
            lock.lock();
            try {
                return queue.isEmpty();
            } finally {
                lock.unlock();
            }
        }
    }

    // Worker class
    static class Worker extends Thread {
        private TaskQueue taskQueue;
        private BufferedWriter writer;
        private int workerId;

        public Worker(TaskQueue taskQueue, BufferedWriter writer, int id) {
            this.taskQueue = taskQueue;
            this.writer = writer;
            this.workerId = id;
        }

        @Override
        public void run() {
            try {
                log("Worker " + workerId + " started.");
                while (true) {
                    String task = taskQueue.getTask();
                    if (task == null) {
                        break;
                    }
                    processTask(task);
                }
                log("Worker " + workerId + " completed.");
            } catch (Exception e) {
                log("Worker " + workerId + " error: " + e.getMessage());
            }
        }

        private void processTask(String task) {
            try {
                // Simulate processing time
                Thread.sleep(500);
                String result = "Processed by worker " + workerId + ": " + task;
                synchronized (writer) {
                    writer.write(result);
                    writer.newLine();
                    writer.flush();
                }
            } catch (Exception e) {
                log("Worker " + workerId + " processing error: " + e.getMessage());
            }
        }

        private void log(String message) {
            System.out.println("[LOG] " + message);
        }
    }

    public static void main(String[] args) {
        TaskQueue taskQueue = new TaskQueue();

        // Populate queue with sample tasks
        for (int i = 1; i <= 20; i++) {
            taskQueue.addTask("Task " + i);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"))) {
            int numWorkers = 4;
            List<Worker> workers = new ArrayList<>();

            for (int i = 1; i <= numWorkers; i++) {
                Worker worker = new Worker(taskQueue, writer, i);
                workers.add(worker);
                worker.start();
            }

            // Wait for all threads to complete
            for (Worker worker : workers) {
                worker.join();
            }

            System.out.println("All tasks completed. Results written to output.txt.");

        } catch (IOException | InterruptedException e) {
            System.out.println("Main error: " + e.getMessage());
        }
    }
}
