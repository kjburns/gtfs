package com.github.kjburns.gtfs;

public class TerminalTimepointException extends Exception {
	private static final long serialVersionUID = 8871634234236364326L;
	
	/**
	 * the trip id which does not have a timepoint at both the beginning and
	 * end of the trip.
	 */
	public String tripId;
	
	TerminalTimepointException(String tid) {
		this.tripId = tid;
	}
}
