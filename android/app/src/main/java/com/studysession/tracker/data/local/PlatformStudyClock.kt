package com.studysession.tracker.data.local

import android.os.SystemClock
import com.studysession.tracker.domain.model.StudyClock

class PlatformStudyClock : StudyClock {
    override fun monotonicMillis(): Long = SystemClock.elapsedRealtime()
    override fun wallMillis(): Long = System.currentTimeMillis()
}
