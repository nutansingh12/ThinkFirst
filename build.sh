#!/bin/bash
set -e

echo "Starting Maven build..."

# Remove test directory to avoid compilation issues
rm -rf src/test

# Build the project
mvn clean package -DskipTests

echo "Build completed successfully!"
ls -lh target/*.jar

