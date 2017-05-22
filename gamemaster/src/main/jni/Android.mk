LOCAL_PATH := $(call my-dir)
  
include $(CLEAR_VARS)

LOCAL_MODULE        := util
LOCAL_SRC_FILES    :=util.c

LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)