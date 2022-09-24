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

import jeigen.DenseMatrix;
import jeigen.Shortcuts.*;

import java.util.Random;

public class SpectrogramView extends View {
    public Paint paint = new Paint();
    public Bitmap bmp;
    private int [] shape;

    public SpectrogramView(Context context, DenseMatrix data, int source_width) {
        super(context);
        constructBitMap(data, source_width);
    }

    private void constructBitMap(DenseMatrix data, int source_width){
        if (data != null) {
            paint.setStrokeWidth(1);
            int rows = data.rows;
            int cols = data.cols;

            int[] arrayCol = new int[rows * cols];
            int counter = 0;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    int value;
                    int color;
                    value = 255 - (int) (data.get(i,j) * 255);
                    color = (value << 16 | value << 8 | value | 255 << 24);
                    arrayCol[counter] = color;
                    counter++;
                }
            }
            bmp = Bitmap.createBitmap(arrayCol, cols, rows, Bitmap.Config.ARGB_8888);

        } else {
            System.err.println("Data Corrupt");
        }
        bmp = Bitmap.createScaledBitmap(bmp, source_width, (int) (source_width*0.5), false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(bmp, 0, 0, paint);
    }
}