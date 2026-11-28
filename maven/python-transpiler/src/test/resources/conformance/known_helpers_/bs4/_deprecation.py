# The shape of bs4's own helpers: Codename One compiles their uses as what
# they mean (KnownHelpers), and this program checks that matches CPython.
import functools
import warnings


def _deprecated_alias(old_name, new_name, version):
    @property
    def alias(self):
        warnings.warn(f"Access to deprecated property {old_name}. (Replaced by {new_name}) -- Deprecated since version {version}.", DeprecationWarning, stacklevel=2)
        return getattr(self, new_name)

    @alias.setter
    def alias(self, value):
        warnings.warn(f"Write to deprecated property {old_name}. (Replaced by {new_name}) -- Deprecated since version {version}.", DeprecationWarning, stacklevel=2)
        return setattr(self, new_name, value)

    return alias


def _deprecated_function_alias(old_name, new_name, version):
    def alias(self, *args, **kwargs):
        warnings.warn(f"Call to deprecated method {old_name}. (Replaced by {new_name}) -- Deprecated since version {version}.", DeprecationWarning, stacklevel=2)
        return getattr(self, new_name)(*args, **kwargs)

    return alias


def _deprecated(replaced_by, version):
    def deprecate(func):
        @functools.wraps(func)
        def with_warning(*args, **kwargs):
            warnings.warn(f"Call to deprecated method {func.__name__}. (Replaced by {replaced_by}) -- Deprecated since version {version}.", DeprecationWarning, stacklevel=2)
            return func(*args, **kwargs)

        return with_warning

    return deprecate
