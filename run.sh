#!/usr/bin/env bash
# Smart Traffic Management System - build and run (Linux / macOS). Needs JDK 17 or newer.
set -e
cd "$(dirname "$0")"
mkdir -p out
javac --release 17 -d out $(find src -name '*.java')
# lib/*.jar (mysql-connector-j) is added to the classpath if present
java -cp "out:lib/*" traffic.Main "$@"
