package dev.bruze.forekast.data.local

import androidx.room.*

@Entity(tableName = "locations")
data class CityEntity(@PrimaryKey val id: String, val name: String, val region: String, val country: String,
    val zone: String, val latitude: Double?, val longitude: Double?, val favoriteOrder: Int? = null)
@Entity(tableName = "app_selection", foreignKeys = [ForeignKey(entity = CityEntity::class, parentColumns = ["id"], childColumns = ["locationId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("locationId")])
data class SelectionEntity(@PrimaryKey val singleton: Int = 0, val locationId: String)
data class LibraryRow(@Embedded val city: CityEntity, val selected: Boolean)

data class CurrentEntity(val time: Long, val temperature: Double?, val feelsLike: Double?, val humidity: Int?,
    val wind: Double?, val condition: String, val code: Int?, val isDay: Boolean?)
@Entity(tableName = "weather_snapshots", foreignKeys = [ForeignKey(entity = CityEntity::class, parentColumns = ["id"], childColumns = ["locationId"], onDelete = ForeignKey.CASCADE)])
data class SnapshotEntity(@PrimaryKey val locationId: String, val fetchedAt: Long, val zone: String?,
    @Embedded(prefix = "current_") val current: CurrentEntity?)
@Entity(tableName = "hourly_weather", primaryKeys = ["locationId", "time"], foreignKeys = [ForeignKey(entity = SnapshotEntity::class, parentColumns = ["locationId"], childColumns = ["locationId"], onDelete = ForeignKey.CASCADE)])
data class HourEntity(val locationId: String, val time: Long, val temperature: Double?, val precipitation: Int?, val condition: String, val code: Int?)
@Entity(tableName = "daily_weather", primaryKeys = ["locationId", "date"], foreignKeys = [ForeignKey(entity = SnapshotEntity::class, parentColumns = ["locationId"], childColumns = ["locationId"], onDelete = ForeignKey.CASCADE)])
data class DayEntity(val locationId: String, val date: String, val low: Double?, val high: Double?, val precipitation: Int?, val condition: String, val code: Int?)
data class StoredSnapshot(@Embedded val header: SnapshotEntity,
    @Relation(parentColumn = "locationId", entityColumn = "locationId") val hours: List<HourEntity>,
    @Relation(parentColumn = "locationId", entityColumn = "locationId") val days: List<DayEntity>)
@Entity(tableName = "refresh_policy", foreignKeys = [ForeignKey(entity = CityEntity::class, parentColumns = ["id"], childColumns = ["locationId"], onDelete = ForeignKey.CASCADE)])
data class AttemptEntity(@PrimaryKey val locationId: String, val attemptedAt: Long)
@Entity(tableName = "host_cooldowns")
data class CooldownEntity(@PrimaryKey val host: String, val until: Long)
