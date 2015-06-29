package whitespotGreenfield;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.AllPermission;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.io.BufferedReader;
import java.io.FileReader;

public class functionsOld {
	static LocationContainer locationContainer;
	static double[] criteriaf;
	static ArrayList<String>[] geomAllocPolys;
	static ArrayList<String>[] buffgeomAllocPolys; 
	static ArrayList<Integer>[] rearrangedPolys = (ArrayList<Integer>[])new ArrayList[3];
	static int counterIdUsed=0;
	static boolean raiseThreshold=false;
	static int lastAreaSmallest;
	static int lastAreaBiggest;
	static int lastPolyID;
	static int numberPolygons;
	static boolean nofoundBiggest=false;
	static boolean nofoundSmallest=false;
	static List<Integer> homePolyIDs;
	static int nofoundlocbiggest=-1;
	static int nofoundlocsmallest=-1;
	static List<Integer> nofoundlocations = new ArrayList<Integer>(); 
	

	public static void setCriteria(double[] crit, int numberlocations){
		criteriaf = new double[numberlocations];
		for (int i=0; i<numberlocations;i++){
			criteriaf[i]=crit[i];
		}
	}
	
	public static double[] getCriteria(){
		return criteriaf;
	}
	
	public static void setnumberPolygons(int number){
		numberPolygons=number;
	}
	
	public static void setGeometryAllocPolys(ArrayList<String>[] geometries){
		geomAllocPolys=geometries;
	}
	
	public static ArrayList<String>[] getGeometryAllocPolys(){
		return geomAllocPolys;
	}
	
	public static void setHomePolys(List<Integer> polyIDs){
		homePolyIDs=polyIDs;
	}
	
	public static double[] setLocations(int numberlocations, boolean PLZ5, boolean common) throws IOException{
		locationContainer = new LocationContainer();
		
		double lonlats[]= new double[numberlocations*2];
		
		//Input file which needs to be parsed
        String fileToParse = "E:\\Studium\\Master\\4.Semester - MA\\OSD_Standorte_MC.csv";
        BufferedReader fileReader = null;
         
        //Delimiter used in CSV file
        final String DELIMITER = ";";
        int pos=0;
			
			if(common){
				boolean satisfied=false;
				int i=0;
				List<Integer> ids = new ArrayList<Integer>();
//				ids.add(5);	//DD Weixdorf
//				ids.add(10); //DD Elbcenter
				ids.add(11); //DD Löbtau
//				ids.add(29);	//DD Seidnitz
//				ids.add(34); //DD Sparkassenhaus
//				ids.add(39);	 //DD Weißig
				ids.add(41); //Radeberg Hauptstr
//				ids.add(51); //Kesselsdorf
//				ids.add(54);	//Kreischa
				ids.add(55); //Rabenau
//				ids.add(56); //Tharandt
//				ids.add(60); //Altenberg
				ids.add(68); //Struppen
				ids.add(72);	//DD Heidenau West
//				ids.add(77);	//Bergießhübel
				ids.add(79);	//Liebstadt
				ids.add(82);	//Neustadt
				ids.add(90); //Panschwitz Kuckau
				ids.add(92); //Schwepnitz
				ids.add(96); //Hoyerswerda Altstadt
				
				String line = "";
	            //Create the file reader
	            fileReader = new BufferedReader(new FileReader(fileToParse));
	            line = fileReader.readLine();
	            
	            while (!satisfied){ 
	            	line = fileReader.readLine();
					
	            	if (line==null){
	            		satisfied=true;
	            	}
	            	else{
						//Get all tokens available in line
		                String[] tokens = line.split(DELIMITER);
						
						if (ids.contains(Integer.valueOf(tokens[0]))){
							i++;
							double lon = Double.parseDouble(tokens[7]);
							double lat = Double.parseDouble(tokens[8]);
							locationContainer.add(i,lon,lat);
							lonlats[pos]=lon;
							lonlats[pos+1]=lat;
							pos=pos+2;
						}
						
						if (i==10){satisfied=true;}
	            	}
				}
			}
			
		fileReader.close();
			
		return lonlats;
	}
	
