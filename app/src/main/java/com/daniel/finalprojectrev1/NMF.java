package com.daniel.finalprojectrev1;

import java.util.ArrayList;

import jeigen.DenseMatrix;
import jeigen.SparseMatrixLil;

public class NMF {
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
    // max number of optimisation iterations
    private int iter_limit;

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
//        // Initialising the cost array
        this.error = new ArrayList<>();
    }


    /* Computation methods */
    // TODO: implement - high
    private void run() {}

    // TODO: implement - high
    private void update() {}

    // TODO: implement - high
    private void calcError() {}


    /* Data initialisation and loading methods */
    private void initW() {
        // Creating the matrices
        this.W1 = SparseMatrixLil.sprand(this.f, this.k);
        this.W2 = SparseMatrixLil.sprand(this.e, this.k);
    }

    private void initH() {
        // Creating the matrices
        this.H = SparseMatrixLil.sprand(this.k, this.n);
    }

    // TODO: implement - low
    private void loadV1(double [][] data) {}

    // TODO: implement - high
    private void loadV1(DenseMatrix data) {}

    // TODO: implement - low
    private void loadV1(SparseMatrixLil data) {}

    // TODO:implement - high
    private void loadW1() {}

    // TODO: implement - high
    private void loadW2() {}


    /* Get methods */
    public SparseMatrixLil getV1(){
        return this.V1;
    }
    public SparseMatrixLil getV2(){
        return this.V2;
    }
    public SparseMatrixLil getW1(){
        return this.W1;
    }
    public SparseMatrixLil getW2(){
        return this.W2;
    }
    public SparseMatrixLil getH(){
        return this.H;
    }
}
