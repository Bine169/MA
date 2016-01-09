
import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class justCrit {
	
	private static double lonlats[]; //stores locations
	private static ArrayList<Double>[] polys; //stores ID and distances to location
	private static ArrayList<String>[] polysGeometry; //stores ID and geometry of polys
	private static ArrayList<Integer>[] allocPolys; //stores allocated polys dependent on location 
	private static ArrayList<String>[] geomAllocPolys; //stores geometries of allocated polygons

//	
//	/**
//	 * Add value of polygon to criteria
//	 * @param polyID: ID of Polygon
//	 * @param location
//	 * @param jdbc: JDBCConnection
//	 * @param criteria: Array of all criterias
//	 * @throws SQLException
//	 */
	private static void addToCriteria(int polyID, int location, double[] criteria) throws SQLException{
		//get criteria of the given polygon
		double critValue = Double.parseDouble(polysGeometry[2].get(polysGeometry[0].indexOf(Integer.toString(polyID))));
		
		
			criteria[location-1]=criteria[location-1]+critValue;
		
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
	
	
	/**
	 * Assign basic areas to territory centre with smallest activity measure
	 * @param numberpolygons: number of all polygons of the region
	 * @param criteria: array of criteria sum for distribute polygons homogeneously
	 * @throws Exception
	 */
	private static void allocatePolygons(int numberlocations, int numberpolygons, double[] criteria, boolean plz5) throws Exception{
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
			
		System.out.println("length"+polys[0].size());
			
		//allocate basic areas
		while (polys[0].size()!=0){
			
			double minCriteria=criteria[0];
			int locationMinCriteria=1;
			
			//determine territory centre with smallest activity measure
			for (int j=1;j<criteria.length;j++){
				if (criteria[j]<minCriteria){
					minCriteria=criteria[j];
					locationMinCriteria=j+1;
				}
			}
			
			//allocate basic area in list
			int polyID = polys[0].get(0).intValue();
	
			double critValue = Double.parseDouble(polysGeometry[2].get(polysGeometry[0].indexOf(Integer.toString(polyID))));
			
			System.out.println("write "+polyID+" to "+(locationMinCriteria));
			allocPolys[locationMinCriteria-1].add(polyID);
			String geometry = polysGeometry[1].get(polysGeometry[0].indexOf(Integer.toString(polyID)));
			geomAllocPolys[locationMinCriteria-1].add(geometry);
			
			addToCriteria(polyID, locationMinCriteria, criteria);
			polys[0].remove(Double.valueOf(polyID));
		}
		
	}
	
	public static void main(String[] args)
	throws Exception {

		long time = System.currentTimeMillis();
		
		//setLocations
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
		int numberpolygons=functions.getNrOrSum(true, plz5);
		
//		//allocate basic areas to territory centres
		allocatePolygons(numberlocations, numberpolygons, criteria, plz5);
		
//		//Create file with allocated basic areas
		for (int i=0; i<numberlocations;i++){
			functions.writePolygon(output, allocPolys[i], geomAllocPolys[i],i+1);
		}
		for (int i = 0; i < numberlocations; i++) {
			System.out.println("Activity measure territory " + (i+1) + " :"
					+ criteria[i]);
		}

		output.flush();
		output.close();
		
		//calculate compactness
		for (int i=0; i<numberlocations;i++){
			double com = calcCompactness(numberpolygons, i, plz5);
			System.out.println("compactness of territory "+ (i+1) + " :"
					+ com);
		}
    
		System.out.println("Time for whole algorithm:"+(System.currentTimeMillis()-time)+" ms");		
		
	    System.out.println("successfully ended");
}
}
