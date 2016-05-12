/*
 * TransitShapeCollection.java
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.github.kjburns.gtfs.misc.CsvFile;

/**
 * A collection of {@link TransitShape} objects.
 * @author Kevin J. Burns
 *
 */
public class TransitShapeCollection {
	private HashMap<String, TransitShape> shapes = new HashMap<>();
	
	/**
	 * Constructor. Creates a shape collection.
	 * @param f File object which describes the shapes.txt object in the
	 * gtfs zip file.
	 * @throws IOException If the file cannot be opened or read for any reason
	 * @throws InvalidDataException if any data in the table is invalid
	 * according to the spec 
	 * @throws MissingRequiredFieldException if any required fields are missing 
	 */
	TransitShapeCollection(File f) 
			throws IOException, MissingRequiredFieldException, 
					InvalidDataException {
		HashMap<String, ArrayList<TransitShapePoint>> tempStorage = 
				new HashMap<>();
		
		try(FileInputStream fis = new FileInputStream(f)) {
			CsvFile table = new CsvFile(fis);
			
			int recordCount = table.getRecordCount();
			for (int record = 1; record <= recordCount; record++) {
				TransitShapePoint pt = new TransitShapePoint(table, record);
				
				ArrayList<TransitShapePoint> ptList;
				String shapeId = pt.getShapeId();
				if (!tempStorage.containsKey(shapeId)) {
					ptList = new ArrayList<TransitShapePoint>();
					tempStorage.put(shapeId, ptList);
				}
				else {
					ptList = tempStorage.get(shapeId);
				}
				
				ptList.add(pt);
			}
		}
		
		for (String id : tempStorage.keySet()) {
			TransitShape shape = new TransitShape(id);
			
			shape.addAll(tempStorage.get(id));
			this.shapes.put(id, shape);
		}
	}
	
	/**
	 * Gets a shape based on the supplied id.
	 * @param id identifier of shape to fetch
	 * @return the shape with the supplied id, if it exists; otherwise,
	 * {@code null}.
	 */
	public TransitShape getShapeById(String id) {
		return this.shapes.get(id);
	}
	
	/**
	 * Gets the number of shapes stored in this collection.
	 * @return
	 */
	public int getShapeCount() {
		return this.shapes.size();
	}
	
	/**
	 * Gets an iterator for iterating over the stored shapes.
	 * <p>
	 * Internally, shapes are stored in a Map, so order of the iterator is 
	 * never guaranteed.
	 * </p>
	 * @return
	 */
	public Iterator<TransitShape> getShapeIterator() {
		return this.shapes.values().iterator();
	}
}
