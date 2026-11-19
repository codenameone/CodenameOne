#!/usr/bin/env bash
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Codename One designates this
# particular file as subject to the "Classpath" exception as provided
# in the LICENSE file that accompanied this code.
#
# Starts SQL Server 2025 serving TDS 8.0 (sslmode=strict) on 127.0.0.1:1434
# with a certificate from a throwaway CA, and prints the URL that connects to
# it strictly. 2025 because SQL Server 2022 on Linux refuses every strict
# connection (error 17821) whatever certificate it is given. It is an x86-64
# image: it runs on GitHub's Linux runners, and crashes under Rosetta and qemu.
#
# usage: start-sqlserver-strict.sh <directory for the certificates>
set -euo pipefail
DIR="${1:?usage: start-sqlserver-strict.sh <directory>}"
mkdir -p "$DIR"
cd "$DIR"
HOST=cn1sqlstrict
openssl req -x509 -newkey rsa:2048 -nodes -keyout ca.key -out ca.pem -days 2 \
  -subj "/CN=cn1 strict test CA" 2>/dev/null
openssl req -newkey rsa:2048 -nodes -keyout server.key -out server.csr \
  -subj "/CN=$HOST" 2>/dev/null
printf 'subjectAltName=DNS:localhost,DNS:%s,IP:127.0.0.1\nextendedKeyUsage=serverAuth\nkeyUsage=digitalSignature,keyEncipherment\n' \
  "$HOST" > ext.cnf
openssl x509 -req -in server.csr -CA ca.pem -CAkey ca.key -CAcreateserial \
  -out server.pem -days 2 -extfile ext.cnf 2>/dev/null
# The server runs as user mssql and must read the key.
chmod 644 server.key server.pem
printf '[network]\ntlscert = /cn1tls/server.pem\ntlskey = /cn1tls/server.key\ntlsprotocols = 1.2\n' > mssql.conf
docker run -d --name cn1-sqlserver-strict --hostname "$HOST" \
  -e ACCEPT_EULA=Y -e MSSQL_SA_PASSWORD=Cn1-Secret-9x -p 1434:1433 \
  -v "$PWD:/cn1tls:ro" -v "$PWD/mssql.conf:/var/opt/mssql/mssql.conf:ro" \
  mcr.microsoft.com/mssql/server:2025-latest >/dev/null
for _ in $(seq 1 90); do
  if docker logs cn1-sqlserver-strict 2>&1 | grep -q "ready for client"; then
    echo "sqlserver://sa:Cn1-Secret-9x@localhost:1434/master?sslmode=strict&sslrootcert=$PWD/ca.pem"
    exit 0
  fi
  sleep 2
done
docker logs cn1-sqlserver-strict 2>&1 | tail -20 >&2
echo "SQL Server 2025 did not become ready" >&2
exit 1
