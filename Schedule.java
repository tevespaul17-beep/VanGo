import java.util.HashSet;
import java.util.Set;

public class Schedule {
    public enum DispatchState {
        SCHEDULED,
        AWAITING_DRIVER_DEPARTURE_APPROVAL,
        DISPATCHED
    }

    private String id;
    private String destination;
    private String departureTime;
    private double fare;
    private Van van;
    private Set<Integer> bookedSeats = new HashSet<>();

    private DispatchState state = DispatchState.SCHEDULED;
    private boolean driverDepartureRequested = false;
    private String rejectionReason = "";

    public Schedule(String id, String destination, String departureTime, double fare, Van van) {
        this.id = id;
        this.destination = destination;
        this.departureTime = departureTime;
        this.fare = fare;
        this.van = van;
    }

    public String getId() { return id; }
    public String getDestination() { return destination; }
    public String getDepartureTime() { return departureTime; }
    public double getFare() { return fare; }
    public Van getVan() { return van; }
    public void setVan(Van van) { this.van = van; }

    public Set<Integer> getBookedSeats() { return bookedSeats; }
    public boolean isSeatBooked(int seat) { return bookedSeats.contains(seat); }
    public void bookSeat(int seat) { bookedSeats.add(seat); }

    public int getBookedCount() { return bookedSeats.size(); }
    public int getCapacity() { return van != null ? van.getCapacity() : 0; }
    public int getRemainingSeats() { return getCapacity() - getBookedCount(); }
    public boolean isFull() { return getBookedCount() >= getCapacity(); }

    public DispatchState getState() { return state; }
    public void setState(DispatchState state) { this.state = state; }

    public boolean isDispatched() { return state == DispatchState.DISPATCHED; }
    public void setDispatched(boolean dispatched) {
        this.state = dispatched ? DispatchState.DISPATCHED : DispatchState.SCHEDULED;
    }

    public boolean getDriverDepartureRequested() { return driverDepartureRequested; }
    public void setDriverDepartureRequested(boolean requested) {
        this.driverDepartureRequested = requested;
        if (requested && state != DispatchState.DISPATCHED) {
            this.state = DispatchState.AWAITING_DRIVER_DEPARTURE_APPROVAL;
        }
    }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}