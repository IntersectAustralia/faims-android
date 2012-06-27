include $(CLEAR_VARS)
# -DOMIT_GEOS=0
# ./configure --build=x86_64-pc-linux-gnu --host=arm-linux-eabi
LOCAL_MODULE    := spatialite
LOCAL_CFLAGS    := \
        -DOMIT_FREEXL \
        -DOMIT_ICONV \
        -DVERSION=\"3.0.1\" \
        -Dfdatasync=fsync \
        -DSQLITE_ENABLE_RTREE=1
        -DSQLITE_DEFAULT_FILE_FORMAT=4 \
        -DSQLITE_OMIT_BUILTIN_TEST=1 \
        -DSQLITE_OMIT_DEPRECATED=1
LOCAL_LDLIBS    := -llog 
LOCAL_C_INCLUDES := \
        $(GEOS_PATH)/source/headers \
        $(GEOS_PATH)/capi \
        $(PROJ4_PATH)/src
LOCAL_SRC_FILES := \
        $(SPATIALITE_PATH)/spatialite.c \
        $(SPATIALITE_PATH)/sqlite3.c
LOCAL_STATIC_LIBRARIES := proj geos
include $(BUILD_STATIC_LIBRARY)