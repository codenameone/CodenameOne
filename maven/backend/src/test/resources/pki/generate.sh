#!/bin/bash
# Regenerates the PKI fixtures of PkiTest with the openssl command line (3.4 or
# newer, for -not_before/-not_after). Run it in this directory, then copy the
# results into vm/backend/demo/authcheck/com/demo/PkiFixtures.java so both
# runtimes test the same files. The keys exist for the tests alone.
set -e
O="${OPENSSL:-openssl}"
cat > ext.cnf <<'EOF'
[req]
distinguished_name=dn
[dn]
[root]
basicConstraints=critical,CA:TRUE
keyUsage=critical,keyCertSign,cRLSign
subjectKeyIdentifier=hash
[inter]
basicConstraints=critical,CA:TRUE,pathlen:0
keyUsage=critical,keyCertSign,cRLSign
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid
[leaf]
basicConstraints=critical,CA:FALSE
keyUsage=critical,digitalSignature
extendedKeyUsage=serverAuth
subjectAltName=DNS:api.example.com,DNS:*.example.com,IP:10.0.0.1,IP:2001:db8::1
authorityKeyIdentifier=keyid
[client]
basicConstraints=critical,CA:FALSE
extendedKeyUsage=clientAuth
subjectAltName=DNS:client.example.com
[critical]
basicConstraints=critical,CA:FALSE
subjectAltName=DNS:api.example.com
certificatePolicies=critical,2.23.140.1.2.1
[subca]
basicConstraints=critical,CA:TRUE
keyUsage=critical,keyCertSign
EOF
NB="-not_before 20250101000000Z -not_after 21250101000000Z"
$O genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -out root.key
$O req -new -x509 -key root.key -subj "/C=US/O=Test PKI/CN=Test Root" -sha256 -extensions root -config ext.cnf $NB -out root.pem
$O genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out inter.key
$O req -new -key inter.key -subj "/C=US/O=Test PKI/CN=Test Intermediate" -config ext.cnf -out inter.csr
$O x509 -req -in inter.csr -CA root.pem -CAkey root.key -set_serial 2 -sha256 -extfile ext.cnf -extensions inter $NB -out inter.pem
$O genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -out leaf.key
$O req -new -key leaf.key -subj "/O=Example/CN=api.example.com" -config ext.cnf -out leaf.csr
$O x509 -req -in leaf.csr -CA inter.pem -CAkey inter.key -set_serial 3 -sha256 -extfile ext.cnf -extensions leaf $NB -out leaf.pem
$O x509 -req -in leaf.csr -CA inter.pem -CAkey inter.key -set_serial 4 -sha256 -extfile ext.cnf -extensions leaf -not_before 20200101000000Z -not_after 20210101000000Z -out expired.pem
$O x509 -req -in leaf.csr -CA inter.pem -CAkey inter.key -set_serial 5 -sha256 -extfile ext.cnf -extensions client $NB -out client.pem
$O x509 -req -in leaf.csr -CA inter.pem -CAkey inter.key -set_serial 6 -sha256 -extfile ext.cnf -extensions critical $NB -out critical.pem
$O x509 -req -in inter.csr -CA leaf.pem -CAkey leaf.key -set_serial 7 -sha256 -extfile ext.cnf -extensions subca $NB -out byleaf.pem
$O genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -out sub.key
$O req -new -key sub.key -subj "/CN=Test Sub CA" -config ext.cnf -out sub.csr
$O x509 -req -in sub.csr -CA inter.pem -CAkey inter.key -set_serial 8 -sha256 -extfile ext.cnf -extensions subca $NB -out sub.pem
$O x509 -req -in leaf.csr -CA sub.pem -CAkey sub.key -set_serial 9 -sha256 -extfile ext.cnf -extensions leaf $NB -out undersub.pem
$O req -new -x509 -key inter.key -subj "/CN=PSS Root" -sha256 -sigopt rsa_padding_mode:pss -sigopt rsa_pss_saltlen:32 -sigopt rsa_mgf1_md:sha256 -extensions root -config ext.cnf $NB -out pss.pem
$O genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-384 -out p384.key
$O req -new -x509 -key p384.key -subj "/CN=P-384 Root" -sha384 -extensions root -config ext.cnf $NB -out p384.pem
$O req -new -x509 -key root.key -subj "/CN=SHA1 Root" -sha1 -extensions root -config ext.cnf $NB -out sha1.pem
cat leaf.pem inter.pem > chain.pem
$O pkcs12 -export -inkey leaf.key -in leaf.pem -certfile inter.pem -name api -passout pass:changeit -out leaf.p12
$O pkcs12 -export -legacy -inkey leaf.key -in leaf.pem -name api -passout pass:changeit -out legacy.p12
$O req -new -key leaf.key -subj "/CN=api.example.com" -addext "subjectAltName=DNS:api.example.com,DNS:www.example.com" -config ext.cnf -out openssl.csr
rm -f inter.csr leaf.csr sub.csr ext.cnf sub.key
