package four_k.coinz;

public class Coin {

    private String id;
    private double value;
    private String currency;
    private double longitude;
    private double latitude;

    public Coin() {}

    public Coin(String id, double value, String currency, double longitude, double latitude){
        this.id = id;
        this.value = value;
        this.currency = currency;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getId() {
        return id;
    }

    public double getValue() {
        return value;
    }

    public String getCurrency() {
        return currency;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }
}