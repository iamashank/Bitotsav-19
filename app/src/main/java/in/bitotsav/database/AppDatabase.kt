package `in`.bitotsav.database

import `in`.bitotsav.events.data.Event
import `in`.bitotsav.events.data.EventDao
import `in`.bitotsav.feed.data.Feed
import `in`.bitotsav.feed.data.FeedDao
import `in`.bitotsav.profile.data.User
import `in`.bitotsav.profile.data.UserDao
import `in`.bitotsav.shared.data.MapConverter
import `in`.bitotsav.teams.championship.data.ChampionshipTeam
import `in`.bitotsav.teams.championship.data.ChampionshipTeamDao
import `in`.bitotsav.teams.nonchampionship.data.NonChampionshipTeam
import `in`.bitotsav.teams.nonchampionship.data.NonChampionshipTeamDao
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Event::class, Feed::class, ChampionshipTeam::class, NonChampionshipTeam::class, User::class],
    version = 2
)
@TypeConverters(MapConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    abstract fun championshipTeamDao(): ChampionshipTeamDao

    abstract fun nonChampionshipTeamDao(): NonChampionshipTeamDao

    abstract fun feedDao(): FeedDao

    abstract fun userDao(): UserDao
}