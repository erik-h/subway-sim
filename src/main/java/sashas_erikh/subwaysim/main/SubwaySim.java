package sashas_erikh.subwaysim.main;

import sashas_erikh.subwaysim.passenger.PassengerList;
import sashas_erikh.subwaysim.station.Station;
import sashas_erikh.subwaysim.station.Destination;
import sashas_erikh.subwaysim.train.Train;

import rmacdonald_kingsu.util.ConfigFile;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Set;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * Simulation of a subway system.
 * This class was based on rmacdonald_kingsu.retailqueuing.GroceryStoreSim and
 * rmacdonald_kingsu.plumbingnetwork.PlumbingNetwork.
 * @author Sasha S
 * @author Erik H
 */
public class SubwaySim {

	public static final String CONFIGSUFFIX = "_config.txt";
	private ConfigFile config; // The simulation options

	/**
	 * Used for drawing random destinations for each Passenger.
	 * NOTE: since Random is uniform, we're assuming all Stations are equally desirable/
	 * likely to be travelled to.
	 */
	private Random stationRNG;

	private String fileBaseName; // The prefix or base name for the config and output files
	private String outDir; // The output file directory
	private String outFileBase; // The prefix for the output files, including directory
	private PrintWriter passengerWriter; // The PrintWriter for passenger data
	public static final String PASSENGERSUFFIX = "_passenger.csv";
	public static final String TRAINSUFFIX = "_train.csv";

	/**
	 * The subway track.
	 * The keys are the stations, and the values are objects which detail what
	 * the next station on the track is, and how long it takes to get there.
	 */
	private Map<Station, Destination> track;

	/**
	 * A Map relating station names to their instances.
	 */
	private Map<String, Station> stationMap;

	/**
	 * The trains on the track.
	 */
	private List<Train> trains;

	private double dt; // The size of the time step.
	private double t; // The current time.
	private int numSteps; // The total number of time steps completed
	private int numPassengers; // The total number of passengers who have completed their travel

	/**
	 * Sets up the subway stations and opens up the output files.
	 * @param fileBaseName the base name (prefix) for the config and output files
	 * @param configDir the directory in which to look for the config file
	 * @param outDir the directory for the output files
	 * @throws FileNotFoundException if there is an error opening any output files
	 * @throws RuntimeException if something goes wrong while setting up the track
	 */
	public SubwaySim(String fileBaseName, String configDir, String outDir) throws FileNotFoundException, RuntimeException {
		this.fileBaseName = fileBaseName;

		if (!configDir.endsWith("/")) {
			configDir += "/";
		}
		if (!outDir.endsWith("/")) {
			outDir += "/";
		}
		this.outDir = outDir;
		outFileBase = outDir + fileBaseName;

		loadConfig(configDir + fileBaseName + CONFIGSUFFIX);
		int stationPickerSeed = config.getInt("stationPickerSeed");

		// Set up the RNG for picking random Passenger destinations.
		// If the config gives us a negative seed, us the default Random seed.
		if (stationPickerSeed > 0) {
			stationRNG = new Random(stationPickerSeed);
		}
		else {
			stationRNG = new Random();
		}

		// Create the PrintWriters for storing passenger and train output.
		setupOutputFiles();

		// Populate the track with stations and place the trains at their starting station.
		setupTrack();
		setupTrains();

		// Write the headers for the output files
		writeTimesHeader();
		writeManifestHeader();

		System.err.println("[DEBUG] the track looks like: " + track);
		System.err.println("[DEBUG] the trains List looks like: " + trains);
		getStations().forEach(
			(station) -> {
				System.err.println("\tStation " + station + ": " + (station.isOccupied() ? "occupied" : "unoccupied"));
			}
		);

		//
		// Set up the clock
		//
		t = 0.0;
		dt = config.getDouble("timeStep");

		numSteps = 0;
		numPassengers = 0;

		// Set when the first Passenger spawns at each Station
		initFirstPassengers();
	}

