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
