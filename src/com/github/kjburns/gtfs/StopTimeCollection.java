/*
 * StopTimeCollection.java
 * General Transit Feed Specification
 * 
 * Copyright 2016 Kevin J. Burns
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 * Revision Log:
 *   2016-05-30  Basic functionality
 *   2016-05-30  Make getEarliestDepartureTime() public
 */
package com.github.kjburns.gtfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.github.kjburns.gtfs.misc.CsvFile;

/**
 * A collection of stop times. Internally, this object keeps stop times
 * mapped by stop id and by trip id.
 * @author Kevin J. Burns
 *
 */
public class StopTimeCollection {
	private HashMap<String, ArrayList<StopTime>> byTrip = new HashMap<>();
	private HashMap<String, ArrayList<StopTime>> byStop = new HashMap<>();
	private GtfsFile gtfs;
	
	/**
	 * Constructor. Reads the collection from stop_times.txt
	 * @param f Pointer to stop_times.txt
	 * @param gtfs GTFS file that this collection is part of
	 * @throws IOException If there is any problem opening or reading the
	 * file
	 * @throws MissingRequiredFieldException if any required fields are missing
	 * @throws InvalidDataException if any invalid data are found in the table
	 * @throws TerminalTimepointException if any trip does not both begin
	 * and end with a timepoint
	 */
	StopTimeCollection(GtfsFile gtfs, File f) 
			throws IOException, MissingRequiredFieldException, 
				InvalidDataException, TerminalTimepointException {
		this.gtfs = gtfs;
		
		try(FileInputStream fis = new FileInputStream(f)) {
			CsvFile table = new CsvFile(fis);
			
			for (int record = 1; record <= table.getRecordCount(); record++) {
				StopTime st = new StopTime(table, record);
				
				ArrayList<StopTime> list;
				
				list = this.byTrip.get(st.getTripId());
				if (list == null) {
					list = new ArrayList<StopTime>();
					this.byTrip.put(st.getTripId(), list);
				}
				list.add(st);
				
				list = this.byStop.get(st.getStopId());
				if (list == null) {
					list = new ArrayList<StopTime>();
					this.byStop.put(st.getStopId(), list);
				}
				list.add(st);
			}
		}
		
		for (ArrayList<StopTime> stopList : this.byTrip.values()) {
			Collections.sort(stopList, (x, y) -> {
				return Integer.compare(
						x.getStopSequence(), y.getStopSequence());
			});
			
			if (!stopList.get(0).isTimepoint() || 
					!stopList.get(stopList.size() - 1).isTimepoint()) {
				throw new TerminalTimepointException(
						stopList.get(0).getTripId());
			}
		}
	}
	
	/**
	 * Gets the sequence of stops along a particular trip.
	 * @param tripId trip_id to query
	 * @return the sequence of stops along a trip if defined; otherwise,
	 * {@code null}.
	 */
	public List<StopTime> getTripSchedule(String tripId) {
		if (!this.byTrip.containsKey(tripId)) {
			return null;
		}
		
		return Collections.unmodifiableList(this.byTrip.get(tripId));
	}
	
	/**
	 * Gets a timetable for a particular stop.
	 * @param stopId stop_id to query
	 * @param date date for the timetable
	 * @return A list of stop times, sorted by earliest departure time.
	 */
	public List<StopTime> getTimetable(String stopId, GregorianCalendar date) {
		return this.byStop.get(stopId).stream()
				.filter((test) -> {
					Trip trip = gtfs.getTrips().getTripById(test.getTripId());
					String serviceId = trip.getServiceId();
					return this.gtfs.getServiceCalendar().isServiceDefinedOn(
							serviceId, date);
				})
				.sorted((x, y) -> {
					return Long.compare(
							this.getEarliestDepartureTime(x, date)
								.getTimeInMillis(), 
							this.getEarliestDepartureTime(y, date)
								.getTimeInMillis());
				})
				.collect(Collectors.toList());
	}
	
	public GregorianCalendar getEarliestDepartureTime(
			StopTime st, GregorianCalendar date) {
		if (st.isTimepoint()) {
			return st.getDepartureTime(date);
		}
		else {
			String tripId = st.getTripId();
			int index = this.byTrip.get(tripId).indexOf(st);
			
			for (int i = index - 1; ; i--) {
				if (index < 0) {
					throw new IndexOutOfBoundsException();
				}
				StopTime previousStop = this.byTrip.get(tripId).get(i);
				if (previousStop.isTimepoint()) {
					return previousStop.getDepartureTime(date);
				}
			}
		}
	}
}
