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

import org.blinkenlights.jid3.MP3File;
import org.gmote.common.media.MediaMetaInfo;
import org.gmote.server.media.MediaInfoUpdater;
import org.gmote.server.media.PlayerUtil;
import uk.co.caprica.vlcj.media.Meta;
import uk.co.caprica.vlcj.player.base.MediaApi;
import uk.co.caprica.vlcj.player.list.MediaListPlayer;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import static uk.co.caprica.vlcj.media.Meta.ARTWORK_URL;


/**
 * Handles commands when playing a playlist. We are using the deprecated
 * Playlist class since there is a bug in the current jvlc's implementation of
 * MediaList. See JVLC Playlist issue:
 * http://forum.videolan.org/viewtopic.php?f=2&t=49612&start=0&st=0&sk=t&sd=a
 * 
 * @author Marc
 * 
 */
// See class description for explanation of suppression of deprecation warning
@SuppressWarnings("deprecation")
public class VlcPlaylistCommandHandler extends VlcDefaultCommandHandler {
  private static final int MAX_ITERATION = 8;
  private static Logger LOGGER = Logger.getLogger(VlcPlaylistCommandHandler.class.getName());
  private MediaListPlayer listPlayer;
  // Time to fast forward/rewind by (in milliseconds).
  private static final long POSITION_INCREMENT_TIME = 12 * 1000;

  private static VlcPlaylistCommandHandler instance = null;
  private int idOfFirstSong = -1;
  private String mrlOfLastMediaUpdate = "";

  // Private constructor to prevent instantiation
  private VlcPlaylistCommandHandler() {
    super();
  }

  public static VlcPlaylistCommandHandler instance(MediaListPlayer player, int idOfOriginal) {
    if (instance == null) {
      instance = new VlcPlaylistCommandHandler();
    }
    instance.listPlayer = player;
    instance.singlePlayer = player.mediaPlayer().mediaPlayer();
    instance.idOfFirstSong = idOfOriginal;
    return instance;
  }

  @Override
  public void pauseMedia() {
    try {
      listPlayer.controls().pause();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  @Override
  public void playMedia() {
    mrlOfLastMediaUpdate = "";
    try {
      if (idOfFirstSong == -1) {
        listPlayer.controls().play();
      } else {
        listPlayer.controls().play(idOfFirstSong);
        idOfFirstSong = -1;
      }

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  @Override
  protected void closeMedia() {
    listPlayer.controls().stop();
  }

  @Override
  public void stopMedia() {
    try {
      // Note: We DON'T do a playlist.stop() here since other operations (such
      // as rewind) will crash the jvm.
      if (!mediaIsPaused) {
        listPlayer.controls().setPause(true);
      }
      
      listPlayer.controls().play(0);
      listPlayer.controls().setPause(true);
/*
      for (int i = 0; player.isRunning() && i < 5; i++) {
        sleep(100);
      }
*/
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  @Override
  public void fastForwardLong() {
    try {
      listPlayer.controls().playNext();
//      waitForNextSong(currentIndex);
      MediaInfoUpdater.instance().sendMediaUpdate(getNewMediaInfo());
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

/*  private void waitForNextSong(int currentIndex) throws Exception {
    if (player.itemsCount() > 1) {
      for (int i = 0; (player.getCurrentIndex() == currentIndex) && i < MAX_ITERATION; i++) {
        sleep(100);
      }
    }
  }*/

  @Override
  public void rewindLong() {

    try {
      listPlayer.controls().playPrevious();
//      waitForNextSong(currentIndex);
      MediaInfoUpdater.instance().sendMediaUpdate(getNewMediaInfo());
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  @Override
  public MediaMetaInfo getNewMediaInfo() {
/*    try {
      if (!player.isRunning()) {
        return null;
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, e.getMessage(),e);
      return null;
    }  */
    
    MediaApi media = listPlayer.mediaPlayer().mediaPlayer().media();
    if (media == null || media.info() == null) {
      return null;
    }
    String mediaMrl = media.info().mrl();
    if (mediaMrl.equals(mrlOfLastMediaUpdate)) {
      return null;
    }
    String artworkUrl = media.meta().get(ARTWORK_URL);
    
    MP3File mp3 = new MP3File(new File(mediaMrl));
    MediaMetaInfo fileInfo = PlayerUtil.getSongMetaInfo(mp3);
    useVlcMetaInfoIfNull(fileInfo, media);
    
    byte[] imageData = PlayerUtil.extractEmbeddedImageData(mp3);
    if (imageData == null && artworkUrl != null && artworkUrl.startsWith("file://")) {
      // Try to get the image from file.
      imageData = PlayerUtil.extractImageArtworkFromFile(artworkUrl);
    }
    if (imageData == null) {
      // In windows, a folder.jpg file often contains the album art
      imageData = PlayerUtil.extractImageFromFolder(mediaMrl);
    }
    
    fileInfo.setImage(imageData);
    if (imageData == null) {
      LOGGER.info("Image data is null");
    }

    mrlOfLastMediaUpdate  = mediaMrl;
    return fileInfo;
  }

  

  private void useVlcMetaInfoIfNull(MediaMetaInfo fileInfo, MediaApi media) {
    // Try to use the vlc data if we wern't able to get the data any other way.
    // We use vlc's data as a backup only since it takes a little while for vlc
    // to update this data, resulting in the user first being presented with
    // partial data (for example, no album art, no album name, and a song name
    // that = file name.mp3), and then receives the correct info a few seconds
    // later.
    if (fileInfo.getTitle() == null) {
      fileInfo.setTitle(media.meta().get(Meta.TITLE));
    }

    if (fileInfo.getArtist() == null) {
      fileInfo.setTitle(media.meta().get(Meta.ARTIST));
    }
    
    if (fileInfo.getAlbum() == null) {
      fileInfo.setTitle(media.meta().get(Meta.ALBUM));
    }
  }

}
