/*
 * CalendarOverride.java
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
 *   2016-06-02  Replace GregorianCalendar functionality with java.time
 */
package com.github.kjburns.gtfs;

import java.text.ParseException;
import java.time.LocalDate;
import com.github.kjburns.gtfs.misc.CsvFile;
import com.github.kjburns.gtfs.misc.CsvFile.FieldNotFoundException;
import com.github.kjburns.gtfs.misc.CsvFile.ReadPastEndOfTableException;

/**
 * An override of entries in calendar.txt, either adding additional service or
 * removing service on a certain date.
 * @author Kevin J. Burns
 *
 */
public class CalendarOverride {
	enum ExceptionTypeEnum {
		SERVICE_ADDED,
		SERVICE_REMOVED;
	}
	
	static final String FIELD_NAME_SERVICE_ID = "service_id";
	static final String FIELD_NAME_DATE = "date";
	private static final String FIELD_NAME_EXCEPTION_TYPE = "exception_type";
	
	private String serviceId;
	private LocalDate date;
	private ExceptionTypeEnum overrideType;
	
	/**
	 * Constructor. Reads a line from the table in calendar_dates.txt.
	 * @param file GTFS file that this entry is part of
	 * @param table the table to read from
	 * @param record the record to read
	 * @throws MissingRequiredFieldException if any required fields are missing
	 * @throws InvalidDataException if the record contains invalid data
	 */
	CalendarOverride(GtfsFile file, CsvFile table, int record) 
			throws MissingRequiredFieldException, InvalidDataException {
		String key = null;
		String value = null;
		
		try {
			key = FIELD_NAME_SERVICE_ID;
			value = table.getData(key, record);
			this.serviceId = value;
			
			key = FIELD_NAME_DATE;
			value = table.getData(key, record);
			this.date = file.parseDate(value);
			
			key = FIELD_NAME_EXCEPTION_TYPE;
			value = table.getData(key, record);
			if (value.equals("1")) {
				this.overrideType = ExceptionTypeEnum.SERVICE_ADDED;
			}
			else if (value.equals("2")) {
				this.overrideType = ExceptionTypeEnum.SERVICE_REMOVED;
			}
			else {
				throw new ParseException(value, 0);
			}
		} catch (ReadPastEndOfTableException ex) {
			throw new IndexOutOfBoundsException();
		} catch (FieldNotFoundException ex) {
			throw new MissingRequiredFieldException(
					GtfsFile.FILENAME_CALENDAR_OVERRIDES, key);
		} catch (ParseException ex) {
			throw new InvalidDataException(
					GtfsFile.FILENAME_CALENDAR_OVERRIDES, key, record, value);
		}
	}

	/**
	 * @return the serviceId
	 */
	public String getServiceId() {
		return this.serviceId;
	}

	/**
	 * @return the overrideType
	 */
	public ExceptionTypeEnum getOverrideType() {
		return this.overrideType;
	}

	/**
	 * Gets the date that this override represents
	 * @return the date
	 */
	public LocalDate getDate() {
		return this.date;
	}
}
