/*
 * RouteCollection.java
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
 *   2016-05-07  Basic functionality
 */
package com.github.kjburns.gtfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import com.github.kjburns.gtfs.misc.CsvFile;

/**
 * A collection of Routes.
 * @author Kevin J. Burns
 *
 */
public class RouteCollection {
	private HashMap<String, Route> routes = new HashMap<>();
	
	/**
	 * Constructor. Reads routes from routes.txt
	 * @param f File referencing routes.txt in the gtfs file
	 * @throws IOException if the file cannot be read for some reason 
	 * @throws InvalidDataException If invalid data exist in the table
	 * @throws MissingRequiredFieldException if required fields are missing
	 */
	RouteCollection(File f) 
			throws IOException, MissingRequiredFieldException, 
				InvalidDataException {
		try(FileInputStream fis = new FileInputStream(f)) {
			CsvFile table = new CsvFile(fis);
			int recordCount = table.getRecordCount();
			for (int record = 1; record <= recordCount; record++) {
				Route rt = new Route(table, record);
				this.routes.put(rt.getRouteId(), rt);
			}
		}
	}
	
	/**
	 * Fetches the route by its id, if it exists.
	 * @param id route id
	 * @return Route, if it exists; otherwise {@code null}.
	 */
	public Route getRouteById(String id) {
		return this.routes.get(id);
	}
}
