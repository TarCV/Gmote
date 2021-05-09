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

package org.gmote.server.media.itunes;

import org.gmote.common.media.MediaMetaInfo;
import org.gmote.server.AppleScriptUtil;
import org.gmote.server.media.MediaCommandHandler;
import org.gmote.server.media.MediaInfoUpdater;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.gmote.server.AppleScriptUtil.convertToBoolean;
import static org.gmote.server.AppleScriptUtil.convertToInt;

// [TarCV] Reimplemented this blindly without testing at all, as I can't test on Mac at the moment
public class ItunesCommandHandler extends MediaCommandHandler {
  private static final Logger LOGGER = Logger.getLogger(ItunesCommandHandler.class
      .getName());
  private static ItunesCommandHandler instance = null;
  private static MediaMetaInfo media = null;
  private static String album = null;
  private final AppleScriptUtil scriptEngine = new AppleScriptUtil();

  protected ItunesCommandHandler() {
  }

  protected void closeMedia() {
    tellItunesTo("quit", 1);
  }

  protected void stopMedia() {
    tellItunesTo("stop", 1);
  }

  protected void rewind() {
    tellItunesTo("set player position to (player position - 12)", 1);
  }

  protected void fastForward() {
    tellItunesTo("set player position to (player position + 12)", 1);
  }

  protected void pauseMedia() {
    tellItunesTo("pause", 1);
  }

  protected void playMedia() {
    tellItunesTo("play\n\nset song repeat of current playlist to all", 1);
    MediaInfoUpdater.instance().sendMediaUpdate(getNewMediaInfo());
  }

  protected void rewindLong() {
    tellItunesTo("previous track", 1);
    MediaInfoUpdater.instance().sendMediaUpdate(getNewMediaInfo());
  }

  protected void fastForwardLong() {
    tellItunesTo("next track", 1);
    MediaInfoUpdater.instance().sendMediaUpdate(getNewMediaInfo());
  }

  protected void setVolume(int volume) {
    tellItunesTo("set sound volume to " + volume, 1);
  }

  protected int getVolume() {
    for (int i=0; i<3; i++) {
      try {
        Object result = tellItunesTo("get sound volume", 1);
        return convertToInt(result);
      } catch(Exception e){
        // getting volume failed, try again
      }
    }
    return 50;
  }

  protected void toggleMute() {
    tellItunesTo("set mute to (not mute)", 1);
  }

  protected void fullScreen() {
    String script = "tell application \"System Events\"\n"
        + "keystroke \"f\" using command down\n" + "end tell\n";
    executeForResult(script, 1);
  }

  public List<String> getPlaylists() {
    List<String> playlists = new ArrayList<String>();
    for (int times = 0; times < 3; times++) {
      try {
        List<String> result = (List<String>)tellItunesTo(
            "get the name of every playlist", 20);

        playlists.addAll(result);
        break;
      } catch (Exception e) {
      }
    }
    return playlists;
  }

  public List<String> getTracksFromPlaylist(String playlist) {
    List<String> tracks = new ArrayList<String>();
  
    for (int times = 0; times < 3; times++) {
      try {
        String script = "set theTracks to {}\n"
            + "repeat with aTrack in tracks of playlist \""
            + playlist
            + "\"\n"
            + "copy (name of aTrack) & \"|\" & (kind of aTrack) to end of theTracks\n"
            + "end repeat\n" + "theTracks\n";
        List<String> result = (List<String>)tellItunesTo(script, 1);

        int numItems = result.size();
        tracks.addAll(result);
        break;
      } catch (Exception e) {
      }
    }
    return tracks;
  }

  protected void launchAudio(String track, String playlist) {
    tellItunesTo("play track \"" + track + "\" of playlist \"" + playlist
        + "\"\nset song repeat of current playlist to all", 1);
  }

  protected void launchVideo(String name) {
    tellItunesTo("set visible of window 1 to true\nset frontmost to true", 1);
    tellItunesTo("set view of browser window 1 to playlist \"Movies\"", 1);
    tellItunesTo("play track \"" + name + "\" of playlist \"Movies\"", 1);
    fullScreen();
    LOGGER.log(Level.INFO, "Launched movie");
  }
  
