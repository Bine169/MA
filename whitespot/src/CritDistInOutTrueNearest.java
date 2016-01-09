
import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CritDistInOutTrueNearest {
	
	private static double lonlats[]; //stores locations
	private static ArrayList<Double>[] polys; //stores ID and distances to location
	private static ArrayList<Double>[] bufferpolys; //stores ID and distances to location
	private static ArrayList<String>[] polysGeometry; //stores ID and geometry of polys
	private static ArrayList<Integer>[] allocPolys; //stores allocated polys dependent on location 
	private static ArrayList<String>[] geomAllocPolys; //stores geometries of allocated polygons

	
	//adds activity measure of basic area to territory centre
	private static void addToCriteria(int polyID, int location, double[] criteria) throws SQLException{
		//get criteria of the given polygon
		double critValue = Double.parseDouble(polysGeometry[2].get(polysGeometry[0].indexOf(Integer.toString(polyID))));
			criteria[location-1]=criteria[location-1]+critValue;
		
	}
	
	//removes activity measure of basic area from terriory centre
	private static void removeFromCriteria(int polyID, int location, double[] criteria) throws SQLException{
		//get criteria of the given polygon
		double critValue = Double.parseDouble(polysGeometry[2].get(polysGeometry[0].indexOf(Integer.toString(polyID))));
		
		
			criteria[location]=criteria[location]-critValue;
		
	}
	
	/**
	 * Assign polygons which are truely nearest to the territory centre with smallest activity measure
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
		
		List<Integer> changedIDs = new ArrayList<Integer>();
		
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
		double distances[] = new double[numberlocations];
		
		//store information of basic areas
		for (int i=0;i<numberpolygons;i++){
			t.next();
			polys[0].add(t.getDouble("id"));
			bufferpolys[0].add(t.getDouble("id"));
			polysGeometry[0].add(t.getString("id"));
			polysGeometry[1].add(t.getString("the_geom"));
			polysGeometry[2].add(t.getString("criteria"));
		}
		
		//calculates distance from each basic area to each territory centre
		for (int i=0;i<polys[0].size();i++){
			String geometry = polysGeometry[1].get(i);
			int poscoords=0;
			for (int j=1; j<numberlocations+1;j++){
				distances[j - 1] = functions.calculateDistance(poscoords, geometry, stmt, lonlats);
				polys[j].add(distances[j-1]);
				bufferpolys[j].add(distances[j-1]);
				poscoords=poscoords+2;
			}
		}
			
		System.out.println("length"+polys[0].size());
			
		int lastID=-1;
		
		//allocate basic areas
		while (polys[0].size()>0){
//			System.out.println("poly "+i);
			double minCriteria=criteria[0];
			int locationMinCriteria=1;
			
			//determine territory centre with smallest activity measure
			for (int j=1;j<criteria.length;j++){
				if (criteria[j]<minCriteria){
					minCriteria=criteria[j];
					locationMinCriteria=j+1;
				}
			}	
			
			//calculate nearest basic area to territory centre with smallest activity measure
			int locMinDist = 0;
			double minDistance = polys[locationMinCriteria].get(0);
			for (int j=1;j<polys[locationMinCriteria].size();j++){
				double actdist=polys[locationMinCriteria].get(j);
				if (actdist<minDistance){
					locMinDist=j;
					minDistance=actdist;
				}
			}
				
			int polyID = polys[0].get(locMinDist).intValue();
			
			boolean nearer=false;
			//check whether Poly is nearer to another territory centre
			int locNearer=-1;
			for (int j=1;j<numberlocations+1;j++){
				if (polys[j].get(locMinDist)<polys[locationMinCriteria].get(locMinDist)){
					nearer=true;
				}
			}
			
			
			//if nearer, determine basic area that is nearer and already asigned
			System.out.println("Poly before: "+polyID+","+minDistance);
			boolean changeId=false;
			if (nearer){
				for (int j=0;j<numberlocations;j++){
					if (j!=locationMinCriteria-1){
						for (int k=0;k<allocPolys[j].size();k++){
							double id =allocPolys[j].get(k);
		//					System.out.println(j+","+locNearer+","+id+","+bufferpolys[0].indexOf(id));
							double actDist=bufferpolys[locationMinCriteria].get(bufferpolys[0].indexOf(id));
							if (actDist<minDistance){
								int buffpolyID = bufferpolys[0].get(bufferpolys[0].indexOf(id)).intValue();
								if (!changedIDs.contains(buffpolyID)){
								minDistance=actDist;
								changeId=true;
								locNearer=j;
								polyID=buffpolyID;
								changedIDs.add(polyID);
								}
							}
						}
					}
				}
				
			}
			
			//allocate basic area
			System.out.println("write "+polyID+" to "+(locationMinCriteria));
			allocPolys[locationMinCriteria-1].add(polyID);
			String geometry = polysGeometry[1].get(polysGeometry[0].indexOf(Integer.toString(polyID)));
			geomAllocPolys[locationMinCriteria-1].add(geometry);
			lastID=locationMinCriteria;
			
			//remove basic area if it was moved from one territory to another
			if (nearer && changeId==true){
				allocPolys[locNearer].remove(Integer.valueOf(polyID));
				geomAllocPolys[locNearer].remove(geometry);
				removeFromCriteria(polyID, locNearer, criteria);
			}
			
			addToCriteria(polyID, locationMinCriteria, criteria);
			
			if (!nearer || changeId==false){
				for (int j=0; j<numberlocations;j++){
					polys[j+1].remove(locMinDist);
				}
			
				polys[0].remove(Double.valueOf(polyID));
			}
			
			System.out.println(polys[0].size());
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
		
		bufferpolys = (ArrayList<Double>[])new ArrayList[numberlocations+1];
		for(int i=0;i<bufferpolys.length;i++) bufferpolys[i] = new ArrayList<Double>();
		
		polysGeometry = (ArrayList<String>[])new ArrayList[3];
		for(int i=0;i<polysGeometry.length;i++) polysGeometry[i] = new ArrayList<String>();
		
		allocPolys = (ArrayList<Integer>[])new ArrayList[numberlocations];
		for(int i=0;i<allocPolys.length;i++) allocPolys[i] = new ArrayList<Integer>();
		
		geomAllocPolys = (ArrayList<String>[])new ArrayList[numberlocations];
		for(int i=0;i<geomAllocPolys.length;i++) geomAllocPolys[i] = new ArrayList<String>();

		
		//calculate number of basic areas in that region
		int numberpolygons=functions.getNrOrSum(true, plz5);
		
		//allocate basic areas to territory centres
		allocatePolygons(numberlocations, numberpolygons, criteria, plz5);
		
		//Create Shapefile with allocated basic areas
		for (int i=0; i<numberlocations;i++){
			functions.writePolygon(output, allocPolys[i], geomAllocPolys[i],i+1);
		}
		
		//print results
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
	
//	static class Distances{
//		int id;
//		
//	}
}
