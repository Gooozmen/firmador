package ar.com.rgp;

import java.awt.TrayIcon.MessageType;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;;

public class WebSocketServer {

	// URL al servidor de backend para obtener y subir los documentos
	public static String backendHOST;
	// URL al servidor de backend
	public static String updateUrl;
	public static Socket clientSocket;
	private int checkUpdateAfter=20;
	private int numeroFirma=0;
	private ServerSocket server;

	public void start() {

		int portNumber = 9678;

		try
		{
			server = new ServerSocket(portNumber);
		}
		catch (IOException exception)
		{
			RgpMain.showTaskBarMessage("Error iniciando", "No se pudo iniciar el servicio: "+exception.getMessage(), MessageType.ERROR);
			throw new IllegalStateException("Could not create web server", exception);
		}

		RgpMain.showTaskBarMessage("Inicio firmador", "Servicio iniciado", MessageType.INFO);

		while (true)
		{

			try
			{
				clientSocket = server.accept(); // waits until a client connects
			}
			catch (IOException waitException)
			{
				RgpMain.showTaskBarMessage("WaitException", "No se pudo aguardar por una conexión de cliente: "+waitException.getMessage(), MessageType.ERROR);
				throw new IllegalStateException("Could not wait for client connection", waitException);
			}

			InputStream inputStream;

			try
			{
				inputStream = clientSocket.getInputStream();
			}
			catch (IOException inputStreamException)
			{
				RgpMain.showTaskBarMessage("inputStreamException", "No se pudo obtener entrada de cliente: "+inputStreamException.getMessage(), MessageType.ERROR);
				throw new IllegalStateException("Could not connect to client input stream", inputStreamException);
			}

			OutputStream outputStream;

			try
			{
				outputStream = clientSocket.getOutputStream();
			}
			catch (IOException inputStreamException)
			{
				throw new IllegalStateException("Could not connect to client input stream", inputStreamException);
			}

			try
			{
				doHandShakeToInitializeWebSocketConnection(inputStream, outputStream);
			}
			catch (UnsupportedEncodingException handShakeException)
			{
				throw new IllegalStateException("Could not connect to client input stream", handShakeException);
			}

			try
			{
				printInputStream(inputStream);
			}
			catch (IOException printException)
			{
				throw new IllegalStateException("Could not connect to client input stream", printException);
			}
		}
	}

