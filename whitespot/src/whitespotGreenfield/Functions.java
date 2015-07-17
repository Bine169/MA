package whitespotGreenfield;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.io.BufferedReader;
import java.io.FileReader;

public class Functions {
	static LocationContainer locationContainer;
	static PolygonContainer polygonContainer;
	static ArrayList<Integer>[] rearrangedPolys = (ArrayList<Integer>[]) new ArrayList[3];
	static int counterIdUsed = 0;
	static boolean raiseThreshold = false;
	static int lastAreaSmallest;
	static int lastAreaBiggest;
	static int lastPolyID;
	static boolean nofoundBiggest = false;
	static boolean nofoundSmallest = false;
	static int nofoundlocbiggest = -1;
	static int nofoundlocsmallest = -1;
	static List<Integer> nofoundlocations = new ArrayList<Integer>();

	private static class polyDistances {
		static List<Integer> ids = new ArrayList<Integer>();
		static ArrayList<Double>[] distances;
	}

	public static void allocatePolygonsByDistance(int numberpolygons,
			int numberlocations) {

		for (int i = 0; i < numberpolygons; i++) {
			int locMinDist = 0;
			double minDistance = polyDistances.distances[i].get(0);
			for (int j = 1; j < numberlocations; j++) {
				if (polyDistances.distances[i].get(j) < minDistance) {
					locMinDist = j;
					minDistance = polyDistances.distances[i].get(j);
				}
			}

			int polyID = polyDistances.ids.get(i);
			System.out.println("write " + polyID + " to " + (locMinDist + 1));
			polygonContainer.setAllocatedLocation(i, locMinDist,
					locationContainer);

			System.out.println(i);
		}

	}

	private static double calculateArea(int loc, int numberpolygons,
			boolean microm, boolean PLZ5) throws SQLException {
		double area = -1;

		Connection jdbc = null;
		Statement stmt = null;

		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		String columnIDs = null;
		String tablegeom = null;
		String tablecrit = null;

		// PLZ5
		if (PLZ5) {
			columnIDs = "_g7304";
			tablegeom = "geometriesplz51";
			tablecrit = "criteriasplz51";
		} else {
			// PLZ8
			columnIDs = "_g7305";
			tablegeom = "geometriesplz81";
			tablecrit = "criteriasplz81";
		}

		StringBuffer sb = new StringBuffer();
		List<Integer> geomIDs = new ArrayList<Integer>();
		for (int i = 0; i < numberpolygons; i++) {
			if (polygonContainer.getPolygon(i).getFlagAllocatedLocation()) {
				if (polygonContainer.getPolygon(i).getAllocatedLocation()
						.getId() == loc) {
					geomIDs.add(polygonContainer.getPolygon(i).getId());
				}
			}
		}

		StringBuilder idsBuffer = new StringBuilder(geomIDs.toString());
		idsBuffer.deleteCharAt(0);
		idsBuffer.deleteCharAt(idsBuffer.length() - 1);

		sb.append("SELECT ST_AREA(ST_UNION(the_geom)) FROM " + tablegeom
				+ " WHERE id IN (" + idsBuffer.toString() + ");");

		ResultSet d = null;
		if (!microm) {
			d = stmt.executeQuery(sb.toString());
		}

		d.next();

		area = d.getDouble(1);

		if (jdbc != null) {
			jdbc.close();
		}

		return area;

	}

	private static double calculateCircumference(int loc, int numberpolygons,
			boolean microm, boolean PLZ5) throws SQLException {
		double circumference = -1;

		Connection jdbc = null;
		Statement stmt = null;

		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		String columnIDs = null;
		String tablegeom = null;
		String tablecrit = null;

		// PLZ5
		if (PLZ5) {
			columnIDs = "_g7304";
			tablegeom = "geometriesplz51";
			tablecrit = "criteriasplz51";
		} else {
			// PLZ8
			columnIDs = "_g7305";
			tablegeom = "geometriesplz81";
			tablecrit = "criteriasplz81";
		}

		StringBuffer sb = new StringBuffer();
		List<Integer> geomIDs = new ArrayList<Integer>();
		for (int i = 0; i < numberpolygons; i++) {
			if (polygonContainer.getPolygon(i).getFlagAllocatedLocation()) {
				if (polygonContainer.getPolygon(i).getAllocatedLocation()
						.getId() == loc) {
					geomIDs.add(polygonContainer.getPolygon(i).getId());
				}
			}
		}

		StringBuilder idsBuffer = new StringBuilder(geomIDs.toString());
		idsBuffer.deleteCharAt(0);
		idsBuffer.deleteCharAt(idsBuffer.length() - 1);

		sb.append("SELECT ST_PERIMETER(ST_UNION(the_geom)) FROM " + tablegeom
				+ " WHERE id IN (" + idsBuffer.toString() + ");");

		ResultSet d = null;
		if (!microm) {
			d = stmt.executeQuery(sb.toString());
		}

		d.next();

		circumference = d.getDouble(1);

		if (jdbc != null) {
			jdbc.close();
		}

		return circumference;

	}

	// /**
	// * Calculates the distance of the location to the actual polygon
	// * @param location
	// * @param geometry: geometry of the polygon to which the distance should
	// be calculated
	// * @param jdbc: JDBCConnection
	// * @return double value of calculated distance
	// * @throws SQLException
	// */
	private static double calculateDist(StringBuffer sb, Statement stmt)
			throws SQLException {
		double distance;
		ResultSet d = stmt.executeQuery(sb.toString());
		d.next();
		distance = d.getDouble(1);

		return distance;
	}

