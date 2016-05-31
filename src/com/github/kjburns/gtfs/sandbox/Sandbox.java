package com.github.kjburns.gtfs.sandbox;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.github.kjburns.gtfs.DatasetUniquenessException;
import com.github.kjburns.gtfs.GtfsFile;
import com.github.kjburns.gtfs.InvalidDataException;
import com.github.kjburns.gtfs.MissingRequiredFieldException;
import com.github.kjburns.gtfs.ParentStationNotStationException;
import com.github.kjburns.gtfs.Route;
import com.github.kjburns.gtfs.Stop;
import com.github.kjburns.gtfs.StopTime;
import com.github.kjburns.gtfs.StopTimeCollection;
import com.github.kjburns.gtfs.TerminalTimepointException;
import com.github.kjburns.gtfs.Trip;

public class Sandbox {

	public static void main(String[] args) throws IOException {
		GtfsFile gtfs = null;
		try {
			gtfs = new GtfsFile(args[0], null);
			
			System.err.println("Service IDs");
			System.out.println(gtfs.getServiceCalendar().getServiceIds());
			
			System.err.println("Routes");
			Iterator<Route> rts = gtfs.getRoutes().iterator();
			while (rts.hasNext()) {
				Route rt = rts.next();
				System.out.println(rt.getRouteId() + " " + 
						rt.getShortName() + " " + rt.getLongName());
			}
			
			System.err.println("Trips");
			Iterator<Trip> trips = gtfs.getTrips().getIterator();
			while (trips.hasNext()) {
				System.out.println(trips.next().getTripId());
			}
			
			// just get the first stop in the list
			Stop stop = gtfs.getStops().iterator().next();
			// do a timetable
			System.err.println("Sample Timetable for Today: Stop " + stop.getStopId());
			StopTimeCollection stopTimes = gtfs.getAllTimetables();
			GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("America/Chicago"));
			List<StopTime> timetable = stopTimes.getTimetable(
					stop.getStopId(), now);
			SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
			for (StopTime st : timetable) {
				System.out.print(df.format(
						stopTimes.getEarliestDepartureTime(st, now).getTime()));
				System.out.print(' ');
				Trip tr = gtfs.getTrips().getTripById(st.getTripId());
				Route route = gtfs.getRoutes().getRouteById(tr.getRouteId());
				System.out.print(route.getShortName());
				System.out.print(" (");
				System.out.print(st.getTripId());
				System.out.println(")");
			}
			
			// do a stop listing for the first trip in the list
			Trip trip = gtfs.getTrips().getIterator().next();
			System.err.println("Stops on trip " + trip.getTripId());
			List<StopTime> timetable2 = stopTimes.getTripSchedule(trip.getTripId());
			for (StopTime st : timetable2) {
				System.out.print(df.format(
						stopTimes.getEarliestDepartureTime(st, now).getTime()));
				System.out.print(' ');
				stop = gtfs.getStops().getStopById(st.getStopId());
				System.out.println(stop.getStopName());
			}
		} catch (IOException | InterruptedException | MissingRequiredFieldException | DatasetUniquenessException
				| InvalidDataException | ParentStationNotStationException | TerminalTimepointException ex) {
			ex.printStackTrace();
			System.err.println(ex.toString());
		} finally {
			if (gtfs != null) {
				gtfs.close();
			}
		}
	}

}
