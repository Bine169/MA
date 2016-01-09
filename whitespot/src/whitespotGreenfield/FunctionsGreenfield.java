package whitespotGreenfield;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FunctionsGreenfield {
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
	
	/**allocate basic areas during greenfield algorithm to create initial territory centres
	 * @param numberpolygons: int, number of basic areas
	 * @param numberlocations: int, number of territory centre
	 * @param PLZ5: boolean, indicates database of basic areas
	 * @throws SQLException, Exception
	 */
	public static void allocatePolygonsGreenfield(int numberpolygons,
			int numberlocations, boolean PLZ5) throws SQLException, Exception {

		Getters();
		
		// detect starting basic area on Boundary
		List<Integer> boundaryPolyIds = new ArrayList<Integer>();
		boundaryPolyIds = FunctionsGreenfieldWhitespot.getBoundaryPolys(PLZ5);

		List<Integer> allocatedPolyIds = new ArrayList<Integer>();
		double critAverage = -1;

		// get sum of basic areas to calculate the threshold value to stop
		// allocation of basic areas to a territory centre
		double sumCriteria=FunctionsGreenfieldWhitespot.getCritSum(numberpolygons);
		
		critAverage=FunctionsGreenfieldWhitespot.calculateCritaverage(PLZ5, sumCriteria, numberlocations);
		
		int sumOfPolygons = 0;
		double oldCrit = 0;

		// create territory centre and allocate basic areas to it
		for (int i = 0; i < numberlocations; i++) {

			Location old = null;

			if (i > 0) {
				old = locationContainer.getLocationByID(i);
			}

			double actcrit = 0;

			// get starting basic area
			Polygon startPoly = null;
			if (i == 0) {
				startPoly = polygonContainer.getPolygonById(numberpolygons,
						boundaryPolyIds.get(0));
			} else {
				// detect starting basic area, startpoly is a boundary basic area neighboured to
				// last territory
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

			// if no boundaryPoly is available anymore; a basic area within the
			// whole area will be taken that is neighboured to territory and not yet allocated
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

			// if no neighboured basic area is possible
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

			// init Distances from startPoly to all other basic areas
			FunctionsGreenfieldWhitespot.initDistancesToCentroids(numberpolygons, startPoly);

			sumOfPolygons++;
			List<Integer> buffAllocatedPolyIds = new ArrayList<Integer>();
			buffAllocatedPolyIds.add(startPoly.getId());
			if (i == (numberlocations - 1)) {
				critThreshold = critAverage + oldCrit;
			}

			boolean takeNextLoc = false;
			int runs = 0;

			//allocate basic areas until threshold is reached
			while (actcrit < critThreshold
					&& sumOfPolygons != (numberpolygons - (numberlocations - i))
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

				//allocate basic area, check coherence
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
			for (int j = 0; j < buffAllocatedPolyIds.size(); j++) {
				allocatedPolyIds.add(buffAllocatedPolyIds.get(j));
			}
			loc.setCriteria(actcrit);
			oldCrit = actcrit;
		}
		
		Setters();
	}

	
	/**calculates coordinates of terriory centres
	 * @param numberpolygons: int, number of basic areas
	 * @param numberlocations: int, number of territory centre
	 * @param PLZ5: boolean, indicates database of basic areas
	 * @throws SQLException
	 */
	public static void calculateGreenfieldLocations(int numberpolygons,
			int numberlocations, boolean PLZ5) throws SQLException {

		Getters();
		
		for (int i = 0; i < numberlocations; i++) {
				double[] coordinates = new double[2];
				
				coordinates=FunctionsGreenfieldWhitespot.calculateLocations(numberpolygons, PLZ5, i);

				locationContainer.setLonLat(coordinates[0],
						coordinates[1], i);
		}
		
		Setters();
	}
	
}
