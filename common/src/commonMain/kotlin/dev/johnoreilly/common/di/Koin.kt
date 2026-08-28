package dev.johnoreilly.common.di

import app.cash.sqldelight.db.SqlDriver
import dev.johnoreilly.common.remote.AstroviewerApi
import dev.johnoreilly.common.remote.PeopleInSpaceApi
import dev.johnoreilly.common.repository.PeopleInSpaceRepository
import dev.johnoreilly.common.repository.PeopleInSpaceRepositoryInterface
import dev.johnoreilly.common.viewmodel.ISSPositionViewModel
import dev.johnoreilly.common.viewmodel.PersonListViewModel
import dev.johnoreilly.peopleinspace.db.PeopleInSpaceDatabase
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

fun initKoin(enableNetworkLogs: Boolean = false, appDeclaration: KoinAppDeclaration? = null) =
    startKoin {
        appDeclaration?.invoke(this)
        modules(commonModule(enableNetworkLogs), nativeModule)
    }

// called by iOS etc
fun initKoin() = initKoin(enableNetworkLogs = false)

// helpers for iOS/Swift clients to resolve view models from Koin
// (composition-root service location so view models can use constructor injection)
fun personListViewModel(): PersonListViewModel = KoinPlatform.getKoin().get()
fun issPositionViewModel(): ISSPositionViewModel = KoinPlatform.getKoin().get()

fun commonModule(enableNetworkLogs: Boolean) = module {
    single { Json { isLenient = true; ignoreUnknownKeys = true } }
    single { createHttpClient(get(), get(), enableNetworkLogs) }
    single { CoroutineScope(Dispatchers.Default + SupervisorJob()) }

    singleOf(::PeopleInSpaceApi)
    singleOf(::AstroviewerApi)
    singleOf(::PeopleInSpaceRepository) bind PeopleInSpaceRepositoryInterface::class

    viewModelOf(::PersonListViewModel)
    viewModelOf(::ISSPositionViewModel)
}

class PeopleInSpaceDatabaseWrapper(val driver: SqlDriver, val instance: PeopleInSpaceDatabase)

expect class ContextWrapper

expect val nativeModule: Module

fun createHttpClient(httpClientEngine: HttpClientEngine, json: Json, enableNetworkLogs: Boolean) = HttpClient(httpClientEngine) {
    install(ContentNegotiation) {
        json(json)
    }
    if (enableNetworkLogs) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }
}
