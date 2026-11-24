# Codename One deployment template -- see ../../references/deployment.md.
# Replace myapp with the project's name and app.example.com with the domain. The binary
# is named after the backend module's artifactId: backend/target/myapp-backend here.
# Delete the webapp line when the server hosts no browser app.
# Dockerfile -- the binary was built on ubuntu:24.04, so it runs on ubuntu:24.04
FROM ubuntu:24.04
RUN apt-get update \
 && apt-get install -y --no-install-recommends libssl3t64 libnghttp2-14 libcurl4t64 ca-certificates \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --home /app myapp
WORKDIR /app
COPY backend/target/myapp-backend ./myapp
COPY backend/application.properties ./
COPY backend/target/webapp ./webapp
USER myapp
EXPOSE 8080
ENTRYPOINT ["/app/myapp"]
