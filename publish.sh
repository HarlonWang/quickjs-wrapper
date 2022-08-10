#!/bin/bash
./gradlew publish --no-daemon --no-parallel
./gradlew closeAndReleaseRepository