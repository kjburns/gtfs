/*
 * Route.java
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

import java.util.HashMap;
import java.util.regex.Pattern;

import com.github.kjburns.gtfs.misc.CsvFile;
import com.github.kjburns.gtfs.misc.CsvFile.FieldNotFoundException;
import com.github.kjburns.gtfs.misc.CsvFile.ReadPastEndOfTableException;

public class Route {
	private static final String FIELD_NAME_ROUTE_ID = "route_id";
	private static final String FIELD_NAME_AGENCY_ID = "agency_id";
	private static final String FIELD_NAME_ROUTE_SHORT_NAME = 
			"route_short_name";
	private static final String FIELD_NAME_ROUTE_LONG_NAME =
			"route_long_name";
	private static final String FIELD_NAME_ROUTE_DESC = "route_desc";
	private static final String FIELD_NAME_ROUTE_TYPE = "route_type";
	private static final String FIELD_NAME_ROUTE_URL = "route_url";
	private static final String FIELD_NAME_ROUTE_COLOR = "route_color";
	private static final String FIELD_NAME_ROUTE_TEXT_COLOR = 
			"route_text_color";
	
	private final String[] requiredFields =
		{		FIELD_NAME_ROUTE_ID, 
				FIELD_NAME_ROUTE_SHORT_NAME,
				FIELD_NAME_ROUTE_LONG_NAME, 
				FIELD_NAME_ROUTE_TYPE
		};
	
	private final String[] optionalFields =
		{		FIELD_NAME_AGENCY_ID,
				FIELD_NAME_ROUTE_DESC,
				FIELD_NAME_ROUTE_URL,
				FIELD_NAME_ROUTE_COLOR,
				FIELD_NAME_ROUTE_TEXT_COLOR
		};
	
	/**
	 * Types of vehicle used on routes.
	 * @author Kevin J. Burns
	 *
	 */
	public enum RouteTypeEnum {
		/**
		 * From the specification:
		 * <blockquote>
		 * Tram, Streetcar, Light rail. Any light rail or street level system 
		 * within a metropolitan area. 
		 * </blockquote>
		 */
		STREET_RAIL,
		/**
		 * From the specification:
		 * <blockquote>
		 * Subway, Metro. Any underground rail system within a metropolitan 
		 * area.
		 * </blockquote>
		 */
		UNDERGROUND,
		/**
		 * From the specification:
		 * <blockquote>
		 * Rail. Used for intercity or long-distance travel.
		 * </blockquote>
		 */
		RAIL,
		/**
		 * From the specification:
		 * <blockquote>
		 * Bus. Used for short- and long-distance bus routes.
		 * </blockquote>
		 */
		BUS,
		/**
		 * From the specification:
		 * <blockquote>
		 * Ferry. Used for short- and long-distance boat service.
		 * </blockquote>
		 */
		FERRY,
		/**
		 * From the specification:
		 * <blockquote>
		 * Cable car. Used for street-level cable cars where the cable runs 
		 * beneath the car.
		 * </blockquote>
		 */
		CABLE_CAR,
		/**
		 * From the specification:
		 * <blockquote>
		 * Gondola, Suspended cable car. Typically used for aerial cable cars 
		 * where the car is suspended from the cable.
		 * </blockquote>
		 */
		GONDOLA,
		/**
		 * From the specification:
		 * <blockquote>
		 * Funicular. Any rail system designed for steep inclines.
		 * </blockquote>
		 */
		FUNICULAR
	}
	
	private HashMap<String, String> dataFromTable = new HashMap<>();
	private RouteTypeEnum routeType;
	
	/**
	 * Constructor. Reads a route from the specified table record.
	 * @param table table to read from
	 * @param record record number to read, where record #1 is the first.
	 * @throws MissingRequiredFieldException if any required fields are missing
	 * @throws InvalidDataException if any data is invalid according to spec
	 */
	Route(CsvFile table, int record) throws MissingRequiredFieldException, InvalidDataException {
		/*
		 * Read required fields 
		 */
		for (int i = 0; i < requiredFields.length; i++) {
			String key = this.requiredFields[i];
			try {
				String value = table.getData(key, record);
				this.dataFromTable.put(key, value);
			} catch (ReadPastEndOfTableException ex) {
				throw new IndexOutOfBoundsException();
			} catch (FieldNotFoundException ex) {
				throw new MissingRequiredFieldException(
						GtfsFile.FILENAME_ROUTES, key);
			}
		}
		/*
		 * Read optional fields
		 */
		for (int i = 0; i < optionalFields.length; i++) {
			String key = this.optionalFields[i];
			if (!table.fieldExists(key)) {
				continue;
			}
			try {
				String value = table.getData(key, record);
				this.dataFromTable.put(key, value);
			} catch (ReadPastEndOfTableException | FieldNotFoundException ex) {
				/*
				 * Neither of these exceptions could come up.
				 * ReadPastEndOfTable would have been handled in the required
				 * fields. FieldNotFoundException is prevented by checking
				 * that the field exists earlier.
				 */
				assert(false);
			}
		}
		/*
		 * Interpret route type
		 */
		String rtTypeStr = this.dataFromTable.get(FIELD_NAME_ROUTE_TYPE);
		InvalidDataException badRouteTypeException = new InvalidDataException(
				GtfsFile.FILENAME_ROUTES, 
				FIELD_NAME_ROUTE_TYPE, 
				record, 
				rtTypeStr);
		try {
			int rtTypeInt = Integer.valueOf(rtTypeStr);
			this.routeType = RouteTypeEnum.values()[rtTypeInt];
		} catch (NumberFormatException | IndexOutOfBoundsException ex) {
			throw badRouteTypeException;
		}
		
		/*
		 * Make sure colors are coded correctly
		 */
		Pattern hexColorPattern = Pattern.compile("^[0-9A-Fa-f]{6}$");
		if (this.dataFromTable.containsKey(FIELD_NAME_ROUTE_COLOR)) {
			String colorCode = this.dataFromTable.get(FIELD_NAME_ROUTE_COLOR);
			colorCode = colorCode.trim();
			if (colorCode.length() > 0) {
				if (!hexColorPattern.matcher(colorCode).matches()) {
					throw new InvalidDataException(
							GtfsFile.FILENAME_ROUTES, 
							FIELD_NAME_ROUTE_COLOR, 
							record, 
							colorCode);
				}
			}
		}
		if (this.dataFromTable.containsKey(FIELD_NAME_ROUTE_TEXT_COLOR)) {
			String colorCode = this.dataFromTable.get(
					FIELD_NAME_ROUTE_TEXT_COLOR);
			colorCode = colorCode.trim();
			if (colorCode.length() > 0) {
				if (!hexColorPattern.matcher(colorCode).matches()) {
					throw new InvalidDataException(
							GtfsFile.FILENAME_ROUTES, 
							FIELD_NAME_ROUTE_TEXT_COLOR, 
							record, 
							colorCode);
				}
			}
		}
	}
	
	/**
	 * Gets the route id for this route
	 * @return
	 */
	public String getRouteId() {
		return this.dataFromTable.get(FIELD_NAME_ROUTE_ID);
	}
	
	/**
	 * Gets the agency id that operates this route, if it has been provided
	 * @return the agency id if provided; otherwise {@code null}
	 */
	public String getAgencyId() {
		return this.dataFromTable.get(FIELD_NAME_AGENCY_ID);
	}
	
	/**
	 * Gets the short name for this route. Might be empty.
	 * @return
	 */
	public String getShortName() {
		return this.dataFromTable.get(FIELD_NAME_ROUTE_SHORT_NAME);
	}
	
	/**
	 * Gets the long name for this route. Might be empty.
	 * @return
	 */
	public String getLongName() {
		return this.dataFromTable.get(FIELD_NAME_ROUTE_LONG_NAME);
	}
	
	/**
	 * Gets the description, if it has been provided.
	 * @return The description, if provided; otherwise, {@code null}.
	 */
	public String getDescription() {
		return this.dataFromTable.get(FIELD_NAME_ROUTE_DESC);
	}

	/**
	 * @return the routeType
	 */
	public RouteTypeEnum getRouteType() {
		return this.routeType;
	}
	
	/**
	 * Gets a url with information about this route, if it has been provided
	 * @return route information url if provided; otherwise, {@code null}
	 */
	public String getRouteUrl() {
		return this.dataFromTable.get(FIELD_NAME_ROUTE_URL);
	}
	
	/**
	 * Gets this route's color, if it has been provided. If not provided, the
	 * default color is white ("ffffff").
	 * @return
	 */
	public String getRouteColor() {
		String color = this.dataFromTable.get(FIELD_NAME_ROUTE_COLOR);
		if (color == null) {
			return "ffffff";
		}
		else {
			return color;
		}
	}
	
	/**
	 * Gets this route's text color, if it has been provided. If not provided,
	 * the default color is black ("000000").
	 * @return
	 */
	public String getRouteTextColor() {
		String color = this.dataFromTable.get(FIELD_NAME_ROUTE_TEXT_COLOR);
		if (color == null) {
			return "000000";
		}
		else {
			return color;
		}
	}
}
