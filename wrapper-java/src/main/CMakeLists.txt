cmake_minimum_required(VERSION 3.23)
project(quickjs-java-wrapper)

set(CMAKE_CXX_STANDARD 11)

include(${CMAKE_CURRENT_SOURCE_DIR}/../../../CMakeLists.txt)

add_library( # Sets the name of the library.
        quickjs-java-wrapper

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${wrapper_src} ${quickjs_src})


include_directories($ENV{JAVA_HOME}/include/)

# keeping this or changing it to include/win32 has no perceivable effect on the build
# include_directories($ENV{JAVA_HOME}/include/darwin)