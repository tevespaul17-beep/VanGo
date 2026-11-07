import java.util.List;

public class Booking {
    private String referenceNo;
    private Schedule schedule;
    private List<Integer> selectedSeats;
    private String passengerName;
    private String passengerContact;
    private double totalAmount;
    private String status;

    public Booking(String referenceNo, Schedule schedule, List<Integer> selectedSeats, String passengerName, String passengerContact, double totalAmount) {
        this.referenceNo = referenceNo;
        this.schedule = schedule;
        this.selectedSeats = selectedSeats;
        this.passengerName = passengerName;
        this.passengerContact = passengerContact;
        this.totalAmount = totalAmount;
        this.status = "PENDING PAYMENT";
    }

    public String getReferenceNo() { return referenceNo; }
    public Schedule getSchedule() { return schedule; }
    public List<Integer> getSelectedSeats() { return selectedSeats; }
    public String getPassengerName() { return passengerName; }
    public String getPassengerContact() { return passengerContact; }
    public double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }
}