	//	
//	/**
//	 * calculates the number of polygons which are inside the Area; it is necessary to allocate all polygons
//	 * @return number of polygons
//	 * @throws SQLException
//	 */
	public static int getNrOrSum(boolean number, boolean PLZ5) throws SQLException, ClassNotFoundException{
		Statement jdbc = getConnection();
		StringBuffer sb = new StringBuffer();
		String table=null;
		if (PLZ5){
			table="criteriasplz5";
		}
		else{
			table="criteriasplz8";
		}
		
		if (number)
			{ sb.append("SELECT COUNT (id) FROM "+table+";");
			}
		else
			{ sb.append("SELECT SUM(CAST(_c1 AS int)) FROM criterias");}
		ResultSet t=jdbc.executeQuery(sb.toString());
		t.next();
		int sum=t.getInt(1);
		if (number)
			{System.out.println("numberofpolygons: "+sum);}
		else
			{System.out.println("sum: "+sum);}

		return sum;
	}
//	
//	
//	/**
//	 * Write Polygons into shape, just necessary for testing purposes
//	 * @param buffershp: name of the ShapeBuffer
//	 * @param shpWriter: name of the ShapeWriter
//	 * @param bufferPoly: IDs of Polygons which belong/are allocated to the location
//	 * @param geomPoly: Geometries of Polygons which belong/are allocated to the location
//	 * @param location
//	 * @throws Exception
//	 */
	public static void writePolygon(FileWriter output, List<Integer> bufferPoly, List<String> geomPoly, int location) throws Exception{
	System.out.println("size"+location+" :"+bufferPoly.size()+","+criteriaf[location-1]);
		
	for (int i=0; i<bufferPoly.size();i++){
		output.append(Objects.toString(bufferPoly.get(i)));
		output.append(";");
		output.append(Objects.toString(location));
		output.append("\n");
	}	
	}
	

//	/**
//	 * Calculates the distance of the location to the actual polygon
//	 * @param location
//	 * @param geometry: geometry of the polygon to which the distance should be calculated
//	 * @param jdbc: JDBCConnection
//	 * @return double value of calculated distance
//	 * @throws SQLException
//	 */
	private static double calculateDist(StringBuffer sb, Statement stmt) throws SQLException{
		double distance;
		ResultSet d=stmt.executeQuery(sb.toString());
		d.next();
		distance=d.getDouble(1);
		
		return distance;
	}
	
	public static double calculateNeighbors(String geometrySource, String geometryTarget, Statement stmt) throws SQLException{
		double distance;
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT st_distance(ST_GeomFromText('"+geometrySource+"'),ST_GeomFromText('"+geometryTarget+"'));");
		distance = calculateDist(sb, stmt);
		return distance;
	}
	
	public static double calculateDistance(int poscoords, String geometry, Statement stmt, double[] lonlats) throws SQLException{
		double distance;
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT st_distance(ST_GeomFromText('POINT("+lonlats[poscoords]+" "+lonlats[poscoords+1]+")'),ST_GeomFromText('"+geometry+"'));");
		distance = calculateDist(sb, stmt);
		return distance;
	}
	
	public static double calculateDistanceToCentroid(int poscoords, String geometry, Statement stmt, double[] lonlats) throws SQLException{
		double distance;
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT ST_AsText(st_centroid(ST_GeomFromText('"+geometry+"')));");
		ResultSet d=stmt.executeQuery(sb.toString());
		d.next();
		String centroid=d.getString(1);
		sb = new StringBuffer();
		sb.append("SELECT st_distance(ST_GeomFromText('POINT("+lonlats[poscoords]+" "+lonlats[poscoords+1]+")'),ST_GeomFromText('"+centroid+"'));");
		distance = calculateDist(sb, stmt);
		
		return distance;
	}
	
	public static int determineHomePoly(int poscoords, double[] lonlats, Statement stmt, String tablegeom) throws SQLException{
		int id;
		StringBuffer sb = new StringBuffer();
		//SELECT id FROM geometriesplz5 WHERE ST_Contains(the_geom,st_setsrid(st_makepoint(13.72047,51.09358),4326)) LIMIT 1;
		sb.append("SELECT id FROM "+tablegeom+" WHERE ST_Contains(the_geom,ST_Setsrid(ST_Makepoint("+lonlats[poscoords]+","+lonlats[poscoords+1]+"),4326)) LIMIT 1;");
		System.out.println(sb);
		ResultSet d = stmt.executeQuery(sb.toString());
		d.next();
		id = d.getInt(1);
		return id;
	}
	
	public static ResultSet getNearestNeighbours(int polyID, String tablegeom, Statement jdbc) throws SQLException{
		//SELECT (pgis_fn_nn(p1.the_geom, 0.0005, 1000, 10, 'geometries', 'true', 'id', 'the_geom' )).nn_gid::int FROM (SELECT st_geomfromtext((Select st_astext(the_geom) FROM geometries WHERE ID=1), 4326) AS the_geom) AS p1;
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT (pgis_fn_nn(p1.the_geom, 0.0, 1000, 10, '"+tablegeom+"', 'true', 'id', 'the_geom' )).nn_gid::int FROM (SELECT st_geomfromtext((Select st_astext(the_geom) FROM "+tablegeom+" WHERE ID="+polyID+"), 4326) AS the_geom) AS p1;");
		ResultSet rNeighbours=jdbc.executeQuery(sb.toString());
		
		return rNeighbours;
	}
	
