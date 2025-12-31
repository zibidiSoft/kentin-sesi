package io.github.thwisse.kentinsesi.util

import android.content.Context
import com.google.firebase.Timestamp
import io.github.thwisse.kentinsesi.R
import java.util.concurrent.TimeUnit

object TimeUtil {
    
    /**
     * Firebase Timestamp'ı "X zaman önce" formatına çevirir
     * 
     * @param timestamp Firebase Timestamp (null olabilir)
     * @param context String resource'ları almak için gerekli
     * @return Formatlanmış zaman metni, null ise boş string
     */
    fun getRelativeTime(timestamp: Timestamp?, context: Context): String {
        if (timestamp == null) return ""
        
        val now = System.currentTimeMillis()
        val postTime = timestamp.toDate().time
        val diffMillis = now - postTime
        
        // Negatif zaman farkı (gelecekteki tarih) durumunda boş string döndür
        if (diffMillis < 0) return ""
        
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
        val days = TimeUnit.MILLISECONDS.toDays(diffMillis)
        
        return when {
            // 1-10 dakika: "Az önce"
            minutes <= 10 -> context.getString(R.string.time_just_now)
            
            // 10-59 dakika: "X dakika önce"
            minutes < 60 -> context.getString(R.string.time_minutes_ago, minutes.toInt())
            
            // 1-23 saat: "X saat önce"
            hours < 24 -> context.getString(R.string.time_hours_ago, hours.toInt())
            
            // 1-6 gün: "X gün önce"
            days < 7 -> context.getString(R.string.time_days_ago, days.toInt())
            
            // 1-4 hafta: "X hafta önce"
            days < 30 -> {
                val weeks = days / 7
                context.getString(R.string.time_weeks_ago, weeks.toInt())
            }
            
            // 1-11 ay: "X ay önce"
            days < 365 -> {
                val months = days / 30
                context.getString(R.string.time_months_ago, months.toInt())
            }
            
            // 12+ ay: "X yıl önce"
            else -> {
                val years = days / 365
                context.getString(R.string.time_years_ago, years.toInt())
            }
        }
    }
}
