include $(CLEAR_VARS)
# ./configure --build=x86_64-pc-linux-gnu --host=arm-linux-eabi
LOCAL_MODULE := iconv
LOCAL_CFLAGS := \
	-Wno-multichar \
	-D_ANDROID \
	-DBUILDING_LIBICONV \
	-DIN_LIBRARY \
	-DLIBDIR="\"~/android-libs/usr/local/lib\"" 
LOCAL_C_INCLUDES := \
	$(ICONV_PATH)/libiconv-1.13.1 \
	$(ICONV_PATH)/libiconv-1.13.1/include \
	$(ICONV_PATH)/libiconv-1.13.1/lib \
	$(ICONV_PATH)/libiconv-1.13.1/libcharset/include
LOCAL_SRC_FILES := \
	$(ICONV_PATH)/lib/iconv.c \
	$(ICONV_PATH)/lib/relocatable.c \
	$(ICONV_PATH)/libcharset/lib/localcharset.c
include $(BUILD_STATIC_LIBRARY)