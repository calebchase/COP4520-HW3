# Problem 1
Problem 1 was not provided due to implementation issues. 

# Problem 2
Usage:
- javac mars.java
- java mars

To dispatch threads, java's newScheduledThreadPool is used. This allows threads to be called periodically. The time periods have been scaled down so that every second that passes represents 10 simulated minutes. 

When writting to shared memory in parallel, each thread is given a unique index to access the shared memory. An atomic integer is used to track the number of writes to shared memory, allowing threads to see when all writes of sensor data to memory have finished. The first thread to see that all threads have writen the sensor data to memory will manage the report generation and tracking of statistics.

This program assumes that "largest temperature difference observed" refers to the largest difference between to consecutive readings done by the same sensor.

To ensure data collection of sensors is done as quickly as possible, each thread reads from a selected sensor in parallel. There are no cases in which dead locks would occur in the program so progress guaranteed. To test for correctness of the program, small test cases and program state monitoring were used. 