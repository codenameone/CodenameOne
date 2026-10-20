#!/usr/bin/env python3
"""Generates the fixture credentials for tests/web-mtls-h2.sh into DIR.

  ca.crt                    the CA both servers trust and the client trusts
  server.crt/.key           the servers' certificate, for "localhost" ONLY
  alice.crt/.key, bob.crt/.key   two valid client identities
  alice-enc.key             alice's key as encrypted PKCS#8, passphrase pem-secret
  alice.p12                 alice as PKCS#12, password p12-secret
  expired.crt/.key          signed by the CA, validity ended a year ago
  rogue.crt/.key            valid dates, signed by a CA nobody trusts

Throwaway material for a test: generated per run and never committed.
"""
import datetime
import sys

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec, rsa
from cryptography.hazmat.primitives.serialization import pkcs12
from cryptography.x509.oid import ExtendedKeyUsageOID, NameOID

D = sys.argv[1]
NOW = datetime.datetime.now(datetime.timezone.utc)


def name(cn):
    return x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, cn)])


def write(path, data):
    with open(D + "/" + path, "wb") as f:
        f.write(data)


def pem_key(key, password=None):
    return key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8,
                             serialization.BestAvailableEncryption(password) if password
                             else serialization.NoEncryption())


def ca(cn):
    key = ec.generate_private_key(ec.SECP256R1())
    cert = (x509.CertificateBuilder().subject_name(name(cn)).issuer_name(name(cn))
            .public_key(key.public_key()).serial_number(x509.random_serial_number())
            .not_valid_before(NOW - datetime.timedelta(days=1))
            .not_valid_after(NOW + datetime.timedelta(days=30))
            .add_extension(x509.BasicConstraints(ca=True, path_length=None), critical=True)
            .sign(key, hashes.SHA256()))
    return key, cert


def leaf(cn, issuer_key, issuer, usage, start, end, san=None, rsa_key=False):
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048) if rsa_key \
        else ec.generate_private_key(ec.SECP256R1())
    builder = (x509.CertificateBuilder().subject_name(name(cn)).issuer_name(issuer.subject)
               .public_key(key.public_key()).serial_number(x509.random_serial_number())
               .not_valid_before(start).not_valid_after(end)
               .add_extension(x509.ExtendedKeyUsage([usage]), critical=False))
    if san:
        builder = builder.add_extension(x509.SubjectAlternativeName([x509.DNSName(san)]),
                                        critical=False)
    return key, builder.sign(issuer_key, hashes.SHA256())


ca_key, ca_cert = ca("cn1 fixture CA")
rogue_ca_key, rogue_ca = ca("rogue CA")
write("ca.crt", ca_cert.public_bytes(serialization.Encoding.PEM))
valid = (NOW - datetime.timedelta(days=1), NOW + datetime.timedelta(days=30))
key, cert = leaf("localhost", ca_key, ca_cert, ExtendedKeyUsageOID.SERVER_AUTH, *valid,
                 san="localhost")
write("server.crt", cert.public_bytes(serialization.Encoding.PEM))
write("server.key", pem_key(key))
for who, issuer_key, issuer, dates in (
        ("alice", ca_key, ca_cert, valid),
        ("bob", ca_key, ca_cert, valid),
        ("expired", ca_key, ca_cert, (NOW - datetime.timedelta(days=400),
                                      NOW - datetime.timedelta(days=365))),
        ("rogue", rogue_ca_key, rogue_ca, valid)):
    # RSA for alice, so the Java SE arm's RSA path is the one exercised there.
    key, cert = leaf(who, issuer_key, issuer, ExtendedKeyUsageOID.CLIENT_AUTH, *dates,
                     rsa_key=(who == "alice"))
    write(who + ".crt", cert.public_bytes(serialization.Encoding.PEM))
    write(who + ".key", pem_key(key))
    if who == "alice":
        write("alice-enc.key", pem_key(key, b"pem-secret"))
        write("alice.p12", pkcs12.serialize_key_and_certificates(
            b"alice", key, cert, None,
            # PBES1/3DES, the encryption every JDK 8 reads; the default AES one
            # needs a newer JDK than the local runtime's floor.
            serialization.PrivateFormat.PKCS12.encryption_builder()
            .key_cert_algorithm(pkcs12.PBES.PBESv1SHA1And3KeyTripleDESCBC)
            .kdf_rounds(2048).hmac_hash(hashes.SHA1()).build(b"p12-secret")))
