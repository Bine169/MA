package whitespotGreenfield;

public class PolygonExtended extends Polygon{
	private Double[] centroidPoint= new Double[2];
	private Double distance;

	public Double[] getCentroid(){
		return this.centroidPoint;
	}
	
	public Double getDistance(){
		return this.distance;
	}
	
	public void setCentroid(double lon, double lat){
		this.centroidPoint[0]=lon;
		this.centroidPoint[1]=lat;
	}
	
	public void setDistance(double dist){
		this.distance=dist;
	}
}
