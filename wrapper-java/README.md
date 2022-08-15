# 构建
## 环境要求:
+ JDK & JAVA_HOME 环境变量
+ 安装好 cmake ninja

### cmake 安装方式 mac
```
brew install cmake
```

### ninja 安装方式 mac
```
brew install ninja
```

### cmake 安装方式 linux
卸载系统自带的版本(太老)

```
apt remove cmake

```

```
wget  https://github.com/Kitware/CMake/releases/download/v3.24.0-rc5/cmake-3.24.0-rc5.tar.gz
```

```
tar -xvf cmake-3.24.0-rc5.tar.gz
```

```
cd make-3.24.0-rc5
```

```
./configure && make && make install
```

### ninja 安装方式 linux
```
apt install ninja-build
```

## 打包 so 链库
```
./gradlew wrapper-java:cmakeBuild
```

## 产物
so 链库地址:
```shell
    wrapper-java/build/cmake/libquickjs-java-wrapper.dylib
```

## TODO
- [x] 跨平台编译方式(在单一平台编译出其他平台产物)