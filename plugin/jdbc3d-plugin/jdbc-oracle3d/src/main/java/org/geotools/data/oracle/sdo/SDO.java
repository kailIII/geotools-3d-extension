/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2003-2014, Open Source Geospatial Foundation (OSGeo)
 *    
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    Refractions Research Inc. Can be found on the web at:
 *    http://www.refractions.net/
 */
package org.geotools.data.oracle.sdo;

import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.geometry.iso.aggregate.MultiCurveImpl;
import org.geotools.geometry.iso.aggregate.MultiPointImpl;
import org.geotools.geometry.iso.aggregate.MultiPrimitiveImpl;
import org.geotools.geometry.iso.aggregate.MultiSurfaceImpl;
import org.geotools.geometry.iso.coordinate.DirectPositionImpl;
import org.geotools.geometry.iso.primitive.CurveImpl;
import org.geotools.geometry.iso.primitive.PointImpl;
import org.geotools.geometry.iso.primitive.SurfaceImpl;
import org.geotools.geometry.iso.root.GeometryImpl;
import org.geotools.geometry.iso.util.algorithm2D.CGAlgorithms;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;
import org.opengis.geometry.ISOGeometryBuilder;
import org.opengis.geometry.aggregate.MultiCurve;
import org.opengis.geometry.aggregate.MultiPoint;
import org.opengis.geometry.aggregate.MultiPrimitive;
import org.opengis.geometry.aggregate.MultiSurface;
import org.opengis.geometry.coordinate.LineSegment;
import org.opengis.geometry.coordinate.LineString;
import org.opengis.geometry.coordinate.PointArray;
import org.opengis.geometry.coordinate.Polygon;
import org.opengis.geometry.coordinate.Position;
import org.opengis.geometry.primitive.Curve;
import org.opengis.geometry.primitive.OrientableCurve;
import org.opengis.geometry.primitive.OrientableSurface;
import org.opengis.geometry.primitive.Point;
import org.opengis.geometry.primitive.Primitive;
import org.opengis.geometry.primitive.Ring;
import org.opengis.geometry.primitive.Shell;
import org.opengis.geometry.primitive.Solid;
import org.opengis.geometry.primitive.SolidBoundary;
import org.opengis.geometry.primitive.Surface;
import org.opengis.geometry.primitive.SurfaceBoundary;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Oracle Spatial Data Object utilities functions.
 * 
 * <p>
 * Provide utility functions for working with JTS Geometries in terms Oracle
 * Spatial Data Objects
 * </p>
 *
 * <p>
 * This class can be used for normal JTS Geometry persistence with little fuss
 * and bother - please see GeometryConverter for an example of this.
 * <p>
 * With a little fuss and bother LRS information can also be handled.
 * Although it is very rare that JTS makes use of such things. 
 * </p>
 * @see <a href="http://otn.oracle.com/pls/db10g/db10g.to_toc?pathname=appdev.101%2Fb10826%2Ftoc.htm&remark=portal+%28Unstructured+data%29">Spatial User's Guide (10.1)</a>
 * @author Jody Garnett, Refractions Reasearch Inc.
 * @author Taehoon Kim, Pusan National University
 *
 * @source $URL$
 * @version CVS Version
 *
 * @see net.refractions.jspatial.jts
 */
public final class SDO {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geotools.data.oracle.sdo");
    public static final int SRID_NULL = -1;

    /** Used to test for Counter Clockwise or Clockwise Linear Rings */
    //private static RobustCGAlgorithms clock = new RobustCGAlgorithms();

    // 
    // Encoding Helper Functions
    //

    /**
     * Produce SDO_GTYPE representing provided Geometry.
     * 
     * <p>
     * Encoding of Geometry type and dimension.
     * </p>
     * 
     * <p>
     * SDO_GTYPE defined as for digits <code>[d][l][tt]</code>:
     * </p>
     * 
     * <ul>
     * <li>
     * <b><code>d</code>:</b> number of dimensions (limited to 2,3, or 4)
     * </li>
     * <li>
     * <b><code>l</code>:</b> measure representation (ordinate 3 or 4) or 0 to
     * represent none/last
     * </li>
     * <li>
     * <b><code>tt:</code></b> See the TT. constants defined in this class
     * </li>
     * </ul>
     * 
     * <p>
     * Definition provided by Oracle Spatial User�s Guide and Reference.
     * </p>
     *
     * @param geom
     *
     */
    
    public static int gType(Geometry geom) {
        int d = D(geom) * 1000;
        int l = L(geom) * 100;
        int tt = TT(geom);

        return d + l + tt;
    }

    /**
     * Return D as defined by SDO_GTYPE (either 2,3 or 4).
     * 
     * <p>
     * For normal JTS Geometry this will be 2 or 3 depending if
     * geom.getCoordinate.z is Double.NaN.
     * </p>
     * 
     * <p>
     * Subclasses may override as required.
     * </p>
     *
     * @param geom
     *
     * @return <code>3</code>
     */

    public static int D(Geometry geom) {
    	return geom.getCoordinateDimension();
    }
    
    /**
     * Return L as defined by SDO_GTYPE (either 3,4 or 0).
     * 
     * <p>
     * L represents support for LRS (Liniar Referencing System?). JTS Geometry
     * objects do not support LRS so this will be 0.
     * </p>
     * 
     * <p>
     * Subclasses may override as required.
     * </p>
     *
     * @param geom
     *
     * @return <code>0</code>
     */
    public static int L(Geometry geom) {
        return 0;
    }

    /**
     * Return TT as defined by SDO_GTYPE (represents geometry type).
     * 
     * <p>
     * TT is used to represent the type of the JTS Geometry:
     * </p>
     * <pre><code>
     * <b>Value Geometry Type    JTS Geometry</b>
     * 00    UNKNOWN_GEOMETRY null
     * 01    POINT            Point
     * 02    LINE             LineString
     *       CURVE            <i>not supported</i>
     * 03    POLYGON          Polygon
     * 04    COLLECTION       GeometryCollection
     * 05    MULTIPOINT       MultiPoint
     * 06    MULTILINE        MultiLineString
     *       MULTICURVE       <i>not supported</i>
     * 07    MULTIPOLYGON     MultiPolygon
     * </code></pre>
     *
     * @param geom
     *
     * @return <code>TT</code> representing <code>geom</code>
     *
     * @throws IllegalArgumentException If SDO_GTYPE can not be represetned by
     *         JTS
     */
    
    public static int TT(Geometry geom) {
    	if (geom == null) {
            return TT.UNKNOWN; // UNKNOWN
    	} else if (geom instanceof Point) {
            return TT.POINT;
        } else if (geom instanceof Curve || geom instanceof OrientableCurve || geom instanceof LineString) {
            return TT.LINE;
        } else if (geom instanceof Surface || geom instanceof OrientableSurface||geom instanceof Polygon) {
            return TT.POLYGON;
        } else if (geom instanceof Solid) {
            return TT.SOLID;
        } else if (geom instanceof MultiPoint) {
            return TT.MULTIPOINT;
        } else if (geom instanceof MultiCurve) {
            return TT.MULTICURVE;
        } else if (geom instanceof MultiSurface) {
            return TT.MULTISURFACE;
        } else if (geom instanceof MultiPrimitive) {
            return TT.COLLECTION;
        } 
    	
    	throw new IllegalArgumentException("Cannot encode ISO "
                + geom.getClass().getTypeName() + " as SDO_GTYPE "
                + "(Limitied to Point, Curve, Surface, Solid "
                + "and aggregate geometry of primitive geometry)");
	}

    /**
     * Returns geometry SRID.
     * 
     * <p>
     * SRID code representing Spatial Reference System. SRID number used must
     * be defined in the Oracle MDSYS.CS_SRS table.
     * </p>
     * 
     * <p>
     * <code>SRID_NULL</code>represents lack of coordinate system.
     * </p>
     *
     * @param geom Geometry SRID Number (JTS14 uses GeometryFactor.getSRID() )
     *
     * @return <code>SRID</code> for provided geom
     */
    public static int SRID(Geometry geom) {
    	if(((GeometryImpl) geom).getUserData() != null)
    		return (int) ((GeometryImpl) geom).getUserData();
    	else
    		return 0;
        
    }

    /**
     * Return SDO_POINT_TYPE for geometry
     * 
     * <p>
     * Will return non null for Point objects. <code>null</code> is returned
     * for all non point objects.
     * </p>
     * 
     * <p>
     * You cannot use this with LRS Coordinates
     * </p>
     * 
     * <p>
     * Subclasses may wish to repress this method and force Points to be
     * represented using SDO_ORDINATES.
     * </p>
     *
     * @param geom
     *
     */
    public static double[] point(Geometry geom) {
        if (geom instanceof Point && (L(geom) == 0)) {
            Point point = (Point) geom;

            return point.getDirectPosition().getCoordinate();
        }

        // SDO_POINT_TYPE only used for non LRS Points
        return null;
    }

    /**
     * Return SDO_ELEM_INFO array for geometry
     * 
     * <p>
     * Describes how to use Ordinates to represent Geometry.
     * </p>
     * <pre><code><b>
     * # Name                Meaning</b>
     * 0 SDO_STARTING_OFFSET Offsets start at one
     * 1 SDO_ETYPE           Describes how ordinates are ordered
     * 2 SDO_INTERPRETATION  SDO_ETYPE: 4, 1005, or 2005
     *                       Number of triplets involved in compound geometry
     *                       
     *                       SDO_ETYPE: 1, 2, 1003, or 2003
     *                       Describes ordering of ordinates in geometry  
     * </code></pre>
     * 
     * <p>
     * For compound elements (SDO_ETYPE values 4 and 5) the last element of one
     * is the first element of the next.
     * </p>
     *
     * @param geom Geometry being represented
     *
     * @return Descriptionof Ordinates representation
     */
    public static int[] elemInfo(Geometry geom) {
        return elemInfo(geom, gType(geom));
    }
    
	@SuppressWarnings("rawtypes")
	public static int[] elemInfo(Geometry geom, final int GTYPE) {
        List list = new LinkedList();
        elemInfo(list, geom, 1, GTYPE);

        return intArray(list);
    }
	
	/**
     * Add to SDO_ELEM_INFO list for geometry and GTYPE.
     *
     * @param elemInfoList List used to gather SDO_ELEM_INFO
     * @param geom Geometry to encode
     * @param STARTING_OFFSET Starting offset in SDO_ORDINATES
     * @param GTYPE Encoding of dimension, measures and geometry type
     *
     * @throws IllegalArgumentException If geom cannot be encoded by ElemInfo
     */
    @SuppressWarnings("rawtypes")
	private static int elemInfo(List elemInfoList, Geometry geom,
        final int STARTING_OFFSET, final int GTYPE) {
        final int tt = TT(geom);
        int offset = 0;
        
        switch (tt) {
        case TT.POINT:
        	offset = addElemInfo(elemInfoList, (Point) geom, STARTING_OFFSET);
            return offset;

        case TT.LINE:
        	offset = addElemInfo(elemInfoList, (Curve) geom, STARTING_OFFSET);
            return offset;

        case TT.POLYGON:
        	offset = addElemInfo(elemInfoList, (Surface) geom, STARTING_OFFSET, GTYPE);
            return offset;

        case TT.MULTIPOINT:
        	offset = addElemInfo(elemInfoList, (MultiPoint) geom, STARTING_OFFSET);
        	return offset;

        case TT.MULTICURVE:
        	offset = addElemInfo(elemInfoList, (MultiCurve) geom, STARTING_OFFSET, GTYPE);
        	return offset;

        case TT.MULTISURFACE:
        	offset = addElemInfo(elemInfoList, (MultiSurface) geom, STARTING_OFFSET, GTYPE);
        	return offset;

        case TT.COLLECTION:
        	offset = addElemInfo(elemInfoList, (MultiPrimitive) geom, STARTING_OFFSET, GTYPE);
        	return offset;
          
        case TT.SOLID:
        	offset = addElemInfo(elemInfoList, (Solid) geom, STARTING_OFFSET, GTYPE);
        	return offset;
        }

        throw new IllegalArgumentException("Cannot encode ISO "
        		+ geom.getClass().getTypeName() + " as SDO_ELEM_INFO "
            + "(Limitied to Point, Line, Polygon, GeometryCollection, MultiPoint,"
            + " MultiLineString and MultiPolygon)");
    }

	/**
     * Not often called as POINT_TYPE prefered over ELEMINFO & ORDINATES.
     * 
     * <p>
     * This method is included to allow for multigeometry encoding.
     * </p>
     *
     * @param elemInfoList List containing ELEM_INFO array
     * @param point Point to encode
     * @param STARTING_OFFSET Starting offset in SDO_ORDINATE array
     */
    @SuppressWarnings("rawtypes")
	private static int addElemInfo(List elemInfoList, Point point,
        final int STARTING_OFFSET) {
    	int GTYPE = gType(point);
    	int LEN = D(GTYPE) + L(GTYPE);
    	
        addInt(elemInfoList, STARTING_OFFSET);
        addInt(elemInfoList, ETYPE.POINT);
        addInt(elemInfoList, 1); // INTERPRETATION single point
        
        return LEN;
    }

    @SuppressWarnings("rawtypes")
	private static int addElemInfo(List elemInfoList, Curve line,
        final int STARTING_OFFSET) {
    	int GTYPE = gType(line);
    	int LEN = D(GTYPE) + L(GTYPE);
    	
        addInt(elemInfoList, STARTING_OFFSET);
        addInt(elemInfoList, ETYPE.LINE);
        addInt(elemInfoList, 1); // INTERPRETATION straight edges
        
        LineString result = line.asLineString(0.0, 0.0);
        PointArray resultPoints = result.getControlPoints();
        
        return resultPoints.size() * LEN;
    }

