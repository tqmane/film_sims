package com.tqmane.filmsim.di

import com.tqmane.filmsim.domain.ImageLoadUseCase
import com.tqmane.filmsim.domain.ImageLoadUseCaseImpl
import com.tqmane.filmsim.domain.LutApplyUseCase
import com.tqmane.filmsim.domain.LutApplyUseCaseImpl
import com.tqmane.filmsim.domain.WatermarkUseCase
import com.tqmane.filmsim.domain.WatermarkUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindImageLoadUseCase(impl: ImageLoadUseCaseImpl): ImageLoadUseCase

    @Binds
    @Singleton
    abstract fun bindLutApplyUseCase(impl: LutApplyUseCaseImpl): LutApplyUseCase

    @Binds
    @Singleton
    abstract fun bindWatermarkUseCase(impl: WatermarkUseCaseImpl): WatermarkUseCase
}
