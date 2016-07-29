package it.unical.scalab.groi;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.context.jts.JtsSpatialContext;
import com.spatial4j.core.shape.jts.JtsGeometry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;

/**
 * @author <a href="mailto:fmarozzo@dimes.unical.it">Fabrizio Marozzo</a>
 * @author <a href="mailto:lbelcastro@dimes.unical.it">Loris Belcastro</a>
 * @version 1.0
 * 
 * 
 * This class implements the G-RoI algorithm.
 * If you are using the source code, please cite the paper:
 * L.Belcastro, F. Marozzo, D. Talia, P. Trunfio. G-RoI: Automatic Region-of-Interest detection driven by geotagged social media data. Under Review. 2016.
 * 
 * This code is open source software issued under the <a href="https://www.gnu.org/licenses/gpl.html">GNU General Public License </a>.	
 */
public class GRoI {
	final static SpatialContext ctx = SpatialContext.GEO;

	/**
	 *  Approximated degrees of latitude for a 1 meter length in the Cartesian coordinates system;
	 */
	private static final double STEP_1M_Y = 8.992909382672273E-6;
	/**
	 * Approximated degrees of longitude for a 1 meter length in the Cartesian coordinates system;
	 */
	private static final double STEP_1M_X = 1.2080663828690774E-5;

	/**
	 * The surface area of ​​the Earth covered by a square degree  
	 */
	final static double SURFACE_AREA_ONE_SQUARE_DEGREE = 12365.1613;
	
	/**
	 * The implementation of the G-RoI reduction procedure using default values. More details in the paper mentioned above.
	 * @param coordinates A list of coordinates of locations from which users have created geotagged items referring to a $poi$.
	 */
	public static LinkedList<ConvexPolygon> gRoIReduction(LinkedList<Coordinate> coordinates) {

		return gRoIReduction(coordinates, 0, 8, 2);
	}
	
	/**
	 * The implementation of the G-RoI reduction procedure. More details in the paper mentioned above.
	 * @param coordinates A list of location coordinates from which users have created geotagged items referring to a $poi$.
	 * @param pointsWithSameCoordinates The maximum allowed number of geotagged items with the same coordinates. -1 to be unlimited. 
	 * @param cellWidth To aggregate geotagged items in squared cells with a side of $cellWidth$ (in meter). Default 8. -1 to do not aggregate. 
	 * @param minCellSupport To filter out cells that do not reach the minCellSupport. -1 to do not filter.
	 * @return The list of convex polygons CP obtained after the n steps that have been performed.
	 */
	public static LinkedList<ConvexPolygon> gRoIReduction(LinkedList<Coordinate> coordinates, int pointsWithSameCoordinates, double cellWidth,
			int minCellSupport) {

		System.out.print("G-RoI reduction start...");
		HashMap<Coordinate, Integer> locations = filterCoordinates(coordinates, pointsWithSameCoordinates, cellWidth, minCellSupport);

		JtsGeometry convexPol0 = convexHull(locations.keySet());
		LinkedList<ConvexPolygon> convexPolygons = new LinkedList<ConvexPolygon>();

		// Current variables
		JtsGeometry currConvexPol = convexPol0;
		int currTotSupport = 0;
		HashSet<Coordinate> currLocations = new HashSet<Coordinate>();
		for (Entry<Coordinate, Integer> entry : locations.entrySet()) {
			currLocations.add(entry.getKey());
			currTotSupport += entry.getValue();
		}
		convexPolygons.add(new ConvexPolygon(convexPol0, currTotSupport));

		// maxVariables
		double maxDensity = 0;
		JtsGeometry maxConvexPol = null;
		Coordinate delVertex = null;

		// tmpVariables
		JtsGeometry tmpConvexPol;
		double tmpArea;
		double tmpDensity;

		do {
			maxDensity = 0;
			tmpConvexPol = null;
			delVertex = null;

			for (Coordinate tmpVertex : currConvexPol.getGeom().getCoordinates()) {
				currLocations.remove(tmpVertex);
				tmpConvexPol = convexHull(currLocations);
				tmpArea = tmpConvexPol.getArea(ctx);
				if (tmpArea > 0) {
					tmpDensity = (currTotSupport - locations.get(tmpVertex)) / tmpArea;
					if (tmpDensity > maxDensity) {
						maxDensity = tmpDensity;
						maxConvexPol = tmpConvexPol;
						delVertex = tmpVertex;
					}// max density
				}// Area>0
				currLocations.add(tmpVertex);
			}// for

			if (maxDensity > 0) {
				convexPolygons.add(new ConvexPolygon(maxConvexPol, currTotSupport));
				currConvexPol = maxConvexPol;
				currLocations.remove(delVertex);
				currTotSupport -= locations.get(delVertex);
			}
		} while (maxDensity > 0);
		System.out.println(" end");
		return convexPolygons;
	}	

