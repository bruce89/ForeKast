package dev.bruze.forekast

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.bruze.forekast.data.remote.*
import kotlinx.coroutines.runBlocking
import java.time.Clock
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith

/** Explicitly opt-in: never spends public API quota in the regular suite. */
@RunWith(AndroidJUnit4::class)
class LiveWeatherSmokeTest {
    @Test fun searchesAndMapsRealHttpsResponses() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveWeather") == "true")
        weatherHttpClient().use { http ->
            val api = OpenMeteoClient(http, Clock.systemUTC())
            val directory = RemoteLocationDirectory(api)
            val city = directory.search("Lisboa").first()
            val (forecast, daily) = api.forecast(city)
            val snapshot = WeatherMapper.map(city.id, forecast, daily, Clock.systemUTC().instant())
            assertTrue(snapshot.hours.isNotEmpty())
            assertEquals(7, snapshot.days.size)
            assertNotNull(snapshot.current)
        }
    }
}
