"""SQLAlchemy exception hierarchy (the subset the Codename One SQLAlchemy provides)."""


class SQLAlchemyError(Exception):
    pass


class ArgumentError(SQLAlchemyError):
    pass


class CompileError(SQLAlchemyError):
    pass


class NoSuchModuleError(ArgumentError):
    pass


class InvalidRequestError(SQLAlchemyError):
    pass


class NoResultFound(InvalidRequestError):
    pass


class MultipleResultsFound(InvalidRequestError):
    pass


class NoSuchTableError(InvalidRequestError):
    pass


class StatementError(SQLAlchemyError):
    def __init__(self, message, statement=None, params=None, orig=None):
        SQLAlchemyError.__init__(self, message)
        self.statement = statement
        self.params = params
        self.orig = orig


class DBAPIError(StatementError):
    pass


class InterfaceError(DBAPIError):
    pass


class DatabaseError(DBAPIError):
    pass


class DataError(DatabaseError):
    pass


class OperationalError(DatabaseError):
    pass


class IntegrityError(DatabaseError):
    pass


class InternalError(DatabaseError):
    pass


class ProgrammingError(DatabaseError):
    pass


class NotSupportedError(DatabaseError):
    pass


_BY_NAME = {
    "InterfaceError": InterfaceError, "DatabaseError": DatabaseError, "DataError": DataError,
    "OperationalError": OperationalError, "IntegrityError": IntegrityError,
    "InternalError": InternalError, "ProgrammingError": ProgrammingError,
    "NotSupportedError": NotSupportedError,
}


def _wrap(e, statement, params):
    """Wraps a DB-API exception the way SQLAlchemy does, keeping the original as ``orig``."""
    cls = _BY_NAME.get(type(e).__name__, DBAPIError)
    msg = "(sqlite3.%s) %s\n[SQL: %s]" % (type(e).__name__, e, statement)
    return cls(msg, statement, params, e)
