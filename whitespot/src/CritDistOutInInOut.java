
import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;

public class CritDistOutInInOut {
	
	private static double lonlats[]; //stores locations
	private static ArrayList<Double>[] polys; //stores ID and distances to location
	private static ArrayList<String>[] polysGeometry; //stores ID and geometry of polys
	private static ArrayList<Integer>[] allocPolys; //stores allocated polys dependent on location 
	private static ArrayList<String>[] geomAllocPolys; //stores geometries of allocated polygons

	//adds activity measure of basic area to territory centre
	private static void addToCriteria(int polyID, int location, double[] criteria) throws SQLException{
		//get criteria of the given polygon
		double critValue = Double.parseDouble(polysGeometry[2].get(polysGeometry[0].indexOf(Integer.toString(polyID))));
			criteria[location-1]=criteria[location-1]+critValue;
		
	}
	

	/**
	 * Assign polygons which are nearest to the territory centre with smallest activity measure
	 * @param numberpolygons: number of all polygons of the region
	 * @param criteria: array of criteria sum for distribute polygons homogeneously
	 * @throws Exception
	 */
	private static void allocateOuterPolygons(int numberlocations, int numberpolygons, double[] criteria, boolean plz5) throws Exception{
		Statement stmt = functions.getConnection();
		String columnIDs="_g7304";
		
		if (!plz5){
			columnIDs="_g7305";
		}
			
		//get all PolygonIDs and store it
		StringBuffer sb = new StringBuffer();
		//SELECT t2.id, ST_AsTEXT(the_geom) AS the_geom, _c1 AS criteria FROM _varea_1424340553765 AS t1 INNER JOIN _vcriteria_1424340553765 As t2 ON t2._g7304=t1.id
		if (plz5){
			sb.append("SELECT t2.id AS id, ST_AsTEXT(the_geom) AS the_geom, _c1 AS criteria FROM geometriesplz51 AS t1 INNER JOIN criteriasplz51 AS t2 ON t2."+columnIDs+"=t1.\"id\"");
		}
		else{
			sb.append("SELECT t2.id AS id, ST_AsTEXT(the_geom) AS the_geom, _c1 AS criteria FROM geometriesplz81 AS t1 INNER JOIN criteriasplz81 AS t2 ON t2."+columnIDs+"=t1.\"id\"");
			
		}
		System.out.println(sb);
		ResultSet t=stmt.executeQuery(sb.toString());
		
		//store information of basic areas
		for (int i=0;i<numberpolygons;i++){
			t.next();
			polys[0].add(t.getDouble("id"));
			polysGeometry[0].add(t.getString("id"));
			polysGeometry[1].add(t.getString("the_geom"));
			polysGeometry[2].add(t.getString("criteria"));
		}
		
		//calculate distance of each basic area to each territory centre
		double distances[] = new double[numberlocations];
		for (int i=0; i<polys[0].size();i++){
			int poscoords=0;
			String geometry = polysGeometry[1].get(i);
			for (int j=1; j<numberlocations+1;j++){
				distances[j-1]=functions.calculateDistance(poscoords, geometry, stmt, lonlats);
				polys[j].add(distances[j-1]);
				poscoords=poscoords+2;			
			}
			
			//compare distances, to determine basic areas that can be allocated unique to a territory centre
			int[] compDistances = new int[numberlocations];
			for (int j=0;j<numberlocations;j++) compDistances[j]=0;
			for (int k=0;k<numberlocations;k++){
				for (int l=0; l<numberlocations;l++){
					if((l!=k)){
						if (distances[k]!=0.0){
							if (polys[0].get(i)==119.0 || polys[0].get(i)==125.0 || polys[0].get(i)==130.0) System.out.println(100-(distances[l]*100/distances[k])+","+(distances[k]<distances[l])+","+k+","+l);
							if (100-(distances[l]*100/distances[k])<-30 && distances[k]<distances[l]){
								compDistances[k]=compDistances[k]+1;
								if (polys[0].get(i)==119.0 || polys[0].get(i)==125.0 || polys[0].get(i)==130.0) System.out.println("comDis "+compDistances[k]);
							}
						}

					}
				}
			}
			
			//allocate basic areas that contain territory centres to the centres
			boolean allocated = false;
			for(int k=0;k<numberlocations;k++){
				if (distances[k]==0.0){
					allocated = true;
					System.out.println("write "+polys[0].get(i).intValue()+" to "+(k+1)+"-HomePoly");
					allocPolys[k].add(polys[0].get(i).intValue());
					geomAllocPolys[k].add(geometry);
				}
			}
			
			//allocate basic areas that be allocated unique to a territory centre
			for(int k=0;k<numberlocations;k++){
				if (compDistances[k]==numberlocations-1 && allocated == false){
					System.out.println("write "+polys[0].get(i).intValue()+" to "+(k+1));
					allocPolys[k].add(polys[0].get(i).intValue());
					geomAllocPolys[k].add(geometry);
				}
			}
			

		}

		//sum value of activity measure of all assigned polygons
		for (int i=0;i<numberlocations;i++){
				for (int j=0;j<allocPolys[i].size();j++){
					for (int k=0;k<numberlocations;k++){
						polys[k+1].remove(polys[0].indexOf(Double.valueOf(allocPolys[i].get(j))));
					}
					polys[0].remove(Double.valueOf(allocPolys[i].get(j)));
					addToCriteria(allocPolys[i].get(j), i+1, criteria);
				}
		}
			
		
		
		
	}
	
