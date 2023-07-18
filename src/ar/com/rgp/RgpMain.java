package ar.com.rgp;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;

import javax.swing.ImageIcon;

public class RgpMain {
	public static final boolean debug = true;
	private static final int LOCALHOST = 0;
	public static boolean testUser = false;
	public static final String version = "1.0.4.5";
	private static TrayIcon trayIcon;
	
	public static void main(String[] args) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		System.setProperty("file.encoding","UTF-8");
		Field charset = Charset.class.getDeclaredField("defaultCharset");
		charset.setAccessible(true);
		charset.set(null,null);

		/*
		try {
			SocketServer socketServer = new SocketServer();
			socketServer.StartServer();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		*/

		/*test JUAN FIRMAR EXITOSO
		AccionesFirmador acciones = new AccionesFirmador();
		try {
			acciones.procesarFirma("DEBANNE DIEGO OSCAR", "token", "101092109");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		END TEST*/

		//REGION COMENTADA

		try {
			if (args != null && args.length >= 1 && (args[0].equals("NO_USER"))) {
				RgpMain.testUser = true;
			}

			setBarIcons();

			WebSocketServer server = new WebSocketServer();
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
 	protected static Image createImage(String path, String description) {
	        URL imageURL = RgpMain.class.getResource(path);
	         
	        if (imageURL == null) {
	            System.err.println("Resource not found: " + path);
	            return null;
	        } else {
	            return (new ImageIcon(imageURL, description)).getImage();
	        }
	    }
	private static void setBarIcons() {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }
        final PopupMenu popup = new PopupMenu();
        trayIcon = new TrayIcon(createImage("/images/true.gif", "tray icon"));
        final SystemTray tray = SystemTray.getSystemTray();
       
        MenuItem exitItem = new MenuItem("Salir");
        exitItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
		    	System.exit(0);
			}
		});
       
        popup.add(exitItem);
       
        trayIcon.setPopupMenu(popup);
        trayIcon.setToolTip("Registro General de la Propiedad v"+RgpMain.version);
		if (testUser)
			trayIcon.setToolTip(trayIcon.getToolTip()+" - SIN VALIDACIONES");

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
        }
	}
	public static void showTaskBarMessage(String titulo, String message, MessageType type) {
        trayIcon.displayMessage(titulo, message, type);
	}

}
