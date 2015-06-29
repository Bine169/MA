package whitespotGreenfield;

import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class DistCrit {
	
	private static double lonlats[]; //stores locations
	private static ArrayList<Double>[] polys; //stores ID and distances to location
	private static ArrayList<String>[] polysGeometry; //stores ID and geometry of polys
	private static ArrayList<Integer>[] allocPolys; //stores allocated polys dependent on location 
	private static ArrayList<String>[] geomAllocPolys; //stores geometries of allocated polygons

	
	private static class polygones{
		static List<Integer> ids;
		static List<String> geometry;
		static List<Double> criteria;
	}
	
	private static class polyDistances{
		static List<Integer> ids;
		static ArrayList<Double>[] distances;
	}
	
	private static class homePolys{
		static List<Integer> idLoc;
		static List<Integer> idPoly;
 	}
	
	private static class polyNeighbours{
		static List<Integer> polyIds;
		static ArrayList<Integer>[] neighbours;
	}
//	
//	/**
//	 * Add value of polygon to criteria
//	 * @param polyID: ID of Polygon
//	 * @param location
//	 * @param jdbc: JDBCConnection
//	 * @param criteria: Array of all criterias
//	 * @throws SQLException
//	 */
//	public static void addToCriteria(int polyID, int location, int locationMaxCriteria, double[] criteria, boolean rearranged) throws SQLException{
//		//get criteria of the given polygon
//		double critValue = Double.parseDouble(polysGeometry[2].get(polysGeometry[0].indexOf(String.valueOf(polyID))));
//		
//		if (!rearranged){
//			criteria[location-1]=criteria[location-1]+critValue;
//		}
//		else{			
//			System.out.println("criterias before: "+ criteria[locationMaxCriteria-1]+","+criteria[location-1]);
//			criteria[locationMaxCriteria-1]=criteria[locationMaxCriteria-1]-critValue;
//			criteria[location-1]=criteria[location-1]+critValue;
//			System.out.println("criterias after: "+ criteria[locationMaxCriteria-1]+","+criteria[location-1]);
//		}
//		
//	}
//	
//	/**
//	 * Assign polygons which are near to the locations
//	 * @param numberpolygons: number of all polygons of the region
//	 * @param criteria: array of criteria sum for distribute polygons homogeneously
//	 * @throws Exception
//	 */
	private static void getPolygones(int numberlocations, int numberpolygons, double[] criteria, boolean PLZ5) throws Exception{
		Statement jdbc = functionsOld.getConnection();
		String columnIDs=null;
		String tablegeom=null;
		String tablecrit=null;
		//PLZ5
				if (PLZ5){
					columnIDs="_g7304";
					tablegeom="geometriesplz5";
					tablecrit="criteriasplz5";
				}
				else{
				//PLZ8
					columnIDs="_g7305";
					tablegeom="geometriesplz8";
					tablecrit="criteriasplz8";
				}
			
		//get all PolygonIDs and store it
		StringBuffer sb = new StringBuffer();
		//SELECT t2.id, ST_AsTEXT(the_geom) AS the_geom, _c1 AS criteria FROM _varea_1424340553765 AS t1 INNER JOIN _vcriteria_1424340553765 As t2 ON t2._g7304=t1.id
		sb.append("SELECT t2.id AS id, ST_AsTEXT(the_geom) AS the_geom, _c1 AS criteria FROM "+tablegeom+" AS t1 INNER JOIN "+tablecrit+" AS t2 ON t2."+columnIDs+"=t1.id");
		System.out.println(sb);
		ResultSet t=jdbc.executeQuery(sb.toString());
			
		//get ids, geometry and criteria
		for (int i=0;i<numberpolygons;i++){
			t.next();
			polys[0].add(t.getDouble("id"));
			polysGeometry[0].add(t.getString("id"));
			polysGeometry[1].add(t.getString("the_geom"));
			polysGeometry[2].add(t.getString("criteria"));
			polygones.ids.add(t.getInt("id"));
			polygones.geometry.add(t.getString("the_geom"));
			polygones.criteria.add(t.getDouble("criteria"));
			polyDistances.ids.add(t.getInt("id"));
			
		}
			
		System.out.println("length"+polys[0].size());
		int poscoords=0;
		
		//calculate HomePolys
		for (int i=0;i<numberlocations;i++){
			int homePolyid=functionsOld.determineHomePoly(poscoords, lonlats, jdbc, tablegeom);
			homePolys.idPoly.set(i,homePolyid);
			System.out.println("HomePoly:"+i+homePolyid);
			poscoords=poscoords+2;
		}
		
		//claculate distances and neighbours
		double distances[] = new double[numberlocations];
		for (int i=0; i<polyDistances.ids.size();i++){
//			System.out.println(i);
			poscoords=0;
			String geometry = polygones.geometry.get(i);
			for (int j=0; j<numberlocations;j++){
				
				
				distances[j] = functionsOld.calculateDistanceToCentroid(poscoords, geometry, jdbc, lonlats);
				
//				distances[j] = functions.calculateDistance(poscoords, geometry, jdbc, lonlats);
				polyDistances.distances[j].add(distances[j]);
				
//				if (distances[j]==0){
//					homePolys.idPoly.set(j, polygones.ids.get(i));
//				}
				
				poscoords=poscoords+2;
			}
				
			int locMinDist = 0;
			double minDistance = distances[0];
			for (int j=1;j<numberlocations;j++){
				if (distances[j]<minDistance){
					locMinDist=j;
					minDistance=distances[j];
				}
			}
				
			int polyID=polyDistances.ids.get(i).intValue();
			polyNeighbours.polyIds.add(polyID);
			
			ResultSet nN = functionsOld.getNearestNeighbours(polyID, tablegeom, jdbc);
			boolean last=false;
			while (!last){
				nN.next();
				polyNeighbours.neighbours[i].add(nN.getInt(1));
				if (nN.isLast()){last=true;}
			}
			
			System.out.println("write "+polyDistances.ids.get(i)+" to "+(locMinDist+1));
			allocPolys[locMinDist].add(polyDistances.ids.get(i));
			geomAllocPolys[locMinDist].add(geometry);

			System.out.println(i);
		}

		
		//sum value of criteria of all assigned polygons
		for (int i=0;i<numberlocations;i++){
				for (int j=0;j<allocPolys[i].size();j++){
					criteria=functionsOld.addToCriteria(allocPolys[i].get(j), i+1, -1, criteria, false, polygones.ids, polygones.criteria);
				}
			}
		
	}
	
	public static void main(String[] args)
	throws Exception {

		boolean PLZ5=true;
		boolean common=true;
		
		long time = System.currentTimeMillis();
		
		//setLocations
		int numberlocations;
		if (common){
				numberlocations =10;
			}
			else{
				numberlocations =55;
			}
		lonlats= new double[numberlocations*2];
		lonlats=functionsOld.setLocations(numberlocations, PLZ5, common);
		
		//create FileWriter
//		FileWriter output = functions.createFileWriter();
//		functions.createFileWriterLocs(numberlocations, lonlats);
		
		FileWriter output =functionsOld.createFileWriter();
		functionsOld.createFileWriterLocs(numberlocations, lonlats);

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
		int numberpolygons=functionsOld.getNrOrSum(true, PLZ5);
		functionsOld.setnumberPolygons(numberpolygons);
		polyNeighbours.polyIds = new ArrayList<Integer>();
		polyNeighbours.neighbours = (ArrayList<Integer>[])new ArrayList[numberpolygons];
		for (int i=0; i<polyNeighbours.neighbours.length;i++) polyNeighbours.neighbours[i]=new ArrayList<Integer>();
		
		homePolys.idLoc = new ArrayList<Integer>();
		homePolys.idPoly = new ArrayList<Integer>();
		
		for (int i=0;i<numberlocations;i++){
			homePolys.idLoc.add(i);
			homePolys.idPoly.add(-1);
		}
		
//		//alocate Polygons to locations
		polygones.ids = new ArrayList<Integer>();
		polygones.geometry = new ArrayList<String>();
		polygones.criteria = new ArrayList<Double>();
		polyDistances.ids = new ArrayList<Integer>();
		polyDistances.distances = (ArrayList<Double>[])new ArrayList[numberpolygons];
		for (int i=0; i<polyDistances.distances.length;i++) polyDistances.distances[i]=new ArrayList<Double>();
		
		
		getPolygones(numberlocations, numberpolygons, criteria, PLZ5);
		
		functionsOld.setCriteria(criteria, numberlocations);
		functionsOld.setGeometryAllocPolys(geomAllocPolys);
		functionsOld.setHomePolys(homePolys.idPoly);
		allocPolys=functionsOld.checkthresholdCombi(numberlocations, allocPolys, polyDistances.ids, polyDistances.distances, polygones.ids, polygones.criteria, polyNeighbours.polyIds, polyNeighbours.neighbours);
		geomAllocPolys=functionsOld.getGeometryAllocPolys();
		criteria=functionsOld.getCriteria();
		
//		//Create Shapefile with allocated poylgons
		for (int i=0; i<numberlocations;i++){
			functionsOld.writePolygon(output, allocPolys[i], geomAllocPolys[i],i+1);
		}
		
//		System.out.println("1:"+criteria[0]+",2:"+criteria[1]+"3:"+criteria[2]+"4:"+criteria[3]);
//		System.out.println("1:"+criteria[0]+",2:"+criteria[1]);
		System.out.println("Time for whole algorithm:"+(System.currentTimeMillis()-time)+" ms");
//		
		output.flush();
		output.close();
	    
	    System.out.println("successfully ended");
}
}
