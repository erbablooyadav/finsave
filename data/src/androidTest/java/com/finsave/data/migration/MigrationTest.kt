package com.finsave.data.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.finsave.data.local.FinSaveDatabase
import com.finsave.data.local.migration.Migrations
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FinSaveDatabase::class.java,
        emptyList(), // FrameworkSQLiteOpenHelperFactory is the default
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        // Create earliest version of the database.
        var db = helper.createDatabase(TEST_DB, 1)

        // Database has schema version 1. Insert some data.
        db.execSQL("INSERT INTO transactions (amount_paise, type, account_id, merchant_name, note, date, created_at) " +
                "VALUES (1000, 'DEBIT', 1, 'Zomato', 'Lunch', 1672531200000, 1672531200000)")

        // Prepare for the next version.
        db.close()

        // Re-open the database with version 2 and provide MIGRATION_1_2.
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, Migrations.MIGRATION_1_2)

        // Migration 1 to 2 adds transactions_fts. Verify it exists and has data.
        val cursor = db.query("SELECT * FROM transactions_fts WHERE merchant_name MATCH 'Zomato*'")
        assert(cursor.moveToFirst())
        assert(cursor.getString(cursor.getColumnIndexOrThrow("merchant_name")) == "Zomato")
        cursor.close()
    }
}
