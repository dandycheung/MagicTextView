package com.qwerjk.contour;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Region;

/*
 * Modified 2010-08-01
 */

public class ContourTracer {
    private static final byte FOREGROUND = 1;
    private static final byte BACKGROUND = 0;

    private List<Contour> outerContours = null;
    private List<Contour> innerContours = null;
    private List<Contour> randomInnerContours = null;
    private List<Contour> randomInnerContoursInside = null;

    private List<Contour> randomOuterContours = null;
    private List<Contour> randomOuterContoursInside = null;

    private int regionId = 0;

    private final Bitmap ip;
    private final int width;
    private final int height;
    private byte[][] pixelArray;
    private int[][] labelArray;

    // label values in labelArray can be:
    // 0 ... unlabeled
    // -1 ... previously visited background pixel
    // >0 ... valid label

    // constructor method
    public ContourTracer(Bitmap ip) {
        this.ip = ip;
        this.width = ip.getWidth();
        this.height = ip.getHeight();

        makeAuxArrays();
        findAllContours();
    }

    public List<Contour> getOuterContours() {
        return outerContours;
    }

    public List<Contour> getInnerContours() {
        return innerContours;
    }

    public List<Contour> getRandomInnerContours() {
        return randomInnerContours;
    }

    public List<Contour> getRandomOuterContours() {
        return randomOuterContours;
    }

    public List<Contour> getRandomInnerInsideContours() {
        return randomInnerContoursInside;
    }

    public List<Contour> getRandomOuterInsideContours() {
        return randomOuterContoursInside;
    }

    // Return the region label (if existent) at position (x, y).
    public int getLabel(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height)
            return labelArray[y][x];

        return BACKGROUND;
    }

    // non-public methods -----------------------------------------------

    // Create auxil. arrays, which are "padded", i.e.,
    // are 2 rows and 2 columns larger than the image:
    private void makeAuxArrays() {
        int h = ip.getHeight();
        int w = ip.getWidth();

        pixelArray = new byte[h + 2][w + 2];
        labelArray = new int[h + 2][w + 2]; // initialized to zero (0)

        // initialize pixelArray[][]:
        // for (int j = 0; j < pixelArray.length; j++) {
        // for (int i = 0; i < pixelArray[j].length; i++) {
        // pixelArray[j][i] = BACKGROUND;
        // }
        // }

        // copy the contents of the binary image to pixelArray,
        // starting at array coordinate [1][1], i.e., centered:
        for (int v = 0; v < h; v++) {
            for (int u = 0; u < w; u++) {
                // Log.e("Vao day ngoai nay"," Vao day ngoai nay");
                int alpha = Color.alpha(ip.getPixel(u, v));
                if (alpha > 125) {
                    // Log.e("Vao day"," Vao day ");
                    pixelArray[v + 1][u + 1] = FOREGROUND;
                }
            }
        }
    }

    private Contour traceOuterContour(int cx, int cy, int label) {
        Contour cont = new Contour(label);
        traceContour(cx, cy, label, 0, cont);
        return cont;
    }

    private Contour traceInnerContour(int cx, int cy, int label) {
        Contour cont = new Contour(label);
        traceContour(cx, cy, label, 1, cont);
        return cont;
    }

    // Trace one contour starting at (xS, yS) in direction dS with label label
    private Contour traceContour(int xS, int yS, int label, int dS, Contour cont) {
        int xT, yT; // T = successor of starting point (xS,yS)
        int xP, yP; // P = previous contour point
        int xC, yC; // C = current contour point

        Point pt = new Point(xS, yS);
        int dNext = findNextPoint(pt, dS);
        cont.addPoint(pt);

        xC = xT = pt.x;
        yC = yT = pt.y;

        boolean done = (xS == xT && yS == yT); // true if isolated pixel

        while (!done) {
            labelArray[yC][xC] = label;

            pt = new Point(xC, yC);

            int dSearch = (dNext + 6) % 8;
            dNext = findNextPoint(pt, dSearch);

            xP = xC;
            yP = yC;
            xC = pt.x;
            yC = pt.y;

            // are we back at the starting position?
            done = (xP == xS && yP == yS && xC == xT && yC == yT);
            if (!done)
                cont.addPoint(pt);
        }

        return cont;
    }

    // Starts at Point pt in direction dir
    // returns the final tracing direction and modifies pt
    private int findNextPoint(Point pt, int dir) {
        final int[][] delta = {
            { 1, 0 },  { 1, 1 },   { 0, 1 },  { -1, 1 },
            { -1, 0 }, { -1, -1 }, { 0, -1 }, { 1, -1 }
        };

        for (int i = 0; i < 7; i++) {
            int x = pt.x + delta[dir][0];
            int y = pt.y + delta[dir][1];
            if (pixelArray[y][x] == BACKGROUND) {
                labelArray[y][x] = -1; // mark surrounding background pixels
                dir = (dir + 1) % 8;
            } else { // found non-background pixel
                pt.x = x;
                pt.y = y;
                break;
            }
        }

        return dir;
    }

    private void findAllContours() {
        outerContours = new ArrayList<>();
        innerContours = new ArrayList<>();
        randomInnerContours = new ArrayList<>();
        randomOuterContours = new ArrayList<>();
        randomInnerContoursInside = new ArrayList<>();
        randomOuterContoursInside = new ArrayList<>();

        int label; // current label

        // scan top to bottom, left to right
        for (int v = 1; v < pixelArray.length - 1; v++) {
            label = 0; // no label

            for (int u = 1; u < pixelArray[v].length - 1; u++) {
                if (pixelArray[v][u] == FOREGROUND) {
                    if (label != 0) { // keep using same label
                        labelArray[v][u] = label;
                    } else {
                        label = labelArray[v][u];
                        if (label == 0) { // unlabeled - new outer contour
                            label = ++regionId;

                            Contour oc = traceOuterContour(u, v, label);
                            outerContours.add(oc);

                            labelArray[v][u] = label;
                        }
                    }
                } else { // BACKGROUND pixel
                    if (label != 0) {
                        if (labelArray[v][u] == 0) { // unlabeled - new inner contour
                            Contour ic = traceInnerContour(u - 1, v, label);
                            innerContours.add(ic);
                        }
                        label = 0;
                    }
                }
            }
        }

        // shift back to original coordinates
        Contour.moveContoursBy(outerContours, -1, -1);
        Contour.moveContoursBy(innerContours, -1, -1);
    }
}
