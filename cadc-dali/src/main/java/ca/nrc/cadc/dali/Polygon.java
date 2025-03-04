/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2025.                            (c) 2025.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*
*  NRC disclaims any warranties,        Le CNRC dénie toute garantie
*  expressed, implied, or               énoncée, implicite ou légale,
*  statutory, of any kind with          de quelque nature que ce
*  respect to the software,             soit, concernant le logiciel,
*  including without limitation         y compris sans restriction
*  any warranty of merchantability      toute garantie de valeur
*  or fitness for a particular          marchande ou de pertinence
*  purpose. NRC shall not be            pour un usage particulier.
*  liable in any event for any          Le CNRC ne pourra en aucun cas
*  damages, whether direct or           être tenu responsable de tout
*  indirect, special or general,        dommage, direct ou indirect,
*  consequential or incidental,         particulier ou général,
*  arising from the use of the          accessoire ou fortuit, résultant
*  software.  Neither the name          de l'utilisation du logiciel. Ni
*  of the National Research             le nom du Conseil National de
*  Council of Canada nor the            Recherches du Canada ni les noms
*  names of its contributors may        de ses  participants ne peuvent
*  be used to endorse or promote        être utilisés pour approuver ou
*  products derived from this           promouvoir les produits dérivés
*  software without specific prior      de ce logiciel sans autorisation
*  written permission.                  préalable et particulière
*                                       par écrit.
*
*  This file is part of the             Ce fichier fait partie du projet
*  OpenCADC project.                    OpenCADC.
*
*  OpenCADC is free software:           OpenCADC est un logiciel libre ;
*  you can redistribute it and/or       vous pouvez le redistribuer ou le
*  modify it under the terms of         modifier suivant les termes de
*  the GNU Affero General Public        la “GNU Affero General Public
*  License as published by the          License” telle que publiée
*  Free Software Foundation,            par la Free Software Foundation
*  either version 3 of the              : soit la version 3 de cette
*  License, or (at your option)         licence, soit (à votre gré)
*  any later version.                   toute version ultérieure.
*
*  OpenCADC is distributed in the       OpenCADC est distribué
*  hope that it will be useful,         dans l’espoir qu’il vous
*  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
*  without even the implied             GARANTIE : sans même la garantie
*  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
*  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
*  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
*  General Public License for           Générale Publique GNU Affero
*  more details.                        pour plus de détails.
*
*  You should have received             Vous devriez avoir reçu une
*  a copy of the GNU Affero             copie de la Licence Générale
*  General Public License along         Publique GNU Affero avec
*  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
*  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
*                                       <http://www.gnu.org/licenses/>.
*
*  $Revision: 5 $
*
************************************************************************
 */

package ca.nrc.cadc.dali;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * DALI polygon class with port of CAOM-2.4 polygon validation code. Note: the code
 * in this class assumes that polygons are small (flat) enough that cartesian math is
 * a reasonable approximation and it will give wrong answers for polygons that are too
 * large (10s of degrees across??) and especially for those larger than half the sphere.
 *
 * @author pdowler
 */
public class Polygon implements Shape {

    private static final Logger log = Logger.getLogger(Polygon.class);

    private final List<Point> points = new ArrayList<>();

    // lazily computed
    private transient Point center;
    private transient Double area;
    private transient Double span;
    private transient Boolean ccw;

    public Polygon() {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Polygon[");
        for (Point v : points) {
            sb.append(v.getLongitude()).append(" ").append(v.getLatitude()).append(" ");
        }
        sb.setCharAt(sb.length() - 1, ']');
        return sb.toString();
    }

    public List<Point> getVertices() {
        return points;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        Polygon rhs = (Polygon) obj;

        if (this.points.size() != rhs.points.size()) {
            return false;
        }
        for (int i = 0; i < points.size(); i++) {
            Point tp = this.points.get(i);
            Point rp = rhs.points.get(i);
            if (!tp.equals(rp)) {
                return false;
            }
        }
        return true;
    }

    public double[] toArray() {
        double[] arr = new double[points.size() * 2];
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            arr[i * 2] = p.getLongitude();
            arr[i * 2 + 1] = p.getLatitude();
        }
        return arr;
    }

