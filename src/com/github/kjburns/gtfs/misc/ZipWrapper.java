package com.github.kjburns.gtfs.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.SwingWorker;

/**
 * <p>Provides a facility for reading and writing zip files.</p>
 * <p>
 * The general workflow for using this class to read an existing zip file is 
 * as follows:
 * <ol>
 * <li>Load the file using either of the following constructors:
 * {@link #ZipWrapper(String)}, {@link #ZipWrapper(String, SwingWorker)}, or
 * {@link #ZipWrapper(String, SwingWorker, int, int)}.</li>
 * <li>If you don't know what you are looking for, get a list of all entries 
 * using {@link #getEntries()}. If you do know what you are looking for, get a 
 * list of certain entries using {@link #getEntries(String)}.</li>
 * <li>For the entries you are interested in, call {@link #getEntry(String)} 
 * to get a {@link File} object so you can read the contents.</li>
 * <li>Close the zip file using {@link #close()}. All File objects retrieved 
 * in step 4 will then be invalid.</li>
 * </ol>
 * </p>
 * <p>
 * The general workflow for using this class to write a zip file is as follows:
 * <ol>
 * <li>Create a new zip wrapper using {@link #ZipWrapper()}.</li>
 * <li>Add entries from existing files using {@link #addEntry(String, String)}. 
 * Make sure those existing files stay on disk for now.</li>
 * <li>If using a SwingWorker to provide updates to the user, if desired, call 
 * the {@link #setMinProgress(int)} and {@link #setMaxProgress(int)} functions 
 * to change the progress range reported to the SwingWorker. The default range 
 * is [0, 100].</li>
 * <li>Write the zip file to disk using either {@link #write(String)} or
 * {@link #write(String, SwingWorker)}. After calling one of those functions, 
 * you may delete the original files on disk if they are temporary in nature. 
 * However, make sure not to use the zip wrapper after deleting the original 
 * files, because the original locations are stored internally in the zip 
 * wrapper and exceptions will be thrown if another write attempt is made.
 * </li>
 * </ol>
 * </p>
 * @author Kevin J. Burns
 *
 */
public class ZipWrapper implements AutoCloseable {
	private static class Entry {
		public String locationInFile;
		public String locationOnDisk;
		
		public Entry(String file, String disk) {
			this.locationInFile = file;
			this.locationOnDisk = disk;
		}
	}
	
	private ArrayList<Entry> entries = new ArrayList<Entry>();
	private HashMap<String, Entry> entriesByZipLocation = 
			new HashMap<String, Entry>();
	private String tempFolder = null;
	private int minProgress = 0;
	private int maxProgress = 100;
	
	/**
	 * Creates an empty zip file.
	 */
	public ZipWrapper() {
	}
	
	/**
	 * Loads a zip file. If invoked in this manner, you must call the 
	 * {@link #close()} function.
	 * @param filename
	 * @throws IOException 
	 */
	public ZipWrapper(String filename) throws IOException {
		this(filename, null);
	}
	
	/**
	 * Loads a zip file. If invoked in this manner, you must call the 
	 * {@link #close()} function.
	 * Progress on the worker thread ranges from 0 to 100.
	 * @param filename Source file to read
	 * @param workerThread Worker thread to report to
	 * @throws IOException 
	 */
	public ZipWrapper(String filename, SwingWorker<?, ?> workerThread) 
			throws IOException {
		this.doLoading(filename, workerThread);
	}
	
	/**
	 * Loads a zip file. If invoked in this manner, you must call the 
	 * {@link #close()} function.
	 * Progress on the worker thread is within the range you specify.
	 * @param filename Source file to read
	 * @param workerThread Worker thread to report to
	 * @param minProgress Minimum progress of worker thread
	 * @param maxProgress Maximum progress of worker thread
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public ZipWrapper(String filename, SwingWorker<?, ?> workerThread, 
			int minProgress, int maxProgress) 
					throws FileNotFoundException, IOException {
		this.minProgress = minProgress;
		this.maxProgress = maxProgress;
		
		this.doLoading(filename, workerThread);
	}

	private void doLoading(String filename, SwingWorker<?, ?> workerThread)
			throws IOException, FileNotFoundException {
		this.tempFolder = System.getProperty("java.io.tmpdir");
		this.tempFolder += System.getProperty("file.separator");
		this.tempFolder += "zip";
		this.tempFolder += String.valueOf(System.currentTimeMillis());
		
		File folder = new File(this.tempFolder);
		folder.mkdir();

		try (ZipFile zf = new ZipFile(filename)) {
			Enumeration<? extends ZipEntry> zipEntries = zf.entries();
			int count = 0;
			int oldValue = this.minProgress;
			int newValue;
			
			while (zipEntries.hasMoreElements()) {
				if (workerThread != null) {
					if (workerThread.isCancelled()) break;
				}
				
				ZipEntry ze = zipEntries.nextElement();
				String path = this.tempFolder + 
						System.getProperty("file.separator") + 
						String.valueOf(count);
				try(
						InputStream is = zf.getInputStream(ze); 
						FileOutputStream fos = new FileOutputStream(path)) {
					byte[] bytes = new byte[1024];
					int length;
					while ((length = is.read(bytes)) >= 0) {
						fos.write(bytes, 0, length);
					}
					
					this.addEntryToLists(new Entry(ze.getName(), path));
				}
				
				count++;
				if (workerThread != null) {
					newValue = this.minProgress + 
							count * (this.maxProgress - this.minProgress) / 
							zf.size();
					workerThread.firePropertyChange(
							"progress", oldValue, newValue);
					oldValue = newValue;
				}
			}
		}
	}
	
	/**
	 * Adds an entry to the zip file's list. Does not actually write the zip 
	 * file to disk. To write the file to disk, call {@link #write(String)} or 
	 * {@link #write(String, SwingWorker)}. The file must be maintained on 
	 * disk until after <code>write()</code> is called.  It is up to the 
	 * calling function to delete the original file after writing the zip, if 
	 * necessary.
	 * @param locationOnDisk Where to find the file to be written
	 * @param locationInFile Virtual path in zip file where the file should be 
	 * located
	 */
	public void addEntry(String locationOnDisk, String locationInFile) {
		Entry e = new Entry(locationInFile, locationOnDisk);
		this.addEntryToLists(e);
	}
	
