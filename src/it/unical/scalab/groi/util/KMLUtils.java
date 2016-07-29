package it.unical.scalab.groi.util;



import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Rectangle;
import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.jts.JtsGeometry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Helper class used to translate polygons and lines in a KML format.
 * @author <a href="mailto:fmarozzo@dimes.unical.it">Fabrizio Marozzo</a>
 * @author <a href="mailto:lbelcastro@dimes.unical.it">Loris Belcastro</a>
 * @version 1.0
 * 
 * This code is open source software issued under the <a href="https://www.gnu.org/licenses/gpl.html">GNU General Public License </a>.	
 */
public class KMLUtils {

	public static String serialize(Shape shape) throws IOException {
		return serialize(shape, false, null);
	}

	public static String serialize(Shape shape, boolean closeFile, Map<String, String> ext) throws IOException {
		if (shape instanceof JtsGeometry) {
			Geometry geometry = ((JtsGeometry) shape).getGeom();
			if (geometry instanceof Point) {
				return serializePoint((Point) geometry, closeFile, ext);
			} else if (geometry instanceof LineString) {
				 return serializeLineString((LineString) geometry, closeFile, ext);
			} else if (geometry instanceof Polygon) {
				return serializePolygon((Polygon) geometry, closeFile, ext);
			} else {
				throw new IllegalArgumentException("Geometry type [" + geometry.getGeometryType() + "] not supported");
			}
		} else if (shape instanceof Point) {
			return serializePoint((Point) shape, closeFile, ext);
		} else if (shape instanceof Rectangle) {
			return serializeRectangle((Rectangle) shape, closeFile, ext);
		} else {
			throw new IllegalArgumentException("Shape type [" + shape.getClass().getSimpleName() + "] not supported");
		}
	}