    @SuppressWarnings("rawtypes")
	private static int addElemInfo(List elemInfoList, Surface surface,
        final int STARTING_OFFSET, final int GTYPE) { 
    	final SurfaceBoundary surfaceBoundary = surface.getBoundary(); 
        final int HOLES = surfaceBoundary.getInteriors().size();
        int LEN = D(GTYPE) + L(GTYPE);
        int offset = STARTING_OFFSET;
        Ring ring;

        ring = surfaceBoundary.getExterior();
        addInt(elemInfoList, offset);
        addInt(elemInfoList, elemInfoEType(surface));
        addInt(elemInfoList, elemInfoInterpretation(surface, ETYPE.POLYGON_EXTERIOR));
        
        if(isRectangle(surface)) {
            offset += (2 * LEN);
        }
        else {
        	PointArray resultPoints = getPointsOfRing(ring);
            offset += (resultPoints.size() * LEN);	
        }
        
        if (HOLES == 0) {
            return offset - STARTING_OFFSET;
        }

        for (int i = 1; i <= HOLES; i++) {
            ring = surfaceBoundary.getInteriors().get(i - 1);
            addInt(elemInfoList, offset);
            addInt(elemInfoList, ETYPE.POLYGON_INTERIOR);
            addInt(elemInfoList, elemInfoInterpretation(ring, ETYPE.POLYGON_INTERIOR));
            
            if(isRectangle(surface)) {
                offset += (2 * LEN);
            }
            else {
            	PointArray resultPoints = getPointsOfRing(ring);
                offset += (resultPoints.size() * LEN);	
            }
        }
        
        return offset - STARTING_OFFSET;
    }

    @SuppressWarnings("rawtypes")
	private static int addElemInfo(List elemInfoList, MultiPoint points,
        final int STARTING_OFFSET) {
    	int GTYPE = gType(points);
    	int LEN = D(GTYPE) + L(GTYPE);
    	
        addInt(elemInfoList, STARTING_OFFSET);
        addInt(elemInfoList, ETYPE.POINT);
        addInt(elemInfoList, elemInfoInterpretation(points, ETYPE.POINT));
        
        return points.getElements().size() * LEN;
    }

    @SuppressWarnings("rawtypes")
	private static int addElemInfo(List elemInfoList, MultiCurve lines,
        final int STARTING_OFFSET, final int GTYPE) {
        int offset = STARTING_OFFSET;
        
        Iterator<OrientableCurve> itr = lines.getElements().iterator();
        while(itr.hasNext()){
        	OrientableCurve line = itr.next();
        	offset += addElemInfo(elemInfoList, (Curve) line, offset);
        }
        
        return offset - STARTING_OFFSET;
    }

    @SuppressWarnings("rawtypes")
	private static int addElemInfo(List elemInfoList, MultiSurface sufaces,
        final int STARTING_OFFSET, final int GTYPE) {
        Surface surface;
        int offset = STARTING_OFFSET;

        Iterator<OrientableSurface>surfaceIter = sufaces.getElements().iterator();
        while(surfaceIter.hasNext()){
        	surface = (Surface) surfaceIter.next();
        	offset += addElemInfo(elemInfoList, surface, offset, GTYPE);
        }
        
        return offset - STARTING_OFFSET;
    }

    @SuppressWarnings("rawtypes")
	private static int addElemInfo(List elemInfoList, MultiPrimitive geoms,
        final int STARTING_OFFSET, final int GTYPE) {
        Geometry geom;
        int offset = STARTING_OFFSET;
        
        Iterator<? extends Primitive> itr = geoms.getElements().iterator();
        while(itr.hasNext()) {
        	geom = itr.next();
        	offset += elemInfo(elemInfoList, geom, offset, GTYPE);
        }
        
        return offset - STARTING_OFFSET;
    }

    @SuppressWarnings("rawtypes")
	private static int addElemInfo(List elemInfoList, Solid solid, 
			final int STARTING_OFFSET, int GTYPE) {
		addInt(elemInfoList, STARTING_OFFSET);
        addInt(elemInfoList, elemInfoEType(solid));
        addInt(elemInfoList, elemInfoInterpretation(solid, ETYPE.SOLID));

        int LEN = D(GTYPE) + L(GTYPE);
        int offset = STARTING_OFFSET;
        Shell shell;
        
        shell = solid.getBoundary().getExterior();
        if(shell == null) 
        	return 0;
        
        addInt(elemInfoList, offset);
        addInt(elemInfoList, ETYPE.COMPOSITE_SURFACE_EXTERIOR);
        addInt(elemInfoList, shell.getElements().size());
        
        Collection<? extends Primitive> shellElements = shell.getElements();
        for (Primitive surface : shellElements) {
        	addInt(elemInfoList, offset);
            addInt(elemInfoList, elemInfoEType((Surface)surface));
            addInt(elemInfoList, elemInfoInterpretation((Surface)surface, ETYPE.COMPOSITE_SURFACE_EXTERIOR));
            
            switch (elemInfoInterpretation(surface, ETYPE.POLYGON_EXTERIOR)) {
            case 4:  // circle not supported
                break;
           
            case 3:
            	offset += (2 * LEN);
                break;

            case 2: // curve not suppported
                break;

            case 1:	 
            	SurfaceBoundary surfaceBoundary = ((Surface) surface).getBoundary();
            	PointArray pointArray = getPointsOfRing(surfaceBoundary.getExterior());
            	offset += (pointArray.size() * LEN);
            	if(surfaceBoundary.getInteriors().size() != 0){
            		Iterator<Ring> ringIter = surfaceBoundary.getInteriors().iterator();
            		while(ringIter.hasNext()){
            			pointArray = getPointsOfRing(ringIter.next());
            			offset += (pointArray.size() * LEN);		
            		}
            	}
            }
            
		}        
        // TODO : If Solid has a hole 
        
        return offset - STARTING_OFFSET;
	}

	@SuppressWarnings("rawtypes")
	public static PointArray getPointsOfRing(Ring ring) {
		List<OrientableCurve> generators = ring.getGenerators();
        Iterator elementIter = generators.iterator();
        Curve element = (Curve) elementIter.next();
        LineString result = element.asLineString(0.0, 0.0);
        PointArray resultPoints = result.getControlPoints();
        while (elementIter.hasNext()) {
            element = (Curve) elementIter.next();
            LineString nextLine = element.asLineString(0.0, 0.0);
            
            if (nextLine.getEndPoint().equals(result.getStartPoint())) {
                LinkedList<Position> posToAdd = new LinkedList<Position>(nextLine.getControlPoints());
                posToAdd.removeLast();
                resultPoints.addAll(0, posToAdd);
            } else if (result.getEndPoint().equals(nextLine.getStartPoint())) {
                LinkedList<Position> posToAdd = new LinkedList<Position>(nextLine.getControlPoints());
                posToAdd.removeFirst();
                resultPoints.addAll(posToAdd);
            } else {
                throw new IllegalArgumentException("The LineString do not agree in a start and end point");
            }
        }
		return resultPoints;
	}
	
