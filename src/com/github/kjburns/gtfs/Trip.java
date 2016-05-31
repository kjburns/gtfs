/*
 * Trip.java
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
 */
package com.github.kjburns.gtfs;

import java.util.HashMap;

import com.github.kjburns.gtfs.misc.CsvFile;
import com.github.kjburns.gtfs.misc.CsvFile.FieldNotFoundException;
import com.github.kjburns.gtfs.misc.CsvFile.ReadPastEndOfTableException;

/**
 * A trip on a public transit service. 
 * @author Kevin J. Burns
 *
 */
public class Trip {
	private static final String FIELD_NAME_ROUTE_ID = "route_id";
	private static final String FIELD_NAME_SERVICE_ID = "service_id";
	static final String FIELD_NAME_TRIP_ID = "trip_id";
	private static final String FIELD_NAME_TRIP_HEADSIGN = "trip_headsign";
	private static final String FIELD_NAME_TRIP_SHORT_NAME = "trip_short_name";
	private static final String FIELD_NAME_DIRECTION_ID = "direction_id";
	private static final String FIELD_NAME_BLOCK_ID = "block_id";
	private static final String FIELD_NAME_SHAPE_ID = "shape_id";
	private static final String FIELD_NAME_WHEELCHAIR_ACCESSIBLE = 
			"wheelchair_accessible";
	private static final String FIELD_NAME_BIKES_ALLOWED = "bikes_allowed";
	
	private final String[] requiredFieldNames = {
			FIELD_NAME_ROUTE_ID, FIELD_NAME_SERVICE_ID, FIELD_NAME_TRIP_ID
	};
	private final String[] optionalFieldNames = {
		FIELD_NAME_TRIP_HEADSIGN,
		FIELD_NAME_TRIP_SHORT_NAME,
		FIELD_NAME_DIRECTION_ID,
		FIELD_NAME_BLOCK_ID,
		FIELD_NAME_SHAPE_ID,
		FIELD_NAME_WHEELCHAIR_ACCESSIBLE,
		FIELD_NAME_BIKES_ALLOWED
	};
	
	private int originalRecord;
	
	private HashMap<String, String> tableData = new HashMap<>();
	private WheelchairAccessibilityEnum wheelchairAccessibility;
	private BikeAccessibilityEnum bikeAccessibility;
	private int directionId;
	
	/**
	 * Constructor. Reads a record from trips.txt
	 * @param table Table to read from
	 * @param record Record number to read from, where 1 is the first record.
	 * @throws MissingRequiredFieldException If any required field is missing.
	 * @throws InvalidDataException If any invalid data is found in the record.
	 */
	Trip(CsvFile table, int record) 
			throws MissingRequiredFieldException, InvalidDataException {
		this.originalRecord = record;
		
		for (String key : this.requiredFieldNames) {
			try {
				String value = table.getData(key, record);
				this.tableData.put(key, value);
			} catch (ReadPastEndOfTableException ex) {
				throw new IndexOutOfBoundsException();
			} catch (FieldNotFoundException ex) {
				throw new MissingRequiredFieldException(
						GtfsFile.FILENAME_TRIPS, key);
			}
		}
		
		for (String key : this.optionalFieldNames) {
			if (table.fieldExists(key)) {
				try {
					String value = table.getData(key, record);
					this.tableData.put(key, value);
				} catch (ReadPastEndOfTableException | 
						 FieldNotFoundException ex) {
					/*
					 * Both of these exceptions shouldn't happen.
					 * ReadPastEndOfTableException would have been thrown in
					 *   the required fields.
					 * FieldNotFoundException can't occur because that check
					 *   was already made.
					 */
					assert(false);
				}
			}
		}
		
		this.interpretFieldValues();
	}

