package com.daniel.finalprojectrev1;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;

import java.util.ArrayList;

public class AnnotatedTimeline {
    private int num_data_points;
    private double label_value_max;
    private double label_value_min;
    private int num_labels;

    private ScatterChart plot;
    private String [] event_classes;

    public class tempFormatter extends ValueFormatter {
        ArrayList<String> label_array;
        public tempFormatter(ArrayList<String> label_array){
            this.label_array = label_array;
        }

        @Override
        public String getAxisLabel(float value, AxisBase axis){
            Integer position = Math.round(value);
            if (value > 1 && value < 2) {
                position = 0;
            } else if (value > 2 && value < 3) {
                position = 1;
            } else if (value > 3 && value < 4) {
                position = 2;
            } else if (value > 4 && value <= 5) {
                position = 3;
            }
            if (position < label_array.size())
                return label_array.get(position);
            return "";
        }
    }

    public AnnotatedTimeline(ScatterChart plot) {
        this.plot = plot;
        this.event_classes = new String[]{"Dog", "Breaking glass", "Siren", "Gunshot", "Explosion"};

        // Setting number of time frame data points
        this.num_data_points = (int) (Math.floor((Globals.cap_time_interval *
                (Globals.proc_sample_rate * 1.0) - (int) Math.ceil(Globals.proc_window_time *
                Globals.proc_sample_rate)) / (int) Math.ceil(Globals.proc_hop_time *
                Globals.proc_sample_rate))+1);

        // Setting maximum x-axis label value
        this.label_value_max = (double) Globals.cap_time_interval;
        // Setting minimum x-axis label value
        this.label_value_min = 0.0;
        // Setting number of discrete visible labels
        int temp = 15;
        if (temp > Globals.cap_time_interval) {
            temp = Globals.cap_time_interval;
        }
        this.num_labels = temp;

        constructPlot();
    }

