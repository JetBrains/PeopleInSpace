package dev.johnoreilly.common.di

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.johnoreilly.peopleinspace.db.PeopleInSpaceDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import org.koin.dsl.module

actual class ContextWrapper(val context: Context)

actual val nativeModule = module {
    single { ContextWrapper(get()) }
    single<HttpClientEngine> { Android.create() }
    single {
        val driver = AndroidSqliteDriver(PeopleInSpaceDatabase.Schema.synchronous(), get<ContextWrapper>().context, "peopleinspace.db")
        PeopleInSpaceDatabaseWrapper(driver, PeopleInSpaceDatabase(driver))
    }
}