	public void checkForUpdates() {
		try {
			if (RgpMain.debug)
				System.out.println("--------- checkForUpdates");
			if (RgpMain.debug)
				System.out.println("WebSocketServer.updateUrl:" + WebSocketServer.updateUrl+"/firmador/");

			byte byt[] = getBytes(WebSocketServer.updateUrl+"serviceUpdate");
			
			if (byt!=null && byt.length>0) {
				String updVersion = new String (byt);
				updVersion = updVersion.replaceAll("\n", "");

				System.out.println("Versión local: " + RgpMain.version);
				System.out.println("Versión vigente: " + updVersion);
				if (!updVersion.equalsIgnoreCase(RgpMain.version)) {


					byt = getBytes(WebSocketServer.updateUrl+"Firmador-"+updVersion+".jar");

					if (byt!=null && byt.length>0) {
						FileOutputStream fos = new FileOutputStream ("Firmador-"+updVersion+".jar");
						fos.write(byt);
						fos.flush();
						fos.close();
						
						File file = new File ("setVersion.cmd");
						if (file.exists()) {
							file.delete();
						}
						fos = new FileOutputStream ("setVersion.cmd");
						fos.write(new String ("SET RGP_VERSION=Firmador-"+updVersion+".jar").getBytes());
						fos.flush();
						fos.close();
						
						WebSocketServer.sendMessageToClient("VALIDANDO");
						WebSocketServer.sendMessageToClient("ERROR: El firmador se deber� reiniciar para ejecutar la actualizaci�n correctamente. Reintente nuevamente en un minuto.");
						try {
							server.close();
						}
						catch (Exception e) {
							
						}
					    try {
					    	Runtime.getRuntime().exec(new String[]{"cmd.exe","/c","firmador.cmd"});
					    	Thread.sleep(5000);

					    } catch (IOException e) {
					        e.printStackTrace();
					    }
						System.exit(0);
					}
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void printInputStream(InputStream inputStream) throws IOException {
		int len = 0;
		byte[] b = new byte[4024];
		// rawIn is a Socket.getInputStream();
		boolean isFirst = true;
		// while (isFirst) {
		len = inputStream.read(b);
		if (len != -1) {

			byte rLength = 0;
			int rMaskIndex = 2;
			int rDataStart = 0;
			// b[0] is always text in my case so no need to check;
			byte data = b[1];
			byte op = (byte) 127;
			rLength = (byte) (data & op);

			if (rLength == (byte) 126)
				rMaskIndex = 4;
			if (rLength == (byte) 127)
				rMaskIndex = 10;

			byte[] masks = new byte[4];

			int j = 0;
			int i = 0;
			for (i = rMaskIndex; i < (rMaskIndex + 4); i++) {
				masks[j] = b[i];
				j++;
			}

			rDataStart = rMaskIndex + 4;

			int messLen = len - rDataStart;

			byte[] message = new byte[messLen];

			for (i = rDataStart, j = 0; i < len; i++, j++) {
				message[j] = (byte) (b[i] ^ masks[j % 4]);
			}

			System.out.println(new String(message));
			AccionesFirmador acciones = new AccionesFirmador();
			try {
				String mensaje = new String (message);
				String args[]=mensaje.split("-##-");
				
				String protocol=args[3];
				String puerto="80";
				if (args.length>5 && !args[5].trim().equalsIgnoreCase("") && !args[5].trim().equalsIgnoreCase("0")) {
					puerto = args[5];
				}
				else {
					if (protocol.startsWith("https"))
						puerto = "443";
				}

				// puerto = "7001";

				WebSocketServer.backendHOST = args[3]+"//"+args[4]+":"+puerto+"/rgp-web/api/";
				WebSocketServer.updateUrl = args[3]+"//"+args[4]+":"+puerto+"/firmador/";

				// Verifica la primera vez que se conecta si hay actualizaciones o cuando el número de firmas sea igual al definido por checkUpdateAfter
				if (numeroFirma==0 || numeroFirma<this.checkUpdateAfter)
				{
					checkForUpdates();
				}
				System.out.println("NRO FIRMA: "+numeroFirma);
				numeroFirma+=1;

				if (args[2].contains(",")) {
					String[] idTramites = args[2].split(",");
					for (String idTramite : idTramites) {
						acciones.firmar(args[0], args[1], idTramite);
					}
				} else {
					acciones.firmar(args[0], args[1], args[2]);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				// sendMessageToClient (e.getMessage());
				WebSocketServer.sendMessageToClient("ERROR: " + e.getLocalizedMessage());
				RgpMain.showTaskBarMessage("Error firmando", e.getMessage(), MessageType.ERROR);
			} finally {
				sendMessageToClient ("END");
			}

			b = new byte[1024];
		}
		isFirst = false;
		inputStream.close();
		// }
	}
	public static void sendMessageToClient(String mensaje) {
		System.err.println("-----------------------------------" + mensaje);
		try {
			OutputStream outputStream = clientSocket.getOutputStream();
			outputStream.write(encode(mensaje));
			outputStream.flush();
		} catch (UnsupportedEncodingException err) {
			err.printStackTrace();
		} catch (IOException err) {
			err.printStackTrace();
		}
	}
	public static byte[] encode(String mess) throws IOException {
		byte[] rawData = mess.getBytes();

		int frameCount = 0;
		byte[] frame = new byte[10];

		frame[0] = (byte) 129;

		if (rawData.length <= 125) {
			frame[1] = (byte) rawData.length;
			frameCount = 2;
		} else if (rawData.length >= 126 && rawData.length <= 65535) {
			frame[1] = (byte) 126;
			int len = rawData.length;
			frame[2] = (byte) ((len >> 8) & (byte) 255);
			frame[3] = (byte) (len & (byte) 255);
			frameCount = 4;
		} else {
			frame[1] = (byte) 127;
			int len = rawData.length;
			frame[2] = (byte) ((len >> 56) & (byte) 255);
			frame[3] = (byte) ((len >> 48) & (byte) 255);
			frame[4] = (byte) ((len >> 40) & (byte) 255);
			frame[5] = (byte) ((len >> 32) & (byte) 255);
			frame[6] = (byte) ((len >> 24) & (byte) 255);
			frame[7] = (byte) ((len >> 16) & (byte) 255);
			frame[8] = (byte) ((len >> 8) & (byte) 255);
			frame[9] = (byte) (len & (byte) 255);
			frameCount = 10;
		}

		int bLength = frameCount + rawData.length;

		byte[] reply = new byte[bLength];

		int bLim = 0;
		for (int i = 0; i < frameCount; i++) {
			reply[bLim] = frame[i];
			bLim++;
		}
		for (int i = 0; i < rawData.length; i++) {
			reply[bLim] = rawData[i];
			bLim++;
		}

		return reply;
	}
	private static void doHandShakeToInitializeWebSocketConnection(InputStream inputStream, OutputStream outputStream)
			throws UnsupportedEncodingException {
		String data = new Scanner(inputStream, "UTF-8").useDelimiter("\\r\\n\\r\\n").next();

		Matcher get = Pattern.compile("^GET").matcher(data);

		if (get.find()) {
			Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
			match.find();

			byte[] response = null;
			try {
				response = ("HTTP/1.1 101 Switching Protocols\r\n" + "Connection: Upgrade\r\n"
						+ "Upgrade: websocket\r\n" + "Sec-WebSocket-Accept: "
						+ DatatypeConverter.printBase64Binary(MessageDigest.getInstance("SHA-1")
								.digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")))
						+ "\r\n\r\n").getBytes("UTF-8");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				outputStream.write(response, 0, response.length);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {

		}
	}
	private static byte[] getBytes (String urlString) throws Exception {
		int BUFFER_SIZE = 4096;
		byte byt[] = null;
		HttpURLConnection httpConn = null;
		URL url = new URL(urlString);
		httpConn = (HttpURLConnection) url.openConnection();

		int responseCode = httpConn.getResponseCode();
		String responseBody = httpConn.getResponseMessage();

		if (responseCode == HttpURLConnection.HTTP_OK) {
			String fileName = "";
			String disposition = httpConn.getHeaderField("Content-Disposition");
			String contentType = httpConn.getContentType();
			int contentLength = httpConn.getContentLength();

			InputStream inputStream = httpConn.getInputStream();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			int bytesRead = -1;
			byte[] buffer = new byte[BUFFER_SIZE];
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
			byt = outputStream.toByteArray();
			outputStream.close();
			inputStream.close();
		}
		
		return byt;
	}
}