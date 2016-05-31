/*
 * GtfsFile.java
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
 * Development Log:
 *   2016-04-29  Load zip file from disk
 *   2016-05-01  Load and retrieve transit agencies
 *   2016-05-01  Raise exception if dataset-unique field contains duplicate
 *               values
 *   2016-05-02  Load and retrieve stops
 *   2016-05-06  Load and process transfers.txt
 *   2016-05-07  Load and process routes.txt
 *   2016-05-11  Load and process shapes.txt
 *   2016-05-11  Add ParentStationNotStation exception when loading stops
 *   2016-05-15  Load and process calendar.txt and calendar_dates.txt
 *   2016-05-18  Load and process trips.txt
 *   2016-05-30  Load and process stop_times.txt
 * Revision Log:
 */
package com.github.kjburns.gtfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingWorker;

import com.github.kjburns.gtfs.misc.CsvFile;
import com.github.kjburns.gtfs.misc.ZipWrapper;

/**
 * An object which retrieves data from a General Transit Feed Specification
 * (GTFS) file. 
 * @see <a href="https://developers.google.com/transit/gtfs/reference">
 * Specification</a>
 * @author Kevin J. Burns
 *
 */
public class GtfsFile implements AutoCloseable {
	private ZipWrapper zipFile = null;
	
	private AgencyCollection transitAgencies;
	private StopCollection stops;
	private RouteCollection routes;
	private TransitShapeCollection shapes;
	private ServiceCalendar serviceCalendar;
	private TripCollection trips;
	private StopTimeCollection stopTimes;

	private static final Pattern DATE_PATTERN = 
				Pattern.compile("^(\\d{4})(\\d{2})(\\d{2})$");

	static final String FILENAME_AGENCY = "agency.txt";
	static final String FILENAME_STOPS = "stops.txt";
	static final String FILENAME_FARE_RULES = "fare_rules.txt";
	static final String FILENAME_TRANSFERS = "transfers.txt";
	static final String FILENAME_ROUTES = "routes.txt";
	static final String FILENAME_SHAPES = "shapes.txt";
	static final String FILENAME_CALENDAR = "calendar.txt";
	static final String FILENAME_CALENDAR_OVERRIDES = "calendar_dates.txt";
	static final String FILENAME_TRIPS = "trips.txt";
	static final String FILENAME_STOP_TIMES = "stop_times.txt";
	
	/**
	 * Loads a GTFS file from disk. The file is loaded lazily (i.e., individual
	 * text files are only parsed as they are needed).
	 * @param path Path to the file to load. This file must be a zip file.
	 * @param worker An optional worker thread to report progress to. 
	 * Invocation of cancel on the worker thread will be honored on a 
	 * best-effort basis. If there is no worker thread, pass {@code null}.
	 * @throws IOException If there are problems opening the supplied zip file
	 * @throws InterruptedException if a worker thread was passed and it was
	 * canceled prematurely 
	 * @throws MissingRequiredFieldException if any of the files have a 
	 * required field which is missing.
	 * @throws DatasetUniquenessException if a file with a dataset-unique
	 * field contains illegal duplicate values
	 * @throws InvalidDataException if any data is invalid by the spec
	 * @throws ParentStationNotStationException if a stop is listed with a
	 * parent station, but the alleged parent station is not a station
	 * @throws TerminalTimepointException if a trip fails to start and end 
	 * with a timepoint
	 */
	public GtfsFile(String path, SwingWorker<?, ?> worker) 
			throws IOException, InterruptedException, 
					MissingRequiredFieldException, DatasetUniquenessException, 
					InvalidDataException, ParentStationNotStationException, 
					TerminalTimepointException {
		this.zipFile = new ZipWrapper(path, worker);
		if (worker != null) {
			if (worker.isCancelled()) {
				throw new InterruptedException();
			}
		}

		/*
		 * The following files are required, so if IOException is raised it
		 * will be passed along to caller
		 */
		try {
			this.loadAgencies();
			this.loadStops();
			this.loadRoutes();
			this.loadTrips();
			
			this.serviceCalendar = new ServiceCalendar(this);
			this.loadStopTimes();
		} catch (MissingRequiredFieldException ex) {
			/*
			 * If a required field is missing, the file is invalid. Pass along
			 * the exception for now, although later it may be desirable to
			 * allow processing to continue because the missing fields in this
			 * file don't necessarily preclude the useful processing of the
			 * feed.
			 */
			throw ex;
		}
		
		/*
		 * The following files are optional.
		 */
		try {
			this.loadTransfers();
		} catch (IOException| MissingRequiredFieldException | 
				InvalidDataException ex) {
			/*
			 * Since the file is optional, do nothing for now, but maybe
			 * log it or something later 
			 */
		}
		try {
			this.loadShapes();
		} catch (Exception ex) {
			/*
			 * Since the file is optional, do nothing for now, but maybe
			 * log it or something later 
			 */
		}
	}
	
	private void loadStopTimes() 
			throws IOException, MissingRequiredFieldException, 
			InvalidDataException, TerminalTimepointException {
		File stopTimesFile = this.zipFile.getEntry(FILENAME_STOP_TIMES);
		this.stopTimes = new StopTimeCollection(this, stopTimesFile);
	}

