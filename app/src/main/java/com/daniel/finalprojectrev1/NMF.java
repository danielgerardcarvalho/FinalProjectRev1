package com.daniel.finalprojectrev1;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

//import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.Primitive64Store;
import org.ojalgo.random.Gamma;

import java.util.ArrayList;
import java.util.Arrays;

public class NMF {

    // Load C++ library into program
    static {
        System.loadLibrary("finalprojectrev1");
    }

    // Import struct-like-class
    public static class NMF_Mini {
        double[][] W1;
        double[][] W2;
        double training_cost;

        NMF_Mini(){}
    }

    // non-negative data matrix
    private double[][] V1;
    // annotated activity data matrix
    private double[][] V2;
    // dictionary matrix 1 (primary dictionary matrix)
    private double[][] W1;
    // dictionary matrix 2 (secondary dictionary matrix)
    private double[][] W2;
    // coefficient matrix
    private double[][] H;
    // ones matrix
    private double[][] ones;
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

    // update implementation - true (matrix update), false (full C++ update)
    public boolean matrix_update_flag = true;

    // UI update variables
    private Activity activity;
    private TextView progress_update_indicator;

    // Multi-threading variables
    private double[][] mt_update_v1_est_val;
    private double[][] mt_update_denom_val;
    private double[][] mt_update_numer_val;

