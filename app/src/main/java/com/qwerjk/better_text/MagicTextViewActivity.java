package com.qwerjk.better_text;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import com.qwerjk.contour.Border;

public class MagicTextViewActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Contour test, https://github.com/dandycheung/border_image_android
        // Contour with image
        ImageView imageImv = findViewById(R.id.imv2);
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        Border mBorderImg = new Border();
        imageImv.setImageBitmap(mBorderImg.process(icon));

        // Contour with text
        imageImv = findViewById(R.id.imv4);
        Bitmap textImg = BitmapFactory.decodeResource(getResources(), R.drawable.text);
        Border mBorderText = new Border();

        imageImv.setImageBitmap(mBorderText.process(textImg));
    }
}
