package org.os;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;
import java.util.*;

class Parking_Lot
{
    private final Semaphore spots;
    private final AtomicInteger total_served_cars = new AtomicInteger(0);
    private final Map<String, AtomicInteger> gateServedCars = new HashMap<>();
    private final int total_spots;

    public Parking_Lot(int totalSpots)
    {
        this.spots = new Semaphore(totalSpots, true); // Fair semaphore
        this.total_spots = totalSpots;
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
        synchronized (this)
        {
            logStatus("Car " + carId + " from " + gateName + " arrived at time " + arrivalTime);
            if (getAvailableSpots() == 0)
            {
                logStatus("Car " + carId + " from " + gateName + " waiting for a spot.");
            }
        }

        try
        {
            if (spots.tryAcquire())
            {
                synchronized (this)
                {
                    logStatus("Car " + carId + " from " + gateName + " parked. (Parking Status: " + (total_spots - getAvailableSpots()) + " spots occupied)");
                }
                Thread.sleep(duration * 1000);
                spots.release();
                synchronized (this)
                {
                    logStatus("Car " + carId + " from " + gateName + " left after " + duration + " units of time. (Parking Status: " + (total_spots - getAvailableSpots()) + " spots occupied)");
                }
            }
            else
            {
                spots.acquire();
                long waitTime = (long) Math.ceil((System.currentTimeMillis() - waitStartTime) / 1000.0);
                synchronized (this)
                {
                    logStatus("Car " + carId + " from " + gateName + " parked after waiting for " + waitTime + " units of time. (Parking Status: " + (total_spots - getAvailableSpots()) + " spots occupied)");
                }
                Thread.sleep(duration * 1000);
                spots.release();
                synchronized (this)
                {
                    logStatus("Car " + carId + " from " + gateName + " left after " + duration + " units of time. (Parking Status: " + (total_spots - getAvailableSpots())  + " spots occupied)");
                }
            }
        }
        catch (InterruptedException e)
        {
            logStatus("Car " + carId + " from " + gateName + " was interrupted.");
        }

        total_served_cars.incrementAndGet();
        gateServedCars.computeIfAbsent(gateName, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public int getTotalServedCars()
    {
        return total_served_cars.get();
    }

    public Map<String, Integer> getGateStatistics()
    {
        Map<String, Integer> stats = new HashMap<>();
        for (Map.Entry<String, AtomicInteger> entry : gateServedCars.entrySet())
        {
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
    private final Parking_Lot parkingLot;

    public Car(int carId, String gateName, int arrivalTime, int parkingDuration, Parking_Lot parkingLot)
    {
        this.carId = carId;
        this.gateName = gateName;
        this.arrivalTime = arrivalTime;
        this.parkingDuration = parkingDuration;
        this.parkingLot = parkingLot;
    }

    public int getArrivalTime()
    {
        return arrivalTime;
    }

    @Override
    public void run()
    {
        try
        {
            Thread.sleep(arrivalTime * 1000); // simulate arrival time
            parkingLot.parkCar(carId, gateName, arrivalTime, parkingDuration);
        }
        catch (InterruptedException e) {
            System.out.println("Car " + carId + " was interrupted during arrival.");
        }
    }
}

public class ParkingSimulation
{
    public static void main(String[] args)
    {
        Parking_Lot parkingLot = new Parking_Lot(4); // 4 parking spots
        List<Thread> gates = new ArrayList<>();

        PriorityQueue<Car> carQueue = new PriorityQueue<>(Comparator.comparingInt(Car::getArrivalTime));

        try (BufferedReader br = new BufferedReader(new FileReader("file.txt")))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                String[] parts = line.split(",\\s*");
                String gateName = parts[0];
                int carId = Integer.parseInt(parts[1].split(" ")[1]);
                int arrivalTime = Integer.parseInt(parts[2].split(" ")[1]);
                int parkingDuration = Integer.parseInt(parts[3].split(" ")[1]);

                Car car = new Car(carId, gateName, arrivalTime, parkingDuration, parkingLot);
                carQueue.add(car);
            }
        }
        catch (IOException e)
        {
            System.err.println("Error reading the file: " + e.getMessage());
        }

        while (!carQueue.isEmpty())
        {
            Car car = carQueue.poll();
            Thread carThread = new Thread(car);
            gates.add(carThread);
            carThread.start();
        }

        for (Thread gate : gates)
        {
            try
            {
                gate.join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        System.out.println("...");
        System.out.println("Total Cars Served: " + parkingLot.getTotalServedCars());
        System.out.println("Current Cars in Parking: " + (4 - parkingLot.getAvailableSpots()));
        System.out.println("Details:");
        Map<String, Integer> gateStats = parkingLot.getGateStatistics();
        for (Map.Entry<String, Integer> entry : gateStats.entrySet())
        {
            System.out.println("- " + entry.getKey() + " served " + entry.getValue() + " cars.");
        }
    }
}