	private void interpretFieldValues() throws InvalidDataException {
		String key = null;
		String strValue = null;
		
		/*
		 * Wheelchair Accessibility
		 */
		key = Trip.FIELD_NAME_WHEELCHAIR_ACCESSIBLE;
		strValue = this.tableData.get(key);
		InvalidDataException wheelchairException = new InvalidDataException(
				GtfsFile.FILENAME_TRIPS, 
				key, 
				this.originalRecord, 
				strValue);
		try {
			if (strValue != null) {
				strValue = strValue.trim();
				if (strValue.length() > 0) {
					int value = Integer.valueOf(strValue);
					if ((value < 0) || (value >= 
							WheelchairAccessibilityEnum.values().length)) {
						throw wheelchairException;
					}
					this.wheelchairAccessibility = 
							WheelchairAccessibilityEnum.values()[value];
				}
				else {
					this.wheelchairAccessibility =
							WheelchairAccessibilityEnum.UNKNOWN;
				}
			}
		} catch(NumberFormatException ex) {
			throw wheelchairException;
		}
		
		/*
		 * Bike Accessibility
		 */
		key = Trip.FIELD_NAME_BIKES_ALLOWED;
		strValue = this.tableData.get(key);
		InvalidDataException bikeException = new InvalidDataException(
				GtfsFile.FILENAME_TRIPS, 
				key, 
				this.originalRecord, 
				strValue);
		try {
			if (strValue != null) {
				strValue = strValue.trim();
				if (strValue.length() > 0) {
					int value = Integer.valueOf(strValue);
					if ((value < 0) || (value >= 
							BikeAccessibilityEnum.values().length)) {
						throw bikeException;
					}
					this.bikeAccessibility = 
							BikeAccessibilityEnum.values()[value];
				}
				else {
					this.bikeAccessibility = BikeAccessibilityEnum.UNKNOWN;
				}
			}
		} catch(NumberFormatException ex) {
			throw wheelchairException;
		}
		
		/*
		 * Direction
		 */
		key = Trip.FIELD_NAME_DIRECTION_ID;
		strValue = this.tableData.get(key);
		InvalidDataException dirException = new InvalidDataException(
				GtfsFile.FILENAME_TRIPS, key, this.originalRecord, strValue);
		try {
			if (strValue != null) {
				strValue = strValue.trim();
				int value = Integer.valueOf(strValue);
				if ((value < 0) || (value > 1)) {
					throw dirException;
				}
				this.directionId = value;
			}
			else {
				this.directionId = -1;
			}
		} catch(NumberFormatException ex) {
			throw dirException;
		}
	}

	/**
	 * Gets the trip_id for this trip.
	 * @return
	 */
	public String getTripId() {
		return this.tableData.get(FIELD_NAME_TRIP_ID);
	}
	
	/**
	 * Gets the service_id for this trip. No error checking is done to ensure
	 * that the service_id actually exists.
	 * @return
	 */
	public String getServiceId() {
		return this.tableData.get(FIELD_NAME_SERVICE_ID);
	}
	
	/**
	 * Gets the route_id for this trip. No error checking is done to ensure
	 * that the route_id actually exists.
	 * @return
	 */
	public String getRouteId() {
		return this.tableData.get(FIELD_NAME_ROUTE_ID);
	}
	
	/**
	 * Gets the headsign for this trip, if one has been defined.
	 * @return The headsign for this trip, or {@code null} if none has been
	 * defined.
	 */
	public String getHeadsign() {
		return this.tableData.get(FIELD_NAME_TRIP_HEADSIGN);
	}
	
	/**
	 * Gets the short name for this trip, if one has been defined.
	 * @return The short name for this trip, or {@code null} if none has been
	 * defined.
	 */
	public String getShortName() {
		return this.tableData.get(FIELD_NAME_TRIP_SHORT_NAME);
	}
	
	/**
	 * Gets the block_id for this trip, if one has been defined.
	 * <p>
	 * A block is a group of trips on which a patron can transfer from one
	 * trip to another without leaving the vehicle.
	 * </p>
	 * @return The block_id for this trip, or {@code null} if none has been
	 * defined.
	 */
	public String getBlockId() {
		return this.tableData.get(FIELD_NAME_BLOCK_ID);
	}
	
	/**
	 * Gets the shape_id for this trip, if one has been defined. No error
	 * checking is performed to ensure that the shape_id actually exists.
	 * @return The shape_id for this trip, or {@code null} if none has been
	 * defined.
	 */
	public String getShapeId() {
		return this.tableData.get(FIELD_NAME_SHAPE_ID);
	}

	/**
	 * Gets an enumeration value describing the ability of a person in a
	 * wheelchair to use this trip, if it has been defined.
	 * @return A wheelchair accessibility value, if defined; otherwise,
	 * returns {@link WheelchairAccessibilityEnum#UNKNOWN}.
	 */
	public WheelchairAccessibilityEnum getWheelchairAccessibility() {
		return this.wheelchairAccessibility;
	}

	/**
	 * Gets an enumeration value describing the ability to transport a bike
	 * on this trip, if it has been defined.
	 * @return A bike accessibility value, if defined; otherwise, return
	 * {@link BikeAccessibilityEnum#UNKNOWN}.
	 */
	public BikeAccessibilityEnum getBikeAccessibility() {
		return this.bikeAccessibility;
	}

	/**
	 * Gets the direction_id, if defined.
	 * @return 0 or 1, if defined; otherwise, returns -1.
	 */
	public int getDirectionId() {
		return this.directionId;
	}
}
