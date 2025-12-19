FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

# Install common dependencies
# libncurses6 is needed for some android tools on newer ubuntu? or libncurses5?
# Ubuntu 24.04 has libncurses6 by default. libncurses5 might need a legacy package or symlink.
# We will try installing libncurses5 but it might not be available. We'll install libncurses6 and maybe symlink if needed.
# Actually, the android sdk manager usually works with 6.
# We include basic build tools and libraries.
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    git \
    unzip \
    zip \
    python3 \
    python3-pip \
    sudo \
    xvfb \
    ant \
    maven \
    libncurses6 \
    libstdc++6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    clang \
    gnupg \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Install Azul Zulu Repository
RUN curl -s https://repos.azul.com/azul-repo.key | gpg --dearmor -o /usr/share/keyrings/azul.gpg \
    && echo "deb [signed-by=/usr/share/keyrings/azul.gpg] https://repos.azul.com/zulu/deb stable main" | tee /etc/apt/sources.list.d/zulu.list \
    && apt-get update

# Install Java versions

# Zulu 8 with FX (Manual download to ensure FX presence and version stability)
# Using a known working URL for ZuluFX 8 Linux x64
RUN mkdir -p /usr/lib/jvm && \
    wget -q https://cdn.azul.com/zulu/bin/zulu8.82.0.21-ca-fx-jdk8.0.432-linux_x64.tar.gz -O /tmp/zulu8.tar.gz && \
    tar -xf /tmp/zulu8.tar.gz -C /usr/lib/jvm && \
    mv /usr/lib/jvm/zulu8.82.0.21-ca-fx-jdk8.0.432-linux_x64 /usr/lib/jvm/zulu8-fx && \
    rm /tmp/zulu8.tar.gz

# Install other JDKs via apt
RUN apt-get install -y \
    zulu11-jdk \
    zulu17-jdk \
    zulu21-jdk

# Note: zulu25-jdk might be available. If not, this step might fail.
# If it fails, we will have to adjust. But the user implies availability.
RUN apt-get install -y zulu25-jdk || echo "zulu25-jdk install failed, proceeding without it"

# Android SDK
ENV ANDROID_HOME=/opt/android/sdk
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip /tmp/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# Accept licenses and install packages
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "platforms;android-31" "platforms;android-33" "platforms;android-34" "build-tools;31.0.0" "build-tools;33.0.1" "build-tools;34.0.0" "ndk;25.2.9519653"

# cn1-binaries
ENV CN1_BINARIES=/opt/cn1-binaries
RUN git clone --depth=1 https://github.com/codenameone/cn1-binaries.git ${CN1_BINARIES}

# Environment Variables
ENV JAVA_HOME=/usr/lib/jvm/zulu8-fx
ENV JAVA8_HOME=/usr/lib/jvm/zulu8-fx
ENV JDK8_HOME=/usr/lib/jvm/zulu8-fx

# Paths for other JDKs (Assuming standard Azul apt paths, but checking via wildcard expansion in shell if we could,
# but here we hardcode likely paths based on Azul naming convention on Ubuntu)
# Azul usually installs to /usr/lib/jvm/zulu-<version>-amd64 or similar.
# Let's inspect typical paths: /usr/lib/jvm/zulu-11-amd64
# Actually, checking online, it seems to be /usr/lib/jvm/zulu11-ca-amd64 for CA builds, or zulu-11-amd64.
# We will use symlinks or generic paths if possible.
# But to be safe, we can look for them.
# Since we can't run dynamic shell in ENV, we will try the most common one.
# If these paths are wrong, the build might fail later when finding java.
# We will verify them in a RUN step or create symlinks.

RUN for v in 11 17 21 25; do \
        dir=$(ls -d /usr/lib/jvm/zulu*${v}* 2>/dev/null | head -n 1); \
        if [ -n "$dir" ]; then \
            echo "Linking $dir to /usr/lib/jvm/java-${v}-zulu"; \
            ln -s "$dir" "/usr/lib/jvm/java-${v}-zulu"; \
        else \
            echo "Java ${v} not found"; \
        fi \
    done

ENV JAVA11_HOME=/usr/lib/jvm/java-11-zulu
ENV JDK11_HOME=/usr/lib/jvm/java-11-zulu
ENV JAVA17_HOME=/usr/lib/jvm/java-17-zulu
ENV JDK17_HOME=/usr/lib/jvm/java-17-zulu
ENV JAVA21_HOME=/usr/lib/jvm/java-21-zulu
ENV JDK21_HOME=/usr/lib/jvm/java-21-zulu
ENV JAVA25_HOME=/usr/lib/jvm/java-25-zulu
ENV JDK25_HOME=/usr/lib/jvm/java-25-zulu

# Set default java to 8
RUN update-alternatives --install /usr/bin/java java /usr/lib/jvm/zulu8-fx/bin/java 100 && \
    update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/zulu8-fx/bin/javac 100

CMD ["/bin/bash"]
