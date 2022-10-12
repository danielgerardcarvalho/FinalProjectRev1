// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("finalprojectrev1");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("finalprojectrev1")
//      }
//    }
#include <jni.h>
#include "math.h"

extern "C"
JNIEXPORT jdoubleArray
JNICALL
Java_com_daniel_finalprojectrev1_NMF_matmul(JNIEnv *env, jobject, jdoubleArray m1, jdoubleArray m2, jint mr1, jint mc1, jint mr2, jint mc2){
    return math().matmul(env, m1, m2, mr1, mc1, mr2, mc2);
};
extern "C"
JNIEXPORT jobjectArray
JNICALL
Java_com_daniel_finalprojectrev1_NMF_matmulDirect(JNIEnv *env, jobject, jobjectArray m1, jobjectArray m2, jint mr1, jint mc1, jint mr2, jint mc2){
    return math().matmulDirect(env, m1, m2, mr1, mc1, mr2, mc2);
};
extern "C"
JNIEXPORT jdoubleArray
JNICALL
Java_com_daniel_finalprojectrev1_NMF_transpose(JNIEnv *env, jobject, jdoubleArray mat, jint mat_rows, jint mat_cols){
    return math().transpose(env, mat, mat_rows, mat_cols);
};
extern "C"
JNIEXPORT jdoubleArray
JNICALL
Java_com_daniel_finalprojectrev1_NMF_add(JNIEnv *env, jobject, jdoubleArray m1, jdoubleArray m2, jint m_row, jint m_cols){
    return math().add(env, m1, m2, m_row, m_cols);
};