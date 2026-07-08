// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::working-with-linux-bash-001[]
mvn -pl common package -Dcodename1.platform=linux -Dcodename1.buildTarget=local-linux-device cn1:build
// end::working-with-linux-bash-001[]

// tag::working-with-linux-bash-002[]
sudo apt-get install -y \
  cmake ninja-build pkg-config \
  libgtk-3-dev libcairo2-dev libpango1.0-dev libgdk-pixbuf-2.0-dev libglib2.0-dev \
  libfontconfig1-dev libfreetype-dev libcurl4-openssl-dev \
  libgstreamer1.0-dev libgstreamer-plugins-base1.0-dev \
  libwebkit2gtk-4.1-dev libsecret-1-dev libnotify-dev libgeoclue-2-dev \
  libepoxy-dev libegl1-mesa-dev libgles2-mesa-dev
// end::working-with-linux-bash-002[]