	public static Statement getConnection(){
		Connection jdbc = null;
		Statement stmt = null;
		try {
	         Class.forName("org.postgresql.Driver");
	         jdbc = DriverManager
	            .getConnection("jdbc:postgresql://localhost:5432/MA",
	            "postgres", "");
	      System.out.println("Opened database successfully");
	      stmt = jdbc.createStatement();
		}
	    catch ( Exception e ) {
	          System.err.println( e.getClass().getName()+": "+ e.getMessage() );
	          System.exit(0);
	    }
		
		return stmt;
	}
	
	public static FileWriter createFileWriter() throws IOException{
		String filename = "polygones"+System.currentTimeMillis()+".csv";
		FileWriter output = new FileWriter(filename);
		output.append(new String("ID,Location,geometry"));
		output.append("\n");
		
		return output;
	}
	
	
	public static void createFileWriterLocs(int numberlocations, double[] lonlats) throws IOException{
		FileWriter outputloc = new FileWriter("locations.csv");
		outputloc.append(new String("ID, Long, Lat"));
		outputloc.append("\n");
		
		int coordpos=0;
		for (int i=1; i<numberlocations+1;i++){
			outputloc.append(Objects.toString(i));
			outputloc.append(",");
			outputloc.append(Objects.toString(lonlats[coordpos]));
			outputloc.append(",");
			outputloc.append(Objects.toString(lonlats[coordpos+1]));
			outputloc.append("\n");
			coordpos=coordpos+2;
		}
		outputloc.close();
	}

	public static double[] addToCriteria(int polyID, int location, int locationMaxCriteria, double[] criteria, boolean rearranged, List<Integer> polyIDs, List<Double> polysCriteria) throws SQLException{
		//get criteria of the given polygon
		double critValue = polysCriteria.get(polyIDs.indexOf(polyID));
		
		if (!rearranged){
			criteria[location-1]=criteria[location-1]+critValue;
		}
		else{			
//			System.out.println("criterias before: "+ criteria[locationMaxCriteria-1]+","+criteria[location-1]);
			criteria[locationMaxCriteria-1]=criteria[locationMaxCriteria-1]-critValue;
			criteria[location-1]=criteria[location-1]+critValue;
//			System.out.println("criterias after: "+ criteria[locationMaxCriteria-1]+","+criteria[location-1]);
		}
		
		return criteria;
	}
	
