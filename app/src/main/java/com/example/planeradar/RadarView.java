package com.example.planeradar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.SurfaceView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RadarView extends SurfaceView {

    Paint textPaint;
    Paint centerPaint;
    double latPixelLength; // 0.0033015
    double latAPIRange = 0.41599281; // 46,3km = 0,41599281

    double screenLatDiff;
    double screenLonDiff;

    int frameLatency = 1000;


    public RadarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.GREEN);
        textPaint.setTextSize(16);

        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setARGB(200, 255,255,255);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        screenLatDiff = (getWidth() / 2.0);
        screenLonDiff = (getHeight() / 2.0);
        latPixelLength = latAPIRange / (getWidth() / 2.0);
//        System.out.println(getWidth() + "  " + getHeight());

        MainActivity mainActivity = (MainActivity) getContext();

        if (mainActivity.isPlanesServiceBound && mainActivity.boundPlanesService.radarData != null) {

            try {

                Thread.sleep(frameLatency);

                    JSONArray arr = mainActivity.boundPlanesService.radarData.getJSONArray("ac");

                    for (int i = 0; i < arr.length(); i++) {
                        float calculatedLat = transPlaneLatToScreenPixel(Double.parseDouble(arr.getJSONObject(i).get("lat").toString()), PlanesService.myLat);
                        float calculatedLon = transPlaneLonToScreenPixel(Double.parseDouble(arr.getJSONObject(i).get("lon").toString()),
                                PlanesService.myLat, PlanesService.myLon);

                        canvas.drawText(arr.getJSONObject(i).get("call").toString(), calculatedLon-34, calculatedLat-10, textPaint);
                        canvas.drawCircle(calculatedLon, calculatedLat, 5, textPaint);
                    }

            } catch (Exception ignored) {
                ;
            }
        }
        canvas.drawLine((float) screenLatDiff-8, (float) screenLonDiff,
                (float) screenLatDiff+8, (float) screenLonDiff,centerPaint);
        canvas.drawLine((float) screenLatDiff, (float) screenLonDiff-8,
                (float) screenLatDiff, (float) screenLonDiff+8,centerPaint);
        invalidate();
    }

    float transPlaneLonToScreenPixel(double planeLon, double myLat, double myLon) {
        double pixelLon = planeLon - myLon;
        double lonKiloMetersInOneDgr = 40075 * Math.cos(myLat) / 360 * 10;
        double lon100mLength = 1 / lonKiloMetersInOneDgr;
        double lonAPIRange = lon100mLength * 463;
        double lonPixelLength = lonAPIRange / (getHeight() / 2.0);
        return (float) ((pixelLon / lonPixelLength) + screenLonDiff);
    }

    float transPlaneLatToScreenPixel(double planeLat, double myLat) {
        double pixelLat = planeLat - myLat;
        return (float) (-(pixelLat / latPixelLength) + screenLatDiff);
    }
}
