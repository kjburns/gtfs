/*
 * TransitShape.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A shape as described in shapes.txt. So named to avoid ambiguity with
 * java.awt.shape.
 * <p>
 * Because shape points are required to have sequencing markers, this class
 * puts points in their proper sequence as they are added.
 * </p>
 * 
 * @author Kevin J. Burns
 *
 */
public class TransitShape {
	private List<TransitShapePoint> points = new ArrayList<>();
	private String shapeId;
	
	/**
	 * Constructor.
	 * @param id shape_id for this shape
	 */
	TransitShape(String id) {
		this.shapeId = id;
	}
	
	/**
	 * Adds a single point to this shape.
	 * @param pt point to add
	 */
	void add(TransitShapePoint pt) {
		this.points.add(pt);
		
		Collections.sort(this.points, TransitShapePoint.defaultSorter);
	}
	
	/**
	 * Adds a collection of points to this shape.
	 * <p>
	 * Calling this method (rather than calling {@link #add(TransitShapePoint)}
	 * repetitively) is preferable so that the list of points is only sorted
	 * once.
	 * </p>
	 * @param pts
	 */
	void addAll(Collection<TransitShapePoint> pts) {
		this.points.addAll(pts);
		
		Collections.sort(this.points, TransitShapePoint.defaultSorter);
	}
	
	/**
	 * Gets the number of points in this shape.
	 * @return
	 */
	public int getPointCount() {
		return this.points.size();
	}
	
	/**
	 * Gets an iterator of points in this shape.
	 * @return
	 */
	public Iterator<TransitShapePoint> getIterator() {
		return this.points.iterator();
	}

	/**
	 * @return the shapeId
	 */
	public String getShapeId() {
		return this.shapeId;
	}
}