	private void addEntryToLists(Entry e) {
		this.entries.add(e);
		this.entriesByZipLocation.put(e.locationInFile, e);
	}
	
	/**
	 * Gets a {@link File} object necessary for accessing the zip file entry. 
	 * @param pathInFile Virtual path in zip file where the file can be found.
	 * @return A File object if the entry is found; otherwise <code>null</code>.
	 */
	public File getEntry(String pathInFile) {
		Entry entry = this.entriesByZipLocation.get(pathInFile);
		if (entry == null) return null;
		String path = entry.locationOnDisk;
		if (path == null) return null;
		
		return new File(path);
	}

	/**
	 * Gets a list of all entries in the file. The virtual paths are returned.
	 * @return
	 */
	public List<String> getEntries() {
		List<String> ret = new ArrayList<String>();
		
		for (Entry e : this.entries) {
			ret.add(e.locationInFile);
		}
		
		return ret;
	}
	
	/**
	 * Gets a list of entries in the file whose virtual paths match the 
	 * supplied pattern.
	 * @param regex Pattern to match against
	 * @return List of entries which match. If no entries are found, returns 
	 * an empty list.
	 */
	public List<String> getEntries(String regex) {
		List<String> ret = new ArrayList<String>();
		
		for (Entry e : this.entries) {
			if (Pattern.matches(regex, e.locationOnDisk)) {
				ret.add(e.locationInFile);
			}
		}
		
		return ret;
	}
	
	/**
	 * Writes the zip file to disk. This is a convenience method for 
	 * {@link #write(String, SwingWorker)}.
	 * @param outputFilename Path where the zip file is to be written
	 * @throws IOException for any of the following conditions:
	 * <ul>
	 * <li>if a file exists but is a directory rather than a regular file</li>
	 * <li>if a file does not exist but cannot be created</li> 
	 * <li>if a file cannot be opened for any other reason</li></ul>
	 * @throws NullPointerException if <code>outputFilename</code> is null
	 * @throws SecurityException if a security manager exists and its 
	 * <code>checkWrite</code>  or <code>checkRead</code> method denies write 
	 * access to any involved file.
	 */
	public void write(String outputFilename) 
			throws IOException, NullPointerException, SecurityException {
		this.write(outputFilename, null);
	}
	
	/**
	 * Writes the zip file to disk and reports back to the supplied worker 
	 * thread about its progress.
	 * @param outputFilename Path where the zip file is to be written
	 * @param workerThread Worker thread to report to. This can be 
	 * <code>null</code>
	 * @throws IOException for any of the following conditions:
	 * <ul>
	 * <li>if a file exists but is a directory rather than a regular file</li>
	 * <li>if a file does not exist but cannot be created</li> 
	 * <li>if a file cannot be opened for any other reason</li></ul>
	 * @throws NullPointerException if <code>outputFilename</code> is null
	 * @throws SecurityException if a security manager exists and its 
	 * <code>checkWrite</code> or <code>checkRead</code> method denies write 
	 * access to any involved file.
	 */
	public void write(String outputFilename, SwingWorker<?, ?> workerThread) 
			throws IOException, NullPointerException, SecurityException {
		File outFile = new File(outputFilename);
		try (ZipOutputStream zos = 
				new ZipOutputStream(new FileOutputStream(outFile))) {
			int count = 0;
			int oldProgress = this.minProgress;
			int newProgress;
			for (Entry e : this.entries) {
				if (workerThread != null) {
					if (workerThread.isCancelled()) break;
				}
				
				File inFile = new File(e.locationOnDisk);
				try (FileInputStream fis = new FileInputStream(inFile)) {
					ZipEntry ze = new ZipEntry(e.locationInFile);
					zos.putNextEntry(ze);
					
					byte[] bytes = new byte[1024];
					int length;
					while ((length = fis.read(bytes)) >= 0) {
						zos.write(bytes, 0, length);
					}
					
					zos.closeEntry();
					fis.close();
					
					count++;
					if (workerThread != null) {
						newProgress = this.minProgress + 
								count * (this.maxProgress - this.minProgress) / 
								this.entries.size();
						workerThread.firePropertyChange(
								"progress", oldProgress, newProgress);
						oldProgress = newProgress;
					}
				}
			}
		}
	}

	/**
	 * Closes this zip wrapper and deletes any associated temporary files.
	 */
	@Override
	public void close() throws IOException {
		if (this.tempFolder == null) return;
		
		File folder = new File(tempFolder);
		File[] files = folder.listFiles();
		for (File f : files) {
			f.delete();
		}
		folder.delete();
	}

	/**
	 * @param minProgress the minProgress to set
	 */
	public void setMinProgress(int minProgress) {
		this.minProgress = minProgress;
	}

	/**
	 * @param maxProgress the maxProgress to set
	 */
	public void setMaxProgress(int maxProgress) {
		this.maxProgress = maxProgress;
	}
}
