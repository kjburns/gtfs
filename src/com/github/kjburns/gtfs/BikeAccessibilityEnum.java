package com.github.kjburns.gtfs;

/**
 * An enumeration that describes whether bikes can be used on a particular trip
 * @author Kevin J. Burns
 *
 */
public enum BikeAccessibilityEnum {
	/**
	 * From the spec:
	 * <blockquote>
	 * indicates that there is no bike information for the trip
	 * </blockquote>
	 */
	UNKNOWN,
	/**
	 * From the spec:
	 * <blockquote>
	 * indicates that the vehicle being used on this particular trip can 
	 * accommodate at least one bicycle
	 * </blockquote>
	 */
	NOT_NONE,
	/**
	 * From the spec:
	 * <blockquote>
	 * indicates that no bicycles are allowed on this trip
	 * </blockquote>
	 */
	NONE;
}
