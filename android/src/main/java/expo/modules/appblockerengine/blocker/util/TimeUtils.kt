package expo.modules.appblockerengine.blocker.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TimeUtils {
    
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    fun parseTime(timeString: String): Pair<Int, Int>? {
        return try {
            val parts = timeString.split(":")
            if (parts.size != 2) return null
            
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null
            }
            
            Pair(hour, minute)
        } catch (e: Exception) {
            null
        }
    }
    
    fun isTimeReached(scheduledTime: String): Boolean {
        val parsed = parseTime(scheduledTime) ?: return false
        
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        return currentHour > parsed.first || 
               (currentHour == parsed.first && currentMinute >= parsed.second)
    }
    
    fun getCurrentTimeFormatted(): String {
        return timeFormat.format(System.currentTimeMillis())
    }
    
    fun getNextScheduleTime(scheduledTime: String): Long {
        val parsed = parseTime(scheduledTime) ?: return System.currentTimeMillis()
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, parsed.first)
        calendar.set(Calendar.MINUTE, parsed.second)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return calendar.timeInMillis
    }
}
