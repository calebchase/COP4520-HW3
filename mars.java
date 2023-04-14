import java.sql.Timestamp;
import java.time.*;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

class mars {
    static int THREAD_COUNT = 8;

    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    Lock readLock = lock.readLock();

    Instant start = Instant.now();
    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

    public static void main(String[] args) {
        AtomicInteger writes = new AtomicInteger(0);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);
        SharedMemeory sharedMemeory = new SharedMemeory(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            scheduler.scheduleAtFixedRate(
                    new SensorHandler(writes, sharedMemeory, i), 0, 1, TimeUnit.SECONDS);
        }
    }
}

class SharedMemeory {
    int[] sharedDataArray;
    FixSizedHeap minHeap;
    FixSizedHeap maxHeap;
    int timeElapsed;
    int maxDiffInterval;
    int maxDiff;
    int[] currentSensorDiffs;

    SharedMemeory(int threadCount) {
        sharedDataArray = new int[threadCount];
        maxHeap = new FixSizedHeap(5, true);
        minHeap = new FixSizedHeap(5, false);
        currentSensorDiffs = new int[threadCount];
    }

    void WriteData(int data, int i) {
        int tempDiff = Math.abs(data - sharedDataArray[i]);
        currentSensorDiffs[i] = Math.max(tempDiff, currentSensorDiffs[i]);
        sharedDataArray[i] = data;
        sharedDataArray[i] = data;
    }

    void ClearMaxData() {
        maxDiff = 0;
        for (int i = 0; i < currentSensorDiffs.length; i++) {
            currentSensorDiffs[i] = 0;
        }
    }
}

class FixSizedHeap {
    int maxSize;
    private PriorityQueue<Integer> queue;

    FixSizedHeap(int maxSize, boolean isMax) {
        this.maxSize = maxSize;
        if (isMax)
            queue = new PriorityQueue<Integer>();
        else
            queue = new PriorityQueue<Integer>(Collections.reverseOrder());
    }

    void Add(int ele) {
        queue.add(ele);
        if (queue.size() > maxSize)
            queue.poll();
    }

    int Take() {
        return queue.poll();
    }
}

class SensorHandler implements Runnable {
    int sensorIndex;
    AtomicInteger writes;
    SharedMemeory sharedMemory;
    Stack<Integer> maxTemps = new Stack<Integer>();

    SensorHandler(AtomicInteger writes, SharedMemeory sharedMemeory, int sensorIndex) {
        this.writes = writes;
        this.sharedMemory = sharedMemeory;
        this.sensorIndex = sensorIndex;
    }

    int ReadSensorData() {
        int[] range = { -70, 100 };
        return (int) (Math.random() * (range[1] - range[0]) + range[0]);
    }

    void WriteSensorData() {
        sharedMemory.WriteData(ReadSensorData(), sensorIndex);
        writes.addAndGet(1);
    }

    void CheckReport() {
        if (sharedMemory.timeElapsed % 1 == 0) {
            int tempMax = 0;
            for (int i = 0; i < sharedMemory.currentSensorDiffs.length; i++) {
                tempMax = Math.max(tempMax, sharedMemory.currentSensorDiffs[i]);
            }
            if (tempMax > sharedMemory.maxDiff) {
                sharedMemory.maxDiff = tempMax;
                sharedMemory.maxDiffInterval = sharedMemory.timeElapsed / 3;
            }
        }

        if (sharedMemory.timeElapsed % 6 == 0) {
            sharedMemory.timeElapsed = 0;
            System.out.println("Report for last 60 minutes");

            System.out.println("Highest 5 temps:");
            for (int i = 0; i < sharedMemory.maxHeap.maxSize; i++) {
                System.out.print(sharedMemory.maxHeap.Take() + " ");
            }
            System.out.println();

            System.out.println("Lowest 5 temps:");
            for (int i = 0; i < sharedMemory.maxHeap.maxSize; i++) {
                System.out.print(sharedMemory.minHeap.Take() + " ");
            }
            System.out.println();

            System.out.println("The greatest temp change for a sensor was " + sharedMemory.maxDiff
                    + " and occured in 10 minute interval "
                    + sharedMemory.maxDiffInterval);
            sharedMemory.ClearMaxData();

            System.out.println();
        }
    }

    void CheckMinMaxData() {
        for (int i = 0; i < sharedMemory.sharedDataArray.length; i++) {
            sharedMemory.maxHeap.Add(sharedMemory.sharedDataArray[i]);
            sharedMemory.minHeap.Add(sharedMemory.sharedDataArray[i]);
        }
    }

    void CheckMaxInterval() {

    }

    @Override
    public void run() {
        WriteSensorData();
        // The first thread to see that reading sensor data has finished
        if (writes.get() == 8) {
            sharedMemory.timeElapsed++;
            writes.set(0);
            CheckMinMaxData();
            CheckReport();
        }
    }
}
