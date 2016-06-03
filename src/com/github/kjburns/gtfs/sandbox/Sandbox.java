package com.github.kjburns.gtfs.sandbox;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
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
			
			System.out.println("Service IDs______________________");
			System.out.println(gtfs.getServiceCalendar().getServiceIds());
			
			System.out.println("Routes___________________________");
			Iterator<Route> rts = gtfs.getRoutes().iterator();
			while (rts.hasNext()) {
				Route rt = rts.next();
				System.out.println(rt.getRouteId() + " " + 
						rt.getShortName() + " " + rt.getLongName());
			}
			
			System.out.println("Trips____________________________");
			Iterator<Trip> trips = gtfs.getTrips().getIterator();
			while (trips.hasNext()) {
				System.out.println(trips.next().getTripId());
			}
			
			// just get the first stop in the list
			Stop stop = gtfs.getStops().iterator().next();
			// do a timetable
			System.out.println("Sample Timetable for Today: Stop " + stop.getStopId());
			StopTimeCollection stopTimes = gtfs.getAllTimetables();
			LocalDate now = LocalDate.now(ZoneId.of("America/Chicago"));
			List<StopTime> timetable = stopTimes.getTimetable(
					stop.getStopId(), now);
			DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_TIME;
			for (StopTime st : timetable) {
				System.out.print(stopTimes.getEarliestDepartureTime(st, now).format(df));
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
			System.out.println("Stops on trip " + trip.getTripId());
			List<StopTime> timetable2 = stopTimes.getTripSchedule(trip.getTripId());
			for (StopTime st : timetable2) {
				System.out.print(
						stopTimes.getEarliestDepartureTime(st, now).format(df));
				System.out.print(' ');
				stop = gtfs.getStops().getStopById(st.getStopId());
				System.out.println(stop.getStopName());
			}
			
			// do a timepoint listing for the first trip in the list
			System.out.println("Timepoints on trip " + trip.getTripId());
			List<StopTime> timetable3 = 
					stopTimes.getTripScheduleTimepointsOnly(trip.getTripId());
			final GtfsFile gtfs2 = gtfs;
			// playing with streams
			timetable3.stream().forEach((st) -> {
				System.out.print(
						stopTimes.getEarliestDepartureTime(st, now).format(df));
				System.out.print(' ');
				Stop stop2 = gtfs2.getStops().getStopById(st.getStopId());
				System.out.println(stop2.getStopName());
			});
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