	private static boolean checkUnitCalculationGives(int polyID, int loc, ArrayList<Integer>[] allocPolys, List<Integer> neighbourPolyIds, ArrayList<Integer>[] neighbourNeighbours) throws InterruptedException{
		boolean unit=false;
		
		//simpler to use buffPoly without polyID
		//List<Integer> buffAllocPolysLoc = new ArrayList<Integer>();
//		buffAllocPolysLoc=allocPolys[loc];
		
		//check unit of location that gives geometry
		//check unit if on polygone has no neighbour
		int nrNeighbours=0;
		for (int i=0; i<allocPolys[loc].size();i++){
			if (!allocPolys[loc].get(i).equals(polyID)){
				int actPoly = allocPolys[loc].get(i);
				int getAcctPolyNeigPos = -1;
			
				
				for (int j=0;j<numberPolygons;j++){
					if (neighbourPolyIds.get(j).equals(actPoly)){
						getAcctPolyNeigPos=j;
					}
				}
				
//				System.out.println(polyID+","+ actPoly+","+getAcctPolyNeigPos);
				
				boolean neighbour=false;
				int pos=0;
				
				while (!neighbour){	
					for (int j=0;j<neighbourNeighbours[getAcctPolyNeigPos].size();j++){
						if (neighbourNeighbours[getAcctPolyNeigPos].get(j).equals(allocPolys[loc].get(pos)) && !neighbourNeighbours[getAcctPolyNeigPos].get(j).equals(actPoly) && !allocPolys[loc].get(pos).equals(polyID)){
							neighbour=true;
							nrNeighbours++;
						}
					}	
							
					pos++;
					if (pos>=allocPolys[loc].size()){
						neighbour=true;
					}
				}
			}
		}
		
//		System.out.println(nrNeighbours+","+allocPolys[loc].size());
		//+1 because size is 1 bigger caused by polyID which should be rearranged
		
		if ((nrNeighbours+1)==allocPolys[loc].size()){
			unit=true;
		}
		
//		if (polyID==1 && loc==0){
//			System.out.println("first:"+unit);
//		}
		
		//check unit by using graphs
		if (unit){
			int pos=0;
			boolean graphEnds = false;
			List<Integer> neighbours = new ArrayList<Integer>();
			List<Integer> polysTaken = new ArrayList<Integer>();
			List<Integer> buffAllocPolysLoc = new ArrayList<Integer>();
			for (int i=0;i<allocPolys[loc].size();i++){
				buffAllocPolysLoc.add(allocPolys[loc].get(i));
			}
			buffAllocPolysLoc.remove(Integer.valueOf(polyID));
			
//			if (polyID==1 && loc==0){
//				System.out.println(polyID+","+allocPolys[loc]);
//				System.out.println(polyID+","+buffAllocPolysLoc);
//			}
			
			while (!graphEnds){
				int actPoly = buffAllocPolysLoc.get(pos);
				polysTaken.add(actPoly);
				int getAcctPolyNeigPos=-1;
				
				for (int j=0;j<numberPolygons;j++){
					if (neighbourPolyIds.get(j).equals(actPoly)){
						getAcctPolyNeigPos=j;
					}
				}
				
//				System.out.println(actPoly+","+neighbourNeighbours[getAcctPolyNeigPos]);
				for (int j=0;j<neighbourNeighbours[getAcctPolyNeigPos].size();j++){
					for (int k=0;k<buffAllocPolysLoc.size();k++){
						if (buffAllocPolysLoc.get(k).equals(neighbourNeighbours[getAcctPolyNeigPos].get(j))){
							if (!neighbours.contains(buffAllocPolysLoc.get(k)) && !polysTaken.contains(buffAllocPolysLoc.get(k))){
								neighbours.add(buffAllocPolysLoc.get(k));
							}
						}
					}
				}
				
//				if (polyID==1 && loc==0){
//				System.out.println(neighbours);
//				System.out.println(polysTaken);
////				System.out.println(neighbours.size());
//				}
				
//				Thread.sleep(5000);
				if (neighbours.size()>0){
					pos = buffAllocPolysLoc.indexOf(neighbours.get(0));
					neighbours.remove(0);
				}
				else{
					graphEnds=true;
				}
			}
			
			int countPolysTaken=0;
			
			for (int j=0;j<polysTaken.size();j++){
				for (int k=0; k<buffAllocPolysLoc.size();k++){
					if (buffAllocPolysLoc.get(k).equals(polysTaken.get(j))){
						countPolysTaken++;
					}
				}
			}
			
//			if (polyID==1 && loc==0){
//				System.out.println(countPolysTaken+","+buffAllocPolysLoc.size());
//				}
			
//			System.out.println(polyID+" from "+loc+","+countPolysTaken+","+allocPolys[loc].size());
			if (buffAllocPolysLoc.size()==countPolysTaken){
				unit=true;
			}
			else{
				unit=false;
			}
		}
		
//		if (polyID==1 && loc==0){
//			System.out.println("second:"+unit);
//		}
		
		return unit;
	}
	
	public static boolean checkUnitCalculationGets(int polyID, int loc, ArrayList<Integer>[] allocPolys, List<Integer> neighbourPolyIds, ArrayList<Integer>[] neighbourNeighbours){
		boolean unit=false;
		
		//check unit of location that gets geometry
		
		//get Position of poly
		int getPosPolyIDNeighbours = -1;
		for (int j=0;j<numberPolygons;j++){
			if (neighbourPolyIds.get(j).equals(polyID)){
				getPosPolyIDNeighbours=j;
			}
		}
		
		boolean foundNeighbour=false;
		int counter=0;
		
		//check neighbours
		while(!foundNeighbour){
			//take all neighbours of poly
			for (int j=0; j<neighbourNeighbours[getPosPolyIDNeighbours].size();j++){
				//take all polys of location
				for (int k=0; k<allocPolys[loc].size();k++){
					if (neighbourNeighbours[getPosPolyIDNeighbours].get(j).equals(allocPolys[loc].get(k))){
						unit=true;
						foundNeighbour=true;
					}
				}
				counter++;
			}
			
			if (counter==neighbourNeighbours[getPosPolyIDNeighbours].size() && !foundNeighbour){
				foundNeighbour=true;
			}
		}
		
		return unit;
	}
	
	
	public static boolean checkUnit(int polyID, int locGive, int locGet, ArrayList<Integer>[] allocPolys, List<Integer> neighbourPolyIds, ArrayList<Integer>[] neighbourNeighbours) throws InterruptedException{
		boolean unit=false;
		
		boolean unitGives= checkUnitCalculationGives(polyID, locGive, allocPolys, neighbourPolyIds, neighbourNeighbours);
		boolean unitGets = checkUnitCalculationGets(polyID, locGet, allocPolys, neighbourPolyIds, neighbourNeighbours);
		
		if (unitGets && unitGives){
			unit=true;
		}
		
//		System.out.println(polyID+","+unit+" from "+locGive+" to "+locGet);
		return unit; 
	}
	