    /**
     * Validate this polygon for conformance to IVOA DALI polygon rules.
     *
     * @throws InvalidPolygonException violation of DALI spec
     */
    public final void validate() throws InvalidPolygonException {
        validatePoints();
        validateSegments();
        initProps();
        // DALI polygons are always CCW 
        // unsupported: if we detect CW here it is equivalent to the region 
        // outside with area = 4*pi - area and larger than half the sphere
        //if (!ccw) {
        //    throw new InvalidPolygonException("clockwise winding direction");
        //}
    }

    /**
     * Check if the polygon is counter-clockwise.
     *
     * @return true if counter-clockwise (valid), false is clockwise (invalid)
     */
    public boolean getCounterClockwise() {
        if (ccw == null) {
            initProps();
        }
        return ccw;
    }

    @Override
    public Point getCenter() {
        if (center == null) {
            initProps();
        }
        return center;
    }

    @Override
    public double getArea() {
        if (area == null) {
            initProps();
        }
        return area;
    }

    public double getSize() {
        if (span == null) {
            initProps();
        }
        return span;
    }

    /**
     * Reverse the vertex order aka swap winding direction.
     *
     * @param in input polygon
     * @return flipped polygon
     */
    public static Polygon flip(Polygon in) {
        Polygon ret = new Polygon();
        for (int i = in.getVertices().size() - 1; i >= 0; i--) {
            Point p = in.getVertices().get(i);
            ret.getVertices().add(p);
        }
        return ret;
    }

    private void initProps() {
        PolygonProperties pp = computePolygonProperties();
        if (pp.windCounterClockwise) {
            this.area = pp.area;
            this.center = pp.center;
            this.span = pp.span;
            this.ccw = pp.windCounterClockwise;
        } else {
            // TBD: instead of clockwise being invalid, inside-on-the-left can
            // be interpretted as the other large part of the unit sphere
            
            // 1 steradian == (180/PI)^2 sq. deg
            double rd = (180.0 / Math.PI);
            double ster = rd * rd;
            this.area = 4.0 * Math.PI * ster - pp.area;
            double clong = pp.center.getLongitude() + 180.0;
            if (clong < 0.0) {
                clong += 360.0;
            } else if (clong > 360.0) {
                clong -= 360.0;
            }
            double clat = -1.0 * pp.center.getLatitude();
            this.center = new Point(clong, clat);
            this.span = 360.0 - pp.span;
            this.ccw = pp.windCounterClockwise;
        }
    }

    private static class PolygonProperties {
        boolean windCounterClockwise;
        Double area;
        Point center;
        Double span;
    }

    private PolygonProperties computePolygonProperties() {
        // log.debug("computePolygonProperties: " + poly);
        // the transform needed for computing things in long/lat using cartesian
        // approximation
        CartesianTransform trans = CartesianTransform.getTransform(this);
        Polygon tpoly = trans.transform(this);
        log.debug("tpoly: " + tpoly);
        
        // algorithm from
        // http://astronomy.swin.edu.au/~pbourke/geometry/polyarea/
        double a = 0.0;
        double cx = 0.0;
        double cy = 0.0;
        Iterator<Point> pi = tpoly.getVertices().iterator();
        Point start = pi.next();
        Point v1 = start;
        while (pi.hasNext()) {
            Point v2 = pi.next();
            //log.warn("props: " + v1 + " " + v2);
            double tmp = v1.getLongitude() * v2.getLatitude() - v2.getLongitude() * v1.getLatitude();
            a += tmp;
            cx += (v1.getLongitude() + v2.getLongitude()) * tmp;
            cy += (v1.getLatitude() + v2.getLatitude()) * tmp;
            v1 = v2;
            if (!pi.hasNext()) {
                // implicit closing segment
                v2 = start;
                //log.warn("props/implicit close: " + v1 + " " + v2);
                tmp = v1.getLongitude() * v2.getLatitude() - v2.getLongitude() * v1.getLatitude();
                a += tmp;
                cx += (v1.getLongitude() + v2.getLongitude()) * tmp;
                cy += (v1.getLatitude() + v2.getLatitude()) * tmp;
            }
        }

        //log.warn("raw props: " + cx + "," + cy + " a=" + a);
        a *= 0.5;
        cx = cx / (6.0 * a);
        cy = cy / (6.0 * a);
        log.debug("props: " + cx + "," + cy + " a=" + a);

        // quick and dirty size computation
        double d = 0.0;
        for (int i = 0; i < tpoly.getVertices().size(); i++) {
            Point vi = tpoly.getVertices().get(i);
            for (int j = 0; j < tpoly.getVertices().size(); j++) {
                Point vj = tpoly.getVertices().get(j);
                double dd = Math.sqrt(distanceSquared(v1, vj));
                if (dd > d) {
                    d = dd;
                }
            }
        }

        PolygonProperties ret = new PolygonProperties();
        ret.windCounterClockwise = (a < 0.0); // RA-DEC increases left-up
        log.debug("a: " + a + " ccw: " + ret.windCounterClockwise);
        if (a < 0.0) {
            a *= -1.0;
        }
        ret.area = a;

        CartesianTransform inv = trans.getInverseTransform();
        //log.debug("transform: " + cx + "," + cy + " with " + inv);
        ret.center = inv.transform(new Point(cx, cy));

        ret.span = d;

        return ret;
    }

