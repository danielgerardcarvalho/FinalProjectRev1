package com.daniel.finalprojectrev1;

import android.util.Log;

import java.util.ArrayList;

import jeigen.DenseMatrix;
import jeigen.SparseMatrixCCS;
import jeigen.SparseMatrixLil;

public class NMF {

    // Import struct-like-class
    // TODO: ojalgo
    public static class NMF_Mini {
        double[][] W1;
        double[][] W2;
        double training_cost;

        NMF_Mini(){}
    }

    // non-negative data matrix
    private SparseMatrixLil V1;
    // annotated activity data matrix
    private SparseMatrixLil V2;
    // dictionary matrix 1 (primary dictionary matrix)
    private SparseMatrixLil W1;
    // dictionary matrix 2 (secondary dictionary matrix)
    private SparseMatrixLil W2;
    // coefficient matrix
    private SparseMatrixLil H;
    // frequency bins
    private int f;
    // number of frames/windows
    private int n;
    // number of event classes
    private int e;
    // number of internal components
    private int k;

    // optimisation error array
    private ArrayList<Double> error;
    // final training error
    private double final_training_error;
    // max number of optimisation iterations
    private int iter_limit;
    // current iteration number
    private int curr_iter_num;

    // operation flag
    private boolean operation_flag;

    // TODO: ojalgo
    public NMF(int f, int n, int e, int k, int iter_limit){
        // Initialising the configuration settings
        this.f = f;
        this.n = n;
        this.e = e;
        this.k = k;
        this.iter_limit = iter_limit;
        // Constructing the V1 and V2 matrices
        this.V1 = new SparseMatrixLil(f, n);
        this.V2 = new SparseMatrixLil(e, n);
        // Constructing the dictionary matrices W1 and W2
        initW();
        // Constructing the coefficient matrix H
        initH();
        // Initialising the cost array
        this.error = new ArrayList<>();
        // Setting the operation flag to false
        this.operation_flag = false;
        // Clearing the current iteration number
        this.curr_iter_num = 0;
    }

    /* Control methods */
    // TODO: ojalgo
    public void start() {
        // TODO: nmf: start the classifier nmf calc
        // Starts the classes operations
        this.operation_flag = true;
        // Re-initialise the H matrix
        initH();
        // Creating operation variables
        this.curr_iter_num = 0;
        // start nmf calculation
        run();
    }

    // TODO: ojalgo
    public void stop() {
        // TODO: nmf: stop the classifier nmf calc
        // Stops the classes operations
        this.operation_flag = false;
    }

    // TODO: ojalgo
    public void continueRun(){
        this.operation_flag = true;
        run();
    }

    // TODO: ojalgo
    public void clear() {
        // Clearing all major class variables
        this.V1 = null;
        this.V2 = null;
        this.W1 = null;
        this.W2 = null;
        this.H = null;
        this.error = null;
        this.operation_flag = false;
    }

    /* Primary methods */
    // TODO: implement - high
    private void run() {
        // TODO: could initialise several H matrices and use one that achieves best results
        //  or average the results between them. Will this really help? obviouse computation and
        //  storage burden. Maybe different binarisations. Don't need multiple runs for that!

        double min_const = Math.pow(1.0 * 10, -30);
        // Starting the computation loop
        // TODO: add error thresholding
        while (curr_iter_num < this.iter_limit && operation_flag) {
            // Update the matrices
            update();
            // Calculate the error
            calcError();
            // Progress display, giving error and current iteration number and total iterations
            if (curr_iter_num % 10 == 0){
                Log.v("ClassifierNMF", String.format("Iteration: %d / %d, error: %f",
                        curr_iter_num, this.iter_limit, this.error.get(this.error.size()-1)));
            }
            curr_iter_num++;
        }
        // Saving result
        this.V2 = this.W2.mmul(this.H);
    }

    // TODO: implement - high
    // TODO: ojalgo
    private void update() {
        // Kullback-Leibler Multiplicative Update Rule
        SparseMatrixCCS temp1 = this.W1.toDense().mmul(this.H.toDense());
        SparseMatrixLil temp = toSparseMatrixLil(this.W1.toDense().mmul(this.H.toDense()));
        this.H = toSparseMatrixLil(this.H.mul((this.W1.t().mmul(toSparseMatrixLil(this.V1.div(temp.toDense())))).div(this.W1.t().mmul(toSparseMatrixLil(DenseMatrix.ones(this.V1.rows, this.V1.cols))).toDense())));
    }

    // TODO: implement - high
    // TODO: ojalgo
    private void calcError() {
        SparseMatrixLil temp = this.W1.mmul(this.H);
        error.add(this.V1.mul(this.V1.div(temp.toDense()).mlog()).sumOverRows().sumOverCols().getValues()[0]);
    }


    /* Data initialisation and loading methods */
    // TODO: ojalgo
    private void initW() {
        // Creating the matrices
        this.W1 = SparseMatrixLil.sprand(this.f, this.k);
        this.W2 = SparseMatrixLil.sprand(this.e, this.k);
    }

    // TODO: ojalgo
    private void initH() {
        // Creating the matrices
        this.H = SparseMatrixLil.sprand(this.k, this.n);
    }

    // TODO: implement - low
    // TODO: ojalgo
    private void loadV1(double [][] data) {}

    // TODO: implement - high
    // TODO: ojalgo
    public void loadV1(DenseMatrix data) {
        this.V1 = toSparseMatrixLil(data);
    }

    // TODO: implement - low
    // TODO: ojalgo
    private void loadV1(SparseMatrixLil data) {}

    // TODO: ojalgo
    public void loadW1(double [][] data) {
        this.W1 = toSparseMatrixLil(new DenseMatrix(data));
    }

    // TODO: ojalgo
    public void loadW2(double [][] data) {
        this.W2 = toSparseMatrixLil(new DenseMatrix(data));
    }

    // TODO: ojalgo
    public void loadTrainingError(double training_error){
        this.final_training_error = training_error;
    }

    /* Get methods */
    // TODO: ojalgo
    public SparseMatrixLil getV1(){
        return this.V1;
    }
    // TODO: ojalgo
    public SparseMatrixLil getV2(){
        return this.V2;
    }
    // TODO: ojalgo
    public SparseMatrixLil getW1(){
        return this.W1;
    }
    // TODO: ojalgo
    public SparseMatrixLil getW2(){
        return this.W2;
    }
    // TODO: ojalgo
    public SparseMatrixLil getH(){
        return this.H;
    }

    /* Helpers */
    // TODO: ojalgo
    public SparseMatrixLil toSparseMatrixLil(DenseMatrix temp) {
        SparseMatrixLil result = new SparseMatrixLil(temp.rows, temp.cols);
        int notZero = 0;
        int count = temp.rows * temp.cols;

        int c;
        for(c = 0; c < count; ++c) {
            if (temp.getValues()[c] != 0.0D) {
                ++notZero;
            }
        }

        result.reserve(notZero);

        for(c = 0; c < temp.cols; ++c) {
            for(int r = 0; r < temp.rows; ++r) {
                double value = temp.getValues()[temp.rows * c + r];
                if (value != 0.0D) {
                    result.append(r, c, value);
                }
            }
        }

        return result;
    }

}

