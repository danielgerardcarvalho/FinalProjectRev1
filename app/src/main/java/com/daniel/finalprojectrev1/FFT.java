package com.daniel.finalprojectrev1;

import android.util.Log;

import java.util.ArrayList;

public class FFT {
    // Inputs
    private int size;
    private int stages;
    // Outputs
    private double [] solution_real;
    private double [] solution_imag;
    // Pre-Calculations
    private double [] data_imag_def;
    private double [] twiddle_real;
    private double [] twiddle_imag;

    public FFT(int size){
        // Initialising Variables
        this.size = size;
        this.stages = (int)(Math.log(size)/Math.log(2));
        // Checking for appropriate size of FFT
        if (size % 2 != 0) {
            Log.e("FFT Class", "FFT size is not a power of 2.");
            return;
        }

        // Calculate Twiddle Factor Tables
        // - Initialisation
        int twiddle_size = this.size/2;
        twiddle_real = new double[twiddle_size];
        twiddle_imag = new double[twiddle_size];
        // - Calculation
        double twiddle_temp;
        for (int i = 0; i < twiddle_size; i++) {
            twiddle_temp = -2 * Math.PI * i / this.size;
            twiddle_real[i] = Math.cos(twiddle_temp);
            twiddle_imag[i] = Math.sin(twiddle_temp);
        }

        // Creating initial imaginary data array
        data_imag_def = new double[this.size];
        for (int i = 0; i < this.size; i++){
            data_imag_def[i] = 0;
        }

        // Initialise primary arrays
        solution_real = new double[this.size];
        solution_imag = new double[this.size];
    }

    public ArrayList<double[]> fft(double [] data_real){

        // Bit reversal
        int reversal_out;
        int reversal_init;
        // Loop for each data point index
        for (int i = 0; i < this.size; i++){
            // Reversing the index
            reversal_init = i;
            reversal_out = 0;
            for (int j = 0; j < this.stages; j++){
                reversal_out = reversal_out * 2;
                if ((reversal_init & 0x01) == 0x01){
                    reversal_out = reversal_out ^ 0x01;
                }
                reversal_init = reversal_init / 2;
            }
            // Filling the real and imag arrays with new indices
            solution_real[i] = data_real[reversal_out];
            solution_real[reversal_out] = data_real[i];
        }
        // Retrieving the imaginary array
        solution_imag = data_imag_def.clone();

        // Performing the actual FFT
        int offset = 1;
        int index_size = 2;
        for (int i = 1; i <= this.stages; i++) {
            int twiddle_index = 0;
            for (int j = 0; j < offset; j++) {
                for (int k = j; k < this.size; k = k + index_size) {
                    // Multiplying the data with the twiddle factor
                    double temp_twiddle_real = solution_real[k + offset] * twiddle_real[twiddle_index]
                            - solution_imag[k + offset] * twiddle_imag[twiddle_index];
                    double temp_twiddle_imag = solution_real[k + offset] * twiddle_imag[twiddle_index]
                            + solution_imag[k + offset] * twiddle_real[twiddle_index];
                    // Real output
                    solution_real[k + offset] = solution_real[k] - temp_twiddle_real;
                    solution_real[k] = solution_real[k] + temp_twiddle_real;
                    // Imaginary output
                    solution_imag[k + offset] = solution_imag[k] - temp_twiddle_imag;
                    solution_imag[k] = solution_imag[k] + temp_twiddle_imag;
                }
                twiddle_index = twiddle_index + (int) Math.pow(2, this.stages - i);
            }
            offset = offset * 2;
            index_size = index_size * 2;
        }
        return getFFT();
    }

    /* Helpers and Gets */
    public ArrayList<double[]> getFFT(){
        ArrayList<double[]> ret = new ArrayList<double[]>();
        ret.add(solution_real);
        ret.add(solution_imag);
        return ret;
    }

    public double[] getFFTReal(){
        return solution_real;
    }

    public double[] getFFTImag(){
        return solution_imag;
    }

}