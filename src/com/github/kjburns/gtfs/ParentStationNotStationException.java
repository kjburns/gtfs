package com.github.kjburns.gtfs;

public class ParentStationNotStationException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1542088447647673092L;
	private String stopId;
	private String allegedParentStationId;
	
	ParentStationNotStationException(String stop, String parent) {
		this.stopId = stop;
		this.allegedParentStationId = parent;
	}
	
	/**
	 * @return the stopId
	 */
	public String getStopId() {
		return this.stopId;
	}
	/**
	 * @return the allegedParentStationId
	 */
	public String getAllegedParentStationId() {
		return this.allegedParentStationId;
	}
}