	public static ArrayList<Integer>[] rearrangeFromBiggest(int numberlocations, ArrayList<Integer>[] allocPolys, List<Integer> polyDistanceIds, ArrayList<Double>[] polyDistances, List<Integer> polyIDs, List<Double> polysCriteria,List<Integer> neighbourPolyIds, ArrayList<Integer>[] neighbourNeighbours) throws SQLException, InterruptedException{

		double[] criteriaBuffer = new double[numberlocations];
		List<Integer> notNeighbouredPlys = new ArrayList<Integer>();
		
		for (int j=0; j<criteriaBuffer.length;j++){
			criteriaBuffer[j]=criteriaf[j];
		}
		
		
		for (int j = 0; j < criteriaBuffer.length - 1; j++)
        {
            int index = j;
            for (int k = j + 1; k < criteriaBuffer.length; k++)
                if (criteriaBuffer[k] > criteriaBuffer[index])
                    index = k;
            double greaterNumber = criteriaBuffer[index]; 
            criteriaBuffer[index] = criteriaBuffer[j];
            criteriaBuffer[j] = greaterNumber;
        }
		
		//criteriaBuffer[0] area with biggest critSum
		//detect neighbours of this area
		
		List<Integer> actNeighbours = new ArrayList<Integer>();
		int locBiggest=-1;
		
		//determine location with biggest critSum
		for (int j=0;j<numberlocations;j++){
			if (criteriaf[j]==criteriaBuffer[0]){
				locBiggest=j;
			}
		}
		
		//determine neighbours of area of location with biggest critSum
		for (int j=0;j<numberlocations;j++){
			if (j!=locBiggest){
				boolean neighbour=false;
				int pos=0;
				
//				System.out.println(locBiggest);
				
				while (pos<allocPolys[locBiggest].size() && neighbour==false){
					//check every allocated Polygone whether it is a neighbour of one of the polys of another location
					//take poly of locBiggest and check to every poly of loc
					
					int actPoly = allocPolys[locBiggest].get(pos);
					int posActPoly = neighbourPolyIds.indexOf(actPoly);
					boolean neighbourfound=false;
					
					for(int k=0;k<allocPolys[j].size();k++){
						int comparePoly = allocPolys[j].get(k);
						for (int l=0;l<neighbourNeighbours[posActPoly].size();l++){
							if (neighbourNeighbours[posActPoly].get(l).equals(comparePoly) && !neighbourfound){
								neighbour=true;
								actNeighbours.add(j);
								neighbourfound=true;
							}
						}
					}
					
					pos++;
				}
			}
		}
		
//		System.out.println(actNeighbours);
		
		//determine that area of neighbours areas with smallest critSum
		double minsum=-1;
		boolean first=true;
		int locMinsum=-1;
		
		for(int j=0;j<numberlocations;j++){
			boolean found=false;
			int posLoc=0;
			while (!found){
				if (actNeighbours.get(posLoc)==j && j!=lastAreaBiggest && !nofoundlocations.contains(j) && j!=nofoundlocbiggest){
					if (first){
						first=false;
						minsum=criteriaf[j];
						locMinsum=j;
					}
					else{
						if (criteriaf[j]<minsum){
							locMinsum=j;
							minsum=criteriaf[j];
						}
					}
					
					found=true;
				}
				if ((posLoc+1)<actNeighbours.size()){
					posLoc++;
				}
				else{
					found=true;
				}
			}
		}
		
		//give locMinSum 1 polygone of locbiggest
//		System.out.println("neighbours:"+actNeighbours);
//		System.out.println("smallest "+(locMinsum+1)+","+minsum);
		
		int polyID = -1;
//		System.out.println(polyID);
		double minDist=-1;
		
		boolean getStartPoly=false;
		boolean foundStartPoly=false;
		int pos=0;
		while (!getStartPoly){
			polyID=allocPolys[locBiggest].get(pos).intValue();
			if (polyID!=homePolyIDs.get(locBiggest) && polyID!=lastPolyID){
				if (checkUnit(polyID,locBiggest,locMinsum, allocPolys, neighbourPolyIds, neighbourNeighbours)){
					getStartPoly=true;
					minDist = polyDistances[locMinsum].get(polyDistanceIds.indexOf(polyID));
					foundStartPoly=true;
				}
				else{
					pos++;
				}
			}
			else{
				pos++;
			}
			
			if (pos==allocPolys[locBiggest].size()){
				getStartPoly=true;
			}
		}

		if (foundStartPoly){
		for (int l=1;l<allocPolys[locBiggest].size();l++){
			int buffpolyID=allocPolys[locBiggest].get(l).intValue();
			double actDist = polyDistances[locMinsum].get(polyDistanceIds.indexOf(buffpolyID));

			if (actDist<minDist && buffpolyID!=homePolyIDs.get(locBiggest)){
				boolean unit = checkUnit(buffpolyID, locBiggest,locMinsum, allocPolys, neighbourPolyIds, neighbourNeighbours);
				
				if (unit){
					minDist=actDist;
					polyID=allocPolys[locBiggest].get(l).intValue();
				}
			}
		}
		
		counterIdUsed=0;
		for (int k=0;k<rearrangedPolys[0].size();k++){
			if (rearrangedPolys[2].get(k).equals(locMinsum) && rearrangedPolys[1].get(k).equals(locBiggest) && rearrangedPolys[0].get(k).equals(polyID)){
				counterIdUsed++;
			}
		}
		
		lastAreaBiggest=locBiggest;
//		System.out.println(counterIdUsed);
//		System.out.println(rearrangedPolys[0]);
//		System.out.println(rearrangedPolys[2]);
		
		rearrangedPolys[0].add(polyID);
		rearrangedPolys[1].add(locBiggest);
		rearrangedPolys[2].add(locMinsum);
		
		if (rearrangedPolys[0].size()>(numberlocations*numberlocations)){
			rearrangedPolys[0].remove(0);
			rearrangedPolys[1].remove(0);
			rearrangedPolys[2].remove(0);
		}
		
		//add polyID to locMinsum and remove from locbiggest
		lastPolyID=polyID;
		System.out.println("Set to "+(locMinsum+1)+" remove "+polyID+" from "+(locBiggest+1));
		String geom = geomAllocPolys[locBiggest].get(allocPolys[locBiggest].indexOf(polyID));
		
		buffgeomAllocPolys[locBiggest].remove(allocPolys[locBiggest].indexOf(polyID));
		allocPolys[locBiggest].remove(Integer.valueOf(polyID));
		
		allocPolys[locMinsum].add(polyID);
		buffgeomAllocPolys[locMinsum].add(geom);
		
//		System.out.println(allocPolys[locationremove].size()+","+allocPolys[i].size());
		criteriaf=addToCriteria(polyID, locMinsum+1, locBiggest+1, criteriaf, true, polyIDs, polysCriteria);
	
		}
		else{
			nofoundBiggest=true;
			nofoundlocbiggest=locMinsum;
		}
		return allocPolys;
	}
	
