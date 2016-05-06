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
 * Revision Log:
 *   2016-04-29  Load zip file from disk
 *   2016-05-01  Load and retrieve transit agencies
 *   2016-05-01  Raise exception if dataset-unique field contains duplicate
 *               values
 *   2016-05-02  Load and retrieve stops
 *   2016-05-06  Load and process transfers.txt
 */
package com.github.kjburns.gtfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

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

	static final String FILENAME_AGENCY = "agency.txt";
	static final String FILENAME_STOPS = "stops.txt";
	static final String FILENAME_FARE_RULES = "fare_rules.txt";
	static final String FILENAME_TRANSFERS = "transfers.txt";
	
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
	 */
	public GtfsFile(String path, SwingWorker<?, ?> worker) 
			throws IOException, InterruptedException, 
					MissingRequiredFieldException, DatasetUniquenessException, InvalidDataException {
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
	}
	
	private void loadTransfers() 
			throws IOException, MissingRequiredFieldException, 
				InvalidDataException {
		File transfersFile = this.zipFile.getEntry(FILENAME_TRANSFERS);
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
			MissingRequiredFieldException, DatasetUniquenessException {
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
	
	public StopCollection getStops() {
		return this.stops;
	}
	
	public String getTimezone() {
		Iterator<Agency> it = this.transitAgencies.iterator();
		Agency agency = it.next();
		
		return agency.getTimeZone();
	}
}
