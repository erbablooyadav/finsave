package com.finsave.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Central registry for all Room database migrations.
 * Every schema change must have a corresponding Migration object here.
 */
object Migrations {

    /**
     * Baseline migration for v1. No-op since v1 is the start.
     */
    val MIGRATION_1_1 = object : Migration(1, 1) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Baseline — no changes
        }
    }

    /**
     * Migration 1 to 2: Add Transaction FTS support.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create the FTS virtual table
            // Note: Room uses FTS4 for compatibility with older SQLite versions and SQLCipher
            database.execSQL("""
                CREATE VIRTUAL TABLE IF NOT EXISTS `transactions_fts` 
                USING fts4(content=`transactions`, `merchant_name`, `note`)
            """)
            
            // Populate the FTS table with existing data
            database.execSQL("INSERT INTO transactions_fts(transactions_fts) VALUES('rebuild')")
        }
    }

    /**
     * Migration 2 to 3: Add local analytics support.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `analytics_events` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `event_name` TEXT NOT NULL, 
                    `properties_json` TEXT, 
                    `timestamp` INTEGER NOT NULL
                )
            """)
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_analytics_events_timestamp` ON `analytics_events` (`timestamp`)")
        }
    }

    /**
     * All migrations list for use in Room builder.
     */
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_1,
        MIGRATION_1_2,
        MIGRATION_2_3
    )
}
