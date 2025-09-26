package com.example.warehouseCounter.data.room.database

import android.content.Context
import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.warehouseCounter.data.room.dao.orderRequest.LogDao
import com.example.warehouseCounter.data.room.entity.orderRequest.Log
import com.github.javafaker.Faker
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogDaoTest {
    private lateinit var logDao: LogDao
    private lateinit var database: WcTempDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        database = inMemoryDatabaseBuilder(context, WcTempDatabase::class.java)
            // Allowing the main thread queries, just for testing.
            .allowMainThreadQueries().build()
        logDao = database.logDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun inserting_an_log_should_return_a_total_of_one_log() = runTest {
        val fakeLog = createFakeLog()
        logDao.insert(fakeLog)
        assert(logDao.count() == 1)
    }

    @Test
    fun inserting_an_log_should_return_a_equal_log_id() = runTest {
        val fakeLog = createFakeLog()
        val id = logDao.insert(fakeLog)
        assert(fakeLog.id == id)
    }

    @Test
    fun inserting_an_log_should_return_a_equal_log() = runTest {
        val fakeLog = createFakeLog()
        logDao.insert(fakeLog)
        val log = logDao.getByOrderId(fakeLog.orderRequestId).firstOrNull()
        assert(fakeLog == log)
    }

    @Test
    fun inserting_an_log_should_return_a_same_hashcode_log() = runTest {
        val fakeLog = createFakeLog()
        logDao.insert(fakeLog)
        val log = logDao.getByOrderId(fakeLog.orderRequestId).firstOrNull()
        assert(fakeLog.hashCode() == log!!.hashCode())
    }

    companion object {
        fun createFakeLog(): Log {
            val faker = Faker()

            return Log(
                id = faker.number().randomNumber(),
                orderRequestId = faker.number().randomNumber(),
                clientId = faker.number().randomNumber(),
                userId = faker.number().randomNumber(),
                itemId = faker.number().randomNumber(),
                itemDescription = faker.lorem().sentence(),
                itemCode = faker.lorem().word(),
                scannedCode = faker.lorem().word(),
                variationQty = if (faker.random().nextBoolean()) faker.number().randomDouble(2, 1, 100) else null,
                finalQty = if (faker.random().nextBoolean()) faker.number().randomDouble(2, 1, 100) else null,
                date = if (faker.random().nextBoolean()) faker.date().birthday().toString() else null
            )
        }
    }
}