    public void constructPlot() {
        // Configuring the plot
        // Setting the visibility of the plot
        plot.setVisibility(View.VISIBLE);
//        plot.setDrawBorders(true);
        // Configure Axes

        /* Y - Axis */
        YAxis yl = plot.getAxisLeft();
        plot.getAxisRight().setEnabled(false);
        plot.getAxisLeft().setEnabled(false);
        yl.setAxisMinimum(0f);

//        /* X - Axis */
//        double num_data_points_per_discrete_label = (double)num_data_points/(double)num_labels;
//        // Creating label values
//        ArrayList<String> label_array = new ArrayList<>();
//        double current_label_value = label_value_min;
//        int label_value_array_count = 0;
//        for (int i = 0; i < num_data_points; i++){
//            if (i >= num_data_points_per_discrete_label * label_value_array_count){
//                if (num_labels > 1+label_value_array_count){
//                    current_label_value = ((double)label_value_max/(double)num_data_points) * i + label_value_min; //label_value_array.get(label_value_array_count++);
//                } else {
//                    current_label_value = label_value_max;
//                }
//            }
//            label_array.add(String.format("%.1f", current_label_value));
//        }
//        label_array.set(label_array.size()-1, String.format("%.1f", label_value_max));
//        // Configuring the x-axis
//        // get x-axis object
//        XAxis xl = plot.getXAxis();
//        // set position of x-axis
//        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
//        // enable x-axis
//        xl.setEnabled(true);
//        // set x-axis label values
//        xl.setValueFormatter(new IndexAxisValueFormatter(label_array));
//        // set the number of viewable labels
//        xl.setLabelCount(num_labels);
//        // Prevent last index from being clipped
//        xl.setAvoidFirstLastClipping(true);
////        xl.setGranularity(1.f);
////        xl.setAxisMaximum((float) num_data_points);
////        xl.setAxisMaximum((float) 0);
////        plot.setVisibleXRange(1f, 1f);
//        xl.setAxisMaximum(num_data_points);
//        xl.setAxisMinimum(0);
//        xl.setDrawGridLines(true);

        XAxis x_axis = plot.getXAxis();
        x_axis.setAvoidFirstLastClipping(true);
        x_axis.setPosition(XAxis.XAxisPosition.BOTTOM);
        x_axis.setEnabled(true);
        x_axis.setLabelCount(num_labels);
        x_axis.setAxisMaximum((float) label_value_max);
        x_axis.setAxisMinimum((float) label_value_min);
        x_axis.setDrawGridLines(true);
        x_axis.setDrawAxisLine(true);
        ArrayList<String> x_labels = new ArrayList<>();
        for (int i = 0; i < num_data_points; i++){
            x_labels.add(i + "s");
        }
        x_axis.setValueFormatter(new IndexAxisValueFormatter(x_labels));

        // Disable content descriptions
        plot.getDescription().setEnabled(false);
        plot.setDrawGridBackground(false);
        plot.setTouchEnabled(false);
        plot.setMaxVisibleValueCount((int)(1000000));


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

    public void updatePlot(Context context, double[][] data) {
        // Creating a wrapper for data to be plotted
        ArrayList<ArrayList<Entry>> dataPoints = new ArrayList<>();
        ArrayList<Drawable> icon_array = new ArrayList<>();
        icon_array.add(ContextCompat.getDrawable(context, R.drawable.annotated_timeline_icon_0));
        icon_array.add(ContextCompat.getDrawable(context, R.drawable.annotated_timeline_icon_1));
        icon_array.add(ContextCompat.getDrawable(context, R.drawable.annotated_timeline_icon_2));
        icon_array.add(ContextCompat.getDrawable(context, R.drawable.annotated_timeline_icon_3));
        icon_array.add(ContextCompat.getDrawable(context, R.drawable.annotated_timeline_icon_4));


        // Populating the wrappers
        for (int i = 0; i < data.length; i++) {
            // Temporary Entry array
            ArrayList<Entry> temp_entry = new ArrayList<>();
            for (int j = 0; j < data[i].length; j++) {
                // Converting data to appropriate form
                if (data[i][j] != 0) {
                    Entry temp = new Entry((float) (data[i][j] * num_labels/data[i].length * j), (float) (i + 0.5));
//                    Entry temp = new Entry((float) (data[i][j] * 15/data[i].length * j), (float) (i + 0.5));
                    temp.setIcon(icon_array.get(i));
                    temp_entry.add(temp);
//                    temp_entry.add(new Entry((int) (data.get(i, j) * j),  (float) (i + 0.5)));
                }
            }
            dataPoints.add(temp_entry);
        }
        // Create a dataset, and configure the properties
        ArrayList<ScatterDataSet> dataSets = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            // Loadingthe data into a dataset
            dataSets.add(new ScatterDataSet(dataPoints.get(i), this.event_classes[i]));

            dataSets.get(i).setDrawIcons(true);

//            dataSets.get(i).setScatterShape(ScatterChart.ScatterShape.SQUARE);
            switch (i){
                case 0:
                    dataSets.get(i).setColor(ContextCompat.getColor(context, R.color.annotation_0));
                    break;
                case 1:
                    dataSets.get(i).setColor(ContextCompat.getColor(context, R.color.annotation_1));
                    break;
                case 2:
                    dataSets.get(i).setColor(ContextCompat.getColor(context, R.color.annotation_2));
                    break;
                case 3:
                    dataSets.get(i).setColor(ContextCompat.getColor(context, R.color.annotation_3));
                    break;
                case 4:
                    dataSets.get(i).setColor(ContextCompat.getColor(context, R.color.annotation_4));
                    break;
            }
            dataSets.get(i).setScatterShapeSize(0.25f);
            dataSets.get(i).setDrawValues(false);
        }

        // Populating the dataset with data
        ArrayList<IScatterDataSet> scatterDataSets = new ArrayList<>(dataSets);
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
