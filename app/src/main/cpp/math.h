//
// Created by Daniel on 10/11/2022.
//

#ifndef FINALPROJECTREV1_MATH_H
#define FINALPROJECTREV1_MATH_H

#include <jni.h>

class math {
    public: jdoubleArray matmul(JNIEnv *env, jdoubleArray matrix1, jdoubleArray matrix2, jint matrix1_rows,
                                jint matrix1_cols, jint matrix2_rows, jint matrix2_cols);
    public: jdoubleArray transpose(JNIEnv *env, jdoubleArray mat, jint mat_rows, jint mat_cols);
    public: jdoubleArray add(JNIEnv *env, jdoubleArray m1, jdoubleArray m2, jint m1_rows, jint m1_cols);
};

#endif //FINALPROJECTREV1_MATH_H