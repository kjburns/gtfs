/*
 * StopCollection.java
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
 *   2016-05-02  Basic functionality
 */
package com.github.kjburns.gtfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import com.github.kjburns.gtfs.misc.CsvFile;

/**
 * A collection of stops.
 * @author Kevin J. Burns
 *
 */
public class StopCollection {
	private GtfsFile gtfs;
	private HashMap<String, Stop> stops = new HashMap<>();

	/**
	 * Creates a stop collection from stops.txt in a gtfs file
	 * @param gtfs gtfs object that will contain this collection
	 * @param stopsFile file to read from
	 * @throws IOException If there is a problem opening the file
	 * @throws InvalidDataException If any invalid data is contained in the
	 * file
	 * @throws MissingRequiredFieldException If any required fields are
	 * missing in the file
	 * @throws DatasetUniquenessException If any set of two or more stops
	 * in the file have the same stop id
	 */
	StopCollection(GtfsFile gtfs, File stopsFile) 
			throws IOException, InvalidDataException, 
			MissingRequiredFieldException, DatasetUniquenessException {
		this.gtfs = gtfs;
		try (FileInputStream fis = new FileInputStream(stopsFile)) {
			CsvFile table = new CsvFile(fis);
			for (int record = 1; record <= table.getRecordCount(); record++) {
				Stop stop = Stop.createStopFromTableRow(this, table, record);
				String sid = stop.getStopId();
				if (this.stops.containsKey(sid)) {
					/*
					 * Stop ID is supposed to be dataset-unique, but it's
					 * already in the dictionary.
					 */
					throw new DatasetUniquenessException(
							GtfsFile.FILENAME_STOPS, 
							Stop.FIELD_NAME_STOP_ID, 
							sid);
				}
				this.stops.put(sid, stop);
			}
		} catch (InvalidDataException ex) {
			/*
			 * In the future, may want to just log it and move to the next
			 * row, but for now, shut it down
			 */
			throw ex;
		} catch (MissingRequiredFieldException ex) {
			throw ex;
		}
		
		this.establishStopToStationRelations();
	}

	private void establishStopToStationRelations() {
		for (Stop stop : this.stops.values()) {
			if (stop instanceof Station) {
				/*
				 * Stations can't have parent stations
				 */
				continue;
			}
			
			Stop parentStop = this.stops.get(stop.getParentStationId());
			if (parentStop == null) {
				/*
				 * TODO should this be an error?
				 */
			}
			if (!(parentStop instanceof Station)) {
				/*
				 * TODO this is an error condition; parent must be a station
				 */
			}
			Station parent = (Station)parentStop;
			parent.registerChildStop(stop);
		}
	}

	/**
	 * @return the gtfs
	 */
	GtfsFile getGtfs() {
		return this.gtfs;
	}
	
	/**
	 * Searches this collection for a stop with the given id.
	 * @return the stop with the given id if it exists; otherwise {@code null}.
	 */
	public Stop getStopById(String id) {
		return this.stops.get(id);
	}
}