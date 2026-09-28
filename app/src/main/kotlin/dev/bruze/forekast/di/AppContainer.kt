package dev.bruze.forekast.di

import android.content.Context
import androidx.room.Room
import dev.bruze.forekast.BuildConfig
import dev.bruze.forekast.core.ports.*
import dev.bruze.forekast.data.demo.*
import dev.bruze.forekast.data.remote.*
import dev.bruze.forekast.data.local.*
import java.time.Clock

class AppContainer(context: Context) {
    val clock: Clock = Clock.systemUTC()
    val isDemo = BuildConfig.DEMO
    private val fake = FakeWeatherRepository(clock)
    private val database by lazy { Room.databaseBuilder(context.applicationContext, ForeKastDatabase::class.java, "forekast.db").build() }
    private val api by lazy { OpenMeteoClient(weatherHttpClient(), clock, RoomCooldownStore(database.dao())) }
    val locations: LocationDirectory = if (isDemo) DemoLocationDirectory else RemoteLocationDirectory(api)
    val weatherRepository: WeatherRepository = if (isDemo) fake else PersistentWeatherRepository(api, database.dao(), clock)
    val library: LibraryRepository? = if (isDemo) null else RoomLibraryRepository(database.dao())
    val preferences: PreferencesRepository? = if (isDemo) null else DataStorePreferences(context.applicationContext.forekastPreferences)
    val demoController: DemoController = if (isDemo) fake else object : DemoController {
        override fun prepare(locationId: String, scenario: DemoScenario) = Unit
    }
}
