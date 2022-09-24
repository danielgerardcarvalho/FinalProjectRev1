package com.daniel.finalprojectrev1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.ejml.simple.SimpleMatrix;

import java.util.Random;

public class SpectrogramView extends View {
    public Paint paint = new Paint();
    public Bitmap bmp;
    private int [] shape;

//    public SpectrogramView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//
////        def();
//    }

//    public SpectrogramView(Context context, AttributeSet attrs, int defStyle) {
//        super(context, attrs, defStyle);
//
//        def();
//    }
//
//    public SpectrogramView(Context context) {
//        super(context);
//        Log.d("pls", "This should work");
////        def();
//    }

    public SpectrogramView(Context context, double [][] data) {
        super(context);
//        SimpleMatrix sm = new SimpleMatrix(data);
//        shape[0] = sm.numCols();
//        shape[1] = sm.numRows();
        constructBitMap(data);
    }

    private void constructBitMap(double [][] data){
        if (data != null) {
            paint.setStrokeWidth(1);
            int width = data.length;
            int height = data[0].length;

            int[] arrayCol = new int[width * height];
            int counter = 0;
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int value;
                    int color;
                    value = 255 - (int) (data[j][i] * 255);
                    color = (value << 16 | value << 8 | value | 255 << 24);
                    arrayCol[counter] = color;
                    counter++;
                }
            }
            bmp = Bitmap.createBitmap(arrayCol, width, height, Bitmap.Config.ARGB_8888);

        } else {
            System.err.println("Data Corrupt");
        }
        Log.d("done", "done");
    }

    private double[][] createData(){
        // Create some data
        int num_freq_bins = 128;
        int num_time_frames = 500;
        Random random = new Random();
        double[][] spec_matrix = new double[num_freq_bins][num_time_frames];
        for (int i = 0; i < num_freq_bins; i++){
            for (int j = 0; j < num_time_frames; j++){
                spec_matrix[i][j] = random.nextDouble();
            }
        }
        // normalise the spec_matrix
        double temp_max = 0;
        for (int i = 0; i < num_freq_bins; i++){
            for (int j = 0; j < num_time_frames; j++){
                temp_max = Math.max(spec_matrix[i][j], temp_max);
            }
        }
        for (int i = 0; i < num_freq_bins; i++){
            for (int j = 0; j < num_time_frames; j++){
                spec_matrix[i][j] = spec_matrix[i][j] / temp_max;
            }
        }
        SimpleMatrix temp = new SimpleMatrix(spec_matrix);
        temp.transpose().convertType(double);
        return transpose(spec_matrix);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(bmp, 0, 0, paint);
    }
}