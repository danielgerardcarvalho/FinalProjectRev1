package com.daniel.finalprojectrev1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import org.ojalgo.matrix.store.Primitive64Store;

public class BitMapView extends View {
    public Paint paint = new Paint();
    public Bitmap bmp;
    private int [] shape;

    public BitMapView(Context context, double[][] data, int source_width) {
        super(context);
        constructBitMap(data, source_width);
    }

    private void constructBitMap(double[][] data, int source_width){
        if (data != null) {
            paint.setStrokeWidth(1);
            // TODO: ojalgo CHECK see that countRows and countColumns does what you think
            int rows = (int) data.length;
            int cols = (int) data[0].length;

            int[] arrayCol = new int[rows * cols];
            int counter = 0;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    int value;
                    int color;
                    value = 255 - (int) (data[i][j] * 255);
                    color = (255 << 24 | value << 16 | value << 8 | value); // ARGB_8888
//                    color = ((value & 0x01f) | (value & 0x03f) << 5 | (value & 0x01f) << 11); // RGB_565
                    arrayCol[counter] = color;
                    counter++;
                }
            }
            bmp = Bitmap.createBitmap(arrayCol, cols, rows, Bitmap.Config.ARGB_8888);
//            bmp = Bitmap.createBitmap(arrayCol, cols, rows, Bitmap.Config.RGB_565);

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