package io.github.lazyengineer.castaway.shared.di

import io.github.lazyengineer.castaway.shared.database.FeedLocalDataSource
import io.github.lazyengineer.castaway.shared.repository.FeedRepository
import io.github.lazyengineer.castaway.shared.usecase.GetFeedUseCase
import io.github.lazyengineer.castaway.shared.webservice.FeedRemoteDataSource
import io.ktor.client.*
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(coreModule, platformModule)
    }

private val coreModule = module {
    single { HttpClient() }
    single {
        FeedRepository(
            remoteDataSource = FeedRemoteDataSource(get()),
            localDataSource = FeedLocalDataSource()
        )
    }
    single { GetFeedUseCase(get() as FeedRepository) }
}

expect val platformModule: Module