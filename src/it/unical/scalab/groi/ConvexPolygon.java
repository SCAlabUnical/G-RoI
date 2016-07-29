package it.unical.scalab.groi;

import com.spatial4j.core.shape.jts.JtsGeometry;



/**
 * @author <a href="mailto:fmarozzo@dimes.unical.it">Fabrizio Marozzo</a>
 * @author <a href="mailto:lbelcastro@dimes.unical.it">Loris Belcastro</a>
 * @version 1.0
 * 
 * A convex polygon. 
 * This code is open source software issued under the <a href="https://www.gnu.org/licenses/gpl.html">GNU General Public License </a>.		
 */

public class ConvexPolygon {
	private JtsGeometry shape;
	private int support;
	/**
	 * 
	 * @param shape The boundaries of the convex polygon
	 * @param support The number of geotagged items enclosed by the polygon
	 */
	public ConvexPolygon(JtsGeometry shape, int support) {
		this.shape = shape;
		this.support = support;
	}
	public double getSupport() {
		return support;
	}
	public void setSupport(int support) {
		this.support = support;
	}
	public JtsGeometry getShape() {
		return shape;
	}
	public void setShape(JtsGeometry shape) {
		this.shape = shape;
	}
}
