package whitespotGreenfield;

import java.io.FileWriter;

public class Whitespot {
	
	public static void main(String[] args)
			throws Exception {
		
		boolean PLZ5=false;
		boolean microm=false;
		
		long time = System.currentTimeMillis();
		
		int numberGivenLocations = 5;
		int numberNewLocations = 5;
		int numberlocations=numberGivenLocations+numberNewLocations;
		int weightCom = 100;
		int weightCrit =00;
		int threshold =50;
		
		//create FileWriter
		FileWriter output =Functions.createFileWriter();
		
		//init variables
		double[] criteria = new double[numberlocations];
		for (int i=0; i<numberlocations;i++){
			criteria[i]=0;
		}
		
		//calculate number of Polygons in that region
		int numberpolygons=Functions.getNrOrSum(true, PLZ5, microm);
		
		//set startLocations
		Functions.initLocationContainer();
		Functions.initStartLocations(numberGivenLocations, microm); 
		
		//determine HomePoly of given Locations
		Functions.determineHomePoly(PLZ5, numberGivenLocations, microm);
		
		//init polys: get id, geometry, criteria
		Functions.initPolygones(numberpolygons, numberlocations, PLZ5, microm);
		
		//init neighbours
		Functions.initNeighbours(numberpolygons, PLZ5, microm);
		
		//init Centroids
		Functions.initCentroids(numberpolygons);
		
		//allocated Polygons
		Functions.allocatePolygonsWhitespot(numberpolygons, numberGivenLocations, numberNewLocations, PLZ5);
		
		//check whether all polygons are allocated
		Functions.checkAllocationGreenfield(numberpolygons, numberlocations, PLZ5, weightCom, weightCrit);
		
		//set Locations from new created ones
		Functions.calculateWhitespotLocations(numberpolygons, numberGivenLocations,numberNewLocations, PLZ5);
		
		//init homePolys of all Locations
		Functions.determineHomePoly(PLZ5, numberlocations, microm);
		
		//init variables
		criteria = new double[numberlocations];
		for (int i=0; i<numberlocations;i++){
			criteria[i]=0;
		}
		
		//reset Allocation
		Functions.resetAllocations(numberpolygons, numberlocations);
		
		//determine distances between poly and locations
		Functions.initDistances(numberpolygons, numberlocations, microm);
		
		//allocate geometries to locations dependent on distance
		Functions.allocatePolygonsByDistance(numberpolygons, numberlocations);
		
		weightCom = 100;
		weightCrit =0;
		
		//check threshold value
		Functions.checkThreshold(numberpolygons, numberlocations, threshold, microm, PLZ5, weightCom, weightCrit, true, numberGivenLocations, numberNewLocations);

		//set endLocations
		Functions.calculateWhitespotLocations(numberpolygons, numberGivenLocations,numberNewLocations, PLZ5);
		
		//writeLocations
		Functions.createFileWriterLocs(numberlocations);
		
		//write Polygons
		Functions.writePolygon(output, numberpolygons);
		
		//show Result
		Functions.showCritResult(numberlocations);
		
		System.out.println("Time for whole algorithm:"+(System.currentTimeMillis()-time)+" ms");

		output.flush();
		output.close();
	    
	    System.out.println("successfully ended");
	}
}
