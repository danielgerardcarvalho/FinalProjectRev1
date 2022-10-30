//
// Created by Daniel on 10/11/2022.
//

#ifndef FINALPROJECTREV1_MATH_H
#define FINALPROJECTREV1_MATH_H

#include <jni.h>

class math {
    public: jdoubleArray matmul(JNIEnv *env, jdoubleArray matrix1, jdoubleArray matrix2,
                                jint matrix1_rows, jint matrix1_cols, jint matrix2_rows, jint matrix2_cols);
    public: jdoubleArray transpose(JNIEnv *env, jdoubleArray mat, jint mat_rows, jint mat_cols);
    public: jdoubleArray add(JNIEnv *env, jdoubleArray m1, jdoubleArray m2, jint m1_rows, jint m1_cols);
    public: jobjectArray matmulDirect(JNIEnv *env, jobjectArray m1, jobjectArray m2, jint m1_rows,
                                       jint m1_cols, jint m2_rows, jint m2_cols);
    public: jobjectArray hUpdateDirect(JNIEnv *env, jobjectArray v1, jobjectArray w1, jobjectArray h,
                                       jint v1_rows, jint v1_cols, jint w1_rows, jint w1_cols,
                                       jint h_rows, jint h_cols);
};

#endif //FINALPROJECTREV1_MATH_H
