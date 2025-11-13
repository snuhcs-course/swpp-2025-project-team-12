package com.example.dailyinsight.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CachedHistory::class,         // 차트 1년치
        BriefingCardCache::class,     // 브리핑 리스트
        OverviewCache::class          // 상세 표
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(
    StockHistoryConverter::class,
    BriefingListConverter::class,
    OverviewConverter::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historyCacheDao(): HistoryCacheDao
    abstract fun briefingDao(): BriefingDao
    abstract fun overviewDao(): OverviewDao

    companion object {
        // @Volatile: 이 변수가 모든 스레드에 즉시 공유되도록 보장
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // INSTANCE가 null이면 새로 생성하고, 아니면 기존 인스턴스 반환
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daily_insight_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}