	public static ArrayList<Integer>[] rearrangeFromSmallest(int numberlocations, ArrayList<Integer>[] allocPolys, List<Integer>polyDistancesID, ArrayList<Double>[] polyDistances, List<Integer> polyIDs, List<Double> polysCriteria, List<Integer> tempbufferlist, List<Integer> neighbourPolyIds, ArrayList<Integer>[] neighbourNeighbours) throws SQLException, InterruptedException{
		
		int locSmallestCritSum=-1;
		
		//init array for sorted critSums
		double[] criteriaBuffer = new double[numberlocations];
		for (int j=0; j<criteriaBuffer.length;j++){
			criteriaBuffer[j]=criteriaf[j];
		}
		
		//sort critSums
		for (int j = 0; j < criteriaBuffer.length - 1; j++)
        {
            int index = j;
            for (int k = j + 1; k < criteriaBuffer.length; k++)
                if (criteriaBuffer[k] < criteriaBuffer[index])
                    index = k;
            double greaterNumber = criteriaBuffer[index]; 
            criteriaBuffer[index] = criteriaBuffer[j];
            criteriaBuffer[j] = greaterNumber;
        }
		
		//determine area with smallest critSum
		boolean foundlocSmallestCritSum=false;
		int poslocSmallCritSum=0;
		
		while (!foundlocSmallestCritSum){
			
			for (int j=0;j<numberlocations;j++){
				if (criteriaBuffer[poslocSmallCritSum]==criteriaf[j]){
					locSmallestCritSum=j;
				}
			}
			
			if (!nofoundlocations.contains(locSmallestCritSum) && locSmallestCritSum!=nofoundlocsmallest){
				foundlocSmallestCritSum=true;
			}
			else{
				poslocSmallCritSum++;
			}
		}
		
		//add all geometries to array which belong not to area with smallest critSum
		for (int j=0;j<numberlocations;j++){
			if (locSmallestCritSum!=j && j!=lastAreaSmallest){
				for(int k=0;k<allocPolys[j].size();k++){
					tempbufferlist.add(allocPolys[j].get(k));
				}
			}
		}
		
		boolean getStartPoly=false;
		int pos=0;
		int polyID=-1;
		double minDistance=-1;
		boolean foundStartPoly=false;
		
		//determine startPoly for distance check
		while (!getStartPoly){
			polyID=polyIDs.get(pos).intValue();
			
			int locationremove=-1;
			int k=0;
			boolean notfound=true;
			
			//determine location of polyID
			while (k<numberlocations && notfound==true){
					for (int l=0;l<allocPolys[k].size();l++){
						if (allocPolys[k].get(l).equals(polyID)){
							locationremove=k;
							notfound=false;
						}
					}
				
				k++;
			}
			
			if (locationremove!=locSmallestCritSum && polyID!=homePolyIDs.get(locationremove) && polyID!=lastPolyID){
//				System.out.println("check StartPoly");
				if (checkUnit(polyID, locationremove, locSmallestCritSum, allocPolys, neighbourPolyIds, neighbourNeighbours)){
					getStartPoly=true;
					minDistance=polyDistances[locSmallestCritSum].get(polyDistancesID.indexOf(tempbufferlist.get(0)));
					foundStartPoly=true;
				}
				else{
					pos++;
				}
			}
			else{
				pos++;
			}
			
			if (pos==polyIDs.size()){
				getStartPoly=true;
			}
		}
		
		if (foundStartPoly){
		for (int j=0;j<tempbufferlist.size();j++){
//			System.out.println(tempbufferlist.get(j)+","+polys[0].indexOf(Double.valueOf(tempbufferlist.get(j)))+","+polys[0].get(polys[0].indexOf(Double.valueOf(tempbufferlist.get(j))))+","+polys[i+1].get(polys[0].indexOf(Double.valueOf(tempbufferlist.get(j)))));
			double actdist=polyDistances[locSmallestCritSum].get(polyDistancesID.indexOf(tempbufferlist.get(j)));
//			System.out.println(minDistance);
			
			int k=0;
			boolean notfound=true;
			int locationremove=-1;
			int buffpolyID=tempbufferlist.get(j);
			
			//determine location of polyID
			while (k<numberlocations && notfound==true){
				if (k!=locSmallestCritSum){
//					System.out.println(j+","+allocPolys[j]);
					for (int l=0;l<allocPolys[k].size();l++){
						if (allocPolys[k].get(l).equals(polyID)){
							locationremove=k;
							notfound=false;
						}
					}
				}
				
				k++;
			}
			
			//check Unit if polyID will be rearranged	
			if (actdist<minDistance && buffpolyID!=homePolyIDs.get(locationremove)){
//				System.out.println("check unit");
				boolean unit = checkUnit(buffpolyID, locationremove, locSmallestCritSum, allocPolys, neighbourPolyIds, neighbourNeighbours);

//				System.out.println("unit:"+unit);
				if (unit){
					minDistance=actdist;
					polyID=buffpolyID;
//					System.out.println(polyID);
				}
			}	
		}
//		System.out.println(polyID);
		int locationremove=-1;
		int j=0;
		boolean notfound=true;
		while (j<numberlocations && notfound==true){
			if (j!=locSmallestCritSum){
//				System.out.println(j+","+allocPolys[j]);
				for (int k=0;k<allocPolys[j].size();k++){
					if (allocPolys[j].get(k).equals(polyID)){
						locationremove=j;
						notfound=false;
					}
				}
			}
			
			j++;
		}
		
		counterIdUsed=0;
		for (int k=0;k<rearrangedPolys[0].size();k++){
			if (rearrangedPolys[2].get(k).equals(locSmallestCritSum) && rearrangedPolys[1].get(k).equals(locationremove) && rearrangedPolys[0].get(k).equals(polyID)){
				counterIdUsed++;
			}
		}
		
		rearrangedPolys[0].add(polyID);
		rearrangedPolys[1].add(locationremove);
		rearrangedPolys[2].add(locSmallestCritSum);
		
		if (rearrangedPolys[0].size()>(numberlocations*numberlocations)){
			rearrangedPolys[0].remove(0);
			rearrangedPolys[1].remove(0);
			rearrangedPolys[2].remove(0);
		}
		
		lastAreaSmallest=locSmallestCritSum;
//		System.out.println(allocPolys[locationremove]);
		lastPolyID=polyID;
		System.out.println("Set to "+(locSmallestCritSum+1)+" remove "+polyID+" from "+(locationremove+1)+","+allocPolys[locationremove].size()+","+allocPolys[locSmallestCritSum].size());
		
		String geom = geomAllocPolys[locationremove].get(allocPolys[locationremove].indexOf(polyID));
		
		buffgeomAllocPolys[locationremove].remove(allocPolys[locationremove].indexOf(polyID));
		allocPolys[locationremove].remove(Integer.valueOf(polyID));

		allocPolys[locSmallestCritSum].add(polyID);
		buffgeomAllocPolys[locSmallestCritSum].add(geom);
		
//		System.out.println(allocPolys[locationremove].size()+","+allocPolys[i].size());
		criteriaf=addToCriteria(polyID, locSmallestCritSum+1, locationremove+1, criteriaf,true,polyIDs, polysCriteria);
		tempbufferlist.clear();
		
		}
		else{
			nofoundSmallest=true;
			nofoundlocsmallest=locSmallestCritSum;
		}
		
		return allocPolys;
	}
	
