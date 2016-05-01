package com.github.kjburns.gtfs.misc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

public class CsvFile {
	public static class FilenameRequiredException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	}

	public static class ModifyPastEndOfTableException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	}

	public static class ReadPastEndOfTableException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	}

	public static class DeleteNotExistentColumnException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	}

	public static class FieldNotFoundException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	}

	private String filename = null;
	private boolean valid = false;
	private boolean empty = true;
	private Vector<Vector<String>> rows = new Vector<Vector<String>>();
	private boolean dataHeadersCaseSensitive = true;
	
	public CsvFile() {
		/*
		 * Initializes a new table with no rows or columns
		 */
	}
	public CsvFile(int width, int height) {
		for (int i = 0; i < width; i++) this.addColumn("");
		for (int j = 1; j < height; j++) this.addRow("");
	}
	
	public CsvFile(String filename) throws IOException {
		this(1, 1);
		
		File f = new File(filename);
		String text = Filesystem.readAllText(f);
		this.csvDecode(text);
		
		this.filename = filename;
		this.empty = false;
		this.valid = true;
	}
	
	public CsvFile(InputStream is) throws IOException {
		this(1, 1);
		
		String text = Filesystem.readAllText(is);
		this.csvDecode(text);
		
		this.empty = false;
		this.valid = true;
	}
	
	protected void csvDecode(String source) {
		int currRow = 0;
		int currCol = 0;
		int maxWidth = 0;
		String currData = "";
		boolean insideQuotes = false;
		boolean ignoreFurther = false;
		boolean finished = false;
		boolean rowFinished = false;
		
		for (int i = 0; i < source.length(); i++) {
			char ch = source.charAt(i);
			String ch2 = source.substring(i, Math.min(i + 2, source.length()));
			
			switch(ch) {
			case '\"':
				if (currData.length() == 0) insideQuotes = true;
				else {
					if (insideQuotes) {
						if (ch2.equals("\"\"")) {
							currData += "\"";
							i++;
							continue;
						}
						else {
							insideQuotes = false;
							ignoreFurther = true;
						}
					}
				}
				break;
			case ',':
				if (insideQuotes) currData += ch;
				else finished = true;
				break;
			case '\n':
				if (insideQuotes) currData += ch;
				else {
					finished = true;
					rowFinished = true;
				}
				break;
			case '\r':
				if (insideQuotes) currData += ch;
				break;
			default:
				if (!ignoreFurther) {
					currData += ch;
				}
			}
			
			if (i == source.length() - 1) finished = true;
			if (finished) {
				while (currRow >= this.rows.size()) this.rows.add(new Vector<String>());
				if (currRow == 0) {
					if (currCol == 0) this.rows.get(0).set(0, currData);
					else this.rows.get(currRow).add(currData);
				}
				else {
					this.rows.get(currRow).add(currData);
				}
				currCol++;
				finished = false;
				ignoreFurther = false;
				insideQuotes = false;
				currData = "";
			}
			if (rowFinished) {
				if (currCol > maxWidth) maxWidth = currCol;
				currCol = 0;
				currRow++;
				this.rows.add(new Vector<String>());
				rowFinished = false;
			}
		}
		
		for (int i = 0; i < this.rows.size(); i++) {
			while (this.rows.get(i).size() < maxWidth) this.rows.get(i).add("");
		}
	}
	
	private String csvEncode(String source) {
		String ret = "\"";
		for (int i = 0; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (ch == '\"') ret += "\"\"";
			else ret += ch;
		}
		ret += "\"";
		
		return ret;
	}
	
	public void addColumn(String defaultValue) {
		this.addField("", "");
		int lastColumn = this.getWidth() - 1;
		
		for (int i = 0; i < this.rows.size(); i++) {
			this.setCell(i, lastColumn, defaultValue);
		}
	}
	
	public void addField(String name, String defaultValue) {
		if (this.empty) {
			this.rows.add(new Vector<String>());
			this.empty = false;
		}
		this.rows.get(0).add(name);
		
		for (int i = 1; i < this.rows.size(); i++) {
			this.rows.get(i).add(defaultValue);
		}
	}
	public int addRecord() throws FieldNotFoundException {
		// returns the record number
		if (this.empty) throw new FieldNotFoundException();
		int recordNumber = this.rows.size();
		
		this.rows.add(new Vector<String>());
		for (int i = 0; i < this.rows.get(0).size(); i++) {
			this.rows.get(recordNumber).add("");
		}
		
		return recordNumber;
	}
	public int addRecord(String[] data) throws FieldNotFoundException {
		/*
		 * Creates a new record, inserts the data, and returns the record number.
		 * How data are handled:
		 * o Data are inserted left-to-right.
		 * o Excess data (more than will fit) are ignored.
		 * o Table is padded if not enough data are given.
		 */
		int recordNumber = this.addRecord();
		for (int i = 0; i < this.rows.get(0).size(); i++) {
			if (i >= data.length) this.setCell(recordNumber, i, "");
			else this.setCell(recordNumber, i, data[i]);
		}
		
		return recordNumber;
	}
	
	public void addRow(String defaultValue) {
		Vector<String> row = new Vector<String>();
		this.rows.add(row);
		for (int i = 0; i < this.getWidth(); i++) {
			row.add(defaultValue);
		}
	}
	
	public void deleteColumn(int col) throws DeleteNotExistentColumnException {
		if (col >= this.rows.get(0).size()) throw new DeleteNotExistentColumnException();
		
		for (int i = 0; i < this.rows.size(); i++) {
			this.rows.get(i).remove(col);
		}
	}
	
	public void deleteField(String name) throws FieldNotFoundException, DeleteNotExistentColumnException {
		int columnNumber = this.getFieldColumnNumber(name);
		if (columnNumber == -1) throw new FieldNotFoundException();
		
		this.deleteColumn(columnNumber);
	}
	
	public void deleteRecord(int num) throws ReadPastEndOfTableException {
		this.deleteRow(num);
	}
	
	public void deleteRow(int num) throws ReadPastEndOfTableException {
		if (num >= this.rows.size()) throw new ReadPastEndOfTableException();
		
		this.rows.remove(num);
	}
	
	public boolean fieldExists(String st) {
		return (this.getFieldColumnNumber(st) != -1);
	}
	
	public void insertColumn(int before, String defaultValue) throws ModifyPastEndOfTableException {
		if (before >= this.rows.size()) throw new ModifyPastEndOfTableException();
		
		for (int i = 0; i < this.rows.size(); i++) {
			this.rows.get(i).insertElementAt(defaultValue, i);
		}
	}
	
	public void insertField(String name, String before, String defaultValue) throws FieldNotFoundException {
		int insertBeforeColumn = this.getFieldColumnNumber(before);
		if (insertBeforeColumn == -1) throw new FieldNotFoundException();
		
		this.rows.get(0).insertElementAt(name, insertBeforeColumn);
		for (int i = 1; i < this.rows.size(); i++) {
			this.rows.get(i).insertElementAt(defaultValue, insertBeforeColumn);
		}
	}
	
	public void insertRecord(int before) throws ModifyPastEndOfTableException {
		if (before >= this.rows.size()) throw new ModifyPastEndOfTableException();
		
		int width = this.rows.get(0).size();
		this.rows.insertElementAt(new Vector<String>(), before);
		for (int i = 0; i < width; i++) {
			this.rows.get(before).set(i, "");
		}
	}
	
	public void insertRecord(int before, String[] data) throws ModifyPastEndOfTableException {
		this.insertRecord(before);
		
		for (int i = 0; i < this.rows.get(0).size(); i++) {
			if (i >= data.length) this.rows.get(before).set(i, "");
			else this.rows.get(before).set(i, data[i]);
		}
	}
	
	public void insertRow(int before, String defaultValue) throws ModifyPastEndOfTableException {
		this.insertRecord(before);
		
		for (int i = 0; i < this.rows.get(0).size(); i++) {
			this.rows.get(before).set(i, defaultValue);
		}
	}
	
	public void save() throws FilenameRequiredException {
		if (this.filename == null) throw new FilenameRequiredException();
		
		this.save(this.filename);
	}
	
	public void save(@SuppressWarnings("hiding") String filename) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < this.getHeight(); i++) {
			for (int j = 0; j < this.getWidth(); j++) {
				sb.append(this.csvEncode(this.rows.get(i).get(j)));
				if (j != this.getWidth() - 1) sb.append(",");
			}
			if (i != this.getHeight() - 1) sb.append("\r\n");
		}
		
		File f = new File(filename);
		Filesystem.writeAllText(f, sb.toString());
		
		this.filename = filename;
	}
	
	public String getCell(int row, int col) {
		if (row >= this.rows.size()) return null;
		if (col >= this.rows.get(0).size()) return null;
		return this.rows.get(row).get(col);
	}
	
	public void setCell(int row, int col, String value) {
		if (row >= this.rows.size()) return;
		if (col >= this.rows.get(0).size()) return;
		this.rows.get(row).set(col, value);
	}
	
	public String getData(String field, int record) throws ReadPastEndOfTableException, FieldNotFoundException {
		if (record >= this.rows.size()) throw new ReadPastEndOfTableException();
		int col = this.getFieldColumnNumber(field);
		if (col == -1) throw new FieldNotFoundException();
		return this.rows.get(record).get(col);
	}
	
	public void setData(String field, int record, String value) throws FieldNotFoundException {
		if (this.empty) this.addField(field, "");
		if (record >= this.rows.size()) {
			int origHeight = this.rows.size();
			for (int i = origHeight; i <= record; i++) {
				this.addRecord();
			}
		}
		
		if (!this.fieldExists(field)) this.addField(field, "");
		this.rows.get(record).set(this.getFieldColumnNumber(field), value);
	}
	
	public boolean isDbTableCaseSensitive() {
		return this.dataHeadersCaseSensitive;
	}
	
	public void setDbTableCaseSensitive(boolean value) {
		this.dataHeadersCaseSensitive = value;
	}
	
	private int getFieldColumnNumber(String fieldName) {
		for (int i = 0; i < this.rows.get(0).size(); i++) {
			if (this.dataHeadersCaseSensitive) {
				if (fieldName.trim().equals(this.rows.get(0).get(i).trim())) return i;
			}
			else {
				if (fieldName.trim().compareToIgnoreCase(this.rows.get(0).get(i).trim()) == 0) return i;
			}
		}
		
		return -1;
	}
	
	public int getHeight() {
		return this.rows.size();
	}
	
	public int getWidth() {
		return this.rows.get(0).size();
	}

	public boolean isValid() {
		return valid;
	}
	
	public int getRecordCount() {
		return this.getHeight() - 1;
	}
}
