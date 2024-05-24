# build
## environment requirements:
+ [Java 8](https://adoptium.net/temurin/releases/?arch=x64&package=jdk&version=8) installed with `JDK`, `JAVA_HOME` in your system environment variables.
+ install cmake & ninja
+ install 
+ On Windows add the following to your system PATH:
  + `<JAVA_HOME>\include`
  + `<JAVA_HOME>\include\win32`
  + `C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\VC\Tools\MSVC\14.29.30133\bin\Hostx64\x64`
  + `C:\path\to\mingw64\bin`

### cmake/ninja macos installation
```shell
brew install cmake
brew install ninja
```

### cmake/ninja windows installation
- Install Visual Studio build tools for C++ development
- Install ninja via chocolatey or [UniGetUI](https://github.com/marticliment/WingetUI)
- Install mingw-w64 via the online installer available at https://github.com/niXman/mingw-builds-binaries

### cmake/ninja ubuntu installation

```shell
apt install ninja-build


# If `cmake --version` is older than 3.24.0:
apt remove cmake
wget  https://github.com/Kitware/CMake/releases/download/v3.24.0-rc5/cmake-3.24.0-rc5.tar.gz
tar -xvf cmake-3.24.0-rc5.tar.gz
cd make-3.24.0-rc5
./configure && make && make install

# Warning: doing this could be dangerous for your environment, using distrobox or your system cmake installation is a safer option here.
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
Symlink to `wrapper-java/build/cmake/libquickjs-java-wrapper.<ext>` (`dylib` for darwin/macos, `dll` for win32/windows, `so` for linux)

## TODO
- [ ] 🚧 Cross-compilation available for all platforms