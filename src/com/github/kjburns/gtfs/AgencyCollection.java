/*
 * AgencyCollection.java
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

import com.github.kjburns.gtfs.misc.CsvFile;

public class AgencyCollection implements Iterable<Agency> {
	private HashMap<String, Agency> agencies = new HashMap<>();
	
	AgencyCollection(File f) 
			throws IOException, MissingRequiredFieldException {
		try (InputStream is = new FileInputStream(f)) {
			CsvFile table = new CsvFile(is);
			
			for (int i = 1; i <= table.getRecordCount(); i++) {
				Agency a = new Agency(table, i);
				this.agencies.put(a.getAgencyID(), a);
			}
		} catch (MissingRequiredFieldException ex) {
			/*
			 * The whole file is invalid if there is a missing field, so
			 * pass this exception along
			 */
			throw(ex);
		}
	}
	
	/**
	 * Gets the number of agencies whose information is collected here.
	 * @return
	 */
	public int getAgencyCount() {
		return this.agencies.size();
	}
	
	/**
	 * Fetches an agency's information based on its agency_id field in
	 * agency.txt in the gtfs file.
	 * <p>
	 * Note that by the gtfs specification agency_id is an optional field
	 * if only one transit agency is represented. If the original file does
	 * not have an agency_id field, the agency_id is stored as {@code null}.
	 * If the original file has an agency_id field but the field is empty,
	 * the agency_id is stored as an empty string.
	 * </p>
	 * @param id unique identifier for the agency
	 * @return the agency's information, if it can be fetched based on the
	 * supplied id; otherwise, {@code null}
	 */
	public Agency getAgencyById(String id) {
		return this.agencies.get(id);
	}

	@Override
	public Iterator<Agency> iterator() {
		return this.agencies.values().iterator();
	}
}