	/**
	 * Create the output files.
	 */
	private void setupOutputFiles() throws FileNotFoundException {
		// Ensure the output directory exists
		File fOutDir = new File(outDir);
		if (!fOutDir.exists()) {
			System.err.println("[INFO] Creating " + fOutDir + " directory for output.");
			fOutDir.mkdir();
		}

		File passengerFile = new File(outFileBase + PASSENGERSUFFIX);

		System.err.println("[INFO] Writing passenger data to: " + passengerFile);
		passengerWriter = new PrintWriter(passengerFile);
		System.err.println("[INFO] Writing train data files to directory: " + outFileBase);
	}

	/**
	 * Close the output PrintWriters.
	 */
	public void closeOutputWriters() {
		if (passengerWriter != null) {
			passengerWriter.close();
		}
		//
		// Close each Train's PrintWriter
		//
		for (Train t : trains) {
			PrintWriter tw = t.getTrainWriter();
			if (tw != null) {
				tw.close();
			}
		}
	}

	/**
	 * Set each Station's initial Passenger spawn time.
	 */
	private void initFirstPassengers() {
		//
		// Set when the first Passenger spawns at each Station
		//
		for (Station station : stationMap.values()) {
			station.drawNextTime(t);
			System.err.println("[DEBUG] " + station + " has first arrival time at: " + station.getNextTime() + " seconds");
		}
	}

	/**
	 * Set up the track with Stations from the config file.
	 */
	private void setupTrack() throws RuntimeException {
		track = new LinkedHashMap<Station, Destination>();
		Set<String> keys = config.getKeySubset("station:"); // Get just the keys that represent subway stations

		stationMap = new HashMap<String, Station>();

		//
		// Create a map of _all_ the stations. We need to do this
		// before we actually create the track because we need all of the Station
		// objects to be created.
		//
		for (String key : keys) {
			String[] keyParts = key.split(":"); // Key format is station:name
			String name = keyParts[1];
			double timeBetweenSpawns = config.getDouble(key, 2);
			int seed = config.getInt("passengerSpawnSeed");

			stationMap.put(name, new Station(name, timeBetweenSpawns, seed));
		}

		//
		// Loop through the station keys again, this time building the track
		// by grabbing instances of the Station class that match the destination name.
		//
		for (String key : keys) {
			String[] keyParts = key.split(":"); // Key format is station:name
			String name = keyParts[1];
			String destName = config.get(key, 0); // Get the destination name
			if (destName == null) {
				throw new RuntimeException("Error: destination station not found for key: " + key);
			}
			int travelTime = config.getInt(key, 1); // Get the travel time to that station, in seconds
			if (travelTime == -1) {
				throw new RuntimeException("Error: travel time not found for key: " + key);
			}

			/*
			 * Grab both the `from` and `to` stations from our Map of all the stations along with the travel time.
			 * We use these three things to insert a portion of the subway track.
			 */
			track.put(stationMap.get(name), new Destination(stationMap.get(destName), travelTime));
		}
	}

	/**
	 * Set up the Trains List from the config file, "placing" the Trains on the track.
	 */
	private void setupTrains() throws RuntimeException {
		trains = new ArrayList<Train>();
		Set<String> keys = config.getKeySubset("train:"); // Get just the keys that represent the trains
		double boardTime = config.getDouble("boardTime");
		double maxWaitTime = config.getDouble("trainWaitTime");
		int capacity = config.getInt("trainCapacity");

		for (String key : keys) {
			String[] keyParts = key.split(":"); // Key format is train:name
			String name = keyParts[1];

			String startStationName = config.get(key);
			if (startStationName == null) {
				throw new RuntimeException("Error: start station not found for key: " + key);
			}
			// Ensure that there isn't already a Train starting at that Station
			if (trains.stream().anyMatch(train -> train.getLastVisited().getName().equals(startStationName))) {
				throw new RuntimeException("Error: can't have multiple trains start at same station; error found on key: " + key);
			}
			Station startStation = stationMap.get(startStationName);

			// Set up the train's initial station and destination
			Train train;
			try {
				train = new Train(name, capacity, startStation, track.get(startStation), boardTime, maxWaitTime, passengerWriter, outFileBase);
			}
			catch (FileNotFoundException e) {
				throw new RuntimeException("Error: problem creating Train output file: " + e);
			}
			trains.add(train);
			startStation.setOccupied(true);
		}
	}

