"""A subset of itsdangerous, wire compatible with the real package.

Signer, TimestampSigner, Serializer, TimedSerializer, URLSafeSerializer and
URLSafeTimedSerializer produce and accept exactly the bytes itsdangerous 2.x
does for the same secret key, salt and key derivation, so a value signed by a
CPython server verifies here and the other way around.  Supported key
derivations are ``django-concat`` (the default), ``concat``, ``hmac`` and
``none``; the digest is SHA-1 unless ``digest_method`` names another hashlib
constructor.  Key rotation (a list of secret keys) is supported: the last key
signs, every key verifies.
"""
import base64 as _base64
import hashlib as _hashlib
import hmac as _hmac
import json as _json
import time as _time
import zlib as _zlib

__all__ = ["BadData", "BadSignature", "BadTimeSignature", "BadPayload", "SignatureExpired",
           "Signer", "TimestampSigner", "Serializer", "TimedSerializer",
           "URLSafeSerializer", "URLSafeTimedSerializer", "base64_encode", "base64_decode"]


class BadData(Exception):
    def __init__(self, message):
        Exception.__init__(self, message)
        self.message = message

    def __str__(self):
        return self.message


class BadSignature(BadData):
    def __init__(self, message, payload=None):
        BadData.__init__(self, message)
        self.payload = payload


class BadTimeSignature(BadSignature):
    def __init__(self, message, payload=None, date_signed=None):
        BadSignature.__init__(self, message, payload)
        self.date_signed = date_signed


class SignatureExpired(BadTimeSignature):
    pass


class BadHeader(BadSignature):
    pass


class BadPayload(BadData):
    def __init__(self, message, original_error=None):
        BadData.__init__(self, message)
        self.original_error = original_error


def want_bytes(s, encoding="utf-8"):
    if isinstance(s, str):
        return s.encode(encoding)
    return s


def base64_encode(string):
    return _base64.urlsafe_b64encode(want_bytes(string)).rstrip(b"=")


def base64_decode(string):
    string = want_bytes(string)
    try:
        return _base64.urlsafe_b64decode(string + b"=" * (-len(string) % 4))
    except Exception as e:
        raise BadData("Could not base64 decode the payload because of an exception")


def int_to_bytes(num):
    out = bytearray()
    while num > 0:
        out.insert(0, num & 0xFF)
        num >>= 8
    return bytes(out)


def bytes_to_int(b):
    n = 0
    for c in b:
        n = (n << 8) | c
    return n


def _secret_keys(secret_key):
    if isinstance(secret_key, (str, bytes)):
        return [want_bytes(secret_key)]
    return [want_bytes(k) for k in secret_key]


class Signer:
    default_digest_method = staticmethod(_hashlib.sha1)
    default_key_derivation = "django-concat"

    def __init__(self, secret_key, salt=b"itsdangerous.Signer", sep=b".", key_derivation=None,
                 digest_method=None, algorithm=None):
        self.secret_keys = _secret_keys(secret_key)
        self.sep = want_bytes(sep)
        if salt is None:
            salt = b"itsdangerous.Signer"
        self.salt = want_bytes(salt)
        self.key_derivation = key_derivation or self.default_key_derivation
        self.digest_method = digest_method or self.default_digest_method

    @property
    def secret_key(self):
        return self.secret_keys[-1]

    def derive_key(self, secret_key=None):
        if secret_key is None:
            secret_key = self.secret_keys[-1]
        else:
            secret_key = want_bytes(secret_key)
        kd = self.key_derivation
        if kd == "concat":
            return self.digest_method(self.salt + secret_key).digest()
        if kd == "django-concat":
            return self.digest_method(self.salt + b"signer" + secret_key).digest()
        if kd == "hmac":
            return _hmac.new(secret_key, self.salt, self.digest_method).digest()
        if kd == "none":
            return secret_key
        raise TypeError("Unknown key derivation method")

    def _sig(self, key, value):
        return base64_encode(_hmac.new(key, value, self.digest_method).digest())

    def get_signature(self, value):
        return self._sig(self.derive_key(), want_bytes(value))

    def sign(self, value):
        value = want_bytes(value)
        return value + self.sep + self.get_signature(value)

    def verify_signature(self, value, sig):
        value = want_bytes(value)
        sig = want_bytes(sig)
        for k in reversed(self.secret_keys):
            if _hmac.compare_digest(self._sig(self.derive_key(k), value), sig):
                return True
        return False

    def unsign(self, signed_value):
        signed_value = want_bytes(signed_value)
        if self.sep not in signed_value:
            raise BadSignature("No '" + self.sep.decode("utf-8") + "' found in value")
        value, sig = signed_value.rsplit(self.sep, 1)
        if self.verify_signature(value, sig):
            return value
        raise BadSignature("Signature " + repr(sig) + " does not match", payload=value)

    def validate(self, signed_value):
        try:
            self.unsign(signed_value)
            return True
        except BadSignature:
            return False


