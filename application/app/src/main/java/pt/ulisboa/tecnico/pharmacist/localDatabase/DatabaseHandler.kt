package pt.ulisboa.tecnico.pharmacist.localDatabase

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHandler(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "pharmacistCache.db"
        private const val DATABASE_VERSION = 3
    }

    override fun onCreate(db: SQLiteDatabase) {

        val PHARMACIES = """
            CREATE TABLE pharmacies (
            pharmacy_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            address TEXT NOT NULL,
            latitude REAL NOT NULL,
            longitude REAL NOT NULL,
            timestamp INT NOT NULL
        );
        """.trimIndent()

        val FAVORITE_PHARMACIES = """
            CREATE TABLE favorite_pharmacies (
                favorite_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                user_id INTEGER NOT NULL,
                pharmacy_id INTEGER NOT NULL,
                timestamp INT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
                FOREIGN KEY (pharmacy_id) REFERENCES pharmacies(pharmacy_id) ON DELETE CASCADE ON UPDATE CASCADE
            );
        """.trimIndent()

        val MEDICINE = """
            CREATE TABLE medicine (
                medicine_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                purpose TEXT NOT NULL,
                timestamp INT NOT NULL
            );
        """.trimIndent()


        val MEDICINE_STOCK = """
            CREATE TABLE medicine_stock (
                medicine_stock_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                pharmacy_id INTEGER NOT NULL,
                medicine_id INTEGER NOT NULL,
                quantity INTEGER NOT NULL,
                timestamp INT NOT NULL,
                FOREIGN KEY (pharmacy_id) REFERENCES pharmacies(pharmacy_id) ON DELETE CASCADE ON UPDATE CASCADE,
                FOREIGN KEY (medicine_id) REFERENCES medicine(medicine_id) ON DELETE CASCADE ON UPDATE CASCADE,
                UNIQUE (pharmacy_id, medicine_id)
            );
        """.trimIndent()
        db.execSQL(PHARMACIES)
        db.execSQL(FAVORITE_PHARMACIES)
        db.execSQL(MEDICINE_STOCK)
        db.execSQL(MEDICINE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

        onCreate(db)
    }

    fun dropAllTables(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS pharmacies")
        db.execSQL("DROP TABLE IF EXISTS favorite_pharmacies")
        db.execSQL("DROP TABLE IF EXISTS medicine")
        db.execSQL("DROP TABLE IF EXISTS medicine_stock")
    }
}