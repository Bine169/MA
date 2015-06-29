package whitespotGreenfield;

import java.io.FileWriter;

public class greenfield {
	
	public static void main(String[] args)
			throws Exception {
		
		boolean PLZ5=true;
		boolean microm=false;
		
		long time = System.currentTimeMillis();
		
		int numberlocations=10;
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
		//Functions.calculateStartLocationsGreenfield(numberpolygons, numberlocations, PLZ5);
		Functions.initLocationContainer();
//		Functions.setLocations(numberlocations, microm);
		
		//init polys: get id, geometry, criteria
		Functions.initPolygones(numberpolygons, numberlocations, PLZ5, microm);
		
		//init neighbours
		Functions.initNeighbours(numberpolygons, PLZ5, microm);
		
		//init Centroids
		Functions.initCentroids(numberpolygons);
		
		//allocated Polygons
		Functions.allocatePolygonsGreenfield(numberpolygons, numberlocations, PLZ5, microm, weightCom, weightCrit );
		
		//check whether all polygons are allocated
		Functions.checkAllocationGreenfield(numberpolygons, numberlocations, PLZ5, weightCom, weightCrit);
		
		//set Locations
		Functions.calculateGreenfieldLocations(numberpolygons, numberlocations, PLZ5);
		
		//init homePolys
		Functions.determineHomePoly(PLZ5, numberlocations, microm);
		
		weightCom = 10;
		weightCrit =90;
		
//		//check threshold value
		Functions.checkThresholdRandom(numberpolygons, numberlocations, threshold, microm, PLZ5, weightCom, weightCrit);
////		Functions.checkthresholdCombi(numberpolygons, numberlocations);
//		
//		//set endLocations
		Functions.calculateGreenfieldLocations(numberpolygons, numberlocations, PLZ5);
		
		//writeLocations
		Functions.createFileWriterLocs(numberlocations);
		
		//write Polygons
		Functions.writePolygon(output, numberpolygons);
		
		Functions.showCritResult(numberlocations);
		
		System.out.println("Time for whole algorithm:"+(System.currentTimeMillis()-time)+" ms");
//		
		output.flush();
		output.close();
	    
	    System.out.println("successfully ended");
		//welche Funktionen in functionsnew überhaupt noch notwendig? welche variablen?
		
	}
}
