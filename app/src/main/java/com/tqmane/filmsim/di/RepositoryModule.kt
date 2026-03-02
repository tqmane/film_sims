package com.tqmane.filmsim.di

import com.tqmane.filmsim.data.update.GithubUpdateRepository
import com.tqmane.filmsim.data.update.UpdateRepository
import com.tqmane.filmsim.domain.ImageLoadUseCase
import com.tqmane.filmsim.domain.ImageLoadUseCaseImpl
import com.tqmane.filmsim.domain.LutApplyUseCase
import com.tqmane.filmsim.domain.LutApplyUseCaseImpl
import com.tqmane.filmsim.domain.WatermarkUseCase
import com.tqmane.filmsim.domain.WatermarkUseCaseImpl
import com.tqmane.filmsim.domain.export.ImageExportUseCase
import com.tqmane.filmsim.domain.export.ImageExportUseCaseImpl
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

    @Binds
    @Singleton
    abstract fun bindImageExportUseCase(impl: ImageExportUseCaseImpl): ImageExportUseCase

    @Binds
    @Singleton
    abstract fun bindUpdateRepository(impl: GithubUpdateRepository): UpdateRepository
}
