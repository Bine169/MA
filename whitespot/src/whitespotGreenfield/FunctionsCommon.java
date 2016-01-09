package whitespotGreenfield;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.io.BufferedReader;
import java.io.FileReader;

public class FunctionsCommon {
	static LocationContainer locationContainer;
	static PolygonContainer polygonContainer;
	static int counterIdUsed = 0;
	static int lastPolyID;
	static List<Integer> nofoundlocations = new ArrayList<Integer>();

	private static class polyDistances {
		static List<Integer> ids = new ArrayList<Integer>();
		static ArrayList<Double>[] distances;
	}
	
	//setter and getter functions
	public static void setPolygonContainer(PolygonContainer polyCon){
		polygonContainer=polyCon;
	}
	
	public static void setLocationContainer(LocationContainer locCon){
		locationContainer=locCon;
	}
	
	public static PolygonContainer getPolygonContainer(){
		return polygonContainer;
	}
	
	public static LocationContainer getLocationContainer(){
		return locationContainer;
	}
	
	//three main functions:initialisation, area segmentation and visulisation --> are used by all three algorithm
	
	/**calls functions for initialisation
	 * @param numberlocations: number of territory centres
	 * @param number: true or false, dependent on decision whether number (true) or sum (false) should be given back calling getNrOrSum
	 * @param PLZ5: true or false to show which data
	 */
	public static int initialisation(int numberlocations, boolean number, boolean PLZ5, boolean microm) throws Exception, SQLException{
		
		int numberpolygons= getNrOrSum(number, PLZ5, microm);
		
		initPolygones(numberpolygons, numberlocations, PLZ5, microm);
		
		initNeighbours(numberpolygons, PLZ5, microm);
		
		initCentroids(numberpolygons);
		
		initArea(numberpolygons, PLZ5, microm);
		
		initCircumferences(numberpolygons, PLZ5, microm);
		
		return numberpolygons;
	}
	
public static void areaSegmentation(int numberpolygons, int numberlocations, boolean PLZ5, boolean microm, int threshold, int weightCom, int weightCrit) throws Exception{
		areaSegmentation(numberpolygons, numberlocations, PLZ5, microm, threshold, weightCom, weightCrit, false, -1);
	}
	
	/** calls all functions that are necessary for area segmentation
	 * @param numberpolygons: number of basic areas in the calculation areas
	 * @param numberlocations: number of territory centres
	 * @param PLZ5: true or false to indicate data
	 * @param threshold: int value of threshold for rearranging to get balanced activity measure
	 * @param weightCom: int value for weight of compactness: used during rearranging
	 * @param weightCrit: int value for weight of criteria: used during rearranging
	 * @param whitespot: true or false
	 * @param numberGivenLocations: just necessary for whitespot
	 * @param numberNewLocations: just necessary for whitespot
	 */
	public static void areaSegmentation(int numberpolygons, int numberlocations, boolean PLZ5, boolean microm, int threshold, int weightCom, int weightCrit, boolean whitespot, int numberGivenLocations) throws Exception{
		determineHomePoly(PLZ5, numberlocations, microm);
		
		initDistances(numberpolygons, numberlocations, microm);
		
		allocatePolygonsByDistance(numberpolygons, numberlocations);
		System.out.println("init by distance done");
		
		initCriteria(numberpolygons, numberlocations);
		System.out.println("init Criteria done");
		 
		System.out.println("check done");
		 
		 System.out.println("starting rearrangement");
		checkThreshold(numberpolygons, numberlocations, threshold, microm, PLZ5, weightCom, weightCrit, whitespot, numberGivenLocations);
		
		
	}
	
	/**calls functions for visualisation of the results
	 * @param numberpolygons: number of basic areas in calculation area
	 * @param numberlocations: number of territory centres
	 * @param output: File for saving output
	 */
	public static void visualizeResults(int numberpolygons, int numberlocations, FileWriter output) throws Exception{
		writePolygon(output, numberpolygons);
		
		showCritResult(numberlocations);
		}

	//----------------Initialisation------------------------
	
