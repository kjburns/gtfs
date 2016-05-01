/*
 * Agency.java
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
 *   2016-05-01  Basic functionality
 */
package com.github.kjburns.gtfs;

import java.util.HashMap;

import com.github.kjburns.gtfs.misc.CsvFile;
import com.github.kjburns.gtfs.misc.CsvFile.FieldNotFoundException;
import com.github.kjburns.gtfs.misc.CsvFile.ReadPastEndOfTableException;

/**
 * Data about a transit agency included in a GTFS file.
 * @author Kevin J. Burns
 *
 */
public class Agency {
	private static final String FIELD_NAME_AGENCY_EMAIL = "agency_email";
	private static final String FIELD_NAME_AGENCY_FARE_URL = "agency_fare_url";
	private static final String FIELD_NAME_AGENCY_PHONE = "agency_phone";
	private static final String FIELD_NAME_AGENCY_LANG = "agency_lang";
	private static final String FIELD_NAME_AGENCY_TIMEZONE = "agency_timezone";
	private static final String FIELD_NAME_AGENCY_URL = "agency_url";
	private static final String FIELD_NAME_AGENCY_NAME = "agency_name";
	private static final String FIELD_NAME_AGENCY_ID = "agency_id";
	private String[] requiredFields = { 
			FIELD_NAME_AGENCY_NAME, 
			FIELD_NAME_AGENCY_URL,
			FIELD_NAME_AGENCY_TIMEZONE };
	private String[] optionalFields = { 
			FIELD_NAME_AGENCY_LANG, 
			FIELD_NAME_AGENCY_PHONE, 
			FIELD_NAME_AGENCY_FARE_URL, 
			FIELD_NAME_AGENCY_EMAIL};
	private HashMap<String, String> data = new HashMap<>();
	
	/**
	 * Constructor.
	 * @param table table to read the data from
	 * @param record record number to read the data from, where the first 
	 * record is #1. If the supplied record number is invalid, expect 
	 * IndexOutOfBoundsException to be raised.
	 * @throws MissingRequiredFieldException if the table is missing one or
	 * more required fields
	 */
	Agency(CsvFile table, int record) throws MissingRequiredFieldException {
		this.runValidRecordCheck(table, record);
		this.runRequiredFieldCheck(table);
		this.readData(table, record);
	}

	private void runValidRecordCheck(CsvFile table, int record) {
		if ((record < 1) || (record > table.getRecordCount())) {
			throw new IndexOutOfBoundsException(
					"Invalid record number in " +
					GtfsFile.FILENAME_AGENCY);
		}
	}

	private void readData(CsvFile table, int record) {
		for (int i = 0; i < this.requiredFields.length; i++) {
			String key = this.requiredFields[i];
			try {
				String value = table.getData(key, record);
				this.data.put(key, value);
			} catch (ReadPastEndOfTableException | FieldNotFoundException ex) {
				/*
				 * Neither exception should occur, as these have already been
				 * checked in run...Check() methods
				 */
				assert(false);
			}
		}
		
		try {
			String value = table.getData(FIELD_NAME_AGENCY_ID, record);
			this.data.put(FIELD_NAME_AGENCY_ID, value);
		} catch (ReadPastEndOfTableException ex) {
			/*
			 * Shouldn't occur, as this has already been checked in
			 * runValidRecordCheck()
			 */
			assert(false);
		} catch (FieldNotFoundException ex) {
			if (this.isAgencyIdFieldRequired(table)) {
				/*
				 * Should never get here if this record is required
				 */
				assert(false);
			}
			else {
				/*
				 * Nothing to do here, as the field has been determined to be
				 * optional
				 */
			}
		}
		
		for (int i = 0; i < this.optionalFields.length; i++) {
			String key = this.optionalFields[i];
			String value;
			try {
				value = table.getData(key, record);
				this.data.put(key, value);
			} catch (ReadPastEndOfTableException ex) {
				/*
				 * Shouldn't occur, as this has already been checked in
				 * runValidRecordCheck()
				 */
				assert(false);
			} catch (FieldNotFoundException ex) {
				/*
				 * No worries, the field isn't required.
				 */
			}
		}
	}

	private void runRequiredFieldCheck(CsvFile table) 
			throws MissingRequiredFieldException {
		for (int i = 0; i < this.requiredFields.length; i++) {
			if (!table.fieldExists(this.requiredFields[i])) {
				throw new MissingRequiredFieldException(
						GtfsFile.FILENAME_AGENCY, requiredFields[i]);
			}
		}
		
		if (!table.fieldExists(FIELD_NAME_AGENCY_ID)) {
			if (this.isAgencyIdFieldRequired(table)) {
				throw new MissingRequiredFieldException(
						GtfsFile.FILENAME_AGENCY, FIELD_NAME_AGENCY_ID);
			}
		}
	}

	private boolean isAgencyIdFieldRequired(CsvFile table) {
		return (table.getRecordCount() > 1);
	}

	/**
	 * @return the agencyID if known; otherwise, {@code null}
	 */
	public String getAgencyID() {
		return this.data.get(FIELD_NAME_AGENCY_ID);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.data.get(FIELD_NAME_AGENCY_NAME);
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return this.data.get(FIELD_NAME_AGENCY_URL);
	}

	/**
	 * @return the timeZone
	 */
	public String getTimeZone() {
		return this.data.get(FIELD_NAME_AGENCY_TIMEZONE);
	}

	/**
	 * @return the language if known; otherwise, {@code null}
	 */
	public String getLanguage() {
		return this.data.get(FIELD_NAME_AGENCY_LANG);
	}

	/**
	 * @return the phoneNumber if known; otherwise, {@code null}
	 */
	public String getPhoneNumber() {
		return this.data.get(FIELD_NAME_AGENCY_PHONE);
	}

	/**
	 * @return the fareUrl if known; otherwise, {@code null}
	 */
	public String getFareUrl() {
		return this.data.get(FIELD_NAME_AGENCY_FARE_URL);
	}

	/**
	 * @return the email if known; otherwise, {@code null}
	 */
	public String getEmail() {
		return this.data.get(FIELD_NAME_AGENCY_EMAIL);
	}
}
