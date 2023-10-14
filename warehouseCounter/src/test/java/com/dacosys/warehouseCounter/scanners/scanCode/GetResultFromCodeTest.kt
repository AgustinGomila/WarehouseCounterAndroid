package com.dacosys.warehouseCounter.scanners.scanCode

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dacosys.warehouseCounter.data.room.database.WcDatabase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest


class GetResultFromCodeTest : KoinTest {

    private lateinit var db: WcDatabase

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WcDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        startKoin {
            modules(listOf(getTestModule(), testDispatcherModule))
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        db.close()
    }

    private fun getTestModule() = module {
        single { /** todo */ }
    }
}