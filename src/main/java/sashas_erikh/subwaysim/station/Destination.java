package sashas_erikh.subwaysim.station;

/**
 * A destination in a subway track.
 * Destinations consist of a `to` station as well as how long
 * it takes to get there.
 * @author Sasha S
 */
public class Destination {
	/**
	 * The destination station.
	 */
	private Station destStation;
	/**
	 * The time it takes to get to the destination station.
	 */
	private double travelTime;

	/**
	 * Create a destination.
	 * @param destStation the destination station
	 * @param travelTime the time it takes to get to the destination station, in seconds
	 */
	public Destination(Station destStation, double travelTime) {
		this.destStation = destStation;
		this.travelTime = travelTime;
	}

	/**
	 * Get the destination station.
	 */
	public Station getDestStation() {
		return destStation;
	}

	/**
	 * Get the travel time to the destination station.
	 */
	public double getTravelTime() {
		return travelTime;
	}

	/**
	 * @return a String representation of this Destination; the Station and time to that Station are included.
	 */
	public String toString() {
		return "{" + destStation + ", " + travelTime + "secs}";
	}
}
