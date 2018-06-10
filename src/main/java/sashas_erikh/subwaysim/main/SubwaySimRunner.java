package sashas_erikh.subwaysim.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * A class for running Subway Simulations.
 * @author Erik H
 */
public class SubwaySimRunner {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("[FATAL] please specify the filename prefix.");
			System.exit(1);
		}

		final String LOGSUFFIX = "_log.txt";
		final String INPUTDIR = "./config/";
		final String OUTPUTDIR = "./data/";
		String fileBaseName = args[0];

		SubwaySim sim = null;
		try {
			sim = new SubwaySim(fileBaseName, INPUTDIR, OUTPUTDIR);
		}
		catch (Exception e) {
			System.err.println("[FATAL] error creating sim: " + e);
			System.exit(1);
		}

		System.err.println("[INFO] using config:");
		System.err.println(sim.getConfig());

		// Set up the sim stopping conditions
		final double MAXTIME = 60*60*24.0; // Run for this many hours
		final int MAXPASSENGERS = 6000; // ... or until we hit this many passengers

		System.err.println("[DEBUG] before sim starts, here is the state of all the Stations:");
		sim.getStations().forEach(
			(station) -> {
				System.err.println("\tStation " + station + ": " + (station.isOccupied() ? "occupied" : "unoccupied"));
			}
		);

		//
		// Run the sim until our stop conditions in number of Passengers or maximum run time are met.
		//
		while (sim.getTime() < MAXTIME && sim.getNumPassengers() < MAXPASSENGERS) {
			System.err.println("\t[DEBUG] time check: " + sim.getTime() + " < " + MAXTIME);
			System.err.println("\t[DEBUG] passengers check: " + sim.getNumPassengers() + " < " + MAXPASSENGERS);
			// Run the sim!!!!!!
			sim.step();
		}

		sim.closeOutputWriters();
		System.out.println("[INFO] Completed sim, serving " + sim.getNumPassengers() + " passengers in " + sim.getTime() + " seconds.");

		File logFile = new File(OUTPUTDIR + fileBaseName + LOGSUFFIX);
		System.out.println("[INFO] Writing sim log to " + logFile);
		PrintWriter logWriter;
		try {
			logWriter = new PrintWriter(logFile);
			logWriter.println("" + sim.getConfig());

			logWriter.println("Total passengers: " + sim.getNumPassengers());
			logWriter.println("Total time (seconds): " + sim.getTime());
			logWriter.println("Passengers/second: " + (sim.getNumPassengers()/sim.getTime()));
			logWriter.println("Seconds/Passenger: " + (sim.getTime()/sim.getNumPassengers()));
			logWriter.close();
		}
		catch (FileNotFoundException e) {
			System.err.println("[FATAL] Error writing log file: " + e);
			System.exit(1);
		}
	}
}
