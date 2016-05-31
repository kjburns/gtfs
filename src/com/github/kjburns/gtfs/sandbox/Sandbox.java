package com.github.kjburns.gtfs.sandbox;

import java.io.IOException;

import com.github.kjburns.gtfs.DatasetUniquenessException;
import com.github.kjburns.gtfs.GtfsFile;
import com.github.kjburns.gtfs.InvalidDataException;
import com.github.kjburns.gtfs.MissingRequiredFieldException;
import com.github.kjburns.gtfs.ParentStationNotStationException;
import com.github.kjburns.gtfs.TerminalTimepointException;

public class Sandbox {

	public static void main(String[] args) {
		try {
			GtfsFile gtfs = new GtfsFile(args[0], null);
			
			System.err.println("Service IDs");
			System.out.println(gtfs.getServiceCalendar().getServiceIds());
		} catch (IOException | InterruptedException | MissingRequiredFieldException | DatasetUniquenessException
				| InvalidDataException | ParentStationNotStationException | TerminalTimepointException ex) {
			ex.printStackTrace();
			System.err.println(ex.toString());
		}
	}

}
