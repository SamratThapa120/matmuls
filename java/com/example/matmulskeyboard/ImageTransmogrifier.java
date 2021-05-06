package com.example.matmulskeyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.Image;
import android.media.ImageReader;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import static android.content.Context.WINDOW_SERVICE;

public class ImageTransmogrifier implements ImageReader.OnImageAvailableListener {
    private static final String ENGLISH_LANGUAGE ="eng" ;
    private final int width;
    private final int height;
    private final ImageReader imageReader;
    private final MyInputMethodService svc;
    private final int density;
//    private final TextExtractorFromImage textExtractor;
    private Bitmap latestBitmap = null;
    private int OFFSET_TOP,OFFSET_BOTTOM;

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
    public int getDensity() {
        return density;
    }


    public ImageReader getImageReader() {
        return imageReader;
    }

    public Bitmap getLatestBitmap() {
        return latestBitmap;
    }
    public Surface getSurface() {
        return imageReader.getSurface();
    }

    @SuppressLint("WrongConstant")
    ImageTransmogrifier(MyInputMethodService svc) {
        this.svc = svc;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) svc.getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        while (width * height > (2 << 18)) {
            width = width >> 1;
            height = height >> 1;
        }
        this.width = width;
        this.height = height;
        this.density = displayMetrics.densityDpi;
        imageReader = ImageReader.newInstance(width, height,
                PixelFormat.RGBA_8888, 1);
        imageReader.setOnImageAvailableListener(this, svc.getHandler());
        Resources r = svc.getResources() ;
        OFFSET_BOTTOM = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                50f+18f+40f,
                r.getDisplayMetrics()
        );
        OFFSET_TOP = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                12f,
                r.getDisplayMetrics()
        );
//        textExtractor = new TextExtractorFromImage(svc.getApplicationContext(),ENGLISH_LANGUAGE);
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        final Image image=reader.acquireLatestImage();
        if (image!=null) {
            Image.Plane[] planes=image.getPlanes();
            ByteBuffer buffer=planes[0].getBuffer();
            int pixelStride=planes[0].getPixelStride();
            int rowStride=planes[0].getRowStride();
            int rowPadding=rowStride - pixelStride * width;
            int bitmapWidth=width + rowPadding / pixelStride;
            if (latestBitmap == null ||
                    latestBitmap.getWidth() != bitmapWidth ||
                    latestBitmap.getHeight() != height) {
                if (latestBitmap != null) {
                    latestBitmap.recycle();
                }
                latestBitmap=Bitmap.createBitmap(bitmapWidth,
                        height, Bitmap.Config.ARGB_8888);
            }
            latestBitmap.copyPixelsFromBuffer(buffer);
            if (image != null) {
                image.close();
            }
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            Bitmap cropped=Bitmap.createBitmap(latestBitmap, 0, OFFSET_TOP,
                    width, height-OFFSET_BOTTOM);
            cropped.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] newPng=baos.toByteArray();
            svc.updateBitmap(cropped);
//            svc.updateExtractedText(textExtractor.getFristLineOCRResult(cropped));
        }
    }

    public void close() {
        if(imageReader!=null){
            imageReader.close();
        }
    }
}