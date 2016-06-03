/*
 * ServiceCalendar.java
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.github.kjburns.gtfs.misc.CsvFile;

/**
 * An aggregation of the contents of calendar.txt and calendar_dates.txt
 * @author Kevin J. Burns
 *
 */
public class ServiceCalendar {
	private GtfsFile gtfs;
	private HashMap<String, CalendarEntry> entries = new HashMap<>();
	private HashMap<String, HashMap<LocalDate, CalendarOverride>> overrides =
			new HashMap<>();
	
	/**
	 * Constructor. Reads calendar.txt and calendar_dates.txt to get service
	 * dates.
	 * @param gtfs The GTFS file that this will be part of 
	 * @throws MissingRequiredFieldException If any required fields are missing
	 * @throws InvalidDataException If any tables include invalid data
	 * @throws IOException If either file cannot be read for any reason
	 * @throws DatasetUniquenessException If data which are required to be
	 * dataset-unique appear multiple times
	 */
	ServiceCalendar(GtfsFile gtfs) 
			throws MissingRequiredFieldException, InvalidDataException, 
					IOException, DatasetUniquenessException {
		this.gtfs = gtfs;

		this.readBasics();
		this.readOverrides();
	}

	private void readBasics() 
			throws MissingRequiredFieldException, InvalidDataException, 
				IOException, DatasetUniquenessException {
		File basicFile = 
				gtfs.getZipFile().getEntry(GtfsFile.FILENAME_CALENDAR);
		if (!basicFile.exists()) {
			return;
		}
		
		try(FileInputStream fis = new FileInputStream(basicFile)) {
			CsvFile table = new CsvFile(fis);
			for (int record = 1; record <= table.getRecordCount(); record++) {
				CalendarEntry entry = new CalendarEntry(gtfs, table, record);
				String key = entry.getServiceId();
				
				if (this.entries.containsKey(key)) {
					throw new DatasetUniquenessException(
							GtfsFile.FILENAME_CALENDAR, 
							CalendarEntry.FIELD_NAME_SERVICE_ID, 
							key);
				}
				this.entries.put(key, entry);
			}
		} catch (FileNotFoundException ex) {
			/*
			 * This has already been checked; should never happen
			 */
			assert(false);
		}
	}

	private void readOverrides() 
			throws IOException, MissingRequiredFieldException, 
				InvalidDataException, DatasetUniquenessException {
		File overridesFile = gtfs.getZipFile().getEntry(
				GtfsFile.FILENAME_CALENDAR_OVERRIDES);
		if (!overridesFile.exists()) {
			return;
		}
		
		try(FileInputStream fis = new FileInputStream(overridesFile)) {
			CsvFile table = new CsvFile(fis);
			
			for (int record = 1; record <= table.getRecordCount(); record++) {
				CalendarOverride co = 
						new CalendarOverride(gtfs, table, record);
				String serviceId = co.getServiceId();
				
				HashMap<LocalDate, CalendarOverride> overrideMapForService;
				if (this.overrides.containsKey(serviceId)) {
					overrideMapForService = this.overrides.get(serviceId);
				}
				else {
					overrideMapForService = 
							new HashMap<LocalDate, CalendarOverride>();
					this.overrides.put(serviceId, overrideMapForService);
				}
				
				LocalDate key = co.getDate();
				if (overrideMapForService.containsKey(key)) {
					DateTimeFormatter df = 
							DateTimeFormatter.ofPattern("yyyy MM dd");
					throw new DatasetUniquenessException(
							GtfsFile.FILENAME_CALENDAR_OVERRIDES, 
							CalendarOverride.FIELD_NAME_SERVICE_ID + "+" + 
									CalendarOverride.FIELD_NAME_DATE, 
							serviceId + "+" + key.format(df));
				}
				overrideMapForService.put(key, co);
			}
		} catch (FileNotFoundException ex) {
			/*
			 * This has already been checked; should never happen
			 */
			assert(false);
		}
	}
	
	/**
	 * Gets a list of available service ids.
	 * @return
	 */
	public List<String> getServiceIds() {
		List<String> ret = new ArrayList<>();
		
		ret.addAll(this.entries.keySet());
		
		for (String serviceId : this.overrides.keySet()) {
			if (!ret.contains(serviceId)) {
				ret.add(serviceId);
			}
		}
		
		return ret;
	}
	
	/**
	 * Determines whether a particular service id is available on a date. 
	 * @param serviceId service_id to query
	 * @param date date to query
	 * @return {@code true} if service is defined on that date; {@code false}
	 * otherwise.
	 */
	public boolean isServiceDefinedOn(String serviceId, LocalDate date) {
		boolean available = false;
		
		CalendarEntry entry = this.entries.get(serviceId);
		if (entry != null) {
			if (date.isBefore(entry.getStartDate()) || 
					date.isAfter(entry.getEndDate())) {
				available = false;
			}
			else {
				if (entry.getHasServiceOn(date.getDayOfWeek())) {
					available = true;
				}
			}
		}
		
		HashMap<LocalDate, CalendarOverride> serviceOverride = 
				this.overrides.get(serviceId);
		if (serviceOverride != null) {
			CalendarOverride exc = serviceOverride.get(date);
			if (exc != null) {
				switch(exc.getOverrideType()) {
				case SERVICE_ADDED:
					available = true;
					break;
				case SERVICE_REMOVED:
					available = false;
					break;
				default:
					/*
					 * should never get here
					 */
					assert(false);
					break;
				}
			}
		}
		
		return available;
	}
}
