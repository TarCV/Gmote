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

package org.gmote.server;

import com.apple.eawt.ApplicationEvent;
import org.gmote.server.settings.StartupSettings;

import javax.imageio.ImageIO;
import javax.swing.UIManager;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * @author Mimi
 *
 */
public class GmoteServerUiMac extends GmoteServerUi {
  SystemTray tray = SystemTray.getSystemTray();
  TrayIcon trayIcon;
  PopupMenu popupMenu;

  public GmoteServerUiMac(GmoteServer server) {
    super(server);
    this.server = server;
  }

  void initializeUi() throws AWTException {
    MenuItem item;
    
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
        e.printStackTrace();
    }
    if( Integer.parseInt(System.getProperty("java.version").substring(2,3)) >=5 )
        System.setProperty("javax.swing.adjustPopupLocationToFit", "false");
    
    popupMenu = new PopupMenu("Gmote Menu");

    addMediaPlayerControls();

    Menu settingsMenu = new Menu("Settings");
    popupMenu.add(settingsMenu);
    
    item = new MenuItem("Change password");
    item.addActionListener(settingsListener);
    settingsMenu.add(item);

    item = new MenuItem("Change media paths");
    item.addActionListener(mediaPathListener);
    settingsMenu.add(item);
    
    Menu helpMenu = new Menu("Help");
    popupMenu.add(helpMenu);
    
    item = new MenuItem("Show local ip address");
    item.addActionListener(ipAddressListener);
    helpMenu.add(item);
    
    item = new MenuItem("Show settings and logs folder");
    item.addActionListener(logFolderListener);
    helpMenu.add(item);      
    
    item = new MenuItem("Connection Help");
    item.addActionListener(helpListener);
    helpMenu.add(item);
    
    
    popupMenu.addSeparator();
    item = new MenuItem("Quit");
    item.addActionListener(exitListener);
    popupMenu.add(item);

    Image i;
    try (final InputStream resourceStream = GmoteServerUiMac.class.getResourceAsStream("/res/gmote_icon_s.png")) {
      i = ImageIO.read(resourceStream);
    } catch (IOException e) {
      i = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
      LOGGER.log(Level.SEVERE, "Failed to load app icon", e);
    }
    trayIcon = new TrayIcon(i, "Gmote", popupMenu);
    trayIcon.setImageAutoSize(true);
    tray.add(trayIcon);
    
  }

  void handleExtraSettings(StartupSettings settings) {
    
  }
  
  public static void main(String[] args) {
    GmoteServer server = new GmoteServer();
    GmoteServerUi ui = new GmoteServerUiMac(server);
    ui.sharedMain(args);
  }

  public void addMediaPlayerControls() {
    if (!mediaPlayerControlsVisible) {
      MenuItem item;
      item = new MenuItem("Pause");
      item.addActionListener(pauseListener);
      popupMenu.insert(item, 0);

      item = new MenuItem("Play");
      item.addActionListener(playListener);
      popupMenu.insert(item, 0);
      
      item = new MenuItem("Previous");
      item.addActionListener(previousListener);
      popupMenu.insert(item, 0);
      
      item = new MenuItem("Next");
      item.addActionListener(nextListener);
      popupMenu.insert(item, 0);
      
      popupMenu.addSeparator();
      mediaPlayerControlsVisible = true;
    }
  }
  
  public void removeMediaPlayerControls() {
  }
  
  public void quit(ApplicationEvent e) {
    System.exit(0);
  }
}
