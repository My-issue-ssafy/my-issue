// PlayerModule.kt
package com.ioi.myssue.player

import android.content.Context
import com.ioi.myssue.player.AudioController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideAudioController(
        @ApplicationContext context: Context
    ): AudioController = AudioController(context.applicationContext)
}