  public synchronized MediaMetaInfo getNewMediaInfo() {
    try {
      List<String> result = (List<String>)tellItunesTo("get {name, artist, album} of current track", 1);
      String title = result.get(1);
      String artist = result.get(2);

      if (media != null && media.getTitle().equals(title) && media.getArtist().equals(artist)) {
        return null;
      } else {
        String newAlbum = result.get(3);
        media = new MediaMetaInfo();
        media.setTitle(title);
        media.setArtist(artist);
        media.setAlbum(newAlbum);
        
        if (!newAlbum.equals(album)) {
          album = newAlbum;
          getArtwork();
        } else {
          media.setImage(new byte[]{1});
        }
      }
        
      return media;
    } catch(java.lang.NullPointerException e) {
    } catch(Exception e) {
      LOGGER.log(Level.INFO, e.getMessage(), e);
    }
    media = null;
    return media;
  }

  private void getArtwork() {
/* TODO   NSAppleEventDescriptor result;
    try {
      int myPool = NSAutoreleasePool.push();

      result = tellItunesTo("get data of artwork 1 of current track", 1);
      NSData data = result.data();
      byte[] pictbytes = data.bytes(0, data.length());
      byte []newpictbytes = new byte[512+pictbytes.length];
      System.arraycopy(pictbytes,0,newpictbytes,512,pictbytes.length);
      
      if(QTSession.isInitialized() == false) {
        QTSession.open();
      }
      QTHandle qt = new QTHandle(newpictbytes); 
      GraphicsImporter gc = new GraphicsImporter(QTUtils.toOSType("PICT"));
      gc.setDataHandle(qt);
      QDRect qdRect = gc.getNaturalBounds();
      GraphicsImporterDrawer myDrawer = new quicktime.app.view.GraphicsImporterDrawer(gc);
      QTImageProducer qtProducer = new QTImageProducer (myDrawer, new Dimension(qdRect.getWidth(),qdRect.getHeight()));
      Image img = Toolkit.getDefaultToolkit().createImage(qtProducer);
      
      BufferedImage bu = new BufferedImage(img.getWidth(null),img.getHeight(null),BufferedImage.TYPE_INT_RGB);
      Graphics g = bu.getGraphics();
      g.drawImage(img,0,0,null);
      g.dispose();
      
      ByteArrayOutputStream bas = new ByteArrayOutputStream();
      ImageIO.write(bu, "PNG", bas);
      byte[] image = bas.toByteArray();
      //// System.out.println("++ image size: " + image.length);
      media.setImage(image);
      
      NSAutoreleasePool.pop(myPool);
    } catch(java.lang.NullPointerException e) {
    } catch(Exception e) {
      LOGGER.log(Level.INFO, e.getMessage(), e);
    } finally {
        QTSession.close();
    }*/
  }
  
  public static ItunesCommandHandler instance() {
    if (instance  == null) {
      instance = new ItunesCommandHandler();
    }
    return instance;
  }
  
  protected boolean running() {
    try {
      String script = "tell application \"System Events\"\n" +
        "set isRunning to ((application processes whose (name is equal to \"iTunes\")) count)\n" +
        "end tell\n" +
        "if isRunning is greater than 0 then\n" +
        "return true\n" +
        "else\n" +
        "return false\n" + 
        "end if";
      Object result = executeForResult(script, 3);
      System.out.println("running: " +  result);
      return convertToBoolean(result);
    } catch (Exception e) {
      LOGGER.log(Level.INFO, e.getMessage(), e);
    }
    return false;
  }
  
  public boolean isMediaOpen() {
    return running();
  }
  
  protected boolean isMediaPaused() {
    return false; //TODO(mimi)
  }
  
  /* Run commands with iTunes */
  private Object tellItunesTo(String actions, int timeout) {
    String script = "tell application \"iTunes\"\n" + actions + "\n end tell\n";
    return executeForResult(script, timeout);
  }

  private synchronized Object executeForResult(String script, int timeout) {
    script = "with timeout " + timeout + " seconds\n" + script + "\nend timeout\n";
    return scriptEngine.eval(script);
  }

  void launchFile(String fileName) {
    String fileNameForScript = fileName.replace('/', ':');
    tellItunesTo("play file \"" + fileNameForScript + "\"", 2);
  }
  
}
