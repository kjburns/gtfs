/*
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
 *   2016-04-29  Load zip file from disk
 */
package com.github.kjburns.gtfs;

import java.io.IOException;

import javax.swing.SwingWorker;

import com.github.kjburns.gtfs.misc.ZipWrapper;

/**
 * An object which retrieves data from a General Transit Feed Specification
 * (GTFS) file. 
 * @see <a href="https://developers.google.com/transit/gtfs/reference">
 * Specification</a>
 * @author Kevin J. Burns
 *
 */
public class GtfsFile implements AutoCloseable {
	private ZipWrapper zipFile = null;
	
	/**
	 * Loads a GTFS file from disk. The file is loaded lazily (i.e., individual
	 * text files are only parsed as they are needed).
	 * @param path Path to the file to load. This file must be a zip file.
	 * @param worker An optional worker thread to report progress to. 
	 * Invocation of cancel on the worker thread will be honored on a 
	 * best-effort basis. If there is no worker thread, pass {@code null}.
	 * @throws IOException If there are problems opening the supplied zip file
	 * @throws InterruptedException if a worker thread was passed and it was
	 * canceled prematurely 
	 */
	public GtfsFile(String path, SwingWorker<?, ?> worker) 
			throws IOException, InterruptedException {
		this.zipFile = new ZipWrapper(path, worker);
		if (worker != null) {
			if (worker.isCancelled()) {
				throw new InterruptedException();
			}
		}
	}
	
	@Override
	public void close() throws IOException {
		if (this.zipFile != null) {
			this.zipFile.close();
		}
	}
}
