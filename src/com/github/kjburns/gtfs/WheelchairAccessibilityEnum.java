/*
 * WheelchairAccessibilityEnum.java
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
 *   2016-05-02  Basic functionality
 */
package com.github.kjburns.gtfs;

/**
 * An enumeration for the wheelchair_boarding field in stops.txt
 * @author Kevin J. Burns
 *
 */
public enum WheelchairAccessibilityEnum {
	/**
	 * From specification:
	 * <blockquote>
	 * indicates that there is no accessibility information for the stop.
	 * </blockquote>
	 */
	UNKNOWN,
	/**
	 * From specification:
	 * <blockquote>
	 * indicates that at least some vehicles at this stop can be boarded 
	 * by a rider in a wheelchair.
	 * </blockquote>
	 * Additionally, in the context of a station:
	 * <blockquote>
	 * there exists some accessible path from outside the station to the 
	 * specific stop/platform
	 * </blockquote>
	 */
	NOT_NONE,
	/**
	 * From specification:
	 * <blockquote>
	 * wheelchair boarding is not possible at this stop
	 * </blockquote>
	 * Additionally, in the context of a station:
	 * <blockquote>
	 * there exists no accessible path from outside the station to the 
	 * specific stop/platform
	 * </blockquote>
	 */
	NONE;
}