class TimestampSigner(Signer):
    def get_timestamp(self):
        return int(_time.time())

    def timestamp_to_datetime(self, ts):
        import datetime as _dt
        return _dt.datetime.fromtimestamp(ts, _dt.timezone.utc)

    def sign(self, value):
        value = want_bytes(value)
        value = value + self.sep + base64_encode(int_to_bytes(self.get_timestamp()))
        return value + self.sep + self.get_signature(value)

    def unsign(self, signed_value, max_age=None, return_timestamp=False):
        try:
            result = Signer.unsign(self, signed_value)
            sig_error = None
        except BadSignature as e:
            sig_error = e
            result = e.payload or b""
        sep = self.sep
        if sep not in result:
            if sig_error:
                raise sig_error
            raise BadTimeSignature("timestamp missing", payload=result)
        value, ts_bytes = result.rsplit(sep, 1)
        ts_int = None
        try:
            ts_int = bytes_to_int(base64_decode(ts_bytes))
        except Exception:
            pass
        if sig_error is not None:
            raise BadTimeSignature(str(sig_error), payload=value)
        if ts_int is None:
            raise BadTimeSignature("Malformed timestamp", payload=value)
        if max_age is not None:
            age = self.get_timestamp() - ts_int
            if age > max_age:
                raise SignatureExpired("Signature age " + str(age) + " > " + str(max_age)
                                       + " seconds", payload=value)
            if age < 0:
                raise SignatureExpired("Signature age " + str(age) + " < 0 seconds", payload=value)
        if return_timestamp:
            return value, self.timestamp_to_datetime(ts_int)
        return value

    def validate(self, signed_value, max_age=None):
        try:
            self.unsign(signed_value, max_age=max_age)
            return True
        except BadSignature:
            return False


class _CompactJson:
    @staticmethod
    def loads(payload):
        return _json.loads(payload)

    @staticmethod
    def dumps(obj, **kwargs):
        kwargs.setdefault("ensure_ascii", False)
        kwargs.setdefault("separators", (",", ":"))
        return _json.dumps(obj, **kwargs)


class Serializer:
    default_serializer = _json
    default_signer = Signer

    def __init__(self, secret_key, salt=b"itsdangerous", serializer=None, serializer_kwargs=None,
                 signer=None, signer_kwargs=None, fallback_signers=None):
        self.secret_keys = _secret_keys(secret_key)
        self.salt = want_bytes(salt) if salt is not None else None
        self.serializer = serializer if serializer is not None else self.default_serializer
        self.serializer_kwargs = serializer_kwargs or {}
        self.signer = signer if signer is not None else self.default_signer
        self.signer_kwargs = signer_kwargs or {}

    @property
    def secret_key(self):
        return self.secret_keys[-1]

    def load_payload(self, payload, serializer=None):
        s = serializer if serializer is not None else self.serializer
        try:
            if isinstance(payload, bytes):
                payload = payload.decode("utf-8")
            return s.loads(payload)
        except Exception as e:
            raise BadPayload("Could not load the payload because an exception occurred on"
                             " unserializing the data.", original_error=e)

    def dump_payload(self, obj):
        return want_bytes(self.serializer.dumps(obj, **self.serializer_kwargs))

    def make_signer(self, salt=None):
        if salt is None:
            salt = self.salt
        return self.signer(self.secret_keys, salt=salt, **self.signer_kwargs)

    def dumps(self, obj, salt=None):
        rv = self.make_signer(salt).sign(self.dump_payload(obj))
        return rv.decode("utf-8")

    def loads(self, s, salt=None, **kwargs):
        return self.load_payload(self.make_signer(salt).unsign(want_bytes(s), **kwargs))

    def loads_unsafe(self, s, salt=None, **kwargs):
        try:
            return True, self.loads(s, salt, **kwargs)
        except BadSignature as e:
            if e.payload is None:
                return False, None
            try:
                return False, self.load_payload(e.payload)
            except BadPayload:
                return False, None


class TimedSerializer(Serializer):
    default_signer = TimestampSigner

    def loads(self, s, max_age=None, return_timestamp=False, salt=None):
        signer = self.make_signer(salt)
        base64d, ts = signer.unsign(want_bytes(s), max_age=max_age, return_timestamp=True)
        payload = self.load_payload(base64d)
        if return_timestamp:
            return payload, ts
        return payload


class URLSafeSerializerMixin:
    default_serializer = _CompactJson

    def load_payload(self, payload, *args, **kwargs):
        payload = want_bytes(payload)
        decompress = False
        if payload.startswith(b"."):
            payload = payload[1:]
            decompress = True
        try:
            json = base64_decode(payload)
        except Exception as e:
            raise BadPayload("Could not base64 decode the payload because of an exception",
                             original_error=e)
        if decompress:
            try:
                json = _zlib.decompress(json)
            except Exception as e:
                raise BadPayload("Could not zlib decompress the payload before decoding the"
                                 " payload", original_error=e)
        return Serializer.load_payload(self, json, *args, **kwargs)

    def dump_payload(self, obj):
        json = Serializer.dump_payload(self, obj)
        is_compressed = False
        compressed = _zlib.compress(json)
        if len(compressed) < (len(json) - 1):
            json = compressed
            is_compressed = True
        base64d = base64_encode(json)
        if is_compressed:
            base64d = b"." + base64d
        return base64d


class URLSafeSerializer(URLSafeSerializerMixin, Serializer):
    pass


class URLSafeTimedSerializer(URLSafeSerializerMixin, TimedSerializer):
    pass
