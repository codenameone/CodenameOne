
using System.Threading.Tasks;
using com.codename1.db;
using SQLitePCL;

namespace com.codename1.impl
{
    class WinDBRawCursor : com.codename1.db.Cursor, com.codename1.db.Row
    {
        sqlite3_stmt stmt;
        private int pos=-1;

        public WinDBRawCursor(sqlite3_stmt stmt)
        {
            this.stmt = stmt;
        }

        public void close()
        {
            int result = raw.sqlite3_finalize(stmt);
            if (raw.SQLITE_OK != result)
            {
                throw new java.io.IOException("Failed to close sqlite cursor with result " + result);
            }
        }

        public bool first()
        {
            this.pos = -1;
            int result = raw.sqlite3_reset(stmt);
            if (result != raw.SQLITE_OK && result != raw.SQLITE_DONE && result != raw.SQLITE_ROW)
            {
                throw new java.io.IOException("SQLError in reset.  Code: " + result);
            }
            return true;
        }

        public int getColumnCount()
        {
            return raw.sqlite3_column_count(stmt);
        }

        public int getColumnIndex(string columnName)
        {
            columnName = columnName.ToLower();
            for (int iter = 0; true; iter++)
            {
                string n = getColumnName(iter);
                if (n == null)
                {
                    return -1;
                }
                if (n.ToLower().Equals(columnName))
                {
                    return iter;
                }
            }
        }

        public string getColumnName(int i)
        {
            return raw.sqlite3_column_name(stmt, i);
        }

        public int getPosition()
        {
            return pos;
        }

        public Row getRow()
        {
            return this;
        }

        public bool last()
        {
            throw new java.io.IOException("Cursor.last() Not supported");
        }

        public bool next()
        {
            pos++;
            int result = raw.sqlite3_step(stmt);
            if (result == raw.SQLITE_ROW)
            {
                return true;
            }
            if (result != raw.SQLITE_DONE && result != raw.SQLITE_OK)
            {
                throw new java.io.IOException("SQL Error in step Code: " + result);
            }
            return false;
        }

        public bool position(int i)
        {
            throw new java.io.IOException("Cursor.position unsupported");
        }

        public bool prev()
        {
            throw new java.io.IOException("Cursor.prev unsupported");
        }

        public int getInteger(int i)
        {
            return raw.sqlite3_column_int(stmt, i);
        }

        public string getString(int i)
        {
            return raw.sqlite3_column_text(stmt, i);
        }

        public double getDouble(int i)
        {
            return raw.sqlite3_column_double(stmt, i);
        }

        public byte[] getBlob(int i)
        {
            return raw.sqlite3_column_blob(stmt, i);
        }

        public float getFloat(int i)
        {
            return (float)raw.sqlite3_column_double(stmt, i);
        }

        public long getLong(int i)
        {
            return raw.sqlite3_column_int64(stmt, i);
        }

        public short getShort(int i)
        {
            return (short)raw.sqlite3_column_int(stmt, i);
        }
    }

    class WinDBRaw : com.codename1.db.Database 
    {

        sqlite3 db;
        static bool initialized;

        static void init()
        {
            if (!initialized)
            {
                initialized = true;
                SQLitePCL.Batteries.Init();
            }
        }

        public WinDBRaw(sqlite3 db)
        {
            this.db = db;
        }

        public static WinDBRaw openOrCreate(string path)
        {
            sqlite3 db;
            init();
            raw.sqlite3_open(path, out db);
            return new WinDBRaw(db);
        }

        public override void beginTransaction()
        {
            execute("BEGIN");
        }

        public override void close()
        {
            raw.sqlite3_close(db);
        }

        public override void commitTransaction()
        {
            execute("COMMIT");
        }

        public override void execute(string str)
        {
            string errMsg;
            int result = raw.sqlite3_exec(db, str, out errMsg);
            if (raw.SQLITE_OK != result && raw.SQLITE_DONE != result && raw.SQLITE_ROW != result)
            {
                throw new java.io.IOException(errMsg);
            }
        }

        public override void execute(string str, string[] args)
        {
            
            sqlite3_stmt stmt;
            int result = raw.sqlite3_prepare_v2(db, str, out stmt);
            try
            {
                if (raw.SQLITE_OK != result)
                {
                    throw new java.io.IOException("Failed to prepare SQL statement " + str + ".  Response code " + result);
                }

                if (args != null)
                {
                    int i = 1;
                    foreach (string arg in args)
                    {
                        result = raw.sqlite3_bind_text(stmt, i++, arg);
                        if (raw.SQLITE_OK != result)
                        {
                            throw new java.io.IOException("Failed to bind arg " + arg + " to SQL statement " + str + ".  Response code " + result);
                        }
                    }
                }
                result = raw.sqlite3_step(stmt);
                if (raw.SQLITE_OK != result && raw.SQLITE_DONE != result && raw.SQLITE_ROW != result)
                {
                    throw new java.io.IOException("Failed to execute SQL statement " + str + ".  Response code " + result);
                }
            }
            finally
            {

                result = raw.sqlite3_finalize(stmt);
                if (raw.SQLITE_OK != result)
                {
                    throw new java.io.IOException("Failed to finalize SQL statement " + str + ".  Response code " + result);
                }
            }
        }

        public override Cursor executeQuery(string str)
        {
            return executeQuery(str, null);
        }

        public override Cursor executeQuery(string str, string[] args)
        {
            sqlite3_stmt stmt;
            int result = raw.sqlite3_prepare_v2(db, str, out stmt);
            
            if (raw.SQLITE_OK != result)
            {
                throw new java.io.IOException("Failed to prepare SQL statement " + str + ".  Response code " + result);
            }

            if (args != null)
            {
                int i = 1;
                foreach (string arg in args)
                {
                    result = raw.sqlite3_bind_text(stmt, i++, arg);
                    if (raw.SQLITE_OK != result)
                    {
                        throw new java.io.IOException("Failed to bind arg " + arg + " to SQL statement " + str + ".  Response code " + result);
                    }
                }
            }

            return new WinDBRawCursor(stmt);

        }

        public override void rollbackTransaction()
        {
            execute("ROLLBACK");
        }
    }
}

