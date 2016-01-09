package whitespotGreenfield;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class FunctionsGreenfieldWhitespot {
	static LocationContainer locationContainer;
	static PolygonContainer polygonContainer;
	
	public static void getPolygonContainer(){
		polygonContainer = FunctionsCommon.getPolygonContainer();
	}
	
	public static void getLocationContainer (){
		locationContainer=FunctionsCommon.getLocationContainer();
	}
	
	private static void setPolygonContainer(){
		FunctionsCommon.setPolygonContainer(polygonContainer);
	}
	
	private static void setLocationContainer(){
		FunctionsCommon.setLocationContainer(locationContainer);
	}
	
	private static void Getters(){
		getPolygonContainer();
		getLocationContainer();
	}
	
	private static void Setters(){
		setPolygonContainer();
		setLocationContainer();
	}
	
	/**calculates threshold for abording allocation
	 * @param PLZ5: boolean, indicates table of used basic areas
	 * @param sumCriteria: double, sum of activity measures of all basic areas
	 * @param numberlocations: int, number of territory centres
	 */
	public static double calculateCritaverage(boolean PLZ5, double sumCriteria, int numberlocations){
		double critAverage=0;
		
		if (PLZ5) {
			critAverage = sumCriteria / (numberlocations+ 2);
		} else {
			critAverage = sumCriteria / (numberlocations);
		}
		
		return critAverage;
	}
	
	/**calculates territory centre
	 * @param numberpolygons: int, number of basic areas
	 * @param PLZ5: boolean, indicates database of basic areas
	 * @param loc: int, indicates territory centre
	 * @throws SQLException
	 * @return coordinates of territory centres 
	 */
	public static double[] calculateLocations(int numberpolygons, boolean PLZ5, int loc) throws SQLException {
			Getters();
			
			Connection jdbc = null;
			Statement stmt = null;

			jdbc = FunctionsCommon.getConnection();
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
				List<Integer> geomIDs = new ArrayList<Integer>();
				for (int j = 0; j < numberpolygons; j++) {
					if (polygonContainer.getPolygon(j).getAllocatedLocation()
							.getId() == locationContainer.getLocation(loc).getId()) {
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

			if (jdbc != null) {
				jdbc.close();
			}
			
			Setters();
			
			//save coordinates
			double[] coordinates = new double[2];
			coordinates[0]=Double.parseDouble(lon);
			coordinates[1]=Double.parseDouble(lat);
			
			return coordinates;
			
	}
	
	/**determine basic areas at the boundary of the investigation area
	 * @param PLZ5: boolean, indicates database of basic areas
	 * @throws SQLException
	 * @return list of IDs of basic areas
	 */
	public static List<Integer> getBoundaryPolys(boolean PLZ5)
			throws SQLException {
		
		List<Integer> polys = new ArrayList<Integer>();

		Connection jdbc = null;
		Statement stmt = null;

		jdbc = FunctionsCommon.getConnection();
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
	
	/**calculates sum of activity measure of all basic areas
	 * @param numberpolygons: int, number of basic areas
	 * @return double, sum of activity measure 
	 */
	public static double getCritSum(int numberpolygons){
		Getters();
		
		double sumCrit =0;
		for (int i=0;i<numberpolygons;i++){
			sumCrit=sumCrit+polygonContainer.getPolygon(i).getCriteria();
		}
		
		return sumCrit;
	}
	
	/**calculates distance of starting basic area to each other basic area
	 * @param numberpolygons: int, number of basic areas
	 * @param startPoly: basic area that is used as starting geometry
	 */
	public static void initDistancesToCentroids(int numberpolygons,
			Polygon startPoly) {
		
		Getters();
		
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
		
		Setters();
	}
	
	/**check whether all basic areas are allocated to one territory
	 * @param numberpolygons: int, number of basic areas
	 * @param numberlocations: int, number of territory centre
	 * @param PLZ5: boolean, indicates database of basic areas
	 * @param weightCom: int, weighting value of compactness
	 * @param weightCrit: int, weighting value of activity measure
	 * @throws SQLException, Exception
	 */
	public static void checkAllocation(int numberpolygons,
			int numberlocations, boolean PLZ5, int weightCom, int weightCrit)
			throws SQLException, Exception {
		
		Getters();
		
		//calculate sum of activity measures of all basic areas
		double sumCriteria=0;
		for (int i=0;i<numberpolygons;i++){
			sumCriteria=sumCriteria+polygonContainer.getPolygon(i).getCriteria();
		}

		//calculate threshold value
		double critAverage = sumCriteria / (numberlocations + 1);
		boolean allAllocated = false;
		boolean next = false;
		int i = 0;

		//allocate basic areas that are not allocated yet
		while (!allAllocated) {
			//check allocation, do if not allocated
			if (!polygonContainer.getPolygon(i).getFlagAllocatedLocation()) {
				Polygon actPoly = polygonContainer.getPolygon(i);

				List<Polygon> neighbours = actPoly.getNeighbours();
				List<Location> neighbourLocs = new ArrayList<Location>();

				// get neighbourled territories
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

				// allocate basic area
				double minWeight = -1;
				Location bestLocation = null;

				for (int j = 0; j < neighbourLocs.size(); j++) {

					Location actLoc = neighbourLocs.get(j);

					// calculate change
					double weight = FunctionsCommon.calculateWeightValue(actLoc, numberpolygons, false, PLZ5, critAverage,
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
		
		FunctionsCommon.showCritResult(numberlocations);
		Setters();
	}
	
	/**resetting allocations after initial creation of territoris centre to start area segmentation to all territory centres
	 * @param numberpolygons: int, number of basic areas
	 * @param numberlocations: int, number of territory centre
	 */
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
