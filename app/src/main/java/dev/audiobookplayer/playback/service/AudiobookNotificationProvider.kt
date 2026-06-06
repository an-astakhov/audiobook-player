package dev.audiobookplayer.playback.service

import android.content.Context
import android.os.Bundle
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

        if (playerCommands.contains(Player.COMMAND_SEEK_BACK)) {
            buttons.add(
                buildPlayerButton(
                    icon = CommandButton.ICON_SKIP_BACK_30,
                    playerCommand = Player.COMMAND_SEEK_BACK,
                    compactViewIndex = 0,
                    displayName = appContext.getString(R.string.playback_notification_seek_back),
                ),
            )
        }

        if (playerCommands.contains(Player.COMMAND_PLAY_PAUSE)) {
            buttons.add(
                buildPlayerButton(
                    icon = if (showPauseButton) {
                        CommandButton.ICON_PAUSE
                    } else {
                        CommandButton.ICON_PLAY
                    },
                    playerCommand = Player.COMMAND_PLAY_PAUSE,
                    compactViewIndex = 1,
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

        if (playerCommands.contains(Player.COMMAND_SEEK_FORWARD)) {
            buttons.add(
                buildPlayerButton(
                    icon = CommandButton.ICON_SKIP_FORWARD_30,
                    playerCommand = Player.COMMAND_SEEK_FORWARD,
                    compactViewIndex = 2,
                    displayName = appContext.getString(R.string.playback_notification_seek_forward),
                ),
            )
        }

        return buttons.build()
    }

    private fun buildPlayerButton(
        icon: Int,
        playerCommand: Int,
        compactViewIndex: Int,
        displayName: String,
    ): CommandButton {
        val extras = Bundle().apply {
            putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, compactViewIndex)
        }
        return CommandButton.Builder(icon)
            .setPlayerCommand(playerCommand)
            .setDisplayName(displayName)
            .setExtras(extras)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "playback"
        const val NOTIFICATION_ID = 1001
    }
}
