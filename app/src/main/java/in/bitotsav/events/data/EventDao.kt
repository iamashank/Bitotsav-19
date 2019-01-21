package `in`.bitotsav.events.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EventDao {
    @Query("SELECT * FROM event")
    fun getAll(): LiveData<List<Event>>

    @Query("SELECT * FROM event WHERE id = :id")
    fun getById(id: Int): Event

    @Query("SELECT name FROM event WHERE id = :id")
    fun getEventName(id: Int): String

    @Query("SELECT isStarred FROM event WHERE id = :id")
    fun isStarred(id: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg events: Event)
}