	public static ArrayList<Integer>[] checkthresholdCombi(int numberlocations, ArrayList<Integer>[] allocPolys, List<Integer> polyDistanceIds, ArrayList<Double>[] polyDistances, List<Integer> polyIDs, List<Double> polysCriteria,List<Integer> neighbourPolyIds, ArrayList<Integer>[] neighbourNeighbours) throws Exception{
		boolean satisfied=false; 
		System.out.println(homePolyIDs);
		List<Integer> tempbufferlist = new ArrayList<Integer>();
		for(int i=0;i<rearrangedPolys.length;i++) rearrangedPolys[i] = new ArrayList<Integer>();
		
		ArrayList<Integer>[] buffallocPolys; 
		buffallocPolys=allocPolys;
		buffgeomAllocPolys=geomAllocPolys;
		
		lastAreaBiggest=-1;
		lastAreaSmallest=-1;
		int run=0;
		int threshold = 10;
		
		while (!satisfied){
			int[] compCriterias = new int[numberlocations];
			for (int i=0; i<numberlocations; i++) compCriterias[i]=0;
			
			for (int i=0; i<numberlocations;i++){
				for (int j=0;j<numberlocations;j++){
					if (j!=i){
						if (criteriaf[i]<criteriaf[j]){
							if (100-(criteriaf[j]*100/criteriaf[i])>-threshold)
								compCriterias[i]++;
						}
						else{
							if ((criteriaf[j]*100/criteriaf[i])-100< threshold)
								compCriterias[i]++;
						}

					}
				}
			}
			
			int no=0;
			boolean arranged=false;
			
			for (int i=0; i<numberlocations;i++){	
				if (compCriterias[i]!=(numberlocations-1) && !arranged && !nofoundlocations.contains(i)){
					
					if (run%2==1){
						System.out.println("biggest");
						allocPolys=rearrangeFromBiggest(numberlocations, buffallocPolys, polyDistanceIds, polyDistances, polyIDs, polysCriteria, neighbourPolyIds, neighbourNeighbours);
					}
					else{
						System.out.println("smallest");
						allocPolys=rearrangeFromSmallest(numberlocations, buffallocPolys, polyDistanceIds, polyDistances, polyIDs, polysCriteria, tempbufferlist, neighbourPolyIds, neighbourNeighbours);
					}
					
//					System.out.println("counterIDused:"+counterIdUsed);
					
					if (counterIdUsed>=numberlocations){
						counterIdUsed=0;
						rearrangedPolys[0].clear();
						rearrangedPolys[1].clear();
						rearrangedPolys[2].clear();
						raiseThreshold=true;
					}
					
					arranged=true;
				}
				else no++;
			}
			run++;
			
			if (no==numberlocations){
				satisfied=true;
			}
			
			if (nofoundBiggest && nofoundSmallest){
				nofoundBiggest=false;
				nofoundSmallest=false;
				if (nofoundlocsmallest==nofoundlocbiggest){
					nofoundlocations.add(nofoundlocsmallest);
				}
			}
			
			if (raiseThreshold && !satisfied ){
				threshold=threshold+5;
				raiseThreshold=false;
				buffallocPolys=allocPolys;
				buffgeomAllocPolys=geomAllocPolys;
				System.out.println("Threshold raised to "+threshold);
			}
			
			if (nofoundlocations.size()>=numberlocations){
				satisfied=true;
				System.out.println("Break");
			}
			
//			
//			FileWriter output =createFileWriter();
//			for (int i=0; i<numberlocations;i++){
//				writePolygon(output, allocPolys[i], geomAllocPolys[i],i+1);
//			}
//			output.close();
		}
		
		allocPolys=buffallocPolys;
		geomAllocPolys=buffgeomAllocPolys;
		
		System.out.println("rearranged with a variance of "+threshold+"%");
		System.out.println("no better arrangement for "+nofoundlocations);
		return allocPolys;
	}
	
}