    private void validatePoints() throws InvalidPolygonException {
        if (points.size() < 3) {
            throw new InvalidPolygonException(
                    "polygon has " + points.size() + " points: minimum 3");
        }
        StringBuilder msg = new StringBuilder("invalid Polygon: ");
        for (Point p : points) {
            boolean flong = p.getLongitude() < 0.0 || p.getLongitude() > 360.0;
            boolean flat = p.getLatitude() < -90.0 || p.getLatitude() > 90.0;
            if (flong && flat) {
                msg.append("longitude,latitude not in [0,360],[-90,90]: ").append(p.getLongitude()).append(",").append(p.getLatitude());
            } else if (flong) {
                msg.append("longitude not in [0,360]: ").append(p.getLongitude());
            } else if (flat) {
                msg.append("latitude not in [-90,90]: ").append(p.getLatitude());
            }
            if (flong || flat) {
                throw new InvalidPolygonException(msg.toString());
            }
        }
    }

    private void validateSegments() throws InvalidPolygonException {
        CartesianTransform trans = CartesianTransform.getTransform(this);
        Polygon tpoly = trans.transform(this);

        Iterator<Point> vi = tpoly.getVertices().iterator();
        List<Segment> tsegs = new ArrayList<>();
        List<Segment> psegs = tsegs;
        Point start = vi.next();
        Point v1 = start;
        while (vi.hasNext()) {
            Point v2 = vi.next();
            Segment s = new Segment(v1, v2);
            //log.warn("[validateSegments] tseg: " + s);
            tsegs.add(s);
            v1 = v2;
            if (!vi.hasNext()) {
                v2 = start;
                s = new Segment(v1, v2);
                //log.warn("[validateSegments] implicit tseg: " + s);
                tsegs.add(s);
            }
        }
        if (this != tpoly) {
            // make segments with orig coords for reporting
            vi = this.getVertices().iterator();
            psegs = new ArrayList<>();
            start = vi.next();
            v1 = start;
            while (vi.hasNext()) {
                Point v2 = vi.next();
                Segment s = new Segment(v1, v2);
                //log.warn("[validateSegments] pseg: " + s);
                psegs.add(s);
                v1 = v2;
                if (!vi.hasNext()) {
                    v2 = start;
                    s = new Segment(v1, v2);
                    //log.warn("[validateSegments] implicit pseg: " + s);
                    psegs.add(s);
                }

            }
        }
        intersects(tsegs, psegs);
    }

    private static void intersects(List<Segment> transSegments, List<Segment> origSegments) throws InvalidPolygonException {
        for (int i = 0; i < transSegments.size(); i++) {
            Segment s1 = transSegments.get(i);
            for (int j = 0; j < transSegments.size(); j++) {
                if (i != j) {
                    Segment s2 = transSegments.get(j);
                    if (intersects(s1, s2)) {
                        Segment r1 = origSegments.get(i);
                        Segment r2 = origSegments.get(j);
                        throw new InvalidPolygonException("invalid Polygon: segment intersect " + r1 + " vs " + r2);
                    }
                }
            }
        }
    }

