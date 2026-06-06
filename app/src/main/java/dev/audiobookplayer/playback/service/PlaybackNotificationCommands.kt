package dev.audiobookplayer.playback.service

import android.content.Context
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import dev.audiobookplayer.R

object PlaybackNotificationCommands {
    const val ACTION_SEEK_BACK = "dev.audiobookplayer.action.SEEK_BACK"
    const val ACTION_SEEK_FORWARD = "dev.audiobookplayer.action.SEEK_FORWARD"

    val seekBackCommand = SessionCommand(ACTION_SEEK_BACK, Bundle.EMPTY)
    val seekForwardCommand = SessionCommand(ACTION_SEEK_FORWARD, Bundle.EMPTY)

    fun buildAvailableSessionCommands(): SessionCommands {
        return MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
            .buildUpon()
            .add(seekBackCommand)
            .add(seekForwardCommand)
            .build()
    }

    fun buildCustomLayout(context: Context): List<CommandButton> {
        return listOf(
            buildSeekCommandButton(
                context = context,
                command = seekBackCommand,
                icon = CommandButton.ICON_SKIP_BACK_30,
                compactViewIndex = 0,
                displayName = context.getString(R.string.playback_notification_seek_back),
            ),
            buildSeekCommandButton(
                context = context,
                command = seekForwardCommand,
                icon = CommandButton.ICON_SKIP_FORWARD_30,
                compactViewIndex = 2,
                displayName = context.getString(R.string.playback_notification_seek_forward),
            ),
        )
    }

    fun isSeekBack(command: SessionCommand?): Boolean {
        return command?.customAction == ACTION_SEEK_BACK
    }

    fun isSeekForward(command: SessionCommand?): Boolean {
        return command?.customAction == ACTION_SEEK_FORWARD
    }

    fun buildPlayPauseButton(
        context: Context,
        playerCommand: Int,
        icon: Int,
        displayName: String,
    ): CommandButton {
        return CommandButton.Builder(icon)
            .setPlayerCommand(playerCommand)
            .setDisplayName(displayName)
            .setExtras(
                Bundle().apply {
                    putInt(DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX, 1)
                },
            )
            .build()
    }

    private fun buildSeekCommandButton(
        context: Context,
        command: SessionCommand,
        icon: Int,
        compactViewIndex: Int,
        displayName: String,
    ): CommandButton {
        return CommandButton.Builder(icon)
            .setSessionCommand(command)
            .setDisplayName(displayName)
            .setExtras(
                Bundle().apply {
                    putInt(DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX, compactViewIndex)
                },
            )
            .build()
    }
}
