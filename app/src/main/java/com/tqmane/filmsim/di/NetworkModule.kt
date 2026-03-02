package com.tqmane.filmsim.di

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideCertificatePinner(): CertificatePinner =
        CertificatePinner.Builder()
            .add("api.github.com", "sha256/H8zmHRgw4cFDQn+MvcyfhImeWNY4kN9HXO/J9xX32gk=")
            .add("api.github.com", "sha256/i7WTqTvh0OioIruIfFR4kMPnBqrS2rdiVPl/s2uC/CY=")
            .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(pinner: CertificatePinner): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .certificatePinner(pinner)
            .build()
}
