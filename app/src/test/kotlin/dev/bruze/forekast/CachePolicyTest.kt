package dev.bruze.forekast

import dev.bruze.forekast.core.model.CachePolicy
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class CachePolicyTest {
    private val fetched = Instant.parse("2026-09-25T12:00:00Z")
    @Test fun automaticRefreshStartsAtThirtyMinutes() {
        assertFalse(CachePolicy.shouldRefresh(fetched, fetched.plusSeconds(1799)))
        assertTrue(CachePolicy.shouldRefresh(fetched, fetched.plusSeconds(1800)))
        assertTrue(CachePolicy.shouldRefresh(null, fetched))
    }
    @Test fun staleStartsAtSixHoursAndExpiryAtSevenDays() {
        assertFalse(CachePolicy.stale(fetched, fetched.plusSeconds(21599)))
        assertTrue(CachePolicy.stale(fetched, fetched.plusSeconds(21600)))
        assertFalse(CachePolicy.expired(fetched, fetched.plusSeconds(604799)))
        assertTrue(CachePolicy.expired(fetched, fetched.plusSeconds(604800)))
    }
    @Test fun backwardsClockRequestsCorrectionWithoutCallingDataFresh() {
        assertTrue(CachePolicy.shouldRefresh(fetched, fetched.minusSeconds(1)))
        assertTrue(CachePolicy.stale(fetched, fetched.minusSeconds(1)))
        assertFalse(CachePolicy.expired(fetched, fetched.minusSeconds(1)))
    }
    @Test fun attemptsRespectSixtySecondsAndRecoverFromClockRollback() {
        assertEquals(fetched.plusSeconds(60), CachePolicy.nextAttempt(fetched, fetched.plusSeconds(59)))
        assertNull(CachePolicy.nextAttempt(fetched, fetched.plusSeconds(60)))
        assertNull(CachePolicy.nextAttempt(fetched, fetched.minusSeconds(1)))
    }
}
