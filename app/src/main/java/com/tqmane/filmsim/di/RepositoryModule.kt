package com.tqmane.filmsim.di

import com.tqmane.filmsim.domain.ImageLoadUseCase
import com.tqmane.filmsim.domain.LutApplyUseCase
import com.tqmane.filmsim.domain.WatermarkUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideImageLoadUseCase(): ImageLoadUseCase = ImageLoadUseCase()

    @Provides
    @Singleton
    fun provideLutApplyUseCase(): LutApplyUseCase = LutApplyUseCase()

    @Provides
    @Singleton
    fun provideWatermarkUseCase(): WatermarkUseCase = WatermarkUseCase()
}
