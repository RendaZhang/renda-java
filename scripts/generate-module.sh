#!/bin/bash

# Maven Archetype Helper Script
# Usage: ./scripts/generate-module.sh [module-name]

MODULE_NAME=$1

if [ -z "$MODULE_NAME" ]; then
    echo "Usage: $0 [module-name]"
    exit 1
fi

echo "Generating new module: $MODULE_NAME..."

# Create directory structure
mkdir -p $MODULE_NAME/src/main/java/com/renda/$MODULE_NAME
mkdir -p $MODULE_NAME/src/main/resources
mkdir -p $MODULE_NAME/src/test/java/com/renda/$MODULE_NAME

# Create pom.xml
cat <<EOF > $MODULE_NAME/pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.renda</groupId>
        <artifactId>renda-java</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>$MODULE_NAME</artifactId>
    <name>$MODULE_NAME Module</name>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
EOF

# Register in parent pom.xml if not exists
if ! grep -q "<module>$MODULE_NAME</module>" pom.xml; then
    sed -i "/<modules>/a \        <module>$MODULE_NAME</module>" pom.xml
fi

echo "Module $MODULE_NAME generated and registered in parent POM."
