public class Van {
    private String id;
    private String model;
    private String plateNumber;
    private int capacity;
    private Driver driver;
    private String imagePath;
    private int tripsCompletedToday = 0;

    public Van(String id, String model, String plateNumber, int capacity, Driver driver, String imagePath) {
        this.id = id;
        this.model = model;
        this.plateNumber = plateNumber;
        this.capacity = capacity;
        this.driver = driver;
        this.imagePath = imagePath;
    }

    public String getId() { return id; }
    public String getModel() { return model; }
    public String getPlateNumber() { return plateNumber; }
    public int getCapacity() { return capacity; }
    public Driver getDriver() { return driver; }
    public void setDriver(Driver driver) { this.driver = driver; }
    public String getImagePath() { return imagePath; }

    public int getTripsCompletedToday() { return tripsCompletedToday; }
    public void incrementTripsCompleted() { this.tripsCompletedToday++; }

    @Override
    public String toString() {
        return model + " [" + plateNumber + "]";
    }
}