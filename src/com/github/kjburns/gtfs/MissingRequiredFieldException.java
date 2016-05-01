package com.github.kjburns.gtfs;

public class MissingRequiredFieldException extends Exception {
	private static final long serialVersionUID = 1348355953927095849L;

	public String file;
	public String missingFieldName;
	
	public MissingRequiredFieldException(String file, String fieldName) {
		this.file = file;
		this.missingFieldName = fieldName;
	}
}
