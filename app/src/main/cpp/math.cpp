//
// Created by Daniel on 10/11/2022.
//
#include <jni.h>
#include <vector>
#include "math.h"

jdoubleArray math::matmul(JNIEnv *env, jdoubleArray m1, jdoubleArray m2, jint m1_rows, jint m1_cols, jint m2_rows, jint m2_cols){
    // Create the local arrays
    jdouble* loc_a1 = env->GetDoubleArrayElements(m1, 0);
    jdouble* loc_a2 = env->GetDoubleArrayElements(m2, 0);
    // Filling matrices
    double *loc_m1[m1_rows];
    for (int i = 0; i < m1_rows; i++){
        loc_m1[i] = new double[m1_cols];
        for (int j = 0; j < m1_cols; j++){
            loc_m1[i][j] = loc_a1[i*m1_cols+j];
        }
    }
    env->ReleaseDoubleArrayElements(m1, loc_a1, JNI_ABORT);
    double *loc_m2[m2_rows];
    for (int i = 0; i < m2_rows; i++){
        loc_m2[i] = new double[m2_cols];
        for (int j = 0; j < m2_cols; j++){
            loc_m2[i][j] = loc_a2[i*m2_cols+j];
        }
    }
    env->ReleaseDoubleArrayElements(m2, loc_a2, JNI_ABORT);

    // Performing matmul
    double* temp_ret = new double[(int) (m1_rows*m2_cols)];
    for (int i = 0; i < m1_rows; i++){
        for (int j = 0; j < m2_cols; j++){
            double temp = 0.0;
            for (int k = 0; k < m2_rows; k++){
                temp = temp + (loc_m1[i][k] * loc_m2[k][j]);
            }
            temp_ret[i*m2_cols+j] = temp;
        }
    }

    // Release local matrices
    for (int i = 0; i < m1_rows; i++) {
        delete[] loc_m1[i];
    }
    for (int i = 0; i < m2_rows; i++) {
        delete[] loc_m2[i];
    }
    // Performing matmul
    jdoubleArray ret = env->NewDoubleArray(m1_rows * m2_cols);
    env->SetDoubleArrayRegion(ret, 0, m1_rows * m2_cols, (const jdouble*) temp_ret);
    // Release matmul matrix
    delete[] temp_ret;

    return ret;
}

jdoubleArray math::transpose(JNIEnv *env, jdoubleArray mat, jint mat_rows, jint mat_cols){
    jdouble *loc_array = env->GetDoubleArrayElements(mat, 0);

    int array_size = mat_rows * mat_cols;
    double *temp_ret = new double[array_size];
    int temp_ret_count = 0;
    for (int i = 0; i < mat_rows; ++i) {
        for (int j = 0; j < mat_cols; ++j) {
            temp_ret[temp_ret_count++] = loc_array[i + j*mat_rows];
        }
    }
    env->ReleaseDoubleArrayElements(mat, loc_array, JNI_ABORT);

    jdoubleArray ret = env->NewDoubleArray(array_size);
    env->SetDoubleArrayRegion(ret, 0, array_size, (const jdouble*) temp_ret);
    delete[] temp_ret;
    return ret;
}

jdoubleArray math::add(JNIEnv *env, jdoubleArray m1, jdoubleArray m2, jint m_rows, jint m_cols){
    // Create the local arrays
    jdouble* loc_a1 = env->GetDoubleArrayElements(m1, 0);
    jdouble* loc_a2 = env->GetDoubleArrayElements(m2, 0);
    int matrix_size = m_rows * m_cols;
    // Performing addition
    double* temp_ret = new double[matrix_size];
    for (int i = 0; i < matrix_size; i++){
            temp_ret[i] = loc_a1[i] * loc_a2[i];
    }
    // Release data
    env->ReleaseDoubleArrayElements(m1, loc_a1, JNI_ABORT);
    env->ReleaseDoubleArrayElements(m2, loc_a2, JNI_ABORT);

    jdoubleArray ret = env->NewDoubleArray(matrix_size);
    env->SetDoubleArrayRegion(ret, 0, matrix_size, (const jdouble*) temp_ret);
    delete[] temp_ret;
    return ret;
}

jobjectArray math::matmulDirect(JNIEnv *env, jobjectArray m1, jobjectArray m2, jint m1_rows, jint m1_cols, jint m2_rows, jint m2_cols){
    // Converting jdoubleArray to matrix vectors
//    std::vector<std::vector<double>> loc_m1(m1_rows);
    double* loc_m1[m1_rows];
    for (int i = 0; i < m1_rows; i++) {
        // Setting size of each row
        loc_m1[i] = new double[m1_cols];
        jdoubleArray temp_matrix = static_cast<jdoubleArray>(env->GetObjectArrayElement(m1, i));
        jdouble *temp_data = env->GetDoubleArrayElements(temp_matrix, 0);
        for (int j = 0; j < m1_cols; j++){
            loc_m1[i][j] = temp_data[j];
        }
        env->ReleaseDoubleArrayElements(temp_matrix, temp_data, 0);
    }
    double* loc_m2[m2_rows];
    for (int i = 0; i < m2_rows; i++) {
        // Setting size of each row
        loc_m2[i] = new double[m2_cols];
        jdoubleArray temp_matrix = static_cast<jdoubleArray>(env->GetObjectArrayElement(m2, i));
        jdouble *temp_data = env->GetDoubleArrayElements(temp_matrix, 0);
        for (int j = 0; j < m2_cols; j++){
            loc_m2[i][j] = temp_data[j];
        }
        env->ReleaseDoubleArrayElements(temp_matrix, temp_data, 0);
    }

    // Performing matmul
    double* temp_ret[m1_rows];
    for (int i = 0; i < m1_rows; i++){
        temp_ret[i] = new double[m2_cols];
        for (int j = 0; j < m2_cols; j++){
            double temp = 0.0;
            for (int k = 0; k < m2_rows; k++){
                temp = temp + (loc_m1[i][k] * loc_m2[k][j]);
            }
            temp_ret[i][j] = temp;
        }
    }

    // Releasing the local matrices
    for (int i = 0; i < m1_rows; i++){
        delete [] loc_m1[i];
    }
    for (int i = 0; i < m2_rows; i++){
        delete [] loc_m2[i];
    }

    // Constructing jobjectArray structure
    jdoubleArray ret_array = env->NewDoubleArray(m2_cols);
    jobjectArray ret_matrix = env->NewObjectArray(m1_rows, env->GetObjectClass(ret_array), nullptr);
    // Filling the jobjectArray
    for (int i = 0; i < m1_rows; i++) {
        env->SetDoubleArrayRegion(ret_array, 0, m2_cols, temp_ret[i]);
        env->SetObjectArrayElement(ret_matrix, i, ret_array);
        // Allocate a new spot in memory for the next array
        if(i != m1_rows-1) {
            ret_array = env->NewDoubleArray(m2_cols);
        }
    }
    // Release matmul matrix, vectors clear themselves
    for (int i = 0; i < m1_rows; i++){
        delete[] temp_ret[i];
    }
    return ret_matrix;
}

