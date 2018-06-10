package sashas_erikh.subwaysim.passenger;

import sashas_erikh.subwaysim.station.Station;

/**
 * A passenger travelling in a subway system.
 * @author Erik H
 */
public class Passenger {
	/**
	 * The global time at which this passenger was "spawned"/began waiting at a
	 * station.
	 */
	private double spawnTime;
	/**
	 * The station where this passenger spawned.
	 */
	private Station spawnLocation;
	/**
	 * The time when this passenger boarded the train.
	 */
	private double boardTime;
	/**
	 * The end destination station for this passenger.
	 */
	private Station destination;

	/**
	 * Create a passenger.
	 * @param spawnTime the passenger's spawn time, from the global clock
	 * @param destination the station where this passenger wants to go
	 */
	public Passenger(double spawnTime, Station spawnLocation, Station destination) {
		this.spawnTime = spawnTime;
		this.spawnLocation = spawnLocation;
		this.destination = destination;
		boardTime = -1;
	}

	/**
	 * Get the time when this passenger was spawned at a station.
	 * @return the spawn time
	 */
	public double getSpawnTime() {
		return spawnTime;
	}

	/**
	 * Set this Passenger's boarding time
	 * @param boardTime the boarding time, in seconds
	 */
	public void setBoardTime(double boardTime) {
		this.boardTime = boardTime;
	}

	/**
	 * @return the time when this Passenger boarded, in seconds
	 */
	public double getBoardTime() {
		return boardTime;
	}

	/**
	 * Get this passenger's desired end destination.
	 * @return the destination Station
	 */
	public Station getDestination() {
		return destination;
	}

	/**
	 * @return get the Station where this Passenger spawned
	 */
	public Station getSpawnLocation() {
		return spawnLocation;
	}

	/**
	 * @return a String representation of this Passenger, with their spawn
	 * time, boarding time, and goal Destination
	 */
	public String toString() {
		return "~" + spawnTime + "," + boardTime + "," + destination.getName() + "~";
	}
}