    /* Constructor */
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
    public void start() throws InterruptedException {
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

    public void continueRun() throws InterruptedException {
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
    private void run() throws InterruptedException {
        // Starting the computation loop
        // Re-initialising the H matrix
        initH();

        while (curr_iter_num < this.iter_limit && operation_flag) {
            // Calculate the error
            Thread temp = new Thread(()->calcError_mt());
            temp.start();
            // Update the matrices
            if (matrix_update_flag) {
                update();
            } else {
                this.H = h_update_jd(this.V1, this.W1, this.H);
            }
            // Calculate the error
            temp.join();
            calcError();
            // Progress display, giving error and current iteration number and total iterations
            if (curr_iter_num % 1 == 0){
                // Update UI
                if (activity != null && progress_update_indicator != null) {
                    activity.runOnUiThread(() -> progress_update_indicator.setText(
                            activity.getText(R.string.progress_indicator_string) +
                                    String.format("\t%d / %d", curr_iter_num + 1, iter_limit)));
                }
                // Log progress
                Log.v("ClassifierNMF", String.format("Iteration: %d / %d, error: %f",
                        curr_iter_num, this.iter_limit, this.error.get(this.error.size()-1)));
                // Check for interrupt exception
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e){
//                    this.clear();
                    throw new InterruptedException();
                }
            }
            curr_iter_num++;
        }
        // Saving results
        this.V2 = matmul_jd(this.W2, this.H);
        Log.v("NMFRun", String.format("Size of A: (%d, %d), Size of B(%d, %d), Size of C (%d, %d)", this.W2.length, this.W2[0].length, this.H.length, this.H[0].length, this.V2.length, this.V2[0].length));
    }
    private void update() {
        // Kullback-Leibler Multiplicative Update Rule
        // Pre-allocating data
        double[][] V1_estimate;
        double[][] denominator;
        mt_update_v1_est_val = new double[this.W1.length][this.H[0].length];
        mt_update_denom_val = new double[this.W1[0].length][this.V1[0].length];

        // Calculating the V1 estimate
        Thread v1_est_thread = new Thread( () -> update_v1_est_mt(this.W1, this.H));
        v1_est_thread.start();

        // Calculating the denominator
        Thread denom_thread = new Thread( () -> update_denominator_mt(this.W1, this.ones));
        denom_thread.start();

        // Wait for thread join
        try {
            v1_est_thread.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        V1_estimate = mt_update_v1_est_val;

        // Calculating the numerator
        double[][] numerator = matmul_jd(transpose(this.W1), divide(this.V1, V1_estimate));



        // Wait for threads to synchronise
        try {
            denom_thread.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        denominator = mt_update_denom_val;

        this.H = mul(this.H, (divide(numerator, denominator)));
    }
    private void update_numerator_mt(double[][] w1, double[][] v1, double[][] v1_est){
        mt_update_numer_val = matmul_jd(transpose(w1), divide(v1, v1_est));
    }
    private void update_denominator_mt(double[][] w1, double[][] ones){
        mt_update_denom_val = matmul_jd(transpose(w1), ones);
    }
    private void update_v1_est_mt(double[][] w1, double[][] h) {
        mt_update_v1_est_val = matmul_jd(w1, h);
    }

    private void calcError() {
        // NOTE: current implementation
        double[][] V1_est = add(matmul_jd(this.W1, this.H), this.MIN_CONST);
        double[][] deviation_log = log(divide(this.V1, V1_est));
        double[][] deviation = mul(this.V1, deviation_log);
        error.add(sum(add(deviation,sub(V1_est,this.V1))));
    }
    private void calcError_mt() {
        double[][] V1_est = add(matmul_jd(this.W1, this.H), this.MIN_CONST);
        double[][] deviation_log = log(divide(this.V1, V1_est));
        double[][] deviation = mul(this.V1, deviation_log);
        error.add(sum(add(deviation,sub(V1_est,this.V1))));
    }

    /* Data initialisation and loading methods */
    private void initW() {
        // Creating the matrices
        this.W1 = createMatrix(this.f, this.k);
        this.W2 = createMatrix(this.e, this.k);
    }

    private void initH() {
        // Creating the matrices
        this.H = Primitive64Store.FACTORY.makeFilled(this.k, this.n, new Gamma(1.0, 1.0)).toRawCopy2D();
        this.H = add(divide(this.H, max(abs(this.H))), MIN_CONST);
//        this.H = toPrimitive(this.H.divide(max(abs(this.H))).add(MIN_CONST));
        Log.e("NMF TEST", String.format("H max: %f, min: %f", max(abs(this.H)), min(abs(this.H))));
//        this.H = (Primitive64Store) Primitive64Store.FACTORY.makeFilled(this.k, this.n, new Poisson(1.0));
    }

    public void loadV1(double[][] data) {
        this.V1 = divide(data, sum(data));
    }

    public void loadW1(double [][] data) {
        this.W1 = data.clone();
    }

    public void loadW2(double [][] data) {
        this.W2 = data.clone();
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
    public double[][] getV1(){
        return this.V1;
    }
    public double[][] getV2(){
        return this.V2;
    }
    public double[][] getW1(){
        return this.W1;
    }
    public double[][] getW2(){
        return this.W2;
    }
    public double[][] getH(){
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
    private double[][] createMatrix(int rows, int columns) {
        return new double[rows][columns];
    }
    // Create a matrix, filled with scalar value
    private double[][] createMatrix(int rows, int columns, double value) {
        double[][] ret = new double[rows][columns];
        for (double[] doubles : ret) {
            Arrays.fill(doubles, value);
        }
        return ret;
    }

    // Element-wise logarithm of matrix, with base
    private double[][] log(double[][] matrix, int log_base) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++){
                matrix[i][j] = Math.log(matrix[i][j]) / Math.log(log_base);
            }
        }
        return matrix;
    }
    // Element-wise logarithm of matrix, natural log
    private double[][] log(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++){
                matrix[i][j] = Math.log(matrix[i][j]);
            }
        }
        return matrix;
    }
    // Element-wise divide of matrix and scalar
    private double[][] divide(double[][] matrix, double scalar){
        double[][] ret = new double[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++){
            for (int j = 0; j < matrix[i].length; j++){
                ret[i][j] = matrix[i][j] / scalar;
            }
        }
        return ret;
    }
    // Element-wise divide of matrix
    private double[][] divide(double[][] numerator, double[][] denominator) {
        double[][] ret = new double[numerator.length][numerator[0].length];
        for (int i = 0; i < ret.length; i++) {
            for (int j = 0; j < ret[0].length; j++) {
                ret[i][j] = numerator[i][j] / denominator[i][j];
            }
        }
        return ret;
    }
    // Element-wise multiplication of matrix
    private double[][] mul(double[][] matrix1, double[][] matrix2){
        // TODO: check that .size, countRows, countColumns does what you think.
        double[][] ret = new double[matrix1.length][matrix1[0].length];
        for (int i = 0; i < ret.length; i++) {
            for (int j = 0; j < ret[i].length; j++) {
                ret[i][j] = matrix1[i][j] * matrix2[i][j];
            }
        }
        return ret;
    }
    // Element-wise addition of matrix and scalar
    private double[][] add(double[][] matrix, double scalar){
        double [][] ret = new double [matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++){
            for (int j = 0; j < matrix[i].length; j++){
                ret[i][j] = ret[i][j] + scalar;
            }
        }
        return ret;
    }
    // Element-wise addition of matrix and matrix
    private double[][] add(double[][] matrix1, double[][] matrix2){
        double[][] ret = new double[matrix1.length][matrix1[0].length];
        for (int i = 0; i < matrix1.length; i++){
            for (int j = 0; j < matrix1[i].length; j++){
                ret[i][j] = matrix1[i][j] + matrix2[i][j];
            }
        }
        return ret;
    }
    // Element-wise subtraction of matrix and matrix
    private double[][] sub(double[][] matrix1, double[][] matrix2){
        double[][] ret = new double[matrix1.length][matrix1[0].length];
        for (int i = 0; i < matrix1.length; i++){
            for (int j = 0; j < matrix1[i].length; j++){
                ret[i][j] = matrix1[i][j] - matrix2[i][j];
            }
        }
        return ret;
    }

