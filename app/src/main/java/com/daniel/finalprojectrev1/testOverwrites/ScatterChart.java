package com.daniel.finalprojectrev1;

import android.content.Context;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.interfaces.dataprovider.ScatterDataProvider;
import com.github.mikephil.charting.renderer.ScatterChartRenderer;

public class ScatterChart extends BarLineChartBase<ScatterData> implements ScatterDataProvider {

    public ScatterChart(Context context) {
        super(context);
    }

    public ScatterChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScatterChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void init() {
        super.init();

        mRenderer = new ScatterChartRenderer(this, mAnimator, mViewPortHandler);

        getXAxis().setSpaceMin(0.5f);
        getXAxis().setSpaceMax(0.5f);
    }

    @Override
    public ScatterData getScatterData() {
        return mData;
    }

    /**
     * Predefined ScatterShapes that allow the specification of a shape a ScatterDataSet should be drawn with.
     * If a ScatterShape is specified for a ScatterDataSet, the required renderer is set.
     */
    public enum ScatterShape {

        CUSTOM("CUSTOM"),
        SQUARE("SQUARE"),
        CIRCLE("CIRCLE"),
        TRIANGLE("TRIANGLE"),
        CROSS("CROSS"),
        X("X"),
        CHEVRON_UP("CHEVRON_UP"),
        CHEVRON_DOWN("CHEVRON_DOWN");

        private final String shapeIdentifier;

        ScatterShape(final String shapeIdentifier) {
            this.shapeIdentifier = shapeIdentifier;
        }

        @Override
        public String toString() {
            return shapeIdentifier;
        }

        public static ScatterChart.ScatterShape[] getAllDefaultShapes() {
            return new ScatterChart.ScatterShape[]{CUSTOM, SQUARE, CIRCLE, TRIANGLE, CROSS, X, CHEVRON_UP, CHEVRON_DOWN};
        }
    }
}

