package whitespotGreenfield;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class AreaSegmentation {
	
	public static void main(String[] args)
			throws Exception {
		
		boolean PLZ5=false;
		boolean common=true;
		boolean microm=false;
		
		long time = System.currentTimeMillis();
		int numberlocations=10;
		Functions.setLocations(numberlocations, microm);
		
		//create FileWriter
		FileWriter output =Functions.createFileWriter();
		Functions.createFileWriterLocs(numberlocations);
		
		//init variables
		double[] criteria = new double[numberlocations];
		for (int i=0; i<numberlocations;i++){
			criteria[i]=0;
		}
		
		//calculate number of Polygons in that region
		int numberpolygons=Functions.getNrOrSum(true, PLZ5, microm);
		
		//init polys: get id, geometry, criteria
		Functions.initPolygones(numberpolygons, numberlocations, PLZ5, microm);
		
		//init neighbours
		Functions.initNeighbours(numberpolygons, PLZ5, microm);
		
		//init homePolys
		Functions.determineHomePoly(PLZ5, numberlocations, microm);
		
		//determine distances between poly and locations
		Functions.initDistances(numberpolygons, numberlocations, microm);
		
		//allocate geometries to locations dependent on distance
		Functions.allocatePolygonsByDistance(numberpolygons, numberlocations);
		
		//sum criterias of allocated geometries
		Functions.initCriteria(numberpolygons, numberlocations);
		
		
		int weightCom = 70;
		int weightCrit =30;
		int threshold =50;
		
		//check threshold value
		Functions.checkThreshold(numberpolygons, numberlocations, threshold, microm, PLZ5, weightCom, weightCrit, false, -1, -1);
//		Functions.checkthresholdCombi(numberpolygons, numberlocations);
		
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