	//allocate basic areas that are not assigned yet
	private static void allocateInnerPolygons(int numberlocations, int numberpolygons, double[] criteria) throws SQLException{
		//allocate all basic areas
		while (polys[0].size()>0){
			double minCriteria=criteria[0];
			int locationMinCriteria=1;
			
			//determine territory centre with smallest activity measure
			for (int j=1;j<criteria.length;j++){
				if (criteria[j]<minCriteria){
					minCriteria=criteria[j];
					locationMinCriteria=j+1;
				}
			}	
			
			//determine nearest basic area
			int locMinDist = 0;
			double minDistance = polys[locationMinCriteria].get(0);
			for (int j=1;j<polys[locationMinCriteria].size();j++){
				double actdist=polys[locationMinCriteria].get(j);
				if (actdist<minDistance){
					locMinDist=j;
					minDistance=actdist;
				}
			}
				
			//allocate basic area
			int polyID = polys[0].get(locMinDist).intValue();
			System.out.println("write "+polys[0].get(locMinDist).intValue()+" to "+(locationMinCriteria));
			allocPolys[locationMinCriteria-1].add(polyID);
			String geometry = polysGeometry[1].get(polysGeometry[0].indexOf(Integer.toString(polyID)));
			geomAllocPolys[locationMinCriteria-1].add(geometry);
			
			addToCriteria(polyID, locationMinCriteria, criteria);
			for (int j=0; j<numberlocations;j++){
				polys[j+1].remove(locMinDist);
			}
			polys[0].remove(Double.valueOf(polyID));
		}
	}
	
