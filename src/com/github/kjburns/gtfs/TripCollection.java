/*
 * TripCollection.java
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
 *   2016-05-18  Basic functionality
 *   2016-05-30  getTripById()
 */
package com.github.kjburns.gtfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import com.github.kjburns.gtfs.misc.CsvFile;

/**
 * A collection of {@link Trip}s; the contents of trips.txt
 * @author Kevin J. Burns
 *
 */
public class TripCollection {
	private HashMap<String, Trip> trips = new HashMap<>();
	
	/**
	 * Constructor. Reads trips.txt
	 * @param f File object pointing to the location of trips.txt in the
	 * zip file
	 * @throws IOException if there is any problem opening or reading the file
	 * @throws MissingRequiredFieldException if any required fields are missing
	 * @throws InvalidDataException if any invalid data are found in the table
	 * @throws DatasetUniquenessException if more than one record exists for 
	 * a given trip_id
	 */
	TripCollection(File f) 
			throws IOException, MissingRequiredFieldException, 
				InvalidDataException, DatasetUniquenessException {
		try(FileInputStream fis = new FileInputStream(f)) {
			CsvFile table = new CsvFile(fis);
			for (int record = 1; record <= table.getRecordCount(); record++) {
				Trip t = new Trip(table, record);
				String id = t.getTripId();
				if (this.trips.containsKey(id)) {
					throw new DatasetUniquenessException(
							GtfsFile.FILENAME_TRIPS, 
							Trip.FIELD_NAME_TRIP_ID, 
							id);
				}
				
				this.trips.put(id, t);
			}
		}
	}
	
	/**
	 * Gets the number of registered trips.
	 * @return
	 */
	public int getTripCount() {
		return this.trips.size();
	}
	
	/**
	 * Gets an iterator for the stored trips.
	 * @return
	 */
	public Iterator<Trip> getIterator() {
		return this.trips.values().iterator();
	}

	public Trip getTripById(String tripId) {
		return this.trips.get(tripId);
	}
}
	