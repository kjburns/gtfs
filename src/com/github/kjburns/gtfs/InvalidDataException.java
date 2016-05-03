/*
 * InvalidDataException.java
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

public class InvalidDataException extends Exception {
	private static final long serialVersionUID = -5001446423363512177L;

	public String filename;
	public String fieldName;
	public int record;
	public String badData;
	
	InvalidDataException(String filename, String fieldName, 
			int record, String badData) {
		this.fieldName = fieldName;
		this.filename = filename;
		this.record = record;
		this.badData = badData;
	}
}
