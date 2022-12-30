package com.dacosys.warehouseCounter.dataBaseHelper

import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics.Companion.DATABASE_NAME
import com.dacosys.warehouseCounter.Statics.Companion.DATABASE_VERSION
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.client.dbHelper.ClientDbHelper
import com.dacosys.warehouseCounter.errorLog.ErrorLog
import com.dacosys.warehouseCounter.item.dbHelper.ItemDbHelper
import com.dacosys.warehouseCounter.itemCategory.dbHelper.ItemCategoryDbHelper
import com.dacosys.warehouseCounter.itemCode.dbHelper.ItemCodeDbHelper
import com.dacosys.warehouseCounter.itemRegex.dbHelper.ItemRegexDbHelper
import com.dacosys.warehouseCounter.lot.dbHelper.LotDbHelper
import com.dacosys.warehouseCounter.user.dbHelper.UserDbHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


open class DataBaseHelper : SQLiteOpenHelper(
    context(),
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    @Synchronized
    override fun close() {
        if (myDataBase != null)
            myDataBase!!.close()

        super.close()
    }

    override fun onCreate(db: SQLiteDatabase) {
        createTables(db)

        // Limpiamos la instancia estática de la base de datos para
        // forzar que una nueva se cree la próxima vez.
        cleanInstance()
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // TODO Auto-generated method stub
    }

    companion object {
        private var myDataBase: SQLiteDatabase? = null

        //////////////////////////////////////////////////////////////
        /////////////// INSTANCIA ESTÁTICA (SINGLETON) ///////////////
        private var sInstance: DataBaseHelper? = null

        fun beginDataBase() {
            try {
                createDb()
            } catch (ioe: IOException) {
                throw Error(context().getString(R.string.unable_to_create_database))
            }
        }

        fun getReadableDb(): SQLiteDatabase {
            return getInstance()!!.readableDatabase
        }

        fun getWritableDb(): SQLiteDatabase {
            return getInstance()!!.writableDatabase
        }

        private fun cleanInstance() {
            sInstance = null
        }

        private fun createDb() {
            createDataBase()
        }

        @Synchronized
        private fun getInstance(): DataBaseHelper? {
            // Use the application context, which will ensure that you
            // don't accidentally leak an Activity's context.
            // See this article for more information: http://bit.ly/6LRzfx
            if (sInstance == null) {
                sInstance = DataBaseHelper()
            }
            return sInstance
        }

        fun createTables(db: SQLiteDatabase) {
            db.beginTransaction()
            try {
                for (sql in allCommands) {
                    println("$sql;")
                    db.execSQL(sql)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

        private val allCommands: ArrayList<String>
            get() {
                val c: ArrayList<String> = ArrayList()

                c.add(UserDbHelper.CREATE_TABLE)
                c.add(ItemDbHelper.CREATE_TABLE)
                c.add(ItemCodeDbHelper.CREATE_TABLE)
                c.add(ItemCategoryDbHelper.CREATE_TABLE)
                c.add(ClientDbHelper.CREATE_TABLE)
                c.add(LotDbHelper.CREATE_TABLE)
                c.add(ItemRegexDbHelper.CREATE_TABLE)

                for (sql in UserDbHelper.CREATE_INDEX) {
                    c.add(sql)
                }
                for (sql in ItemDbHelper.CREATE_INDEX) {
                    c.add(sql)
                }
                for (sql in ItemCodeDbHelper.CREATE_INDEX) {
                    c.add(sql)
                }
                for (sql in ItemCategoryDbHelper.CREATE_INDEX) {
                    c.add(sql)
                }
                for (sql in ClientDbHelper.CREATE_INDEX) {
                    c.add(sql)
                }
                for (sql in LotDbHelper.CREATE_INDEX) {
                    c.add(sql)
                }
                for (sql in ItemRegexDbHelper.CREATE_INDEX) {
                    c.add(sql)
                }
                return c
            }

        /**
         * Creates a empty database on the system and rewrites it with your own database.
         */
        @Throws(IOException::class)
        fun createDataBase() {
            val dbExist = checkDataBase()
            if (!dbExist) {
                // Llamando a este método se creará un base de datos según el modelo en
                // la carpeta determinada del sistama para nuestra aplicación.
                StaticDbHelper.getReadableDb()
            }
        }

        /**
         * Check if the database already exist to avoid re-copying the file each time you open the application.
         *
         * @return true if it exists, false if it doesn't
         */
        private fun checkDataBase(): Boolean {
            try {
                openDataBase()
            } catch (e: SQLiteException) {
                Log.e(
                    this::class.java.simpleName,
                    context()
                        .getString(R.string.database_is_not_created_yet)
                )
            }
            return myDataBase != null
        }

        fun copyDbToDocuments(): Boolean {
            try {
                val dbFile =
                    File(context().getDatabasePath(DATABASE_NAME).toString())

                //Open your local db as the input stream
                val myInput = FileInputStream(dbFile)

                // Path to the just created empty db
                val outDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                if (!outDir.exists()) {
                    outDir.mkdir()
                }

                val outFile = File(outDir, DATABASE_NAME)
                if (outFile.exists()) {
                    outFile.delete()
                }

                outFile.createNewFile()

                //Open the empty db as the output stream
                val myOutput = FileOutputStream(outFile)

                //transfer bytes from the inputfile to the outputfile
                val buffer = ByteArray(1024)
                var length: Int
                while (run {
                        length = myInput.read(buffer)
                        length
                    } > 0) {
                    myOutput.write(buffer, 0, length)
                }

                //Close the streams
                myOutput.flush()
                myOutput.close()
                myInput.close()

                return true
            } catch (e: IOException) {
                e.printStackTrace()
                ErrorLog.writeLog(null, this::class.java.simpleName, "DB write failed: $e")
                return false
            }
        }

        /**
         * Copies your database from your local assets-folder to the just created empty database in the
         * system folder, from where it can be accessed and handled.
         * This is done by transfering bytestream.
         */
        @Throws(IOException::class)
        private fun copyDataBase() {
            //Open your local db as the input stream
            val myInput = context().assets.open(DATABASE_NAME)

            // Path to the just created empty db
            val outFileName = context().getDatabasePath(DATABASE_NAME).toString()

            try {
                //Open the empty db as the output stream
                val myOutput = FileOutputStream(outFileName)

                //transfer bytes from the inputfile to the outputfile
                val buffer = ByteArray(1024)
                var length: Int
                while (run {
                        length = myInput.read(buffer)
                        length
                    } > 0) {
                    myOutput.write(buffer, 0, length)
                }

                //Close the streams
                myOutput.flush()
                myOutput.close()
                myInput.close()
            } catch (e: IOException) {
                e.printStackTrace()
                ErrorLog.writeLog(null, this::class.java.simpleName, "DB write failed: $e")
            }
        }

        @Throws(SQLException::class)
        fun openDataBase() {
            //Open the database
            val myPath = context().getDatabasePath(DATABASE_NAME).toString()
            myDataBase = SQLiteDatabase.openDatabase(
                myPath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )
        }

        fun copyDataBase(destinationDbFile: File?): Boolean {
            if (destinationDbFile == null) return false

            Log.d(
                this::class.java.simpleName,
                context().getString(R.string.copying_database)
            )

            //Open your local db as the input stream
            val myInput = FileInputStream(destinationDbFile)

            // Path to the just created empty db
            val outFileName =
                context().getDatabasePath(DATABASE_NAME)
                    .toString()

            DataBaseHelper().close()
            SQLiteDatabase.releaseMemory()

            // Limpiamos la instancia singleton de la DB.
            cleanInstance()

            val file = File(outFileName)
            if (file.exists()) {
                Log.d(this::class.java.simpleName, "Eliminando base de datos antigua: $outFileName")
                SQLiteDatabase.deleteDatabase(file)
            }

            Log.d(
                this::class.java.simpleName,
                "${
                    context().getString(R.string.origin)
                }: ${destinationDbFile.absolutePath}"
            )
            Log.d(
                this::class.java.simpleName,
                "${
                    context().getString(R.string.destination)
                }: $outFileName"
            )

            try {
                //Open the empty db as the output stream
                val myOutput = FileOutputStream(outFileName)

                //transfer bytes from the inputfile to the outputfile
                val buffer = ByteArray(1024)
                var length: Int
                while (run {
                        length = myInput.read(buffer)
                        length
                    } > 0) {
                    myOutput.write(buffer, 0, length)
                }

                //Close the streams
                myOutput.flush()
                myOutput.close()
                myInput.close()
            } catch (e: IOException) {
                ErrorLog.writeLog(
                    null,
                    this::class.java.simpleName,
                    "${
                        context().getString(R.string.exception_error)
                    } (Copy database): ${e.message}"
                )
                return false
            }

            Log.d(this::class.java.simpleName, context().getString(R.string.copy_ok))
            return true
        }
    }
}