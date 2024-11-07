package org.os;
import java.util.concurrent.Semaphore;

class ParkingLot {
    private final Semaphore spots;
    private int totalServedCars = 0;

    public ParkingLot(int totalSpots) {
        this.spots = new Semaphore(totalSpots);
    }

    public void parkCar(int carId, String gateName, int duration) {
        try {
            System.out.println("Car " + carId + " from " + gateName + " arrived.");
            if (spots.tryAcquire()) {
                System.out.println("Car " + carId + " from " + gateName + " parked. (Parking Status: " + (4 - spots.availablePermits()) + " spots occupied)");
                Thread.sleep(duration * 1000); // simulate parking duration
                spots.release();
                System.out.println("Car " + carId + " from " + gateName + " left after " + duration + " units of time. (Parking Status: " + (4 - spots.availablePermits()) + " spots occupied)");
                synchronized (this) {
                    totalServedCars++;
                }
            } else {
                System.out.println("Car " + carId + " from " + gateName + " waiting for a spot.");
                spots.acquire(); // waits for a parking spot to become available
                System.out.println("Car " + carId + " from " + gateName + " parked after waiting. (Parking Status: " + (4 - spots.availablePermits()) + " spots occupied)");
                Thread.sleep(duration * 1000); // simulate parking duration
                spots.release();
                System.out.println("Car " + carId + " from " + gateName + " left after " + duration + " units of time. (Parking Status: " + (4 - spots.availablePermits()) + " spots occupied)");
                synchronized (this) {
                    totalServedCars++;
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Car " + carId + " from " + gateName + " was interrupted.");
        }
    }

    public int getTotalServedCars() {
        return totalServedCars;
    }
}

class Car implements Runnable {
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
    public void run() {
        try {
            Thread.sleep(arrivalTime * 1000); // simulate arrival time
            parkingLot.parkCar(carId, gateName, parkingDuration);
        } catch (InterruptedException e) {
            System.out.println("Car " + carId + " was interrupted during arrival.");
        }
    }
}

public class ParkingSimulation {
    public static void main(String[] args) {
        ParkingLot parkingLot = new ParkingLot(4);

        Thread[] gates = new Thread[] {
                new Thread(new Car(0, "Gate 1", 0, 3, parkingLot)),
                new Thread(new Car(1, "Gate 1", 1, 4, parkingLot)),
                new Thread(new Car(2, "Gate 1", 2, 2, parkingLot)),
                new Thread(new Car(5, "Gate 2", 3, 4, parkingLot)),
                new Thread(new Car(10, "Gate 3", 2, 4, parkingLot)),
                // Add more car threads based on the input
        };

        for (Thread gate : gates) {
            gate.start();
        }

        for (Thread gate : gates) {
            try {
                gate.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Total Cars Served: " + parkingLot.getTotalServedCars());
    }
}
