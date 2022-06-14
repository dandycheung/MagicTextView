package com.qwerjk.better_text;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.WeakHashMap;

public class MagicTextView extends TextView {
	public static class Shadow {
		protected final float r;
		protected final float dx;
		protected final float dy;
		protected final int color;

		public Shadow(float r, float dx, float dy, int color) {
			this.r = r;
			this.dx = dx;
			this.dy = dy;
			this.color = color;
		}
	}

	static class InnerShadow extends Shadow {
		protected final BlurMaskFilter filter;
		public InnerShadow(float r, float dx, float dy, int color) {
			super(r, dx, dy, color);
			filter = new BlurMaskFilter(r, BlurMaskFilter.Blur.NORMAL);
		}
	}

	private ArrayList<Shadow> outerShadows;
	private ArrayList<InnerShadow> innerShadows;

	private WeakHashMap<String, Pair<Canvas, Bitmap>> canvasStore;

	private Canvas tempCanvas;
	private Bitmap tempBitmap;

	private Drawable foregroundDrawable;

	private float strokeWidth;
	private Integer strokeColor;
	private Join strokeJoin;
	private float strokeMiter;

	private int[] lockedCompoundPadding;
	private boolean frozen = false;

	public MagicTextView(Context context) {
		super(context);
		init(null);
	}

