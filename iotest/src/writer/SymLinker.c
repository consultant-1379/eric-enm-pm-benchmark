#include <unistd.h>
#include "SymLinker.h"

JNIEXPORT jint JNICALL Java_writer_SymLinker_create(JNIEnv *env, jclass theclass, jstring frompath, jstring topath)
{
    jint result;
    const char *p_fromPath = (*env)->GetStringUTFChars(env, frompath, 0);
    const char *p_toPath = (*env)->GetStringUTFChars(env, topath, 0);

    result = symlink(p_fromPath, p_toPath);

    (*env)->ReleaseStringUTFChars(env, frompath, p_fromPath);
    (*env)->ReleaseStringUTFChars(env, topath, p_toPath);

    return result;
}
