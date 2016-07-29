package it.unical.scalab.groi.main;

import it.unical.scalab.groi.ConvexPolygon;
import it.unical.scalab.groi.GRoI;
import it.unical.scalab.groi.util.KMLUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;

import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.jts.JtsGeometry;
import com.vividsolutions.jts.geom.Coordinate;
/***
 * 
 * This main shows how G-RoI algoritm can be excute on a a set of input.
 * Each input is a CSV file, containing location coordinates of geotagged items refferring to a $poi$.
 * The location coordinates are in the format <x,y> or <longitude, latitude>.
 * 
 * To visualize the output file GRoI-output.kml on a map can be used 
 * the online service <a href="http://ivanrublev.me/kml/">http://ivanrublev.me/kml/</a>.
 *
 * This code is open source software issued 
 * under the <a href="https://www.gnu.org/licenses/gpl.html">GNU General Public License </a>.	
 */
public class Main {

	public static void main(String[] args) throws IOException {
		
		// A directory contained all CSV files to be processed. N.B.: A CSV file for each poi to be analyzed.
		File dir = new File("data//Paris");

		LinkedList<File> files = listFilesForFolder(dir);
		StringBuilder sb = new StringBuilder();

		double threshold = 0.27;

		for (File file : files) {
			System.out.println("Analyzing file " + file.getName());
			try {
				LinkedList<Coordinate> coordinates= loadFromFile(file);

				LinkedList<ConvexPolygon> shapes = GRoI.gRoIReduction(coordinates);

				JtsGeometry roi = GRoI.gRoISelection(shapes, threshold);
				
				HashMap<String, String> metadata = new HashMap<String, String>();
				metadata.put("name", file.getName());
				sb.append(KMLUtils.serialize((Shape) roi, false, metadata));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("Saving the kml file");
		try {
			PrintWriter pw = new PrintWriter(new File("GRoI-output.kml"));
			System.out.println("GRoI-output.kml ");
			pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>");
			pw.println(sb.toString());
			pw.println("</Document></kml>");
			pw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		/**
		 * To visualize the output file GRoI-output.kml on a map can be used 
		 * the online service <a href="http://ivanrublev.me/kml/">http://ivanrublev.me/kml/</a>.
		 *
		 */
	}// main

	private static LinkedList<File> listFilesForFolder(final File folder) {
		LinkedList<File> ret = new LinkedList<File>();

		for (final File fileEntry : folder.listFiles()) {
			if (!fileEntry.isDirectory()) {
				ret.add(fileEntry);
			}
		}
		return ret;
	}

	private static LinkedList<Coordinate> loadFromFile(File file) {

		try {
			FileInputStream fstream = new FileInputStream(file);
			@SuppressWarnings("resource")
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String strLine;

			LinkedList<Coordinate> list = new LinkedList<Coordinate>();
			int support = 0;
			while ((strLine = br.readLine()) != null) {
				String[] parts = strLine.split(",");
				if (parts.length == 2)
					list.add(new Coordinate(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])));
				else if (parts.length == 3) {
					support = Integer.parseInt(parts[2]);
					for (int i = 0; i < support; i++)
						list.add(new Coordinate(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])));
				}
			}
			return list;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
