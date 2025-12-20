FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

# Install common tools and dependencies
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    unzip \
    zip \
    git \
    python3 \
    xvfb \
    ant \
    maven \
    libncurses6 \
    sudo \
    openjdk-17-jdk \
    openjdk-21-jdk \
    && rm -rf /var/lib/apt/lists/*

# Install Java 8 (Zulu FX)
RUN mkdir -p /usr/lib/jvm
# Using a specific version of Zulu 8 with FX
RUN wget -q https://cdn.azul.com/zulu/bin/zulu8.82.0.21-ca-fx-jdk8.0.432-linux_x64.tar.gz -O /tmp/java8.tar.gz \
    && tar -xzf /tmp/java8.tar.gz -C /usr/lib/jvm \
    && mv /usr/lib/jvm/zulu8.82.0.21-ca-fx-jdk8.0.432-linux_x64 /usr/lib/jvm/java-8-zulu-fx \
    && rm /tmp/java8.tar.gz

# Download cn1-binaries
# We download it to /opt/cn1-binaries so it can be reused
RUN wget -q https://github.com/codenameone/cn1-binaries/archive/refs/heads/master.zip -O /tmp/cn1-binaries.zip \
    && unzip -q /tmp/cn1-binaries.zip -d /opt \
    && mv /opt/cn1-binaries-master /opt/cn1-binaries \
    && rm /tmp/cn1-binaries.zip

# Set convenient env vars
ENV JAVA_HOME_8=/usr/lib/jvm/java-8-zulu-fx
ENV JAVA_HOME_17=/usr/lib/jvm/java-17-openjdk-amd64
ENV JAVA_HOME_21=/usr/lib/jvm/java-21-openjdk-amd64

ENV CN1_BINARIES=/opt/cn1-binaries
