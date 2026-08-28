package dev.johnoreilly.common.di

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.johnoreilly.peopleinspace.db.PeopleInSpaceDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.*
import org.koin.dsl.module

actual class ContextWrapper

actual val nativeModule = module {
    single { ContextWrapper() }
    single<HttpClientEngine> { Java.create() }
    single {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            .also { PeopleInSpaceDatabase.Schema.create(it) }
        PeopleInSpaceDatabaseWrapper(driver, PeopleInSpaceDatabase(driver))
    }
}