	public static double calculateDistStCentroid(int i, String geometry,
			Statement stmt) throws SQLException {
		double distance;
		Location loc = locationContainer.getLocation(i);
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT ST_AsText(st_centroid(ST_GeomFromText('" + geometry
				+ "')));");
		ResultSet d = stmt.executeQuery(sb.toString());
		d.next();
		String centroid = d.getString(1);
		sb = new StringBuffer();
		sb.append("SELECT st_distance(ST_GeomFromText('POINT(" + loc.getLon()
				+ " " + loc.getLat() + ")'),ST_GeomFromText('" + centroid
				+ "'));");
		distance = calculateDist(sb, stmt);

		return distance;
	}

	public static double calculateDistStDistance(int i, String geometry,
			Statement stmt) throws SQLException {
		double distance;
		Location loc = locationContainer.getLocation(i);
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT st_distance(ST_GeomFromText('POINT(" + loc.getLon()
				+ " " + loc.getLat() + ")'),ST_GeomFromText('" + geometry
				+ "'));");
		distance = calculateDist(sb, stmt);

		return distance;
	}

	public static double calculateNeighbors(String geometrySource,
			String geometryTarget, Statement stmt) throws SQLException {
		double distance;
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT st_distance(ST_GeomFromText('" + geometrySource
				+ "'),ST_GeomFromText('" + geometryTarget + "'));");
		distance = calculateDist(sb, stmt);
		return distance;
	}

	private static String calculateUnit(boolean PLZ5) throws SQLException {
		String unit = null;

		Connection jdbc = null;
		Statement stmt = null;

		String columnIDs = null;
		String tablegeom = null;
		String tablecrit = null;

		// PLZ5
		if (PLZ5) {
			columnIDs = "_g7304";
			tablegeom = "geometriesplz51";
			tablecrit = "criteriasplz51";
		} else {
			// PLZ8
			columnIDs = "_g7305";
			tablegeom = "geometriesplz81";
			tablecrit = "criteriasplz81";
		}

		StringBuffer sb = new StringBuffer();
		sb.append("SELECT ST_UNION(the_geom) FROM " + tablegeom + ";");

		ResultSet d = stmt.executeQuery(sb.toString());

		d.next();

		unit = d.getString(1);

		if (jdbc != null) {
			jdbc.close();
		}

		return unit;
	}

	private static double calculateWeightValue(int locID, Location actLoc,
			int numberpolygons, boolean microm, boolean PLZ5,
			double critAverage, int weightCom, int weightCrit)
			throws SQLException {
		double A_area = calculateArea(locID, numberpolygons, microm, PLZ5);
		double U_area = calculateCircumference(locID, numberpolygons, microm,
				PLZ5);

		// umfang kreis r=U/(2*pi) --> A = pi*r^2 = pi*(U/2*pi)^2
		double A_circle = Math.PI * Math.pow((U_area / (2 * Math.PI)), 2);

		double ratioCom = A_area / A_circle;

		double compactness = 1 - ratioCom;

		// check criteria
		double ratioCrit = actLoc.getCriteria() / critAverage;
		double criteria = Math.abs(1 - ratioCrit);

		double weight = compactness * weightCom + criteria * weightCrit;

		return weight;
	}

	private static void changeCriteriaAfterRearrange(int polyID, int location,
			int locationMaxCriteria, int numberpolygons) throws SQLException {
		// get criteria of the given polygon

		Polygon poly = polygonContainer.getPolygonById(numberpolygons, polyID);
		Location locMaxCrit = locationContainer
				.getLocationByID(locationMaxCriteria);
		Location loc = locationContainer.getLocationByID(location);
		// Location locMaxCrit =
		// locationContainer.getLocation(locationMaxCriteria);
		// Location loc = locationContainer.getLocation(location);

		double critValue = poly.getCriteria();

		System.out.println("criterias before:" + loc.getCriteria() + ","
				+ locMaxCrit.getCriteria());
		double locMaxCritValue = locMaxCrit.getCriteria() - critValue;
		double locCritValue = loc.getCriteria() + critValue;

		locationContainer.setCriteriaByLoc(locMaxCrit, locMaxCritValue);
		locationContainer.setCriteriaByLoc(loc, locCritValue);
		System.out.println("criterias after:" + loc.getCriteria() + ","
				+ locMaxCrit.getCriteria());
		System.out.println("reaarange polygon " + polyID
				+ " with a criteria of " + critValue + " from "
				+ locationMaxCriteria + " to " + location);
	}

	private static double checkChangeofCompactness(Polygon actPoly,
			Location actLoc, int location, double critAverage,
			int numberpolygons, int numberlocations, boolean microm,
			boolean PLZ5, int weightCom, int weightCrit, boolean givesPoly)
			throws SQLException {

		double rateCompCritGive = -1;
		double rateCompCritGet = -1;

		int[] locationIDS = new int[2];
		locationIDS[0] = location;
		locationIDS[1] = actLoc.getId();

		// location Basis= location thats gives or gets a geometry
		Location locBasis = null;

		for (int i = 0; i < numberlocations; i++) {
			if (locationContainer.getLocation(i).getId() == location) {
				locBasis = locationContainer.getLocation(i);
			}
		}

		// simulate Change
		if (givesPoly) {
			actLoc.getAllocatedPolygon().add(actPoly);
			actPoly.setAllocatedLocation(actLoc);

			for (int i = 0; i < locBasis.getAllocatedPolygon().size(); i++) {
				if (locBasis.getAllocatedPolygon().get(i).getId() == actPoly
						.getId()) {
					locBasis.getAllocatedPolygon().remove(i);
				}
			}
		} else {
			locBasis.getAllocatedPolygon().add(actPoly);
			actPoly.setAllocatedLocation(locBasis);

			for (int i = 0; i < actLoc.getAllocatedPolygon().size(); i++) {
				if (actLoc.getAllocatedPolygon().get(i).getId() == actPoly
						.getId()) {
					actLoc.getAllocatedPolygon().remove(i);
				}
			}
		}

		// do calculation for location that gets the geometry and that gives the
		// geometry
		// taken compactness algorithm: Cox algorithm; ratio of an area of a
		// geometry to the area of a circle with same circumference
		// compactness value should be as closed as possible to 1
		for (int i = 0; i < 2; i++) {
			double weight = calculateWeightValue(locationIDS[i], actLoc,
					numberpolygons, microm, PLZ5, critAverage, weightCom,
					weightCrit);

			if (i == 0) {
				rateCompCritGive = weight;
			} else {
				rateCompCritGet = weight;
			}
		}

		// check change of weightedValue ; value >0 compactness getting worse,
		// value<0 compactness getting better
		double changeGive = rateCompCritGive - locBasis.getWeightValue();
		double changeGet = rateCompCritGet - actLoc.getWeightValue();

		// reset change
		if (givesPoly) {
			locBasis.getAllocatedPolygon().add(actPoly);
			actPoly.setAllocatedLocation(locBasis);

			for (int i = 0; i < actLoc.getAllocatedPolygon().size(); i++) {
				if (actLoc.getAllocatedPolygon().get(i).getId() == actPoly
						.getId()) {
					actLoc.getAllocatedPolygon().remove(i);
				}
			}
		} else {
			actLoc.getAllocatedPolygon().add(actPoly);
			actPoly.setAllocatedLocation(actLoc);

			for (int i = 0; i < locBasis.getAllocatedPolygon().size(); i++) {
				if (locBasis.getAllocatedPolygon().get(i).getId() == actPoly
						.getId()) {
					locBasis.getAllocatedPolygon().remove(i);
				}
			}
		}

		// sum could be < 0 if both compactness getting better or one is getting
		// better much
		return (changeGive + changeGet);
	}

	public static void checkThreshold(int numberpolygons, int numberlocations,
			int threshold, boolean microm, boolean PLZ5,
			int weightCom, int weightCrit, boolean whitespot, int numberGivenLocations, int numberNewLocations) throws SQLException, Exception {

		boolean satisfied = false;

		for (int i = 0; i < rearrangedPolys.length; i++)
			rearrangedPolys[i] = new ArrayList<Integer>();

		lastAreaBiggest = -1;
		lastAreaSmallest = -1;
		int run = 0;
		double critAverage = 0;

		double critSum = 0;

		for (int i = 0; i < numberlocations; i++) {
			critSum = critSum + locationContainer.getCriteria(i);
		}

			critAverage = critSum / numberlocations;

		for (int i = 0; i < numberlocations; i++) {
			initCompactnessRatio(locationContainer.getLocation(i), critAverage,
					numberpolygons, microm, PLZ5, weightCom, weightCrit);
		}

		Random random = new Random();
		int location = 0;

		while (!satisfied) {
			int[] compCriterias = new int[numberlocations];
			int compCrits = 0;
			for (int i = 0; i < numberlocations; i++) {
				compCriterias[i] = 0;
				System.out.println(locationContainer.getLocation(i)
						.getCriteria());
			}

			for (int i = 0; i < numberlocations; i++) {
				double value = locationContainer.getCriteria(i) * 100
						/ critAverage;
				double difference = -1;

				if (value > 100) {
					difference = value - 100;
				} else {
					difference = 100 - value;
				}

				if (difference < threshold) {
					compCrits++;
				}
			}

			int no = 0;
			boolean arranged = false;

				List<Integer> compCriteriasFail = new ArrayList<Integer>();

				if (compCrits != numberlocations) {

					System.out.println("location" + location);

					// check whether it is necessary to rearrange a polygon in
					// that area
					// get difference to average value
					double value = locationContainer.getCriteria(location)
							* 100 / critAverage;
					double difference = -1;

					if (value > 100) {
						difference = value - 100;
					} else {
						difference = 100 - value;
					}
					System.out.println(difference + "," + critAverage);

					if (!nofoundlocations.contains((location + 1))
							&& difference > threshold) {
						if (!whitespot){
						rearrangePolysWithRandomStart(numberpolygons,
								numberlocations, (location + 1), critAverage,
								microm, PLZ5, weightCom, weightCrit);}
						else{
							rearrangePolysWhitespot(numberpolygons,
									numberGivenLocations, numberNewLocations, (location + 1), critAverage,
									microm, PLZ5, weightCom, weightCrit);
						}
//						 FileWriter output =createFileWriter();
//						 writePolygon(output, numberpolygons);
//						 output.close();
					}

					location++;
					if (location >= numberlocations) {
						location = 0;
					}
				}
			run++;

			if (compCrits == numberlocations) {
				satisfied = true;
			}

			if (nofoundBiggest && nofoundSmallest) {
				nofoundBiggest = false;
				nofoundSmallest = false;
				if (nofoundlocsmallest == nofoundlocbiggest) {
					nofoundlocations.add(nofoundlocsmallest);
				}
			}

			if (raiseThreshold && !satisfied) {
				threshold = threshold + 5;
				raiseThreshold = false;
				System.out.println("Threshold raised to " + threshold);
			}

			if (nofoundlocations.size() >= numberlocations || run > 500) {
				satisfied = true;
				System.out.println("Break");
			}

		}

		System.out.println("rearranged with a variance of " + threshold + "%");
		System.out.println("no better arrangement for " + nofoundlocations);
	}

	private static boolean checkUnitCalculationGives(int polyID, int loc,
			int numberpolygons) throws InterruptedException {
		boolean unit = false;

		// simpler to use buffPoly without polyID
		// List<Integer> buffAllocPolysLoc = new ArrayList<Integer>();
		// buffAllocPolysLoc=allocPolys[loc];

		// check unit of location that gives geometry
		// check unit if on polygone has no neighbour
		int nrNeighbours = 0;
		int countAllocatedPolygons = 0;

		for (int i = 0; i < numberpolygons; i++) {
			Polygon poly = polygonContainer.getPolygon(i);

			if (poly.getAllocatedLocation().getId() == loc) {

				if (poly.getId() != polyID) {
					countAllocatedPolygons++;
					int actPoly = poly.getId();

					boolean neighbour = false;
					int pos = 0;

					List<Polygon> neighbours = poly.getNeighbours();
					while (!neighbour) {
						Polygon polyNeigh = polygonContainer.getPolygon(pos);
						if (polyNeigh.getAllocatedLocation().getId() == loc) {
							for (int j = 0; j < neighbours.size(); j++) {
								if (neighbours.get(j).getId() == polyNeigh
										.getId()
										&& neighbours.get(j).getId() != actPoly
										&& poly.getId() != polyID) {
									neighbour = true;
									nrNeighbours++;
								}
							}
						}

						pos++;
						if (pos >= numberpolygons) {
							neighbour = true;
						}
					}
				}
			}
		}

		if ((nrNeighbours) == countAllocatedPolygons) {
			unit = true;
		}

		// check unit by using graphs
		if (unit) {
			int pos = 0;
			boolean graphEnds = false;
			List<Integer> neighbours = new ArrayList<Integer>();
			List<Polygon> polysTaken = new ArrayList<Polygon>();
			List<Polygon> buffAllocPolysLoc = new ArrayList<Polygon>();
			for (int i = 0; i < numberpolygons; i++) {
				Polygon poly = polygonContainer.getPolygon(i);
				if (poly.getAllocatedLocation().getId() == loc) {
					buffAllocPolysLoc.add(poly);
				}
			}

			for (int i = 0; i < buffAllocPolysLoc.size(); i++) {
				if (buffAllocPolysLoc.get(i).getId() == polyID) {
					buffAllocPolysLoc.remove(i);
				}
			}

			while (!graphEnds) {
				Polygon actPoly = polygonContainer.getPolygon(pos);

				if (actPoly.getAllocatedLocation().getId() == loc
						&& actPoly.getId() != polyID) {

					boolean takeNextNeighbour = false;
					if (neighbours.size() > 0) {
						if (actPoly.getId() == neighbours.get(0)) {
							takeNextNeighbour = true;

						}
					} else {
						takeNextNeighbour = true;
					}

					if (takeNextNeighbour) {
						boolean allreadyTaken = false;
						for (int i = 0; i < polysTaken.size(); i++) {
							if (actPoly.getId() == polysTaken.get(i).getId()) {
								allreadyTaken = true;
							}
						}

						if (!allreadyTaken) {
							polysTaken.add(actPoly);

							for (int j = 0; j < actPoly.getNeighbours().size(); j++) {
								for (int k = 0; k < buffAllocPolysLoc.size(); k++) {
									if (buffAllocPolysLoc.get(k).getId() == actPoly
											.getNeighbours().get(j).getId()) {

										if (!neighbours
												.contains(buffAllocPolysLoc
														.get(k).getId())) {
											neighbours.add(buffAllocPolysLoc
													.get(k).getId());

										}
									}
								}
							}
						}

						if (neighbours.size() > 0) {
							pos = 0;
							neighbours.remove(0);
						} else {
							graphEnds = true;
						}

					} else {
						pos++;
					}
				} else {
					pos++;
				}
			}

			int countPolysTaken = 0;

			for (int j = 0; j < polysTaken.size(); j++) {
				for (int k = 0; k < buffAllocPolysLoc.size(); k++) {
					if (buffAllocPolysLoc.get(k).equals(polysTaken.get(j))) {
						countPolysTaken++;
					}
				}
			}

			if (buffAllocPolysLoc.size() == countPolysTaken) {
				unit = true;
			} else {
				unit = false;
			}
		}

		return unit;
	}

	private static boolean checkUnitCalculationGets(int polyID, int loc,
			int numberpolygons) {
		boolean unit = false;

		// check unit of location that gets geometry

		// get Position of poly

		Polygon poly = polygonContainer.getPolygonById(numberpolygons, polyID);
		boolean foundNeighbour = false;
		int counter = 0;

		// check neighbours
		while (!foundNeighbour) {
			// take all neighbours of poly
			for (int j = 0; j < poly.getNeighbours().size(); j++) {
				// take all polys of location
				if (poly.getNeighbours().get(j).getId()!=poly.getId()){
					for (int k = 0; k < numberpolygons; k++) {
						Polygon actPoly = polygonContainer.getPolygon(k);
						if (actPoly.getFlagAllocatedLocation()) {
							if (actPoly.getAllocatedLocation().getId() == loc) {
								if (poly.getNeighbours().get(j).getId() == actPoly
										.getId()) {
									unit = true;
									foundNeighbour = true;
								}
							}
						}
					}
				}
				counter++;
			}

			if (counter == poly.getNeighbours().size() && !foundNeighbour) {
				foundNeighbour = true;
			}
		}

		return unit;
	}

	public static boolean checkUnit(int polyID, int locGive, int locGet,
			int numberpolygons) throws InterruptedException {
		boolean unit = false;

		boolean unitGives = checkUnitCalculationGives(polyID, locGive,
				numberpolygons);
		boolean unitGets = checkUnitCalculationGets(polyID, locGet,
				numberpolygons);

		if (unitGets && unitGives) {
			unit = true;
		}

		return unit;
	}
	
	public static void checkUnityAfterAllocByDist(int numberpolygons, int numberlocations) throws Exception{
		//check unity for every location
		for (int j=0;j<numberlocations;j++){
			
			Location loc = locationContainer.getLocation(j);
			
			boolean unit;
			boolean first=true;
			int pos = 0;
			boolean graphEnds = false;
			List<Integer> neighbours = new ArrayList<Integer>();
			List<Polygon> polysTaken = new ArrayList<Polygon>();
			List<Polygon> polysNotTaken = new ArrayList<Polygon>();
			List<Polygon> buffAllocPolysLoc = new ArrayList<Polygon>();
			for (int i = 0; i < numberpolygons; i++) {
				Polygon poly = polygonContainer.getPolygon(i);
				if (poly.getAllocatedLocation().getId() == loc.getId()) {
					buffAllocPolysLoc.add(poly);
				}
			}

			while (!graphEnds) {
				Polygon actPoly=null;
				if (first){
					for (int i=0;i<buffAllocPolysLoc.size();i++){
						if (buffAllocPolysLoc.get(i).getId()==loc.getHomePolyId()){
							actPoly=buffAllocPolysLoc.get(i);
							first=false;
						}
					}
				}
				else{
					actPoly = polygonContainer.getPolygon(pos);
				}

				if (actPoly.getAllocatedLocation().getId() == loc.getId()) {

					boolean takeNextNeighbour = false;
					if (neighbours.size() > 0) {
						if (actPoly.getId() == neighbours.get(0)) {
							takeNextNeighbour = true;

						}
					} else {
						takeNextNeighbour = true;
					}

					if (takeNextNeighbour) {
						boolean allreadyTaken = false;
						for (int i = 0; i < polysTaken.size(); i++) {
							if (actPoly.getId() == polysTaken.get(i).getId()) {
								allreadyTaken = true;
							}
						}

						if (!allreadyTaken) {
							polysTaken.add(actPoly);

							for (int l = 0; l < actPoly.getNeighbours().size(); l++) {
								for (int k = 0; k < buffAllocPolysLoc.size(); k++) {
									if (buffAllocPolysLoc.get(k).getId() == actPoly
											.getNeighbours().get(l).getId()) {

										if (!neighbours
												.contains(buffAllocPolysLoc
														.get(k).getId())) {
											neighbours.add(buffAllocPolysLoc
													.get(k).getId());

										}
									}
								}
							}
						}

						if (neighbours.size() > 0) {
							pos = 0;
							neighbours.remove(0);
						} else {
							graphEnds = true;
						}

					} else {
						pos++;
					}
				} else {
					pos++;
				}
			}

			int countPolysTaken = 0;

			for (int l = 0; l < polysTaken.size(); l++) {
				for (int k = 0; k < buffAllocPolysLoc.size(); k++) {
					if (buffAllocPolysLoc.get(k).equals(polysTaken.get(l))) {
						countPolysTaken++;
					}
				}
			}

			if (buffAllocPolysLoc.size() == countPolysTaken) {
				unit = true;
			} else {
				unit = false;
			}

			//if area has more than one part
			if (!unit){
				for (int i=0;i<buffAllocPolysLoc.size();i++){
					if (!polysTaken.contains(buffAllocPolysLoc.get(i))){
						polysNotTaken.add(buffAllocPolysLoc.get(i));
					}
				}
				
				//take every poly which is aside and rearrange it to another area
				int posPoly=0;
				while(polysNotTaken.size()>0){
					if (posPoly>=polysNotTaken.size()){
						posPoly=0;
					}
					
					Polygon actPoly=polysNotTaken.get(posPoly);
					
					boolean rearranged=false;
					List <Integer> notUseableLocs = new ArrayList<Integer>();
					notUseableLocs.add(loc.getId());
					
					while(!rearranged){
						double minDist =-1;
						Location locMinDist = null;
						
						int posActPoly = -1;
						for (int k=0;k<numberpolygons;k++){
							if (polyDistances.ids.get(k)==actPoly.getId()){
								posActPoly=k;
							}
						}
						
						boolean firstLoc=true;
						//determine Location with next smallest Dist
						for (int k=0;k<numberlocations;k++){
							Location actLoc = locationContainer.getLocation(k);
							if (firstLoc && !notUseableLocs.contains(actLoc.getId())){
								minDist=polyDistances.distances[posActPoly].get(k);
								locMinDist=locationContainer.getLocation(k);
								firstLoc=false;
							}
							else{
								if (polyDistances.distances[posActPoly].get(k)<minDist && !notUseableLocs.contains(actLoc.getId())){
									minDist=polyDistances.distances[posActPoly].get(k);
									locMinDist=locationContainer.getLocation(k);
								}
							}
						}
						
						
						//simulate Change
						locMinDist.getAllocatedPolygon().add(actPoly);
						actPoly.setAllocatedLocation(locMinDist);
						loc.removeAllocatedPolygon(actPoly);
	
						//check unity of area that gets the polygon
						boolean unity = checkUnitCalculationGets(actPoly.getId(), locMinDist.getId(), numberpolygons);
						
						
						//do or abort change
						if (unity){
							System.out.println("set "+actPoly.getId()+" from "+loc.getId()+" to "+locMinDist.getId());
							System.out.println("old: "+loc.getCriteria()+","+locMinDist.getCriteria());
							
							double critPoly = actPoly.getCriteria();
							double critOld = loc.getCriteria();
							double critNew = critOld-critPoly;
							loc.setCriteria(critNew);
							
							critOld = locMinDist.getCriteria();
							critNew = critOld+critPoly;
							locMinDist.setCriteria(critNew);
							rearranged=true;
							polysNotTaken.remove(posPoly);
							
							System.out.println("new: "+loc.getCriteria()+","+locMinDist.getCriteria());
						}
						else{
							locMinDist.removeAllocatedPolygon(actPoly);
							actPoly.setAllocatedLocation(loc);
							loc.getAllocatedPolygon().add(actPoly);
							notUseableLocs.add(locMinDist.getId());
						}
						
						if (notUseableLocs.size()==numberlocations){
							posPoly++;
							rearranged=true;
						}
					}
				}
			}
		}
		
		for (int i = 0; i < numberlocations; i++) {
			System.out.println(locationContainer.getLocation(i)
					.getCriteria());
		}
	}

	public static FileWriter createFileWriter() throws IOException {
		String filename = "polygones" + System.currentTimeMillis() + ".csv";
		FileWriter output = new FileWriter(filename);
		output.append(new String("ID,Location"));
		output.append("\n");

		return output;
	}

	public static void createFileWriterLocs(int numberlocations)
			throws IOException {
		FileWriter outputloc = new FileWriter("locations.csv");
		outputloc.append(new String("ID, Long, Lat"));
		outputloc.append("\n");

		int coordpos = 0;
		for (int i = 1; i < (numberlocations + 1); i++) {
			Location loc = locationContainer.getLocation(i - 1);
			outputloc.append(Objects.toString(i));
			outputloc.append(",");
			outputloc.append(Objects.toString(loc.getLon()));
			outputloc.append(",");
			outputloc.append(Objects.toString(loc.getLat()));
			outputloc.append("\n");
			coordpos = coordpos + 2;
		}
		outputloc.close();
	}

	private static String createUnion(int location, int numberpolygons,
			boolean microm, boolean PLZ5) throws SQLException {
		String union = null;

		Connection jdbc = null;
		Statement stmt = null;

		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		String columnIDs = null;
		String tablegeom = null;
		String tablecrit = null;

		// PLZ5
		if (PLZ5) {
			columnIDs = "_g7304";
			tablegeom = "geometriesplz51";
			tablecrit = "criteriasplz51";
		} else {
			// PLZ8
			columnIDs = "_g7305";
			tablegeom = "geometriesplz81";
			tablecrit = "criteriasplz81";
		}

		StringBuffer sb = new StringBuffer();
		List<Integer> geomIDs = new ArrayList<Integer>();
		for (int i = 0; i < numberpolygons; i++) {
			if (polygonContainer.getPolygon(i).getAllocatedLocation().getId() == location) {
				geomIDs.add(polygonContainer.getPolygon(i).getId());
			}
		}

		StringBuilder idsBuffer = new StringBuilder(geomIDs.toString());
		idsBuffer.deleteCharAt(0);
		idsBuffer.deleteCharAt(idsBuffer.length() - 1);

		sb.append("SELECT ST_ASTEXT(ST_UNION(the_geom)) FROM " + tablegeom
				+ " WHERE id IN (" + idsBuffer.toString() + ");");
		System.out.println(sb);

		ResultSet d = null;
		if (!microm) {
			d = stmt.executeQuery(sb.toString());
		}

		d.next();

		union = d.getString(1);

		if (jdbc != null) {
			jdbc.close();
		}

		return union;
	}

	public static void determineHomePoly(boolean PLZ5, int numberlocations,
			boolean microm) throws SQLException {

		Connection jdbc = null;
		Statement stmt = null;

		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		String columnIDs = null;
		String tablegeom = null;
		String tablecrit = null;

		// PLZ5
		if (PLZ5) {
			columnIDs = "_g7304";
			tablegeom = "geometriesplz51";
			tablecrit = "criteriasplz51";
		} else {
			// PLZ8
			columnIDs = "_g7305";
			tablegeom = "geometriesplz81";
			tablecrit = "criteriasplz81";
		}

		int id;
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < numberlocations; i++) {
			System.out.println(i+","+numberlocations);
			Location loc = locationContainer.getLocation(i);
			sb = new StringBuffer();
			// SELECT id FROM geometriesplz5 WHERE
			// ST_Contains(the_geom,st_setsrid(st_makepoint(13.72047,51.09358),4326))
			// LIMIT 1;
			sb.append("SELECT id FROM " + tablegeom
					+ " WHERE ST_Contains(the_geom,ST_Setsrid(ST_Makepoint("
					+ loc.getLon() + "," + loc.getLat() + "),4326)) LIMIT 1;");
			ResultSet d = null;
			if (!microm) {
				d = stmt.executeQuery(sb.toString());
			}

			d.next();
			id = d.getInt(1);
			locationContainer.setHomePoly(i, id);
		}

		if (jdbc != null) {
			jdbc.close();
		}

	}

	public static Connection getConnection() {
		Connection jdbc = null;
		try {
			Class.forName("org.postgresql.Driver");
			jdbc = DriverManager.getConnection(
					"jdbc:postgresql://localhost:5432/MA", "postgres", "");
			System.out.println("Opened database successfully");
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}

		return jdbc;

	}

	public static ResultSet getNearestNeighbours(int polyID, String tablegeom,
			Statement jdbc) throws SQLException {
		// SELECT (pgis_fn_nn(p1.the_geom, 0.0005, 1000, 10, 'geometries',
		// 'true', 'id', 'the_geom' )).nn_gid::int FROM (SELECT
		// st_geomfromtext((Select st_astext(the_geom) FROM geometries WHERE
		// ID=1), 4326) AS the_geom) AS p1;
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT (pgis_fn_nn(p1.the_geom, 0.0, 1000, 10, '"
				+ tablegeom
				+ "', 'true', 'id', 'the_geom' )).nn_gid::int FROM (SELECT st_geomfromtext((Select st_astext(the_geom) FROM "
				+ tablegeom + " WHERE ID=" + polyID
				+ "), 4326) AS the_geom) AS p1;");
		ResultSet rNeighbours = jdbc.executeQuery(sb.toString());

		return rNeighbours;
	}

	//
	// /**
	// * calculates the number of polygons which are inside the Area; it is
	// necessary to allocate all polygons
	// * @return number of polygons
	// * @throws SQLException
	// */
	public static int getNrOrSum(boolean number, boolean PLZ5, boolean microm)
			throws SQLException, ClassNotFoundException {

		Connection jdbc = null;
		Statement stmt = null;

		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		StringBuffer sb = new StringBuffer();
		String table = null;
		if (PLZ5) {
			table = "criteriasplz51";
		} else {
			table = "criteriasplz81";
		}

		if (number) {
			sb.append("SELECT COUNT (id) FROM " + table + ";");
		} else {
			sb.append("SELECT SUM(CAST(_c1 AS int)) FROM " + table + ";");
		}

		ResultSet t = null;
		if (!microm) {
			t = stmt.executeQuery(sb.toString());
		}

		t.next();
		int sum = t.getInt(1);
		if (number) {
			System.out.println("numberofpolygons: " + sum);
		} else {
			System.out.println("sum: " + sum);
		}

		if (jdbc != null) {
			jdbc.close();
		}

		return sum;
	}

	private static void initCompactnessRatio(Location loc, double critAverage,
			int numberpolygons, boolean microm, boolean PLZ5, int weightCom,
			int weightCrit) throws SQLException {
		double weight = calculateWeightValue(loc.getId(), loc, numberpolygons,
				microm, PLZ5, critAverage, weightCom, weightCrit);

		loc.setWeightValue(weight);
	}

	public static void initCriteria(int numberpolygons, int numberlocations) {

		//reset criteria to 0 (necessary for whitespot)
		for (int i = 0; i < numberlocations; i++) {
			locationContainer.setCriteria(i, 0);
		}
		
		for (int i = 0; i < numberpolygons; i++) {
			double crit = polygonContainer.getPolygon(i).getCriteria();
			Location loc = polygonContainer.getAllocatedLocation(i);
			double newcrit = loc.getCriteria() + crit;

			locationContainer.setCriteriaByLoc(loc, newcrit);
		}

		for (int i = 0; i < numberlocations; i++) {
			System.out.println(locationContainer.getCriteria(i));
		}
	}

	public static void initDistances(int numberpolygons, int numberlocations,
			boolean microm) throws SQLException {

		Connection jdbc = null;
		Statement stmt = null;

		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		polyDistances.distances = (ArrayList<Double>[]) new ArrayList[numberpolygons];
		for (int i = 0; i < polyDistances.distances.length; i++)
			polyDistances.distances[i] = new ArrayList<Double>();

		for (int i = 0; i < numberpolygons; i++) {
			System.out.println(i);
			Polygon poly = polygonContainer.getPolygon(i);
			String geometry = poly.getGeometry();
			polyDistances.ids.add(poly.getId());

			for (int j = 0; j < numberlocations; j++) {
				double distance = -1;
				if (!microm) {
					// distance = calculateDistStCentroid(j, geometry, stmt);
					distance = calculateDistStDistance(j, geometry, stmt);
				}

				polyDistances.distances[i].add(distance);
			}
		}

		if (jdbc != null) {
			jdbc.close();
		}
	}

	public static LocationContainer initLocationContainer() {
		locationContainer = new LocationContainer();
		return locationContainer;
	}

	public static void initNeighbours(int numberpolygons, boolean PLZ5,
			boolean microm) throws SQLException {

		Connection jdbc = null;
		Statement stmt = null;

		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		String columnIDs = null;
		String tablegeom = null;
		String tablecrit = null;
		// PLZ5
		if (PLZ5) {
			columnIDs = "_g7304";
			tablegeom = "geometriesplz51";
			tablecrit = "criteriasplz51";
		} else {
			// PLZ8
			columnIDs = "_g7305";
			tablegeom = "geometriesplz81";
			tablecrit = "criteriasplz81";
		}

		for (int i = 0; i < numberpolygons; i++) {
			System.out.println(i);
			List<Integer> neighbours = new ArrayList<Integer>();
			Polygon poly = polygonContainer.getPolygon(i);
			ResultSet nN = null;
			if (!microm) {
				nN = getNearestNeighbours(poly.getId(), tablegeom, stmt);
			}

			boolean last = false;
			while (!last) {
				nN.next();
				neighbours.add(nN.getInt(1));
				if (nN.isLast()) {
					last = true;
				}
			}

			List<Polygon> neighbourPolys = new ArrayList<Polygon>();

			for (int j = 0; j < neighbours.size(); j++) {
				boolean found = false;
				int pos = 0;

				while (!found) {
					Polygon actPoly = polygonContainer.getPolygon(pos);
					if (actPoly.getId() == neighbours.get(j)) {
						found = true;
						neighbourPolys.add(actPoly);
					} else {
						pos++;
					}

					if (pos > numberpolygons) {
						found = true;
					}
				}
			}

			poly.setNeighbours(neighbourPolys);
		}

		if (jdbc != null) {
			jdbc.close();
		}
	}

	private static void rearrangeFromBiggest(int numberlocations,
			int numberpolygons) throws SQLException, Exception {

		double[] criteriaBuffer = new double[numberlocations];
		List<Integer> notNeighbouredPlys = new ArrayList<Integer>();

		for (int j = 0; j < criteriaBuffer.length; j++) {
			criteriaBuffer[j] = locationContainer.getCriteria(j);
		}

		for (int j = 0; j < criteriaBuffer.length - 1; j++) {
			int index = j;
			for (int k = j + 1; k < criteriaBuffer.length; k++)
				if (criteriaBuffer[k] > criteriaBuffer[index])
					index = k;
			double greaterNumber = criteriaBuffer[index];
			criteriaBuffer[index] = criteriaBuffer[j];
			criteriaBuffer[j] = greaterNumber;
		}

		// criteriaBuffer[0] area with biggest critSum
		// detect neighbours of this area

		List<Integer> actNeighbours = new ArrayList<Integer>();
		int locBiggest = -1;

		// determine location with biggest critSum
		for (int j = 0; j < numberlocations; j++) {
			if (locationContainer.getCriteria(j) == criteriaBuffer[0]) {
				locBiggest = j + 1;
			}
		}

		// System.out.println("determine neigbours");
		// determine neighbours of area of location with biggest critSum
		for (int j = 0; j < numberlocations; j++) {
			if (j != locBiggest) {
				boolean neighbour = false;
				int pos = 0;

				// System.out.println(locBiggest);

				while (pos < numberpolygons && neighbour == false) {
					// check every allocated Polygone whether it is a neighbour
					// of one of the polys of another location
					// take poly of locBiggest and check to every poly of loc

					Polygon poly = polygonContainer.getPolygon(pos);
					// System.out.println(poly.getLocationId()+","+(locBiggest+1));
					if (poly.getLocationId() == (locBiggest)) {
						boolean neighbourfound = false;

						for (int k = 0; k < numberpolygons; k++) {
							Polygon comparePoly = polygonContainer
									.getPolygon(k);

							// System.out.println(comparePoly.getLocationId()+","+j);
							if (comparePoly.getLocationId() == (j + 1)) {
								List<Polygon> neighbours = poly.getNeighbours();
								// System.out.println("compare "+comparePoly.getId()+" to "+poly.getId()+","+neighbours);
								for (int l = 0; l < neighbours.size(); l++) {
									if (neighbours.get(l).getId() == comparePoly
											.getId() && !neighbourfound) {
										neighbour = true;
										actNeighbours.add(j + 1);
										neighbourfound = true;
									}
								}
							}
						}

						pos++;
					} else {
						pos++;
					}

				}
			}
		}

		// determine that area of neighbours areas with smallest critSum
		double minsum = -1;
		boolean first = true;
		int locMinsum = -1;

		for (int j = 0; j < numberlocations; j++) {
			boolean found = false;
			int posLoc = 0;
			while (!found) {
				if (actNeighbours.get(posLoc) == j && j != lastAreaBiggest
						&& !nofoundlocations.contains(j)
						&& j != nofoundlocbiggest) {
					if (first) {
						first = false;
						minsum = locationContainer.getCriteria(j);
						locMinsum = j;
					} else {
						if (locationContainer.getCriteria(j) < minsum) {
							locMinsum = j;
							minsum = locationContainer.getCriteria(j);
						}
					}

					found = true;
				}
				if ((posLoc + 1) < actNeighbours.size()) {
					posLoc++;
				} else {
					found = true;
				}
			}
		}

		// give locMinSum 1 polygone of locbiggest

		int polyID = -1;
		double minDist = -1;

		boolean getStartPoly = false;
		boolean foundStartPoly = false;
		int pos = 0;
		while (!getStartPoly) {
			Polygon poly = polygonContainer.getPolygon(pos);
			if (poly.getAllocatedLocation().getId() == locBiggest) {
				polyID = poly.getId();

				// check whether polyID is homepoly
				boolean homepoly = false;
				for (int i = 0; i < numberlocations; i++) {
					Location loc = locationContainer.getLocation(i);
					if (polyID == loc.getHomePolyId()) {
						homepoly = true;
					}
				}

				if (!homepoly && polyID != lastPolyID) {
					if (checkUnit(polyID, locBiggest, locMinsum, numberpolygons)) {
						getStartPoly = true;
						minDist = polyDistances.distances[polyDistances.ids
								.indexOf(polyID)].get(locMinsum - 1);
						foundStartPoly = true;
					} else {
						pos++;
					}
				} else {
					pos++;
				}
			} else {
				pos++;
			}

			if (pos == numberpolygons) {
				getStartPoly = true;
			}
		}

		if (foundStartPoly) {
			for (int l = 1; l < numberpolygons; l++) {
				Polygon poly = polygonContainer.getPolygon(l);

				if (poly.getAllocatedLocation().getId() == locBiggest) {
					int buffpolyID = poly.getId();
					double actDist = polyDistances.distances[polyDistances.ids
							.indexOf(buffpolyID)].get(locMinsum - 1);

					boolean homePoly = false;
					for (int i = 0; i < numberlocations; i++) {
						if (buffpolyID == locationContainer.getLocation(i)
								.getHomePolyId())
							homePoly = true;
					}

					if (actDist < minDist && !homePoly) {
						boolean unit = checkUnit(buffpolyID, locBiggest,
								locMinsum, numberpolygons);

						if (unit) {
							minDist = actDist;
							polyID = buffpolyID;
						}
					}
				}
			}

			counterIdUsed = 0;
			for (int k = 0; k < rearrangedPolys[0].size(); k++) {
				if (rearrangedPolys[2].get(k).equals(locMinsum)
						&& rearrangedPolys[1].get(k).equals(locBiggest)
						&& rearrangedPolys[0].get(k).equals(polyID)) {
					counterIdUsed++;
				}
			}

			lastAreaBiggest = locBiggest;

			rearrangedPolys[0].add(polyID);
			rearrangedPolys[1].add(locBiggest);
			rearrangedPolys[2].add(locMinsum);
			System.out.println(counterIdUsed);

			if (rearrangedPolys[0].size() > (numberlocations * numberlocations)) {
				rearrangedPolys[0].remove(0);
				rearrangedPolys[1].remove(0);
				rearrangedPolys[2].remove(0);
			}

			// add polyID to locMinsum and remove from locbiggest
			lastPolyID = polyID;
			System.out.println("Set to " + (locMinsum) + " remove " + polyID
					+ " from " + (locBiggest));

			Location locGets = locationContainer.getLocation(locMinsum - 1);
			Location locGives = locationContainer.getLocation(locBiggest - 1);
			Polygon polyMoves = polygonContainer.getPolygonById(numberpolygons,
					polyID);

			locGets.setAllocatedPolygon(polyMoves);
			locGives.removeAllocatedPolygon(polyMoves);
			polyMoves.setAllocatedLocation(locGets);

			changeCriteriaAfterRearrange(polyID, locMinsum, locBiggest,
					numberpolygons);

		} else {
			nofoundBiggest = true;
			nofoundlocbiggest = locMinsum;
		}
	}

	public static void initPolygones(int numberpolygons, int numberlocations,
			boolean PLZ5, boolean microm) throws SQLException {

		Connection jdbc = null;
		Statement stmt = null;

		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		String columnIDs = null;
		String tablegeom = null;
		String tablecrit = null;
		// PLZ5
		if (PLZ5) {
			columnIDs = "_g7304";
			tablegeom = "geometriesplz51";
			tablecrit = "criteriasplz51";
		} else {
			// PLZ8
			columnIDs = "_g7305";
			tablegeom = "geometriesplz81";
			tablecrit = "criteriasplz81";
		}

		// get all PolygonIDs and store it
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT t2.id AS id, ST_AsTEXT(the_geom) AS the_geom, _c1 AS criteria FROM "
				+ tablegeom
				+ " AS t1 INNER JOIN "
				+ tablecrit
				+ " AS t2 ON t2." + columnIDs + "=t1.id");
		System.out.println(sb);

		ResultSet t = null;

		if (!microm) {
			t = stmt.executeQuery(sb.toString());
		}

		polygonContainer = new PolygonContainer();

		// get ids, geometry and criteria
		for (int i = 0; i < numberpolygons; i++) {
			System.out.println(i);
			t.next();
			int id = t.getInt("id");
			String geometry = t.getString("the_geom");
			double criteria = t.getDouble("criteria");

			polygonContainer.add(id, geometry, criteria);
		}

		if (jdbc != null) {
			jdbc.close();
		}
	}

	private static void rearrangeFromSmallest(int numberlocations,
			int numberpolygons) throws SQLException, InterruptedException {

		int locSmallestCritSum = -1;

		// init array for sorted critSums
		double[] criteriaBuffer = new double[numberlocations];
		for (int j = 0; j < criteriaBuffer.length; j++) {
			criteriaBuffer[j] = locationContainer.getCriteria(j);
		}

		// sort critSums
		for (int j = 0; j < criteriaBuffer.length - 1; j++) {
			int index = j;
			for (int k = j + 1; k < criteriaBuffer.length; k++)
				if (criteriaBuffer[k] < criteriaBuffer[index])
					index = k;
			double greaterNumber = criteriaBuffer[index];
			criteriaBuffer[index] = criteriaBuffer[j];
			criteriaBuffer[j] = greaterNumber;
		}

		// determine area with smallest critSum
		boolean foundlocSmallestCritSum = false;
		int poslocSmallCritSum = 0;

		while (!foundlocSmallestCritSum) {

			for (int j = 0; j < numberlocations; j++) {
				if (criteriaBuffer[poslocSmallCritSum] == locationContainer
						.getCriteria(j)) {
					locSmallestCritSum = j + 1;
				}
			}

			if (!nofoundlocations.contains(locSmallestCritSum)
					&& locSmallestCritSum != nofoundlocsmallest) {
				foundlocSmallestCritSum = true;
			} else {
				poslocSmallCritSum++;
			}

		}

		System.out.println("area with smallest critsum " + locSmallestCritSum);

		boolean getStartPoly = false;
		int pos = 0;
		int polyID = -1;
		double minDistance = -1;
		boolean foundStartPoly = false;

		// determine startPoly for distance check
		while (!getStartPoly) {
			Polygon actPoly = polygonContainer.getPolygon(pos);

			if (actPoly.getAllocatedLocation().getId() != locSmallestCritSum) {
				polyID = actPoly.getId();

				// determine location of polyID
				int locationremove = actPoly.getAllocatedLocation().getId();

				// check whether polyID is homePoly
				boolean homepoly = false;
				for (int i = 0; i < numberlocations; i++) {
					Location actLoc = locationContainer.getLocation(i);
					if (actLoc.getHomePolyId() == polyID) {
						homepoly = true;
					}
				}

				System.out.println(polyID + "," + homepoly + "," + lastPolyID);

				if (locationremove != locSmallestCritSum && !homepoly
						&& polyID != lastPolyID) {
					if (checkUnit(polyID, locationremove, locSmallestCritSum,
							numberpolygons)) {
						getStartPoly = true;
						minDistance = polyDistances.distances[polyDistances.ids
								.indexOf(polyID)].get(locSmallestCritSum - 1);
						foundStartPoly = true;
					} else {
						pos++;
					}
				} else {
					pos++;
				}
			} else {
				pos++;
			}

			if (pos >= numberpolygons) {
				getStartPoly = true;
			}
		}

		pos = 0;

		System.out.println("foundStartPoly:" + foundStartPoly);

		if (foundStartPoly) {
			for (int j = 0; j < numberpolygons; j++) {
				Polygon actPoly = polygonContainer.getPolygon(pos);

				if (actPoly.getAllocatedLocation().getId() != locSmallestCritSum) {
					double actdist = polyDistances.distances[polyDistances.ids
							.indexOf(actPoly.getId())]
							.get(locSmallestCritSum - 1);

					int locationremove = actPoly.getAllocatedLocation().getId();
					int buffpolyID = actPoly.getId();

					// check Unit if polyID will be rearranged
					if (actdist < minDistance
							&& buffpolyID != locationContainer.getLocation(
									locationremove - 1).getHomePolyId()) {
						boolean unit = checkUnit(buffpolyID, locationremove,
								locSmallestCritSum, numberpolygons);

						if (unit) {
							minDistance = actdist;
							polyID = buffpolyID;
						}
					}
				}
			}

			int locationremove = -1;
			for (int j = 0; j < numberpolygons; j++) {
				Polygon polyRemove = polygonContainer.getPolygon(j);
				if (polyRemove.getId() == polyID) {
					locationremove = polyRemove.getAllocatedLocation().getId();
				}
			}

			counterIdUsed = 0;
			for (int k = 0; k < rearrangedPolys[0].size(); k++) {
				if (rearrangedPolys[2].get(k).equals(locSmallestCritSum)
						&& rearrangedPolys[1].get(k).equals(locationremove)
						&& rearrangedPolys[0].get(k).equals(polyID)) {
					counterIdUsed++;
				}
			}

			rearrangedPolys[0].add(polyID);
			rearrangedPolys[1].add(locationremove);
			rearrangedPolys[2].add(locSmallestCritSum);

			if (rearrangedPolys[0].size() > (numberlocations * numberlocations)) {
				rearrangedPolys[0].remove(0);
				rearrangedPolys[1].remove(0);
				rearrangedPolys[2].remove(0);
			}

			lastAreaSmallest = locSmallestCritSum;
			lastPolyID = polyID;
			System.out.println("Set to " + (locSmallestCritSum) + " remove "
					+ polyID + " from " + (locationremove + 1));

			Location locGets = locationContainer
					.getLocation(locSmallestCritSum - 1);
			Location locGives = locationContainer
					.getLocation(locationremove - 1);
			Polygon polyMoves = polygonContainer.getPolygonById(numberpolygons,
					polyID);

			locGets.setAllocatedPolygon(polyMoves);
			locGives.removeAllocatedPolygon(polyMoves);
			polyMoves.setAllocatedLocation(locGets);

			changeCriteriaAfterRearrange(polyID, locSmallestCritSum,
					locationremove, numberpolygons);

		} else {
			nofoundSmallest = true;
			nofoundlocsmallest = locSmallestCritSum;
		}
	}

	private static void rearrangePolysWithRandomStart(int numberpolygons,
			int numberlocations, int location, double critAverage,
			boolean microm, boolean PLZ5, int weightCom, int weightCrit)
			throws Exception {

		// check whether area gives or gets a geometry
		boolean givesPoly = false;

		// locBasis = location that gets or gives a geometry
		Location locBasis = null;
		for (int i = 0; i < numberlocations; i++) {
			if (locationContainer.getLocation(i).getId() == location) {
				locBasis = locationContainer.getLocation(i);
			}
		}

		// if (critAverage<locBasis.getCriteria()){
		// givesPoly=true;
		// }

		// create List of all polygones that belong to the location
		List<Polygon> rearrangePoly = new ArrayList<Polygon>();
		for (int i = 0; i < numberpolygons; i++) {
			Polygon actPoly = polygonContainer.getPolygon(i);
			if (actPoly.getAllocatedLocation().getId() == location) {
				rearrangePoly.add(actPoly);
			}
		}

		// determine neighbour polygons
		List<Polygon> neighbourPolys = new ArrayList<Polygon>();

		// polygons of location
		for (int i = 0; i < rearrangePoly.size(); i++) {
			List<Polygon> neighbourIds = rearrangePoly.get(i).getNeighbours();

			// neighbours of one polygon
			for (int j = 0; j < neighbourIds.size(); j++) {

				boolean found = false;
				// compare every neighbour to List of polygones (rearrange
				// polys), whether it is containt in that list (found=true) or
				// not (found=false)
				for (int k = 0; k < rearrangePoly.size(); k++) {
					if (neighbourIds.get(j).getId() == rearrangePoly.get(k)
							.getId()) {
						found = true;
					}
				}

				if (!found) {
					if (!neighbourPolys.contains(neighbourIds.get(j))) {
						neighbourPolys.add(neighbourIds.get(j));
					}
				}
			}
		}

		// StringBuffer sb = new StringBuffer();
		// for (int i=0;i<neighbourPolys.size();i++){
		// sb.append(neighbourPolys.get(i).getId()+",");
		// }
		// System.out.println(sb);

		// remove all neighbours which belong to an area that can't get new
		// polygones
		for (int i = 0; i < neighbourPolys.size(); i++) {
			boolean removed = false;

			for (int j = 0; j < nofoundlocations.size(); j++) {
				if (neighbourPolys.get(i).getAllocatedLocation().getId() == nofoundlocations
						.get(j)) {
					removed = true;
				}
			}

			if (removed) {
				neighbourPolys.remove(i);
				i--;
			}
		}

		// //shrink the neighbours to neighbour areas which have a bigger
		// critsum in case of givesPoly==false, else the other way around
		List<Polygon> neighPolygonsBigger = new ArrayList<Polygon>();
		for (int i = 0; i < neighbourPolys.size(); i++) {
			neighPolygonsBigger.add(neighbourPolys.get(i));
		}

		// check neighbourPolys whether they are hompolys
		List<Polygon> neighPolygonsNotHome = new ArrayList<Polygon>();
		for (int i = 0; i < neighPolygonsBigger.size(); i++) {
			neighPolygonsNotHome.add(neighPolygonsBigger.get(i));
		}

		for (int i = 0; i < numberlocations; i++) {
			for (int j = 0; j < neighbourPolys.size(); j++) {
				if (neighbourPolys.get(j).getId() == locationContainer
						.getLocation(i).getHomePolyId()) {

					for (int k = 0; k < neighPolygonsNotHome.size(); k++) {
						if (neighbourPolys.get(j).getId() == neighPolygonsNotHome
								.get(k).getId()) {
							neighPolygonsNotHome.remove(k);
						}
					}
				}
			}
		}

		// create List of all neighbour Locations
		List<Location> neighbourLocations = new ArrayList<Location>();

		for (int i = 0; i < neighPolygonsNotHome.size(); i++) {
			Location actLoc = neighPolygonsNotHome.get(i)
					.getAllocatedLocation();
			boolean contained = false;
			for (int j = 0; j < neighbourLocations.size(); j++) {
				if (neighbourLocations.get(j).getId() == actLoc.getId()) {
					contained = true;
				}
			}

			if (!contained && !nofoundlocations.contains(actLoc.getId())) {
				neighbourLocations.add(actLoc);
			}
		}

		System.out.println("unitsize:" + neighPolygonsNotHome.size());
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < neighPolygonsNotHome.size(); i++) {
			sb.append(neighPolygonsNotHome.get(i).getId() + ",");
		}
		System.out.println(sb);

		// check unity of polygons
		List<Polygon> neighPolygonsUnit = new ArrayList<Polygon>();
		for (int i = 0; i < neighPolygonsNotHome.size(); i++) {
			neighPolygonsUnit.add(neighPolygonsNotHome.get(i));
		}

		for (int i = 0; i < neighbourLocations.size(); i++) {
			givesPoly = false;

			// determine whether area gives or gets an geometry
			if (locBasis.getCriteria() > neighbourLocations.get(i)
					.getCriteria()) {
				givesPoly = true;
			}

			if (!givesPoly) {
				for (int j = 0; j < neighPolygonsNotHome.size(); j++) {
					boolean unit = false;

					if (neighPolygonsNotHome.get(j).getAllocatedLocation()
							.getId() == neighbourLocations.get(i).getId()) {
						unit = checkUnit(neighPolygonsNotHome.get(j).getId(),
								neighPolygonsNotHome.get(j)
										.getAllocatedLocation().getId(),
								location, numberpolygons);

						if (!unit) {
							for (int k = 0; k < neighPolygonsUnit.size(); k++) {
								if (neighPolygonsNotHome.get(j).getId() == neighPolygonsUnit
										.get(k).getId()) {
									neighPolygonsUnit.remove(k);
								}
							}

						}
					}

				}
			} else {
				// remove all neighbours which belong to the location that gets
				// a new geometry
				for (int k = 0; k < neighPolygonsUnit.size(); k++) {
					if (neighPolygonsUnit.get(k).getAllocatedLocation().getId() == neighbourLocations
							.get(i).getId()) {
						neighPolygonsUnit.remove(k);
						k--;
					}
				}

				// check all geometries of locBasis to determine all that can be
				// given
				for (int j = 0; j < rearrangePoly.size(); j++) {
					boolean unit = false;

					unit = checkUnit(rearrangePoly.get(j).getId(), location,
							neighbourLocations.get(i).getId(), numberpolygons);

					// add all geometries that can be given
					if (unit) {
						if (!neighPolygonsUnit.contains(rearrangePoly.get(j))
								&& rearrangePoly.get(j).getId() != locBasis
										.getHomePolyId()) {
							neighPolygonsUnit.add(rearrangePoly.get(j));
						}
					}
				}
			}

		}

		System.out.println("unitsize after:" + neighPolygonsUnit.size());
		sb = new StringBuffer();
		for (int i = 0; i < neighPolygonsUnit.size(); i++) {
			sb.append(neighPolygonsUnit.get(i).getId() + ",");
		}
		System.out.println(sb);

		if (neighPolygonsUnit.size() > 0) {

			double smallestChange = -1;
			int posSmallestChange = -1;
			givesPoly = false;
			Location locSmallestChange = null;

			// calculate compactness & detect polygon with best change (= best
			// ratio of change of criteria and compactness)
			for (int i = 0; i < neighPolygonsUnit.size(); i++) {
				Polygon actPoly = neighPolygonsUnit.get(i);

				// determine location of actPoly
				Location actLoc = neighPolygonsUnit.get(i)
						.getAllocatedLocation();

				// determine whether area gives or gets an geometry
				if (actLoc.getId() == locBasis.getId()) {
					givesPoly = true;
				}

				double changeValue = -1;

				if (!givesPoly) {
					changeValue = checkChangeofCompactness(actPoly, actLoc,
							location, critAverage, numberpolygons,
							numberlocations, microm, PLZ5, weightCom,
							weightCrit, givesPoly);

					if (i == 0) {
						smallestChange = changeValue;
						posSmallestChange = 0;
						locSmallestChange = actLoc;
					} else {
						if (changeValue < smallestChange) {
							smallestChange = changeValue;
							posSmallestChange = i;
							locSmallestChange = actLoc;
						}
					}
				} else {
					for (int j = 0; j < neighbourLocations.size(); j++) {
						actLoc = neighbourLocations.get(j);

						boolean unit = checkUnit(neighPolygonsUnit.get(i)
								.getId(), location, neighbourLocations.get(j)
								.getId(), numberpolygons);

						if (unit) {
							changeValue = checkChangeofCompactness(actPoly,
									actLoc, location, critAverage,
									numberpolygons, numberlocations, microm,
									PLZ5, weightCom, weightCrit, givesPoly);

							if (i == 0) {
								smallestChange = changeValue;
								posSmallestChange = 0;
								locSmallestChange = actLoc;
							} else {
								if (changeValue < smallestChange) {
									smallestChange = changeValue;
									posSmallestChange = i;
									locSmallestChange = actLoc;
								}
							}
						}
					}
				}
			}

			Polygon polyToChange = neighPolygonsUnit.get(posSmallestChange);

			// determine location of polyToChange
			Location locChange = null;
			locChange = polyToChange.getAllocatedLocation();
			givesPoly = false;

			// determine whether area gives or gets an geometry
			if (locChange.getId() == locBasis.getId()) {
				givesPoly = true;
			}

			if (givesPoly) {
				locChange = locSmallestChange;
			}

			if (location == 10 || location == 8) {
				StringBuffer debugging = new StringBuffer();
				for (int i = 0; i < numberpolygons; i++) {
					Polygon actPoly = polygonContainer.getPolygon(i);
					if (actPoly.getAllocatedLocation().getId() == location) {
						debugging.append(actPoly.getId() + ",");
					}
				}

				System.out.println("locBasis:" + debugging);

				debugging = new StringBuffer();
				for (int i = 0; i < numberpolygons; i++) {
					Polygon actPoly = polygonContainer.getPolygon(i);
					if (actPoly.getAllocatedLocation().getId() == locChange
							.getId()) {
						debugging.append(actPoly.getId() + ",");
					}
				}

				System.out.println("locChange" + debugging);
			}

			// rearrange Poly
			if (!givesPoly) {
				locBasis.getAllocatedPolygon().add(polyToChange);
				polyToChange.setAllocatedLocation(locBasis);

				for (int i = 0; i < locChange.getAllocatedPolygon().size(); i++) {
					if (locChange.getAllocatedPolygon().get(i).getId() == polyToChange
							.getId()) {
						locChange.getAllocatedPolygon().remove(i);
					}
				}

				changeCriteriaAfterRearrange(polyToChange.getId(),
						locBasis.getId(), locChange.getId(), numberpolygons);
			} else {
				locChange.getAllocatedPolygon().add(polyToChange);
				polyToChange.setAllocatedLocation(locChange);

				for (int i = 0; i < locBasis.getAllocatedPolygon().size(); i++) {
					if (locBasis.getAllocatedPolygon().get(i).getId() == polyToChange
							.getId()) {
						locBasis.getAllocatedPolygon().remove(i);
					}
				}

				changeCriteriaAfterRearrange(polyToChange.getId(),
						locChange.getId(), locBasis.getId(), numberpolygons);
			}

			if (location == 10 || location == 8) {
				StringBuffer debugging = new StringBuffer();
				for (int i = 0; i < numberpolygons; i++) {
					Polygon actPoly = polygonContainer.getPolygon(i);
					if (actPoly.getAllocatedLocation().getId() == location) {
						debugging.append(actPoly.getId() + ",");
					}
				}

				System.out.println("locBasis after:" + debugging);

				debugging = new StringBuffer();
				for (int i = 0; i < numberpolygons; i++) {
					Polygon actPoly = polygonContainer.getPolygon(i);
					if (actPoly.getAllocatedLocation().getId() == locChange
							.getId()) {
						debugging.append(actPoly.getId() + ",");
					}
				}

				System.out.println("locChange after" + debugging);
			}
		} else {
			if (!nofoundlocations.contains(locBasis.getId())) {
				nofoundlocations.add(locBasis.getId());
				System.out.println("nofoundlocation added " + locBasis.getId());
				Thread.sleep(5000);
			}
		}

	}

	public static void setLocations(int numberlocations, boolean microm)
			throws IOException {
		locationContainer = initLocationContainer();

		// Input file which needs to be parsed
		String fileToParse = null;
		if (!microm) {
			fileToParse = "E:\\Studium\\Master\\4.Semester - MA\\OSD_Standorte_MC.csv";
		} else {
			fileToParse = "C:\\Users\\s.schmidt@microm-mapchart.com\\Desktop\\Praktikum\\MA\\OSD_Standorte_MC.csv";
		}
		BufferedReader fileReader = null;

		// Delimiter used in CSV file
		final String DELIMITER = ";";
		int pos = 0;

		boolean satisfied = false;
		int i = 0;
		List<Integer> ids = new ArrayList<Integer>();

		// locations small area: 3, 5, 11, 12, 14, 21, 26, 33, 42, 53, 72
		// locations huge area: 11, 41, 55, 68, 72, 79, 82, 90, 92, 96
		ids.add(3); // DD Goldener Reiter
		ids.add(5); // DD Weixdorf
		// ids.add(10); //DD Elbcenter
		ids.add(11); // DD Wilder Mann
		ids.add(12); // DD Cossebaude
		ids.add(14); // DD Löbtau
		ids.add(21); // DD Leubnitz
		ids.add(26); // DD Leuben
		// ids.add(29); //DD Seidnitz
		ids.add(33); // DD Johannstadt
		// ids.add(34); //DD Sparkassenhaus
		// ids.add(39); //DD Weißig
		// ids.add(41); //Radeberg Hauptstra�e
		ids.add(42); // Radeberg
		// ids.add(51); //Kesselsdorf
		ids.add(53); // Possendorf
		// ids.add(54); //Kreischa
		// ids.add(55); //Rabenau
		// ids.add(56); //Tharandt
		// ids.add(60); //Altenberg
		// ids.add(68); //Struppen
		ids.add(72); // DD Heidenau West
		// ids.add(77); //Bergießhübel
		// ids.add(79); //Liebstadt
		// ids.add(82); //Neustadt
		// ids.add(90); //Panschwitz Kuckau
		// ids.add(92); //Schwepnitz
		// ids.add(96); //Hoyerswerda Altstadt

		String line = "";
		// Create the file reader
		fileReader = new BufferedReader(new FileReader(fileToParse));
		line = fileReader.readLine();

		while (!satisfied) {
			line = fileReader.readLine();

			if (line == null) {
				satisfied = true;
			} else {
				// Get all tokens available in line
				String[] tokens = line.split(DELIMITER);

				if (ids.contains(Integer.valueOf(tokens[0]))) {
					i++;
					double lon = Double.parseDouble(tokens[7]);
					double lat = Double.parseDouble(tokens[8]);
					locationContainer.add(i, lon, lat);
					pos = pos + 2;
				}

				if (i == numberlocations) {
					satisfied = true;
				}
			}
		}

		fileReader.close();
	}

	public static void showCritResult(int numberlocations) {
		for (int i = 0; i < numberlocations; i++) {
			System.out.println("Critsize location " + i + " :"
					+ locationContainer.getCriteria(i));
		}
	}

	//
	// /**
	// * Write Polygons into shape, just necessary for testing purposes
	// * @param buffershp: name of the ShapeBuffer
	// * @param shpWriter: name of the ShapeWriter
	// * @param bufferPoly: IDs of Polygons which belong/are allocated to the
	// location
	// * @param geomPoly: Geometries of Polygons which belong/are allocated to
	// the location
	// * @param location
	// * @throws Exception
	// */
	public static void writePolygon(FileWriter output, int numberpolygons)
			throws Exception {
		for (int i = 0; i < numberpolygons; i++) {
			Polygon poly = polygonContainer.getPolygon(i);
			Location loc = polygonContainer.getAllocatedLocation(i);

			if (loc != null) {
				output.append(Objects.toString(poly.getId()));
				output.append(";");
				output.append(Objects.toString(loc.getId()));
				output.append("\n");
			}

		}
	}

	// --------------------------------------------------------------------
	// functions for Greenfield
	// --------------------------------------------------------------------

	public static void allocatePolygonsGreenfield(int numberpolygons,
			int numberlocations, boolean PLZ5) throws SQLException, Exception {

		// detect startPolys on Boundary
		List<Integer> boundaryPolyIds = new ArrayList<Integer>();
		boundaryPolyIds = getBoundaryPolys(PLZ5);

		List<Integer> allocatedPolyIds = new ArrayList<Integer>();
		double critAverage = -1;

		// get sum of Polygons to calculate a critAverage value to stop
		// allocation of polys for one location
		double sumCriteria = getNrOrSum(false, PLZ5, false);
		if (PLZ5) {
			critAverage = sumCriteria / (numberlocations + 2);
		} else {
			critAverage = sumCriteria / (numberlocations);
		}
		int sumOfPolygons = 0;
		double oldCrit = 0;

		// create locations and allocate Polygons to it
		for (int i = 0; i < numberlocations; i++) {

			Location old = null;

			if (i > 0) {
				old = locationContainer.getLocationByID(i);
			}

			double actcrit = 0;

			// getStartPoly
			Polygon startPoly = null;
			if (i == 0) {
				startPoly = polygonContainer.getPolygonById(numberpolygons,
						boundaryPolyIds.get(0));
			} else {
				// detect startPoly, startpoly is a boundary poly neighbourd to
				// last location
				int j = 0;
				boolean found = false;

				while (j < numberpolygons && !found) {
					if (polygonContainer.getPolygon(j)
							.getFlagAllocatedLocation()) {
						if (polygonContainer.getPolygon(j)
								.getAllocatedLocation().getId() == old.getId()) {
							List<Polygon> neighbours = polygonContainer
									.getPolygon(j).getNeighbours();

							for (int k = 0; k < neighbours.size(); k++) {
								for (int l = 0; l < boundaryPolyIds.size(); l++) {
									if (neighbours.get(k).getId() == boundaryPolyIds
											.get(l)
											&& !polygonContainer
													.getPolygonById(
															numberpolygons,
															boundaryPolyIds
																	.get(l))
													.getFlagAllocatedLocation()) {
										found = true;
										startPoly = polygonContainer
												.getPolygonById(numberpolygons,
														boundaryPolyIds.get(l));
									}
								}
							}

							j++;
						} else {
							j++;
						}
					} else {
						j++;
					}
				}
			}

			// if no boundaryPoly is available anymore; a polygon within the
			// whole area will be taken
			if (startPoly == null) {
				int j = 0;
				boolean found = false;

				while (j < numberpolygons && !found) {
					if (polygonContainer.getPolygon(j)
							.getFlagAllocatedLocation()) {
						if (polygonContainer.getPolygon(j)
								.getAllocatedLocation().getId() == old.getId()) {
							List<Polygon> neighbours = polygonContainer
									.getPolygon(j).getNeighbours();

							for (int k = 0; k < neighbours.size(); k++) {
								if (!neighbours.get(k)
										.getFlagAllocatedLocation()) {
									found = true;
									startPoly = polygonContainer
											.getPolygonById(numberpolygons,
													neighbours.get(k).getId());
								}
							}

							if (!found) {
								j++;
							}
						} else {
							j++;
						}
					} else {
						j++;

					}
				}
			}

			// if no neighbour polygone is possible
			if (startPoly == null) {
				if (boundaryPolyIds.size() > 0) {
					for (int j = 0; j < boundaryPolyIds.size(); j++) {
						if (!allocatedPolyIds.contains(boundaryPolyIds.get(j))) {
							startPoly = polygonContainer.getPolygonById(
									numberpolygons, boundaryPolyIds.get(j));
						}
					}
				} else {
					for (int j = 0; j < numberpolygons; j++) {
						if (!allocatedPolyIds.contains(polygonContainer
								.getPolygon(j).getId())) {
							startPoly = polygonContainer.getPolygon(j);
						}
					}
				}
			}

			// set variables for startPoly
			locationContainer.add(i + 1);
			Location loc = locationContainer.getLocationByID(i + 1);
			startPoly.setAllocatedLocation(loc);
			loc.setAllocatedPolygon(startPoly);
			actcrit = startPoly.getCriteria();
			double critThreshold = -1;
			if (i == 0) {
				critThreshold = critAverage;
			} else {
				critThreshold = 2 * critAverage - oldCrit;
				if (critThreshold > critAverage) {
					critThreshold = critAverage;
				}
			}

			// init Distances
			initDistancesToCentroids(numberpolygons, startPoly);

			sumOfPolygons++;
			List<Integer> buffAllocatedPolyIds = new ArrayList<Integer>();
			buffAllocatedPolyIds.add(startPoly.getId());
			if (i == (numberlocations - 1)) {
				critThreshold = critAverage + oldCrit;
			}

			boolean takeNextLoc = false;
			int runs = 0;

			while (actcrit < critThreshold
					&& sumOfPolygons != (numberpolygons - (numberlocations - i))
					&& !takeNextLoc && runs != numberpolygons) {

				// detect Polygon with minimal distance
				Polygon minPoly = null;
				boolean minPolyfound = false;
				for (int k = 0; k < numberpolygons; k++) {
					if (!polygonContainer.getPolygon(k)
							.getFlagAllocatedLocation()) {
						boolean unit = checkUnitCalculationGets(
								polygonContainer.getPolygon(k).getId(),
								loc.getId(), numberpolygons);
						if (unit) {
							minPoly = polygonContainer.getPolygon(k);
							minPolyfound = true;
						}
					}
				}

				if (minPolyfound) {
					for (int k = 0; k < numberpolygons; k++) {
						Polygon actPoly = polygonContainer.getPolygon(k);
						if (!actPoly.getFlagAllocatedLocation()
								&& actPoly.getId() != startPoly.getId()) {
							if (actPoly.getDistance() < minPoly.getDistance()) {
								boolean unit = checkUnitCalculationGets(
										actPoly.getId(), loc.getId(),
										numberpolygons);
								if (unit) {
									minPoly = actPoly;
								}
							}
						}
					}

					// allocate polygon
					loc.setAllocatedPolygon(minPoly);
					minPoly.setAllocatedLocation(loc);
					actcrit = actcrit + minPoly.getCriteria();
					buffAllocatedPolyIds.add(minPoly.getId());
					sumOfPolygons++;
				} else { // if no neighbour polygon is possible anymore
					takeNextLoc = true;
				}
			}

			System.out.println(actcrit);
			for (int j = 0; j < buffAllocatedPolyIds.size(); j++) {
				allocatedPolyIds.add(buffAllocatedPolyIds.get(j));
			}
			loc.setCriteria(actcrit);
			oldCrit = actcrit;
		}
	}

	
	public static String calculateCentroid(String geometry) throws SQLException {
		Connection jdbc = null;
		Statement stmt = null;

		jdbc = getConnection();
		stmt = jdbc.createStatement();

		StringBuffer sb = new StringBuffer();
		sb.append("SELECT ST_AsText(st_centroid(ST_GeomFromText('" + geometry
				+ "')));");
		ResultSet d = stmt.executeQuery(sb.toString());
		d.next();
		String centroid = d.getString(1);

		if (jdbc != null) {
			jdbc.close();
		}

		return centroid;
	}

	public static void calculateGreenfieldLocations(int numberpolygons,
			int numberlocations, boolean PLZ5) throws SQLException {
		Connection jdbc = null;
		Statement stmt = null;

		jdbc = getConnection();
		stmt = jdbc.createStatement();

		String columnIDs = null;
		String tablegeom = null;
		String tablecrit = null;

		// PLZ5
		if (PLZ5) {
			columnIDs = "_g7304";
			tablegeom = "geometriesplz51";
			tablecrit = "criteriasplz51";
		} else {
			// PLZ8
			columnIDs = "_g7305";
			tablegeom = "geometriesplz81";
			tablecrit = "criteriasplz81";
		}

		for (int i = 0; i < numberlocations; i++) {

			StringBuffer sb = new StringBuffer();
			List<Integer> geomIDs = new ArrayList<Integer>();
			for (int j = 0; j < numberpolygons; j++) {
				if (polygonContainer.getPolygon(j).getAllocatedLocation()
						.getId() == locationContainer.getLocation(i).getId()) {
					geomIDs.add(polygonContainer.getPolygon(j).getId());
				}
			}

			StringBuilder idsBuffer = new StringBuilder(geomIDs.toString());
			idsBuffer.deleteCharAt(0);
			idsBuffer.deleteCharAt(idsBuffer.length() - 1);

			sb.append("SELECT ST_AsText(ST_PointOnSurface(ST_UNION(the_geom))) FROM "
					+ tablegeom
					+ " WHERE id IN ("
					+ idsBuffer.toString()
					+ ");");

			ResultSet d = stmt.executeQuery(sb.toString());

			d.next();
			String location = d.getString(1);
			int posBracket = location.indexOf("(");
			int posSpace = location.indexOf(" ");
			String lon = location.substring(posBracket + 1, posSpace);

			posBracket = location.indexOf(")");
			String lat = location.substring(posSpace + 1, posBracket);

			locationContainer.setLonLat(Double.parseDouble(lon),
					Double.parseDouble(lat), i);
		}

		if (jdbc != null) {
			jdbc.close();
		}
	}

	private static void calculateStartLocationsGreenfield(int numberpolygons,
			int numberlocations, boolean PLZ5) throws SQLException {
		Connection jdbc = null;
		Statement stmt = null;

		jdbc = getConnection();
		stmt = jdbc.createStatement();

		String unit = calculateUnit(PLZ5);

		int nrarea = 1;
		StringBuffer sb = new StringBuffer();
		List<String> areas = new ArrayList<String>();
		boolean first = true;

		ArrayList<String>[] splittedGeoms = new ArrayList[numberlocations];
		for (int i = 0; i < numberlocations; i++) {
			splittedGeoms[i] = new ArrayList<String>();
		}

		while (nrarea < numberlocations) {
			String geom = null;
			if (first) {
				geom = unit;
				first = false;
			}

			sb.append("SELECT ST_YMIN(ST_GeomFromText(" + unit + ");");
			ResultSet d = stmt.executeQuery(sb.toString());
			d.next();
			double ymin = d.getDouble(1);

			sb = new StringBuffer();
			sb.append("SELECT ST_YMAX(ST_GeomFromText(" + unit + ");");
			d = stmt.executeQuery(sb.toString());
			d.next();
			double ymax = d.getDouble(1);

			sb.append("SELECT ST_SPLIT(" + geom + ");");
		}

		if (jdbc != null) {
			jdbc.close();
		}

	}

	public static void checkAllocationGreenfield(int numberpolygons,
			int numberlocations, boolean PLZ5, int weightCom, int weightCrit)
			throws SQLException, Exception {

		double sumCriteria = getNrOrSum(false, PLZ5, false);
		double critAverage = sumCriteria / (numberlocations + 1);
		boolean allAllocated = false;
		boolean next = false;
		int i = 0;

		while (!allAllocated) {
			if (!polygonContainer.getPolygon(i).getFlagAllocatedLocation()) {
				Polygon actPoly = polygonContainer.getPolygon(i);

				List<Polygon> neighbours = actPoly.getNeighbours();
				List<Location> neighbourLocs = new ArrayList<Location>();

				// get neighbourlocations
				for (int j = 0; j < neighbours.size(); j++) {
					for (int k = 0; k < numberlocations; k++) {
						if (locationContainer.getLocation(k)
								.getAllocatedPolygon()
								.contains(neighbours.get(j))) {
							if (!neighbourLocs.contains(locationContainer
									.getLocation(k))) {
								neighbourLocs.add(locationContainer
										.getLocation(k));
							}
						}
					}
				}

				// allocate polygon
				double minWeight = -1;
				Location bestLocation = null;

				for (int j = 0; j < neighbourLocs.size(); j++) {

					Location actLoc = neighbourLocs.get(j);

					// calculate change
					double weight = calculateWeightValue(actLoc.getId(),
							actLoc, numberpolygons, false, PLZ5, critAverage,
							weightCom, weightCrit);

					if (j == 0) {
						minWeight = weight;
						bestLocation = actLoc;
					} else {
						if (weight < minWeight) {
							minWeight = weight;
							bestLocation = actLoc;
						}
					}
				}

				if (bestLocation != null) {
					actPoly.setAllocatedLocation(bestLocation);
					bestLocation.setAllocatedPolygon(actPoly);
					double critBefore = bestLocation.getCriteria();
					double critNew = critBefore + actPoly.getCriteria();
					bestLocation.setCriteria(critNew);
				} else {
					next = true;
				}

				if (next) {
					i++;
				}

			} else {
				i++;
			}

			if (i == numberpolygons && !next) {
				allAllocated = true;
			} else if (i == numberpolygons && next) {
				i = 0;
				next = false;
			}
		}
	}

	private static List<Integer> getBoundaryPolys(boolean PLZ5)
			throws SQLException {
		List<Integer> polys = new ArrayList<Integer>();

		Connection jdbc = null;
		Statement stmt = null;

		jdbc = getConnection();
		stmt = jdbc.createStatement();

		String tablegeom = null;

		// PLZ5
		if (PLZ5) {
			tablegeom = "geometriesplz51";
		} else {
			// PLZ8
			tablegeom = "geometriesplz81";
		}

		StringBuffer sb = new StringBuffer();
		sb.append("SELECT id FROM "
				+ tablegeom
				+ " WHERE ST_INTERSECTS((SELECT ST_Boundary(the_geom) FROM (SELECT ST_UNION(the_geom) as the_geom FROM "
				+ tablegeom + ") as p2),the_geom);");
		ResultSet p = stmt.executeQuery(sb.toString());

		boolean last = false;
		while (!last) {
			p.next();
			polys.add(p.getInt(1));
			if (p.isLast()) {
				last = true;
			}
		}

		if (jdbc != null) {
			jdbc.close();
		}

		return polys;
	}

	public static void initCentroids(int numberpolygons) throws SQLException {
		for (int i = 0; i < numberpolygons; i++) {
			String geom = polygonContainer.getPolygon(i).getGeometry();
			String centroid = calculateCentroid(geom);

			System.out.println(centroid);
			int posBracket = centroid.indexOf("(");
			int posSpace = centroid.indexOf(" ");
			double lon = Double.parseDouble(centroid.substring(posBracket + 1,
					posSpace));
			double lat = Double.parseDouble(centroid.substring(posSpace + 1,
					centroid.length() - 1));

			polygonContainer.getPolygon(i).setCentroid(lon, lat);
		}
	}

	private static void initDistancesToCentroids(int numberpolygons,
			Polygon startPoly) {
		for (int i = 0; i < numberpolygons; i++) {
			Polygon actPoly = polygonContainer.getPolygon(i);
			if (!actPoly.getFlagAllocatedLocation()) {
				double phi = Math.acos(Math.sin(startPoly.getCentroid()[1])
						* Math.sin(actPoly.getCentroid()[1])
						+ Math.cos(startPoly.getCentroid()[1])
						* Math.cos(actPoly.getCentroid()[1])
						* Math.cos(actPoly.getCentroid()[0]
								- startPoly.getCentroid()[0]));
				double distance = phi * 6370;
				actPoly.setDistance(distance);
			}
		}
	}
	
	// --------------------------------------------------------------------
	// functions for Whitespot
	// --------------------------------------------------------------------
	
	public static void allocatePolygonsWhitespot(int numberpolygons, int numberGivenlocations, int numberNewLocations, boolean PLZ5) throws Exception, SQLException{
		//Vorgehen: 
		//1. allocate polygons to given locations; nearest distance
		//2. allocate polygons while creating x new locations
		//a. detect boundary polygone: take nearest distance polys that are available
		//b: if no boundary poly exist, take on from the middle
		
		// detect startPolys on Boundary
		List<Integer> boundaryPolyIds = new ArrayList<Integer>();
		boundaryPolyIds = getBoundaryPolys(PLZ5);
				
		double critAverage =-1;
		List<Integer> allocatedPolyIds = new ArrayList<Integer>();
		
		double sumCriteria = getNrOrSum(false, PLZ5, false);
		if (PLZ5) {
			critAverage = sumCriteria / (numberGivenlocations+numberNewLocations + 2);
		} else {
			critAverage = sumCriteria / (numberGivenlocations+numberNewLocations);
		}
		int sumOfPolygons = 0;
		double oldCrit = 0;
		
		for (int i=0;i<(numberGivenlocations+numberNewLocations);i++){
			double critThreshold = -1;
			if (i == 0) {
				critThreshold = critAverage;
			} else {
				critThreshold = 2 * critAverage - oldCrit;
				if (critThreshold > critAverage) {
					critThreshold = critAverage;
				}
			}
			
			Polygon startPoly = null;
			double actcrit = 0;
			
			if (i<numberGivenlocations){
				Location loc = locationContainer.getLocation(i);
				
				//take border Polygon as startPoly
				startPoly = polygonContainer.getPolygonById(numberpolygons, loc.getHomePolyId());
//				
				//take next polygon in list which is not allocated yet
//				for (int j=0;j<numberpolygons;j++){
//					if (!polygonContainer.getPolygon(j).getFlagAllocatedLocation()){
//						startPoly=polygonContainer.getPolygon(j);
//					}
//				}
				
				startPoly.setAllocatedLocation(loc);
				loc.setAllocatedPolygon(startPoly);
				actcrit = startPoly.getCriteria();
				
				// init Distances
				initDistancesToCentroids(numberpolygons, startPoly);

				sumOfPolygons++;
				List<Integer> buffAllocatedPolyIds = new ArrayList<Integer>();
				buffAllocatedPolyIds.add(startPoly.getId());

				boolean takeNextLoc = false;
				int runs = 0;

				while (actcrit < critThreshold
						&& sumOfPolygons != (numberpolygons - ((numberGivenlocations+numberNewLocations)- i))
						&& !takeNextLoc && runs != numberpolygons) {

					// detect Polygon with minimal distance
					Polygon minPoly = null;
					boolean minPolyfound = false;
					for (int k = 0; k < numberpolygons; k++) {
						if (!polygonContainer.getPolygon(k)
								.getFlagAllocatedLocation()) {
							boolean unit = checkUnitCalculationGets(
									polygonContainer.getPolygon(k).getId(),
									loc.getId(), numberpolygons);
							if (unit) {
								minPoly = polygonContainer.getPolygon(k);
								minPolyfound = true;
							}
						}
					}

					if (minPolyfound) {
						for (int k = 0; k < numberpolygons; k++) {
							Polygon actPoly = polygonContainer.getPolygon(k);
							if (!actPoly.getFlagAllocatedLocation()
									&& actPoly.getId() != startPoly.getId()) {
								if (actPoly.getDistance() < minPoly.getDistance()) {
									boolean unit = checkUnitCalculationGets(
											actPoly.getId(), loc.getId(),
											numberpolygons);
									if (unit) {
										minPoly = actPoly;
									}
								}
							}
						}

						// allocate polygon
						loc.setAllocatedPolygon(minPoly);
						minPoly.setAllocatedLocation(loc);
						actcrit = actcrit + minPoly.getCriteria();
						buffAllocatedPolyIds.add(minPoly.getId());
						sumOfPolygons++;
					} else { // if no neighbour polygon is possible anymore
						takeNextLoc = true;
					}
				}

				System.out.println(actcrit);
				for (int j = 0; j < buffAllocatedPolyIds.size(); j++) {
					allocatedPolyIds.add(buffAllocatedPolyIds.get(j));
				}
				loc.setCriteria(actcrit);
				oldCrit = actcrit;
			}
			else{ //create new locations
				Location old = null;

				if (i >= numberGivenlocations) {
					old = locationContainer.getLocationByID(i);
				}

				// getStartPoly
				startPoly = null;

				// detect startPoly, startpoly is a boundary poly 
//				int j = 0;
//				boolean found = false;
//
//				//take next border polygone as startPoly
//				while (j < numberpolygons && !found) {
//					if (!allocatedPolyIds.contains(boundaryPolyIds.get(j))){
//						startPoly = polygonContainer.getPolygonById(numberpolygons, boundaryPolyIds.get(j));
//						found=true;
//					}
//					else{
//						j++;
//					}
//				}
				
				for (int j=0;j<numberpolygons;j++){
					if (!polygonContainer.getPolygon(j).getFlagAllocatedLocation()){
						startPoly=polygonContainer.getPolygon(j);
					}
				}
				
				// if no boundaryPoly is available anymore; a polygon within the
				// whole area will be taken
				if (startPoly == null) {
					int j = 0;
					boolean found = false;

					while (j < numberpolygons && !found) {
						Polygon actPoly = polygonContainer.getPolygon(j);
						if (!actPoly.getFlagAllocatedLocation()){
							startPoly = actPoly;
							found= true;
						}
						else{
							j++;
						}
					}
				}

				// set variables for startPoly
				locationContainer.add(i + 1);
				Location loc = locationContainer.getLocationByID(i + 1);
				startPoly.setAllocatedLocation(loc);
				loc.setAllocatedPolygon(startPoly);
				actcrit = startPoly.getCriteria();
				critThreshold = -1;
				if (i == 0) {
					critThreshold = critAverage;
				} else {
					critThreshold = 2 * critAverage - oldCrit;
					if (critThreshold > critAverage) {
						critThreshold = critAverage;
					}
				}

				// init Distances
				initDistancesToCentroids(numberpolygons, startPoly);

				sumOfPolygons++;
				List<Integer> buffAllocatedPolyIds = new ArrayList<Integer>();
				buffAllocatedPolyIds.add(startPoly.getId());
				if (i == ((numberGivenlocations+numberNewLocations) - 1)) {
					critThreshold = critAverage + oldCrit;
				}

				boolean takeNextLoc = false;
				int runs = 0;

				while (actcrit < critThreshold
						&& sumOfPolygons != (numberpolygons - ((numberGivenlocations+numberNewLocations) - i))
						&& !takeNextLoc && runs != numberpolygons) {

					// detect Polygon with minimal distance
					Polygon minPoly = null;
					boolean minPolyfound = false;
					for (int k = 0; k < numberpolygons; k++) {
						if (!polygonContainer.getPolygon(k)
								.getFlagAllocatedLocation()) {
							boolean unit = checkUnitCalculationGets(
									polygonContainer.getPolygon(k).getId(),
									loc.getId(), numberpolygons);
							if (unit) {
								minPoly = polygonContainer.getPolygon(k);
								minPolyfound = true;
							}
						}
					}

					if (minPolyfound) {
						for (int k = 0; k < numberpolygons; k++) {
							Polygon actPoly = polygonContainer.getPolygon(k);
							if (!actPoly.getFlagAllocatedLocation()
									&& actPoly.getId() != startPoly.getId()) {
								if (actPoly.getDistance() < minPoly.getDistance()) {
									boolean unit = checkUnitCalculationGets(
											actPoly.getId(), loc.getId(),
											numberpolygons);
									if (unit) {
										minPoly = actPoly;
									}
								}
							}
						}

						// allocate polygon
						loc.setAllocatedPolygon(minPoly);
						minPoly.setAllocatedLocation(loc);
						actcrit = actcrit + minPoly.getCriteria();
						buffAllocatedPolyIds.add(minPoly.getId());
						sumOfPolygons++;
					} else { // if no neighbour polygon is possible anymore
						takeNextLoc = true;
					}
				}

				System.out.println(actcrit);
				for (int k = 0; k < buffAllocatedPolyIds.size(); k++) {
					allocatedPolyIds.add(buffAllocatedPolyIds.get(k));
				}
				loc.setCriteria(actcrit);
				oldCrit = actcrit;
				
			}
		}
	}
	
	public static void calculateWhitespotLocations(int numberpolygons, int numberGivenLocations, int numberNewLocations, boolean PLZ5) throws SQLException {
		Connection jdbc = null;
		Statement stmt = null;

		jdbc = getConnection();
		stmt = jdbc.createStatement();

		String columnIDs = null;
		String tablegeom = null;
		String tablecrit = null;

		// PLZ5
		if (PLZ5) {
			columnIDs = "_g7304";
			tablegeom = "geometriesplz51";
			tablecrit = "criteriasplz51";
		} else {
			// PLZ8
			columnIDs = "_g7305";
			tablegeom = "geometriesplz81";
			tablecrit = "criteriasplz81";
		}

		for (int i = numberGivenLocations; i < (numberNewLocations+numberGivenLocations); i++) {

			StringBuffer sb = new StringBuffer();
			List<Integer> geomIDs = new ArrayList<Integer>();
			for (int j = 0; j < numberpolygons; j++) {
				if (polygonContainer.getPolygon(j).getAllocatedLocation()
						.getId() == locationContainer.getLocation(i).getId()) {
					geomIDs.add(polygonContainer.getPolygon(j).getId());
				}
			}

			StringBuilder idsBuffer = new StringBuilder(geomIDs.toString());
			idsBuffer.deleteCharAt(0);
			idsBuffer.deleteCharAt(idsBuffer.length() - 1);

			sb.append("SELECT ST_AsText(ST_PointOnSurface(ST_UNION(the_geom))) FROM "
					+ tablegeom
					+ " WHERE id IN ("
					+ idsBuffer.toString()
					+ ");");

			ResultSet d = stmt.executeQuery(sb.toString());

			d.next();
			String location = d.getString(1);
			int posBracket = location.indexOf("(");
			int posSpace = location.indexOf(" ");
			String lon = location.substring(posBracket + 1, posSpace);

			posBracket = location.indexOf(")");
			String lat = location.substring(posSpace + 1, posBracket);

			locationContainer.setLonLat(Double.parseDouble(lon),
					Double.parseDouble(lat), i);
		}

		if (jdbc != null) {
			jdbc.close();
		}
	}

	
	public static void initStartLocations(int numberlocations, boolean microm)
			throws IOException {
		locationContainer = initLocationContainer();

		// Input file which needs to be parsed
		String fileToParse = null;
		if (!microm) {
			fileToParse = "E:\\Studium\\Master\\4.Semester - MA\\OSD_Standorte_MC.csv";
		} else {
			fileToParse = "C:\\Users\\s.schmidt@microm-mapchart.com\\Desktop\\Praktikum\\MA\\OSD_Standorte_MC.csv";
		}
		BufferedReader fileReader = null;

		// Delimiter used in CSV file
		final String DELIMITER = ";";
		int pos = 0;

		boolean satisfied = false;
		int i = 0;
		List<Integer> ids = new ArrayList<Integer>();

		// locations small area: 3, 5, 11, 12, 14, 21, 26, 33, 42, 53, 72
		// locations huge area: 11, 41, 55, 68, 72, 79, 82, 90, 92, 96
//		ids.add(3); // DD Goldener Reiter
		// ids.add(5); // DD Weixdorf
		// ids.add(10); //DD Elbcenter
		// ids.add(11); // DD Wilder Mann
		// ids.add(12); // DD Cossebaude
		ids.add(14); // DD Löbtau
		//ids.add(21); // DD Leubnitz
		ids.add(26); // DD Leuben
		// ids.add(29); //DD Seidnitz
		ids.add(33); // DD Johannstadt
		// ids.add(34); //DD Sparkassenhaus
		// ids.add(39); //DD Weißig
		// ids.add(41); //Radeberg Hauptstra�e
		ids.add(42); // Radeberg
		// ids.add(51); //Kesselsdorf
		//ids.add(53); // Possendorf
		// ids.add(54); //Kreischa
		// ids.add(55); //Rabenau
		// ids.add(56); //Tharandt
		// ids.add(60); //Altenberg
		// ids.add(68); //Struppen
		ids.add(72); // DD Heidenau West
		// ids.add(77); //Bergießhübel
		// ids.add(79); //Liebstadt
		// ids.add(82); //Neustadt
		// ids.add(90); //Panschwitz Kuckau
		// ids.add(92); //Schwepnitz
		// ids.add(96); //Hoyerswerda Altstadt

		String line = "";
		// Create the file reader
		fileReader = new BufferedReader(new FileReader(fileToParse));
		line = fileReader.readLine();

		while (!satisfied) {
			line = fileReader.readLine();

			if (line == null) {
				satisfied = true;
			} else {
				// Get all tokens available in line
				String[] tokens = line.split(DELIMITER);

				if (ids.contains(Integer.valueOf(tokens[0]))) {
					i++;
					double lon = Double.parseDouble(tokens[7]);
					double lat = Double.parseDouble(tokens[8]);
					locationContainer.add(i, lon, lat);
					pos = pos + 2;
				}

				if (i == numberlocations) {
					satisfied = true;
				}
			}
		}

		fileReader.close();
	}
	
	private static void rearrangePolysWhitespot(int numberpolygons,
			int numberGivenLocations, int numberNewLocations, int location, double critAverage,
			boolean microm, boolean PLZ5, int weightCom, int weightCrit)
			throws Exception {

		int numberlocations = numberGivenLocations+numberNewLocations;
		// check whether area gives or gets a geometry
		boolean givesPoly = false;

		// locBasis = location that gets or gives a geometry
		Location locBasis = null;
		for (int i = 0; i < numberlocations; i++) {
			if (locationContainer.getLocation(i).getId() == location) {
				locBasis = locationContainer.getLocation(i);
			}
		}

		// if (critAverage<locBasis.getCriteria()){
		// givesPoly=true;
		// }

		// create List of all polygones that belong to the location
		List<Polygon> rearrangePoly = new ArrayList<Polygon>();
		for (int i = 0; i < numberpolygons; i++) {
			Polygon actPoly = polygonContainer.getPolygon(i);
			if (actPoly.getAllocatedLocation().getId() == location) {
				rearrangePoly.add(actPoly);
			}
		}

		// determine neighbour polygons
		List<Polygon> neighbourPolys = new ArrayList<Polygon>();

		// polygons of location
		for (int i = 0; i < rearrangePoly.size(); i++) {
			List<Polygon> neighbourIds = rearrangePoly.get(i).getNeighbours();

			// neighbours of one polygon
			for (int j = 0; j < neighbourIds.size(); j++) {

				boolean found = false;
				// compare every neighbour to List of polygones (rearrange
				// polys), whether it is containt in that list (found=true) or
				// not (found=false)
				for (int k = 0; k < rearrangePoly.size(); k++) {
					if (neighbourIds.get(j).getId() == rearrangePoly.get(k)
							.getId()) {
						found = true;
					}
				}

				if (!found) {
					if (!neighbourPolys.contains(neighbourIds.get(j))) {
						neighbourPolys.add(neighbourIds.get(j));
					}
				}
			}
		}

		// StringBuffer sb = new StringBuffer();
		// for (int i=0;i<neighbourPolys.size();i++){
		// sb.append(neighbourPolys.get(i).getId()+",");
		// }
		// System.out.println(sb);

		// remove all neighbours which belong to an area that can't get new
		// polygones
		for (int i = 0; i < neighbourPolys.size(); i++) {
			boolean removed = false;

			for (int j = 0; j < nofoundlocations.size(); j++) {
				if (neighbourPolys.get(i).getAllocatedLocation().getId() == nofoundlocations
						.get(j)) {
					removed = true;
				}
			}

			if (removed) {
				neighbourPolys.remove(i);
				i--;
			}
		}

		// //shrink the neighbours to neighbour areas which have a bigger
		// critsum in case of givesPoly==false, else the other way around
		List<Polygon> neighPolygonsBigger = new ArrayList<Polygon>();
		for (int i = 0; i < neighbourPolys.size(); i++) {
			neighPolygonsBigger.add(neighbourPolys.get(i));
		}

		// check neighbourPolys whether they are hompolys, just for locations that are given
		List<Polygon> neighPolygonsNotHome = new ArrayList<Polygon>();
		for (int i = 0; i < neighPolygonsBigger.size(); i++) {
			neighPolygonsNotHome.add(neighPolygonsBigger.get(i));
		}

		for (int i = 0; i < numberlocations; i++) {
			for (int j = 0; j < neighbourPolys.size(); j++) {
				if (i<numberNewLocations){
					if (neighbourPolys.get(j).getId() == locationContainer
							.getLocation(i).getHomePolyId()) {
	
						for (int k = 0; k < neighPolygonsNotHome.size(); k++) {
							if (neighbourPolys.get(j).getId() == neighPolygonsNotHome
									.get(k).getId()) {
								neighPolygonsNotHome.remove(k);
							}
						}
					}
				}
			}
		}

		// create List of all neighbour Locations
		List<Location> neighbourLocations = new ArrayList<Location>();

		for (int i = 0; i < neighPolygonsNotHome.size(); i++) {
			Location actLoc = neighPolygonsNotHome.get(i)
					.getAllocatedLocation();
			boolean contained = false;
			for (int j = 0; j < neighbourLocations.size(); j++) {
				if (neighbourLocations.get(j).getId() == actLoc.getId()) {
					contained = true;
				}
			}

			if (!contained && !nofoundlocations.contains(actLoc.getId())) {
				neighbourLocations.add(actLoc);
			}
		}

		System.out.println("unitsize:" + neighPolygonsNotHome.size());
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < neighPolygonsNotHome.size(); i++) {
			sb.append(neighPolygonsNotHome.get(i).getId() + ",");
		}
		System.out.println(sb);

		// check unity of polygons
		List<Polygon> neighPolygonsUnit = new ArrayList<Polygon>();
		for (int i = 0; i < neighPolygonsNotHome.size(); i++) {
			neighPolygonsUnit.add(neighPolygonsNotHome.get(i));
		}

		for (int i = 0; i < neighbourLocations.size(); i++) {
			givesPoly = false;

			// determine whether area gives or gets an geometry
			if (locBasis.getCriteria() > neighbourLocations.get(i)
					.getCriteria()) {
				givesPoly = true;
			}

			if (!givesPoly) {
				for (int j = 0; j < neighPolygonsNotHome.size(); j++) {
					boolean unit = false;

					if (neighPolygonsNotHome.get(j).getAllocatedLocation()
							.getId() == neighbourLocations.get(i).getId()) {
						unit = checkUnit(neighPolygonsNotHome.get(j).getId(),
								neighPolygonsNotHome.get(j)
										.getAllocatedLocation().getId(),
								location, numberpolygons);

						if (!unit) {
							for (int k = 0; k < neighPolygonsUnit.size(); k++) {
								if (neighPolygonsNotHome.get(j).getId() == neighPolygonsUnit
										.get(k).getId()) {
									neighPolygonsUnit.remove(k);
								}
							}

						}
					}

				}
			} else {
				// remove all neighbours which belong to the location that gets
				// a new geometry
				for (int k = 0; k < neighPolygonsUnit.size(); k++) {
					if (neighPolygonsUnit.get(k).getAllocatedLocation().getId() == neighbourLocations
							.get(i).getId()) {
						neighPolygonsUnit.remove(k);
						k--;
					}
				}

				// check all geometries of locBasis to determine all that can be
				// given
				for (int j = 0; j < rearrangePoly.size(); j++) {
					boolean unit = false;

					unit = checkUnit(rearrangePoly.get(j).getId(), location,
							neighbourLocations.get(i).getId(), numberpolygons);

					// add all geometries that can be given
					if (unit) {
						if (!neighPolygonsUnit.contains(rearrangePoly.get(j))
								&& rearrangePoly.get(j).getId() != locBasis
										.getHomePolyId()) {
							neighPolygonsUnit.add(rearrangePoly.get(j));
						}
					}
				}
			}

		}

		System.out.println("unitsize after:" + neighPolygonsUnit.size());
		sb = new StringBuffer();
		for (int i = 0; i < neighPolygonsUnit.size(); i++) {
			sb.append(neighPolygonsUnit.get(i).getId() + ",");
		}
		System.out.println(sb);

		if (neighPolygonsUnit.size() > 0) {

			double smallestChange = -1;
			int posSmallestChange = -1;
			givesPoly = false;
			Location locSmallestChange = null;

			// calculate compactness & detect polygon with best change (= best
			// ratio of change of criteria and compactness)
			for (int i = 0; i < neighPolygonsUnit.size(); i++) {
				Polygon actPoly = neighPolygonsUnit.get(i);

				// determine location of actPoly
				Location actLoc = neighPolygonsUnit.get(i)
						.getAllocatedLocation();

				// determine whether area gives or gets an geometry
				if (actLoc.getId() == locBasis.getId()) {
					givesPoly = true;
				}

				double changeValue = -1;

				if (!givesPoly) {
					changeValue = checkChangeofCompactness(actPoly, actLoc,
							location, critAverage, numberpolygons,
							numberlocations, microm, PLZ5, weightCom,
							weightCrit, givesPoly);

					if (i == 0) {
						smallestChange = changeValue;
						posSmallestChange = 0;
						locSmallestChange = actLoc;
					} else {
						if (changeValue < smallestChange) {
							smallestChange = changeValue;
							posSmallestChange = i;
							locSmallestChange = actLoc;
						}
					}
				} else {
					for (int j = 0; j < neighbourLocations.size(); j++) {
						actLoc = neighbourLocations.get(j);

						boolean unit = checkUnit(neighPolygonsUnit.get(i)
								.getId(), location, neighbourLocations.get(j)
								.getId(), numberpolygons);

						if (unit) {
							changeValue = checkChangeofCompactness(actPoly,
									actLoc, location, critAverage,
									numberpolygons, numberlocations, microm,
									PLZ5, weightCom, weightCrit, givesPoly);

							if (i == 0) {
								smallestChange = changeValue;
								posSmallestChange = 0;
								locSmallestChange = actLoc;
							} else {
								if (changeValue < smallestChange) {
									smallestChange = changeValue;
									posSmallestChange = i;
									locSmallestChange = actLoc;
								}
							}
						}
					}
				}
			}

			Polygon polyToChange = neighPolygonsUnit.get(posSmallestChange);

			// determine location of polyToChange
			Location locChange = null;
			locChange = polyToChange.getAllocatedLocation();
			givesPoly = false;

			// determine whether area gives or gets an geometry
			if (locChange.getId() == locBasis.getId()) {
				givesPoly = true;
			}

			if (givesPoly) {
				locChange = locSmallestChange;
			}

			if (location == 10 || location == 8) {
				StringBuffer debugging = new StringBuffer();
				for (int i = 0; i < numberpolygons; i++) {
					Polygon actPoly = polygonContainer.getPolygon(i);
					if (actPoly.getAllocatedLocation().getId() == location) {
						debugging.append(actPoly.getId() + ",");
					}
				}

				System.out.println("locBasis:" + debugging);

				debugging = new StringBuffer();
				for (int i = 0; i < numberpolygons; i++) {
					Polygon actPoly = polygonContainer.getPolygon(i);
					if (actPoly.getAllocatedLocation().getId() == locChange
							.getId()) {
						debugging.append(actPoly.getId() + ",");
					}
				}

				System.out.println("locChange" + debugging);
			}

			// rearrange Poly
			if (!givesPoly) {
				locBasis.getAllocatedPolygon().add(polyToChange);
				polyToChange.setAllocatedLocation(locBasis);

				for (int i = 0; i < locChange.getAllocatedPolygon().size(); i++) {
					if (locChange.getAllocatedPolygon().get(i).getId() == polyToChange
							.getId()) {
						locChange.getAllocatedPolygon().remove(i);
					}
				}

				changeCriteriaAfterRearrange(polyToChange.getId(),
						locBasis.getId(), locChange.getId(), numberpolygons);
			} else {
				locChange.getAllocatedPolygon().add(polyToChange);
				polyToChange.setAllocatedLocation(locChange);

				for (int i = 0; i < locBasis.getAllocatedPolygon().size(); i++) {
					if (locBasis.getAllocatedPolygon().get(i).getId() == polyToChange
							.getId()) {
						locBasis.getAllocatedPolygon().remove(i);
					}
				}

				changeCriteriaAfterRearrange(polyToChange.getId(),
						locChange.getId(), locBasis.getId(), numberpolygons);
			}

			if (location == 10 || location == 8) {
				StringBuffer debugging = new StringBuffer();
				for (int i = 0; i < numberpolygons; i++) {
					Polygon actPoly = polygonContainer.getPolygon(i);
					if (actPoly.getAllocatedLocation().getId() == location) {
						debugging.append(actPoly.getId() + ",");
					}
				}

				System.out.println("locBasis after:" + debugging);

				debugging = new StringBuffer();
				for (int i = 0; i < numberpolygons; i++) {
					Polygon actPoly = polygonContainer.getPolygon(i);
					if (actPoly.getAllocatedLocation().getId() == locChange
							.getId()) {
						debugging.append(actPoly.getId() + ",");
					}
				}

				System.out.println("locChange after" + debugging);
			}
		} else {
			if (!nofoundlocations.contains(locBasis.getId())) {
				nofoundlocations.add(locBasis.getId());
				System.out.println("nofoundlocation added " + locBasis.getId());
				Thread.sleep(5000);
			}
		}

	}
	
	public static void resetAllocations(int numberpolygons, int numberlocations){
		for (int i=0;i<numberpolygons;i++){
			polygonContainer.getPolygon(i).setAllocatedLocation(null);
			polygonContainer.getPolygon(i).setFlagAllocatedLocation(false);
		}
		
		for (int i=0;i<numberlocations;i++){
			locationContainer.getLocation(i).resetAllocatedPolys();
		}
	}
}
