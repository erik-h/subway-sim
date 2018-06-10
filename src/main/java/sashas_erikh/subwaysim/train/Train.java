package sashas_erikh.subwaysim.train;

import sashas_erikh.subwaysim.main.SubwaySim;
import sashas_erikh.subwaysim.passenger.Passenger;
import sashas_erikh.subwaysim.passenger.PassengerList;
import sashas_erikh.subwaysim.station.Station;
import sashas_erikh.subwaysim.station.Destination;

import java.util.Map;
import java.util.Queue;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * This class represents a train in a subway system.
 * @author Sasha S
 * @author Erik H
 */
public class Train {

	TrainState state;
	/**
	 *	This tells us the last station the train stopped at.
	 *	It allows to determine the next  station based on the track Map
	 *	in the SubwaySim class.
	 */
	private Station lastVisited;

	/**
	 * The next station.
	 */
	private Destination currentDestination;

	/**
	 *The time needed to board/disembark a train.
	 */
	private double boardTime;

	/**
	 * The waiting time that a train is waiting at a station before leaving.
	 */
	private double maxWaitTime;

	/**
	 * Departure time stamp.
	 */
	private double tripStartTime;

	/**
	 * Time stamp when the train starts disembarking Passengers.
	 */
	private double disembarkStartTime;

	/**
	 * Time stamp when the train started waiting for new Passengers.
	 */
	private double waitingStartTime;

	/**
	 * The total time it took to disembark the people who needed to get off the train.
	 */
	private double totalDisembarkTime;

	/**
	 * Number of people boarding the train.
	 */
	private int currentlyBoarding;

	/**
	 * PrintWriter for passenger travel time output.
	 */
	private PrintWriter passengerWriter;

	/**
	 * PrintWriter for train manifest at time of Station departure output.
	 */
	private PrintWriter trainWriter;
	private String name;

	/**
	 * A linked list that contains all the passengers travelling on this train.
	 */
	PassengerList passengers;

	/**
	 * train constructor.
	 */
	public Train(String name, int capacity, Station startStation, Destination destination, double boardTime, double maxWaitTime, PrintWriter passengerWriter, String manifestBaseName) throws FileNotFoundException {

		this.name = name;
		this.boardTime = boardTime;
		this.maxWaitTime = maxWaitTime;
		this.passengerWriter = passengerWriter;

		try {
			this.trainWriter = new PrintWriter(new File(manifestBaseName + "_" + name + SubwaySim.TRAINSUFFIX));
		}
		catch (FileNotFoundException e) {
			throw e;
		}

		lastVisited = startStation;
		currentDestination = destination;
		passengers = new PassengerList(capacity);

		state = TrainState.BOARDING;
		waitingStartTime = 0.0;
		currentlyBoarding = 0;
	}

	/**
	 * Runs the train, used for travelling/disembarking/boarding actions.
	 * @param t the global time
	 * @param track the track this Train is on
	 * @return the PassengerList of Passengers that got off if the train arrived at a Station, otherwise null
	 */
	public PassengerList run(double t, Map<Station, Destination> track) {

		if (state == TrainState.TRAVELLING) {

			// arrived at the destination
			if (t - tripStartTime >= currentDestination.getTravelTime()) {
				System.err.println("[DEBUG] " + getName() + " has arrived at " + currentDestination.getDestStation() + " at t = " + t);
				currentDestination.getDestStation().setOccupied(true);

				// changes the state to disembarking, and updates the current/destination Stations.
				state = TrainState.DISEMBARKING;
				// Set our last visited station to the one we just arrived at
				lastVisited = currentDestination.getDestStation();
				// Set our new destination based to the next Station on the track
				currentDestination = track.get(lastVisited);
				int originalPassengerSize = passengers.size();
				System.err.println("\tPrior to passenger remove: " + passengers);
				PassengerList arrivedPassengers = passengers.removeForStation(lastVisited);

				/*
				 * It will take this many seconds for the Passengers who want to
				 * get off at this station to disembark.
				 */
				totalDisembarkTime = arrivedPassengers.size() * boardTime;
				disembarkStartTime = t;
				System.err.println("\t" + name + " STARTING disembarking at t = " + t);
				System.err.println("\tIt will take " + totalDisembarkTime + " to disembark " + arrivedPassengers.size() + "/" + originalPassengerSize + " passengers.");
				System.err.println("\tHere are the people that are left: " + passengers);

				storeTimes(t, arrivedPassengers); // Store the passenger arrival info in a file
				return arrivedPassengers; // Return the passengers that got off so we can count them
			}
		}

		else if (state == TrainState.DISEMBARKING) {
			// We have "finished" booting off passengers, and are ready to board
			if (t - disembarkStartTime >= totalDisembarkTime) {
				System.err.println("\t" + name + " FINISHED disembarking at t = " + t);
				state = TrainState.BOARDING;
				waitingStartTime = t;
				System.err.println("\t" + name + " STARTING boarding at t = " + t);
			}
		}

		else if (state == TrainState.BOARDING) {
			if (t - waitingStartTime >= maxWaitTime && currentlyBoarding == 0) {
				System.err.println("\t" + name + " FINISHED boarding (maybe; or we're waiting for a station to open up) at t = " + t);
				System.err.println("\tcurrentlyBoarding = " + currentlyBoarding);
				/*
				 * We've waited for as long as we can AND we're finished
				 * boarding (there can be an overlap of one passenger over our
				 * max waiting time), so we must leave if the next Station is
				 * unoccupied.
				 */
				leaveIfPossible(t);
			}
			// We have time to board people. Let's board as many as we can.
			else {
				Queue<Passenger> stationQueue = lastVisited.getStationQueue();

				// Board someone, keeping track of their boarding time.
				if (currentlyBoarding > 0) {
					System.err.println("[DEBUG] passengers.size() " + passengers.size() + ", currentlyBoarding " + currentlyBoarding);
					passengers.get(passengers.size()-currentlyBoarding).setBoardTime(t);
					System.err.println("\t[DEBUG] Set boarding time for passenger; they now look like: " + passengers.get(passengers.size()-currentlyBoarding));
					// We can only board one person per time step.
					currentlyBoarding--;
				}

				double timeWaited = t - waitingStartTime;
				double boardTimeLeft = maxWaitTime - timeWaited;
				/*
				 * How many people we can board given the time we have left
				 */
				double boardablePassengers = (boardTimeLeft / boardTime) - currentlyBoarding;
				System.err.println("\tcan board " + boardablePassengers + " passengers MAX");

				// We _can_ actually board at least one person
				if (boardablePassengers >= 1.0) {
					for (int i = 0; i < (int)boardablePassengers && stationQueue.size() > 0; i++) {
						// Add a passenger, breaking out of the loop if the PassengerList is full
						if (!passengers.add(stationQueue.remove())) {
							// The PassengerList is full, so we can't board anyone else
							break;
						}
						else {
							// We successfully boarded someone, so increment our counter.
							// This person's boarding time has not yet been taken into account;
							// on the next clock tick the time will be taken into account.
							currentlyBoarding++;
						}
					}
				}
			}
		}
		return null; //No passengers got off
	}

