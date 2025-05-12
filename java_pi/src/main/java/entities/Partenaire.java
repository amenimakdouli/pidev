package entities;

public class Partenaire {
    private int id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String website;

    // Default constructor
    public Partenaire() {
    }

    // Constructor with all fields
    public Partenaire(int id, String name, String email, String phone, String address, String website) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.website = website;
    }

    // Constructor without id (for new entities)
    public Partenaire(String name, String email, String phone, String address, String website) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.website = website;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    // toString method for debugging
    @Override
    public String toString() {
        return "Partenaire{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", website='" + website + '\'' +
                '}';
    }
}
