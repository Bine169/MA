import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;

public class sumDivNumberDist {
	
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
//	
//	/**
//	 * Assign polygons which are near to the locations
//	 * @param numberpolygons: number of all polygons of the region
//	 * @param criteria: array of criteria sum for distribute polygons homogeneously
//	 * @throws Exception
//	 */
	private static void allocatePolygons(int numberlocations, int numberpolygons, double[] criteria, int sumcriteria, boolean plz5) throws Exception{
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
			
		}System.out.println(sb);
		ResultSet t=stmt.executeQuery(sb.toString());
			
		for (int i=0;i<numberpolygons;i++){
			t.next();
			polys[0].add(t.getDouble("id"));
			polysGeometry[0].add(t.getString("id"));
			polysGeometry[1].add(t.getString("the_geom"));
			polysGeometry[2].add(t.getString("criteria"));
		}
			
		System.out.println("length"+polys[0].size());
		double totalsum=sumcriteria/numberlocations;
		double distances[] = new double[numberlocations];
		
		for (int i=0; i<polys[0].size();i++){
			int poscoords=0;
			String geometry = polysGeometry[1].get(i);
			for (int j=1; j<numberlocations+1;j++){
				polys[j].add(functions.calculateDistance(poscoords, geometry, stmt, lonlats));
				poscoords=poscoords+2;
			}
		}
		
		for (int i=0;i<numberlocations;i++){
			double sumloc=0;
			while (sumloc<totalsum && polys[0].size()>0){
				
				double minDistance = polys[i+1].get(0);
				for (int j=1;j<polys[i].size();j++){
					if (polys[i+1].get(j)<minDistance){
						minDistance=polys[i+1].get(j);
					}
				}
				
				int polyID = polys[0].get(polys[i+1].indexOf(minDistance)).intValue();
				double critValue = Double.parseDouble(polysGeometry[2].get(polysGeometry[0].indexOf(Integer.toString(polyID))));
				sumloc=sumloc+critValue;
				
				System.out.println("write "+polyID+" to "+(i+1));
				allocPolys[i].add(polyID);
				String geometry = polysGeometry[1].get(polysGeometry[0].indexOf(Integer.toString(polyID)));
				geomAllocPolys[i].add(geometry);
				
				addToCriteria(polyID, i+1, criteria);
				for (int j=1;j<numberlocations+1;j++){
					polys[j].remove(polys[0].indexOf((double)polyID));
				}
				polys[0].remove(Double.valueOf(polyID));
			}
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

		
//		//calculate number of Polygons in that region
		int numberpolygons=functions.getNrOrSum(true, plz5);
		int sumcriteria=functions.getNrOrSum(false, plz5);
		
//		//alocate Polygons to locations
		allocatePolygons(numberlocations, numberpolygons, criteria, sumcriteria, plz5);
		
//		//Create Shapefile with allocated poylgons
		for (int i=0; i<numberlocations;i++){
			functions.writePolygon(output, allocPolys[i], geomAllocPolys[i],i+1);
		}
//		
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
//		
		output.flush();
		output.close();
	    
	    System.out.println("successfully ended");
}
	private static double calculateCircumference(int numberpolygons, int location, boolean plz5) throws SQLException{
		double area=-1;
		Statement stmt = functions.getConnection();
		StringBuffer sb = new StringBuffer();
		
		if (allocPolys[location].size()>0){
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
		
		area=d.getDouble(1);
		}
		else{
			area=-1;
		}
		
		return area;
	}
	
	private static double calculateArea(int numberpolygons, int location, boolean plz5) throws SQLException{
		double area=-1;
		Statement stmt = functions.getConnection();
		StringBuffer sb = new StringBuffer();
		
		if (allocPolys[location].size()>0){
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
		
		area=d.getDouble(1);
		}
		else{
			area=-1;
		}
		
		return area;
	}
	
	public static double calcCompactness(int numberpolygons, int i, boolean plz5) throws SQLException{
		double compactness=-1;
		
		double U_area = calculateCircumference(numberpolygons, i, plz5);
		double A_area = calculateArea(numberpolygons, i, plz5);
		
		double A_circle = Math.PI * Math.pow((U_area / (2 * Math.PI)), 2);

		if (A_area!=-1 && U_area!=-1){
			compactness = A_area / A_circle;
		}

		return compactness;
	}
}
