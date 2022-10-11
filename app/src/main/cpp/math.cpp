//
// Created by Daniel on 10/11/2022.
//
#include <jni.h>
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