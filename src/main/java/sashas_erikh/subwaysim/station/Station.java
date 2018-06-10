package sashas_erikh.subwaysim.station;

import sashas_erikh.subwaysim.passenger.Passenger;

import rmacdonald_kingsu.retailqueuing.RNGArrivalTime;

import java.util.Queue;
import java.util.LinkedList;

/**
 * A subway station.
 * @author Erik H
 */
public class Station {
	/**
	 * The name of the Station.
	 */
	private String name;
	/**
	 * Mean time between passenger spawns.
	 */
	private double timeBetweenSpawns;
	/**
	 * If a train is currently occupying the station.
	 */
	private boolean occupied;
	/**
	 * Passengers waiting at the station.
	 */
	Queue<Passenger> stationQueue;
	/**
	 * The RNG for exponential distribution draws for passenger arrivals.
	 */
	RNGArrivalTime rng;
	/**
	 * The arrival time of the next passenger at this station.
	 */
	double tnext;

	/**
	 * Create a subway station.
	 * @param name the station's human readable name
	 * @param timeBetweenSpawns the station's mean time between spawns, in seconds
	 * @param seed the seed for the arrival RNG
	 */
	public Station(String name, double timeBetweenSpawns, int seed) {
		this.name = name;
		this.timeBetweenSpawns = timeBetweenSpawns;

		rng = new RNGArrivalTime(timeBetweenSpawns, seed);
		stationQueue = new LinkedList<Passenger>();
	}

	/**
	 * Draw for when the next passenger spawns.
	 * Our RNG gives us the time until the next arrival, so the sum of the
	 * current time and this time gives us the actual time when the next Passenger
	 * should spawn.
	 * @param currentTime the current global time
	 */
	public double drawNextTime(double currentTime) {
		tnext = currentTime + rng.nextTime();
		return tnext;
	}

	/**
	 * Add a Passenger to this Station's queue.
	 * @param spawnTime when the Passenger spawned, in seconds
	 * @param dest the Passenger's destination Station
	 */
	public void enqueuePassenger(double spawnTime, Station dest) {
		stationQueue.add(new Passenger(spawnTime, this, dest));
	}

	/**
	 * @return the time when the next Passenger at this Station will spawn
	 */
	public double getNextTime() {
		return tnext;
	}

	/**
	 * Check if this station is occupied by a train.
	 * @return whether this station is occupied
	 */
	public boolean isOccupied() {
		return occupied;
	}

	/**
	 * Set the occupied status of the station.
	 * @param occupied the true/false value for occupation
	 */
	public void setOccupied(boolean occupied) {
		this.occupied = occupied;
	}

	/**
	 * Get this station's time between Passenger spawns.
	 * @return the mean time between spawns
	 */
	public double getTimeBetweenSpawns() {
		return timeBetweenSpawns;
	}

	/**
	 * @return this station's passenger queue
	 */
	public Queue<Passenger> getStationQueue() {
		return stationQueue;
	}

	/**
	 * @return this Station's name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get a string representation of a station.
	 * @return a String containing the station's name
	 */
	public String toString() {
		return "[" + name + ", " + timeBetweenSpawns + "]";
	}
}
