/*
 * TransitShapePoint.java
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
 *   2016-05-11  Basic functionality
 */
package com.github.kjburns.gtfs;

import java.util.Comparator;

import com.github.kjburns.gtfs.misc.CsvFile;
import com.github.kjburns.gtfs.misc.CsvFile.FieldNotFoundException;
import com.github.kjburns.gtfs.misc.CsvFile.ReadPastEndOfTableException;

/**
 * A point along a shape, as defined by shapes.txt
 * @author Kevin J. Burns
 *
 */
public class TransitShapePoint {
	static final Comparator<TransitShapePoint> defaultSorter = (x, y) -> {
		return Integer.compare(x.sequence, y.sequence);
	};
	
	private static final String FIELD_NAME_SHAPE_ID = "shape_id";
	private static final String FIELD_NAME_LAT = "shape_pt_lat";
	private static final String FIELD_NAME_LON = "shape_pt_lon";
	private static final String FIELD_NAME_SHAPE_SEQ = "shape_pt_sequence";
	private static final String FIELD_NAME_DIST_TRAVELED = 
			"shape_dist_traveled";
	
	private double distanceTraveled = Double.NaN;
	private double lat;
	private double lon;
	private int sequence;
	private String shapeId;

	/**
	 * Constructor. Reads a record from shapes.txt
	 * @param table table read from shapes.txt
	 * @param record Record number to read from
	 * @throws MissingRequiredFieldException If any required fields are missing
	 * @throws InvalidDataException If any data are not compliant with the spec
	 */
	TransitShapePoint(CsvFile table, int record) 
			throws MissingRequiredFieldException, InvalidDataException {
		String key = null;
		String strValue = null;
		
		try {
			/*
			 * read latitude
			 */
			key = FIELD_NAME_LAT;
			strValue = table.getData(key, record);
			InvalidDataException badLatExc = new InvalidDataException(
					GtfsFile.FILENAME_SHAPES, key, record, strValue);
			try {
				this.lat = Double.valueOf(strValue);
			} catch (NumberFormatException ex) {
				throw badLatExc;
			}
			if ((lat > 90.) || (lat < -90.)) {
				throw badLatExc;
			}
			
			/*
			 * read longitude
			 */
			key = FIELD_NAME_LON;
			strValue = table.getData(key, record);
			InvalidDataException badLonExc = new InvalidDataException(
					GtfsFile.FILENAME_SHAPES, key, record, strValue);
			try {
				this.lon = Double.valueOf(strValue);
			} catch (NumberFormatException ex) {
				throw badLonExc;
			}
			if ((lon > 180.) || (lon < -180.)) {
				throw badLonExc;
			}
			
			/*
			 * read sequence
			 */
			key = FIELD_NAME_SHAPE_SEQ;
			strValue = table.getData(key, record);
			InvalidDataException badSeqExc = new InvalidDataException(
					GtfsFile.FILENAME_SHAPES, key, record, strValue);
			try {
				this.sequence = Integer.valueOf(strValue);
			} catch (NumberFormatException ex) {
				throw badSeqExc;
			}
			if (this.sequence < 0) {
				throw badSeqExc;
			}
			
			/*
			 * read shape id
			 */
			key = FIELD_NAME_SHAPE_ID;
			this.shapeId = table.getData(key, record);
			
			/*
			 * read distance traveled
			 */
			key = FIELD_NAME_DIST_TRAVELED;
			if (table.fieldExists(key)) {
				strValue = table.getData(key, record);
				InvalidDataException badDistExc = new InvalidDataException(
						GtfsFile.FILENAME_SHAPES, key, record, strValue);
				try {
					this.distanceTraveled = Double.valueOf(strValue);
				} catch (NumberFormatException ex) {
					throw badDistExc;
				}
				if (this.distanceTraveled < 0) {
					throw badDistExc;
				} 
			}
		} catch (ReadPastEndOfTableException ex) {
			throw new IndexOutOfBoundsException();
		} catch (FieldNotFoundException ex) {
			throw new MissingRequiredFieldException(
					GtfsFile.FILENAME_SHAPES, key);
		}
	}

	/**
	 * Gets the shape ID of the shape that this point belongs to
	 * @return
	 */
	public String getShapeId() {
		return this.shapeId;
	}

	/**
	 * Gets the distance traveled along the shape to this point
	 * @return the distanceTraveled
	 */
	public double getDistanceTraveled() {
		return this.distanceTraveled;
	}

	/**
	 * @return the latitude of this point
	 */
	public double getLat() {
		return this.lat;
	}

	/**
	 * @return the longitude of this point
	 */
	public double getLon() {
		return this.lon;
	}

	/**
	 * @return the sequence number of this point, where sequence numbers 
	 * increase along the shape
	 */
	public int getSequence() {
		return this.sequence;
	}
}
