/*
 * Stop.java
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

import java.util.HashMap;

import com.github.kjburns.gtfs.misc.CsvFile;
import com.github.kjburns.gtfs.misc.CsvFile.FieldNotFoundException;
import com.github.kjburns.gtfs.misc.CsvFile.ReadPastEndOfTableException;

/**
 * A stop on a transit system.
 * @author Kevin J. Burns
 *
 */
public class Stop {
	/**
	 * the stop_id field name defined in stops.txt
	 */
	static final String FIELD_NAME_STOP_ID = "stop_id";
	private static final String FIELD_NAME_STOP_CODE = "stop_code";
	private static final String FIELD_NAME_STOP_NAME = "stop_name";
	private static final String FIELD_NAME_STOP_DESC = "stop_desc";
	private static final String FIELD_NAME_STOP_LAT = "stop_lat";
	private static final String FIELD_NAME_STOP_LON = "stop_lon";
	private static final String FIELD_NAME_ZONE_ID = "zone_id";
	private static final String FIELD_NAME_STOP_URL = "stop_url";
	private static final String FIELD_NAME_LOCATION_TYPE = "location_type";
	private static final String FIELD_NAME_PARENT_STATION = "parent_station";
	private static final String FIELD_NAME_STOP_TIMEZONE = "stop_timezone";
	private static final String 
			FIELD_NAME_WHEELCHAIR_BOARDING = "wheelchair_boarding";
	
	private static String[] requiredFields = {
			FIELD_NAME_STOP_ID,
			FIELD_NAME_STOP_NAME,
			FIELD_NAME_STOP_LAT,
			FIELD_NAME_STOP_LON
	};
	private static String[] optionalFields = {
			FIELD_NAME_STOP_CODE,
			FIELD_NAME_STOP_DESC,
			FIELD_NAME_STOP_URL,
			FIELD_NAME_PARENT_STATION,
			FIELD_NAME_STOP_TIMEZONE,
			FIELD_NAME_WHEELCHAIR_BOARDING
	};
	
	private StopCollection collection;
	private HashMap<String, String> dataFromTable = new HashMap<>();
	private double latitude;
	private double longitude;
	private WheelchairAccessibilityEnum accessibility;
	
	/**
	 * Reads the specified row of the table to create either a Stop or a 
	 * Station, as appropriate
	 * @param collection the StopCollection that this stop is part of
	 * @param table The table to read from
	 * @param record The record number to read from, where the first record is
	 * #1
	 * @return A Stop or a Station
	 * @throws InvalidDataException if location_type has bad data
	 * @throws MissingRequiredFieldException if a required field is missing
	 */
	static Stop createStopFromTableRow(StopCollection collection,  
			CsvFile table, int record) 
					throws InvalidDataException,
					MissingRequiredFieldException {
		String locationType = "";
		if (table.fieldExists(FIELD_NAME_LOCATION_TYPE)) {
			try {
				locationType = table.getData(FIELD_NAME_LOCATION_TYPE, record);
			} catch (ReadPastEndOfTableException ex) {
				throw new IndexOutOfBoundsException();
			} catch (FieldNotFoundException ex) {
				/*
				 * Already tested for this
				 */
				assert(false);
			}
		}
		else {
			locationType = "";
		}
		
		locationType = locationType.trim();
		if (locationType.equals("") || locationType.equals("0")) {
			/*
			 * It's a stop
			 */
			return new Stop(collection, table, record);
		}
		else if (locationType.equals("1")) {
			/*
			 * It's a station
			 */
			return new Station(collection, table, record);
		}
		else {
			/*
			 * Invalid data
			 */
			throw new InvalidDataException(
					GtfsFile.FILENAME_STOPS, FIELD_NAME_LOCATION_TYPE, 
					record, locationType);
		}
	}
	
