LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := CollectPath

LOCAL_DEX_PREOPT := false
LOCAL_CERTIFICATE := platform
#LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

# additionally, build tests in sub-folders in a separate .apk
#include $(call all-makefiles-under,$(LOCAL_PATH))
