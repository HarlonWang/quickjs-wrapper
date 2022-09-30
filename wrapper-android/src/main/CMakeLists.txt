# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

project(quickjs-android-wrapper)

set(QUICKJS_PATH "../../../native/quickjs")

file(STRINGS "${QUICKJS_PATH}/VERSION" CONFIG_VERSION)

add_definitions(-DCONFIG_VERSION=\"${CONFIG_VERSION}\")
add_definitions(-DCONFIG_BIGNUM)

file(GLOB wrapper_src
        "../../../native/cpp/*.cpp"
        "../../../native/cpp/*.h"
        )

file(GLOB quickjs_src
        "${QUICKJS_PATH}/cutils.c"
        "${QUICKJS_PATH}/cutils.h"
        "${QUICKJS_PATH}/libbf.c"
        "${QUICKJS_PATH}/libbf.h"
        "${QUICKJS_PATH}/libregexp-opcode.h"
        "${QUICKJS_PATH}/libregexp.c"
        "${QUICKJS_PATH}/libregexp.h"
        "${QUICKJS_PATH}/libunicode-table.h"
        "${QUICKJS_PATH}/libunicode.c"
        "${QUICKJS_PATH}/libunicode.h"
        "${QUICKJS_PATH}/list.h"
        "${QUICKJS_PATH}/quickjs-atom.h"
        "${QUICKJS_PATH}/quickjs-opcode.h"
        "${QUICKJS_PATH}/quickjs.c"
        "${QUICKJS_PATH}/quickjs.h"
        )

add_library( # Sets the name of the library.
        quickjs-android-wrapper

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${wrapper_src} ${quickjs_src})

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        quickjs-android-wrapper

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib} )