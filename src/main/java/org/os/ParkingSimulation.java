package org.os;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ParkingLot {
    private final Semaphore spots;
    private final AtomicInteger totalServedCars = new AtomicInteger(0);
    private final Map<String, AtomicInteger> gateServedCars = new HashMap<>();
    private final int totalSpots;

    public ParkingLot(int totalSpots)
    {
        this.spots = new Semaphore(totalSpots);
        this.totalSpots = totalSpots;
    }

    public synchronized int getAvailableSpots()
    {
        return spots.availablePermits();
    }

    public synchronized void logStatus(String message)
    {
        System.out.println(message);
    }

    public void parkCar(int carId, String gateName, int arrivalTime, int duration)
    {
        long waitStartTime = System.currentTimeMillis();
        logStatus("Car " + carId + " from " + gateName + " arrived at time " + arrivalTime);

        try {
            if (spots.tryAcquire())
            {
                long waitTime = (System.currentTimeMillis() - waitStartTime) / 1000;
                logStatus("Car " + carId + " from " + gateName + " parked. (Parking Status: "
                        + (totalSpots - getAvailableSpots()) + " spots occupied)");
                Thread.sleep(duration * 1000); // simulate parking duration
                spots.release();
                logStatus("Car " + carId + " from " + gateName + " left after " + duration
                        + " units of time. (Parking Status: " + (totalSpots - getAvailableSpots())
                        + " spots occupied)");
            }
            else {
                logStatus("Car " + carId + " from " + gateName + " waiting for a spot.");
                spots.acquire(); // waits for a parking spot to become available
                long waitTime = (System.currentTimeMillis() - waitStartTime) / 1000;
                logStatus("Car " + carId + " from " + gateName + " parked after waiting for "
                        + waitTime + " units of time. (Parking Status: "
                        + (totalSpots - getAvailableSpots()) + " spots occupied)");
                Thread.sleep(duration * 1000); // simulate parking duration
                spots.release();
                logStatus("Car " + carId + " from " + gateName + " left after " + duration
                        + " units of time. (Parking Status: " + (totalSpots - getAvailableSpots())
                        + " spots occupied)");
            }
        }
        catch (InterruptedException e) {
            logStatus("Car " + carId + " from " + gateName + " was interrupted.");
        }

        // Update statistics
        totalServedCars.incrementAndGet();
        gateServedCars.computeIfAbsent(gateName, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public int getTotalServedCars() {
        return totalServedCars.get();
    }

    public Map<String, Integer> getGateStatistics()
    {
        Map<String, Integer> stats = new HashMap<>();
        for (Map.Entry<String, AtomicInteger> entry : gateServedCars.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().get());
        }
        return stats;
    }
}

class Car implements Runnable
{
    private final int carId;
    private final String gateName;
    private final int arrivalTime;
    private final int parkingDuration;
    private final ParkingLot parkingLot;

    public Car(int carId, String gateName, int arrivalTime, int parkingDuration, ParkingLot parkingLot) {
        this.carId = carId;
        this.gateName = gateName;
        this.arrivalTime = arrivalTime;
        this.parkingDuration = parkingDuration;
        this.parkingLot = parkingLot;
    }

    @Override
    public void run()
    {
        try {
            Thread.sleep(arrivalTime * 1000); // simulate arrival time
            parkingLot.parkCar(carId, gateName, arrivalTime, parkingDuration);
        } catch (InterruptedException e) {
            System.out.println("Car " + carId + " was interrupted during arrival.");
        }
    }
}

public class ParkingSimulation
{
    public static void main(String[] args)
    {
        ParkingLot parkingLot = new ParkingLot(4); // 4 parking spots
        List<Thread> gates = new ArrayList<>();

        // Read from file.txt
        try (BufferedReader br = new BufferedReader(new FileReader("file.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Split the line based on commas and spaces
                String[] parts = line.split(",\\s*");
                String gateName = parts[0];
                int carId = Integer.parseInt(parts[1].split(" ")[1]);
                int arrivalTime = Integer.parseInt(parts[2].split(" ")[1]);
                int parkingDuration = Integer.parseInt(parts[3].split(" ")[1]);

                // Create a car thread
                Thread carThread = new Thread(new Car(carId, gateName, arrivalTime, parkingDuration, parkingLot));
                gates.add(carThread);
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }

        // Start all car threads
        for (Thread gate : gates) {
            gate.start();
        }

        // Wait for all threads to finish
        for (Thread gate : gates) {
            try {
                gate.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("...");
        System.out.println("Total Cars Served: " + parkingLot.getTotalServedCars());
        System.out.println("Current Cars in Parking: " + (4 - parkingLot.getAvailableSpots()));

        // Print detailed gate statistics
        System.out.println("Details:");
        Map<String, Integer> gateStats = parkingLot.getGateStatistics();
        for (Map.Entry<String, Integer> entry : gateStats.entrySet()) {
            System.out.println("-" + entry.getKey() + " served " + entry.getValue() + " cars.");
        }
    }
}
