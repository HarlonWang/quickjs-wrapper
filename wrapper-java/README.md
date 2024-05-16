# build
## environment requirements:
+ [Java 8](https://adoptium.net/temurin/releases/?arch=x64&package=jdk&version=8) installed with `JDK`, `JAVA_HOME` in your system environment variables.
+ install cmake & ninja
+ On Windows add the following to your system PATH:
  + `<JAVA_HOME>/include`
  + `<JAVA_HOME>/include/win32`
  + `C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\VC\Tools\MSVC\14.29.30133\bin\Hostx64\x64`

### cmake/ninja macos installation
```shell
brew install cmake
brew install ninja
```

### cmake/ninja windows installation
- Install Visual Studio build tools for C++ development
- Install ninja via chocolatey or [UniGetUI](https://github.com/marticliment/WingetUI)
```shell
"C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" x86_amd64
```

### cmake/ninja ubuntu installation

```shell
apt install ninja-build


# If `cmake --version` is older than 3.24.0:
apt remove cmake
wget  https://github.com/Kitware/CMake/releases/download/v3.24.0-rc5/cmake-3.24.0-rc5.tar.gz
tar -xvf cmake-3.24.0-rc5.tar.gz
cd make-3.24.0-rc5
./configure && make && make install

# Warning: doing this could be dangerous for your environment, using distrobox is a safer option here.
```

## Building Dynamic Link Libraries
```shell
cd wrapper-java

// step 1
cmake -DCMAKE_BUILD_TYPE=Debug -DCMAKE_MAKE_PROGRAM=ninja -G Ninja -S ./src/main -B ./build/cmake

// step 2
cmake --build ./build/cmake --target quickjs-java-wrapper -j 6
```

## Output
Symlink to `wrapper-java/build/cmake/libquickjs-java-wrapper.dylib`

## TODO
- [ ] Cross-compilation available for all platforms