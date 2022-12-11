package com.example.editeditscanner.utils

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.example.editeditscanner.dao.DocumentDao
import com.example.editeditscanner.dao.FrameDao
import com.example.editeditscanner.data.Document
import com.example.editeditscanner.data.Frame

@Database(entities = [Document::class, Frame::class], version = 5, exportSchema = false)
abstract class MyDatabase : RoomDatabase() {

    abstract fun frameDao(): FrameDao
    abstract fun documentDao(): DocumentDao

    companion object {
        private var INSTANCE: MyDatabase? = null

        fun getDatabase(context: Context) : MyDatabase? {
            synchronized(Database::class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        MyDatabase::class.java,
                        "database"
                    ).fallbackToDestructiveMigration().build()
                }
            }
            return INSTANCE
        }
    }
}