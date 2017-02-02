package se.lup.turbine.discovery.amqp;

/**
 * Encapsulates a single discovery token message from a hystrix-producer.
 */
public class DiscoveryToken {

    /**
     * String-rep of {@link AmqpInstance.Status}
     */
    private String state;

    /**
     * Typically something like http://some.ip.address:8181/hystrix.stream
     */
    private String address;

    public DiscoveryToken() {
    }

    public DiscoveryToken(String state, String address) {
        this.state = state;
        this.address = address;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