	private static String serializeRectangle(Rectangle rectangle, boolean closeFile, Map<String, String> extendedData) {
		StringBuilder sb = new StringBuilder();

		if (closeFile) {
			sb.append(
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>");
		}

		double[][] points = new double[4][2];
		points[0][0] = rectangle.getMinX();
		points[0][1] = rectangle.getMinY();
		points[1][0] = rectangle.getMaxX();
		points[1][1] = rectangle.getMaxY();
		points[2][0] = rectangle.getMinX();
		points[2][1] = rectangle.getMaxY();
		points[3][0] = rectangle.getMaxX();
		points[3][1] = rectangle.getMinY();

		HashMap<String, String> ext = generateExtendedDataString(extendedData);
		sb.append("<Placemark>" + ext.get("data") + "<Polygon><outerBoundaryIs><LinearRing><coordinates>");
		sb.append(points[0][0] + "," + points[0][1] + " ");
		sb.append(points[3][0] + "," + points[3][1] + " ");
		sb.append(points[1][0] + "," + points[1][1] + " ");
		sb.append(points[2][0] + "," + points[2][1] + " ");
		sb.append("</coordinates></LinearRing></outerBoundaryIs></Polygon></Placemark>");
		if (ext.containsKey("style"))
			sb.append(ext.get("style"));

		if (closeFile) {
			sb.append("</Document></kml>");
		}

		return sb.toString();

	}

	private static String serializePolygon(Polygon geometry, boolean closeFile, Map<String, String> extendedData) {

		StringBuilder sb = new StringBuilder();

		if (closeFile) {
			sb.append(
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>");
		}

		HashMap<String, String> ext = generateExtendedDataString(extendedData);
		sb.append("<Placemark>" + ext.get("data")
				+ "<Polygon><outerBoundaryIs><LinearRing><tessellate>0</tessellate><coordinates>");
		Coordinate[] coordinates = geometry.getCoordinates();
		List<Coordinate> pointsList = new LinkedList<Coordinate>();

		double sumLat = 0;
		double sumLng = 0;
		for (Coordinate coordinate : coordinates) {
			pointsList.add(coordinate);
			sumLat += coordinate.y;
			sumLng += coordinate.x;
		}
		Coordinate reference = new Coordinate(sumLng / coordinates.length, sumLat / coordinates.length);
		Collections.sort(pointsList, new ClockwiseCoordinateComparator(reference));
		Coordinate tmp = pointsList.remove(coordinates.length - 1);
		pointsList.add(0, tmp);
		for (Coordinate c : pointsList) {
			sb.append(c.x + "," + c.y + ",0.0 ");
		}
		sb.append("</coordinates></LinearRing></outerBoundaryIs></Polygon></Placemark>");

		sb.append(ext.get("style"));

		if (closeFile) {
			sb.append("</Document></kml>");
		}

		return sb.toString();
	}

	private static String serializeLineString(LineString geometry, boolean closeFile, Map<String, String> extendedData) {
		StringBuilder sb = new StringBuilder();
		if (closeFile) {
			sb.append(
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>");
		}
		HashMap<String, String> ext = generateExtendedDataString(extendedData);
		sb.append("<Placemark>"+ext.get("data") +"<LineString><coordinates>");
		double[][] coordinates = getCoordinateMatrix(geometry);
		for (double[] ds : coordinates) {
			sb.append(ds[0] + "," + ds[1] + " ");
		}
		sb.append("</coordinates></LineString>");
		sb.append("</Placemark>");
		if (closeFile) {
			sb.append("</Document></kml>");
		}
		return sb.toString();

	}

	private static String serializePoint(Point geometry, boolean closeFile, Map<String, String> extendedData) {
		StringBuilder sb = new StringBuilder();
		if (closeFile) {
			sb.append(
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>");
		}
		HashMap<String, String> ext = generateExtendedDataString(extendedData);
		sb.append("<Placemark>" + ext.get("data") + "<Point><coordinates>" + geometry.getX() + "," + geometry.getY()
				+ "</coordinates></Point></Placemark>");
		if (closeFile) {
			sb.append("</Document></kml>");
		}
		return sb.toString();
	}

	private static HashMap<String, String> generateExtendedDataString(Map<String, String> extendedData) {
		String ext = "";
		String preText = "";
		HashMap<String, String> ret = new HashMap<String, String>();
		if (extendedData != null && extendedData.size() > 0) {
			ext = "<ExtendedData>";
			for (Map.Entry<String, String> entry : extendedData.entrySet()) {
				if (entry.getKey().equals("styleUrl") || entry.getKey().equals("description")) {
					continue;
				} else if (entry.getKey().equals("color")) {
					preText += "<styleUrl>#poly-" + entry.getValue().trim() + "</styleUrl>";
					String style = "<Style id=\"poly-" + entry.getValue().trim() + "\">" + "<LineStyle>" + "<color>"
							+ entry.getValue().trim() + "</color>" + "	<width>2</width>" + "</LineStyle>"
							+ "<PolyStyle>" + "<color>" + entry.getValue().trim() + "</color>" + "	<fill>1</fill>"
							+ "<outline>1</outline>" + "</PolyStyle></Style>";
					ret.put("style", style);

				} else if (entry.getKey().equals("description")) {
					preText += "<description><![CDATA[descrizione:" + entry.getValue() + "]]></description>";
				} else if (entry.getKey().equals("name")) {
					preText += "<name>" + entry.getValue() + "</name>";
				} else {
					ext += "<Data name=\"" + entry.getKey() + "\"><value>" + entry.getValue() + "</value></Data>";
				}
			}
			ext += "</ExtendedData>";
		}
		ret.put("data", preText + ext);
		return ret;
	}
	
	public static double[][] getCoordinateMatrix(Geometry geometry) {
		Coordinate[] points = geometry.getCoordinates();

		List<Coordinate> pointsList = new LinkedList<Coordinate>();
		double sumLat = 0;
		double sumLng = 0;
		for (Coordinate coordinate : points) {
			pointsList.add(coordinate);
			sumLat += coordinate.y;
			sumLng += coordinate.x;
		}

		Coordinate reference = new Coordinate(sumLng / points.length, sumLat / points.length);
		Collections.sort(pointsList, new ClockwiseCoordinateComparator(reference));
		Coordinate tmp = pointsList.remove(points.length - 1);
		pointsList.add(0, tmp);

		// Create the coordinate's matrix
		double[][] coordinates = new double[points.length][2];
		for (int i = 0; i < points.length; i++) {
			tmp = pointsList.get(i);
			coordinates[i][0] = tmp.x;
			coordinates[i][1] = tmp.y;
		}

		return coordinates;
	}

	public static double[][][] getCoordinateMatrixPolygon(Geometry geometry) {
		Coordinate[] points = geometry.getCoordinates();

		List<Coordinate> pointsList = new LinkedList<Coordinate>();
		double sumLat = 0;
		double sumLng = 0;
		for (Coordinate coordinate : points) {
			pointsList.add(coordinate);
			sumLat += coordinate.y;
			sumLng += coordinate.x;
		}

		Coordinate reference = new Coordinate(sumLng / points.length, sumLat / points.length);

		Collections.sort(pointsList, new ClockwiseCoordinateComparator(reference));

		Coordinate tmp = pointsList.remove(0);
		pointsList.add(tmp);

		// Create the coordinate's matrix
		double[][][] coordinates = new double[1][points.length][2];
		for (int i = 0; i < points.length; i++) {
			tmp = pointsList.get(i);
			coordinates[0][i][0] = tmp.x;
			coordinates[0][i][1] = tmp.y;
		}

		return coordinates;
	}


}

class ClockwisePointComparator implements Comparator<Point> {
	
	private Point reference;

	public ClockwisePointComparator(Point reference) {
		this.reference =reference;
	}
	

	public int compare(Point a, Point b) {
			// Variables to Store the atans
			double aTanA, aTanB;

			// Fetch the atans
			aTanA = Math.atan2(a.getY() - reference.getY(), a.getX() - reference.getX());
			aTanB = Math.atan2(b.getY() - reference.getY(), b.getX() - reference.getX());

			// Determine next point in Clockwise rotation
			if (aTanA < aTanB)
				return -1;
			else if (aTanB < aTanA)
				return 1;
			return 0;

	}

}

class ClockwiseCoordinateComparator implements Comparator<Coordinate> {

	private Coordinate reference;

	public ClockwiseCoordinateComparator(Coordinate reference) {
		this.reference = reference;
	}

	public int compare(Coordinate a, Coordinate b) {
		// Variables to Store the atans
		double aTanA, aTanB;

		// Fetch the atans
		aTanA = Math.atan2(a.y - reference.y, a.x - reference.x);
		aTanB = Math.atan2(b.y - reference.y, b.x - reference.x);

		// Determine next point in Clockwise rotation
		if (aTanA < aTanB)
			return -1;
		else if (aTanB < aTanA)
			return 1;
		return 0;

	}

}
