package com.example.warehouseCounter.data.room.database

import android.content.Context
import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.warehouseCounter.data.room.dao.lot.LotDao
import com.github.javafaker.Faker
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.warehouseCounter.data.room.entity.lot.Lot as LotRoom

@RunWith(AndroidJUnit4::class)
class LotDaoTest {
    private lateinit var lotDao: LotDao
    private lateinit var database: WcDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        database = inMemoryDatabaseBuilder(context, WcDatabase::class.java)
            // Allowing the main thread queries, just for testing.
            .allowMainThreadQueries().build()
        lotDao = database.lotDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun inserting_an_lot_should_return_a_total_of_one_lot() = runTest {
        val fakeLot = createFakeLot()
        lotDao.insert(fakeLot)
        assert(lotDao.count() == 1)
    }

    @Test
    fun inserting_an_lot_should_return_a_equal_lot_id() = runTest {
        val fakeLot = createFakeLot()
        val id = lotDao.insert(fakeLot)
        assert(fakeLot.lotId == id)
    }

    @Test
    fun inserting_an_lot_should_return_a_equal_lot() = runTest {
        val fakeLot = createFakeLot()
        val id = lotDao.insert(fakeLot)
        val lot = lotDao.getByLotId(id!!)
        assert(fakeLot == lot)
    }

    @Test
    fun inserting_an_lot_should_return_a_same_hashcode_lot() = runTest {
        val fakeLot = createFakeLot()
        val id = lotDao.insert(fakeLot)
        val lot = lotDao.getByLotId(id!!)
        assert(fakeLot.hashCode() == lot!!.hashCode())
    }

    companion object {
        fun createFakeLot(): LotRoom {
            val faker = Faker()

            return LotRoom(
                lotId = faker.number().randomNumber(),
                code = faker.lorem().word(),
                active = faker.number().randomDigit()
            )
        }
    }
}
