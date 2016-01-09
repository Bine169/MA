package whitespotGreenfield;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class FunctionsWhitespot {

	static LocationContainer locationContainer;
	static PolygonContainer polygonContainer;
	static List<Integer> nofoundlocations = new ArrayList<Integer>();
	
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
	
	/**allocates the polygons for whitespot: 
	 * 1. allocates geometries to given lcoations
	 * 2. create new location and allocates geometries to that
	 * @param numberpolygons: int, number of geometries
	 * @param numberGivenlocations: int, number of given locations
	 * @param numberNewLocations: int number of new locations
	 * @param PLZ5: boolean, indicates database
	 * @throws Exception
	 * @throws SQLException
	 */
	public static void allocatePolygonsWhitespot(int numberpolygons, int numberGivenlocations, int numberNewLocations, boolean PLZ5) throws Exception, SQLException{
		Getters();
		
		//process: 
		//1. allocate basic areas to given territory centres; nearest distance
		//2. allocate basic areas while creating x new territory centres
		//a. detect boundary basic areas: take nearest distance basic area that are available
		//b: if no boundary basic area exist, take on from the middle
		
		// detect starting basic area on Boundary
		List<Integer> boundaryPolyIds = new ArrayList<Integer>();
		boundaryPolyIds = FunctionsGreenfieldWhitespot.getBoundaryPolys(PLZ5);
				
		double critAverage =-1;
		List<Integer> allocatedPolyIds = new ArrayList<Integer>();
		
		//determine threshold
		double sumCriteria=FunctionsGreenfieldWhitespot.getCritSum(numberpolygons);
		int numberlocations=numberGivenlocations+numberNewLocations;
		
		critAverage = FunctionsGreenfieldWhitespot.calculateCritaverage(PLZ5, sumCriteria, numberlocations);
		int sumOfPolygons = 0;
		double oldCrit = 0;
		
		//allocate basic areas to territory centres
		for (int i=0;i<(numberGivenlocations+numberNewLocations);i++){
			
			//init threshold value for break
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
			
			//allocate basic areas to territory centres that are given
			if (i<numberGivenlocations){
				Location loc = locationContainer.getLocation(i);
				
				//take basic area that contain territory centre as start geometry
				startPoly = polygonContainer.getPolygonById(numberpolygons, loc.getHomePolyId());
				
				startPoly.setAllocatedLocation(loc);
				loc.setAllocatedPolygon(startPoly);
				actcrit = startPoly.getCriteria();
				
				// init Distances
				FunctionsGreenfieldWhitespot.initDistancesToCentroids(numberpolygons, startPoly);

				sumOfPolygons++;
				List<Integer> buffAllocatedPolyIds = new ArrayList<Integer>();
				buffAllocatedPolyIds.add(startPoly.getId());

				boolean takeNextLoc = false;
				int runs = 0;

				//allocate basic areas until threshold is reached
				while (actcrit < critThreshold
						&& sumOfPolygons != (numberpolygons - ((numberGivenlocations+numberNewLocations)- i))
						&& !takeNextLoc && runs != numberpolygons) {

					// detect basic area with minimal distance
					Polygon minPoly = null;
					boolean minPolyfound = false;
					for (int k = 0; k < numberpolygons; k++) {
						if (!polygonContainer.getPolygon(k)
								.getFlagAllocatedLocation()) {
							boolean unit = FunctionsCommon.checkUnitCalculationGets(
									polygonContainer.getPolygon(k).getId(),
									loc.getId(), numberpolygons);
							if (unit) {
								minPoly = polygonContainer.getPolygon(k);
								minPolyfound = true;
							}
						}
					}

					//allocate closest basic area, check contiguity of territory
					if (minPolyfound) {
						for (int k = 0; k < numberpolygons; k++) {
							Polygon actPoly = polygonContainer.getPolygon(k);
							if (!actPoly.getFlagAllocatedLocation()
									&& actPoly.getId() != startPoly.getId()) {
								if (actPoly.getDistance() < minPoly.getDistance()) {
									boolean unit = FunctionsCommon.checkUnitCalculationGets(
											actPoly.getId(), loc.getId(),
											numberpolygons);
									if (unit) {
										minPoly = actPoly;
									}
								}
							}
						}

						// allocate basic area
						loc.setAllocatedPolygon(minPoly);
						minPoly.setAllocatedLocation(loc);
						actcrit = actcrit + minPoly.getCriteria();
						buffAllocatedPolyIds.add(minPoly.getId());
						sumOfPolygons++;
					} else { // if no neighbour basic area is possible anymore
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
			//allocate basic area to territory centre that should be created
			else{ 
				

				// get starting basic area
				startPoly = determineStartPoly(numberpolygons);
				
				// if no boundaryPoly is available anymore; a basic area within the
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
				FunctionsGreenfieldWhitespot.initDistancesToCentroids(numberpolygons, startPoly);

				sumOfPolygons++;
				List<Integer> buffAllocatedPolyIds = new ArrayList<Integer>();
				buffAllocatedPolyIds.add(startPoly.getId());
				if (i == ((numberGivenlocations+numberNewLocations) - 1)) {
					critThreshold = critAverage + oldCrit;
				}

				boolean takeNextLoc = false;
				int runs = 0;

				//allocate basic areas until threshold is reached or no basic area is available anymore
				while (actcrit < critThreshold
						&& sumOfPolygons != (numberpolygons - ((numberGivenlocations+numberNewLocations) - i))
						&& !takeNextLoc && runs != numberpolygons) {

					// detect basic area with minimal distance
					Polygon minPoly = null;
					boolean minPolyfound = false;
					for (int k = 0; k < numberpolygons; k++) {
						if (!polygonContainer.getPolygon(k)
								.getFlagAllocatedLocation()) {
							boolean unit = FunctionsCommon.checkUnitCalculationGets(
									polygonContainer.getPolygon(k).getId(),
									loc.getId(), numberpolygons);
							if (unit) {
								minPoly = polygonContainer.getPolygon(k);
								minPolyfound = true;
							}
						}
					}

					//allocate nearest basic area, check coherence of territory 
					if (minPolyfound) {
						for (int k = 0; k < numberpolygons; k++) {
							Polygon actPoly = polygonContainer.getPolygon(k);
							if (!actPoly.getFlagAllocatedLocation()
									&& actPoly.getId() != startPoly.getId()) {
								if (actPoly.getDistance() < minPoly.getDistance()) {
									boolean unit = FunctionsCommon.checkUnitCalculationGets(
											actPoly.getId(), loc.getId(),
											numberpolygons);
									if (unit) {
										minPoly = actPoly;
									}
								}
							}
						}

						// allocate basic area
						loc.setAllocatedPolygon(minPoly);
						minPoly.setAllocatedLocation(loc);
						actcrit = actcrit + minPoly.getCriteria();
						buffAllocatedPolyIds.add(minPoly.getId());
						sumOfPolygons++;
					} else { // if no neighboured basic area is possible anymore
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
		
		Setters();
	}
	
	/**calculates coordinates for new created territory centre
	 * @param numberpolygons: int, number of basic areas
	 * @param numberGivenLocations: int, number of given territory centres
	 * @param numberNewLocations: int, number of new territory centres
	 * @param PLZ5: boolean, indicates database
	 * @throws SQLException
	 */
	public static void calculateWhitespotLocations(int numberpolygons, int numberGivenLocations, int numberNewLocations, boolean PLZ5) throws SQLException {
		Getters();
		

		for (int i = numberGivenLocations; i < (numberNewLocations+numberGivenLocations); i++) {
			double[] coordinates = new double[2];
			
			coordinates=FunctionsGreenfieldWhitespot.calculateLocations(numberpolygons, PLZ5, i);

			locationContainer.setLonLat(coordinates[0],
					coordinates[1], i);
		}
		Setters();
	}
	
	/**calculates basic area which will be used first for allocation
	 * @param numberpolygons: int, number of basic areas
	 * @return basic area
	 */
	private static Polygon determineStartPoly(int numberpolygons){
		Polygon startPoly=null;

		List<Polygon> notAllocatedPolys = new ArrayList<Polygon>();
		
		for (int i=0;i<numberpolygons;i++){
			if (!polygonContainer.getPolygon(i).getFlagAllocatedLocation()){
				notAllocatedPolys.add(polygonContainer.getPolygon(i));
			}
		}
		
		double maxCrit =-1;
		int pos=0;
		
		List<Polygon> polygonsTaken = new ArrayList<Polygon>();
		List<Polygon> polygonsBuff = new ArrayList<Polygon>();
		
		//check basic areas to determine startPoly
		for (int i=0;i<notAllocatedPolys.size();i++){
			Polygon actPoly = notAllocatedPolys.get(i);
			double actCrit = 0;
			
			if (!polygonsTaken.contains(actPoly)){
				
			boolean graphEnds=false;
			List<Polygon> neighbours = new ArrayList<Polygon>();
			List<Polygon> polysTaken = new ArrayList<Polygon>();
			neighbours.add(actPoly);
			
			//while no end of graph is found
			while (!graphEnds) {
				//take basic area
					boolean takeNextNeighbour = false;
					
					if (neighbours.size() > 0) {
						actPoly = neighbours.get(pos);
						if (actPoly == neighbours.get(0)) {
							takeNextNeighbour = true;
						}
					} else {
						takeNextNeighbour = true;
					}

					if (takeNextNeighbour) {
						boolean allreadyTaken = false;
						for (int j = 0; j < polysTaken.size(); j++) {
							if (actPoly== polysTaken.get(j)) {
								allreadyTaken = true;
							}
						}

						if (!allreadyTaken) {
							polysTaken.add(actPoly);
							polygonsTaken.add(actPoly);
							actCrit=actCrit+actPoly.getCriteria();

							for (int j = 0; j < actPoly.getNeighbours().size(); j++) {
								for (int k = 0; k < notAllocatedPolys.size(); k++) {
									if (notAllocatedPolys.get(k).getId() == actPoly
											.getNeighbours().get(j).getId()) {

										if (!neighbours
												.contains(notAllocatedPolys
														.get(k))) {
											neighbours.add(notAllocatedPolys
													.get(k));

										}
									}
								}
							}
						}

						if (neighbours.size() > 0) {
							if (neighbours.get(0)==actPoly){
								neighbours.remove(0);
							}
							pos = 0;
						} else {
							graphEnds = true;
						}

					} else {
						pos++;
					}
			}
			
			if (maxCrit==-1){
				maxCrit=actCrit;
				polygonsBuff=polysTaken;
			}
			else{
				if (actCrit>maxCrit){
					maxCrit=actCrit;
					polygonsBuff=polysTaken;
				}
			}
		}
		}
		
		//determine basic area with highest activity measure
		maxCrit = -1;
		for (int j=0;j<polygonsBuff.size();j++){
				if (maxCrit==-1){
					startPoly=polygonsBuff.get(j);
					maxCrit=startPoly.getCriteria();
				}
				else{
					if (polygonsBuff.get(j).getCriteria()>maxCrit){
						startPoly=polygonsBuff.get(j);
						maxCrit=startPoly.getCriteria();
					}
				}
		}
		
		return startPoly;
	}
}
