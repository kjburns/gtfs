package com.github.kjburns.gtfs.misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A utility class which exposes the entries in a zip file, but does not 
 * provide any access to the data itself.
 * @author Kevin J. Burns
 *
 */
public class ZipInspector {
	private ArrayList<String> entries = new ArrayList<String>();
	private boolean error = false;
	
	public ZipInspector(String zipPath) {
		try (ZipFile zf = new ZipFile(zipPath)) {
			Enumeration<? extends ZipEntry> zipEntries = zf.entries();
			while (zipEntries.hasMoreElements()) {
				ZipEntry ze = zipEntries.nextElement();
				this.entries.add(ze.getName());
			}
		} catch (IOException ex) {
			this.error = true;
		}
	}

	/**
	 * Gets a list of entries in the zip file.
	 * @return the entries
	 */
	public List<String> getEntries() {
		return this.entries;
	}

	/**
	 * Reports whether an error occurred when accessing the zip file.
	 * @return <code>true</code> if an error occurred; <code>false</code> 
	 * otherwise.
	 */
	public boolean isError() {
		return this.error;
	}

	/**
	 * Gets a list of entries in the zip file that match the supplied regex 
	 * filter.
	 * @param regexFilter The filter to use to decide on inclusion in the list.
	 * @return A list of entries that matched the regex. This can be an empty 
	 * list.
	 */
	public List<String> getEntries(String regexFilter) {
		ArrayList<String> ret = new ArrayList<>();
		Pattern p = Pattern.compile(regexFilter);
		
		for (String s : this.entries) {
			if (p.matcher(s).matches()) {
				ret.add(s);
			}
		}
		
		return ret;
	}
}
