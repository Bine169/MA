package whitespotGreenfield;

import java.io.FileWriter;

public class Whitespot {
	
	public static void main(String[] args)
			throws Exception {
		
		boolean PLZ5=false;
		boolean microm=false;
		
		long time = System.currentTimeMillis();
		
		int numberGivenLocations = 10;
		int numberNewLocations = 1;
		int numberlocations=numberGivenLocations+numberNewLocations;
		int weightCom = 50;
		int weightCrit =50;
		int threshold =50;
		
		//create FileWriter
		FileWriter output =FunctionsCommon.createFileWriter();
		
		//init variables
		double[] criteria = new double[numberlocations];
		for (int i=0; i<numberlocations;i++){
			criteria[i]=0;
		}

		
		//set starting territory centres
		FunctionsCommon.initLocationContainer();
		FunctionsCommon.setLocations(numberGivenLocations, microm);
		
		//determine basic areas that contain territory centres
		FunctionsCommon.determineHomePoly(PLZ5, numberGivenLocations, microm);
		
		int numberpolygons=FunctionsCommon.initialisation(numberlocations, true, PLZ5, microm);
		
		//allocate basic areas
		FunctionsWhitespot.allocatePolygonsWhitespot(numberpolygons, numberGivenLocations, numberNewLocations, PLZ5);
		 
		//check whether all basic areas are allocated
		 FunctionsGreenfieldWhitespot.checkAllocation(numberpolygons, numberlocations, PLZ5, weightCom, weightCrit);
		
		//set territory centre from new created territory
		FunctionsWhitespot.calculateWhitespotLocations(numberpolygons, numberGivenLocations,numberNewLocations, PLZ5);
		 
		//init variables
		criteria = new double[numberlocations];
		for (int i=0; i<numberlocations;i++){
			criteria[i]=0;
		}
		
		//write territory centres
		FunctionsCommon.createFileWriterLocs(numberlocations);
		
		//reset Allocation
		FunctionsGreenfieldWhitespot.resetAllocations(numberpolygons, numberlocations);
		
		weightCom = 100;
		weightCrit =00;
		
		FunctionsCommon.areaSegmentation(numberpolygons, numberlocations, PLZ5, microm, threshold, weightCom, weightCrit, true, numberGivenLocations);
		
		//set final territory centres
		FunctionsWhitespot.calculateWhitespotLocations(numberpolygons, numberGivenLocations,numberNewLocations, PLZ5);
		
		//write territory centres
		FunctionsCommon.createFileWriterLocs(numberlocations);
		
		FunctionsCommon.visualizeResults(numberpolygons, numberlocations, output);
		
		System.out.println("Time for whole algorithm:"+(System.currentTimeMillis()-time)+" ms");

		output.flush();
		output.close();
	    
	    System.out.println("successfully ended");
	}
}
