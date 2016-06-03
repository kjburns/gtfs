/*
 * StopTime.java
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
 *   2016-05-30  Bug fix: getNoonOnDate() was using 12-hour hour instead of
 *               24-hour hour 
 *   2016-06-02  Replace GregorianCalendar functionality with java.time
 */
package com.github.kjburns.gtfs;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.kjburns.gtfs.misc.CsvFile;
import com.github.kjburns.gtfs.misc.CsvFile.FieldNotFoundException;
import com.github.kjburns.gtfs.misc.CsvFile.ReadPastEndOfTableException;

/**
 * An instance of a particular bus turning up at a particular stop at a 
 * particular time.
 * @author Kevin J. Burns
 *
 */
public class StopTime {
	private static final String FIELD_NAME_TRIP_ID = "trip_id";
	private static final String FIELD_NAME_ARRIVAL_TIME = "arrival_time";
	private static final String FIELD_NAME_DEPARTURE_TIME = "departure_time";
	private static final String FIELD_NAME_STOP_ID = "stop_id";
	private static final String FIELD_NAME_STOP_SEQUENCE = "stop_sequence";
	private static final String FIELD_NAME_STOP_HEADSIGN = "stop_headsign";
	private static final String FIELD_NAME_PICKUP_TYPE = "pickup_type";
	private static final String FIELD_NAME_DROPOFF_TYPE = "drop_off_type";
	private static final String FIELD_NAME_SHAPE_DIST_TRAVELED =
			"shape_dist_traveled";
	private static final String FIELD_NAME_TIMEPOINT = "timepoint";
	
	private static final Pattern timePattern = 
			Pattern.compile("^(\\d{1,2}):(\\d{2}):(\\d{2})$");
	
	private final String[] requiredFields = {
		StopTime.FIELD_NAME_TRIP_ID,
		StopTime.FIELD_NAME_ARRIVAL_TIME,
		StopTime.FIELD_NAME_DEPARTURE_TIME,
		StopTime.FIELD_NAME_STOP_ID,
		StopTime.FIELD_NAME_STOP_SEQUENCE
	};
	private final String[] optionalFields = {
		StopTime.FIELD_NAME_STOP_HEADSIGN,
		StopTime.FIELD_NAME_PICKUP_TYPE,
		StopTime.FIELD_NAME_DROPOFF_TYPE,
		StopTime.FIELD_NAME_SHAPE_DIST_TRAVELED,
		StopTime.FIELD_NAME_TIMEPOINT
	};
	
	private HashMap<String, String> tableData = new HashMap<>();
	private int recordNumber;
	
	private int arrivalTimeOffset = Integer.MIN_VALUE;
	private int departureTimeOffset = Integer.MIN_VALUE;
	private int stopSequence;
	private PickupDropoffTypeEnum pickupType;
	private PickupDropoffTypeEnum dropoffType;
	private double shapeDistanceTraveled = Double.NaN;
	private boolean timepoint = true;
	private GtfsFile gtfs;
	
	StopTime(GtfsFile gtfs, CsvFile table, int record) throws 
			MissingRequiredFieldException, InvalidDataException {
		this.recordNumber = record;
		this.gtfs = gtfs;
		
		for (int i = 0; i < this.requiredFields.length; i++) {
			String key = this.requiredFields[i];
			try {
				String value = table.getData(key, record);
				this.tableData.put(key, value);
			} catch (ReadPastEndOfTableException ex) {
				throw new IndexOutOfBoundsException();
			} catch (FieldNotFoundException ex) {
				throw new MissingRequiredFieldException(
						GtfsFile.FILENAME_STOP_TIMES, key);
			}
		}
		
		for (int i = 0; i < this.optionalFields.length; i++) {
			String key = this.optionalFields[i];
			try {
				String value = table.getData(key, record);
				this.tableData.put(key, value);
			} catch (ReadPastEndOfTableException ex) {
				/*
				 * should have been caught previously
				 */
				assert(false);
			} catch (FieldNotFoundException ex) {
				/*
				 * No problem; the field is optional
				 */
			}
		}
		
		this.parseTextualData();
	}

	private void parseTextualData() throws InvalidDataException {
		if (this.tableData.containsKey(FIELD_NAME_TIMEPOINT)) {
			String value = tableData.get(FIELD_NAME_TIMEPOINT);
			if ("".equals(value) || "1".equals(value)) {
				this.timepoint = true;
			}
			else if ("0".equals(value)) {
				this.timepoint = false;
			}
			else {
				throw new InvalidDataException(
						GtfsFile.FILENAME_STOP_TIMES, 
						StopTime.FIELD_NAME_TIMEPOINT, 
						this.recordNumber, 
						value);
			}
		}

		this.arrivalTimeOffset = 
				this.parseOffsetFromTime(FIELD_NAME_ARRIVAL_TIME);
		this.departureTimeOffset = 
				this.parseOffsetFromTime(FIELD_NAME_DEPARTURE_TIME);
		
		this.pickupType = this.parsePickupDropoffType(FIELD_NAME_PICKUP_TYPE);
		this.dropoffType = this.parsePickupDropoffType(FIELD_NAME_DROPOFF_TYPE);
		
		String key = FIELD_NAME_SHAPE_DIST_TRAVELED;
		if (this.tableData.containsKey(key)) {
			String value = this.tableData.get(key);
			try {
				this.shapeDistanceTraveled = Double.valueOf(value);
			} catch (NumberFormatException ex1) {
				throw new InvalidDataException(
						GtfsFile.FILENAME_STOP_TIMES, 
						key, this.recordNumber, value);
			}
		}
		
		key = FIELD_NAME_STOP_SEQUENCE;
		String value = this.tableData.get(key);
		try {
			this.stopSequence = Integer.valueOf(value);
		} catch (NumberFormatException ex1) {
			throw new InvalidDataException(
					GtfsFile.FILENAME_STOP_TIMES, 
					key, this.recordNumber, value);
		}
	}
	
