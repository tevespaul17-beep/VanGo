public class Driver {
    private String id;
    private String name;
    private String licenseNo;
    private String phone;

    public Driver(String id, String name, String licenseNo, String phone) {
        this.id = id;
        this.name = name;
        this.licenseNo = licenseNo;
        this.phone = phone;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLicenseNo() { return licenseNo; }
    public String getPhone() { return phone; }

    @Override
    public String toString() {
        return name + " (" + licenseNo + ")";
    }
}