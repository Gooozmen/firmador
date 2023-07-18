package ar.com.rgp;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.com.rgp.common.validations.ListaMensaje;
import firmaENG.FirmadorByteArray;

public class AccionesFirmador {
	private static final boolean debug = true;
	private static final String INGRESO="INGRESO";
	private static final String OBSERVADO="OBSERVADO";

	public AccionesFirmador() {
	}

	public void firmar(String userId, String token, String idTramite) throws Exception {
		if (debug)
			System.out.println("--------- Firmar: Resultado (FIRMANDO) ");

		if (debug)
			System.out.println("Firmar, idTramite: " + idTramite);

		procesarFirma(userId, token, idTramite);
	}

	protected byte[] procesarFirma(String user, String token, String trmId) throws Exception {

		user = user.replace("\"","");

		if (debug)
			System.out.println("--------- ProcesarFirma");
		if (debug)
			System.out.println("user:" + user);
		if (debug)
			System.out.println("token:" + token);
		if (debug)
			System.out.println("trmId: " + trmId);

		// TODO Modificar el aliasCertificado para que sea igual al USER que viene como
		// parametro
		String aliasCertificado = "GOBIERNO CBA PRUEBA";
		if (!RgpMain.testUser)
			aliasCertificado = user;

		String tipoAlmacen = "Windows-MY";
		String setNombreAlmacen = null;
		String setContraAlmacen = "S";
		String dirImagen = "." + java.io.File.separator + "images" + java.io.File.separator + "logo_gobcordoba.png";
		String fuente = "." + java.io.File.separator + "images" + java.io.File.separator + "tahoma.ttf";

		// CONFIG FIRMADOR
		FirmadorByteArray fir = new FirmadorByteArray();
		fir.setAliasCertificado(aliasCertificado);
		fir.setTipoAlmacen(tipoAlmacen);
		fir.setNombreAlmacen(setNombreAlmacen);
		fir.setContraAlmacen(setContraAlmacen);
		fir.setNombreCiudadFirma("Cordoba, Argentina");
		fir.setRazonFirma("Aseguramiento del no cambio del contenido del documento");
		fir.setCerrarDocumento(true);
		fir.setDirImgFirma(dirImagen);
		fir.setDirFuenteFirma(fuente);
		fir.setFirmaCadaHoja(true);
		fir.setFirmaVisible(true);

		byte byt[] = null;
		try {

			//DESCOMENTAR INICIO
			//WebSocketServer.sendMessageToClient("VALIDANDO " + trmId);
			//byt = getPdfFromService(WebSocketServer.backendHOST + "report/getPdfFormulario/" + trmId, token);
			//WebSocketServer.sendMessageToClient("FIRMANDO");
			//DESCOMENTAR FIN

			//TRY JUAN
			Path path = Paths.get("C:\\Users\\jguzm\\Downloads\\EjemploNoFirmado.pdf");
			byt = Files.readAllBytes(path);
			//END TRY

			byt = fir.firmar(byt);
			if (byt == null || (fir.getMensaje() != null && !fir.getMensaje().getTexto().equals("Listo"))) {
				throw new Exception(fir.getMensaje().getTexto());
			}


			//DESCOMENTAR INICIO
			//WebSocketServer.sendMessageToClient("UPLOAD");
			//uploadDocFirmado(byt, trmId, token);
			//DESCOMENTAR FIN

			String outputPath = "C:\\Users\\jguzm\\Downloads\\";
			String outputFileName = "pruebaCicloCompletoFirmaVisible.pdf";

			try(FileOutputStream fos = new FileOutputStream(outputPath+outputFileName))
			{
				fos.write(byt);
			}
			System.out.println("PDF saved successfully.");

		} catch (Exception e) {
			System.err.println("--------- Firmar: Resultado (ERROR) " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
		if (debug)
			System.out.println("--------- Firmar: Resultado (EXITO) ");

		//DESCOMENTAR INICIO
		//WebSocketServer.sendMessageToClient ("OK " + trmId);
		//DESCOMENTAR FIN

		return byt;
	}

	public void uploadDocFirmado(byte byt[], String trmId, String token) throws Exception {
		if (debug)
			System.out.println("uploadDocFirmado: " + trmId);
		int BUFFER_SIZE = 4096;

		URL url = new URL(WebSocketServer.backendHOST + "tramite/ingresarTramiteFirmado/" + trmId + "/base64");
		System.out.println("url: " + url.toString());
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setDoOutput(true);
		httpConn.setRequestMethod("POST");
		httpConn.addRequestProperty("Content-Type", "application/octet-stream");
		httpConn.addRequestProperty("X-AUTHENTICATION-TOKEN", token);

		httpConn.setUseCaches(false);
		DataOutputStream wr = new DataOutputStream(httpConn.getOutputStream());
		
		byt = Base64.getEncoder().encode(byt);
		
		wr.write(byt);
		wr.flush();
		wr.close();

		int responseCode = httpConn.getResponseCode();
		String responseBody = httpConn.getResponseMessage();

		if (responseCode == HttpURLConnection.HTTP_OK) {
			String fileName = "";
			String disposition = httpConn.getHeaderField("Content-Disposition");
			String contentType = httpConn.getContentType();
			int contentLength = httpConn.getContentLength();

			if (debug)
				System.out.println("Content-Type = " + contentType);
			if (debug)
				System.out.println("Content-Disposition = " + disposition);
			if (debug)
				System.out.println("Content-Length = " + contentLength);
			if (debug)
				System.out.println("fileName = " + fileName);

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
			httpConn.disconnect();

		} else {
			if (debug)
				System.out.println("uploadDocFirmado, response body " + responseBody);
			String errorMessage = httpConn.getHeaderField("x-error");
			if (responseCode == 404)
			{
				throw new Exception("En este momento no se puede acceder al servicio de registro de documentos firmados. Disculpe las molestias ocasionadas.\r\nCodigo ERROR: " + responseCode
						+ ".\r\nMensaje:" + errorMessage);
			}
			if (responseCode == 401)
			{
				throw new Exception("No se pudo validar la identidad del usuario. No se puede registrar el documento en el servidor. \r\nCodigo ERROR: " + responseCode);
			}

			throw new Exception("No se pudo registrar el documento firmado. \r\n\r\n- Codigo ERROR: " + responseCode
					+ ".\r\nMensaje:" + errorMessage);
		}
	}

	public byte[] getPdfFromService(String fileURL, String token) throws Exception {
		if (debug)
			System.out.println("--------- getPdfFromService");
		if (debug)
			System.out.println("fileURL:" + fileURL);
		int BUFFER_SIZE = 4096;

		byte byt[] = null;
		URL url = new URL(fileURL);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.addRequestProperty("X-AUTHENTICATION-TOKEN", token);
		int responseCode = httpConn.getResponseCode();
		String responseBody = httpConn.getResponseMessage();

		if (responseCode == HttpURLConnection.HTTP_OK) {
			String fileName = "";
			String disposition = httpConn.getHeaderField("Content-Disposition");
			String contentType = httpConn.getContentType();
			int contentLength = httpConn.getContentLength();

			if (disposition != null) {

				int index = disposition.indexOf("filename=");
				if (index > 0) {
					fileName = disposition.substring(index + 10, disposition.length() - 1);
				}
			} else {
				fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1, fileURL.length());
			}

			if (debug)
				System.out.println("Content-Type = " + contentType);
			if (debug)
				System.out.println("Content-Disposition = " + disposition);
			if (debug)
				System.out.println("Content-Length = " + contentLength);
			if (debug)
				System.out.println("fileName = " + fileName);

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

			if (debug)
				System.out.println("File downloaded");
		} else {
			if (responseCode == 400) {
				Charset charset = Charset.forName("UTF-8");

				InputStreamReader inputStream = new InputStreamReader(httpConn.getErrorStream(), charset);
				BufferedReader in = new BufferedReader(inputStream);

				String line = "";
				StringBuffer sb = new StringBuffer();
				while((line=in.readLine())!=null){
					sb.append(line);
				}
				in.close();
				inputStream.close();
				httpConn.disconnect();

				ObjectMapper mapper = new ObjectMapper();
				String jsonInString = sb.toString();//new String(byt, charset);
				System.out.println("Locale: "+Locale.getDefault().getDisplayName());
				System.out.println("jsonInString: "+jsonInString);

				ListaMensaje error = mapper.readValue(jsonInString, ListaMensaje.class);

				String textoError = "";
				for (int i = 0; error != null && error.getListaErrores() != null
						&& i < error.getListaErrores().size(); i++) {
					textoError += " - " + error.getListaErrores().get(i).getMensajeError() + " \n";
				}
				//WebSocketServer.sendMessageToClient("ERROR: " + textoError);
				System.out.println("textoError: "+textoError);
				throw new Exception("El trámite se encuentra incompleto. \n" + textoError);
			}

			if (debug)
				System.out.println("Error getPdfFromService");
			String errorMessage = httpConn.getHeaderField("x-error");
			httpConn.disconnect();

			if (responseCode == 404)
			{
				throw new Exception("En este momento no se puede acceder al servicio. Disculpe las molestias ocasionadas.\r\nCodigo ERROR: " + responseCode
						+ ".\r\nMensaje:" + errorMessage);
			}
			if (responseCode == 401)
			{
				throw new Exception("No se pudo validar la identidad del usuario. No se puede recuperar el documento del servidor. \r\nCodigo ERROR: " + responseCode);
			}
			throw new Exception("No se puede recuperar el documento del servidor. \r\nCodigo ERROR: " + responseCode
					+ ".\r\nMensaje:" + errorMessage);
		}
		httpConn.disconnect();
		return byt;
	}

}
