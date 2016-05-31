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
 */
package com.github.kjburns.gtfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import com.github.kjburns.gtfs.misc.CsvFile;

/**
 * An aggregation of the contents of calendar.txt and calendar_dates.txt
 * @author Kevin J. Burns
 *
 */
public class ServiceCalendar {
	private GtfsFile gtfs;
	private HashMap<String, CalendarEntry> entries = new HashMap<>();
	private HashMap<String, HashMap<Long, CalendarOverride>> 
			overrides =	new HashMap<>();
	
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
				
				HashMap<Long, CalendarOverride>	overrideMapForService;
				if (this.overrides.containsKey(serviceId)) {
					overrideMapForService = this.overrides.get(serviceId);
				}
				else {
					overrideMapForService = 
							new HashMap<Long, CalendarOverride>();
					this.overrides.put(serviceId, overrideMapForService);
				}
				
				long key = this.makeYMD(co.getDate());
				if (overrideMapForService.containsKey(key)) {
					SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
					throw new DatasetUniquenessException(
							GtfsFile.FILENAME_CALENDAR_OVERRIDES, 
							CalendarOverride.FIELD_NAME_SERVICE_ID + "+" + 
									CalendarOverride.FIELD_NAME_DATE, 
							serviceId + "+" + df.format(key));
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
	 * @param cal date to query
	 * @return {@code true} if service is defined on that date; {@code false}
	 * otherwise.
	 */
	public boolean isServiceDefinedOn(
			String serviceId, GregorianCalendar cal) {
		boolean available = false;
		
		CalendarEntry entry = this.entries.get(serviceId);
		long ymd = this.makeYMD(cal);
		if (entry != null) {
			long startYmd = this.makeYMD(entry.getStartDate());
			long endYmd = this.makeYMD(entry.getEndDate());
			
			if ((ymd < startYmd) || (ymd > endYmd)) {
				available = false;
			}
			else {
				if (entry.getHasServiceOn(cal.get(Calendar.DAY_OF_WEEK))) {
					available = true;
				}
			}
		}
		
		HashMap<Long, CalendarOverride> serviceOverrides = 
				this.overrides.get(serviceId);
		if (serviceOverrides != null) {
			CalendarOverride exc = serviceOverrides.get(ymd);
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
	
	/**
	 * Determines whether a particular service id is available on a date. 
	 * <p>
	 * This method does not check for date composition blunders directly
	 * (e.g., 29 February on a non-leap-year, 31 April in any year). Instead
	 * the values are fed into GregorianCalendar, and any errors are handled
	 * according to that specification.
	 * </p>
	 * @param serviceId service_id to query
	 * @param yr year of date to check
	 * @param mo month of date to check
	 * @param dy day of date to check
	 * @return {@code true} if service is defined on that date; {@code false}
	 * otherwise.
	 */
	public boolean isServiceDefinedOn(
			String serviceId, int yr, int mo, int dy) {
		return this.isServiceDefinedOn(serviceId, this.makeGc(yr, mo, dy));
	}
	
	private GregorianCalendar makeGc(int yr, int mo, int dy) {
		TimeZone tz = TimeZone.getTimeZone(this.gtfs.getTimezone());
		GregorianCalendar ret = new GregorianCalendar(tz);
		
		ret.set(yr, mo - 1, dy);
		
		return ret;
	}
	
	private long makeYMD(GregorianCalendar gc) {
		return this.makeYMD(gc.get(Calendar.YEAR), 
							gc.get(Calendar.MONTH) + 1, 
							gc.get(Calendar.DAY_OF_MONTH));
	}
	
	private long makeYMD(int yr, int mo, int dy) {
		return yr * 10000 + mo * 100 + dy;
	}
}
