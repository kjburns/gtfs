/*
 * PickupDropoffTimeEnum.java
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
 *   2016-05-30  Basic functionality
 */
package com.github.kjburns.gtfs;

public enum PickupDropoffTypeEnum {
	/**
	 * From the spec:
	 * <blockquote>
	 * Regularly scheduled pickup or dropoff 
	 * </blockquote>
	 */
	REGULARLY_SCHEDULED,
	/**
	 * From the spec:
	 * <blockquote>
	 * No pickup or dropoff available
	 * </blockquote>
	 */
	NONE_AVAILABLE,
	/**
	 * From the spec:
	 * <blockquote>
	 * Must phone agency to arrange pickup or dropoff
	 * </blockquote>
	 */
	CALL_AGENCY_TO_ARRANGE,
	/**
	 * From the spec:
	 * <blockquote>
	 * Must coordinate with driver to arrange pickup or dropoff
	 * </blockquote>
	 */
	CONTACT_DRIVER_TO_ARRANGE;
}