    private static boolean intersects(Segment ab, Segment cd) {
        //log.debug("intersects: " + ab + " vs " + cd);
        // rden = (Bx-Ax)(Dy-Cy)-(By-Ay)(Dx-Cx)
        double den = (ab.v2.getLongitude() - ab.v1.getLongitude()) * (cd.v2.getLatitude() - cd.v1.getLatitude())
                - (ab.v2.getLatitude() - ab.v1.getLatitude()) * (cd.v2.getLongitude() - cd.v1.getLongitude());
        //log.debug("den = " + den);

        //rnum = (Ay-Cy)(Dx-Cx)-(Ax-Cx)(Dy-Cy)
        double rnum = (ab.v1.getLatitude() - cd.v1.getLatitude()) * (cd.v2.getLongitude() - cd.v1.getLongitude())
                - (ab.v1.getLongitude() - cd.v1.getLongitude()) * (cd.v2.getLatitude() - cd.v1.getLatitude());
        //log.debug("rnum = " + rnum);

        if (Math.abs(den) < 1.0e-12) { //(den == 0.0)
            if (Math.abs(rnum) < 1.0e-12) { //(rnum == 0.0)
                // colinear: check overlap on one axis
                if (ab.v2 == cd.v1 || ab.v1 == cd.v2) {
                    return false; // end-to-end
                }
                double len1 = ab.lengthSquared();
                double len2 = cd.lengthSquared();
                Segment s = ab;
                if (len2 > len1) {
                    s = cd; // the longer one
                }
                double dx = Math.abs(s.v1.getLongitude() - s.v2.getLongitude());
                double dy = Math.abs(s.v1.getLatitude() - s.v2.getLatitude());
                if (dx > dy) { // more horizontal = project to coordX
                    if (ab.v2.getLongitude() < cd.v1.getLongitude()) {
                        return false; // ab left of cd
                    }
                    if (ab.v1.getLongitude() > cd.v2.getLongitude()) {
                        return false; // ab right of cd
                    }
                } else { // more vertical = project to coordY
                    if (ab.v2.getLatitude() < cd.v1.getLatitude()) {
                        return false; // ab below cd
                    }
                    if (ab.v1.getLatitude() > cd.v2.getLatitude()) {
                        return false; // ab above cd
                    }
                }
                return true; // overlapping
            }
            return false; // just parallel
        }

        double r = rnum / den;
        //log.debug("radius = " + radius);
        // no intersect, =0 or 1 means the ends touch, which is normal  but pg_sphere doesn't like it
        //if (radius < 0.0 || radius > 1.0)
        if (r <= 0.0 || r >= 1.0) {
            return false;
        }

        //snum = (Ay-Cy)(Bx-Ax)-(Ax-Cx)(By-Ay)
        double snum = (ab.v1.getLatitude() - cd.v1.getLatitude()) * (ab.v2.getLongitude() - ab.v1.getLongitude())
                - (ab.v1.getLongitude() - cd.v1.getLongitude()) * (ab.v2.getLatitude() - ab.v1.getLatitude());
        //log.debug("snum = " + snum);

        double s = snum / den;
        //log.debug("s = " + s);
        //if (s < 0.0 || s > 1.0)
        if (s <= 0.0 || s >= 1.0) {
            return false; // no intersect, =0 or 1 means the ends touch, which is normal
        }

        // radius in [0,1] and s in [0,1] = intersects
        return true;
    }

    private static class Segment {

        Point v1;
        Point v2;

        Segment(Point v1, Point v2) {
            this.v1 = v1;
            this.v2 = v2;
        }

        double length() {
            return Math.sqrt(lengthSquared());
        }

        double lengthSquared() {
            return distanceSquared(v1, v2);
        }

        @Override
        public String toString() {
            return "Segment[" + v1.getLongitude() + "," + v1.getLatitude() + ":" + v2.getLongitude() + "," + v2.getLatitude() + "]";
        }
    }

    private static double distanceSquared(Point v1, Point v2) {
        return (v1.getLongitude() - v2.getLongitude()) * (v1.getLongitude() - v2.getLongitude())
                + (v1.getLatitude() - v2.getLatitude()) * (v1.getLatitude() - v2.getLatitude());
    }
}