	/**
	 * @return if this train is completely filled with Passengers
	 */
	public boolean isFull() {
		return passengers.size() == passengers.getCapacity();
	}

	/**
	 * Leave the Station if the destination Station is unoccupied.
	 */
	private void leaveIfPossible(double t) {

		currentlyBoarding = 0;
		System.err.println("[DEBUG] " + getName() + " trying to leave from " + lastVisited.getName() + "...");
		System.err.println("\tI have " + passengers.size() + " passengers");
		System.err.println("\tThey look like: " + passengers);

		// The next station is available, so we can start our next trip
		if (!currentDestination.getDestStation().isOccupied()) {
			System.err.println("\tsuccessfully left at t = " + t);
			state = TrainState.TRAVELLING;
			lastVisited.setOccupied(false);
			tripStartTime = t;

			// Write the train manifest data based on the people we just picked up
			storeManifest(t);
		}
		else {
			System.err.println("[DEBUG] COULDN'T LEAVE! NEXT STATION IS OCCUPIED!");
		}
	}


	/**
	 * Store in a file the total trip times for the passengers who disembark
	 * the train.
	 */
	public void storeTimes(double t, PassengerList pl) {
		// Store the total trip time, arrival at station time, board time,
		// and destination station for each passenger.
		for (Passenger p : pl) {
			double totalTripTime = t - p.getSpawnTime();
			passengerWriter.println(
				totalTripTime + "," + p.getSpawnTime() + "," +
				p.getBoardTime() + "," + p.getSpawnLocation().getName() + "," +
				p.getDestination().getName());
		}
	}

	/**
	 * Writes the passenger count on the train to a file.
	 */
	public void storeManifest(double t) {
		// Store the number of Passengers, current Station, next Station, and
		// the global time (the time when we're leaving).
		trainWriter.println(
			passengers.size() + "," + lastVisited.getName() + "," +
			currentDestination.getDestStation().getName() + "," + t
		);
	}

	/**
	 * Set this Train's Destination
	 * @param currentDestination the Destination
	 */
	public void setCurrentDestination(Destination currentDestination) {
		this.currentDestination = currentDestination;
	}

	/**
	 * @return this Train's current Destination
	 */
	public Destination getCurrentDestination() {
		return currentDestination;
	}

	/**
	 * Set this Train's last visited Station
	 * @param lastVisited the Station
	 */
	public void setLastVisited(Station lastVisited) {
		this.lastVisited = lastVisited;
	}

	/**
	 * @return this Train's last visited Station
	 */
	public Station getLastVisited() {
		return lastVisited;
	}

	/**
	 * @return this Train's name
	 */
	public String getName() {
		return name;
	}

	public PrintWriter getTrainWriter() {
		return trainWriter;
	}

	/**
	 * @return a String representation of this Train; includes name, last station, and current destination
	 */
	public String toString() {
		return "|" + name + ", " + lastVisited + ", " + currentDestination + "|";
	}
}
