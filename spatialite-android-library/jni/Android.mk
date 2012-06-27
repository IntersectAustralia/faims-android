LOCAL_PATH := $(call my-dir)

JSQLITE_PATH := javasqlite-20120209
SPATIALITE_PATH := libspatialite-amalgamation-3.0.1
GEOS_PATH := geos-3.2.2
PROJ4_PATH := proj-4.7.0

include $(LOCAL_PATH)/proj4.mk
include $(LOCAL_PATH)/geos.mk
include $(LOCAL_PATH)/spatialite.mk
include $(LOCAL_PATH)/jsqlite.mk
