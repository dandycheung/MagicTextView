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

    private void setGetPath(boolean isGetPath) {
        this.isGetPath = isGetPath;
    }

    public Path[] getOuterPath() {
        return outerPath;
    }

    private void setOuterPath(Path[] outerPath) {
        this.outerPath = outerPath;
    }

    public Path[] getInnerPath() {
        return innerPath;
    }

    private void setInnerPath(Path[] innerPath) {
        this.innerPath = innerPath;
    }

    public List<Contour> getOuterContours() {
        return outerContours;
    }

    private void setOuterContours(List<Contour> outerContours) {
        this.outerContours = outerContours;
    }

    public List<Contour> getInnerContours() {
        return innerContours;
    }

    private void setInnerContours(List<Contour> innerContours) {
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

        float borderSize = getBorderSize();

        // Border size = 0 -> 3% width or height for small bitmap
        int sizeActual = Math.min(width, height);
        if (sizeActual < 150f)
            borderSize = (borderSize / 50f) * (sizeActual * 0.3f);

        Paint paint = new Paint();
        paint.setStrokeWidth(borderSize);
        paint.setColor(getColor());
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setFilterBitmap(true);
        paint.setStyle(Style.STROKE);

        /*
         * You should store this Path for next time draw
         */
        if (!isGetPath()) {
            Log.e("is Run ", "is Run ");

            // init paint to get extract alpha bitmap
            Paint alphaPaint = new Paint();
            alphaPaint.setDither(true);
            alphaPaint.setAntiAlias(true);
            alphaPaint.setColor(Color.BLACK);

            int[] offset = new int[2];
            Bitmap bmAlpha = src.extractAlpha(alphaPaint, offset);

            Bitmap resultBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
            Canvas canvas = new Canvas(resultBitmap);

            Paint mainPaint = new Paint();
            mainPaint.setDither(true);
            mainPaint.setAntiAlias(true);
            mainPaint.setFilterBitmap(true);
            mainPaint.setStrokeJoin(Paint.Join.ROUND);
            mainPaint.setStrokeCap(Paint.Cap.SQUARE);
            canvas.drawBitmap(bmAlpha, 0, 0, mainPaint);

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
        Matrix matrix = new Matrix();
        matrix.setScale((width - borderSize) / (width * 1f),
            (height - borderSize) / (height * 1f), width / 2f, height / 2f);

        Canvas canvasBorder = new Canvas(finalBitmap);

        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);

        for (Path p : getOuterPath()) {
            Path temp = new Path(p);
            temp.transform(matrix);
            canvasBorder.drawPath(temp, paint);
        }

        for (Path p : getInnerPath()) {
            Path temp = new Path(p);
            temp.transform(matrix);
            canvasBorder.drawPath(temp, paint);
        }

        canvasBorder.drawBitmap(src, matrix, paint);

        return finalBitmap;
    }
}