	private PickupDropoffTypeEnum parsePickupDropoffType(String key)
			throws InvalidDataException {
		String value = this.tableData.get(key);
		
		if ((value == null) || "".equals(value)) {
			return PickupDropoffTypeEnum.REGULARLY_SCHEDULED;
		}
		
		InvalidDataException ex = new InvalidDataException(
				GtfsFile.FILENAME_STOP_TIMES, key, this.recordNumber, value);
		try {
			int intValue = Integer.valueOf(value);
			if ((intValue < 0) || 
					(intValue > PickupDropoffTypeEnum.values().length)) {
				throw ex;
			}
			return PickupDropoffTypeEnum.values()[intValue];
		} catch (NumberFormatException ex1) {
			throw ex;
		}
	}

	private int parseOffsetFromTime(String key) 
			throws InvalidDataException {
		String value = this.tableData.get(key);
		InvalidDataException ex = new InvalidDataException(
				GtfsFile.FILENAME_STOP_TIMES,
				key,
				this.recordNumber,
				value);
		
		if (value.equals("")) {
			return Integer.MIN_VALUE;
		}
		
		Matcher m = timePattern.matcher(value);
		if (!m.matches()) {
			throw ex;
		}
		int hr = Integer.valueOf(m.group(1));
		int min = Integer.valueOf(m.group(2));
		int sec = Integer.valueOf(m.group(3));
		
		if (min > 59) {
			throw ex;
		}
		if (sec > 59) {
			/*
			 * ignore leap seconds
			 */
			throw ex;
		}
		return hr * 3600 + min * 60 + sec - 43200;
	}

	/**
	 * Gets the stop sequence for this stop time.
	 * @return the stopSequence
	 */
	public int getStopSequence() {
		return this.stopSequence;
	}

	/**
	 * Gets the pickup type for this stop-time. See
	 * {@link PickupDropoffTypeEnum} for definitions.
	 * @return the pickupType
	 */
	public PickupDropoffTypeEnum getPickupType() {
		return this.pickupType;
	}

	/**
	 * Gets the dropoff type for this stop-time. See
	 * {@link PickupDropoffTypeEnum} for definitions.
	 * @return the dropoffType
	 */
	public PickupDropoffTypeEnum getDropoffType() {
		return this.dropoffType;
	}

	/**
	 * Gets the distance traveled on the trip's shape, if defined.
	 * @return the distanced traveled along the shape, if defined; otherwise,
	 * returns {@link Double#NaN}.
	 */
	public double getShapeDistanceTraveled() {
		return this.shapeDistanceTraveled;
	}

	/**
	 * Returns whether this stop is a timepoint. If the stop is a timepoint,
	 * the departure and arrival times are considered exact.
	 * @return {@code true} if this stop is a timepoint; 
	 * {@code false} otherwise.
	 */
	public boolean isTimepoint() {
		return this.timepoint;
	}

	/**
	 * Gets the trip id for this stop time.
	 * @return
	 */
	public String getTripId() {
		return this.tableData.get(FIELD_NAME_TRIP_ID);
	}
	
	/**
	 * Gets the stop id for this stop time.
	 * @return
	 */
	public String getStopId() {
		return this.tableData.get(FIELD_NAME_STOP_ID);
	}
	
	/**
	 * Gets the headsign for this stop time. Per the spec, this value overrides
	 * the headsign for the trip. 
	 * @return The headsign for this stop time, if defined; otherwise, 
	 * {@code null}.
	 */
	public String getStopHeadsign() {
		return this.tableData.get(FIELD_NAME_STOP_HEADSIGN);
	}
	
	/**
	 * Gets the arrival time, if it is defined. In general, it is only
	 * defined if the stop is a timepoint.
	 * @param date The date to use as a seed for the return value.
	 * @return The arrival time, if it is defined; otherwise, {@code null}.
	 */
	public ZonedDateTime getArrivalTime(LocalDate date) {
		if (this.arrivalTimeOffset == Integer.MIN_VALUE) {
			return null;
		}
		
		ZonedDateTime ret = date.atStartOfDay(
				ZoneId.of(this.gtfs.getTimezone()));
		ret = ret.withHour(12).withMinute(0).withSecond(0);
		ret = ret.plusSeconds(arrivalTimeOffset);
		return ret;
	}

	/**
	 * Gets the departure time, if it is defined. In general, it is only
	 * defined if the stop is a timepoint.
	 * @param date The date to use as a seed for the return value
	 * @return The departure time, if it is defined; otherwise, {@code null}.
	 */
	public ZonedDateTime getDepartureTime(LocalDate date) {
		if (this.departureTimeOffset == Integer.MIN_VALUE) {
			return null;
		}
		
		ZonedDateTime ret = date.atStartOfDay(
				ZoneId.of(this.gtfs.getTimezone()));
		ret = ret.withHour(12).withMinute(0).withSecond(0);
		ret = ret.plusSeconds(departureTimeOffset);
		return ret;
	}
}
