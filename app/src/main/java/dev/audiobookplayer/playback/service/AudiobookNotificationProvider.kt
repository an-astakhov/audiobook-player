package dev.audiobookplayer.playback.service

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import dev.audiobookplayer.R

@UnstableApi
class AudiobookNotificationProvider(
    context: Context,
) : DefaultMediaNotificationProvider(
    context,
    { NOTIFICATION_ID },
    CHANNEL_ID,
    R.string.playback_notification_channel_name,
) {
    private val appContext = context.applicationContext

    override fun getMediaButtons(
        session: MediaSession,
        playerCommands: Player.Commands,
        customLayout: ImmutableList<CommandButton>,
        showPauseButton: Boolean,
    ): ImmutableList<CommandButton> {
        val buttons = ImmutableList.builder<CommandButton>()

        customLayout.firstOrNull { commandButton ->
            PlaybackNotificationCommands.isSeekBack(commandButton.sessionCommand)
        }?.let { buttons.add(it) }

        if (playerCommands.contains(Player.COMMAND_PLAY_PAUSE)) {
            buttons.add(
                PlaybackNotificationCommands.buildPlayPauseButton(
                    context = appContext,
                    playerCommand = Player.COMMAND_PLAY_PAUSE,
                    icon = if (showPauseButton) {
                        CommandButton.ICON_PAUSE
                    } else {
                        CommandButton.ICON_PLAY
                    },
                    displayName = appContext.getString(
                        if (showPauseButton) {
                            R.string.playback_notification_pause
                        } else {
                            R.string.playback_notification_play
                        },
                    ),
                ),
            )
        }

        customLayout.firstOrNull { commandButton ->
            PlaybackNotificationCommands.isSeekForward(commandButton.sessionCommand)
        }?.let { buttons.add(it) }

        return buttons.build()
    }

    companion object {
        const val CHANNEL_ID = "playback"
        const val NOTIFICATION_ID = 1001
    }
}
