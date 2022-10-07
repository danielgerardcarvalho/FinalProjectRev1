package com.daniel.finalprojectrev1;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import org.ojalgo.matrix.store.MatrixStore;
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
    // ones matrix
    private Primitive64Store ones;
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
    // MIN CONST
    private double MIN_CONST = Math.pow(10, -20);

    // operation flag
    private boolean operation_flag;

    // UI update variables
    private Activity activity;
    private TextView progress_update_indicator;

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
        this.ones = createMatrix(f, n, 1.0);
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
        // Progress indicator updater
        activity = null;
        progress_update_indicator = null;
    }

    /* Control methods */
    public void start() {
        // Starts the classes operations
        this.operation_flag = true;
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
    private void run() {
        // TODO: could initialise several H matrices and use one that achieves best results
        //  or average the results between them. Will this really help? obviouse computation and
        //  storage burden. Maybe different binarisations. Don't need multiple runs for that!

        // Starting the computation loop
        // TODO: add error thresholding
        // Re-initialising the H matrix
        initH();

        while (curr_iter_num < this.iter_limit && operation_flag) {
            // Update the matrices
            update();
            // Calculate the error
            calcError();
            // Progress display, giving error and current iteration number and total iterations
            if (curr_iter_num % 1 == 0){
                if (activity != null && progress_update_indicator != null) {
                    activity.runOnUiThread(() -> progress_update_indicator.setText(
                            activity.getText(R.string.progress_indicator_string) +
                                    String.format("\t%d / %d", curr_iter_num + 1, iter_limit)));
                }
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

    // TODO: improve - high
    private void update() {
        // NOTE: Current implementation
        // Kullback-Leibler Multiplicative Update Rule
        // Pre-allocating data
        Primitive64Store V1_estimate = Primitive64Store.FACTORY.make(this.W1.countRows(), this.H.countColumns());
        Primitive64Store numerator = Primitive64Store.FACTORY.make(this.W1.countColumns(), this.V1.countColumns());
        Primitive64Store denominator = Primitive64Store.FACTORY.make(this.W1.countColumns(), this.V1.countColumns());
        // Calculating the V1 estimate
        this.W1.multiply(this.H, V1_estimate);
//        V1_estimate = toPrimitive(V1_estimate.add(MIN_CONST));
        // Calculating the numerator
        (this.W1.transpose()).multiply(divide(this.V1, V1_estimate), numerator);
        // Calculating the denominator
        (this.W1.transpose()).multiply(this.ones, denominator);
//        denominator = toPrimitive(denominator.add(min_const));
        // TODO: Check if the min_const add to denominator is needed
        this.H = mul(this.H, (divide(numerator, denominator)));

//        // Kullback-Leibler Multiplicative Update Rule
//        Primitive64Store V1_estimate = Primitive64Store.FACTORY.make(this.f, this.n);
//        Primitive64Store numerator = Primitive64Store.FACTORY.make(this.k, this.n);
//        for (int k = 0; k < this.k; k++) {
//            this.W1.multiply(this.H, V1_estimate);
//            this.W1.transpose().multiply(divide(this.V1, V1_estimate), numerator);
//            double denominator_sum = sum(toPrimitive(this.W1.column(k)));
//            for (int n = 0; n < this.n; n++) {
//                double numerator_sum = numerator.get(k,n);
//                this.H.set(k, n, this.H.get(k,n) * numerator_sum/denominator_sum);
//            }
//        }
//        Log.v("NMFUpdate_out", String.format("Shapes: f=%d, e=%d, n=%d, k=%d" +
//                        "\n\tV1: (%d,%d)" +
//                        "\n\tW1: (%d,%d)" +
//                        "\n\tH: (%d,%d)" +
//                        "\n\tones: (%d,%d)" +
//                        "\n\tnumerator: (%d,%d)" +
//                        "\n\tdenominator: (%d,%d)" +
//                        "\n\tV1 est: (%d,%d)", this.f, this.e, this.n, this.k, this.V1.countRows(),
//                this.V1.countColumns(), this.W1.countRows(), this.W1.countColumns(),
//                this.H.countRows(), this.H.countColumns(), this.ones.countRows(),
//                this.ones.countColumns(), numerator.countRows(), numerator.countColumns(),
//                denominator.countRows(), denominator.countColumns(), V1_estimate.countRows(),
//                V1_estimate.countColumns()));
    }

    // TODO: improve - high
    private void calcError() {
        // NOTE: current implementation
        // TODO: see that this multiplication is matmul and not element wise
        Primitive64Store V1_est = toPrimitive(this.W1.multiply(this.H).add(this.MIN_CONST));
        Primitive64Store deviation_log = log(divide(this.V1, V1_est));
        Primitive64Store deviation = toPrimitive(mul(this.V1, deviation_log));
        error.add(sum(toPrimitive(deviation.add(V1_est.subtract(this.V1)))));

//        error.add(sum(toPrimitive(this.V1.multiply(toPrimitive(log(divide(this.V1, V1_est)))).subtract(this.V1).add(V1_est))));


//        // Clarity improvement
//        // Pre-allocating data
//        Primitive64Store V1_estimate = Primitive64Store.FACTORY.make(this.V1.countRows(), this.V1.countColumns());
//        Primitive64Store distribution = Primitive64Store.FACTORY.make(this.V1.countRows(), this.V1.countColumns());
//        Primitive64Store distribution_error = Primitive64Store.FACTORY.make(this.V1.countRows(), this.V1.countColumns());
//        // Calculating V1 estimate
//        this.W1.multiply(this.H, V1_estimate);
//        V1_estimate = toPrimitive(V1_estimate.add(Math.pow(10,-30)));
//        // Calculating distribution
//        distribution = mul(this.V1, log(divide(this.V1, V1_estimate)));
//        // Calculating distribution error
//        distribution_error = toPrimitive(this.V1.add(V1_estimate));
//        // Calculating error
//        this.error.add(sum(toPrimitive(distribution.subtract(distribution_error))));

//        Primitive64Store V1_estimate = Primitive64Store.FACTORY.make(this.V1.countRows(), this.V1.countColumns());
//        this.W1.multiply(this.H, V1_estimate);
//        V1_estimate = toPrimitive(V1_estimate.add(this.MIN_CONST));
//        Primitive64Store dif = toPrimitive(V1_estimate.subtract(this.V1));
//        Primitive64Store div_dif = log(divide(this.V1, V1_estimate), 10);
//        error.add(sum(toPrimitive(this.V1.multiply(div_dif).add(dif))));
    }

    /* Data initialisation and loading methods */
    private void initW() {
        // Creating the matrices
        this.W1 = createMatrix(this.f, this.k);
        this.W2 = createMatrix(this.e, this.k);
    }

    private void initH() {
        // Creating the matrices
        this.H = Primitive64Store.FACTORY.makeFilled(this.k, this.n, new Gamma(1.0, 1.0));
        this.H = toPrimitive(this.H.divide(max(abs(this.H))).add(MIN_CONST));
        Log.e("NMF TEST", String.format("H max: %f, min: %f", max(abs(this.H)), min(abs(this.H))));
//        this.H = (Primitive64Store) Primitive64Store.FACTORY.makeFilled(this.k, this.n, new Poisson(1.0));
    }

    // TODO: implement - low
    private void loadV1(double [][] data) {}

    public void loadV1(Primitive64Store data) {
        this.V1 = toPrimitive(data.divide(sum(data)));
//        this.V1 = data.copy();
//        this.V1 = toPrimitive(this.V1.divide(sum(this.V1)));
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

    /* Set methods */
    public void setProgressUpdateVars(Activity act, TextView textV) {
        this.activity = act;
        this.progress_update_indicator = textV;
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
    public int getCurr_iter_num() {
        return curr_iter_num;
    }
    public int getIter_limit() {
        return iter_limit;
    }

    /* Helpers */
    // Create a matrix - empty, shape only
    private Primitive64Store createMatrix(long rows, long columns) {
//        PhysicalStore.Factory<Double, Primitive64Store> storeFactory = Primitive64Store.FACTORY;
//        return storeFactory.make(rows, columns);
        return Primitive64Store.FACTORY.make(rows, columns);
    }
    // Create a matrix, filled with scalar value
    private Primitive64Store createMatrix(long rows, long columns, double value) {
//        PhysicalStore.Factory<Double, Primitive64Store> storeFactory = Primitive64Store.FACTORY;
//        Primitive64Store ret = storeFactory.make(rows, columns);
        Primitive64Store ret = createMatrix(rows, columns);
        for (int i = 0; i < ret.size(); i++){
            ret.set(i, value);
        }
        return ret;
    }
    // Create a matrix - filled, from 2D double
    private Primitive64Store createMatrix(double[][] matrix) {
        return Primitive64Store.FACTORY.rows(matrix);
    }
    // Convert MatrixStore to Primitive64Store
    private Primitive64Store toPrimitive(MatrixStore matrix) {
        Primitive64Store ret = Primitive64Store.FACTORY.make(matrix);
        for (int i = 0; i < matrix.size(); i++) {
            ret.set(i, matrix.get(i));
        }
        return ret;
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
    // Get largest value in the matrix
    private double max(Primitive64Store matrix) {
        double ret = matrix.get(0);
        for (long i = 0; i < matrix.size(); i++){
            ret = Math.max(matrix.get(i), ret);
        }
        return ret;
    }
    // Get smallest value in the matrix
    private double min(Primitive64Store matrix) {
        double ret = matrix.get(0);
        for (long i = 0; i < matrix.size(); i++){
            ret = Math.min(matrix.get(i), ret);
        }
        return ret;
    }
    // Get absolute values of matrix
    private Primitive64Store abs(Primitive64Store matrix) {
        Primitive64Store ret = Primitive64Store.FACTORY.make(matrix.countRows(), matrix.countColumns());
        for (long i = 0; i < matrix.size(); i++){
            ret.set(i, Math.abs(matrix.get(i)));
        }
        return ret;
    }
}