	private void loadTrips() 
			throws IOException, MissingRequiredFieldException, 
					InvalidDataException, DatasetUniquenessException {
		File tripsFile = this.zipFile.getEntry(FILENAME_TRIPS);
		this.trips = new TripCollection(tripsFile);
	}

	private void loadShapes() 
			throws IOException, MissingRequiredFieldException, 
					InvalidDataException {
		File shapesFile = this.zipFile.getEntry(FILENAME_SHAPES);
		if (!shapesFile.exists()) {
			return;
		}
		
		this.shapes = new TransitShapeCollection(shapesFile);
	}

	private void loadRoutes() 
			throws IOException, MissingRequiredFieldException, 
					InvalidDataException {
		File routesFile = this.zipFile.getEntry(FILENAME_ROUTES);
		this.routes = new RouteCollection(routesFile);
	}

	private void loadTransfers() 
			throws IOException, MissingRequiredFieldException, 
				InvalidDataException {
		File transfersFile = this.zipFile.getEntry(FILENAME_TRANSFERS);
		if (transfersFile == null) {
			return;
		}
		if (!transfersFile.exists()) {
			return;
		}
		
		try(FileInputStream fis = new FileInputStream(transfersFile)) {
			CsvFile table = new CsvFile(fis);
			for (int i = 1; i <= table.getRecordCount(); i++) {
				TransferRule rule = new TransferRule(table, i);
				this.stops.registerTransferRule(rule);
			}
		}
	}

	private void loadAgencies() 
			throws IOException, MissingRequiredFieldException, 
					DatasetUniquenessException {
		File agencyFile = this.zipFile.getEntry(FILENAME_AGENCY);
		this.transitAgencies = new AgencyCollection(agencyFile);
	}
	
	private void loadStops() 
			throws IOException, InvalidDataException, 
			MissingRequiredFieldException, DatasetUniquenessException, 
			ParentStationNotStationException {
		File stopsFile = this.zipFile.getEntry(FILENAME_STOPS);
		this.stops = new StopCollection(this, stopsFile);
	}

	@Override
	public void close() throws IOException {
		if (this.zipFile != null) {
			this.zipFile.close();
		}
	}

	/**
	 * Gets a collection of transit agencies that are represented in this
	 * file.
	 * @return the transitAgencies
	 */
	public AgencyCollection getTransitAgencies() {
		return this.transitAgencies;
	}
	
	boolean isFilePresent(String filename) {
		return this.zipFile.getEntry(filename) != null;
	}
	
	/**
	 * Gets a collection of stops listed in this file
	 * @return
	 */
	public StopCollection getStops() {
		return this.stops;
	}
	
	/**
	 * Gets the time zone used in this file 
	 * @return
	 */
	public String getTimezone() {
		Iterator<Agency> it = this.transitAgencies.iterator();
		Agency agency = it.next();
		
		return agency.getTimeZone();
	}
	
	/**
	 * Gets the routes listed in this file
	 * @return
	 */
	public RouteCollection getRoutes() {
		return this.routes;
	}

	/**
	 * Gets the shapes listed in this file. Since shapes.txt is optional,
	 * the return value may be null.
	 * @return
	 */
	public TransitShapeCollection getShapes() {
		return this.shapes;
	}

	/**
	 * Parses a date in yyyymmdd format.
	 * @param date The date to parse
	 * @return A GregorianCalendar object set to the specified date, in the
	 * timezone of the gtfs file, and set to midnight local time.
	 * @throws ParseException If the string cannot be parsed
	 */
	GregorianCalendar parseDate(String date) throws ParseException {
		Matcher m = GtfsFile.DATE_PATTERN.matcher(date);
		if (!m.matches()) {
			throw new ParseException(date, 0);
		}
		int year = Integer.valueOf(m.group(1));
		int month = Integer.valueOf(m.group(2));
		int day = Integer.valueOf(m.group(3));
		
		TimeZone tz = TimeZone.getTimeZone(getTimezone());
		GregorianCalendar ret = new GregorianCalendar(tz);
		/*
		 * read something from the calendar so the time zone will take effect 
		 */
		ret.get(Calendar.DAY_OF_MONTH);
		/*
		 * now set to midnight local time
		 */
		ret.set(Calendar.HOUR, 0);
		ret.set(Calendar.MINUTE, 0);
		ret.set(Calendar.SECOND, 0);
		ret.get(Calendar.HOUR);
		/*
		 * now set date
		 */
		ret.set(Calendar.YEAR, year);
		ret.set(Calendar.MONTH, month - 1);
		ret.set(Calendar.DAY_OF_MONTH, day);
		ret.get(Calendar.DAY_OF_MONTH);
		
		return ret;
	}
	
	ZipWrapper getZipFile() {
		return this.zipFile;
	}

	/**
	 * @return the serviceCalendar
	 */
	public ServiceCalendar getServiceCalendar() {
		return this.serviceCalendar;
	}

	/**
	 * @return the trips
	 */
	public TripCollection getTrips() {
		return this.trips;
	}
	
	/**
	 * Gets all timetables defined in this file
	 * @return
	 */
	public StopTimeCollection getAllTimetables() {
		return this.stopTimes;
	}
}