	public static void main(String[] args)
	throws Exception {

		long time = System.currentTimeMillis();
		
		//set territory centres
		int numberlocations =10;
		boolean plz5 = false;
		lonlats= new double[numberlocations*2];
		lonlats=functions.setLocations(numberlocations);
		
		//create FileWriter
		FileWriter output = functions.createFileWriter();
		functions.createFileWriterLocs(numberlocations, lonlats);
		
		//initialize variables
		double[] criteria = new double[numberlocations];
		for (int i=0; i<numberlocations;i++){
			criteria[i]=0;
		}
		
		polys = (ArrayList<Double>[])new ArrayList[numberlocations+1];
		for(int i=0;i<polys.length;i++) polys[i] = new ArrayList<Double>();
		
		polysGeometry = (ArrayList<String>[])new ArrayList[3];
		for(int i=0;i<polysGeometry.length;i++) polysGeometry[i] = new ArrayList<String>();
		
		allocPolys = (ArrayList<Integer>[])new ArrayList[numberlocations];
		for(int i=0;i<allocPolys.length;i++) allocPolys[i] = new ArrayList<Integer>();
		
		geomAllocPolys = (ArrayList<String>[])new ArrayList[numberlocations];
		for(int i=0;i<geomAllocPolys.length;i++) geomAllocPolys[i] = new ArrayList<String>();

		
//		//calculate number of basic areas in that region
		int numberpolygons=functions.getNrOrSum(true,plz5);
		
//		//allocate basic ares to territory centres
		System.out.println("outer");
		allocateOuterPolygons(numberlocations, numberpolygons, criteria,plz5);
		System.out.println("inner");
		allocateInnerPolygons(numberlocations, numberpolygons, criteria);
		
//		//Create file with allocated basic areas
		for (int i=0; i<numberlocations;i++){
			functions.writePolygon(output, allocPolys[i], geomAllocPolys[i],i+1);
		}

		for (int i = 0; i < numberlocations; i++) {
			System.out.println("Activity measure territory " + (i+1) + " :"
					+ criteria[i]);
		}
		
		for (int i=0; i<numberlocations;i++){
			double com = calcCompactness(numberpolygons, i, plz5);
			System.out.println("compactness of territory "+ (i+1) + " :"
					+ com);
		}
		
		System.out.println("Time for whole algorithm:"+(System.currentTimeMillis()-time)+" ms");
		
		output.flush();
		output.close();
	    
	    System.out.println("successfully ended");
}
	
	//calculate circumference for calculation of compactness
	private static double calculateCircumference(int numberpolygons, int location, boolean plz5) throws SQLException{
		Statement stmt = functions.getConnection();
		StringBuffer sb = new StringBuffer();
		
		StringBuilder idsBuffer = new StringBuilder(allocPolys[location].toString());
		idsBuffer.deleteCharAt(0);
		idsBuffer.deleteCharAt(idsBuffer.length()-1);
		
		if(plz5){
			sb.append("SELECT ST_PERIMETER(ST_UNION(the_geom)) FROM geometriesplz51 WHERE id IN ("+idsBuffer.toString()+");");
		}
		else{
			sb.append("SELECT ST_PERIMETER(ST_UNION(the_geom)) FROM geometriesplz81 WHERE id IN ("+idsBuffer.toString()+");");
			
		}

		ResultSet d = null;
		d = stmt.executeQuery(sb.toString());
		d.next();
		
		double area=d.getDouble(1);
		
		return area;
	}
	
	//calculate area for calculation of compactness
	private static double calculateArea(int numberpolygons, int location, boolean plz5) throws SQLException{

		Statement stmt = functions.getConnection();
		StringBuffer sb = new StringBuffer();
		
		StringBuilder idsBuffer = new StringBuilder(allocPolys[location].toString());
		idsBuffer.deleteCharAt(0);
		idsBuffer.deleteCharAt(idsBuffer.length()-1);
		
		if(plz5){
			sb.append("SELECT ST_AREA(ST_UNION(the_geom)) FROM geometriesplz51 WHERE id IN ("+idsBuffer.toString()+");");
		}
		else{
			sb.append("SELECT ST_AREA(ST_UNION(the_geom)) FROM geometriesplz81 WHERE id IN ("+idsBuffer.toString()+");");
			
		}
		ResultSet d = null;
		d = stmt.executeQuery(sb.toString());
		d.next();
		
		double area=d.getDouble(1);
		
		return area;
	}
	
	//calculate compactness
	public static double calcCompactness(int numberpolygons, int i, boolean plz5) throws SQLException{
		
		double U_area = calculateCircumference(numberpolygons, i, plz5);
		double A_area = calculateArea(numberpolygons, i, plz5);
		
		double A_circle = Math.PI * Math.pow((U_area / (2 * Math.PI)), 2);

		double compactness = A_area / A_circle;

		return compactness;
	}
}
