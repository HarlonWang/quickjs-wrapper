# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

project(quickjs-android-wrapper)

include(${CMAKE_CURRENT_SOURCE_DIR}/../../../CMakeLists.txt)

file(GLOB android_src
        "cpp/quickjs_android.cpp"
        )

add_library( # Sets the name of the library.
        quickjs-android-wrapper

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${wrapper_src} ${quickjs_src} ${android_src})

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
        "-Wl,-z,max-page-size=16384"
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})