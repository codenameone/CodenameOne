# Builds the Go baseline as a static binary.
#
# CGO_ENABLED=0 and -ldflags "-s -w" so the comparison is static-stripped against
# static-stripped: the Codename One binary the musl target produces links nothing
# and carries no symbol table, and a dynamically linked Go binary with debug
# information would be a different measurement.
FROM golang:alpine AS build
WORKDIR /src
COPY bench-server.go .
RUN go mod init bench >/dev/null 2>&1 || true
RUN CGO_ENABLED=0 go build -trimpath -ldflags "-s -w" -o /out/bench-go bench-server.go

FROM alpine:3.20
COPY --from=build /out/bench-go /bench-go
ENTRYPOINT ["/bench-go"]
