package com.daniel.finalprojectrev1;

import android.util.Log;

import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.store.Primitive64Store;
import org.ojalgo.random.Gamma;

import java.util.ArrayList;

public class NMF {

    // Import struct-like-class
    public static class NMF_Mini {
        double[][] W1;
        double[][] W2;
        double training_cost;

        NMF_Mini(){}
    }

    // non-negative data matrix
    private Primitive64Store V1;
    // annotated activity data matrix
    private Primitive64Store V2;
    // dictionary matrix 1 (primary dictionary matrix)
    private Primitive64Store W1;
    // dictionary matrix 2 (secondary dictionary matrix)
    private Primitive64Store W2;
    // coefficient matrix
    private Primitive64Store H;
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

    public NMF(int f, int n, int e, int k, int iter_limit){
        // Initialising the configuration settings
        this.f = f;
        this.n = n;
        this.e = e;
        this.k = k;
        this.iter_limit = iter_limit;
        // Constructing the V1 and V2 matrices
        this.V1 = createMatrix(f, n);
        this.V2 = createMatrix(e, n);
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
    public void start() {
        // Starts the classes operations
        this.operation_flag = true;
        // Re-initialise the H matrix
        initH();
        // Creating operation variables
        this.curr_iter_num = 0;
        // start nmf calculation
        run();
    }

    public void stop() {
        // Stops the classes operations
        this.operation_flag = false;
    }

    public void continueRun(){
        this.operation_flag = true;
        run();
    }

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
        // TODO: Check if this is matrix multiplication, check if countRows and countColumns does what is expected
        // Calculating V2, V2 = W2 x H
        this.W2.multiply(this.H, this.V2);
        Log.v("NMFRun", String.format("Size of A: (%d, %d), Size of B(%d, %d), Size of C (%d, %d)", this.W2.countRows(), this.W2.countColumns(), this.H.countRows(), this.H.countColumns(), this.V2.countRows(), this.V2.countColumns()));
    }

    // TODO: implement - high
    private void update() {
        // Kullback-Leibler Multiplicative Update Rule
        Primitive64Store V1_est = (Primitive64Store) this.W1.multiply(this.H);
        this.H = mul(this.H,
                divide( (Primitive64Store) (this.W1.transpose().multiply(divide(this.V1,V1_est))),
                        (Primitive64Store) (this.W1.transpose().multiply(
                                createMatrix(this.V1.countRows(),this.V1.countColumns(),1)))));
    }

    // TODO: implement - high
    private void calcError() {
        // TODO: see that this multiplication is matmul and not element wise
        Primitive64Store V1_est = (Primitive64Store) this.W1.multiply(this.H);
        error.add(sum((Primitive64Store) log(divide(V1, V1_est)).subtract(this.V1).add(V1_est)));
    }

    /* Data initialisation and loading methods */
    private void initW() {
        // Creating the matrices
        this.W1 = createMatrix(this.f, this.k);
        this.W2 = createMatrix(this.e, this.k);
    }

    private void initH() {
        // Creating the matrices
        this.H = (Primitive64Store) Primitive64Store.FACTORY.makeFilled(this.k, this.n, new Gamma(1.0, 1.0));
    }

    // TODO: implement - low
    private void loadV1(double [][] data) {}

    public void loadV1(Primitive64Store data) {
        this.V1 = data.copy();
    }

    public void loadW1(double [][] data) {
        this.W1 = createMatrix(data);
    }

    public void loadW2(double [][] data) {
        this.W2 = createMatrix(data);
    }

    public void loadTrainingError(double training_error){
        this.final_training_error = training_error;
    }

    /* Get methods */
    public Primitive64Store getV1(){
        return this.V1;
    }
    public Primitive64Store getV2(){
        return this.V2;
    }
    public Primitive64Store getW1(){
        return this.W1;
    }
    public Primitive64Store getW2(){
        return this.W2;
    }
    public Primitive64Store getH(){
        return this.H;
    }

    /* Helpers */

    // Create a matrix - empty, shape only
    private Primitive64Store createMatrix(long rows, long columns) {
        PhysicalStore.Factory<Double, Primitive64Store> storeFactory = Primitive64Store.FACTORY;
        return storeFactory.make(rows, columns);
    }
    // Create a matrix, filled with scalar value
    private Primitive64Store createMatrix(long rows, long columns, double value) {
        PhysicalStore.Factory<Double, Primitive64Store> storeFactory = Primitive64Store.FACTORY;
        Primitive64Store ret = storeFactory.make(rows, columns);
        for (int i = 0; i < ret.size(); i++){
            ret.set(i, value);
        }
        return ret;
    }
    // Create a matrix - filled, from 2D double
    private Primitive64Store createMatrix(double[][] matrix) {
        return Primitive64Store.FACTORY.rows(matrix);
    }

    // Element-wise logarithm of matrix, with base
    private Primitive64Store log(Primitive64Store matrix, int log_base) {
        for (long i = 0; i < matrix.size(); i++){
            matrix.set(i, Math.log(matrix.get(i))/Math.log(log_base));
        }
        return matrix;
    }
    // Element-wise logarithm of matrix, natural log
    private Primitive64Store log(Primitive64Store matrix) {
        for (long i = 0; i < matrix.size(); i++){
            matrix.set(i, Math.log(matrix.get(i)));
        }
        return matrix;
    }
    // Element-wise divide of matrix
    private Primitive64Store divide(Primitive64Store numerator, Primitive64Store denominator) {
        Primitive64Store ret = createMatrix(numerator.countRows(), numerator.countColumns());
        for (long i = 0; i < ret.size(); i++) {
            ret.set(i, numerator.get(i) / denominator.get(i));
        }
        return ret;
    }
    // Element-wise multiplication of matrix
    private Primitive64Store mul(Primitive64Store matrix1, Primitive64Store matrix2){
        // TODO: check that .size, countRows, countColumns does what you think.
        Primitive64Store ret = createMatrix(matrix1.countRows(), matrix1.countColumns());
        for (long i = 0; i < ret.size(); i++) {
            ret.set(i, matrix1.get(i) * matrix2.get(i));
        }
        return ret;
    }

    // Sum of matrix
    private double sum(Primitive64Store matrix) {
        // TODO: see that size does what you think
        double value = 0;
        for (int i = 0; i < matrix.size(); i++){
            value = value + matrix.get(i);
        }
        return value;
    }
}