	/**
	 * A method for generating a convex polygon from a given collection of location coordinates.
	 * @param points A collections of location coordinates
	 * @return A convex polygon
	 */
	private static JtsGeometry convexHull(Collection<Coordinate> points) {
		GeometryFactory geometryFactory = new GeometryFactory();
		MultiPoint mp = geometryFactory.createMultiPoint(points.toArray(new Coordinate[0]));
		Geometry geoHull = mp.convexHull();
		return new JtsGeometry(geoHull, JtsSpatialContext.GEO, false, false);
	}

	/**
	 * Filter and aggregate location coordinates.
	 * @param coordinates A list of location coordinates from which users have created geotagged items referring to a $poi$.
	 * @param pointsWithSameCoordinates The maximum allowed number of geotagged items with the same coordinates. -1 to be unlimited. 
	 * @param cellWidth To aggregate geotagged items in squared cells with a side of $cellWidth$ (in meter). Default 8. -1 to do not aggregate. 
	 * @param minCellSupport To filter out cells that do not reach the minCellSupport. -1 to do not filter.
	 * @return The location coordinates filtered and aggregated.
	 */
	private static HashMap<Coordinate, Integer> filterCoordinates(LinkedList<Coordinate> coordinates, int pointsWithSameCoordinates,
			double cellWidth, int minCellSupport) {

		HashMap<Coordinate, Integer> locations = new HashMap<Coordinate, Integer>();

		double stepX = STEP_1M_X * cellWidth;
		double stepY = STEP_1M_Y * cellWidth;
		double keyLatitude;
		double keyLongitude;

		// To delete geotagged items with same coordinates
		HashMap<Coordinate, Integer> abnormals = new HashMap<Coordinate, Integer>();

		loop: for (Coordinate tmpCoo : coordinates) {
			if (pointsWithSameCoordinates > 0) {
				if (abnormals.containsKey(tmpCoo) && abnormals.get(tmpCoo) >= pointsWithSameCoordinates)
					continue loop;
				else
					abnormals.put(tmpCoo, abnormals.containsKey(tmpCoo) ? (abnormals.get(tmpCoo) + 1) : 1);
			}

			if (cellWidth > 0) {
				keyLongitude = ((int) (tmpCoo.x / stepX)) * stepX;
				keyLatitude = ((int) (tmpCoo.y / stepY)) * stepY;
				tmpCoo = new Coordinate(keyLongitude, keyLatitude);
			}// if
			locations.put(tmpCoo, locations.containsKey(tmpCoo) ? (locations.get(tmpCoo) + 1) : 1);
		}

		// To delete geotagged items with low support
		if (cellWidth > 0 && minCellSupport > 1) {
			Iterator<Entry<Coordinate, Integer>> it = locations.entrySet().iterator();
			while (it.hasNext()) {
				Entry<Coordinate, Integer> pair = it.next();
				if (pair.getValue() < minCellSupport)
					it.remove();
			}
		}
		return locations;
	}

	/**
	 * The implementation of the G-RoI selection procedure. More details in the paper mentioned above.
	 * @param shapes The list of convex polygons CP returned by G-RoI reduction procedure.
	 * @param threshold_perc A threshold value between 0 and 1
	 * @return A suitable RoI that describes the boundaries of the $poi$'s area.
	 */
	public static JtsGeometry gRoISelection(LinkedList<ConvexPolygon> shapes, double threshold_perc) {
		if (threshold_perc < 0 || threshold_perc > 1)
			throw new IllegalArgumentException("Threashold has to be a value between 0 and 1. Default 0.2");

		System.out.print("G-RoI selection start...");
		int dim = shapes.size();

		double x[] = new double[dim];
		double y[] = new double[dim];

		int count = 0;
		for (ConvexPolygon roi : shapes) {
			x[count] = count;
			y[count] = roi.getShape().getArea(ctx) * SURFACE_AREA_ONE_SQUARE_DEGREE;
			count++;
		}

		// iterative variables
		int cut = 0;
		// max variables
		int iMax = 0;
		double distMax = 0;

		// temporary variables
		double xNorm = -1;
		double yNorm = -1;
		double tmpDist = 0;

		do {
			distMax = 0;
			iMax = cut;

			for (int i = cut + 1; i < dim - 1; i++) {
				xNorm = (x[i] - x[cut]) / (x[dim - 1] - x[cut]);
				yNorm = (y[i] - y[dim - 1]) / (y[cut] - y[dim - 1]);

				if (yNorm < (1 - threshold_perc - xNorm)) {
					tmpDist = ((1 - yNorm) - xNorm) * Math.sqrt(2) / 2.0;
					if (tmpDist > distMax) {
						distMax = tmpDist;
						iMax = i;
					}
				}
			}// for
			if (distMax > 0) {
				cut = iMax;
			}
		} while (distMax > 0);
		System.out.println(" end");
		System.out.println("G-RoI procedure found a suitable roi (CP["+cut+"])");
		System.out.println();

		return shapes.get(cut).getShape();

	}
	
}

