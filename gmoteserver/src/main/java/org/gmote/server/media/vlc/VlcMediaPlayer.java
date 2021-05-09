/**
 * Copyright 2009 Marc Stogaitis and Mimi Sun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gmote.server.media.vlc;

import org.gmote.common.FileInfo;
import org.gmote.common.FileInfo.FileType;
import org.gmote.common.Protocol.Command;
import org.gmote.common.media.MediaMetaInfo;
import org.gmote.server.PlatformUtil;
import org.gmote.server.media.MediaCommandHandler;
import org.gmote.server.media.MediaInfoUpdater;
import org.gmote.server.media.MediaPlayerInterface;
import org.gmote.server.media.UnsupportedCommandException;
import org.gmote.server.settings.DefaultSettings;
import org.gmote.server.settings.DefaultSettingsEnum;
import org.gmote.server.settings.SupportedFiletypeSettings;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.medialist.MediaList;
import uk.co.caprica.vlcj.medialist.MediaListRef;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.fullscreen.adaptive.AdaptiveFullScreenStrategy;
import uk.co.caprica.vlcj.player.list.MediaListPlayer;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("deprecation")
public class VlcMediaPlayer implements MediaPlayerInterface {
  private static final int MAX_DELAY_ATTEMPTS = 5;
  private static Logger LOGGER = Logger.getLogger(VlcMediaPlayer.class.getName());
  private static final String VLC_LOG_NAME = "/logs/vlc.log";

  private static final long VIDEO_DELAY_TIMEOUT = 10 * 1000;

  // VLC Player.
  MediaPlayerFactory jvlc;
  MediaCommandHandler commandHandler;
  VlcMediaPlayerFrame mediaPlayerFrame;

  // Player state.
  private boolean playingVideo = false;

  public VlcMediaPlayer() {
  }

  @Override
  public void initialise(String[] arguments) {
    // Construct a vlc media player object.
    try {
      jvlc = new MediaPlayerFactory();
    } catch (UnsatisfiedLinkError e) {
      String message = "Error while gmote tried to start vlc. Make sure that vlc is installed and in your system PATH variable. Please see the logs for more details or visit www.gmote.org/faq : " + e.getMessage();
      LOGGER.log(Level.SEVERE, message, e);
      JOptionPane.showMessageDialog(null, message);
      System.exit(1);
    }
  }
  
  @Override
  public synchronized void controlPlayer(Command command) throws UnsupportedCommandException {

    // Delegate to the appropriate handler.
    if (commandHandler != null) {

      commandHandler.executeCommand(command);

      if (command == Command.CLOSE) {
        doClose();
      }
    }
  }

  /**
   * Closes the media. Synchronized since we can call this method by receiving
   * an request from the client or by receiving an 'end reached' event on the
   * media listener.
   */
  private synchronized void doClose() {

    if (mediaPlayerFrame != null) {
      mediaPlayerFrame.closeFrame();
      
    }
    playingVideo = false;
    Runtime.getRuntime().gc();
  }

  /**
   * Launches a media file in the media player.
   * 
   * @throws UnsupportedEncodingException
   * @throws UnsupportedCommandException
   * 
   * @see {@link VlcPlaylistCommandHandler} for information about deprecation
   *      warning
   */
  @Override
  public synchronized void runFile(FileInfo fileInfo)
      throws UnsupportedEncodingException, UnsupportedCommandException {

    // Stop the player if it is already playing
    MediaInfoUpdater.instance().setPlayerToPoll(null);
    if (commandHandler != null) {
      if (playingVideo) {
        controlPlayer(Command.CLOSE);
      } else {
        controlPlayer(Command.STOP);
      }
    }

    String fileName = fileInfo.getAbsolutePath();
    FileType fileType = fileInfo.getFileType();
    
    if (fileType == FileType.PLAYLIST || fileType == FileType.MUSIC) {
      runMusic(fileName, fileType);
    } else {
      // Setup the player with the current file to play.
      runMovie(fileName, fileType);
    }
  }
  
  @Override
  public MediaMetaInfo getNewMediaInfo() {
    if (commandHandler == null) {
      return null;
    }
    return commandHandler.getNewMediaInfo();
  }
  

  private void runMovie(String fileName, FileType fileType) throws UnsupportedCommandException {
    // Appends dvdsimple to the file name so that vlc will skip all of the dvd
    // menus.
    if (fileType == FileType.DVD_DRIVE) {
      fileName = "dvdsimple://" + fileName;
    }

    mediaPlayerFrame = new VlcMediaPlayerFrame(this);
    mediaPlayerFrame.createFrame();
    final EmbeddedMediaPlayerComponent player = new EmbeddedMediaPlayerComponent(
                    jvlc,
                    null,
                    new AdaptiveFullScreenStrategy(mediaPlayerFrame.vlcFrame),
                    null,
                    null);

    mediaPlayerFrame.playWith(player);


    // Set the default volume.
    player.mediaPlayer().audio().setVolume(
      Integer.parseInt(DefaultSettings.instance().getSetting(
              DefaultSettingsEnum.VOLUME))
    );

    commandHandler = VlcDefaultCommandHandler.instance(player.mediaPlayer());
    commandHandler.setMediaIsOpen(true);

    playingVideo = true;
    controlPlayer(Command.PLAY);
    player.mediaPlayer().media().play(fileName);

    if (!player.mediaPlayer().media().isValid()) {
      controlPlayer(Command.CLOSE);
      LOGGER.log(Level.SEVERE, "Unable to launch video file");
      return;
    }
    player.mediaPlayer().fullScreen().set(true);
  }

  private void runMusic(String fileName, FileType fileType) throws UnsupportedCommandException {
    mediaPlayerFrame = new VlcMediaPlayerFrame(this);
    mediaPlayerFrame.createFrame();
    final EmbeddedMediaPlayerComponent player = new EmbeddedMediaPlayerComponent(
            jvlc,
            null,
            new AdaptiveFullScreenStrategy(mediaPlayerFrame.vlcFrame),
            null,
            null);
    mediaPlayerFrame.playWith(player);

    // Special handling for playlists since there is a bug in jvlc related to
    // playlists.

    final MediaListPlayer listPlayer = jvlc.mediaPlayers().newMediaListPlayer();
    listPlayer.mediaPlayer().setMediaPlayer(player.mediaPlayer());
    final MediaList playList = jvlc.media().newMediaList();

    int idOfOriginal = -1;
    try {

      if (fileType == FileType.PLAYLIST) {
        playList.media().add(fileName, "Playlist");
      } else {
        // Add all of the files of the directory in the playlist.
        File originalFile = new File(fileName);

        File[] allFilesInDirectory = originalFile.getParentFile().listFiles();
        if (DefaultSettings.instance().getSetting(DefaultSettingsEnum.SHUFFLE_SONGS)
                .equalsIgnoreCase("true")) {
          Collections.shuffle(Arrays.asList(allFilesInDirectory));
        }

        for (File file : allFilesInDirectory) {
          String name = file.getName();
          FileType type = SupportedFiletypeSettings.fileNameToFileType(name);

          if (type == FileType.MUSIC) {

            playList.media().add(file.getAbsolutePath(), file.getName());
            if (originalFile.equals(file)) {
              idOfOriginal = playList.media().count() - 1;
            }
          }
        }
      }

      MediaListRef mediaListRef = playList.newMediaListRef();
      try {
        listPlayer.list().setMediaList(mediaListRef);
      }
      finally {
        mediaListRef.release();
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      playList.release();
    }

    commandHandler = VlcPlaylistCommandHandler.instance(listPlayer, idOfOriginal);

    // Set the default volume.
    player.mediaPlayer().audio().setVolume(
            Integer.parseInt(DefaultSettings.instance().getSetting(
                    DefaultSettingsEnum.VOLUME))
    );

    commandHandler.setMediaIsOpen(true);
    controlPlayer(Command.PLAY);

/*    try {
      for (int i = 0; i < MAX_DELAY_ATTEMPTS && idOfOriginal != -1
          && (id != idOfOriginal); i++) {
        sleep(100);
      }
    } catch (VLCException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      throw new UnsupportedCommandException(e.getMessage());
    }

    // Wait until the song starts playing.
    sleep(100);
    try {
      long startTime = new Date().getTime();
      while (!playList.isRunning() && new Date().getTime() - startTime < VIDEO_DELAY_TIMEOUT) {
        sleep(100);
      }
    } catch (VLCException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }*/
    
    
    MediaInfoUpdater.instance().sendMediaUpdate(getNewMediaInfo());
    MediaInfoUpdater.instance().setPlayerToPoll(this);

  }

  /**
   * Convenience function that simply logs the exception.
   * 
   * @param timeInMili
   */
  private void sleep(long timeInMili) {
    try {
      Thread.sleep(timeInMili);
    } catch (InterruptedException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  @Override
  public List<FileInfo> getBaseLibraryFiles() {
    return null;
  }

  @Override
  public List<FileInfo> getLibrarySubFiles(FileInfo fileInfo) {
    return null;
  }

  @Override
  public boolean isRunning() {
    return true;
  }

}