	public MagicTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public MagicTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}

	public void init(AttributeSet attrs){
		outerShadows = new ArrayList<>();
		innerShadows = new ArrayList<>();

		if (canvasStore == null)
		    canvasStore = new WeakHashMap<>();

		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MagicTextView);

            String typefaceName = a.getString( R.styleable.MagicTextView_typeface);
            if (typefaceName != null) {
                Typeface tf = Typeface.createFromAsset(getContext().getAssets(), String.format("fonts/%s.ttf", typefaceName));
                setTypeface(tf);
            }

			if (a.hasValue(R.styleable.MagicTextView_foreground)) {
				Drawable foreground = a.getDrawable(R.styleable.MagicTextView_foreground);
				if (foreground != null)
					setForegroundDrawable(foreground);
				else
					setTextColor(a.getColor(R.styleable.MagicTextView_foreground, 0xff000000));
			}

			if (a.hasValue(R.styleable.MagicTextView_background)) {
				Drawable background = a.getDrawable(R.styleable.MagicTextView_background);
				if (background != null)
					setBackgroundDrawable(background);
				else
					setBackgroundColor(a.getColor(R.styleable.MagicTextView_background, 0xff000000));
			}

			if (a.hasValue(R.styleable.MagicTextView_innerShadowColor)) {
				addInnerShadow(a.getDimensionPixelSize(R.styleable.MagicTextView_innerShadowRadius, 0),
								a.getDimensionPixelOffset(R.styleable.MagicTextView_innerShadowDx, 0),
								a.getDimensionPixelOffset(R.styleable.MagicTextView_innerShadowDy, 0),
								a.getColor(R.styleable.MagicTextView_innerShadowColor, 0xff000000));
			}

			if (a.hasValue(R.styleable.MagicTextView_outerShadowColor)) {
				addOuterShadow(a.getDimensionPixelSize(R.styleable.MagicTextView_outerShadowRadius, 0),
								a.getDimensionPixelOffset(R.styleable.MagicTextView_outerShadowDx, 0),
								a.getDimensionPixelOffset(R.styleable.MagicTextView_outerShadowDy, 0),
								a.getColor(R.styleable.MagicTextView_outerShadowColor, 0xff000000));
			}

			if (a.hasValue(R.styleable.MagicTextView_strokeColor)) {
				float strokeWidth = a.getDimensionPixelSize(R.styleable.MagicTextView_strokeWidth, 1);
				int strokeColor = a.getColor(R.styleable.MagicTextView_strokeColor, 0xff000000);
				float strokeMiter = a.getDimensionPixelSize(R.styleable.MagicTextView_strokeMiter, 10);
				Join strokeJoin = null;

				switch (a.getInt(R.styleable.MagicTextView_strokeJoinStyle, 0)) {
					case 0: strokeJoin = Join.MITER; break;
					case 1: strokeJoin = Join.BEVEL; break;
					case 2: strokeJoin = Join.ROUND; break;
				}

				setStroke(strokeWidth, strokeColor, strokeJoin, strokeMiter);
			}

			a.recycle();
		}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
        	&& (innerShadows.size() > 0 || foregroundDrawable != null)) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
	}

	public void setStroke(float width, int color, Join join, float miter) {
		strokeWidth = width;
		strokeColor = color;
		strokeJoin = join;
		strokeMiter = miter;
	}

	public void setStroke(float width, int color){
		setStroke(width, color, Join.MITER, 10);
	}

	public void addOuterShadow(float r, float dx, float dy, int color){
		if (r == 0)
			r = 0.0001f;

		outerShadows.add(new Shadow(r, dx, dy, color));
	}

	public void addInnerShadow(float r, float dx, float dy, int color) {
		if (r == 0)
			r = 0.0001f;

		innerShadows.add(new InnerShadow(r, dx, dy, color));
	}

	public void clearInnerShadows(){
		innerShadows.clear();
	}

	public void clearOuterShadows(){
		outerShadows.clear();
	}

	public void setForegroundDrawable(Drawable d){
		foregroundDrawable = d;
	}

	public Drawable getForeground() {
		return foregroundDrawable != null ? foregroundDrawable : new ColorDrawable(getCurrentTextColor());
	}

	private final Rect rcBounds = new Rect();
	private final PorterDuffXfermode srcAtop = new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP);
	private final PorterDuffXfermode dstOut = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		freeze();

		Drawable restoreBackground = getBackground();
		Drawable[] restoreDrawables = getCompoundDrawables();
		int restoreColor = getCurrentTextColor();

		setCompoundDrawables(null,  null, null, null);

		for (Shadow shadow : outerShadows) {
			setShadowLayer(shadow.r, shadow.dx, shadow.dy, shadow.color);
			super.onDraw(canvas);
		}

		setShadowLayer(0,0,0,0);
		setTextColor(restoreColor);

		if (foregroundDrawable != null && foregroundDrawable instanceof BitmapDrawable) {
			generateTempCanvas();

			super.onDraw(tempCanvas);

			Paint paint = ((BitmapDrawable) foregroundDrawable).getPaint();
			paint.setXfermode(srcAtop);
			canvas.getClipBounds(rcBounds);
			foregroundDrawable.setBounds(rcBounds);
			foregroundDrawable.draw(tempCanvas);
			canvas.drawBitmap(tempBitmap, 0, 0, null);
			tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
		}

		if (strokeColor != null) {
			TextPaint paint = getPaint();
			paint.setStyle(Style.STROKE);
			paint.setStrokeJoin(strokeJoin);
			paint.setStrokeMiter(strokeMiter);

			setTextColor(strokeColor);
			paint.setStrokeWidth(strokeWidth);

			super.onDraw(canvas);
			paint.setStyle(Style.FILL);
			setTextColor(restoreColor);
		}

		if (innerShadows.size() > 0) {
			generateTempCanvas();
			TextPaint paint = getPaint();
			for (InnerShadow shadow : innerShadows) {
				setTextColor(shadow.color);
				super.onDraw(tempCanvas);
				setTextColor(0xFF000000);
				paint.setXfermode(dstOut);
				paint.setMaskFilter(shadow.filter);

                tempCanvas.save();
                tempCanvas.translate(shadow.dx, shadow.dy);
				super.onDraw(tempCanvas);
				tempCanvas.restore();
				canvas.drawBitmap(tempBitmap, 0, 0, null);
				tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
				
				paint.setXfermode(null);
				paint.setMaskFilter(null);
				setTextColor(restoreColor);
				setShadowLayer(0,0,0,0);
			}
		}

		setCompoundDrawablesWithIntrinsicBounds(restoreDrawables[0], restoreDrawables[1], restoreDrawables[2], restoreDrawables[3]);

		setBackgroundDrawable(restoreBackground);
		setTextColor(restoreColor);

		unfreeze();
	}

	private void generateTempCanvas() {
	    @SuppressLint("DefaultLocale")
		String key = String.format("%dx%d", getWidth(), getHeight());
	    Pair<Canvas, Bitmap> stored = canvasStore.get(key);
	    if (stored != null) {
	        tempCanvas = stored.first;
	        tempBitmap = stored.second;
	    } else {
            tempCanvas = new Canvas();
            tempBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            tempCanvas.setBitmap(tempBitmap);
            canvasStore.put(key, new Pair<>(tempCanvas, tempBitmap));
	    }
	}

	// Keep these things locked while onDraw in processing
	public void freeze() {
		lockedCompoundPadding = new int[] {
			getCompoundPaddingLeft(),
			getCompoundPaddingRight(),
			getCompoundPaddingTop(),
			getCompoundPaddingBottom()
		};

		frozen = true;
	}
	
	public void unfreeze() {
		frozen = false;
	}
    
    @Override
    public void requestLayout() {
        if (!frozen)
        	super.requestLayout();
    }

	@Override
	public void postInvalidate() {
		if (!frozen)
			super.postInvalidate();
	}

   @Override
    public void postInvalidate(int left, int top, int right, int bottom) {
        if (!frozen)
        	super.postInvalidate(left, top, right, bottom);
    }

	@Override
	public void invalidate() {
		if (!frozen)
			super.invalidate();
	}

	@Override
	public void invalidate(Rect rect) {
		if (!frozen)
			super.invalidate(rect);
	}

	@Override
	public void invalidate(int l, int t, int r, int b) {
		if (!frozen)
			super.invalidate(l, t, r, b);
	}

	@Override
	public int getCompoundPaddingLeft() {
		return !frozen ? super.getCompoundPaddingLeft() : lockedCompoundPadding[0];
	}

	@Override
	public int getCompoundPaddingRight() {
		return !frozen ? super.getCompoundPaddingRight() : lockedCompoundPadding[1];
	}

	@Override
	public int getCompoundPaddingTop() {
		return !frozen ? super.getCompoundPaddingTop() : lockedCompoundPadding[2];
	}

	@Override
	public int getCompoundPaddingBottom() {
		return !frozen ? super.getCompoundPaddingBottom() : lockedCompoundPadding[3];
	}
}