	/**calculates the number of basic areas Or the sum of activity measures
	 * @param number: boolean, true if number should be calculated, false for calculation of criteria sum
	 * @param PLZ5: boolean, to indicate area
	 * @return number or sum
	 */
	private static int getNrOrSum(boolean number, boolean PLZ5, boolean microm)
			throws SQLException, ClassNotFoundException {

		//initialise variables
		Connection jdbc = null;
		Statement stmt = null;

		//get Connection
		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		//choose table
		StringBuffer sb = new StringBuffer();
		String table = null;
		if (PLZ5) {
			table = "criteriasplz51";
		} else {
			table = "criteriasplz81";
		}

		//create SQL statement
		if (number) {
			sb.append("SELECT COUNT (id) FROM " + table + ";");
		} else {
			sb.append("SELECT SUM(CAST(_c1 AS int)) FROM " + table + ";");
		}

		//execute Query
		ResultSet t = null;
		if (!microm) {
			t = stmt.executeQuery(sb.toString());
		}

		//save and return result
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
	
	/**initialisation of basic areas getting id, geometrie, activity measure etc from database and store it into local variables
	 * @param numberpolygons: number of basic areas in calculation area
	 * @param numberlocations: number of territory centres
	 * @param PLZ5: boolean to indicate database
	 */
	private static void initPolygones(int numberpolygons, int numberlocations,
			boolean PLZ5, boolean microm) throws SQLException {

		//initialise variables
		Connection jdbc = null;
		Statement stmt = null;

		//create connection
		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		//init database table and columns
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

		//create SQL statement --> get all PolygonIDs, geometries and critvalues and store it
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT t2.id AS id, ST_AsTEXT(the_geom) AS the_geom, _c1 AS criteria FROM "
				+ tablegeom
				+ " AS t1 INNER JOIN "
				+ tablecrit
				+ " AS t2 ON t2." + columnIDs + "=t1.id");
		System.out.println(sb);

		ResultSet t = null;

		//execute query
		if (!microm) {
			t = stmt.executeQuery(sb.toString());
		}

		//init polygonContainer for saving basic areas; contains instances of polygons
		polygonContainer = new PolygonContainer();

		// get ids, geometry and ativity measure
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
	
	/**initialise the neighboured basic areas of every basic area
	 * @param numberpolygons: number of basic areas in calculation area
	 * @param PLZ5: boolean, indicates table
	 * @param microm: indictaes database conenction
	 */
	private static void initNeighbours(int numberpolygons, boolean PLZ5,
			boolean microm) throws SQLException {

		//init variables
		Connection jdbc = null;
		Statement stmt = null;

		//create connection
		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		//init table and collumns
		String tablegeom = null;
		// PLZ5
		if (PLZ5) {
			tablegeom = "geometriesplz51";
		} else {
			// PLZ8
			tablegeom = "geometriesplz81";
		}

		//getting nearest neighbours for every basic areas and store them
		for (int i = 0; i < numberpolygons; i++) {
			System.out.println(i);
			List<Integer> neighbours = new ArrayList<Integer>();
			Polygon poly = polygonContainer.getPolygon(i);
			ResultSet nN = null;
			
			//calculate Nearest neighbours
			if (!microm) {
				nN = getNearestNeighbours(poly.getId(), tablegeom, stmt);
			}

			//store neighbours
			boolean last = false;
			while (!last) {
				nN.next();
				//store just real neighbours, not polygon itself
				if (nN.getInt(1)!=poly.getId()){
					neighbours.add(nN.getInt(1));
				}
				if (nN.isLast()) {
					last = true;
				}
			}

			//get polygon objects from neighbours to store polygone instance and not only id
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

			//set neighbour polygone instances--> List<Polygones>
			poly.setNeighbours(neighbourPolys);
		}

		if (jdbc != null) {
			jdbc.close();
		}
	}
	
	/**creates the SQL statement for calculation of neighbours
	 * @param polyID: id of basic areas for that neighbours should be calculated
	 * @param tablegeom: name of table where geometries are stored
	 * @param jdbc: connection to database
	 * @return ResultSet which contains the neighbours of the basic areas
	 * @throws SQLException
	 * attention: PostGIS function gives also basic areas with polyID as neighbour back
	 */
	private static ResultSet getNearestNeighbours(int polyID, String tablegeom,
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
	
	/**initialise centroid points of given basic areas and store them local
	 * @param numberpolygons: number of basic areas in calculation area
	 * @throws SQLException
	 */
	public static void initCentroids(int numberpolygons) throws SQLException {
		//getting centroid for every basic area and store it
		for (int i = 0; i < numberpolygons; i++) {
			String geom = polygonContainer.getPolygon(i).getGeometry();
			
			//calculate centroid: Point geometry as String is given back
			String centroid = calculateCentroid(geom);

			System.out.println(centroid);
			//parse centroid to store point with Longitude and Latitude
			int posBracket = centroid.indexOf("(");
			int posSpace = centroid.indexOf(" ");
			double lon = Double.parseDouble(centroid.substring(posBracket + 1,
					posSpace));
			double lat = Double.parseDouble(centroid.substring(posSpace + 1,
					centroid.length() - 1));

			//setCentroid
			polygonContainer.getPolygon(i).setCentroid(lon, lat);
		}
	}
	
	/**calculates centroid and gives result back
	 * @param geometry: geometry for which centroid should be calculated
	 * @return centroid point as string
	 * @throws SQLException
	 */
	public static String calculateCentroid(String geometry) throws SQLException {
		//init variables
		Connection jdbc = null;
		Statement stmt = null;

		//get connection
		jdbc = getConnection();
		stmt = jdbc.createStatement();

		//create SQL statement
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT ST_AsText(st_centroid(ST_GeomFromText('" + geometry
				+ "')));");
		
		//execute Query and store result 
		ResultSet d = stmt.executeQuery(sb.toString());
		d.next();
		String centroid = d.getString(1);

		if (jdbc != null) {
			jdbc.close();
		}

		return centroid;
	}
	
	/**calculates areas of each basic area and stores it
	 * @param numberpolygons: number of basic areas in calculation area
	 * @param PLZ5: boolean, indicates table
	 * @param microm: indictaes database conenction
	 * @throws SQLException
	 */
	private static void initArea(int numberpolygons, boolean PLZ5, boolean microm) throws SQLException{
		//init variables
				Connection jdbc = null;
				Statement stmt = null;

				//init connection
				if (!microm) {
					jdbc = getConnection();
					stmt = jdbc.createStatement();
				}

				//set table and columns
				String tablegeom = null;

				// PLZ5
				if (PLZ5) {
					tablegeom = "geometriesplz51";
				} else {
					// PLZ8
					tablegeom = "geometriesplz81";
				}

				//calculate area for each basic area
				for (int i=0;i<numberpolygons;i++){
					Polygon actPoly = polygonContainer.getPolygon(i);
					
					StringBuffer sb = new StringBuffer();
					List<Integer> geomIDs = new ArrayList<Integer>();
					geomIDs.add(actPoly.getId());

					//formate String for SQL statement
					StringBuilder idsBuffer = new StringBuilder(geomIDs.toString());
					idsBuffer.deleteCharAt(0);
					idsBuffer.deleteCharAt(idsBuffer.length() - 1);

					//create SQL statement
					sb = calculateArea(tablegeom, idsBuffer);

					//execute query and store result
					ResultSet d = null;
					if (!microm) {
						d = stmt.executeQuery(sb.toString());
					}

					d.next();

					double result = d.getDouble(1);
					
					actPoly.setArea(result);
				}
				

				if (jdbc != null) {
					jdbc.close();
				}
	}
	
	/**calculates circumference of each basic area and stores it
	 * @param numberpolygons: number of basic areas in calculation area
	 * @param PLZ5: boolean, indicates table
	 * @param microm: indictaes database conenction
	 * @throws SQLException
	 */
	private static void initCircumferences(int numberpolygons, boolean PLZ5, boolean microm) throws SQLException{
		//init variables
		Connection jdbc = null;
		Statement stmt = null;

		//init connection
		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		//set table and columns
		String tablegeom = null;

		// PLZ5
		if (PLZ5) {
			tablegeom = "geometriesplz51";
		} else {
			// PLZ8
			tablegeom = "geometriesplz81";
		}

		//calculate circumference for each basic area
		for (int i=0;i<numberpolygons;i++){
			Polygon actPoly = polygonContainer.getPolygon(i);
			
			StringBuffer sb = new StringBuffer();
			List<Integer> geomIDs = new ArrayList<Integer>();
			geomIDs.add(actPoly.getId());

			//formate String for SQL statement
			StringBuilder idsBuffer = new StringBuilder(geomIDs.toString());
			idsBuffer.deleteCharAt(0);
			idsBuffer.deleteCharAt(idsBuffer.length() - 1);

			//create SQL statement
			sb = calculateCircumference(tablegeom, idsBuffer);
//			sb.append("SELECT ST_Length(ST_CollectionExtract(ST_Intersection(a_geom, b_geom), 2)) ");
//			sb.append("FROM (SELECT (SELECT the_geom from "+tablegeom+" where id="+geomIDs.get(0)+") AS a_geom,");
//			sb.append("(SELECT the_geom from "+tablegeom+" where id="+geomIDs.get(1)+") AS b_geom) f;");

			//execute query and store result
			ResultSet d = null;
			if (!microm) {
				d = stmt.executeQuery(sb.toString());
			}

			d.next();

			double result = d.getDouble(1);
			
			actPoly.setCircumference(result);
		}
		
		//calculate shared edge with neighboured basic areas
		for (int i=0;i<numberpolygons;i++){
			Polygon actPoly=polygonContainer.getPolygon(i);
			
			for (int j=0;j<actPoly.getNeighbours().size();j++){
				Polygon neighbourPoly=actPoly.getNeighbours().get(j);
				
				StringBuffer sb = new StringBuffer();
				List<Integer> geomIDs = new ArrayList<Integer>();
				geomIDs.add(actPoly.getId());
				geomIDs.add(neighbourPoly.getId());

				//formate String for SQL statement
				StringBuilder idsBuffer = new StringBuilder(geomIDs.toString());
				idsBuffer.deleteCharAt(0);
				idsBuffer.deleteCharAt(idsBuffer.length() - 1);

				//create SQL statement
				sb.append("SELECT ST_Length(ST_CollectionExtract(ST_Intersection(a_geom, b_geom), 2)) ");
				sb.append("FROM (SELECT (SELECT the_geom from "+tablegeom+" where id="+geomIDs.get(0)+") AS a_geom,");
				sb.append("(SELECT the_geom from "+tablegeom+" where id="+geomIDs.get(1)+") AS b_geom) f;");
				
				System.out.println(sb);
				ResultSet d = null;
				if (!microm) {
					d = stmt.executeQuery(sb.toString());
				}

				d.next();

				double sharedCircumference=d.getDouble(1);		
				actPoly.setCircumferenceshared(sharedCircumference);
			}
		}
		

		if (jdbc != null) {
			jdbc.close();
		}
	}
	
	//----------------Area Segmentation------------------------
	
	/**determines the ID of the basic area which contains the territory centre
	 * @param PLZ5: boolean to indicate database
	 * @param numberlocations: number of territory centres
	 * @throws SQLException
	 */
	public static void determineHomePoly(boolean PLZ5, int numberlocations,
			boolean microm) throws SQLException {

		//init variables
		Connection jdbc = null;
		Statement stmt = null;

		//init connection
		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		//init database table and columns
		String tablegeom = null;

		// PLZ5
		if (PLZ5) {
			tablegeom = "geometriesplz51";
		} else {
			// PLZ8
			tablegeom = "geometriesplz81";
		}

		int id;
		StringBuffer sb = new StringBuffer();

		//getting basic area id for every territory center
		for (int i = 0; i < numberlocations; i++) {
			System.out.println(i+","+numberlocations);
			Location loc = locationContainer.getLocation(i);
			sb = new StringBuffer();
			
			//create SQL statement
			sb.append("SELECT id FROM " + tablegeom
					+ " WHERE ST_Contains(the_geom,ST_Setsrid(ST_Makepoint("
					+ loc.getLon() + "," + loc.getLat() + "),4326)) LIMIT 1;");
			ResultSet d = null;
			
			//execute Query and store id
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
	
	/**initialise distances from every territory centre to each basic area
	 * @param numberpolygons: number of basic areas in calculation area
	 * @param numberlocations: number of territory centres
	 * @throws SQLException
	 */
	private static void initDistances(int numberpolygons, int numberlocations,
			boolean microm) throws SQLException {
		
		polyDistances.distances = (ArrayList<Double>[]) new ArrayList[numberpolygons];
		for (int i = 0; i < polyDistances.distances.length; i++)
			polyDistances.distances[i] = new ArrayList<Double>();

		//calculate distance for every basic area to each territory centre
		for (int i = 0; i < numberpolygons; i++) {
			System.out.println(i);
			Polygon poly = polygonContainer.getPolygon(i);
			polyDistances.ids.add(poly.getId());

			for (int j = 0; j < numberlocations; j++) {
				double distance = -1;
				if (!microm) {
					distance = initDistancesToCentroid(j, poly);
				}

				polyDistances.distances[i].add(distance);
			}
		}
	}
	
	/**initialise distances from territory centre to centroid of basic area
	 * @param location: int, indicates territory centre
	 * @param actPoly: actual basic area
	 * @return double value of distance
	 */
	private static double initDistancesToCentroid(int location,
			Polygon actPoly) {
		
			Location loc = locationContainer.getLocation(location);
			
			double phi = Math.acos(Math.sin(loc.getLat())
						* Math.sin(actPoly.getCentroid()[1])
						+ Math.cos(loc.getLat())
						* Math.cos(actPoly.getCentroid()[1])
						* Math.cos(actPoly.getCentroid()[0]
								- loc.getLon()));
				double distance = phi * 6370;
			
			return distance;

	}
	
	
	/**allocates basic areas by their distance to the territory centres; every centre gets the nearest basic area
	 * @param numberpolygons: number of geometries in calculation area
	 * @param numberlocations: number of used locations
	 */
	private static void allocatePolygonsByDistance(int numberpolygons,
			int numberlocations) {

		//allocate every basic area to the territory centre which is nearest
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

	/**initialise activity measure, necessary for rearranging
	 * @param numberpolygons: number of basic areas in calculation area
	 * @param numberlocations: number of territory centres
	 */
	private static void initCriteria(int numberpolygons, int numberlocations) {

		//reset activity measure to 0 (necessary for whitespot)
		for (int i = 0; i < numberlocations; i++) {
			locationContainer.setCriteria(i, 0);
		}
		
		//sum activity measures
		for (int i = 0; i < numberpolygons; i++) {
			double crit = polygonContainer.getPolygon(i).getCriteria();
			Location loc = polygonContainer.getAllocatedLocation(i);
			double newcrit = loc.getCriteria() + crit;

			locationContainer.setCriteriaByLoc(loc, newcrit);
		}

		//print values
		for (int i = 0; i < numberlocations; i++) {
			System.out.println(locationContainer.getCriteria(i));
		}
	}
	
	/**checks whether the created territories (created by allocatePolygonsByDistance) are coherent
	 * @param numberpolygons: number of basic areas in calculation area
	 * @param numberlocations: number of territory centres
	 * @throws Exception
	 */
	private static void checkUnityAfterAllocByDist(int numberpolygons, int numberlocations) throws Exception{
		//check unity of territory for centre
		boolean areaNotCoherent =false;
		
		for (int j=0;j<numberlocations;j++){
			
			Location loc = locationContainer.getLocation(j);
			
			//init variables
			boolean unit;
			boolean first=true;
			int pos = 0;
			boolean graphEnds = false;
			
			//saving these neighbours, which can be reached from the actual basic area AND are contained in buffAllocPolysLoc
			List<Integer> neighbours = new ArrayList<Integer>();
			
			//saving basic areas which contain to graph
			List<Polygon> polysTaken = new ArrayList<Polygon>();
			
			//saving basic areas which are not rearranged jet, necessary during rearrangement of other graphs
			List<Polygon> polysNotTaken = new ArrayList<Polygon>();
			
			//saving all geometries which contain to that territory centre
			List<Polygon> buffAllocPolysLoc = new ArrayList<Polygon>();
			
			//add all basi areas which contain to the territory centre
			for (int i = 0; i < numberpolygons; i++) {
				Polygon poly = polygonContainer.getPolygon(i);
				if (poly.getAllocatedLocation().getId() == loc.getId()) {
					buffAllocPolysLoc.add(poly);
				}
			}

			//check unity using graphs
			//approach: if not all basic areas can be reached using relationship of neighbours, the area is not coherent--> several graphs exist
			while (!graphEnds) {
				Polygon actPoly=null;
				
				//check whether it is first basic area: true, start with basic area that contain territory centre to identify subterritory that is nearest to centre (if territory is not coherent the other area will be rearranged)
				if (first){
					//identify basic area which contains centre and use it
					for (int i=0;i<buffAllocPolysLoc.size();i++){
						if (buffAllocPolysLoc.get(i).getId()==loc.getHomePolyId()){
							actPoly=buffAllocPolysLoc.get(i);
							neighbours.add(actPoly.getId());
							first=false;
						}
					}
				}
				else{
					//take next basic area in list
					actPoly = buffAllocPolysLoc.get(pos);
				}

					//check whether actPoly id is the next one in neighbour list; if so, take next neighbour
					boolean takeNextNeighbour = false;
					if (neighbours.size() > 0) {
						if (actPoly.getId() == neighbours.get(0)) {
							takeNextNeighbour = true;

						}
					} else {
						//necessary for start
						takeNextNeighbour = true;
					}

					if (takeNextNeighbour) {
						boolean allreadyTaken = false;
						//check whether the actual basic area is already checked to contain in graph
						for (int i = 0; i < polysTaken.size(); i++) {
							if (actPoly.getId() == polysTaken.get(i).getId()) {
								allreadyTaken = true;
							}
						}

						//if it was not checked yet
						if (!allreadyTaken) {
							polysTaken.add(actPoly);

							//add all neighbours of actual basic area which are contained in buffAllocPolysLoc AND can be reached from actual basic area
							for (int l = 0; l < actPoly.getNeighbours().size(); l++) {
								for (int k = 0; k < buffAllocPolysLoc.size(); k++) {
									
									//check whether neighbour is contained in buffAllocPolysLoc
									if (buffAllocPolysLoc.get(k).getId() == actPoly
											.getNeighbours().get(l).getId()) {

										//check whether id is already stored in neighbours List
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

						//take always first neighbour in list, if neighbour size > 0
						if (neighbours.size() > 0) {
							pos = 0;
							neighbours.remove(0);
						} else {
							//if size==0, no neighbour to check exists anymore
							graphEnds = true;
						}

					} else {
						pos++;
					}
			}

			int countPolysTaken = 0;

			//check whether all basic areas of buffAllocPolysLoc were taken and found as neighbours; if not more than one graph exists
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

			//if territory has more than one part, rearrange the other parts
			if (!unit){
				
				areaNotCoherent=true;
				
				//store all basic areas which contain not to main graph but to other graph(s)
				for (int i=0;i<buffAllocPolysLoc.size();i++){
					if (!polysTaken.contains(buffAllocPolysLoc.get(i))){
						polysNotTaken.add(buffAllocPolysLoc.get(i));
					}
				}
				
				//take every basic area which is aside and rearrange it to another territory
				int posPoly=0;
				while(polysNotTaken.size()>0){
					
					//reset posPoly to 0 if necessary; reason: every basic area is taken and it is tried to rearrange it. Condition is, that it is need to
					//rearranged in that way that the resulting area is coherent. But if it is a geometry from the middle of the graph, this is not given
					//in every case, so just the others around need to be rearranged and after this also the basic area in the middle can be rearranged to another
					//territory. To achieve this its necessary to go through the polysNotTaken once again
					if (posPoly>=polysNotTaken.size()){
						posPoly=0;
					}
					
					//take basic area
					Polygon actPoly=polysNotTaken.get(posPoly);
					
					//init variables
					boolean rearranged=false;
					
					//store all ids from the territory where the basic area can not be rearranged because the resulting territory would not be coherent
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
						//determine territory centre with next smallest Dist
						for (int k=0;k<numberlocations;k++){
							//take centre
							Location actLoc = locationContainer.getLocation(k);
							
							//check whether its the first one (to init minDist) and whether its not contained in notUseableLocs
							if (firstLoc && !notUseableLocs.contains(actLoc.getId())){
								//init variables
								minDist=polyDistances.distances[posActPoly].get(k);
								locMinDist=locationContainer.getLocation(k);
								firstLoc=false;
							}
							else{
								//check Distance and content of notUseableLocs
								if (polyDistances.distances[posActPoly].get(k)<minDist && !notUseableLocs.contains(actLoc.getId())){
									minDist=polyDistances.distances[posActPoly].get(k);
									locMinDist=locationContainer.getLocation(k);
								}
							}
						}
						
						
						//simulate Change to the territory centre which is nearest to basic area
						locMinDist.getAllocatedPolygon().add(actPoly);
						actPoly.setAllocatedLocation(locMinDist);
						loc.removeAllocatedPolygon(actPoly);
	
						//check unity of territory that gets the basic area
						boolean unity = checkUnitCalculationGets(actPoly.getId(), locMinDist.getId(), numberpolygons);
						
						
						//do or abort change
						if (unity){
							System.out.println("set "+actPoly.getId()+" from "+loc.getId()+" to "+locMinDist.getId());
							System.out.println("old: "+loc.getCriteria()+","+locMinDist.getCriteria());
							
							//change activity measures
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
							//abort if resulting territory is not coherent--> reset change
							locMinDist.removeAllocatedPolygon(actPoly);
							actPoly.setAllocatedLocation(loc);
							loc.getAllocatedPolygon().add(actPoly);
							notUseableLocs.add(locMinDist.getId());
						}
						
						//check whether actual basic area can not be rearranged to one location, in this case the if statement is true
						if (notUseableLocs.size()==numberlocations){
							posPoly++;
							rearranged=true;
						}
					}
				}
			}
		}
		
		//check whether at last one territory was not coherent
		if (areaNotCoherent){
			initCriteria(numberpolygons, numberlocations);
		}
	}

	/**checks whether criteria is balanced (threshold is reached) or rearrangement is necessary
	 * @param numberpolygons: number of basic areas in calculation area
	 * @param numberlocations: number of territory centres
	 * @param threshold: int value of threshold which variance between values is allowed
	 * @param PLZ5: boolean, indicates table
	 * @param weightCom: int, weighting value of compactness
	 * @param weightCrit: int, weighting value of criteria
	 * @param whitespot: boolean, whether whitespot or not
	 * @param numberGivenLocations: int, just necessary for whitespot
	 * @param numberNewLocations: int, just necessary for whitespot
	 * @throws SQLException
	 * @throws Exception
	 */
	public static void checkThreshold(int numberpolygons, int numberlocations,
			int threshold, boolean microm, boolean PLZ5,
			int weightCom, int weightCrit, boolean whitespot, int numberGivenLocations) throws SQLException, Exception {

		//init variables
		boolean satisfied = false;

		int run = 0;
		double critAverage = 0;

		double critSum = 0;

		//calculate average of activity measure; every terriotry should have this value in best case
		for (int i = 0; i < numberlocations; i++) {
			critSum = critSum + locationContainer.getCriteria(i);
		}

		critAverage = critSum / numberlocations;

		//init compactness ratio for every territory to make a comparison during rearranging possible
		for (int i = 0; i < numberlocations; i++) {
			initCompactnessRatio(locationContainer.getLocation(i), critAverage,
					numberpolygons, microm, PLZ5, weightCom, weightCrit);
		}

		int location = 0;

		//while variance given by threshold between the activity measures of territories is not satisfied
		while (!satisfied) {
			
			//init variables
			int[] compCriterias = new int[numberlocations];
			int compCrits = 0;
			for (int i = 0; i < numberlocations; i++) {
				compCriterias[i] = 0;
				System.out.println(locationContainer.getLocation(i)
						.getCriteria());
			}

			//calculate difference from average value
			for (int i = 0; i < numberlocations; i++) {
				double value = locationContainer.getCriteria(i) * 100
						/ critAverage;
				double difference = -1;

				if (value > 100) {
					difference = value - 100;
				} else {
					difference = 100 - value;
				}

				//check whether difference is good; count number of territories which are balanced
				if (difference < (threshold)) {
					compCrits++;
				}
			}

				//if not all territories are balanced, start rearranging
				if (compCrits != numberlocations) {

					System.out.println("location" + location);

					// check whether it is necessary to rearrange a basic area in
					// that territory
					// get difference to average value
					
					
					double difference = calculateDifference(location, critAverage);

					System.out.println(difference + "," + critAverage);

					//check whether variance of activity measure is to big --> rearrange
					if (!nofoundlocations.contains((location + 1))
							&& difference > threshold) {

						rearrangePolys(numberpolygons,
								numberlocations, numberGivenLocations, (location + 1), critAverage,
								microm, PLZ5, weightCom, weightCrit, whitespot);
						
					}

					location++;
					//set location to 0 to start check again
					if (location >= numberlocations) {
						location = 0;
					}
				}
			run++;

			//if all activity measures are balanced rearranging will be stopped
			if (compCrits == numberlocations) {
				satisfied = true;
			}

			//break if too much runs for rearranging
			if (nofoundlocations.size() >= numberlocations || run > 5000) {
				satisfied = true;
				System.out.println("Break");
			}

		}

		System.out.println("rearranged with a variance of " + threshold + "%");
		System.out.println("no better arrangement for " + nofoundlocations);
	}
	
	/**initialise the compactness ratio for comparison to change of compactness during rearranging
	 * @param loc: Location, actual territory centre for which compactness ratio should be calculated
	 * @param critAverage: double, value of activity measure that should be reached
	 * @param numberpolygons: int, number of basic areas in calculation area
	 * @param PLZ5: boolean; indicates table
	 * @param weightCom: int, weighting value for compactness
	 * @param weightCrit: int, weighting value for criteria
	 * @throws SQLException
	 */
	private static void initCompactnessRatio(Location loc, double critAverage,
			int numberpolygons, boolean microm, boolean PLZ5, int weightCom,
			int weightCrit) throws SQLException {
		
		//calculate weight and store it
		double weight = calculateWeightValue(loc, numberpolygons,
				microm, PLZ5, critAverage, weightCom, weightCrit);

		loc.setWeightValue(weight);
		System.out.println(loc.getId()+":"+weight);
	}
	
	/**calculates weight value composite by compactness ratio and abberance to average criteria value 
	 * @param actLoc: Location, actual territory centre
	 * @param numberpolygons: int,number of geometries
	 * @param microm: boolean, indicates database connection
	 * @param PLZ5: boolean; indicates table
	 * @param critAverage: double, value of activity measure that should be reached
	 * @param weightComint, weighting value for compactness
	 * @param weightCrit int, weighting value for criteria
	 * @return double value of weight
	 * @throws SQLException
	 */
	public static double calculateWeightValue(Location actLoc,
			int numberpolygons, boolean microm, boolean PLZ5,
			double critAverage, int weightCom, int weightCrit)
			throws SQLException {
		
		//calculate Area and circumference
		double A_area = calculateAreaForWeight(numberpolygons, actLoc.getId());	
		
		double U_area = calculateCircumferenceForWeight(numberpolygons, actLoc.getId());;

		// circumference circle r=U/(2*pi) --> A = pi*r^2 = pi*(U/2*pi)^2
		double A_circle = Math.PI * Math.pow((U_area / (2 * Math.PI)), 2);

		double ratioCom = A_area / A_circle;

		double compactness = 1 - ratioCom;

		// check activity measure
		double ratioCrit = actLoc.getCriteria() / critAverage;
		double criteria = Math.abs(1 - ratioCrit);

		double weight = compactness * weightCom + criteria * weightCrit;

		return weight;
	}
	
	/**calculates area of territory used during calculation of weighting value 
	 * @param numberpolygons: int,number of geometries
	 * @param loc: int, actual territory centre
	 * @return double value of area
	 * @throws SQLException
	 */
	private static double calculateAreaForWeight(int numberpolygons, int loc){
		double area=0;
		
		//sum areas of all basic areas that contain to territory
		for (int i=0;i<numberpolygons;i++){
			if (polygonContainer.getPolygon(i).getFlagAllocatedLocation()) {
				if (polygonContainer.getPolygon(i).getAllocatedLocation()
						.getId() == loc) {
					area=area+polygonContainer.getPolygon(i).getArea();
				}
			}
		}
		
		return area;
	}
	
	/**calculates circumference of territory used during calculation of weighting value 
	 * @param numberpolygons: int,number of geometries
	 * @param loc: int, actual territory centre
	 * @return double value of area
	 * @throws SQLException
	 */
	private static double calculateCircumferenceForWeight(int numberpolygons, int loc){
		double circum=0;
		
		//calculates circumference using all basic areas that belong to the territory
		for (int i=0;i<numberpolygons;i++){
			Polygon actPoly = polygonContainer.getPolygon(i);
			if (actPoly.getFlagAllocatedLocation()){
				if (actPoly.getAllocatedLocation().getId()==loc){
					circum=circum+actPoly.getCircumference();
					for (int j=0;j<actPoly.getNeighbours().size();j++){
						Polygon neighb = actPoly.getNeighbours().get(j);
						if (neighb.getFlagAllocatedLocation()){
							if (neighb.getAllocatedLocation().getId()==loc && actPoly.getId()!=neighb.getId()){
								circum=circum-actPoly.getCircumferenceShared(j);
							}
						}
					}
				}
			}
		}
		
		return circum;
	}
	
	
	/**creates SQL statement for the calculation of the area
	 * @param tablegeom: string, name of table
	 * @param idsBuffer: StringBuilder, string of ids
	 * @return String for SQL statement
	 */
	private static StringBuffer calculateArea(String tablegeom, StringBuilder idsBuffer){
		StringBuffer sb = new StringBuffer();

		sb.append("SELECT ST_AREA(ST_UNION(the_geom)) FROM " + tablegeom
				+ " WHERE id IN (" + idsBuffer.toString() + ");");

		return sb;
	}
	
	/**creates SQL statement for the calculation of the circumference
	 * @param tablegeom: string, name of table
	 * @param idsBuffer: StringBuilder, string of ids
	 * @return String for SQL statement
	 */
	private static StringBuffer calculateCircumference(String tablegeom,StringBuilder idsBuffer) throws SQLException {
		StringBuffer sb = new StringBuffer();
		
		sb.append("SELECT ST_PERIMETER(ST_UNION(the_geom)) FROM " + tablegeom
				+ " WHERE id IN (" + idsBuffer.toString() + ");");
		
		return sb;
	}
	
	/**calculates difference to optimal activity measure value that each territory should reach
	 * @param location: int, indicates territory centre
	 * @param critAverage: double, activity measure that should be reached
	 * @return double value of difference
	 */ 
	static double calculateDifference(int location, double critAverage){
		double value = locationContainer.getCriteria(location)
				* 100 / critAverage;
		double difference = -1;

		if (value > 100) {
			difference = value - 100;
		} else {
			difference = 100 - value;
		}
		
		return difference;
	}
	
	/**rearrange basic areas dependent on best proportion of change of compactness and criteria
	 * @param numberpolygons: int, number of basic areas in calculation area
	 * @param numberlocations: int, number of territory centres
	 * @param location: int, id of territory centre
	 * @param critAverage: double, activity measure that should by reached by each territory
	 * @param PLZ5: boolean, to indicate databse
	 * @param weightCom: int, weight value of compactness
	 * @param weightCrit: int, weight value of criteria
	 * @throws Exception
	 */
	private static void rearrangePolys(int numberpolygons,
			int numberlocations, int numberGivenLocations, int location, double critAverage,
			boolean microm, boolean PLZ5, int weightCom, int weightCrit, boolean whitespot)
			throws Exception {

		// check whether area gives or gets a basic area
		boolean givesPoly = false;

		// locBasis = territory that gets or gives a basic area
		Location locBasis = null;
		for (int i = 0; i < numberlocations; i++) {
			if (locationContainer.getLocation(i).getId() == location) {
				locBasis = locationContainer.getLocation(i);
			}
		}

		// create List of all basic areas that belong to the territory
		List<Polygon> rearrangePoly = new ArrayList<Polygon>();
		for (int i = 0; i < numberpolygons; i++) {
			Polygon actPoly = polygonContainer.getPolygon(i);
			if (actPoly.getAllocatedLocation().getId() == location) {
				rearrangePoly.add(actPoly);
			}
		}

		// determine neighboured territories
				List<Polygon> neighbourPolys = new ArrayList<Polygon>();

				// basic area of territory
				for (int i = 0; i < rearrangePoly.size(); i++) {
					List<Polygon> neighbourIds = rearrangePoly.get(i).getNeighbours();

					// neighbours of one basic area
					for (int j = 0; j < neighbourIds.size(); j++) {

						boolean found = false;
						// compare every neighbour to List of basic areas (rearrange
						// basic areas), whether it is contained in that list (found=true) or
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
				// remove all neighbours which belong to an territory that can't get new
				// basic areas
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

		//shrink the neighbours to neighboured basic area which arent the basic areas that contain the territoy centres
		List<Polygon> neighPolygonsNotHome = new ArrayList<Polygon>();
		for (int i = 0; i < neighbourPolys.size(); i++) {
			neighPolygonsNotHome.add(neighbourPolys.get(i));
		}

		if (!whitespot){
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
		}
		else{
			// check neighbourPolys whether they are "homepolys", just for territory centres that are given
			for (int i = 0; i < numberlocations; i++) {
				for (int j = 0; j < neighbourPolys.size(); j++) {
					if (i<numberGivenLocations){
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
		}

		// create List of all neighboured territories
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

		// check unity of territories
		List<Polygon> neighPolygonsUnit = new ArrayList<Polygon>();
		for (int i = 0; i < neighPolygonsNotHome.size(); i++) {
			neighPolygonsUnit.add(neighPolygonsNotHome.get(i));
		}

		System.out.println("unitsize after:" + neighPolygonsUnit.size());
		sb = new StringBuffer();
		for (int i = 0; i < neighPolygonsUnit.size(); i++) {
			sb.append(neighPolygonsUnit.get(i).getId() + ",");
		}
		System.out.println(sb);

		//if basic areas exist that create coherent territories
		if (neighPolygonsUnit.size() > 0) {

			double smallestChange = -1;
			int posSmallestChange = -1;
			givesPoly = false;
			Location locSmallestChange = null;

			// calculate compactness & detect basic area with best change (= best
			// ratio of change of criteria and compactness)
			for (int i = 0; i < neighPolygonsUnit.size(); i++) {
				Polygon actPoly = neighPolygonsUnit.get(i);

				// determine territory of actPoly
				Location actLoc = neighPolygonsUnit.get(i)
						.getAllocatedLocation();

				// determine whether territory gives or gets an basic area
				if (actLoc.getId() == locBasis.getId()) {
					givesPoly = true;
				}

				double changeValue = -1;

				//calculate change of compactness
				if (!givesPoly) {
					changeValue = FunctionsCommon.checkChangeofCompactness(actPoly, actLoc,
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

			//get basic area with best change of compactness
			Polygon polyToChange = neighPolygonsUnit.get(posSmallestChange);

			// determine territory of polyToChange
			Location locChange = null;
			locChange = polyToChange.getAllocatedLocation();
			givesPoly = false;

			// determine whether territory gives or gets an basic area
			if (locChange.getId() == locBasis.getId()) {
				givesPoly = true;
			}

			if (givesPoly) {
				locChange = locSmallestChange;
			}

			// rearrange basic area
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

			
		} else {
			if (!nofoundlocations.contains(locBasis.getId())) {
				nofoundlocations.add(locBasis.getId());
				System.out.println("nofoundlocation added " + locBasis.getId());
			}
		}

	}

	/**calculates change of compactness and criteria using weighted function
	 * @param actPoly: Polygon, basic area for which the calculation should be done
	 * @param actLoc: Location, territory centre where the basic area belongs to
	 * @param location: int, territory id
	 * @param critAverage: double, value to reach for activity measure  
	 * @param numberpolygons: int, number of basic areas in calculation area
	 * @param numberlocations: int, number of territory centres
	 * @param PLZ5: boolean, to indicate the database
	 * @param weightCom: double, weight value for compactness
	 * @param weightCrit: double, weighting value for criteria
	 * @param givesPoly: boolean, indicates whether territory of actLoc gives or gets a geometry
	 * @return
	 * @throws SQLException
	 */
	public static double checkChangeofCompactness(Polygon actPoly,
			Location actLoc, int location, double critAverage,
			int numberpolygons, int numberlocations, boolean microm,
			boolean PLZ5, int weightCom, int weightCrit, boolean givesPoly)
			throws SQLException {

		double rateCompCritGive = -1;
		double rateCompCritGet = -1;

		int[] locationIDS = new int[2];
		locationIDS[0] = location;
		locationIDS[1] = actLoc.getId();

		// location Basis= territory centre thats gives or gets a basic area
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

		// do calculation for territory that gets the basic area and that gives the
		// basic area
		// taken compactness algorithm: Cox algorithm; ratio of an area of a
		// geometry to the area of a circle with same circumference
		// compactness value should be as closed as possible to 1
		for (int i = 0; i < 2; i++) {
			Location loc =null;
				loc = locationContainer.getLocationByID(locationIDS[i]);
			double weight = calculateWeightValue(loc,
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
	
	/**checks whether both territories are coherent if a basic area will be rearranged
	 * @param polyID: int, id of basic area that should be rearranged
	 * @param locGive: int, id of territory centre that gives the basic area
	 * @param locGet: int, id of territory centre that gets the basic area
	 * @param numberpolygons: int, number of basic area in calculation area
	 * @return boolean, true if both territories are coherent after rearrangement
	 * @throws InterruptedException
	 */
	public static boolean checkUnit(int polyID, int locGive, int locGet,
			int numberpolygons) throws InterruptedException {
		boolean unit = false;

		int numberpolysLocGive=0;
		
		//count number of basic area that belong to the territory centre that gives basic area
		//necessary because it is not allowed to give basic area away, if just 1 basic area exists
		for (int i=0;i<numberpolygons;i++){
			Polygon actPoly = polygonContainer.getPolygon(i);
			if (actPoly.getAllocatedLocation().getId()==locGive){
				numberpolysLocGive++;
			}
		}
		
		boolean unitGives=false;
		boolean unitGets=false;
		
		if (numberpolysLocGive>1){
		//check unity of both territories
			unitGives = checkUnitCalculationGives(polyID, locGive,
				numberpolygons);
			unitGets = checkUnitCalculationGets(polyID, locGet,
				numberpolygons);
		}

		if (unitGets && unitGives) {
			unit = true;
		}

		return unit;
	}
	
	/**checks coherence of that territory that gives the basic area
	 * @param polyID: int, id of basic area that should be rearranged
	 * @param loc: int, id of territory centre that gives the basic area
	 * @param numberpolygons: int, number of basic area in calculation area
	 * @return: boolean, true if resulting area is coherent
	 * @throws InterruptedException
	 */
	private static boolean checkUnitCalculationGives(int polyID, int loc,
			int numberpolygons) throws InterruptedException {
		boolean unit = false;

		// check unit of territory that gives basic area
		// check unit by using graphs
		//idea: if there exist at least two graphs the area is NOT coherent
		//therefore checking whether all basic areas can be reached
		
		//init variables
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

			//while no end of graph is found
			while (!graphEnds) {
				//take basic area
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
							if (neighbours.get(0)==actPoly.getId()){
								neighbours.remove(0);
							}
							pos = 0;
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

		return unit;
	}
	
	/**checks coherence of that territory that gets the basic area
	 * @param polyID: int, basic area that will be rearranged
	 * @param loc: int, id of territory centre that gets the basic area
	 * @param numberpolygons: int, number of basic area in calculation area
	 * @return boolean, true if resulting area is coherent
	 */
	public static boolean checkUnitCalculationGets(int polyID, int loc,
			int numberpolygons) {
		boolean unit = false;

		// get Position of basic area
		Polygon poly = polygonContainer.getPolygonById(numberpolygons, polyID);
		boolean foundNeighbour = false;
		int counter = 0;

		// check neighbours, if every basic area have a neighbour the area is coherent
		while (!foundNeighbour) {
			// take all neighbours of basic area
			for (int j = 0; j < poly.getNeighbours().size(); j++) {
				// take all basic area of territory
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

	/**changes the activity measures of the two territory centres that rearranged a basic area
	 * @param polyID: int, id of basic area that was rearranged
	 * @param location: int, id of territory centre that gets basic area
	 * @param locationMaxCriteria: int, id of territory centre that gives basic area
	 * @param numberpolygons: int, number of basic areas in calculation area
	 * @throws SQLException
	 */
	public static void changeCriteriaAfterRearrange(int polyID, int location,
			int locationMaxCriteria, int numberpolygons) throws SQLException {
		// get activity measure of the given basic area

		Polygon poly = polygonContainer.getPolygonById(numberpolygons, polyID);
		Location locMaxCrit = locationContainer
				.getLocationByID(locationMaxCriteria);
		Location loc = locationContainer.getLocationByID(location);

		double critValue = poly.getCriteria();

		System.out.println("criterias before:" + loc.getCriteria() + ","
				+ locMaxCrit.getCriteria());
		
		//calculate new activity measure
		double locMaxCritValue = locMaxCrit.getCriteria() - critValue;
		double locCritValue = loc.getCriteria() + critValue;

		//store new activity measure
		locationContainer.setCriteriaByLoc(locMaxCrit, locMaxCritValue);
		locationContainer.setCriteriaByLoc(loc, locCritValue);
		
		
		System.out.println("criterias after:" + loc.getCriteria() + ","
				+ locMaxCrit.getCriteria());
		System.out.println("reaarange polygon " + polyID
				+ " with a criteria of " + critValue + " from "
				+ locationMaxCriteria + " to " + location);
	}

	
	//----------------Visualisation------------------------

	// * Write basic areas into file, just necessary for testing purposes
		// * @param FileWriter: outputfile
		// * @param numberpolygons: int, numer of basic areas in calculation area
		// * @throws Exception
		// */
		public static void writePolygon(FileWriter output, int numberpolygons)
				throws Exception {
			//write each basic area into the file
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
		
		/**visualize distribution of activity measure for every territory
		 * @param numberlocations: int, number of territory centres
		 */
		public static void showCritResult(int numberlocations) {
			for (int i = 0; i < numberlocations; i++) {
				System.out.println("Activity measure territory " + (i+1) + " :"
						+ locationContainer.getCriteria(i));
			}
		}
		
	//----------------common functions------------------------
		
		/**Creates FileWriter for saving the results
		 * @return FileWriter
		 * @throws IOException
		 */
	public static FileWriter createFileWriter() throws IOException { 
		FileWriter output = createFileWriter("polygones");
		
		return output;
	}
		
	/**Creates FileWriter for saving the results
	 * @return FileWriter
	 * @throws IOException
	 */
	public static FileWriter createFileWriter(String name) throws IOException { 
		String filename = name + System.currentTimeMillis() + ".csv";
		FileWriter output = new FileWriter(filename);
		output.append(new String("ID,Location"));
		output.append("\n");

		return output;
	}

	/**creates FileWriter for territory centres to visualize them
	 * @param numberlocations: int, number of territory centres
	 * @throws IOException
	 */
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

	
	/**creates connection to database
	 * @return Connection
	 */
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


	/**initialises the LocationContainer for storing the given territory centres
	 * @return LocationContainer
	 */
	public static LocationContainer initLocationContainer() {
		locationContainer = new LocationContainer();
		return locationContainer;
	}
	
	
	/**sets the given territory centres, parsing them from an example file
	 * @param numberlocations: int, number of given territory centres
	 * @param microm: boolean, indicates database
	 * @throws IOException
	 */
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

		ids.add(3); // DD Goldener Reiter
		ids.add(5); // DD Weixdorf
		ids.add(11); // DD Wilder Mann
		ids.add(12); // DD Cossebaude
		ids.add(14); // DD Löbtau
		ids.add(21); // DD Leubnitz
		ids.add(26); // DD Leuben
		ids.add(33); // DD Johannstadt
		ids.add(42); // Radeberg
		ids.add(53); // Possendorf
		ids.add(72); // DD Heidenau West

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
	
}