    /**
     * Adds contents of array to the list as Interger objects
     *
     * @param list List to append the contents of array to
     * @param array Array of ints to append
     */
    @SuppressWarnings({ "unused", "rawtypes", "unchecked" })
	private static void addInts(List list, int[] array) {
        for (int i = 0; i < array.length; i++) {
            list.add(new Integer(array[i]));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	private static void addInt(List list, int i) {
        list.add(new Integer(i));
    }

    /**
     * Converts provied list to an int array
     *
     * @param list List to cast to an array type
     * @return array of ints 
     */
    @SuppressWarnings("rawtypes")
	private static int[] intArray(List list) {
        int[] array = new int[list.size()];
        int offset = 0;

        for (Iterator i = list.iterator(); i.hasNext(); offset++) {
            array[offset] = ((Number) i.next()).intValue();
        }

        return array;
    }

    /**
     * Starting offset used by SDO_ORDINATES as stored in the SDO_ELEM_INFO
     * array.
     * 
     * <p>
     * Starting offsets start from one.
     * </p>
     * 
     * <p>
     * Describes ordinates as part of <code>SDO_ELEM_INFO</code> data type.
     * </p>
     *
     * @param geom
     *
     * @return <code>1</code> for non nested <code>geom</code>
     */
    public static int elemInfoStartingOffset(Geometry geom) {
        return 1;
    }

    /**
     * Produce <code>SDO_ETYPE</code> for geometry description as stored in the
     * <code>SDO_ELEM_INFO</code>.
     * 
     * <p>
     * Describes how Ordinates are ordered:
     * </p>
     * <pre><code><b>
     * Value Elements Meaning</b>
     *    0           Custom Geometry (like spline) 
     *    1  simple   Point (or Points)
     *    2  simple   Line (or Lines)
     *    3           polygon ring of unknown order (discouraged update to 1003 or 2003)
     * 1003  simple   polygon ring (1 exterior counterclockwise order)
     * 2003  simple   polygon ring (2 interior clockwise order)
     *    4  compound series defines a linestring
     *    5  compound series defines a polygon ring of unknown order (discouraged)
     * 1005  compound series defines exterior polygon ring (counterclockwise order)
     * 2005  compound series defines interior polygon ring (clockwise order)
     * </code></pre>
     * 
     * <p>
     * Keep in mind:
     * </p>
     * 
     * <ul>
     * <li>
     * <code>simple</code> elements are defined by a single triplet entry in
     * the <code>SDO_ELEM_INFO</code> array
     * </li>
     * <li>
     * <code>compound</code> elements are defined by a header triplet, and a
     * series of triplets for the parts. Elements in a compound element share
     * first and last points.
     * </li>
     * <li>
     * We are not allowed to mix 1 digit and 4 digit values for ETYPE and GTYPE
     * in a single geometry
     * </li>
     * </ul>
     * 
     * <p>
     * This whole mess describes ordinates as part of
     * <code>SDO_ELEM_INFO</code> array. data type.
     * </p>
     *
     * @param geom Geometry being represented
     *
     * @return Descriptionof Ordinates representation
     *
     * @throws IllegalArgumentException
     */
    protected static int elemInfoEType(Geometry geom) {
        switch (TT(geom)) {
        case TT.UNKNOWN:
            return ETYPE.CUSTOM;

        case TT.POINT:
            return ETYPE.POINT;

        case TT.LINE:
            return ETYPE.LINE;

        case TT.POLYGON:
            return isExterior((Surface) geom)
            ? ETYPE.POLYGON_EXTERIOR // cc order
            : ETYPE.POLYGON_INTERIOR; // ccw order

        case TT.SOLID:
    		return ETYPE.SOLID;

        default:
            // should never happen!
            throw new IllegalArgumentException("Unknown encoding of SDO_GTYPE");
        }
    }

    /**
     * Produce <code>SDO_INTERPRETATION</code> for geometry description as
     * stored in the <code>SDO_ELEM_INFO</code>.
     * 
     * <p>
     * Describes ordinates as part of <code>SDO_ELEM_INFO</code> array.
     * </p>
     * 
     * <ul>
     * <li>
     * <b><code>compound</code> element:</b>(SDO_ETYPE 4, 1005, or 2005)<br>
     * Number of subsequent triplets are part of compound element
     * </li>
     * <li>
     * <b><code>simple</code> element:</b>(SDE_ELEM 1, 2, 1003, or 2003)<br>
     * Code defines how ordinates are interpreted (lines or curves)
     * </li>
     * </ul>
     * 
     * <p>
     * SDO_INTERPRETAION Values: (from Table 2-2 in reference docs)
     * </p>
     * <pre><code><b>
     * SDO_ETYPE Value    Meaning</b>
     * 0         anything Custom Geometry
     * 1         1        Point
     * 1         N > 1    N points
     * 2         1        LineString of straight lines
     * 2         2        LineString connected by circ arcs (start,any,end pt)
     * 1003/2003 1        Polygon Edged with straight lines
     * 1003/2003 2        Polygon Edged with circ arcs (start, any, end pt)
     * 1003/2003 3        Non SRID rectangle defined by (bottomleft,topright pt)
     * 1003/2003 4        Circle defined by three points on circumference
     * 4         N > 1    Compound Line String made of N (ETYPE=2) lines and arcs
     * 1005/2005 N > 1    Polygon defined by (ETYPE=2) lines and arcs
     * 
     *                 
     * </code></pre>
     * 
     * <p>
     * When playing with circular arcs (SDO_INTERPRETATION==2) arcs are defined
     * by three points. A start point, any point on the arc and the end point.
     * The last point of an arc is the start point of the next. When used to
     * describe a polygon (SDO_ETYPE==1003 or 2003) the first and last point
     * must be the same.
     * </p>
     *
     * @param geom
     *
     */
    public static int elemInfoInterpretation(Geometry geom) {
        return elemInfoInterpretation(geom, elemInfoEType(geom));
    }

    /**
     * Allows specification of <code>INTERPRETATION</code> used to interpret
     * <code>geom</code>.
     * <p>
     * Provides the INTERPRETATION value for the ELEM_INFO triplet
     * of (STARTING_OFFSET, ETYPE, INTERPRETATION).
     * </p>
     * @param geom Geometry to encode
     * @param etype ETYPE value requiring an INTERPREATION
     *
     * @return INTERPRETATION ELEM_INFO entry for geom given etype
     *
     * @throws IllegalArgumentException If asked to encode a curve
     */
    public static int elemInfoInterpretation(Geometry geom, final int etype) {
        switch (etype) {
        case ETYPE.CUSTOM: // customize for your own Geometries
            break;

        case ETYPE.POINT:

            if (geom instanceof Point) {
                return 1;
            }

            if (geom instanceof MultiPoint) {
                return ((MultiPoint) geom).getElements().size();
            }

            break;

        case ETYPE.LINE:

            if (isCurve((Curve) geom)) {
                return 2;
            }

            return 1;

        case ETYPE.POLYGON:
        case ETYPE.POLYGON_EXTERIOR:
        case ETYPE.POLYGON_INTERIOR:

            if (geom instanceof Surface) {
            	Surface surface = (Surface) geom;

                if (isCurve(surface)) {
                    return 2;
                }

                if (isRectangle(surface)) {
                    return 3;
                }

                if (isCircle(surface)) {
                    return 4;
                }
            }

            return 1;

        case ETYPE.COMPOUND:
        case ETYPE.COMPOUND_POLYGON:
        case ETYPE.COMPOUND_POLYGON_INTERIOR:
        case ETYPE.COMPOUND_POLYGON_EXTERIOR:
        	// TODO : In Case of Compound Polygon 
            break;
            
        case ETYPE.SOLID:
    		return 1;
    		
    	case ETYPE.COMPOSITE_SURFACE_EXTERIOR:
    		return 1;
    	
        }

       	throw new IllegalArgumentException("Cannot encode ISO "
                + geom.getClass().getTypeName() + " as SDO_GTYPE "
            + "SDO_INTERPRETATION (Limitied to Point, Line, Polygon, Solid"
            + "MultiPoint, MultiLineString and MultiPolygon)");
    }

    /**
     * Produce <code>SDO_ORDINATES</code> for geometry.
     * 
     * <p>
     * Please see SDO_ETYPE, SDO_INTERPRETATION and SDO_GTYPE for description
     * of how these ordinates are to be interpreted.
     * </p>
     * 
     * <p>
     * Ordinates are ordered by Dimension are non null:
     * </p>
     * 
     * <ul>
     * <li>
     * <p>
     * Two Dimensional:
     * </p>
     * {x1,y1,x2,y2,...}
     * </li>
     * <li>
     * <p>
     * Three Dimensional:
     * </p>
     * {x1,y1,z1,x2,y2,z2,...}
     * </li>
     * </ul>
     * 
     * <p>
     * Spatial will siliently detect and ignore the following:
     * </p>
     * 
     * <ul>
     * <li>
     * d001 point/d005 multipoint elements that are not SDO_ETYPE==1 points
     * </li>
     * <li>
     * d002 lines or curve/d006 multilines or multicurve elements that are not
     * SDO_ETYPE==2 lines or SDO_ETYPE==4 arcs
     * </li>
     * <li>
     * d003 polygon/d007 multipolygon elements that are not SDO_ETYPE==3
     * unordered polygon lines or SDO_ETYPE==5 unorderd compound polygon ring
     * are ignored
     * </li>
     * </ul>
     * 
     * <p>
     * While Oracle is silient on these errors - all other errors will not be
     * detected!
     * </p>
     *
     * @param geom
     *
     */
    @SuppressWarnings("rawtypes")
	public static double[] ordinates(Geometry geom) {
        List list = new ArrayList();

        coordinates(list, geom);

        return ordinates(list, geom);
    }

    /**
     * Encode Geometry as described by GTYPE and ELEM_INFO
     * <p>
     * CoordinateSequence & CoordinateAccess wil be used to determine the
     * dimension, and the number of ordinates added.
     * </p>
     * @param list Flat list of Double
     * @param geom Geometry 
     *
     * @throws IllegalArgumentException If geometry cannot be encoded
     */
    @SuppressWarnings("rawtypes")
	public static void coordinates(List list, Geometry geom) {
        switch (TT(geom)) {
        case TT.UNKNOWN:
            break; // extend with your own custom types

        case TT.POINT:
            addCoordinates(list, (Point) geom);

            return;

        case TT.LINE:
            addCoordinates(list, (Curve) geom);

            return;

        case TT.POLYGON:
            addCoordinates(list, (Surface) geom);

            return;
            
        case TT.SOLID:
    		addCoordinates(list, (Solid) geom);
    		
    		return;
            
        case TT.MULTIPOINT:
            addCoordinates(list, (MultiPoint) geom);

            return;

        case TT.MULTICURVE:
            addCoordinates(list, (MultiCurve) geom);

            return;

        case TT.MULTISURFACE:
            addCoordinates(list, (MultiSurface) geom);

            return;
            
        case TT.COLLECTION:
            addCoordinates(list, (MultiPrimitive) geom);

            return;
            
        }

        throw new IllegalArgumentException("Cannot encode JTS "
            + geom.getClass().getTypeName() + " as "
            + "SDO_ORDINATRES (Limitied to Point, Line, Polygon, Solid,"
            + "MultiPoint, MultiLineString and MultiPolygon)");
    }

	/**
     * Adds a double array to list.
     * 
     * <p>
     * The double array will contain all the ordinates in the Coordinate
     * sequence.
     * </p>
     *
     * @param list
     * @param pointArray
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private static void addCoordinates(List list, PointArray pointArray) {
        for (int i = 0; i < pointArray.size(); i++) {
            list.add(ordinateArray(pointArray.get(i).getDirectPosition()));
        }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private static void addCoordinates(List list, DirectPosition directPosition) {
    	list.add(ordinateArray(directPosition));
    }

    private static double[] ordinateArray(DirectPosition dp) {
    	if(dp.getDimension() == 2)
    		return new double[] { dp.getOrdinate(0), dp.getOrdinate(1) };
    	else if(dp.getDimension() == 3)
    		return new double[] { dp.getOrdinate(0), dp.getOrdinate(1), dp.getOrdinate(2) };
    	else 
    		return null;
    }

    /**
     * 
     * ordinateArray purpose.
     * <p>
     * Description ...
     * </p>
     * @param access
     * @param index
     */
    @SuppressWarnings("unused")
	private static double[] doubleOrdinateArray(CoordinateAccess access, int index) {
        final int D = access.getDimension();
        final int L = access.getNumAttributes();
        final int LEN = D + L;
        double[] ords = new double[LEN];

        for (int i = 0; i < LEN; i++) {
            ords[i] = access.getOrdinate(index, i);
        }

        return ords;
    }
    

    /**
     * Used with ELEM_INFO <code>( 1, 1, 1)</code>
     *
     * @param list List to add coordiantes to
     * @param point Point to be encoded
     */
    @SuppressWarnings("rawtypes")
	private static void addCoordinates(List list, Point point) {
        addCoordinates(list, point.getDirectPosition());
    }

    /**
     * Used with ELEM_INFO <code>(1, 2, 1)</code>
     *
     * @param list List to add coordiantes to
     * @param line LineString to be encoded
     */
    @SuppressWarnings("rawtypes")
	private static void addCoordinates(List list, Curve curve) {
    	LineString result = curve.asLineString(0.0, 0.0);
        PointArray resultPoints = result.getControlPoints();
        addCoordinates(list, resultPoints);
    }

    /**
     * Used to addCoordinates based on polygon encoding.
     * 
     * <p>
     * Elem Info Interpretations supported:
     * 
     * <ul>
     * <li>
     * 3: Rectangle
     * </li>
     * <li>
     * 1: Standard (supports holes)
     * </li>
     * </ul>
     * </p>
     *
     * @param list List to add coordiantes to
     * @param polygon Polygon to be encoded
     */
    @SuppressWarnings("rawtypes")
	private static void addCoordinates(List list, Surface surface) {
        switch (elemInfoInterpretation(surface)) {
        case 4:  // circle not supported
            break;
       
        case 3:
            addCoordinatesInterpretation3(list, surface);
            break;

        case 2: // curve not suppported
            break;

        case 1:
            addCoordinatesInterpretation1(list, surface);

            break;
        }
    }

    /**
     * Rectangle ordinates for polygon.
     * 
     * <p>
     * You should in sure that the provided <code>polygon</code> is a rectangle
     * using isRectangle( Polygon )
     * </p>
     * 
     * <p></p>
     *
     * @param list List to add coordiantes to
     * @param polygon Polygon to be encoded
     */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void addCoordinatesInterpretation3(List list, Surface surface) {
		Envelope envelope = surface.getEnvelope();
		list.add(envelope.getLowerCorner().getCoordinate());
        list.add(envelope.getUpperCorner().getCoordinate());
	}
	
    /**
     * Add ordinates for polygon - with hole support.
     * 
     * <p>
     * Ensure ordinates are added in the correct orientation as External or
     * Internal polygons.
     * </p>
     *
     * @param list List to add coordiantes to
     * @param surface Polygon to be encoded
     */
    @SuppressWarnings("rawtypes")
	private static void addCoordinatesInterpretation1(List list, Surface surface) {
    	final SurfaceBoundary surfaceBoundary = surface.getBoundary();
        final int holes = surfaceBoundary.getInteriors().size();
        final ISOGeometryBuilder builder = new ISOGeometryBuilder(surface.getCoordinateReferenceSystem());
        addCoordinates(list, counterClockWise(builder, getPointsOfRing(surfaceBoundary.getExterior())));

        for (int i = 0; i < holes; i++) {
            addCoordinates(list, clockWise(builder,getPointsOfRing(surfaceBoundary.getInteriors().get(i))));
        }
    }
    
    @SuppressWarnings("rawtypes")
	private static void addCoordinates(List list, Solid solid) {
    	Shell shellExterior = solid.getBoundary().getExterior();
    	if(shellExterior != null){
    		Collection<? extends Primitive> shellElements = shellExterior.getElements();
        	for (Primitive surface : shellElements) {
        		// Assumed that shell is created by 1 surface
        		addCoordinates(list, (Surface)surface);
        	}	
    	}
    	// In case of interior shell?
	}

    @SuppressWarnings("rawtypes")
    private static void addCoordinates(List list, MultiPoint points) {
    	Iterator<Point> pointIter = points.getElements().iterator();
    	while(pointIter.hasNext()){
    		Point point = pointIter.next();
    		addCoordinates(list, point);
    	}
    }

    @SuppressWarnings("rawtypes")
    private static void addCoordinates(List list, MultiCurve curves) {
    	Iterator<OrientableCurve> curveIter = curves.getElements().iterator();
    	while(curveIter.hasNext()){
    		OrientableCurve orientableCurve = curveIter.next();
    		addCoordinates(list, (Curve) orientableCurve);
    	}
        
    }

    @SuppressWarnings("rawtypes")
	private static void addCoordinates(List list, MultiSurface surfaces) {
    	Iterator<OrientableSurface> surfaceIter = surfaces.getElements().iterator();
    	while(surfaceIter.hasNext()){
    		OrientableSurface orientableSurface = surfaceIter.next();
    		addCoordinates(list, (Surface) orientableSurface);
    	}
    }

    
    @SuppressWarnings("rawtypes")
	private static void addCoordinates(List list, MultiPrimitive geoms) {
        Geometry geom;
        
        Iterator<? extends Primitive> collectionIter = geoms.getElements().iterator();
        while(collectionIter.hasNext()){
        	geom = collectionIter.next();
        	
        	if (geom instanceof Point) {
                addCoordinates(list, (Point) geom);
            } else if (geom instanceof Curve) {
                addCoordinates(list, (Curve) geom);
            } else if (geom instanceof Surface) {
                addCoordinates(list, (Surface) geom);
            } else if (geom instanceof MultiPoint) {
                addCoordinates(list, (MultiPoint) geom);
            } else if (geom instanceof MultiCurve) {
                addCoordinates(list, (MultiCurve) geom);
            } else if (geom instanceof MultiSurface) {
                addCoordinates(list, (MultiSurface) geom);
            } else if (geom instanceof MultiPrimitive) {
                addCoordinates(list, (MultiPrimitive) geom);
            }
        }
    }
    

    /**
     * Package up array of requested ordinate, regardless of geometry
     * 
     * <p>
     * Example numbering: for (x y g m) dimension==2, measure==2
     * </p>
     * 
     * <ul>
     * <li>
     * 0: x ordinate array
     * </li>
     * <li>
     * 1: y ordinate array
     * </li>
     * <li>
     * 2: g ordinate array
     * </li>
     * <li>
     * 3: m ordinate array
     * </li>
     * </ul>
     * 
     *
     * @param coords
     * @param ordinate
     *
     */
    public static double[] ordinateArray(PointArray coords, int ordinate) {
    	final int LENGTH = coords.size();
        double[] array = new double[LENGTH];
        DirectPosition dp = null;
        if (ordinate == 0 || ordinate == 1 || ordinate == 2) {
            for (int i = 0; i < LENGTH; i++) {
            	dp = coords.get(i).getDirectPosition();
                array[i] = (dp != null) ? dp.getOrdinate(ordinate) : Double.NaN;
            }
        } else {
            // default to NaN
            for (int i = 0; i < LENGTH; i++) {
                array[i] = Double.NaN;
            }
        }

        return array;
    }

    /**
     * Ordinate access.
     * 
     * <p>
     * CoordinateAccess is required for additional ordinates.
     * </p>
     * 
     * <p>
     * Ordinate limitied to:
     * </p>
     * 
     * <ul>
     * <li>
     * 0: x ordinate array
     * </li>
     * <li>
     * 1: y ordinate array
     * </li>
     * <li>
     * 2: z ordinate array
     * </li>
     * <li>
     * 3: empty ordinate array
     * </li>
     * </ul>
     * 
     *
     * @param array
     * @param ordinate
     *
     */
    /*
    public static double[] ordinateArray(Coordinate[] array, int ordinate) {
        if (array == null) {
            return null;
        }

        final int LENGTH = array.length;
        double[] ords = new double[LENGTH];
        Coordinate c;

        if (ordinate == 0) {
            for (int i = 0; i < LENGTH; i++) {
                c = array[i];
                ords[i] = (c != null) ? c.x : Double.NaN;
            }
        } else if (ordinate == 1) {
            for (int i = 0; i < LENGTH; i++) {
                c = array[i];
                ords[i] = (c != null) ? c.y : Double.NaN;
            }
        } else if (ordinate == 2) {
            for (int i = 0; i < LENGTH; i++) {
                c = array[i];
                ords[i] = (c != null) ? c.z : Double.NaN;
            }
        } else {
            // default to NaN
            for (int i = 0; i < LENGTH; i++) {
                ords[i] = Double.NaN;
            }
        }

        return ords;
    }

    
    public static double[] ordinateArray(List list, int ordinate) {
        if (list == null) {
            return null;
        }
        
        final int LENGTH = list.size();
        double[] ords = new double[LENGTH];
        Coordinate c;

        if (ordinate == 0) {
            for (int i = 0; i < LENGTH; i++) {
                c = (Coordinate) list.get(i);
                ords[i] = (c != null) ? c.x : Double.NaN;
            }
        } else if (ordinate == 1) {
            for (int i = 0; i < LENGTH; i++) {
                c = (Coordinate) list.get(i);
                ords[i] = (c != null) ? c.y : Double.NaN;
            }
        } else if (ordinate == 2) {
            for (int i = 0; i < LENGTH; i++) {
                c = (Coordinate) list.get(i);
                ords[i] = (c != null) ? c.z : Double.NaN;
            }
        } else {
            // default to NaN
            for (int i = 0; i < LENGTH; i++) {
                ords[i] = Double.NaN;
            }
        }

        return ords;
    }
    */
    
    /**
     * Do not use me, I am broken
     * <p>
     * Do not use me, I am broken
     * </p>
     * @deprecated Do not use me, I am broken
     * @param list
     * @param ordinate
     */
    /*
    //TODO: check if I am correct
    public static Object[] attributeArray(List list, int ordinate) {
        if (list == null) {
            return null;
        }

        final int LENGTH = list.size();
        Object[] ords = new Object[LENGTH];
        Coordinate c;
        Double d;
        String s;

        if (ordinate == 0) {
            for (int i = 0; i < LENGTH; i++) {
                c = (Coordinate) list.get(i);
                ords[i] = (c != null) ? new Double(c.x) : new Double(Double.NaN);
            }
        } else if (ordinate == 1) {
            for (int i = 0; i < LENGTH; i++) {
                c = (Coordinate) list.get(i);
                ords[i] = (c != null) ? new Double(c.y) : new Double(Double.NaN);
            }
        } else if (ordinate == 2) {
            for (int i = 0; i < LENGTH; i++) {
                c = (Coordinate) list.get(i);
                ords[i] = (c != null) ? new Double(c.z) : new Double(Double.NaN);
            }
        }
        else if (ordinate == 3) {       //BUG I am broken, do not use me our own Z
            for (int i = 0; i < LENGTH; i++) {
                c = (Coordinate) list.get(i);
                ords[i] = (c != null) ? new Double(Double.NaN) : new Double(Double.NaN);
            }
        }
        else if (ordinate == 4) {       // our own T (a String)
            for (int i = 0; i < LENGTH; i++) {
                c = (Coordinate) list.get(i);
                ords[i] = (c != null) ? new Double(Double.NaN) : new Double(Double.NaN);
            }
        }else {
            // default to NaN
            for (int i = 0; i < LENGTH; i++) {
                ords[i] = list.get(i);
            }
        }

        return ords;
    }
    */

    /**
     * Package up <code>array</code> in correct manner for <code>geom</code>.
     * 
     * <p>
     * Ordinates are placed into an array based on:
     * </p>
     * 
     * <ul>
     * <li>
     * geometryGTypeD - chooses between 2d and 3d representation
     * </li>
     * <li>
     * geometryGTypeL - number of LRS measures
     * </li>
     * </ul>
     * 
     *
     * @param list
     * @param geom
     *
     */
    @SuppressWarnings("rawtypes")
	public static double[] ordinates(List list, Geometry geom) {
        LOGGER.finest("ordinates D:" + D(geom));
        LOGGER.finest("ordinates L:" + L(geom));

        if (D(geom) == 3) {
            return ordinates3d(list, L(geom));
        } else {
            return ordinates2d(list, L(geom));
        }
    }
    
    /**
     * Ordinates (x,y,x1,y1,...) from coordiante list.
     * 
     * <p>
     * No assumptions are made about the order
     * </p>
     *
     * @param list coordinate list
     * @return ordinate array
     */
    @SuppressWarnings("rawtypes")
	public static double[] ordinates2d(List list) {
        final int NUMBER = list.size();
        final int LEN = 2;
        double[] array = new double[NUMBER * LEN];
        double[] ords;
        int offset = 0;

        for (int i = 0; i < NUMBER; i++) {
            ords = (double[]) list.get(i);

            if (ords != null) {
                array[offset++] = ords[0];
                array[offset++] = ords[1];
            } else {
                array[offset++] = Double.NaN;
                array[offset++] = Double.NaN;
            }
        }

        return array;
    }

    /**
     * Ordinates (x,y,z,x2,y2,z2...) from coordiante[] array.
     *
     * @param list List of coordiante
     *
     * @return ordinate array
     */
    @SuppressWarnings("rawtypes")
	public static double[] ordinates3d(List list) {
        final int NUMBER = list.size();
        final int LEN = 3;
        double[] array = new double[NUMBER * LEN];
        double[] ords;
        int offset = 0;

        for (int i = 0; i < NUMBER; i++) {
            ords = (double[]) list.get(i);

            if (ords != null) {
                array[offset++] = ords[0];
                array[offset++] = ords[1];
                array[offset++] = ords[2];
            } else {
                array[offset++] = Double.NaN;
                array[offset++] = Double.NaN;
                array[offset++] = Double.NaN;
            }
        }

        return array;
    }

    /**
     * Ordinates (x,y,...id,x2,y2,...) from coordiante[] List.
     *
     * @param list coordiante list
     * @param L Dimension of ordinates required for representation
     *
     * @return ordinate array
     */
    @SuppressWarnings("rawtypes")
	public static double[] ordinates2d(List list, final int L) {
        if (L == 0) {
            return ordinates2d(list);
        }

        final int NUMBER = list.size();
        final int LEN = 2 + L;
        double[] array = new double[NUMBER * LEN];
        double[] ords;

        for (int i = 0; i < NUMBER; i++) {
            ords = (double[]) list.get(i);

            for (int j = 0; j < LEN; j++) {
                array[(i * LEN) + j] = ords[j];
            }
        }

        return array;
    }

    /**
     * Ordinates (x,y,z,...id,x2,y2,z2...) from coordiante[] array.
     *
     * @param list coordinate array to be represented as ordinates
     * @param L Dimension of ordinates required for representation
     *
     * @return ordinate array
     */
    @SuppressWarnings("rawtypes")
	public static double[] ordinates3d(List list, final int L) {
        if (L == 0) {
            return ordinates3d(list);
        }

        final int NUMBER = list.size();
        final int LEN = 3 + L;
        double[] array = new double[NUMBER * LEN];
        double[] ords;

        for (int i = 0; i < NUMBER; i++) {
            ords = (double[]) list.get(i);

            for (int j = 0; j < LEN; j++) {
                array[(i * LEN) + j] = ords[j];
            }
        }

        return array;
    }

    /**
     * Ensure Ring of Coordinates are in a counter clockwise order.
     * 
     * <p>
     * If the Coordinate need to be reversed a copy will be returned.
     * </p>
     *
     * @param factory Factory to used to reverse CoordinateSequence
     * @param ring Ring of Coordinates
     *
     * @return coords in a CCW order
     */
    public static PointArray counterClockWise(
        ISOGeometryBuilder factory, PointArray ring) {
    	
        if (isCCW(ring)) {
            return ring;
        }

        return Coordinates.reverse(factory, ring);
    }
    
    private static boolean isCCW(PointArray ring) {
    	ArrayList<DirectPosition> directPositions = new ArrayList<>();
    	for(int i = 0; i < ring.size(); i++){
    		directPositions.add(ring.get(i).getDirectPosition());
    	}
    	
        if (CGAlgorithms.isCCW(directPositions)) {
            return true;
        }
        
        return false;
    }

    /**
     * Ensure Ring of Coordinates are in a clockwise order.
     * 
     * <p>
     * If the Coordinate need to be reversed a copy will be returned.
     * </p>
     *
     * @param factory Factory used to reverse CoordinateSequence
     * @param ring Ring of Coordinates
     *
     * @return coords in a CW order
     */
    private static PointArray clockWise(
    		ISOGeometryBuilder factory, PointArray ring) {
    	
        if (!isCCW(ring)) {
            return ring;
        }
        return Coordinates.reverse(factory, ring);
    }

    /**
     * Reverse the clockwise orientation of the ring of Coordinates.
     *
     * @param ring Ring of Coordinates
     *
     * @return coords Copy of <code>ring</code> in reversed order
     */
    @SuppressWarnings("unused")
	private static PointArray reverse(PointArray ring) {
        int length = ring.size();
        
        ISOGeometryBuilder builder = new ISOGeometryBuilder(ring.getCoordinateReferenceSystem());
        PointArray reverse = builder.createPointArray();

        for (int i = 0; i < length; i++) {
        	reverse.add(ring.get(length - i - 1));
        }
        
        return reverse;
    }

    // Utility Functions
    //
    //

    /**
     * Will need to tell if we are encoding a Polygon Exterior or Interior so
     * we can produce the correct encoding.
     *
     * @param geom Polygon to check
     *
     * @return <code>true</code> as we expect PolygonExteriors to be passed in
     */
    private static boolean isExterior(Surface geom) {
        return true; // JTS polygons are always exterior
    }

    /**
     * We need to check if a <code>polygon</code> a cicle so we can produce the
     * correct encoding.
     *
     * @param surface
     *
     * @return <code>true</code> if polygon is a circle
     */
    private static boolean isCircle(Surface surface) {
        return false; // JTS does not do cicles
    }

    /**
     * We need to check if a <code>polygon</code> a rectangle so we can produce
     * the correct encoding.
     * 
     * <p>
     * Rectangles are only supported without a SRID!
     * </p>
     *
     * @param surface
     *
     * @return <code>true</code> if polygon is SRID==0 and a rectangle
     */
    public static boolean isRectangle(Surface surface) {
    	/*
        if (((SurfaceImpl)surface).getUserData() != null) {
            // Rectangles only valid in CAD applications
            // that do not have an SRID system
            return false;
        }
		*/
        if (L(surface) != 0) {
            // cannot support LRS on a rectangle
            return false;
        }

        PointArray coords = getPointsOfRing(surface.getBoundary().getExterior());

        if (coords.size() != 5) {
            return false;
        }

        if ((coords.get(0).getDirectPosition() == null) 
        		|| (coords.get(1).getDirectPosition() == null) 
        		|| (coords.get(2).getDirectPosition() == null) 
        		|| (coords.get(3).getDirectPosition() == null)) {
            return false;
        }

        if (!coords.get(0).getDirectPosition().equals(coords.get(4).getDirectPosition())) {
            return false;
        }
        
        if(surface.getBoundary().getInteriors().size() != 0){
        	return false;
        }

        if(coords.get(0).getDirectPosition().getDimension() == 2){
        	double x1 = coords.get(0).getDirectPosition().getOrdinate(0);
            double y1 = coords.get(0).getDirectPosition().getOrdinate(1);
            double x2 = coords.get(1).getDirectPosition().getOrdinate(0);
            double y2 = coords.get(1).getDirectPosition().getOrdinate(1);
            double x3 = coords.get(2).getDirectPosition().getOrdinate(0);
            double y3 = coords.get(2).getDirectPosition().getOrdinate(1);
            double x4 = coords.get(3).getDirectPosition().getOrdinate(0);
            double y4 = coords.get(3).getDirectPosition().getOrdinate(1);

            if ((x1 == x4) && (y1 == y2) && (x3 == x2) && (y3 == y4)) {
                // 1+-----+2
                //  |     |
                // 4+-----+3
                return true;
            }

            if ((x1 == x2) && (y1 == y4) && (x3 == x4) && (y3 == y2)) {
                // 2+-----+3
                //  |     |
                // 1+-----+4
                return true;
            }	
        }
        else if(coords.get(0).getDirectPosition().getDimension() == 3){
        	double x1 = coords.get(0).getDirectPosition().getOrdinate(0);
            double y1 = coords.get(0).getDirectPosition().getOrdinate(1);
            double z1 = coords.get(0).getDirectPosition().getOrdinate(2);
            double x2 = coords.get(1).getDirectPosition().getOrdinate(0);
            double y2 = coords.get(1).getDirectPosition().getOrdinate(1);
            double z2 = coords.get(1).getDirectPosition().getOrdinate(2);
            double x3 = coords.get(2).getDirectPosition().getOrdinate(0);
            double y3 = coords.get(2).getDirectPosition().getOrdinate(1);
            double z3 = coords.get(2).getDirectPosition().getOrdinate(2);
            double x4 = coords.get(3).getDirectPosition().getOrdinate(0);
            double y4 = coords.get(3).getDirectPosition().getOrdinate(1);
            double z4 = coords.get(3).getDirectPosition().getOrdinate(2);
            
            if(x1 == x2 && x1 == x3 && x1 == x4){
            	if(z1 == z4 && y1 == y2 && z3 == z2 && y3 == y4)
            		return true;
            	if(z1 == z2 && y1 == y4 && z3 == z4 && y3 == y2)
            		return true;
            }
            else if(y1 == y2 && y1 == y3 && y1 == y4){
            	if(x1 == x4 && z1 == z2 && x3 == x2 && z3 == z4)
            		return true;
            	if(x1 == x2 && z1 == z4 && x3 == x4 && z3 == z2)
            		return true;
            }
            else if(z1 == z2 && z1 == z3 && z1 == z4){
            	if(x1 == x4 && y1 == y2 && x3 == x2 && y3 == y4)
            		return true;
            	if(x1 == x2 && y1 == y4 && x3 == x4 && y3 == y2)
            		return true;
            }
        }
        

        return false;
    }

    /**
     * We need to check if a <code>polygon</code> is defined with curves so we
     * can produce the correct encoding.
     * 
     * <p></p>
     *
     * @param surface
     *
     * @return <code>false</code> as JTS does not support curves
     */
    private static boolean isCurve(Surface surface) {
        return false;
    }

    /**
     * We need to check if a <code>lineString</code> is defined with curves so
     * we can produce the correct encoding.
     * 
     * <p></p>
     *
     * @param curve
     *
     * @return <code>false</code> as JTS does not support curves
     */
    private static boolean isCurve(Curve curve) {
        return false;
    }

    // Decoding Helper Functions
    //
    //
    /**
     * Returns a range from a CoordinateList, based on ELEM_INFO triplets.
     *
     * @param geometryFactory Factory used for JTS 
     * @param coords Coordinates
     * @param GTYPE Encoding of <b>D</b>imension, <b>L</b>RS and <b>TT</b>ype
     * @param elemInfo
     * @param triplet
     *
     */
    private static PointArray subList(
        ISOGeometryBuilder geometryFactory, PointArray coords,
        int GTYPE, int[] elemInfo, int triplet, 
        boolean compoundElement) {
            
        final int STARTING_OFFSET = STARTING_OFFSET(elemInfo, triplet);
        int ENDING_OFFSET = STARTING_OFFSET(elemInfo, triplet + 1); // -1 for end
        final int LEN = D(GTYPE);
        if (ENDING_OFFSET != -1 && compoundElement) {
            ENDING_OFFSET += LEN;
        }

        if ((STARTING_OFFSET == 1) && (ENDING_OFFSET == -1)) {
            // Use all Cordiantes
            return coords;
        }


        int start = (STARTING_OFFSET - 1) / LEN;
        int end = (ENDING_OFFSET != -1) ? ((ENDING_OFFSET - 1) / LEN)
                                        : coords.size();

        return subList(geometryFactory, coords, start, end);
    }

    /**
     * Version of List.subList() that returns a CoordinateSequence.
     * 
     * <p>
     * Returns from start (inclusive) to end (exlusive):
     * </p>
     * 
     * <p>
     * Math speak: <code>[start,end)</code>
     * </p>
     *
     * @param geometryFactory Manages CoordinateSequences for JTS
     * @param coords coords to sublist
     * @param start starting offset
     * @param end upper bound of sublist 
     *
     * @return CoordinateSequence
     */
    private static PointArray subList(
        ISOGeometryBuilder geometryFactory, PointArray coords,
        int start, int end) {
        if ((start == 0) && (end == coords.size())) {
            return coords;
        }

        return Coordinates.subList(geometryFactory, coords, start, end);
    }
/*
    private static LinearRing[] toInteriorRingArray(List list) {
        return (LinearRing[]) toArray(list, LinearRing.class);

           //if( list == null ) return null;
           //LinearRing array[] = new LinearRing[ list.size() ];
           //int index=0;
           //for( Iterator i=list.iterator(); i.hasNext(); index++ )
           //{
           //    array[ index ] = (LinearRing) i.next();
           //}
           //return array;
    }

    private static LineString[] toLineStringArray(List list) {
        return (LineString[]) toArray(list, LineString.class);

           //if( list == null ) return null;
           //LineString array[] = new LineString[ list.size() ];
           //int index=0;
           //for( Iterator i=list.iterator(); i.hasNext(); index++ )
           //{
           //    array[ index ] = (LineString) i.next();
           //}
           //return array;
    }

    private static Polygon[] toPolygonArray(List list) {
        return (Polygon[]) toArray(list, Polygon.class);
    }

    private static Geometry[] toGeometryArray(List list) {
        return (Geometry[]) toArray(list, Geometry.class);
    }
*/
    /**
     * Useful function for converting to typed arrays for JTS API.
     * 
     * <p>
     * Example:
     * </p>
     * <pre><code>
     * new MultiPoint( toArray( list, Coordinate.class ) );
     * </code></pre>
     *
     * @param list
     * @param type
     *
     */
    @SuppressWarnings({ "rawtypes", "unused" })
	private static Object toArray(List list, Class type) {
        if (list == null) {
            return null;
        }

        Object array = Array.newInstance(type, list.size());
        int index = 0;

        for (Iterator i = list.iterator(); i.hasNext(); index++) {
            Array.set(array, index, i.next());
        }

        return array;
    }

    /**
     * Access D (for dimension) as encoded in GTYPE
     *
     * @param GTYPE DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static int D(final int GTYPE) {
        return GTYPE / 1000;
    }

    /**
     * Access L (for LRS) as encoded in GTYPE
     *
     * @param GTYPE DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static int L(final int GTYPE) {
        return (GTYPE - (D(GTYPE) * 1000)) / 100;
    }

    /**
     * Access TT (for geometry type) as encoded in GTYPE
     *
     * @param GTYPE DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static int TT(final int GTYPE) {
        return GTYPE - (D(GTYPE) * 1000) - (L(GTYPE) * 100);
    }

    /**
     * Access STARTING_OFFSET from elemInfo, or -1 if not available.
     * 
     * <p></p>
     *
     * @param elemInfo DOCUMENT ME!
     * @param triplet DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    private static int STARTING_OFFSET(int[] elemInfo, int triplet) {
        if (((triplet * 3) + 0) >= elemInfo.length) {
            return -1;
        }

        return elemInfo[(triplet * 3) + 0];
    }

    /**
     * A version of assert that indicates range pre/post condition.
     * <p>
     * Works like assert exception IllegalArgumentException is thrown indicating this
     * is a required check.
     * </p>
     * <p>
     * Example phrased as a positive statement of the requirement to be met:
     * <pre><code>
     * ensure( "STARTING_OFFSET {1} must indicate a valid ordinate between {0} and {2}.
     * </code></pre>
     * </p>
     * @param condition MessageFormat pattern - positive statement of requirement
     * @param min minimum acceptable value ({0} in message format)
     * @param actual value supplied ({1} in message format)
     * @param max maximum acceptable value ({2} in message format)
     * @throws IllegalArgumentException unless min <= actual <= max
     */
    private static void ensure( String condition, int min, int actual, int max  ){
        if( !(min <= actual && actual <= max) ){
            String msg = MessageFormat.format( condition,
                    new Object[]{ new Integer(min), new Integer(actual), new Integer(max) } );
            throw new IllegalArgumentException( msg );
        }
    }
    /**
     * A version of assert that indicates range pre/post condition.
     * <p>
     * Works like assert exception IllegalArgumentException is thrown indicating this
     * is a required check.
     * </p>
     * <p>
     * Example phrased as a positive statement of the requirement to be met:
     * <pre><code>
     * ensure( "INTERPRETATION {0} must be on of {1}.
     * </code></pre>
     * </p>
     * @param condition MessageFormat pattern - positive statement of requirement
     * @param actual value supplied ({0} in message format)
     * @param set Array of acceptable values ({1} in message format)
     * @throws IllegalArgumentException unless actual is a member of set
     */
    private static void ensure( String condition, int actual, int[] set ){
        if( set == null ) return; // don't apparently care
        for( int i=0; i<set.length;i++){
            if( set[i] == actual ) return; // found it
        }
        StringBuffer array = new StringBuffer();        
        for( int i=0; i<set.length;i++){
            array.append( set[i] );
            if( i<set.length){
                array.append( "," );
            }
        }
        String msg = MessageFormat.format( condition,
                new Object[]{ new Integer(actual), array } );        
        throw new IllegalArgumentException( msg );
    }
    /** Returns the "length" of the ordinate array used for the
     * CoordinateSequence, GTYPE is used to determine the dimension.
     * <p>
     * This is most often used to check the STARTING_OFFSET value to ensure
     * that is falls within allowable bounds.
     * </p>
     * <p>
     * Example:<pre><code>
     * if (!(STARTING_OFFSET >= 1) ||
     *     !(STARTING_OFFSET <= ordinateSize( coords, GTYPE ))){
     *     throw new IllegalArgumentException(
     *         "ELEM_INFO STARTING_OFFSET "+STARTING_OFFSET+ 
     *         "inconsistent with COORDINATES length "+size( coords, GTYPE ) );
     * } 
     * </code></pre>
     * </p>
     * @param coords
     * @param GTYPE
     */
    private static int ordinateSize(PointArray coords, int GTYPE ){
        if( coords == null ){
            return 0;
        }
        return coords.size() * D(GTYPE);
    }
    /**
     * ETYPE access for the elemInfo triplet indicated.
     * <p>
     * @see ETYPE for an indication of possible values
     * 
     * @param elemInfo
     * @param triplet
     * @return ETYPE for indicated triplet
     */ 
    private static int ETYPE(int[] elemInfo, int triplet) {
        if (((triplet * 3) + 1) >= elemInfo.length) {
            return -1;
        }

        return elemInfo[(triplet * 3) + 1];
    }

    private static int INTERPRETATION(int[] elemInfo, int triplet) {
        if (((triplet * 3) + 2) >= elemInfo.length) {
            return -1;
        }

        return elemInfo[(triplet * 3) + 2];
    }

    /**
     * Coordinates from <code>(x,y,x2,y2,...)</code> ordinates.
     *
     * @param ordinates DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static PointArray asCoordinates(double[] ordinates) {
        return asCoordinates(ordinates, 2);
    }

    /**
     * Coordinates from a <code>(x,y,i3..,id,x2,y2...)</code> ordinates.
     *
     * @param ordinates DOCUMENT ME!
     * @param d DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static PointArray asCoordinates(double[] ordinates, int d) {
        int length = ordinates.length / d;
        
        Hints hints = GeoTools.getDefaultHints();
        if ( d == 2 )
        	hints.put(Hints.CRS, DefaultGeographicCRS.WGS84);
        else if (d == 3)
        	hints.put(Hints.CRS, DefaultGeographicCRS.WGS84_3D);
        ISOGeometryBuilder builder = new ISOGeometryBuilder(hints);
        PointArray coords = builder.createPointArray();

        for (int i = 0; i < length; i++) {
        	coords.add(builder.createDirectPosition(new double[] {ordinates[i * d], ordinates[(i * d) + 1]}));
        }

        return coords;
    }

    /**
     * Construct CoordinateList as described by GTYPE.
     * 
     * <p>
     * GTYPE encodes the following information:
     * 
     * <ul>
     * <li>
     * D: Dimension of ordinates
     * </li>
     * <li>
     * L: Ordinate that represents the LRS measure
     * </li>
     * </ul>
     * </p>
     * 
     * <p>
     * The number of ordinates per coordinate are taken to be D, and the
     * number of ordinates should be a multiple of this value.
     * </p>
     * 
     * <p>
     * In the Special case of GTYPE 2001 and a three ordinates are interpreted
     * as a single Coordinate rather than an error.
     * </p>
     * 
     * <p>
     * For 3-dimensional coordinates we assume z to be the third ordinate. If
     * the LRS measure value is stored in the third ordinate (L=3) we assume a
     * 2-dimensional coordinate.
     * </p>
     *
     * @param geometryFactory CoordinateSequenceFactory used to encode ordiantes for JTS 
     * @param GTYPE Encoding of <b>D</b>imension, <b>L</b>RS and <b>TT</b>ype
     * @param ordinates
     *
     *
     * @throws IllegalArgumentException DOCUMENT ME!
     */
    public static PointArray coordinates(ISOGeometryBuilder geometryFactory,
        final int GTYPE, double[] ordinates) {
        if ((ordinates == null) || (ordinates.length == 0)) {
            return geometryFactory.createPointArray();
        }

        final int D = SDO.D(GTYPE);
        final int L = SDO.L(GTYPE);
        final int TT = SDO.TT(GTYPE);

        //      POINT_TYPE Special Case
        //
        if ((D == 2) && (L == 0) && (TT == 1)) {
        	PointArray pa = geometryFactory.createPointArray(ordinates);
            return pa;
        }

        final int LEN = D; // bugfix 20121231-BK: LEN = D instead of LEN = D + L as Oracle supports only one LRS ordinate!

        if ((ordinates.length % LEN) != 0) {
            // bugfix 20121231-BK: LEN is D instead of D + L
            throw new IllegalArgumentException("Dimension D:" + D 
                + " denote Coordinates " + "of " + LEN
                + " ordinates. This cannot be resolved with"
                + "an ordinate array of length " + ordinates.length);
        }
        
        // bugfix 20121231-BK: throw exception if L > D (4 lines added)
        if (L != 0 && L > D) {
            throw new IllegalArgumentException("Dimension D:" + D
                + " and LRS with L: " + L + " is not supported at a position > D");
        }

        //final int LENGTH = ordinates.length / LEN;

        OrdinateList x = new OrdinateList(ordinates, 0, LEN);
        OrdinateList y = new OrdinateList(ordinates, 1, LEN);
        OrdinateList z = null;

        // bugfix 20121231-BK: add z-Coordinate just if D >= 3 and L != 3
        if (D >= 3 && L != 3) {
            z = new OrdinateList(ordinates, 2, LEN);
        }

        if (L != 0) {
            // bugfix 20121231-BK: Oracle supports only one LRS ordinate! (removed 6 lines, added 2)
            OrdinateList m = new OrdinateList(ordinates, L - 1, LEN);

            // TODO org.geotools.geometry.jts.CoordinateSequenceFactory does not support 4 dimensions - thus we will get only 3 dimensions!
            return coordiantes(geometryFactory, x, y, z, m);
        } else {
            return coordiantes(geometryFactory, x, y, z);
        }
    }

    /**
     * Construct CoordinateSequence with no LRS measures.
     * 
     * <p>
     * To produce two dimension Coordinates pass in <code>null</code> for z
     * </p>
     *
     * @param f DOCUMENT ME!
     * @param x DOCUMENT ME!
     * @param y DOCUMENT ME!
     * @param z DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static PointArray coordiantes(ISOGeometryBuilder geometryFactory,
        OrdinateList x, OrdinateList y, OrdinateList z) {
    	final int LENGTH = x.size();
        PointArray pa = geometryFactory.createPointArray();
        CoordinateReferenceSystem crs = geometryFactory.getCoordinateReferenceSystem();
        if (z != null) {
            for (int i = 0; i < LENGTH; i++) {
            	double[] ordinates = new double[] { x.getDouble(i), y.getDouble(i), z.getDouble(i) };
            	pa.add(new DirectPositionImpl(crs, ordinates));
            }
        } else {
            for (int i = 0; i < LENGTH; i++) {
            	double[] ordinates = new double[] { x.getDouble(i), y.getDouble(i) };
            	pa.add(new DirectPositionImpl(crs, ordinates));
            }
        }

        return pa;
    }
    
    /**
     * Construct CoordinateSequence with no LRS measures.
     * 
     * <p>
     * To produce two dimension Coordinates pass in <code>null</code> for z
     * </p>
     *
     * @param f DOCUMENT ME!
     * @param x DOCUMENT ME!
     * @param y DOCUMENT ME!
     * @param z DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static PointArray coordiantes(ISOGeometryBuilder geometryFactory,
                                                 AttributeList x, 
                                                 AttributeList y, 
                                                 AttributeList z) {
        final int LENGTH = x.size();
        PointArray pa = geometryFactory.createPointArray();
        CoordinateReferenceSystem crs = geometryFactory.getCoordinateReferenceSystem();
        if (z != null) {
            for (int i = 0; i < LENGTH; i++) {
            	double[] ordinates = new double[] { x.getDouble(i), y.getDouble(i), z.getDouble(i) };
            	pa.add(new DirectPositionImpl(crs, ordinates));
            }
        } else {
            for (int i = 0; i < LENGTH; i++) {
            	double[] ordinates = new double[] { x.getDouble(i), y.getDouble(i) };
            	pa.add(new DirectPositionImpl(crs, ordinates));
            }
        }

        return pa;
    }

    /**
     * <p>
     * To produce three dimension Coordinates pass in <code>null</code> for z
     * </p>
     *
     * @param geometryFactory {@link ISOGeometryBuilder}
     * @param x x-ordinates
     * @param y y-ordinates
     * @param z z-ordinates, <code>null</code> for 2D
     * @param m m-ordinates
     *
     * @return {@link PointArray}
     */
    public static PointArray coordiantes(ISOGeometryBuilder geometryFactory,
        OrdinateList x, OrdinateList y, OrdinateList z, OrdinateList m) {
        final int LENGTH = x.size();
        // TODO org.geotools.geometry.jts.LiteCoordinateSequenceFactory does not support 4 dimensions!
        PointArray pa = geometryFactory.createPointArray();
        CoordinateReferenceSystem crs = geometryFactory.getCoordinateReferenceSystem();
        if (z != null) {
            for (int i = 0; i < LENGTH; i++) {
            	double[] ordinates = new double[] { x.getDouble(i), y.getDouble(i), z.getDouble(i) };
            	pa.add(new DirectPositionImpl(crs, ordinates));
            }
        } else {
            for (int i = 0; i < LENGTH; i++) {
            	double[] ordinates = new double[] { x.getDouble(i), y.getDouble(i) };
            	pa.add(new DirectPositionImpl(crs, ordinates));
            }
        }

        return pa;
    }
    
    /**
     * Decode geometry from provided SDO encoded information.
     * 
     * <p></p>
     *
     * @param geometryFactory Used to construct returned Geometry
     * @param GTYPE SDO_GTYPE represents dimension, LRS, and geometry type
     * @param SRID SDO_SRID represents Spatial Reference System
     * @param point
     * @param elemInfo
     * @param ordinates
     *
     * @return Geometry as encoded
     */
	public static Object create(ISOGeometryBuilder geometryFactory, final int GTYPE,
        final int SRID, double[] point, int[] elemInfo, double[] ordinates) {
        final int L = SDO.L(GTYPE);
        final int TT = SDO.TT(GTYPE);
        //double[] list;
        //double[][] lists;

        PointArray coords;

        if ((L == 0) && (TT == 01) && (point != null) && (elemInfo == null)) {
            // Single Point Type Optimization
            coords = SDO.coordinates(geometryFactory, GTYPE, point);
            elemInfo = new int[] { 1, ETYPE.POINT, 1 };
        } else {
            int element = 0;
            int etype = ETYPE(elemInfo, element);
            if (etype == 0) {
               // complex type, search for encapsulated simpletype (with etype != 0)
               int startpointCoordinates = 0;
               
               // look for a simple one
               while (etype == 0) {
                   element++;
                   etype = ETYPE(elemInfo, element);
                   startpointCoordinates = STARTING_OFFSET(elemInfo, element);
               }
               
               // if we found the simple fallback, read it
               if (etype != -1) {
                   int ol = ordinates.length;
                   int elemsToCopy = ol - (startpointCoordinates - 1);
                   double[] newOrdinates = new double[elemsToCopy];
                   System.arraycopy(ordinates, startpointCoordinates - 1, newOrdinates, 0, elemsToCopy);
                   elemInfo = new int[] { 1, etype, INTERPRETATION(elemInfo, element) };
                   ordinates = newOrdinates;
               }
            }
            coords = SDO.coordinates(geometryFactory, GTYPE,
                     ordinates);
        }

        return create(geometryFactory, GTYPE, SRID, elemInfo, 0, coords, -1);
    }

    /**
     * Consturct geometry with SDO encoded information over a CoordinateList.
     * 
     * <p>
     * Helpful when dealing construction Geometries with your own Coordinate
     * Types. The dimensionality specified in GTYPE will be used to interpret
     * the offsets in elemInfo.
     * </p>
     *
     * @param geometryFactory
     * @param GTYPE Encoding of <b>D</b>imension, <b>L</b>RS and <b>TT</b>ype
     * @param SRID
     * @param elemInfo
     * @param triplet DOCUMENT ME!
     * @param coords
     * @param N Number of triplets (-1 for unknown/don't care)
     *
     * @return Geometry as encoded, or null w/ log if it cannot be represented via JTS
     */
    public static Object create(ISOGeometryBuilder geometryFactory, final int GTYPE,
        final int SRID, final int[] elemInfo, final int triplet,
        PointArray coords, final int N) {
        //CurvedGeometryFactory curvedFactory = getCurvedGeometryFactory(geometryFactory);
        
        switch (SDO.TT(GTYPE)) {
        case TT.POINT:
            return createPoint(geometryFactory, GTYPE, SRID, elemInfo, triplet, coords);

        case TT.LINE:
            return createLine(geometryFactory, GTYPE, SRID, elemInfo, triplet, coords, false);

        case TT.POLYGON:
            return createSurface(geometryFactory, GTYPE, SRID, elemInfo, triplet, coords);
            
        case TT.MULTIPOINT:
            return createMultiPoint(geometryFactory, GTYPE, SRID, elemInfo, triplet, coords);

        case TT.MULTICURVE:
            return createMultiLine(geometryFactory, GTYPE, SRID, elemInfo, triplet, coords, N);

        case TT.MULTISURFACE:
            return createMultiPolygon(geometryFactory, GTYPE, SRID, elemInfo, triplet, coords, N, false);
            
        case TT.COLLECTION:
            return createCollection(geometryFactory, GTYPE, SRID, elemInfo, triplet, coords, N);
            
        case TT.SOLID:
            return createSolid(geometryFactory, GTYPE, SRID, elemInfo, triplet, coords);
        
        case TT.UNKNOWN:  
        default:
            LOGGER.warning( "Cannot represent provided SDO STRUCT (GTYPE ="+GTYPE+") using JTS Geometry");
            return null;    
        }        
    }

    private static Geometry createSolid(ISOGeometryBuilder geometryFactory, final int GTYPE,
            final int SRID, final int[] elemInfo, final int triplet,
            PointArray coords) {
    	
    	final int STARTING_OFFSET = STARTING_OFFSET(elemInfo, triplet);
        final int eTYPE = ETYPE(elemInfo, triplet);
        final int INTERPRETATION = INTERPRETATION(elemInfo, triplet);

        ensure( "ELEM_INFO STARTING_OFFSET {1} must be in the range {0}..{1} of COORDINATES",
                1,STARTING_OFFSET, ordinateSize( coords, GTYPE ) );        
        if( !(1 <= STARTING_OFFSET && STARTING_OFFSET <= ordinateSize( coords, GTYPE ))){
            throw new IllegalArgumentException(
                    "ELEM_INFO STARTING_OFFSET "+STARTING_OFFSET+
                    "inconsistent with COORDINATES length "+ordinateSize( coords, GTYPE ) );
        } 
        ensure("ETYPE {0} must be expected SOLID (one of {1})", eTYPE,
                new int[] { ETYPE.SOLID});
        
        Shell shell = null;
        Solid solid = null;
        
        if(INTERPRETATION == 1){
        	shell = createShell(geometryFactory, GTYPE, SRID, elemInfo, triplet + 1, coords);
        	List<Shell> interiors = new ArrayList<Shell>();
        	SolidBoundary solidBoundary = geometryFactory.createSolidBoundary(shell, interiors);
            solid = geometryFactory.createSolid(solidBoundary);
        }
        else{ // In case INTERPRETATION == 3
        	// TODO : Create Solid by Envelope
        	/*
        	PositionFactory pf = geometryFactory.getPositionFactory();
        	DirectPosition dp1 = pf.createDirectPosition( new double[] {coords.get(0).getDirectPosition().getOrdinate(0), 
        			coords.get(0).getDirectPosition().getOrdinate(1), coords.get(0).getDirectPosition().getOrdinate(2)});
        	DirectPosition dp2 = pf.createDirectPosition( new double[] {coords.get(1).getDirectPosition().getOrdinate(0), 
        			coords.get(1).getDirectPosition().getOrdinate(1), coords.get(1).getDirectPosition().getOrdinate(2)});
        	Envelope envelope = new EnvelopeImpl(dp1, dp2);
        	solid = geometryFactory.createSolid(envelope);
        	*/
        }

		return solid;
	}

	private static Shell createShell(ISOGeometryBuilder geometryFactory, final int GTYPE,
            final int SRID, final int[] elemInfo, final int triplet,
            PointArray coords) {
		final int STARTING_OFFSET = STARTING_OFFSET(elemInfo, triplet);
        final int eTYPE = ETYPE(elemInfo, triplet);
        final int INTERPRETATION = INTERPRETATION(elemInfo, triplet);

        ensure( "ELEM_INFO STARTING_OFFSET {1} must be in the range {0}..{1} of COORDINATES",
                1,STARTING_OFFSET, ordinateSize( coords, GTYPE ) );        
        if( !(1 <= STARTING_OFFSET && STARTING_OFFSET <= ordinateSize( coords, GTYPE ))){
            throw new IllegalArgumentException(
                    "ELEM_INFO STARTING_OFFSET "+STARTING_OFFSET+
                    "inconsistent with COORDINATES length "+ordinateSize( coords, GTYPE ) );
        } 
        ensure("ETYPE {0} must be expected SHELL (one of {1})", eTYPE,
                new int[] { ETYPE.COMPOSITE_SURFACE_EXTERIOR, ETYPE.COMPOSITE_SURFACE_INTERIOR});
		
        int triplets = INTERPRETATION;
        List<OrientableSurface> surfaces = new ArrayList<>(triplets);
        for (int i = 1; i <= triplets; i++) {
        	Ring exteriorRing = createLinearRing(geometryFactory, GTYPE, SRID, elemInfo, triplet+i, coords);
        	SurfaceBoundary sb = geometryFactory.createSurfaceBoundary(exteriorRing);
        	OrientableSurface os = geometryFactory.createSurface(sb);
            surfaces.add(os);
        }

        Shell exteriorShell = geometryFactory.createShell(surfaces);
        
		return exteriorShell;
	}

    /**
     * Create Point as encoded.
     *
     * @param geometryFactory
     * @param GTYPE Encoding of <b>D</b>imension, <b>L</b>RS and <b>TT</b>ype
     * @param SRID
     * @param elemInfo
     * @param element
     * @param coords
     *
     * @return Point
     */
    private static Point createPoint(ISOGeometryBuilder geometryFactory, final int GTYPE,
        final int SRID, final int[] elemInfo, final int element,
        PointArray coords) {
        final int STARTING_OFFSET = STARTING_OFFSET(elemInfo, element);
        final int etype = ETYPE(elemInfo, element);
        final int INTERPRETATION = INTERPRETATION(elemInfo, element);
        final int LENGTH = coords.size()*D(GTYPE); 
        
        if (!(STARTING_OFFSET >= 1) || !(STARTING_OFFSET <= LENGTH)) 
            throw new IllegalArgumentException("Invalid ELEM_INFO STARTING_OFFSET ");
        if (etype != ETYPE.POINT)
            throw new IllegalArgumentException("ETYPE "+etype+" inconsistent with expected POINT");
        if (INTERPRETATION != 1){
            LOGGER.warning( "Could not create ISO Point with INTERPRETATION "+INTERPRETATION+" - we only expect 1 for a single point");
            return null;
        }
        
        Point point = geometryFactory.createPoint(subList(geometryFactory, coords, GTYPE, elemInfo, element, false).get(0).getDirectPosition());
        ((PointImpl) point).setUserData(SRID);

        return point;
    }

    /**
     * Create LineString as encoded.
     *
     * @param geometryFactory
     * @param GTYPE Encoding of <b>D</b>imension, <b>L</b>RS and <b>TT</b>ype
     * @param SRID
     * @param elemInfo
     * @param triplet
     * @param coords
     * @param compoundElement TODO
     * @throws IllegalArgumentException If asked to create a curve
     */
    private static Curve createLine(ISOGeometryBuilder geometryFactory, final int GTYPE,
        final int SRID, final int[] elemInfo, final int triplet,
        PointArray coords, boolean compoundElement) {
        final int etype = ETYPE(elemInfo, triplet);
        final int INTERPRETATION = INTERPRETATION(elemInfo, triplet);

        if (etype != ETYPE.LINE && etype != ETYPE.COMPOUND)
            return null;
            

        Curve curve;
        if (etype == ETYPE.LINE && INTERPRETATION == 1) {
            PointArray subList = subList(geometryFactory, coords, GTYPE, elemInfo, triplet, compoundElement);
            curve = geometryFactory.createCurve(subList);
        } else if (etype == ETYPE.LINE && INTERPRETATION == 2) {
        	PointArray subList = subList(geometryFactory, coords, GTYPE, elemInfo, triplet, compoundElement);
            curve = geometryFactory.createCurve(subList);
        } else if (etype == ETYPE.COMPOUND) {
            int triplets = INTERPRETATION;
            List<OrientableCurve> components = new ArrayList<>(triplets);
            for (int i = 1; i <= triplets; i++) {
            	OrientableCurve component = createLine(geometryFactory, GTYPE, SRID, elemInfo, triplet + i, coords, true);
                components.add(component);
            }
            curve = geometryFactory.createCurve(components);
        } else {
            throw new IllegalArgumentException("ELEM_INFO ETYPE " + etype + " with INTERPRETAION "
                    + INTERPRETATION + " not supported by this decoder");
        }
        ((CurveImpl) curve).setUserData(SRID);

        return curve;
    }

    /**
     * Create Polygon as encoded.
     * 
     * <p>
     * Encoded as a one or more triplets in elemInfo:
     * </p>
     * 
     * <ul>
     * <li>
     * Exterior Polygon Ring: first triplet:
     * 
     * <ul>
     * <li>
     * STARTING_OFFSET: position in ordinal ordinate array
     * </li>
     * <li>
     * ETYPE: 1003 (exterior) or 3 (polygon w/ counter clockwise ordinates)
     * </li>
     * <li>
     * INTERPRETATION: 1 for strait edges, 3 for rectanlge
     * </li>
     * </ul>
     * 
     * </li>
     * <li>
     * Interior Polygon Ring(s): remaining triplets:
     * 
     * <ul>
     * <li>
     * STARTING_OFFSET: position in ordinal ordinate array
     * </li>
     * <li>
     * ETYPE: 2003 (interior) or 3 (polygon w/ clockWise ordinates)
     * </li>
     * <li>
     * INTERPRETATION: 1 for strait edges, 3 for rectanlge
     * </li>
     * </ul>
     * 
     * </li>
     * </ul>
     * 
     * <p>
     * The polygon encoding will process subsequent 2003, or 3 triples with
     * clockwise ordering as interior holes.
     * </p>
     * 
     * <p>
     * A subsequent triplet of any other type marks the end of the polygon.
     * </p>
     * 
     * <p>
     * The dimensionality of GTYPE will be used to transalte the
     * <code>STARTING_OFFSET</code> provided by elemInfo into an index into
     * <code>coords</code>.
     * </p>
     *
     * @param geometryFactory Used to construct polygon
     * @param GTYPE Encoding of <b>D</b>imension, <b>L</b>RS and <b>TT</b>ype
     * @param SRID Spatial Reference System
     * @param elemInfo Interpretation of coords
     * @param triplet Triplet in elemInfo to process as a Polygon
     * @param coords Coordinates to interpret using elemInfo
     *
     * @return Polygon as encoded by elemInfo, or null when faced with and
     *         encoding that can not be captured by JTS
     * @throws IllegalArgumentException When faced with an invalid SDO encoding
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private static Surface createSurface(ISOGeometryBuilder geometryFactory, final int GTYPE,
    		final int SRID, final int[] elemInfo, int triplet,
    		PointArray coords) throws IllegalArgumentException  {
        final int STARTING_OFFSET = STARTING_OFFSET(elemInfo, triplet);
        final int eTYPE = ETYPE(elemInfo, triplet);
        final int INTERPRETATION = INTERPRETATION(elemInfo, triplet);

        ensure( "ELEM_INFO STARTING_OFFSET {1} must be in the range {0}..{1} of COORDINATES",
                1,STARTING_OFFSET, ordinateSize( coords, GTYPE ) );        
        if( !(1 <= STARTING_OFFSET && STARTING_OFFSET <= ordinateSize( coords, GTYPE ))){
            throw new IllegalArgumentException(
                    "ELEM_INFO STARTING_OFFSET "+STARTING_OFFSET+
                    "inconsistent with COORDINATES length "+ordinateSize( coords, GTYPE ) );
        } 
        ensure("ETYPE {0} must be expected POLYGON or POLYGON_EXTERIOR (one of {1})", eTYPE,
                new int[] { ETYPE.COMPOUND_POLYGON_EXTERIOR, ETYPE.COMPOUND_POLYGON, ETYPE.POLYGON,
                        ETYPE.POLYGON_EXTERIOR,
                        ETYPE.FACE_EXTERIOR, ETYPE.FACE_EXTERIOR });
        if ((eTYPE != ETYPE.COMPOUND_POLYGON_EXTERIOR) && (eTYPE != ETYPE.COMPOUND_POLYGON)
                && ((INTERPRETATION < 1) || (INTERPRETATION > 4))) {
            LOGGER.warning("Could not create JTS Polygon with INTERPRETATION "
                    + INTERPRETATION
                    + " "
                    + "- we can only support 1 for straight edges, 2 for circular ones, "
                    + "3 for rectangle and 4 for circles");
            return null;
        }

        Ring exteriorRing = createLinearRing(geometryFactory, GTYPE, SRID, elemInfo,
                triplet, coords);
        if (eTYPE == ETYPE.COMPOUND_POLYGON_EXTERIOR) {
            triplet = triplet + elemInfo[2];
        }

        List interiorRings = new LinkedList();
        int etype;
        HOLES: for (int i = triplet + 1; (etype = ETYPE(elemInfo, i)) != -1;) {
            if (etype == ETYPE.POLYGON_INTERIOR) {
            	interiorRings.add(createLinearRing(geometryFactory, GTYPE, SRID, elemInfo, i, coords));
                i++;
            } else if (etype == ETYPE.COMPOUND_POLYGON_INTERIOR) {
                int subelements = INTERPRETATION(elemInfo, i);
                interiorRings.add(createLinearRing(geometryFactory, GTYPE, SRID, elemInfo, i, coords));
                i += subelements;
            } else if (etype == ETYPE.POLYGON) { // need to test Clockwiseness of Ring to see if it
                                                 // is
                                                 // interior or not - (use POLYGON_INTERIOR to avoid
                                                 // pain)
            	Ring ring = createLinearRing(geometryFactory, GTYPE, SRID, elemInfo, i, coords);
            	
                if (isCCW(getPointsOfRing(ring))) { // it is an Interior Hole
                	interiorRings.add(createLinearRing(geometryFactory, GTYPE, SRID, elemInfo, i, coords));
                	i++;
                } else { // it is the next Polygon! - get out of here
                    break HOLES;
                }
            } else { // not a LinearRing - get out of here
                break HOLES;
            }
        }
        
        SurfaceBoundary sb = geometryFactory.createSurfaceBoundary(exteriorRing, interiorRings);
        Surface surface = geometryFactory.createSurface(sb);
        ((SurfaceImpl) surface).setUserData(SRID);

        return surface;
    }

    /**
     * Create Linear Ring for exterior/interior polygon ELEM_INFO triplets.
     * 
     * <p>
     * Encoded as a single triplet in elemInfo:
     * </p>
     * 
     * <ul>
     * <li>
     * STARTING_OFFSET: position in ordinal ordinate array
     * </li>
     * <li>
     * ETYPE: 1003 (exterior) or 2003 (interior) or 3 (unknown order)
     * </li>
     * <li>
     * INTERPRETATION: 1 for strait edges, 3 for rectanlge
     * </li>
     * </ul>
     * 
     * <p>
     * The dimensionality of GTYPE will be used to transalte the
     * <code>STARTING_OFFSET</code> provided by elemInfo into an index into
     * <code>coords</code>.
     * </p>
     *
     * @param geometryFactory
     * @param GTYPE Encoding of <b>D</b>imension, <b>L</b>RS and <b>TT</b>ype
     * @param SRID
     * @param elemInfo
     * @param triplet
     * @param coords
     *
     * @return LinearRing
     *
     * @throws IllegalArgumentException If circle, or curve is requested
     */
    private static Ring createLinearRing(ISOGeometryBuilder geometryFactory,
        final int GTYPE, final int SRID, final int[] elemInfo,
        final int triplet, PointArray coords) {
            
        final int STARTING_OFFSET = STARTING_OFFSET(elemInfo, triplet);
        final int eTYPE = ETYPE(elemInfo, triplet);
        final int INTERPRETATION = INTERPRETATION(elemInfo, triplet);
        final int LENGTH = coords.size()*D(GTYPE);
        
        if (!(STARTING_OFFSET >= 1) || !(STARTING_OFFSET <= LENGTH))
            throw new IllegalArgumentException("ELEM_INFO STARTING_OFFSET "+STARTING_OFFSET+" inconsistent with ORDINATES length "+coords.size());
        
        ensure("ETYPE {0} must be expected POLYGON or POLYGON_EXTERIOR (one of {1})", eTYPE,
                new int[] { ETYPE.COMPOUND_POLYGON, ETYPE.COMPOUND_POLYGON_EXTERIOR,
                        ETYPE.COMPOUND_POLYGON_INTERIOR, ETYPE.POLYGON, ETYPE.POLYGON_EXTERIOR,
                        ETYPE.POLYGON_INTERIOR, ETYPE.FACE_EXTERIOR, ETYPE.FACE_EXTERIOR });
        if ((eTYPE != ETYPE.COMPOUND_POLYGON_EXTERIOR) && (eTYPE != ETYPE.COMPOUND_POLYGON)
                && (eTYPE != ETYPE.COMPOUND_POLYGON_INTERIOR)
                && ((INTERPRETATION < 1) || (INTERPRETATION > 4))) {
            LOGGER.warning("Could not create LinearRing with INTERPRETATION " + INTERPRETATION
                    + " - we can only support 1, 2, 3 and 4");
            return null;
        }
        Ring ring = null;

        if (eTYPE == ETYPE.COMPOUND_POLYGON_EXTERIOR || eTYPE == ETYPE.COMPOUND_POLYGON_INTERIOR) {
            int triplets = INTERPRETATION;
            List<OrientableCurve> components = new ArrayList<>(triplets);
            for (int i = 1; i <= triplets; i++) {
            	OrientableCurve component = createLine(geometryFactory, GTYPE, SRID, elemInfo, triplet + i, coords,
                        i < triplets);
                components.add(component);
            }
            ring = geometryFactory.createRing(components);
        } else if (INTERPRETATION == 1 || INTERPRETATION == 2) {
            PointArray pa = subList(geometryFactory, coords, GTYPE, elemInfo, triplet, false);
            List<OrientableCurve> edges = new ArrayList<OrientableCurve>();
            for( int i = 0; i < pa.size() - 1; i++){
                int start = i;
                int end = i + 1;
                DirectPosition point1 = pa.getDirectPosition( start, null );
                DirectPosition point2 = pa.getDirectPosition( end, null );
                LineSegment segment = geometryFactory.createLineSegment( point1, point2 );
                edges.add( geometryFactory.createCurve( Arrays.asList(segment) ));
            }
            ring = geometryFactory.createRing(edges);
            
        } else if (INTERPRETATION == 3) {
            // rectangle does not maintain measures
        	
        	PointArray pa = subList(geometryFactory, coords, GTYPE, elemInfo, triplet, false);
            DirectPosition LD = pa.get(0).getDirectPosition(); // min point
            DirectPosition RU = pa.get(1).getDirectPosition(); // max point
            DirectPosition LU = null;
            DirectPosition RD = null;
            if(LD.getDimension() == 2) {
            	LU = geometryFactory.createDirectPosition(new double[] {RU.getOrdinate(0),LD.getOrdinate(1)});
            	RD = geometryFactory.createDirectPosition(new double[] {LD.getOrdinate(0),RU.getOrdinate(1)});
            }
            else {
            	// assume that 3-dim LinearRing is flat on AXIS
            	double commonValue;
            	if(LD.getOrdinate(0) == RU.getOrdinate(0)){	// min.x == max.x
            		commonValue = LD.getOrdinate(0);
            		LU = geometryFactory.createDirectPosition(new double[] {commonValue, RU.getOrdinate(1),LD.getOrdinate(2)});
                	RD = geometryFactory.createDirectPosition(new double[] {commonValue, LD.getOrdinate(1),RU.getOrdinate(2)});
            	}
            	else if(LD.getOrdinate(1) == RU.getOrdinate(1)){	// min.y == max.y
            		commonValue = LD.getOrdinate(1);
            		LU = geometryFactory.createDirectPosition(new double[] {RU.getOrdinate(0), commonValue, LD.getOrdinate(2)});
                	RD = geometryFactory.createDirectPosition(new double[] {LD.getOrdinate(0), commonValue, RU.getOrdinate(2)});
            	}
            	else if(LD.getOrdinate(2) == RU.getOrdinate(2)){	// min.z == max.z
            		commonValue = LD.getOrdinate(2);
            		LU = geometryFactory.createDirectPosition(new double[] {RU.getOrdinate(0), LD.getOrdinate(1), commonValue});
                	RD = geometryFactory.createDirectPosition(new double[] {LD.getOrdinate(0), RU.getOrdinate(1), commonValue});
            	}
            }
            
            LineSegment edge1 = geometryFactory.createLineSegment(LD, LU);
    		LineSegment edge2 = geometryFactory.createLineSegment(LU, RU);
    		LineSegment edge3 = geometryFactory.createLineSegment(RU, RD);
    		LineSegment edge4 = geometryFactory.createLineSegment(RD, LD);
    		
    		List<OrientableCurve> edges = new ArrayList<OrientableCurve>();
    		edges.add( geometryFactory.createCurve( Arrays.asList(edge1) ));
    		edges.add( geometryFactory.createCurve( Arrays.asList(edge2) ));
    		edges.add( geometryFactory.createCurve( Arrays.asList(edge3) ));
    		edges.add( geometryFactory.createCurve( Arrays.asList(edge4) ));
        	ring = geometryFactory.createRing(edges);
            
        } else if (INTERPRETATION == 4) {
            // Circle does not maintain measures
            //
        	/*
            CoordinateSequence ext = subList(geometryFactory.getCoordinateSequenceFactory(), coords, GTYPE,
                    elemInfo, triplet, false);
            if (ext.size() != 3) {
                throw new IllegalArgumentException(
                        "The coordinate sequence for the circle creation must contain 3 points, the one at hand contains "
                                + ext.size() + " instead");
            }
            double[] cp = new double[ext.size() * 2 + 2];
            for (int i = 0; i < ext.size(); i++) {
                cp[i * 2] = ext.getOrdinate(i, 0);
                cp[i * 2 + 1] = ext.getOrdinate(i, 1);
            }
            // figure out the other point
            CircularArc arc = new CircularArc(cp[0], cp[1], cp[2], cp[3], cp[4], cp[5]);
            ring = CurvedGeometries.toCircle(arc, geometryFactory, geometryFactory.getTolerance());
            */
        } else {
            throw new IllegalArgumentException("ELEM_INFO INTERPRETAION "
                + elemInfo[2] + " not supported"
                + "for JTS Polygon Linear Rings."
                    + "ELEM_INFO INTERPRETATION 1,2 and 3 are supported");
        }
        
        ((GeometryImpl) ring).setUserData(SRID);

        return ring;
    }

    /**
     * Create MultiPoint as encoded by elemInfo.
     * 
     * <p>
     * Encoded as a single triplet in elemInfo:
     * </p>
     * 
     * <ul>
     * <li>
     * STARTING_OFFSET: position in ordinal ordinate array
     * </li>
     * <li>
     * ETYPE: 1 for Point
     * </li>
     * <li>
     * INTERPRETATION: number of points
     * </li>
     * </ul>
     * 
     *
     * @param geometryFactory Used to construct polygon
     * @param GTYPE Encoding of <b>D</b>imension, <b>L</b>RS and <b>TT</b>ype
     * @param SRID Spatial Reference System
     * @param elemInfo Interpretation of coords
     * @param triplet Triplet in elemInfo to process as a Polygon
     * @param coords Coordinates to interpret using elemInfo
     *
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private static MultiPoint createMultiPoint(ISOGeometryBuilder geometryFactory,
        final int GTYPE, final int SRID, final int[] elemInfo,
        final int triplet, PointArray coords) {
    	
        final int STARTING_OFFSET = STARTING_OFFSET(elemInfo, triplet);
        final int eTYPE = ETYPE(elemInfo, triplet);
        final int INTERPRETATION = INTERPRETATION(elemInfo, triplet);
        final int LENGTH = coords.size() * D(GTYPE); 
        
        if (!(STARTING_OFFSET >= 1) || !(STARTING_OFFSET <= LENGTH))
            throw new IllegalArgumentException("ELEM_INFO STARTING_OFFSET "+STARTING_OFFSET+" inconsistent with ORDINATES length "+coords.size());
        if(!(eTYPE == ETYPE.POINT))
            throw new IllegalArgumentException("ETYPE "+eTYPE+" inconsistent with expected POINT");
        //CH- changed to >= 1, for GEOS-437, Jody and I looked at docs
        //and multipoint is a superset of point, so it should be fine,
        //for cases when there is just one point.  Bart is testing.
        if (!(INTERPRETATION >= 1)){
            LOGGER.warning( "Could not create MultiPoint with INTERPRETATION "+INTERPRETATION+" - representing the number of points");
            return null;
        }

        final int LEN = D(GTYPE);
        int start = (STARTING_OFFSET - 1) / LEN;
        int end = start + INTERPRETATION;
        PointArray pa = subList(geometryFactory, coords, start, end);
        
        MultiPrimitive multiPrimitive = geometryFactory.createMultiPrimitive();
        Set elements = multiPrimitive.getElements();
        Iterator<Position> pointsIter = pa.iterator();
        while(pointsIter.hasNext()){
        	elements.add(geometryFactory.createPoint(pointsIter.next()));
        }
        
        MultiPoint points = geometryFactory.createMultiPoint(elements);
        ((MultiPointImpl) points).setUserData(SRID);
		
        return points;
    }

    /**
     * Create MultiLineString as encoded by elemInfo.
     * 
     * <p>
     * Encoded as a series line of triplets in elemInfo:
     * </p>
     * 
     * <ul>
     * <li>
     * STARTING_OFFSET: position in ordinal ordinate array
     * </li>
     * <li>
     * ETYPE: 2 for Line
     * </li>
     * <li>
     * INTERPRETATION: 1 for straight edges
     * </li>
     * </ul>
     * 
     * <p></p>
     *
     * @param gf Used to construct MultiLineString
     * @param GTYPE Encoding of <b>D</b>imension, <b>L</b>RS and <b>TT</b>ype
     * @param SRID Spatial Reference System
     * @param elemInfo Interpretation of coords
     * @param triplet Triplet in elemInfo to process as a Polygon
     * @param coords Coordinates to interpret using elemInfo
     * @param N Number of triplets (or -1 for rest)
     *
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private static MultiCurve createMultiLine(ISOGeometryBuilder geometryFactory,
        final int GTYPE, final int SRID, final int[] elemInfo,
        final int triplet, PointArray coords, final int N) {

        final int STARTING_OFFSET = STARTING_OFFSET(elemInfo, triplet);
        final int eTYPE = ETYPE(elemInfo, triplet);
        final int INTERPRETATION = INTERPRETATION(elemInfo, triplet);

        final int LENGTH = coords.size()*D(GTYPE);
        
        if (!(STARTING_OFFSET >= 1) || !(STARTING_OFFSET <= LENGTH))
            throw new IllegalArgumentException("ELEM_INFO STARTING_OFFSET "+STARTING_OFFSET+" inconsistent with ORDINATES length "+coords.size());
        if(!(eTYPE == ETYPE.LINE))
            throw new IllegalArgumentException("ETYPE "+eTYPE+" inconsistent with expected LINE");
        if (!(INTERPRETATION == 1)){
            // we cannot represent INTERPRETATION > 1 
            LOGGER.warning( "Could not create MultiLineString with INTERPRETATION "+INTERPRETATION+" - we can only represent 1 for straight edges");
            return null;
        }

        //final int LEN = D(GTYPE);
        final int endTriplet = (N != -1) ? (triplet + N) : (elemInfo.length / 3);

        MultiPrimitive multiPrimitive = geometryFactory.createMultiPrimitive();
        Set elements = multiPrimitive.getElements();
        int etype;
LINES:      // bad bad gotos jody
        for (int i = triplet;
                (i < endTriplet) && ((etype = ETYPE(elemInfo, i)) != -1);
                i++) {
            if (etype == ETYPE.LINE) {
            	elements.add(createLine(geometryFactory, GTYPE, SRID, elemInfo, i, coords, false));
            } else { // not a LinearString - get out of here

                break LINES;    // goto LINES
            }
        }

        MultiCurve curves = geometryFactory.createMultiCurve(elements);
        ((MultiCurveImpl) curves).setUserData(SRID);

        return curves;
    }

    /**
     * Create MultiPolygon as encoded by elemInfo.
     * 
     * <p>
     * Encoded as a series polygon triplets in elemInfo:
     * </p>
     * 
     * <ul>
     * <li>
     * STARTING_OFFSET: position in ordinal ordinate array
     * </li>
     * <li>
     * ETYPE: 2003 or 3 for Polygon
     * </li>
     * <li>
     * INTERPRETATION: 1 for straight edges, 3 for rectangle
     * </li>
     * </ul>
     * 
     * <p></p>
     *
     * @param gf Used to construct MultiLineString
     * @param GTYPE Encoding of <b>D</b>imension, <b>L</b>RS and <b>TT</b>ype
     * @param SRID Spatial Reference System
     * @param elemInfo Interpretation of coords
     * @param triplet Triplet in elemInfo to process as a Polygon
     * @param coords Coordinates to interpret using elemInfo
     * @param N Number of triplets (or -1 for rest)
     *
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private static MultiSurface createMultiPolygon(ISOGeometryBuilder geometryFactory,
        final int GTYPE, final int SRID, final int[] elemInfo,
        final int triplet, PointArray coords, final int N, boolean threeDimensional) {

        final int STARTING_OFFSET = STARTING_OFFSET(elemInfo, triplet);
        final int eTYPE = ETYPE(elemInfo, triplet);
        final int INTERPRETATION = INTERPRETATION(elemInfo, triplet);
        final int LENGTH = coords.size()*D(GTYPE);
        
        if (!(STARTING_OFFSET >= 1) || !(STARTING_OFFSET <= LENGTH))
            throw new IllegalArgumentException("ELEM_INFO STARTING_OFFSET "+STARTING_OFFSET+" inconsistent with ORDINATES length "+coords.size());
        if (!(eTYPE == ETYPE.POLYGON) && !(eTYPE == ETYPE.POLYGON_EXTERIOR)
                && !(eTYPE == ETYPE.FACE_EXTERIOR) && !(eTYPE == ETYPE.FACE_INTERIOR)
                && !(eTYPE == ETYPE.COMPOUND_POLYGON)
                && !(eTYPE == ETYPE.COMPOUND_POLYGON_EXTERIOR))
            throw new IllegalArgumentException("ETYPE "+eTYPE+" inconsistent with expected POLYGON or POLYGON_EXTERIOR");
        if (!(eTYPE == ETYPE.COMPOUND_POLYGON) && !(eTYPE == ETYPE.COMPOUND_POLYGON_EXTERIOR)
                && INTERPRETATION != 1 && INTERPRETATION != 3) {
            LOGGER.warning( "Could not create MultiPolygon with INTERPRETATION "+INTERPRETATION +" - we can only represent 1 for straight edges, or 3 for rectangle");
            return null;
        }
        final int endTriplet = (N != -1) ? (triplet + N)
                                         : ((elemInfo.length / 3) + 1);

        MultiPrimitive multiPrimitive = geometryFactory.createMultiPrimitive();
        Set elements = multiPrimitive.getElements();
        int etype;
POLYGONS: 
        for (int i = triplet;
                (i < endTriplet) && ((etype = ETYPE(elemInfo, i)) != -1);
                i++) {
            if ((etype == ETYPE.POLYGON) || (etype == ETYPE.POLYGON_EXTERIOR)
                    || (etype == ETYPE.FACE_EXTERIOR) || (etype == ETYPE.FACE_INTERIOR)) {
                Surface surface = createSurface(geometryFactory, GTYPE, SRID, elemInfo, i, coords);
                i += surface.getBoundary().getInteriors().size(); // skip interior rings
                elements.add(surface);
            } else if (etype == ETYPE.COMPOUND_POLYGON_EXTERIOR || etype == ETYPE.COMPOUND_POLYGON) {
            	Surface surface = createSurface(geometryFactory, GTYPE, SRID, elemInfo, i, coords);
                int curvilinearElementsCount = getCurvilinearElementsCount(surface);
                i += curvilinearElementsCount - 1;
                elements.add(surface);
            } else { // not a Polygon - get out here

                break POLYGONS;
            }
        }

        MultiSurface surfaces = geometryFactory.createMultiSurface(elements);
        ((MultiSurfaceImpl) surfaces).setUserData(SRID);

        return surfaces;
    }

    private static int getCurvilinearElementsCount(Surface surface) {
        int sum = getCurvilinearElementsCount(surface.getBoundary().getExterior());
        for (Ring interiorRing : surface.getBoundary().getInteriors()) {
            sum += getCurvilinearElementsCount(interiorRing);
        }
        return sum;
    }

    private static int getCurvilinearElementsCount(Ring ring) {
    	// TODO : When this function using?
    	return 1;
    }

    /**
     * Create MultiPolygon as encoded by elemInfo.
     * 
     * <p>
     * Encoded as a series polygon triplets in elemInfo:
     * </p>
     * 
     * <ul>
     * <li>
     * STARTING_OFFSET: position in ordinal ordinate array
     * </li>
     * <li>
     * ETYPE: 2003 or 3 for Polygon
     * </li>
     * <li>
     * INTERPRETATION: 1 for straight edges, 2 for rectangle
     * </li>
     * </ul>
     * 
     * <p></p>
     *
     * TODO: Confirm that createCollection is not getting cut&paste mistakes from polygonCollection
     * 
     * @param geometryFactory Used to construct MultiLineString
     * @param GTYPE Encoding of <b>D</b>imension, <b>L</b>RS and <b>TT</b>ype
     * @param SRID Spatial Reference System
     * @param elemInfo Interpretation of coords
     * @param triplet Triplet in elemInfo to process as a Polygon
     * @param coords Coordinates to interpret using elemInfo
     * @param N Number of triplets (or -1 for rest)
     *
     * @return GeometryCollection
     *
     * @throws IllegalArgumentException DWhen faced with an encoding error
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private static MultiPrimitive createCollection(ISOGeometryBuilder geometryFactory,
        final int GTYPE, final int SRID, final int[] elemInfo,
        final int triplet, PointArray coords, final int N) {
    	
        final int STARTING_OFFSET = STARTING_OFFSET(elemInfo, triplet);

        final int LENGTH = coords.size()*D(GTYPE);
        
        if (!(STARTING_OFFSET >= 1) || !(STARTING_OFFSET <= LENGTH))
            throw new IllegalArgumentException("ELEM_INFO STARTING_OFFSET "+STARTING_OFFSET+" inconsistent with ORDINATES length "+coords.size());
        
        final int endTriplet = (N != -1) ? (triplet + N)
                                         : ((elemInfo.length / 3) + 1);

        MultiPrimitive multiPrimitive = geometryFactory.createMultiPrimitive();
        Set elements = multiPrimitive.getElements();
        int etype;
        int interpretation;
        Geometry geom;

GEOMETRYS: 
        for (int i = triplet; i < endTriplet; i++) {
            etype = ETYPE(elemInfo, i);
            interpretation = INTERPRETATION(elemInfo, i);

            switch (etype) {
            case -1:
                break GEOMETRYS; // We are the of the list - get out of here

            case ETYPE.POINT:

                if (interpretation == 1) {
                    geom = createPoint(geometryFactory, GTYPE, SRID, elemInfo, i, coords);
                } else if (interpretation > 1) {
                    geom = createMultiPoint(geometryFactory, GTYPE, SRID, elemInfo, i, coords);
                } else {
                    throw new IllegalArgumentException(
                        "ETYPE.POINT requires INTERPRETATION >= 1");
                }

                break;

            case ETYPE.LINE:
                geom = createLine(geometryFactory, GTYPE, SRID, elemInfo, i, coords, false);

                break;

            case ETYPE.POLYGON:
            case ETYPE.POLYGON_EXTERIOR:
            	// TODO : Need to create createSurface function instead createPolygon
            	 
                geom = createSurface(geometryFactory, GTYPE, SRID, elemInfo, i, coords);
                i += ((Surface) geom).getBoundary().getInteriors().size();
                break;

            case ETYPE.POLYGON_INTERIOR:
                throw new IllegalArgumentException(
                    "ETYPE 2003 (Polygon Interior) no expected in a GeometryCollection"
                    + "(2003 is used to represent polygon holes, in a 1003 polygon exterior)");

            case ETYPE.CUSTOM:
            case ETYPE.COMPOUND:
            case ETYPE.COMPOUND_POLYGON:
            case ETYPE.COMPOUND_POLYGON_EXTERIOR:
            case ETYPE.COMPOUND_POLYGON_INTERIOR:default:
                throw new IllegalArgumentException("ETYPE " + etype
                    + " not representable as a JTS Geometry."
                    + "(Custom and Compound Straight and Curved Geometries not supported)");
            }

            elements.add(geom);
        }

        MultiPrimitive geoms = geometryFactory.createMultiPrimitive(elements);
        ((MultiPrimitiveImpl) geoms).setUserData(SRID);

        return geoms;
    }	
}