jobjectArray math::hUpdateDirect(JNIEnv *env, jobjectArray v1, jobjectArray w1, jobjectArray h,
                                 jint v1_rows, jint v1_cols, jint w1_rows, jint w1_cols,
                                 jint h_rows, jint h_cols) {
    // Converting jdoubleArray to matrix vectors
    double* loc_v1[v1_rows];
    for (int i = 0; i < v1_rows; i++) {
        // Setting size of each row
        loc_v1[i] = new double[v1_cols];
        jdoubleArray temp_matrix = static_cast<jdoubleArray>(env->GetObjectArrayElement(v1, i));
        jdouble *temp_data = env->GetDoubleArrayElements(temp_matrix, 0);
        for (int j = 0; j < v1_cols; j++){
            loc_v1[i][j] = temp_data[j];
        }
        env->ReleaseDoubleArrayElements(temp_matrix, temp_data, 0);
    }
    double* loc_w1[w1_rows];
    for (int i = 0; i < w1_rows; i++) {
        // Setting size of each row
        loc_w1[i] = new double[w1_cols];
        jdoubleArray temp_matrix = static_cast<jdoubleArray>(env->GetObjectArrayElement(w1, i));
        jdouble *temp_data = env->GetDoubleArrayElements(temp_matrix, 0);
        for (int j = 0; j < w1_cols; j++){
            loc_w1[i][j] = temp_data[j];
        }
        env->ReleaseDoubleArrayElements(temp_matrix, temp_data, 0);
    }
    double* loc_h[h_rows];
    for (int i = 0; i < h_rows; i++) {
        // Setting size of each row
        loc_w1[i] = new double[h_cols];
        jdoubleArray temp_matrix = static_cast<jdoubleArray>(env->GetObjectArrayElement(h, i));
        jdouble *temp_data = env->GetDoubleArrayElements(temp_matrix, 0);
        for (int j = 0; j < h_cols; j++){
            loc_h[i][j] = temp_data[j];
        }
        env->ReleaseDoubleArrayElements(temp_matrix, temp_data, 0);
    }

    // Performing update
    for (int k = 0; k < w1_cols; k++) {
        for (int n = 0; n < v1_cols; n++) {
            // Calculating V1 estimate - matrix multiplication
            double* v1_est[w1_rows];
            for (int x = 0; x < w1_rows; x++){
                v1_est[x] = new double[h_cols];
                for (int y = 0; y < h_cols; y++){
                    double temp = 0.0;
                    for (int z = 0; z < h_rows; z++){
                        temp = temp + (loc_w1[x][z] * loc_h[z][y]);
                    }
                    v1_est[x][y] = temp;
                }
            }

            // Calculating the numerator and denominator
            double numer = 0;
            double denom = 0;
            for (int f = 0; f < v1_rows; f++) {
                numer = numer + loc_w1[f][k] * loc_v1[f][n] / v1_est[f][n];
                denom = denom + loc_w1[f][k];
            }
            // Updating the H matrix
            loc_h[k][n] = loc_h[k][n] * numer / denom;

            // Deleting the temp matrix
            for (int i = 0; i < v1_rows; i++){
                delete [] v1_est[i];
            }
        }
    }

    // Releasing the local matrices
    for (int i = 0; i < v1_rows; i++){
        delete [] loc_v1[i];
    }
    for (int i = 0; i < w1_rows; i++){
        delete [] loc_w1[i];
    }

    // Constructing jobjectArray structure
    jdoubleArray ret_array = env->NewDoubleArray(h_cols);
    jobjectArray ret_matrix = env->NewObjectArray(h_rows, env->GetObjectClass(ret_array), nullptr);
    // Filling the jobjectArray
    for (int i = 0; i < h_rows; i++) {
        env->SetDoubleArrayRegion(ret_array, 0, h_cols, loc_h[i]);
        env->SetObjectArrayElement(ret_matrix, i, ret_array);
        // Allocate a new spot in memory for the next array
        if(i != h_rows-1) {
            ret_array = env->NewDoubleArray(h_cols);
        }
    }
    // Release matmul matrix, vectors clear themselves
    for (int i = 0; i < h_rows; i++){
        delete[] loc_h[i];
    }
    return ret_matrix;

}
