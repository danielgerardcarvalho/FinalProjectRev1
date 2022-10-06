package com.daniel.finalprojectrev1;

import android.view.View;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.ojalgo.matrix.store.Primitive64Store;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class AnnotatedTimeline {
    private ScatterChart plot;
    private String [] event_classes;

    public AnnotatedTimeline(ScatterChart plot) {
        this.plot = plot;
        this.event_classes = new String[]{"Dog", "Breaking glass", "Siren", "Gunshot", "Explosion"};
        constructPlot();
    }

    public void constructPlot() {
        // Configuring the plot
        // Setting the visibility of the plot
        plot.setVisibility(View.VISIBLE);
        // Configure Axes
        // y-axis
        YAxis yl = plot.getAxisLeft();
        plot.getAxisRight().setEnabled(false);
        plot.getAxisLeft().setEnabled(false);
        yl.setAxisMinimum(0f);
        // x-axis
        XAxis xl = plot.getXAxis();
        xl.setDrawGridLines(false);
        xl.setEnabled(false);

        // Disable content descriptions
        plot.getDescription().setEnabled(false);
        plot.setDrawGridBackground(false);
        plot.setTouchEnabled(false);
        plot.setMaxVisibleValueCount(0);

//        // below line is use to disable the description
//        // of our scatter plot.
//        plot.getDescription().setEnabled(false);
//
//        // below line is use to draw grid background
//        // and we are setting it to false.
//        plot.setDrawGridBackground(false);
//
//        // below line is use to set touch
//        // enable for our plot.
//        plot.setTouchEnabled(true);
//
//        // below line is use to set maximum
//        // highlight distance for our plot.
//        plot.setMaxHighlightDistance(50f);
//
//        // below line is use to set
//        // dragging for our plot.
//        plot.setDragEnabled(true);

        // below line is use to set scale
        // to our plot.
        plot.setScaleEnabled(true);
//        // below line is use to set maximum
//        // visible count to our plot.
//        plot.setMaxVisibleValueCount(200);
//
//        // below line is use to set
//        // pinch zoom to our plot.
//        plot.setPinchZoom(true);

        // below line we are getting
        // the legend of our plot.
        Legend l = plot.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setXOffset(5f);
    }

    public void updatePlot(Primitive64Store data) {
        // Creating a wrapper for data to be plotted
        ArrayList<ArrayList<Entry>> dataPoints = new ArrayList<>();

        // Populating the wrappers
        for (int i = 0; i < data.countRows(); i++) {
            // Temporary Entry array
            ArrayList<Entry> temp_entry = new ArrayList<>();
            for (int j = 0; j < data.countColumns(); j++) {
                // Converting data to appropriate form
                if (data.get(i, j) != 0) {
                    temp_entry.add(new Entry((int) (data.get(i, j) * j),  (float) (i + 0.5)));
                }
            }
            dataPoints.add(temp_entry);
        }
        // Create a dataset, and configure the properties
        ArrayList<ScatterDataSet> dataSets = new ArrayList<>();
        for (int i = 0; i < data.countRows(); i++) {
            dataSets.add(new ScatterDataSet(dataPoints.get(i), this.event_classes[i]));
            dataSets.get(i).setScatterShape(ScatterChart.ScatterShape.SQUARE);
            dataSets.get(i).setColor(ColorTemplate.COLORFUL_COLORS[i]);
            dataSets.get(i).setScatterShapeSize(35f);
            dataSets.get(i).setDrawValues(false);
        }

        // Populating the dataset with data
        ArrayList<IScatterDataSet> scatterDataSets = new ArrayList<>();
        for (int i = 0; i < dataSets.size(); i++) {
            scatterDataSets.add(dataSets.get(i));
        }
        ScatterData plot_data = new ScatterData(scatterDataSets);
        plot.setData(plot_data);
        plot.invalidate();
//        // in below line we are creating a new array list for our data set.
//        ArrayList<IScatterDataSet> dataSets = new ArrayList<>();
//
//        // in below line we are adding all
//        // data sets to above array list.
//        dataSets.add(set1); // add the data sets
//        dataSets.add(set2);
//        dataSets.add(set3);
//
//        ScatterData data1 = new ScatterData(dataSets);
//        plot.setData(data1);
//        plot.invalidate();
    }
}
