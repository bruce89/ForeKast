package dev.bruze.forekast.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ForeKastDao {
    @Query("SELECT c.*, (c.id = s.locationId) AS selected FROM locations c CROSS JOIN app_selection s WHERE s.singleton = 0 ORDER BY c.favoriteOrder, c.id")
    abstract fun library(): Flow<List<LibraryRow>>
    @Query("SELECT * FROM locations WHERE id = :id") abstract suspend fun city(id: String): CityEntity?
    @Query("SELECT locationId FROM app_selection WHERE singleton = 0") abstract suspend fun selected(): String?
    @Upsert abstract suspend fun upsertCity(city: CityEntity)
    @Upsert abstract suspend fun setSelection(selection: SelectionEntity)
    @Query("DELETE FROM locations WHERE favoriteOrder IS NULL AND id NOT IN (SELECT locationId FROM app_selection)") abstract suspend fun pruneCities()
    @Query("SELECT COUNT(*) FROM locations WHERE favoriteOrder IS NOT NULL") abstract suspend fun favoriteCount(): Int
    @Query("SELECT COALESCE(MAX(favoriteOrder), -1) + 1 FROM locations") abstract suspend fun nextFavoriteOrder(): Int
    @Transaction open suspend fun initialize(initial: CityEntity) {
        if (selected() == null) { upsertCity(initial); setSelection(SelectionEntity(locationId = initial.id)) }
        pruneCities()
    }
    @Query("DELETE FROM app_selection") abstract suspend fun deleteSelection()
    @Query("DELETE FROM locations") abstract suspend fun deleteCities()
    @Transaction open suspend fun clearCities() {
        deleteSelection()
        // Foreign keys cascade to forecasts and refresh attempts. Host cooldowns remain.
        deleteCities()
    }
    @Transaction open suspend fun select(city: CityEntity) {
        val previous = city(city.id)
        upsertCity(city.copy(favoriteOrder = previous?.favoriteOrder))
        setSelection(SelectionEntity(locationId = city.id))
        pruneCities()
    }
    @Transaction open suspend fun toggleFavorite(id: String) {
        val city = city(id) ?: return
        if (city.favoriteOrder != null) upsertCity(city.copy(favoriteOrder = null))
        else if (favoriteCount() < 5) upsertCity(city.copy(favoriteOrder = nextFavoriteOrder()))
        pruneCities()
    }

    @Transaction @Query("SELECT * FROM weather_snapshots WHERE locationId = :id") abstract fun observeSnapshot(id: String): Flow<StoredSnapshot?>
    @Transaction @Query("SELECT * FROM weather_snapshots WHERE locationId = :id") abstract suspend fun snapshot(id: String): StoredSnapshot?
    @Upsert abstract suspend fun upsertHeader(snapshot: SnapshotEntity)
    @Insert abstract suspend fun insertHours(hours: List<HourEntity>)
    @Insert abstract suspend fun insertDays(days: List<DayEntity>)
    @Query("DELETE FROM hourly_weather WHERE locationId = :id") abstract suspend fun deleteHours(id: String)
    @Query("DELETE FROM daily_weather WHERE locationId = :id") abstract suspend fun deleteDays(id: String)
    @Query("DELETE FROM weather_snapshots WHERE fetchedAt <= :cutoff") abstract suspend fun purgeExpired(cutoff: Long)
    @Transaction open suspend fun replace(snapshot: StoredSnapshot) {
        // A cancelled city selection must not resurrect an already-pruned city.
        check(city(snapshot.header.locationId) != null)
        upsertHeader(snapshot.header)
        deleteHours(snapshot.header.locationId); deleteDays(snapshot.header.locationId)
        insertHours(snapshot.hours); insertDays(snapshot.days)
    }
    @Query("SELECT * FROM refresh_policy WHERE locationId = :id") abstract suspend fun attempt(id: String): AttemptEntity?
    @Upsert abstract suspend fun recordAttempt(attempt: AttemptEntity)
    @Query("SELECT * FROM host_cooldowns WHERE host = :host") abstract suspend fun cooldown(host: String): CooldownEntity?
    @Upsert abstract suspend fun writeCooldown(cooldown: CooldownEntity)
}

@Database(entities = [CityEntity::class, SelectionEntity::class, SnapshotEntity::class, HourEntity::class,
    DayEntity::class, AttemptEntity::class, CooldownEntity::class], version = 1, exportSchema = true)
abstract class ForeKastDatabase : RoomDatabase() { abstract fun dao(): ForeKastDao }
