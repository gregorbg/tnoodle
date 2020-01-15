package org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model

import java.time.ZoneId
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.WCIFParser.parseWCIFDateWithTimezone

data class Activity(val activityCode: String, val startTime: String, val childActivities: List<Activity> = emptyList()) {
    val nestedChildActivities: List<Activity>
        get() = childActivities.flatMap { it.nestedChildActivities }

    val finestChildActivities
        get() = nestedChildActivities.takeUnless { it.isEmpty() }
            ?: listOf(this)

    fun getLocalStartTime(timeZone: ZoneId) = startTime.parseWCIFDateWithTimezone(timeZone)
}