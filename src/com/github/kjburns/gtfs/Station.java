/*
 * Station.java
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

import java.util.HashSet;
import java.util.Iterator;

import com.github.kjburns.gtfs.misc.CsvFile;

/**
 * A transit station, which contains at least one stop.
 * @author Kevin J. Burns
 *
 */
public class Station extends Stop {
	private HashSet<Stop> childStops = new HashSet<>();
	
	/**
	 * Creates a new Station.
	 * @param collection StopCollection that this station is part of
	 * @param table table to read information from
	 * @param record record number to read from, where the first record is #1
	 * @throws MissingRequiredFieldException if a required field is missing 
	 * @throws InvalidDataException if the record has any data which is
	 * disallowed by the specification
	 */
	Station(StopCollection collection, CsvFile table, int record) 
			throws MissingRequiredFieldException, InvalidDataException {
		super(collection, table, record);
	}
	
	/**
	 * Registers a stop as being contained in this station.
	 * @param child the stop to register.
	 */
	void registerChildStop(Stop child) {
		this.childStops.add(child);
	}
	
	/**
	 * Gets the number of stops at this station.
	 */
	public int getStopCount() {
		return this.childStops.size();
	}
	
	/**
	 * Gets an iterator of stops at this station.
	 */
	public Iterator<Stop> getStopIterator() {
		return this.childStops.iterator();
	}

	/**
	 * Always returns null, as a station cannot be part of another station.
	 * @see com.github.kjburns.gtfs.Stop#getParentStationId()
	 */
	@Override
	String getParentStationId() {
		return null;
	}
}
