public class Event implements Comparable<Event> {

    private final double eventTime;
    private final EventType type;
    private final Passenger passenger;

    public Event(double eventTime, EventType type, Passenger passenger) {
        this.eventTime = eventTime;
        this.type = type;
        this.passenger = passenger;
    }

    public double getEventTime() {
        return eventTime;
    }

    public EventType getType() {
        return type;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    @Override
    public int compareTo(Event other) {
        return Double.compare(this.eventTime, other.eventTime);
    }
}
