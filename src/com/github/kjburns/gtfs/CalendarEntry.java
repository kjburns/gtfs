/*
 * CalendarEntry.java
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
 *   2016-05-15  Basic functionality
 *   2016-06-02  Convert GregorianCalendar usage to java.time
 */
package com.github.kjburns.gtfs;

import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.github.kjburns.gtfs.misc.CsvFile;
import com.github.kjburns.gtfs.misc.CsvFile.FieldNotFoundException;
import com.github.kjburns.gtfs.misc.CsvFile.ReadPastEndOfTableException;

/**
 * A record from calendar.txt
 * @author Kevin J. Burns
 *
 */
class CalendarEntry {
	static final String FIELD_NAME_SERVICE_ID = "service_id";
	private static final String FIELD_NAME_START_DATE = "start_date";
	private static final String FIELD_NAME_END_DATE = "end_date";
	private static final String[] FIELD_NAMES_DAYS_OF_WEEK = {
			null, "sunday", "monday", "tuesday", "wednesday", 
			"thursday", "friday", "saturday"
	};
	private static final Map<DayOfWeek, Integer> dayToIndexMap = 
			mapDaysOfWeek();
	
	private String serviceId;
	private LocalDate startDate;
	private LocalDate endDate;
	/**
	 * Indices for this array run [0..7], allowing GregorianCalendar 
	 * day-of-week constants to be referenced directly as array indices.
	 * Index 0 of this array does not hold any meaningful information.
	 */
	private boolean[] dailyService = new boolean[8];
	
	/**
	 * Constructor. Reads a record from calendar.txt
	 * @param file GTFS file that this calendar entry is part of
	 * @param table table to read from
	 * @param record record number to read
	 * @throws MissingRequiredFieldException if any required field is missing
	 * @throws InvalidDataException If any data is invalid according to the 
	 * spec
	 */
	CalendarEntry(GtfsFile file, CsvFile table, int record)
			throws MissingRequiredFieldException, InvalidDataException {
		String key = null;
		String value = null;
		
		try {
			key = FIELD_NAME_SERVICE_ID;
			value = table.getData(key, record);
			this.serviceId = value;
			
			key = FIELD_NAME_START_DATE;
			value = table.getData(key, record);
			this.startDate = file.parseDate(value);
			
			key = FIELD_NAME_END_DATE;
			value = table.getData(key, record);
			this.endDate = file.parseDate(value);
			
			for (int i = 1; i <= 7; i++) {
				key = FIELD_NAMES_DAYS_OF_WEEK[i];
				value = table.getData(key, record);
				this.dailyService[i] = this.parseBoolean(value);
			}
		} catch (ReadPastEndOfTableException ex) {
			throw new IndexOutOfBoundsException();
		} catch (FieldNotFoundException ex) {
			throw new MissingRequiredFieldException(
					GtfsFile.FILENAME_CALENDAR, key);
		} catch (ParseException ex) {
			throw new InvalidDataException(
					GtfsFile.FILENAME_CALENDAR, key, record, value);
		}
	}
	
	private static Map<DayOfWeek, Integer> mapDaysOfWeek() {
		HashMap<DayOfWeek, Integer> ret = new HashMap<>();
		
		ret.put(DayOfWeek.SUNDAY, Calendar.SUNDAY);
		ret.put(DayOfWeek.MONDAY, Calendar.MONDAY);
		ret.put(DayOfWeek.TUESDAY, Calendar.TUESDAY);
		ret.put(DayOfWeek.WEDNESDAY, Calendar.WEDNESDAY);
		ret.put(DayOfWeek.THURSDAY, Calendar.THURSDAY);
		ret.put(DayOfWeek.FRIDAY, Calendar.FRIDAY);
		ret.put(DayOfWeek.SATURDAY, Calendar.SATURDAY);
		
		return ret;
	}

	private boolean parseBoolean(String value) throws ParseException {
		if (value.equals("1")) {
			return true;
		}
		if (value.equals("0")) {
			return false;
		}
		
		throw new ParseException(value, 0);
	}

	/**
	 * @return the serviceId
	 */
	public String getServiceId() {
		return this.serviceId;
	}
	
	/**
	 * Gets the start date of service for this calendar.
	 * @return the startDate
	 */
	public LocalDate getStartDate() {
		return this.startDate;
	}
	
	/**
	 * Gets the end date of service for this calendar.
	 * @return the endDate
	 */
	public LocalDate getEndDate() {
		return this.endDate;
	}

	/**
	 * Determines whether this calendar entry specifies service on the
	 * provided day of the week.
	 * @param day Day of the week using {@link Calendar} constants. Using
	 * these values, sunday = 1 and saturday = 7.
	 * @return {@code true} if the supplied day of the week is defined to
	 * have service; {@code false} otherwise.
	 */
	private boolean getHasServiceOn(int day) {
		return this.dailyService[day];
	}
	
	/**
	 * Determines whether this calendar entry specifies service on the
	 * provided day of the week.
	 * @param day Day of the week.
	 * @return {@code true} if the supplied day of the week is defined to
	 * have service; {@code false} otherwise. Returns {@code false} if the
	 * argument is null.
	 */
	public boolean getHasServiceOn(DayOfWeek dayOfWeek) {
		if (dayOfWeek == null) {
			return false;
		}
		
		return this.getHasServiceOn(dayToIndexMap.get(dayOfWeek));
	}
}
