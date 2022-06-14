package com.qwerjk.contour;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.Log;

import java.util.List;

public class Border {
    private float borderSize; // borderSize From 0-50
    private int color;

    private boolean isGetPath;
    private Path[] outerPath;
    private Path[] innerPath;
    private List<Contour> outerContours;
    private List<Contour> innerContours;

    public Border() {
        borderSize = 12;
        color = Color.RED;
    }

    public float getBorderSize() {
        return borderSize;
    }

    public void setBorderSize(float borderSize) {
        this.borderSize = borderSize;
    }

    public boolean isGetPath() {
        return isGetPath;
    }

    public void setGetPath(boolean isGetPath) {
        this.isGetPath = isGetPath;
    }

    public Path[] getOuterPath() {
        return outerPath;
    }

    public void setOuterPath(Path[] outerPath) {
        this.outerPath = outerPath;
    }

    public Path[] getInnerPath() {
        return innerPath;
    }

    public void setInnerPath(Path[] innerPath) {
        this.innerPath = innerPath;
    }

    public List<Contour> getOuterContours() {
        return outerContours;
    }

    public void setOuterContours(List<Contour> outerContours) {
        this.outerContours = outerContours;
    }

    public List<Contour> getInnerContours() {
        return innerContours;
    }

    public void setInnerContours(List<Contour> innerContours) {
        this.innerContours = innerContours;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public Bitmap process(Bitmap src) {
        int width = src.getWidth();
        int height = src.getHeight();
        Log.e("src size: ", " " + src.getWidth() + " va " + src.getHeight());

        float borderSize; // sizeBorder From 0 -> 50
        float sizeBorder = getBorderSize();
        int sizeActual = Math.min(width, height);

        Paint mPaint = new Paint();

        // Border size = 0 -> 3% width or height Bitmap with small bitmap
        float newSize = sizeActual * 0.3f;
        if (sizeActual < 150f)
            borderSize = (sizeBorder / 50f) * newSize;
        else
            borderSize = sizeBorder;

        mPaint.setStrokeWidth(borderSize);

        mPaint.setColor(getColor());
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setFilterBitmap(true);
        mPaint.setStyle(Style.STROKE);

        /*
         * You should store this Path for next time draw
         */
        if (!isGetPath()) {
            Log.e("is Run ", "is Run ");

            // init paint to get extract alpha bitmap
            Paint mPainting = new Paint();
            mPainting.setDither(true);
            mPainting.setAntiAlias(true);
            mPainting.setColor(Color.BLACK);

            int[] offset = new int[2];
            Bitmap bmAlpha = src.extractAlpha(mPainting, offset);

            Bitmap resultBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
            Canvas mCanvas = new Canvas(resultBitmap);

            Paint mainPaint = new Paint();
            mainPaint.setDither(true);
            mainPaint.setAntiAlias(true);
            mainPaint.setFilterBitmap(true);
            mainPaint.setStrokeJoin(Paint.Join.ROUND);
            mainPaint.setStrokeCap(Paint.Cap.SQUARE);
            mCanvas.drawBitmap(bmAlpha, 0, 0, mainPaint);

            if (!bmAlpha.isRecycled())
                bmAlpha.recycle();

            // Find contour
            ContourTracer tracer = new ContourTracer(resultBitmap);
            setOuterContours(tracer.getOuterContours());
            setInnerContours(tracer.getInnerContours());

            setOuterPath(Contour.makePolygons(getOuterContours()));
            setInnerPath(Contour.makePolygons(getInnerContours()));
            if (!resultBitmap.isRecycled())
                resultBitmap.recycle();

            setGetPath(true);
        }

        Bitmap finalBitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        Matrix mMatrix = new Matrix();
        mMatrix.setScale((width - borderSize) / (width * 1f),
            (height - borderSize) / (height * 1f), width / 2f, height / 2f);

        Canvas canvasBorder = new Canvas(finalBitmap);

        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        for (Path p : getOuterPath()) {
            Path temp = new Path(p);
            temp.transform(mMatrix);
            canvasBorder.drawPath(temp, mPaint);
        }

        for (Path p : getInnerPath()) {
            Path temp = new Path(p);
            temp.transform(mMatrix);
            canvasBorder.drawPath(temp, mPaint);
        }

        canvasBorder.drawBitmap(src, mMatrix, mPaint);

        return finalBitmap;
    }
}
