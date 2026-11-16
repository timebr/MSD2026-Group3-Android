package com.example.oweowe.sync

import com.example.oweowe.data.model.SyncStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncStatusStoreTest {

    @Test
    fun metadataFallsBackToPendingOrSynced() {
        val store = SyncStatusStore()
        assertEquals(SyncStatus.PENDING, store.statusFor("e1", metadataHasPendingWrites = true))
        assertEquals(SyncStatus.SYNCED, store.statusFor("e1", metadataHasPendingWrites = false))
    }

    @Test
    fun failedStatusOverridesMetadata() {
        val store = SyncStatusStore()
        store.markPending("e1", "Dinner")
        store.markFailed("e1")
        assertEquals(SyncStatus.FAILED, store.statusFor("e1", metadataHasPendingWrites = false))
    }

    @Test
    fun pendingToSyncedEmitsOneBackgroundSyncEvent() = runTest {
        val store = SyncStatusStore()
        val events = mutableListOf<SyncEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.events.collect { events += it }
        }

        store.markPending("e1", "Dinner")
        store.markSynced("e1")
        collector.cancel()

        assertEquals(1, events.size)
        assertEquals(SyncEvent("e1", "Dinner", SyncStatus.SYNCED), events.single())
    }

    @Test
    fun failedThenSyncedAlsoEmits() = runTest {
        val store = SyncStatusStore()
        val events = mutableListOf<SyncEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.events.collect { events += it }
        }

        store.markPending("e1", "Dinner")
        store.markFailed("e1")
        store.markSynced("e1")
        collector.cancel()

        assertEquals(1, events.size)
        assertTrue(events.single().status == SyncStatus.SYNCED)
    }
}
