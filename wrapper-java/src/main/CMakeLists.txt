cmake_minimum_required(VERSION 3.23)
project(quickjs-java-wrapper)

set(CMAKE_CXX_STANDARD 11)

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
        quickjs-java-wrapper

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${wrapper_src} ${quickjs_src})


include_directories($ENV{JAVA_HOME}/include/)
include_directories($ENV{JAVA_HOME}/include/darwin)