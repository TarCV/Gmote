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

import org.gmote.common.media.MediaMetaInfo;
import org.gmote.server.media.MediaCommandHandler;
import uk.co.caprica.vlcj.player.base.MediaPlayer;


public class VlcDefaultCommandHandler extends MediaCommandHandler {

  protected MediaPlayer singlePlayer;
  
  private static VlcDefaultCommandHandler instance = null;
  
  // Private constructor to prevent instantiation
  protected VlcDefaultCommandHandler() {}
  
  public static VlcDefaultCommandHandler instance(MediaPlayer mediaPlayer) {
    if (instance == null) {
      instance = new VlcDefaultCommandHandler();
    }
    instance.singlePlayer = mediaPlayer;
    return instance;
  }
  
  /* (non-Javadoc)
   * @see com.r3mote.server.media.VlcCommandHandler#closeMedia(boolean)
   */
  protected void closeMedia() {
    singlePlayer.controls().stop();
    //player.getMediaDescriptor().release();
  }

  /* (non-Javadoc)
   * @see com.r3mote.server.media.VlcCommandHandler#stopMedia()
   */
  protected void stopMedia() {
    singlePlayer.controls().setTime(0);
    pauseMedia();
  }

  /* (non-Javadoc)
   * @see com.r3mote.server.media.VlcCommandHandler#rewind()
   */
  protected void rewind() {
    singlePlayer.controls().skipTime(-POSITION_INCREMENT_SEC * 1000);
  }

  /* (non-Javadoc)
   * @see com.r3mote.server.media.VlcCommandHandler#fastForward()
   */
  protected void fastForward() {
    singlePlayer.controls().skipTime(POSITION_INCREMENT_SEC * 1000);
  }

  /* (non-Javadoc)
   * @see com.r3mote.server.media.VlcCommandHandler#pauseMedia()
   */
  protected void pauseMedia() {
    singlePlayer.controls().pause();
  }

  /* (non-Javadoc)
   * @see com.r3mote.server.media.VlcCommandHandler#playMedia()
   */
  protected void playMedia() {
    singlePlayer.controls().play();
  }

  @Override
  protected void fastForwardLong() {
    singlePlayer.controls().skipTime(LONG_POSITION_INCREMENT_SEC * 1000);
  }

  @Override
  protected void rewindLong() {
    singlePlayer.controls().skipTime(-LONG_POSITION_INCREMENT_SEC * 1000);
  }

  protected MediaPlayer getPlayer() {
    return singlePlayer;
  }

  @Override
  protected void setVolume(int volume) {
    singlePlayer.audio().setVolume(volume);
  }
  
  @Override
  protected int getVolume() {
    return singlePlayer.audio().volume();
  }

  @Override
  protected void toggleMute() {
    singlePlayer.audio().mute();
  }

  @Override
  public MediaMetaInfo getNewMediaInfo() {
    // TODO(mstogaitis): Might want to pass the video name here.
    return null;
  }


}