	/**
	 * Creates a new Stop.
	 * @param collection StopCollection that this stop is part of
	 * @param table table to read information from
	 * @param record record number to read from, where the first record is #1
	 * @throws MissingRequiredFieldException if required fields are missing
	 * @throws InvalidDataException if record contains any value which is
	 * disallowed by the specification 
	 */
	Stop(StopCollection collection, CsvFile table, int record) 
			throws MissingRequiredFieldException, InvalidDataException {
		this.collection = collection;

		for (int i = 0; i < requiredFields.length; i++) {
			String key = requiredFields[i];
			try {
				String value = table.getData(key, record);
				this.dataFromTable.put(key, value);
			} catch (ReadPastEndOfTableException ex) {
				throw new IndexOutOfBoundsException();
			} catch (FieldNotFoundException ex) {
				throw new MissingRequiredFieldException(
						GtfsFile.FILENAME_STOPS, key);
			}
		}
		
		for (int i = 0; i < optionalFields.length; i++) {
			String key = optionalFields[i];
			if (table.fieldExists(key)) {
				try {
					String value = table.getData(key, record);
					this.dataFromTable.put(key, value);
				} catch (FieldNotFoundException ex) {
					/*
					 * Already tested for this
					 */
					assert(false);
				} catch (ReadPastEndOfTableException ex) {
					throw new IndexOutOfBoundsException();
				}
			}
		}
		
		if (this.isZoneIdRequired()) {
			if (!table.fieldExists(FIELD_NAME_ZONE_ID)) {
				throw new MissingRequiredFieldException(
						GtfsFile.FILENAME_STOPS, Stop.FIELD_NAME_ZONE_ID);
			}
		}

		String value = this.dataFromTable.get(FIELD_NAME_STOP_LAT);
		try {
			this.latitude = Double.valueOf(value);
		} catch (NumberFormatException ex) {
			throw new InvalidDataException(
					GtfsFile.FILENAME_STOPS, 
					FIELD_NAME_STOP_LAT,  
					record,  
					value); 
		}
		
		value = this.dataFromTable.get(FIELD_NAME_STOP_LON);
		try {
			this.longitude = Double.valueOf(value);
		} catch (NumberFormatException ex) {
			throw new InvalidDataException(
					GtfsFile.FILENAME_STOPS, 
					FIELD_NAME_STOP_LON,  
					record,  
					value); 
		}
		
		if (this.dataFromTable.containsKey(FIELD_NAME_WHEELCHAIR_BOARDING)) {
			value = this.dataFromTable.get(FIELD_NAME_WHEELCHAIR_BOARDING);
			if ((value == null) || ("".equals(value.trim()))) {
				this.accessibility = WheelchairAccessibilityEnum.UNKNOWN;
			}
			else {
				try {
					int numValue = Integer.valueOf(value);
					switch(numValue) {
					case 0: // intentional fall-through
					case 1: // intentional fall-through
					case 2:
						this.accessibility = 
								WheelchairAccessibilityEnum.values()[numValue];
						break;
					default:
						throw new InvalidDataException(
								GtfsFile.FILENAME_STOPS, 
								FIELD_NAME_WHEELCHAIR_BOARDING, 
								record, 
								value);
					}
				} catch (NumberFormatException ex) {
					throw new InvalidDataException(
							GtfsFile.FILENAME_STOPS, 
							FIELD_NAME_WHEELCHAIR_BOARDING, 
							record, 
							value);
				}
			}
		}
	}
	
	private boolean isZoneIdRequired() {
		return this.collection.getGtfs().isFilePresent(
				GtfsFile.FILENAME_FARE_RULES);
	}
	
	/**
	 * Gets the stop ID. Every stop has a unique stop ID.
	 * @return
	 */
	public String getStopId() {
		return this.dataFromTable.get(FIELD_NAME_STOP_ID);
	}
	
	/**
	 * Gets the stop ID of the parent station, if the stop has a parent
	 * station.
	 * @return stop ID of the parent station, if the stop has one. Otherwise,
	 * {@code null}.
	 */
	String getParentStationId() {
		String psid = this.dataFromTable.get(FIELD_NAME_PARENT_STATION);
		if ((psid == null) || ("".equals(psid.trim()))) {
			return null;
		}
		else {
			return psid;
		}
	}

	/**
	 * Gets the latitude of this stop, in degrees. Positive latitudes are 
	 * north of the equator.
	 * @return the latitude
	 */
	public double getLatitude() {
		return this.latitude;
	}

	/**
	 * Gets the longitude of this stop, in degrees. Positive longitudes are
	 * east of the Prime Meridian.
	 * @return the longitude
	 */
	public double getLongitude() {
		return this.longitude;
	}
	
	/**
	 * Gets the stop code, if defined.
	 * @return The stop code, if defined; otherwise, {@code null}.
	 */
	public String getStopCode() {
		return this.dataFromTable.get(FIELD_NAME_STOP_CODE);
	}
	
	/**
	 * Gets the stop name.
	 * @return
	 */
	public String getStopName() {
		return this.dataFromTable.get(FIELD_NAME_STOP_NAME);
	}
	
	/**
	 * Gets the stop description, if defined.
	 * @return The stop description, if defined; otherwise, {@code null}.
	 */
	public String getStopDesc() {
		return this.dataFromTable.get(FIELD_NAME_STOP_DESC);
	}
	
	/**
	 * Gets the zone id, if defined.
	 * @return The zone id, if defined; otherwise, {@code null}.
	 */
	public String getZoneId() {
		return this.dataFromTable.get(FIELD_NAME_ZONE_ID);
	}
	
	/**
	 * Gets the stop url, if defined.
	 * @return The stop url, if defined; otherwise, {@code null}.
	 */
	public String getStopUrl() {
		return this.dataFromTable.get(FIELD_NAME_STOP_URL);
	}
	
	/**
	 * Gets this stop's parent station, if it has one.
	 * @return the parent station, if this stop has one; otherwise, 
	 * {@code null}.
	 */
	public Station getParentStation() {
		String psid = this.getParentStationId();
		if (psid == null) {
			return null;
		}
		
		Stop ps = this.collection.getStopById(psid);
		return (Station)ps;
	}
	
	/**
	 * Gets the timezone observed by this stop.
	 * @return
	 */
	public String getTimezone() {
		Station parent = this.getParentStation();
		if (parent != null) {
			return parent.getTimezone();
		}
		
		String tz = this.dataFromTable.get(FIELD_NAME_STOP_TIMEZONE);
		if ((tz == null) || ("".equals(tz.trim()))) {
			return this.collection.getGtfs().getTimezone();
		}
		else {
			return tz;
		}
	}
	
	/**
	 * Gets the wheelchair accessibility of this stop
	 * @return
	 */
	public WheelchairAccessibilityEnum getWheelchairBoarding() {
		if (this.accessibility == null) {
			return WheelchairAccessibilityEnum.UNKNOWN;
		}
		
		if (this.accessibility == WheelchairAccessibilityEnum.UNKNOWN) {
			Station parent = this.getParentStation();
			if (parent != null) {
				return parent.getWheelchairBoarding();
			}
		}
		
		return this.accessibility;
	}
}
