cmake_minimum_required(VERSION 3.23)
project(quickjs-java-wrapper)

set(CMAKE_CXX_STANDARD 11)

set(CPP_PATH "../../../native/cpp")

file(STRINGS "${CPP_PATH}/quickjs/VERSION" CONFIG_VERSION)

add_definitions(-DCONFIG_VERSION=\"${CONFIG_VERSION}\")
add_definitions(-DCONFIG_BIGNUM)

file(GLOB wrapper_src
        "${CPP_PATH}/*.cpp"
        "${CPP_PATH}/*.h"
        )

file(GLOB quickjs_src
        "${CPP_PATH}/quickjs/*.c"
        "${CPP_PATH}/quickjs/*.h"
        )

add_library( # Sets the name of the library.
        quickjs-java-wrapper

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${wrapper_src} ${quickjs_src})


include_directories($ENV{JAVA_HOME}/include/)
include_directories($ENV{JAVA_HOME}/include/darwin)