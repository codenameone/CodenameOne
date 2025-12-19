FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

# Install basics and tools
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    gnupg \
    ca-certificates \
    git \
    unzip \
    zip \
    python3 \
    python3-pip \
    xvfb \
    libgl1-mesa-dri \
    libxtst6 \
    libxxf86vm1 \
    ant \
    maven \
    libncurses6 \
    libstdc++6 \
    && rm -rf /var/lib/apt/lists/*

# Setup Azul Repo
RUN curl -s https://repos.azul.com/azul-repo.key | gpg --dearmor -o /usr/share/keyrings/azul.gpg \
    && echo "deb [signed-by=/usr/share/keyrings/azul.gpg] https://repos.azul.com/zulu/deb stable main" | tee /etc/apt/sources.list.d/zulu.list

# Install JDKs 11, 17, 21, 25
RUN apt-get update && apt-get install -y \
    zulu11-jdk \
    zulu17-jdk \
    zulu21-jdk \
    zulu25-jdk \
    && rm -rf /var/lib/apt/lists/*

# Install ZuluFX 8 (Tarball)
# URL for Zulu 8 + FX (x64 Linux)
# Using a fixed version to ensure reproducibility.
RUN mkdir -p /usr/lib/jvm/zulu8-fx && \
    curl -L "https://cdn.azul.com/zulu/bin/zulu8.82.0.21-ca-fx-jdk8.0.432-linux_x64.tar.gz" -o /tmp/zulu8.tar.gz && \
    tar -xzf /tmp/zulu8.tar.gz -C /usr/lib/jvm/zulu8-fx --strip-components=1 && \
    rm /tmp/zulu8.tar.gz

# Clone cn1-binaries
RUN git clone --depth=1 https://github.com/codenameone/cn1-binaries /opt/cn1-binaries

# Set Environment Variables for JDKs (Paths for apt installed JDKs usually symlinked to /usr/lib/jvm/zulu-XX-amd64)
ENV JAVA8_HOME=/usr/lib/jvm/zulu8-fx
ENV JDK8_HOME=/usr/lib/jvm/zulu8-fx
ENV JAVA11_HOME=/usr/lib/jvm/zulu11-ca-amd64
ENV JDK11_HOME=/usr/lib/jvm/zulu11-ca-amd64
ENV JAVA17_HOME=/usr/lib/jvm/zulu17-ca-amd64
ENV JDK17_HOME=/usr/lib/jvm/zulu17-ca-amd64
ENV JAVA21_HOME=/usr/lib/jvm/zulu21-ca-amd64
ENV JDK21_HOME=/usr/lib/jvm/zulu21-ca-amd64
ENV JAVA25_HOME=/usr/lib/jvm/zulu25-ca-amd64
ENV JDK25_HOME=/usr/lib/jvm/zulu25-ca-amd64

# Install Android SDK
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q ${CMDLINE_TOOLS_URL} -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

ENV PATH=${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/emulator:${PATH}

# Accept licenses and install packages
# Note: sdkmanager requires Java 11+ to run. We use JAVA17_HOME for this step.
# We dynamically locate Zulu 17 to ensure the correct path is used.
RUN export JAVA17_REAL=$(find /usr/lib/jvm -maxdepth 1 -name "zulu*17*" -type d | head -n 1) && \
    echo "Using JDK 17 at: $JAVA17_REAL" && \
    yes | JAVA_HOME=$JAVA17_REAL sdkmanager --licenses && \
    JAVA_HOME=$JAVA17_REAL sdkmanager "platform-tools" "platforms;android-31" "build-tools;31.0.0"

# Set Default JAVA_HOME to Java 8 (as per user requirement for builds)
ENV JAVA_HOME=/usr/lib/jvm/zulu8-fx
ENV PATH=$JAVA_HOME/bin:$PATH

ENV CN1_BINARIES=/opt/cn1-binaries

# Verify installations
RUN java -version && \
    $JAVA11_HOME/bin/java -version && \
    $JAVA17_HOME/bin/java -version && \
    $JAVA21_HOME/bin/java -version && \
    $JAVA25_HOME/bin/java -version && \
    ant -version && \
    mvn -version && \
    python3 --version && \
    JAVA_HOME=${JAVA17_HOME} sdkmanager --version
