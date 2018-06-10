package sashas_erikh.subwaysim.passenger;

import sashas_erikh.subwaysim.station.Station;

import java.util.LinkedList;
import java.util.Iterator;

/**
 * A fixed-size linked list that is used to store train passengers.
 * @author Sasha S
 */
public class PassengerList extends LinkedList<Passenger> {
	private int capacity;

	public PassengerList(int capacity) {
		super();
		this.capacity = capacity;
	}

	/**
	 * Removes from the list all the passengers who want to get off the train at the current station.
	 * @param station the station that is used to determine which passengers to remove from the list
	 * @return another PassengerList that contains all the removed Passengers.
	 */
	public PassengerList removeForStation(Station station) {
		PassengerList removed = new PassengerList(this.size());
		Iterator<Passenger> passengers = super.iterator();

		// Remove and keep track of all the passengers who want to get off
		// at the specified station.
		while (passengers.hasNext()) {

			Passenger p = passengers.next();
			if (p.getDestination().getName() == station.getName()) {

				removed.add(p);
				passengers.remove(); // Remove the passenger from the LinkedList
			}
		}

		return removed;
	}

	/**
	* @return The capacity of the list
	*/
	public int getCapacity() {
		return capacity;
	}

	/**
	 * Used to add passengers to the list.
	 * @param p the passenger object we want to add
	 * @return returns false if adding this passenger will exceed the capacity of the train
	 */
	public boolean add(Passenger p) {
		if (this.size() < capacity) {
			super.add(p);
			return true;
		}
		return false;
	}
}
