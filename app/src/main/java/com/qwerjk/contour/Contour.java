package com.qwerjk.contour;

/**
 * This sample code is made available as part of the book "Digital Image
 * Processing - An Algorithmic Introduction using Java" by Wilhelm Burger
 * and Mark J. Burge, Copyright (C) 2005-2008 Springer-Verlag Berlin,
 * Heidelberg, New York.
 * Note that this code comes with absolutely no warranty of any kind.
 * See http://www.imagingbook.com for details and licensing conditions.
 *
 * Date: 2010/08/01
 */

/*
 * 2006-01-01 made Java 5.0 compliant (generic ArrayList, Iterator)
 */
// import java.awt.Point;
// import java.awt.Polygon;
// import java.awt.Shape;
// import java.awt.geom.Ellipse2D;

import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Point;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Contour {
    private static final int INITIAL_SIZE = 50;

    private int label;
    public List<Point> points;

    public Contour(int label, int size) {
        this.label = label;
        points = new ArrayList<>(size);
    }

    public Contour(int label) {
        this.label = label;
        points = new ArrayList<>(INITIAL_SIZE);
    }

    public Contour(Contour contour) {
        label = contour.label;
        points = contour.points;
    }

    public Contour() {
    }

    void addPoint(Point n) {
        points.add(n);
    }

    // --------------------- drawing ------------
    public Path createPathFromPoint(int[] xList, int[] yList) {
        Path path = new Path();

        boolean isMove = false;
        for (int i = 0; i < xList.length; i++) {
            if (isMove) {
                path.lineTo(xList[i], yList[i]);
            } else {
                path.moveTo(xList[i], yList[i]);
                isMove = true;
            }
        }

        return path;
    }

    Path makePolygon() {
        int m = points.size();
        if (m > 1) {
            int[] xPoints = new int[m];
            int[] yPoints = new int[m];

            int k = 0;
            Iterator<Point> itr = points.iterator();
            while (itr.hasNext() && k < m) {
                Point cpt = itr.next();
                xPoints[k] = cpt.x;
                yPoints[k] = cpt.y;
                k++;
            }

            return createPathFromPoint(xPoints, yPoints);
        } else { // use circles for isolated pixels
            Point cpt = points.get(0);
            return createOval(cpt.x - 0.1f, cpt.y - 0.1f, 0.2f, 0.2f);
        }
    }

    public Path createOval(float x, float y, float w, float h) {
        Path path = new Path();
        path.addOval(new RectF(x, y, x + w, y + h), Direction.CW);
        return path;
    }

    public static Path[] makePolygons(List<Contour> contours) {
        if (contours == null)
            return null;

        Path[] pa = new Path[contours.size()];

        int i = 0;
        for (Contour c : contours)
            pa[i++] = c.makePolygon();

        return pa;
    }

    private void moveBy(int dx, int dy) {
        for (Point pt : points)
            translate(pt, dx, dy);
    }

    public Point translate(Point srcPoint, int dx, int dy) {
        srcPoint.x += dx;
        srcPoint.y += dy;
        return srcPoint;
    }

    static void moveContoursBy(List<Contour> contours, int dx, int dy) {
        for (Contour c : contours) {
            c.moveBy(dx, dy);
        }
    }

    // --------------------- contour statistics ------------

    public int getLength() {
        return points.size();
    }

    public String toString() {
        return "Contour " + label + ": " + getLength() + " points";
    }
}