    // Sum of matrix
    private double sum(double[][] matrix) {
        double value = 0;
        for (int i = 0; i < matrix.length; i++){
            for (int j = 0; j < matrix[i].length; j++){
                value = value + matrix[i][j];
            }
        }
        return value;
    }
    // Get largest value in the matrix
    private double max(double[][] matrix) {
        double ret = matrix[0][0];
        for (int i = 0; i < matrix.length; i++){
            for (int j = 0; j < matrix[i].length; j++){
                ret = Math.max(matrix[i][j], ret);
            }
        }
        return ret;
    }
    // Get smallest value in the matrix
    private double min(double[][] matrix) {
        double ret = matrix[0][0];
        for (int i = 0; i < matrix.length; i++){
            for (int j = 0; j < matrix[i].length; j++) {
                ret = Math.min(matrix[i][j], ret);
            }
        }
        return ret;
    }
    // Get absolute values of matrix
    private double[][] abs(double[][] matrix) {
        double[][] ret = new double[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++){
            for (int j = 0; j < matrix[i].length; j++) {
                ret[i][j] = Math.abs(matrix[i][j]);
            }
        }
        return ret;
    }
    // Transpose matrix
    public double[][] transpose(double[][] matrix){
        double[][] ret = createMatrix(matrix[0].length, matrix.length);
        for (int i = 0; i < matrix[0].length; i++){
            for (int j = 0; j < matrix.length; j++){
                ret[i][j] = matrix[j][i];
            }
        }
        return ret;
    }

    // C++ FUNCTIONS - MATMUL
    public double[][] matmul_j(double[][] matrix1, double[][] matrix2){
        // Calculate matmul
        double[] flat_matrix1 = new double[matrix1.length * matrix1[0].length];
        double[] flat_matrix2 = new double[matrix2.length * matrix2[0].length];
        int count = 0;
        for (int i = 0; i < matrix1.length; i++){
            for (int j = 0; j < matrix1[i].length; j++){
                flat_matrix1[count++] = matrix1[i][j];
            }
        }
        count = 0;
        for (int i = 0; i < matrix2.length; i++){
            for (int j = 0; j < matrix2[i].length; j++){
                flat_matrix2[count++] = matrix2[i][j];
            }
        }
        double [] temp_mat = matmul(flat_matrix1, flat_matrix2, matrix1.length,
                matrix1[0].length, matrix2.length, matrix2[0].length);
        // Convert to Primitive store
        double[][] ret = new double[matrix1.length][matrix2[0].length];
        for (int i = 0; i < matrix1.length; i++){
            for (int j = 0; j < matrix2[0].length; j++){
                ret[i][j] = temp_mat[i*matrix2[0].length + j];
            }
        }
        return ret;
    }
    public native double[] matmul(double[] mat1, double[] mat2, int mat1_rows, int mat1_cols,
                                  int mat2_rows, int mat2_cols);

    public double[][] matmul_jd(double[][] matrix1, double[][] matrix2){
        double [][] ret = matmulDirect(matrix1, matrix2, matrix1.length,
                matrix1[0].length, matrix2.length, matrix2[0].length);
        return ret;
    }
    public native double[][] matmulDirect(double[][] mat1, double[][] mat2, int mat1_rows, int mat1_cols,
                                  int mat2_rows, int mat2_cols);

    // H UPDATE
    public double[][] h_update_jd(double[][] v1_mat, double[][] w1_mat, double[][] h_mat){
        double [][] ret = hUpdateDirect(v1_mat, w1_mat, h_mat, v1_mat.length, v1_mat[0].length,
                w1_mat.length, w1_mat[0].length, h_mat.length, h_mat[0].length);
        return ret;
    }
    public native double[][] hUpdateDirect(double[][] v1_mat, double[][] w1_mat, double[][] h_mat,
                                           int v1_rows, int v1_cols, int w1_rows, int w1_cols,
                                           int h_rows, int h_cols);
}

