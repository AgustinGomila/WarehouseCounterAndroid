package com.example.warehouseCounter.data.room.database

import android.content.Context
import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.warehouseCounter.data.room.dao.client.ClientDao
import com.github.javafaker.Faker
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.warehouseCounter.data.room.entity.client.Client as ClientRoom

@RunWith(AndroidJUnit4::class)
class ClientDaoTest {
    private lateinit var clientDao: ClientDao
    private lateinit var database: WcDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        database = inMemoryDatabaseBuilder(context, WcDatabase::class.java)
            // Allowing the main thread queries, just for testing.
            .allowMainThreadQueries().build()
        clientDao = database.clientDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun inserting_an_client_should_return_a_total_of_one_client() = runTest {
        val fakeClient = createFakeClient()
        clientDao.insert(fakeClient)
        assert(clientDao.count() == 1)
    }

    @Test
    fun inserting_an_client_should_return_a_equal_client_id() = runTest {
        val fakeClient = createFakeClient()
        val id = clientDao.insert(fakeClient)
        assert(fakeClient.clientId == id)
    }

    @Test
    fun inserting_an_client_should_return_a_equal_client() = runTest {
        val fakeClient = createFakeClient()
        val id = clientDao.insert(fakeClient)
        val client = clientDao.getById(id!!)
        assert(fakeClient == client)
    }

    @Test
    fun inserting_an_client_should_return_a_same_hashcode_client() = runTest {
        val fakeClient = createFakeClient()
        val id = clientDao.insert(fakeClient)
        val client = clientDao.getById(id!!)
        assert(fakeClient.hashCode() == client!!.hashCode())
    }

    companion object {
        fun createFakeClient(): ClientRoom {
            val faker = Faker()

            return ClientRoom(
                clientId = faker.number().randomNumber(),
                name = faker.name().fullName(),
                contactName = if (faker.random().nextBoolean()) faker.name().fullName() else null,
                phone = if (faker.random().nextBoolean()) faker.phoneNumber().phoneNumber() else null,
                address = if (faker.random().nextBoolean()) faker.address().streetAddress() else null,
                city = if (faker.random().nextBoolean()) faker.address().city() else null,
                userId = if (faker.random().nextBoolean()) faker.number().numberBetween(1, 1000) else null,
                active = faker.random().nextInt(0, 1),
                latitude = if (faker.random().nextBoolean()) faker.number().randomDouble(6, -90, 90)
                    .toFloat() else null,
                longitude = if (faker.random().nextBoolean()) faker.number().randomDouble(6, -180, 180)
                    .toFloat() else null,
                countryId = if (faker.random().nextBoolean()) faker.number().numberBetween(1, 100) else null,
                taxNumber = if (faker.random().nextBoolean()) faker.business().creditCardNumber() else null
            )
        }
    }
}
