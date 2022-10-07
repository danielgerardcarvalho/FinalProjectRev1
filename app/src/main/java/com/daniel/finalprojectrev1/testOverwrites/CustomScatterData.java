package com.daniel.finalprojectrev1;

import com.github.mikephil.charting.data.BarLineScatterCandleBubbleData;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;

import java.util.List;

public class CustomScatterData extends BarLineScatterCandleBubbleData<IScatterDataSet> {

    public CustomScatterData() {
        super();
    }

    public CustomScatterData(List<IScatterDataSet> dataSets) {
        super(dataSets);
    }

    public CustomScatterData(CustomScatterDataSet... dataSets) {
        super(dataSets);
    }

    /**
     * Returns the maximum shape-size across all DataSets.
     *
     * @return
     */
    public float getGreatestShapeSize() {

        float max = 0f;

        for (IScatterDataSet set : mDataSets) {
            float size = set.getScatterShapeSize();

            if (size > max)
                max = size;
        }

        return max;
    }
}

