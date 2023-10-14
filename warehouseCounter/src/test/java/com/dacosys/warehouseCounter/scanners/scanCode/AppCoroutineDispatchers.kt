package com.dacosys.warehouseCounter.scanners.scanCode

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.koin.dsl.module

data class AppCoroutineDispatchers(
    val main: CoroutineDispatcher,
    val io: CoroutineDispatcher,
    val database: CoroutineDispatcher,
    val network: CoroutineDispatcher
)


private val scheduler = TestCoroutineScheduler()

val mainTestDispatcher = StandardTestDispatcher(scheduler)
val ioTestDispatcher = StandardTestDispatcher(scheduler, name = "io")
val dbTestDispatcher = StandardTestDispatcher(scheduler, name = "db")
val networkTestDispatcher = StandardTestDispatcher(scheduler, name = "network")

val appCoroutineDispatchers = AppCoroutineDispatchers(
    main = mainTestDispatcher, io = ioTestDispatcher, database = dbTestDispatcher, network = networkTestDispatcher
)

val testDispatcherModule = module {
    single {
        appCoroutineDispatchers
    }
}