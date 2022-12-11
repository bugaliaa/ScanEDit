package com.example.editeditscanner.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.editeditscanner.data.Frame

@Dao
interface FrameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(frame: Frame): Long

    @Update
    fun update(frame: Frame)

    @Delete
    fun delete(frame: Frame)

    @Query("SELECT * FROM Frame WHERE docId=:docId ORDER BY `index`")
    fun getFrames(docId: String): LiveData<MutableList<Frame>>

    @Query("SELECT * FROM Frame WHERE id=:id")
    fun getFrame(id: Long): LiveData<Frame>

    @Query("SELECT * FROM Frame WHERE docId=:docId ORDER BY `index`")
    suspend fun getFramesSync(docId: String): MutableList<Frame>

    @Query("SELECT COUNT(id) FROM Frame WHERE docId=:docId")
    fun getFrameCount(docId: String): LiveData<Int>

    @Query("SELECT uri FROM Frame WHERE docId=:docId AND `index`=0")
    fun getFrameUri(docId: String): LiveData<String>
}