	/**
	 * Load parameters from the config file.
	 * @param filename the config filename
	 */
	private void loadConfig(String filename) throws FileNotFoundException {
        // Load and parse the config file.
        config = new ConfigFile(filename);

        // Set default values for any missing keys.
		config.setDefault("boardTime", 10.0); // (seconds)
		config.setDefault("timeStep", config.getDouble("boardTime")); // The time step = amount of time to do smallest thing (board the train)
		config.setDefault("trainWaitTime", 120.0); // How long a train will wait for people if no one is boarding
		config.setDefault("trainCapacity", 160);
		config.setDefault("passengerSpawnSeed", -1); // Use the default seed (probably current time)
		config.setDefault("stationPickerSeed", -1); // Use the default seed (probably current time)

		/*
        config.setDefault("itemTime", 0.1);
        config.setDefault("payTime", 1.0);
        config.setDefault("timestep", config.getDouble("itemTime")); // Smallest unit = time to process 1 item.
        config.setDefault("nTills", 1);
        config.setDefault("arrivalSpacing", 1.0/config.getDouble("nTills")); // One per minute per till.
        config.setDefault("meanItems", 25);
        config.setDefault("sdItems", 50);
        config.setDefault("arrivalTimeSeed", -1);
        config.setDefault("cartSizeSeed", -1);
		*/
	}


	/**
	 * This method is used as the time step system of the simulation. it updates the simulation.
	 */
	public void step() {
		t += dt;
		numSteps++;

		// Possibly add Passengers waiting at stations
		for (Station station : stationMap.values()) {
			// System.err.println("[DEBUG] Queue for " + station + ":");
			// System.err.println(station.getStationQueue());
			double tnext = station.getNextTime();

			// Make sure this Passenger's start Station is different than its
			// desired destination Station.
			while (tnext < t) {

				System.err.println("[DEBUG] SPAWNED A PERSON AT STATION: " + station);
				Station randomDest = drawRandomStation();

				// Ensure that the Passenger's destination is not the same as its spawn location
				while (randomDest.getName().equals(station.getName())) {
					randomDest = drawRandomStation();
				}

				// Add the Passenger to this Station's queue
				station.enqueuePassenger(tnext, randomDest);
				// Draw another spawn time for this Station
				tnext = station.drawNextTime(tnext);
			}
		}

		System.err.println("[DEBUG] here are Station status' for t = " + t + " after adding people.");
		getStations().forEach(
			(station) -> {
				System.err.println("\tStation " + station + ": " + (station.isOccupied() ? "occupied" : "unoccupied") + "; has " + station.getStationQueue().size() + " people.");
			}
		);

		for (Train train : trains) {
			/*
			 * If any Passengers got off, Train.run(...) will return them in a
			 * PassengerList.
			 */
			PassengerList disembarked = train.run(t, track);
			// Count the passengers that got off
			if (disembarked != null) {
				numPassengers += disembarked.size();
			}
		}
	}

	/**
	 * This method is used to write the header of the times file
	 */
	public void writeTimesHeader() {
		passengerWriter.println("Total Trip Time,Spawn Time,Board Time,Spawn Station,Destination Station");
	}

	/**
	 * This method is used to write the header of the manifest file.
	 */
	public void writeManifestHeader() {
		for (Train t : trains) {
			t.getTrainWriter().println("Passengers,Departing Station,Destination Station,Global Time");
		}
	}

	/**
	 * @return a random station, uniformly chosen from the set of all stations.
	 */
	private Station drawRandomStation() {
		List<Station> stations = new ArrayList<Station>(stationMap.values());
		return stations.get(stationRNG.nextInt(stations.size()));
	}

	/**
	 * @return this simulation's config file.
	 */
	public ConfigFile getConfig() {
		return config;
	}

	/**
	 * @return the current global time, in seconds.
	 */
	public double getTime() {
		return t;
	}

	/**
	 * @return the number of Passengers that have completed their travels.
	 */
	public int getNumPassengers() {
		return numPassengers;
	}

	/**
	 * @return the Map of Stations and Destinations representing the track.
	 */
	public Map<Station, Destination> getTrack() {
		return track;
	}

	/**
	 * @return a List of the Stations.
	 */
	public List<Station> getStations() {
		return new ArrayList<Station>(stationMap.values());
	}
}
