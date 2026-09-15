package expo.modules.appblockerengine.blocker.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtils {
    
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
        isLenient = false
    }
    
    private val dateTimeRegex = Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")
    
    private val timeRegex = Regex("\\d{2}:\\d{2}")
    
    fun parseDateTimeToMillis(dateTimeString: String): Long? {
        if (!dateTimeRegex.matches(dateTimeString)) return null
        
        return try {
            val millis = dateTimeFormat.parse(dateTimeString)?.time ?: return null
            millis
        } catch (e: Exception) {
            null
        }
    }
    
    fun formatMillisToDateTime(millis: Long): String {
        return dateTimeFormat.format(Date(millis))
    }
    
    fun getNextDateTimeForTime(timeString: String): String? {
        if (!timeRegex.matches(timeString)) return null
        
        return try {
            val parts = timeString.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            
            if (hour > 23 || minute > 59) return null
            
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            formatMillisToDateTime(calendar.timeInMillis)
        } catch (e: Exception) {
            null
        }
    }
}