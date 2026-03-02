package com.tqmane.filmsim.core.di

import com.tqmane.filmsim.core.asset.AssetProvider
import com.tqmane.filmsim.core.asset.AssetProviderImpl
import com.tqmane.filmsim.core.security.SecurityChecker
import com.tqmane.filmsim.core.security.SecurityCheckerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {

    @Binds
    @Singleton
    abstract fun bindSecurityChecker(impl: SecurityCheckerImpl): SecurityChecker

    @Binds
    @Singleton
    abstract fun bindAssetProvider(impl: AssetProviderImpl): AssetProvider
}
