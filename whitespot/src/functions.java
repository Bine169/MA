
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author sumsum
 *
 */
public class functions {
	static double[] criteriaf;

	public static void setCriteria(double[] crit, int numberlocations){
		criteriaf = new double[numberlocations];
		for (int i=0; i<numberlocations;i++){
			criteriaf[i]=crit[i];
		}
	}
	
	public static double[] getCriteria(){
		return criteriaf;
	}
	
	public static double[] setLocations(int numberlocations) throws IOException{
		double lonlats[]= new double[numberlocations*2];
		// Input file which needs to be parsed
		String fileToParse = null;
		fileToParse = "E:\\Studium\\Master\\4.Semester - MA\\OSD_Standorte_MC.csv";
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
					lonlats[pos]=lon;
					lonlats[pos+1]=lat;
					pos = pos + 2;
				}

				if (i == numberlocations) {
					satisfied = true;
				}
			}
		}

		fileReader.close();
		
		return lonlats;
	}
		
	/**
	 * calculates the number of basic areas which are inside the Area; it is necessary to allocate all polygons
	 * @return number of polygons
	 * @throws SQLException
	 */
	public static int getNrOrSum(boolean number, boolean plz5) throws SQLException, ClassNotFoundException{
		Statement stmt = getConnection();
		StringBuffer sb = new StringBuffer();
	if (plz5){
		if (number)
			{ sb.append("SELECT COUNT (id) FROM criteriasplz51");}
		else
			{ sb.append("SELECT SUM(CAST(_c1 AS int)) FROM criteriasplz51");}
		}
	else{
		if (number)
		{ sb.append("SELECT COUNT (id) FROM criteriasplz81");}
	else
		{ sb.append("SELECT SUM(CAST(_c1 AS int)) FROM criteriasplz81");}
	}
		
	
		ResultSet t=stmt.executeQuery(sb.toString());
		t.next();
		int sum=t.getInt(1);
		if (number)
			{System.out.println("numberofpolygons: "+sum);}
		else
			{System.out.println("sum: "+sum);}

		return sum;
	}	

	/**
	 * Write basic areas into file, just necessary for testing purposes
	 * @param buffershp: name of the ShapeBuffer
	 * @param shpWriter: name of the ShapeWriter
	 * @param bufferPoly: IDs of Polygons which belong/are allocated to the location
	 * @param geomPoly: Geometries of Polygons which belong/are allocated to the location
	 * @param location
	 * @throws Exception
	 */
	public static void writePolygon(FileWriter output, List<Integer> bufferPoly, List<String> geomPoly, int location) throws Exception{
	System.out.println("size"+location+" :"+bufferPoly.size());
		
	for (int i=0; i<bufferPoly.size();i++){
		output.append(Objects.toString(bufferPoly.get(i)));
		output.append(";");
		output.append(Objects.toString(location));
		output.append("\n");
	}	
	}

	/**
	 * Calculates the distance of the territory centres to the actual basic area
	 * @param location
	 * @param geometry: geometry of the polygon to which the distance should be calculated
	 * @param jdbc: JDBCConnection
	 * @return double value of calculated distance
	 * @throws SQLException
	 */
	private static double calculateDist(StringBuffer sb, Statement stmt) throws SQLException{
		double distance;
		ResultSet d=stmt.executeQuery(sb.toString());
		d.next();
		distance=d.getDouble(1);
		
		return distance;
	}
	
	/**
	 * Calculates neighbours of basic area
	 * @param geometrySource: String, geometry of basic area
	 * @param geometrytarget: String, geometry of basic area that should be proofed whether it is neighboored 
	 * @param stmt: Statement 
	 * @return double value of calculated distance
	 * @throws SQLException
	 */
	public static double calculateNeighbors(String geometrySource, String geometryTarget, Statement stmt) throws SQLException{
		double distance;
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT st_distance(ST_GeomFromText('"+geometrySource+"'),ST_GeomFromText('"+geometryTarget+"'));");
		distance = calculateDist(sb, stmt);
		return distance;
	}
	
	/**
	 * Calculates distance of territory centre to basic area
	 * @param poscoords: int, indicates coordinates of territory centre
	 * @param geometry: String, geometry of basic area 
	 * @param stmt: Statement 
	 * @return double value of calculated distance
	 * @throws SQLException
	 */
	public static double calculateDistance(int poscoords, String geometry, Statement stmt, double[] lonlats) throws SQLException{
		double distance;
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT st_distance(ST_GeomFromText('POINT("+lonlats[poscoords]+" "+lonlats[poscoords+1]+")'),ST_GeomFromText('"+geometry+"'));");
		distance = calculateDist(sb, stmt);
		return distance;
	}
	
	/**
	 * creates connection to database
	 * @return statement, connection to database
	 */
	public static Statement getConnection(){
		Connection jdbc = null;
		Statement stmt = null;
		try {
	         Class.forName("org.postgresql.Driver");
	         jdbc = DriverManager
	            .getConnection("jdbc:postgresql://localhost:5432/MA",
	            "postgres", "");
	      stmt = jdbc.createStatement();
		}
	    catch ( Exception e ) {
	          System.err.println( e.getClass().getName()+": "+ e.getMessage() );
	          System.exit(0);
	    }
		
		return stmt;
	}
	
	
	/**
	 * create Filewriter
	 * @return file
	 * @throws IOException
	 */
	public static FileWriter createFileWriter() throws IOException{
		FileWriter output = new FileWriter("polygones.csv");
		output.append(new String("ID,Location,geometry"));
		output.append("\n");
		
		return output;
	}
	
	/**
	 * create Filewriter of territory centres
	 * @param numberlocations: String, geometry of basic areaint, number of territory centres
	 * @param lonlats: Array, which contains the coordinates of the territory centres 
	 * @return double value of calculated distance
	 */
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

	/**
	 * add activity measure of basic area to territory centre
	 * @param polyID: int, ID of the basic area
	 * @param location: int, indicates territory centre that gets the basic area 
	 * @param locationMaxCriteria: int, indicates territory centre that gives the basic area during rearrangement
	 * @param criteria: array, that stores activity measure of each territory centre
	 * @param rearranged: boolean, to indicate whether basic area was rearranged
	 * @param polysGeometry: geometry of basic area 
	 * @return array, that stores activity measure of each territory centre
	 * @throws SQLException
	 */
	public static double[] addToCriteria(int polyID, int location, int locationMaxCriteria, double[] criteria, boolean rearranged, ArrayList<String>[] polysGeometry) throws SQLException{
		//get criteria of the given polygon
		double critValue = Double.parseDouble(polysGeometry[2].get(polysGeometry[0].indexOf(String.valueOf(polyID))));
		
		if (!rearranged){
			criteria[location-1]=criteria[location-1]+critValue;
		}
		else{			
			criteria[locationMaxCriteria-1]=criteria[locationMaxCriteria-1]-critValue;
			criteria[location-1]=criteria[location-1]+critValue;
		}
		
		return criteria;
	}
	
	/**
	 * rearrange basic areas by removing from territory centre with highest activity measures
	 * @param numberlocations: int, number of territory centres
	 * @param allocPolys: array, list of basic areas 
	 * @param polys: array, that stores information of basic areas
	 * @param polysGeometry: array, that stores geometry data of basic areas
	 * @param neighbourPolyIds: list, indicates the id of basic areas 
	 * @param neighbourNeighbours: list, indicates neighbours of each basic area
	 * @throws SQLException
	 */
	public static ArrayList<Integer>[] checkthresholdBiggest(int numberlocations, ArrayList<Integer>[] allocPolys, ArrayList<Double>[] polys, ArrayList<String>[] polysGeometry,List<Integer> neighbourPolyIds, ArrayList<Integer>[] neighbourNeighbours) throws SQLException{
		boolean satisfied=false;
		List<Integer> tempbufferlist = new ArrayList<Integer>();
		
		int lastPolyID=-1;
		int lastArea=-1;
		List<Integer> changedIds = new ArrayList<Integer>();
		
		//rearrange while no balance is achieved
		while (!satisfied){
			int threshold = 30;
			int[] compCriterias = new int[numberlocations];
			for (int i=0; i<numberlocations; i++) compCriterias[i]=0;
			
			//check balance
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
			
			//rearrange if balance is not reached, check every territory centre
			for (int i=0; i<numberlocations;i++){			
				if (compCriterias[i]!=(numberlocations-1)){
					double[] criteriaBuffer = new double[numberlocations];
					
					for (int j=0; j<criteriaBuffer.length;j++){
						criteriaBuffer[j]=criteriaf[j];
					}

					
					//sort activity measures from highest to smallest
					//criteriaBuffer[0] territory with highest activity measure
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
					
					int locBiggest=-1;
					
					for (int j=0;j<numberlocations;j++){
						if (criteriaf[j]==criteriaBuffer[0]){
							locBiggest=j;
						}
					}
					
					//determine neighbours of territory with highest activity measure
					List<Integer> actNeighbours = new ArrayList<Integer>();
					
					for (int j=0;j<numberlocations;j++){
						if (j!=locBiggest){
							boolean neighbour=false;
							int pos=0;
							
							
							while (pos<allocPolys[locBiggest].size() && neighbour==false){
								//check every allocated basic area whether it is a neighbour of one of the basic areas of another territory
								//take basic area of locBiggest and check to every basic area of centre
								
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
					
					
					//determine that territory of neighboured territories with smallest activity measure
					double minsum=-1;
					boolean first=true;
					int locMinsum=-1;
					
					for(int j=0;j<numberlocations;j++){
						boolean found=false;
						int posLoc=0;
						while (!found){
							if (actNeighbours.get(posLoc)==j){
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
					
					//give locMinSum 1 basic area of locbiggest
					int polyID = allocPolys[locBiggest].get(0).intValue();
					System.out.println(polyID);
					double minDist = polys[locMinsum+1].get(polys[0].indexOf(Double.valueOf(polyID)));
					
					for (int l=1;l<allocPolys[locBiggest].size();l++){
						double actDist = polys[locMinsum+1].get(polys[0].indexOf(Double.valueOf(allocPolys[locBiggest].get(l))));
						if (locBiggest==2){
							System.out.println("here");
							
						}
						if (actDist<minDist && !changedIds.contains(allocPolys[locBiggest].get(l).intValue())){
							minDist=actDist;
							polyID=allocPolys[locBiggest].get(l).intValue();
							changedIds.add(polyID);
						}
					}
					
					//add polyID of basic area to locMinsum and remove from locbiggest
					System.out.println("Set to "+(locMinsum+1)+" remove "+polyID+" from "+(locBiggest+1));
					allocPolys[locBiggest].remove(Integer.valueOf(polyID));
					allocPolys[locMinsum].add(polyID);
					criteriaf=addToCriteria(polyID, locMinsum+1, locBiggest+1, criteriaf, true,polysGeometry);
				}
				else no++;
			}
			
			if (no==numberlocations){
				satisfied=true;
			}
		}
		return allocPolys;
	
}
	
}
