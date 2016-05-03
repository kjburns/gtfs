/*
 * DatasetUniquenessException.java
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
 *   2016-05-02  Add this comment block
 */
package com.github.kjburns.gtfs;

/**
 * An exception raised when a field or combination of fields is expected to be
 * dataset-unique but multiple identical instances of said field or 
 * combination of fields is encountered in the file.
 * @author Kevin J. Burns
 *
 */
public class DatasetUniquenessException extends Exception {
	private static final long serialVersionUID = 4182906246891958121L;

	/**
	 * The filename where the transgression occurred.
	 */
	public String filename;
	/**
	 * The name of the field, or the names of fields separated by plus
	 * signs, that is expected to be dataset-unique.
	 */
	public String fieldName;
	/**
	 * The value, or combination of values separated by plus signs, that
	 * was duplicated in the table.
	 */
	public String duplicatedValue;
	
	DatasetUniquenessException(String file, String field, String value) {
		this.fieldName = field;
		this.filename = file;
		this.duplicatedValue = value;
	}
}
