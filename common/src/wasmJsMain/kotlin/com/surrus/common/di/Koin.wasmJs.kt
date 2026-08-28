package dev.johnoreilly.common.di

import app.cash.sqldelight.driver.worker.createDefaultWebWorkerDriver
import dev.johnoreilly.peopleinspace.db.PeopleInSpaceDatabase
import dev.johnoreilly.peopleinspace.db.PeopleInSpaceDatabase.Companion.invoke
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import org.koin.dsl.module

actual class ContextWrapper

actual val nativeModule = module {
    single { ContextWrapper() }
    single<HttpClientEngine> { Js.create() }
    single {
        val driver = createDefaultWebWorkerDriver()
        PeopleInSpaceDatabaseWrapper(driver, PeopleInSpaceDatabase(driver))
    }
}
