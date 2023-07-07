/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package firmaENG;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import firmaSIN.FirmadorAccede;
import frimaOPE.MensajeUI;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author d26151365
 */
public class Firmador implements Cloneable {

    private String nombreAlmacen;
    private String contraAlmacen;
    private String tipoAlmacen;
    private String aliasCertificado;
    private String[] directorioOrigen;
    private String[] nombresArchivosFirmar;
    private String nombreCiudadFirma;
    private String razonFirma;
    private long cantidadFirmada;
    private String[] nombresArchivosVerificar;
    private String nombreProveedorSeguridad;
    private String contraCertificado;
    private MensajeUI mensaje;
    private boolean conTS;
    private boolean firmaCadaHoja;
    private boolean firmaVisible;
    private String dirImgFirma;
    private String dirFuenteFirma;
    private PrintWriter logSucesos;
    private boolean cerrarDocumento;
    private String[] urlCrl;
    private String usuarioProxy;
    private String contraProxy;

    private static void detectarProxy(String location, String usuProxy, String contraProxy) {

        try {

            System.setProperty("java.net.useSystemProxies", "true");

            List l = ProxySelector.getDefault().select(
                    new URI(location));

            for (Iterator iter = l.iterator(); iter.hasNext(); ) {

                Proxy proxy = (Proxy) iter.next();

                System.out.println("proxy type : " + proxy.type());

                InetSocketAddress addr = (InetSocketAddress) proxy.address();

                if (addr == null) {

                    System.out.println("No Proxy");

                } else {

                    System.out.println("proxy hostname : "
                            + addr.getHostName());

                    System.out.println("proxy port : "
                            + addr.getPort());

                    if (proxy.type() == Proxy.Type.HTTP) {
                        System.setProperty("http.proxyHost", addr.getHostName());
                        System.setProperty("http.proxyPort", String.valueOf(addr.getPort()));

                        if (usuProxy != null) {
                            System.setProperty("http.proxyUser", usuProxy);
                        }

                        if (contraProxy != null) {
                            System.setProperty("http.proxyPassword", contraProxy);
                        }

                    }

                }

            }

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    public List<Verificacion> validar() {

        List<Verificacion> lstVeri;
        lstVeri = new ArrayList<>();
        List<Verificacion> lstVer;

        FirmadorAccede fa = new FirmadorAccede();
        fa.setAliasCertificado(getAliasCertificado());
        fa.setNombreAlmacen(getNombreAlmacen());
        fa.setNombreProveedor("SunMSCAPI");

        for (String archVerificar : getNombresArchivosVerificar()) {
            if (archVerificar != null) {
                if (archVerificar.length() > 0) {
                    fa.setNombreArchivoOrigen(archVerificar);
                    try {
                        lstVer = fa.verificar();
                        lstVeri.addAll(lstVer);
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
            }
        }

        return lstVeri;

    }

    public void agregarCampo(String src, String dest) throws IOException, DocumentException, NoSuchAlgorithmException {

        StringBuilder sb;

        sb = new StringBuilder();

        PdfReader readerA = new PdfReader(src);
        PdfReaderContentParser parser = new PdfReaderContentParser(readerA);

        TextExtractionStrategy strategy;
        for (int i = 1; i <= readerA.getNumberOfPages(); i++) {
            strategy = parser.processContent(i, new SimpleTextExtractionStrategy());
            sb.append(strategy.getResultantText());
        }
        readerA.close();

        byte[] bytTimeSClavAp = sb.toString().getBytes("US-ASCII");

        MessageDigest digesto = MessageDigest.getInstance("SHA-512");

        byte[] shaDigesto = digesto.digest(bytTimeSClavAp);

        Formatter formateador = new Formatter();
        for (byte b : shaDigesto) {
            formateador.format("%02x", b);
        }

        String resultado = formateador.toString();

        PdfContentByte under;

        PdfReader reader = new PdfReader(src);
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(dest));

        Font f = new Font(FontFamily.HELVETICA, 8);
        Phrase p = new Phrase(resultado, f);

        for (int i = 1; i < reader.getNumberOfPages() + 1; i++) {
            under = stamper.getOverContent(i);
            ColumnText.showTextAligned(under, Element.ALIGN_LEFT, p, 15, 5, 90);
        }

        /*
        PdfContentByte over = stamper.getOverContent(1);
        p = new Phrase("This watermark is added ON TOP OF the existing content", f);
        ColumnText.showTextAligned(over, Element.ALIGN_CENTER, p, 297, 500, 0);
        p = new Phrase("This TRANSPARENT watermark is added ON TOP OF the existing content", f);
        over.saveState();
        PdfGState gs1 = new PdfGState();
        gs1.setFillOpacity(0.5f);
        over.setGState(gs1);
        ColumnText.showTextAligned(over, Element.ALIGN_CENTER, p, 297, 450, 0);
        over.restoreState();
         */
        stamper.close();
        reader.close();

    }

    public void firmar() throws Exception {

        boolean conError;
        BouncyCastleProvider j;
        FirmadorAccede fa;
        KeyStore ks = null;
        Certificate ej;
        X509Certificate eno;
        Enumeration engu;
        String gyu;
        MensajeUI men = new MensajeUI();
        int n;
        int cant;
        File esDirOArch;
        Firmador firRecursivo;
        String[] vecDirectorio = {""};
        conError = false;
        boolean bEncontro = false;

        j = new BouncyCastleProvider();

        System.out.println("Proveedor cripto: " + j.getInfo() + " nombre " + j.getName());

        Security.addProvider(j);

        fa = new FirmadorAccede();

        fa.setLogSucesos(logSucesos);

        men.setTexto("Listo");
        men.setTipo(MensajeUI.INFORMACION);
        fa.setMensaje(men);

        fa.setAliasCertificado(getAliasCertificado());
        fa.setContraCertificado(getContraCertificado());
        fa.setCiudadFirma(getNombreCiudadFirma());
        fa.setNombreAlmacen(getNombreAlmacen());
        fa.setRazonFirma(getRazonFirma());
        fa.setCerrarDocumento(isCerrarDocumento());

        setCantidadFirmada(0);

        System.out.println("Almacén instancia");
        try {

            System.out.println("Almacén carga");
//            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks = KeyStore.getInstance(getTipoAlmacen());
            if (ks == null) {
                throw new Exception("Error: No se ha podido cargar el almacén de certificados.");
            }

            switch (getTipoAlmacen().toUpperCase()) {
                case "JKS":
                case "PKCS12":
                    ks.load(new FileInputStream(getNombreAlmacen()), getContraAlmacen().toCharArray());
                    break;

                case "WINDOWS-MY":
                case "WINDOWS-ROOT":
                    ks.load(null);
                    break;
            }
            ks.load(null);

            System.out.println("Proveedor almacen: " + ks.getProvider().getInfo());
            System.out.println("Almacén toString: " + ks.toString());
            System.out.println("Entradas en el almacén: " + ks.size());
            System.out.println("Tipo: " + ks.getType());

            engu = ks.aliases();

            bEncontro = false;
            while (engu.hasMoreElements()) {
                gyu = engu.nextElement().toString();
                System.out.println("Alias: " + gyu);
                ej = ks.getCertificate(gyu);

                eno = (X509Certificate) ej;

                if (gyu.equalsIgnoreCase(this.getAliasCertificado())) {
                    bEncontro = true;

                    Date hoy;

                    hoy = new Date();
/*
                    if (eno.getNotAfter().compareTo(hoy) < 0) {
                        throw new Exception("El certificado de identidad digital se encuentra vencido.");
                    }
*/
                    if (eno.getNotBefore().compareTo(hoy) >= 0) {
                        throw new Exception("El certificado de identidad digital aún no es válido por fecha.");
                    }

                    break;

                }

                System.out.println("Descriptivo: " + eno.getSubjectX500Principal().getName());
            }

            if (!bEncontro) {
                throw new Exception("No se ha encontrado certificado de identidad con alias " + this.getAliasCertificado());
            }

            System.out.println("Antes setAlmacenCert");

            fa.setAlmacenCert(ks);
            fa.setDirImgFirma(getDirImgFirma());
            fa.setDirFuenteFirma(getDirFuenteFirma());
            fa.setFirmaVisible(isFirmaVisible());

            System.out.println("Antes baja");

            detectarProxy("https://timestamp.globalsign.com/", this.getUsuarioProxy(), this.getContraProxy());

            fa.setUrlCrl(this.getUrlCrl());

            fa.BajarCrl();

            if (isConTS()) {
                fa.EstablecerUrlExt();
            }

            System.out.println("Luego baja");

        } catch (KeyStoreException ex) {
            System.out.println("Error KeyStoreException: " + ex.getClass().getName() + " " + ex.getMessage());

            conError = true;
            Logger.getLogger(Firmador.class.getName()).log(Level.SEVERE, null, ex);

            getMensaje().setTexto("Error KeyStoreException: " + ex.getClass().getName() + " " + ex.getMessage());
            getMensaje().setTipo(MensajeUI.ERROR);

            throw new Exception("Error KeyStoreException: " + ex.getClass().getName() + " " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("Error IOException: " + ex.getClass().getName() + " " + ex.getMessage());

            conError = true;
            Logger.getLogger(Firmador.class.getName()).log(Level.SEVERE, null, ex);

            getMensaje().setTexto("Error IOException: " + ex.getClass().getName() + " " + ex.getMessage());
            getMensaje().setTipo(MensajeUI.ERROR);

            throw new Exception("Error IOException: " + ex.getClass().getName() + " " + ex.getMessage());
        } catch (NoSuchAlgorithmException ex) {
            System.out.println("Error NoSuchAlgorithmException: " + ex.getClass().getName() + " " + ex.getMessage());

            conError = true;
            Logger.getLogger(Firmador.class.getName()).log(Level.SEVERE, null, ex);

            getMensaje().setTexto("Error NoSuchAlgorithmException: " + ex.getClass().getName() + " " + ex.getMessage());
            getMensaje().setTipo(MensajeUI.ERROR);

            throw new Exception("Error NoSuchAlgorithmException: " + ex.getClass().getName() + " " + ex.getMessage());
        } catch (CertificateException ex) {
            System.out.println("Error CertificateException: " + ex.getClass().getName() + " " + ex.getMessage());

            conError = true;
            Logger.getLogger(Firmador.class.getName()).log(Level.SEVERE, null, ex);

            getMensaje().setTexto("Error CertificateException: " + ex.getClass().getName() + " " + ex.getMessage());
            getMensaje().setTipo(MensajeUI.ERROR);

            throw new Exception("Error CertificateException: " + ex.getClass().getName() + " " + ex.getMessage());
        } catch (Exception ex) {
            System.out.println("Error Exception: " + ex.getClass().getName() + " " + ex.getMessage());

            conError = true;
            Logger.getLogger(Firmador.class.getName()).log(Level.SEVERE, null, ex);

            getMensaje().setTexto("Error Exception: " + ex.getClass().getName() + " " + ex.getMessage());
            getMensaje().setTipo(MensajeUI.ERROR);

            throw new Exception("Error Exception: " + ex.getClass().getName() + " " + ex.getMessage());
        }

        if (conError == false) {
            if (getNombresArchivosFirmar() != null) {
                if (getNombresArchivosFirmar().length > 0) {
                    for (String archFirmar : getNombresArchivosFirmar()) {
                        if (archFirmar != null) {
                            if (archFirmar.length() > 0) {
                                fa.setNombreArchivoOrigen(archFirmar);

                                try {
                                    n = 1;
                                    cant = 1;

                                    if (isFirmaCadaHoja() == true) {
                                        cant = fa.getCantidadHojas(fa.getNombreArchivoOrigen());
                                        for (int i = 0; i < cant; i++) {
                                            fa.firmar(isConTS(), false, n, cant);
                                            n++;
                                        }
                                        //una vez más para certificar
                                        fa.firmar(isConTS(), false, n, cant);
                                    } else {
                                        n++;
                                        fa.firmar(isConTS(), false, n, cant);
                                    }

                                    getMensaje().setTexto(fa.getMensaje().getTexto());
                                    getMensaje().setTipo(fa.getMensaje().getTipo());
                                } catch (IOException e) {
                                    getMensaje().setTexto("Error de entrada salida: " + e.getClass().getName() + " " + e.getMessage());
                                    getMensaje().setTipo(MensajeUI.ERROR);

                                    throw new Exception("Error de entrada salida: " + e.getClass().getName() + " " + e.getMessage());
                                } catch (DocumentException e) {
                                    getMensaje().setTexto("Error de documento: " + e.getClass().getName() + " " + e.getMessage());
                                    getMensaje().setTipo(MensajeUI.ERROR);

                                    throw new Exception("Error de documento: " + e.getClass().getName() + " " + e.getMessage());
                                } catch (GeneralSecurityException e) {
                                    getMensaje().setTexto("Error de seguridad: " + e.getClass().getName() + " " + e.getMessage());
                                    getMensaje().setTipo(MensajeUI.ERROR);

                                    throw new Exception("Error de seguridad: " + e.getClass().getName() + " " + e.getMessage());
                                } catch (Exception e) {
                                    getMensaje().setTexto("Error: " + e.getClass().getName() + " " + e.getMessage());
                                    getMensaje().setTipo(MensajeUI.ERROR);

                                    throw new Exception("Error: " + e.getClass().getName() + " " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }

            if (getDirectorioOrigen() != null) {
                if (getDirectorioOrigen().length > 0) {
                    for (String dire : getDirectorioOrigen()) {
                        if (dire != null) {
                            if (dire.length() > 0) {
                                try {
                                    DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dire));
                                    Iterator<Path> iter = stream.iterator();
                                    while (iter.hasNext()) {

                                        Path path = iter.next();
                                        fa.setNombreArchivoOrigen(path.toString());

                                        esDirOArch = new File(path.toString());
                                        if (esDirOArch.isDirectory()) {
                                            firRecursivo = (Firmador) this.clone();
                                            vecDirectorio[0] = path.toString();
                                            firRecursivo.setDirectorioOrigen(vecDirectorio);
                                            firRecursivo.firmar();
                                        }

                                        n = 1;
                                        cant = 1;

                                        if (fa.getExtensionArchivoOrigen().equals("PDF")) {
                                            if (isFirmaCadaHoja() == true) {
                                                cant = fa.getCantidadHojas(fa.getNombreArchivoOrigen());
                                                for (int i = 0; i < cant; i++) {
                                                    fa.firmar(isConTS(), false, n, cant);
                                                    n++;
                                                }
                                                //una vez más para certificar
                                                if (isCerrarDocumento()) {
                                                    fa.firmar(isConTS(), false, n, cant);
                                                }

                                            } else {
                                                n++;
                                                fa.firmar(isConTS(), false, n, cant);
                                            }

                                            setMensaje(fa.getMensaje());
                                        }

                                    }
                                } catch (IOException e) {
                                    getMensaje().setTexto("Error de entrada salida: " + e.getClass().getName() + " " + e.getMessage());
                                    getMensaje().setTipo(MensajeUI.ERROR);

                                    throw new Exception("Error de entrada salida: " + e.getClass().getName() + " " + e.getMessage());
                                } catch (DocumentException e) {
                                    getMensaje().setTexto("Error de documento: " + e.getClass().getName() + " " + e.getMessage());
                                    getMensaje().setTipo(MensajeUI.ERROR);

                                    throw new Exception("Error de documento: " + e.getClass().getName() + " " + e.getMessage());
                                } catch (GeneralSecurityException e) {
                                    getMensaje().setTexto("Error de seguridad: " + e.getClass().getName() + " " + e.getMessage());
                                    getMensaje().setTipo(MensajeUI.ERROR);

                                    throw new Exception("Error de seguridad: " + e.getClass().getName() + " " + e.getMessage());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    getMensaje().setTexto("Error: " + e.getClass().getName() + " " + e.getMessage());
                                    getMensaje().setTipo(MensajeUI.ERROR);

                                    throw new Exception("Error: " + e.getClass().getName() + " " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        }

        setCantidadFirmada(fa.getCantidadFirmada());

    }

    /**
     * @return the nombreAlmacen
     */
    public String getNombreAlmacen() {
        return nombreAlmacen;
    }

    /**
     * @param nombreAlmacen the nombreAlmacen to set
     */
    public void setNombreAlmacen(String nombreAlmacen) {
        this.nombreAlmacen = nombreAlmacen;
    }

    /**
     * @return the aliasCertificado
     */
    public String getAliasCertificado() {
        return aliasCertificado;
    }

    /**
     * @param aliasCertificado the aliasCertificado to set
     */
    public void setAliasCertificado(String aliasCertificado) {
        this.aliasCertificado = aliasCertificado;
    }

    /**
     * @return the directorioOrigen
     */
    public String[] getDirectorioOrigen() {
        return directorioOrigen;
    }

    /**
     * @param directorioOrigen the directorioOrigen to set
     */
    public void setDirectorioOrigen(String[] directorioOrigen) {
        this.directorioOrigen = directorioOrigen;
    }

    /**
     * @return the nombresArchivosFirmar
     */
    public String[] getNombresArchivosFirmar() {
        return nombresArchivosFirmar;
    }

    /**
     * @param nombresArchivosFirmar the nombresArchivosFirmar to set
     */
    public void setNombresArchivosFirmar(String[] nombresArchivosFirmar) {
        this.nombresArchivosFirmar = nombresArchivosFirmar;
    }

    /**
     * @return the nombreCiudadFirma
     */
    public String getNombreCiudadFirma() {
        return nombreCiudadFirma;
    }

    /**
     * @param nombreCiudadFirma the nombreCiudadFirma to set
     */
    public void setNombreCiudadFirma(String nombreCiudadFirma) {
        this.nombreCiudadFirma = nombreCiudadFirma;
    }

    /**
     * @return the razonFirma
     */
    public String getRazonFirma() {
        return razonFirma;
    }

    /**
     * @param razonFirma the razonFirma to set
     */
    public void setRazonFirma(String razonFirma) {
        this.razonFirma = razonFirma;
    }

    /**
     * @return the cantidadFirmada
     */
    public long getCantidadFirmada() {
        return cantidadFirmada;
    }

    /**
     * @param cantidadFirmada the cantidadFirmada to set
     */
    public void setCantidadFirmada(long cantidadFirmada) {
        this.cantidadFirmada = cantidadFirmada;
    }

    /**
     * @return the nombresArchivosVerificar
     */
    public String[] getNombresArchivosVerificar() {
        return nombresArchivosVerificar;
    }

    /**
     * @param nombresArchivosVerificar the nombresArchivosVerificar to set
     */
    public void setNombresArchivosVerificar(String[] nombresArchivosVerificar) {
        this.nombresArchivosVerificar = nombresArchivosVerificar;
    }

    /**
     * @return the contraAlmacen
     */
    public String getContraAlmacen() {
        return contraAlmacen;
    }

    /**
     * @param contraAlmacen the contraAlmacen to set
     */
    public void setContraAlmacen(String contraAlmacen) {
        this.contraAlmacen = contraAlmacen;
    }

    /**
     * @return the tipoAlmacen
     */
    public String getTipoAlmacen() {
        return tipoAlmacen;
    }

    /**
     * @param tipoAlmacen the tipoAlmacen to set
     */
    public void setTipoAlmacen(String tipoAlmacen) {
        this.tipoAlmacen = tipoAlmacen;
    }

    /**
     * @return the nombreProveedorSeguridad
     */
    public String getNombreProveedorSeguridad() {
        return nombreProveedorSeguridad;
    }

    /**
     * @param nombreProveedorSeguridad the nombreProveedorSeguridad to set
     */
    public void setNombreProveedorSeguridad(String nombreProveedorSeguridad) {
        this.nombreProveedorSeguridad = nombreProveedorSeguridad;
    }

    /**
     * @return the contraCertificado
     */
    public String getContraCertificado() {
        return contraCertificado;
    }

    /**
     * @param contraCertificado the contraCertificado to set
     */
    public void setContraCertificado(String contraCertificado) {
        this.contraCertificado = contraCertificado;
    }

    /**
     * @return the mensaje
     */
    public MensajeUI getMensaje() {
        return mensaje;
    }

    /**
     * @param mensaje the mensaje to set
     */
    public void setMensaje(MensajeUI mensaje) {
        this.mensaje = mensaje;
    }

    /**
     * @return the conTS
     */
    public boolean isConTS() {
        return conTS;
    }

    /**
     * @param conTS the conTS to set
     */
    public void setConTS(boolean conTS) {
        this.conTS = conTS;
    }

    /**
     * @return the firmaCadaHoja
     */
    public boolean isFirmaCadaHoja() {
        return firmaCadaHoja;
    }

    /**
     * @param firmaCadaHoja the firmaCadaHoja to set
     */
    public void setFirmaCadaHoja(boolean firmaCadaHoja) {
        this.firmaCadaHoja = firmaCadaHoja;
    }

    /**
     * @return the firmaVisible
     */
    public boolean isFirmaVisible() {
        return firmaVisible;
    }

    /**
     * @param firmaVisible the firmaVisible to set
     */
    public void setFirmaVisible(boolean firmaVisible) {
        this.firmaVisible = firmaVisible;
    }

    /**
     * @return the dirImgFirma
     */
    public String getDirImgFirma() {
        return dirImgFirma;
    }

    /**
     * @param dirImgFirma the dirImgFirma to set
     */
    public void setDirImgFirma(String dirImgFirma) {
        this.dirImgFirma = dirImgFirma;
    }

    /**
     * @return the dirFuenteFirma
     */
    public String getDirFuenteFirma() {
        return dirFuenteFirma;
    }

    /**
     * @param dirFuenteFirma the dirFuenteFirma to set
     */
    public void setDirFuenteFirma(String dirFuenteFirma) {
        this.dirFuenteFirma = dirFuenteFirma;
    }

    /**
     * @return the logSucesos
     */
    public PrintWriter getLogSucesos() {
        return logSucesos;
    }

    /**
     * @param logSucesos the logSucesos to set
     */
    public void setLogSucesos(PrintWriter logSucesos) {
        this.logSucesos = logSucesos;
    }

    /**
     * @return the cerrarDocumento
     */
    public boolean isCerrarDocumento() {
        return cerrarDocumento;
    }

    /**
     * @param cerrarDocumento the cerrarDocumento to set
     */
    public void setCerrarDocumento(boolean cerrarDocumento) {
        this.cerrarDocumento = cerrarDocumento;
    }

    /**
     * @return the urlCrl
     */
    public String[] getUrlCrl() {
        return urlCrl;
    }

    /**
     * @param urlCrl the urlCrl to set
     */
    public void setUrlCrl(String[] urlCrl) {
        this.urlCrl = urlCrl;
    }

    /**
     * @return the usuarioProxy
     */
    public String getUsuarioProxy() {
        return usuarioProxy;
    }

    /**
     * @param usuarioProxy the usuarioProxy to set
     */
    public void setUsuarioProxy(String usuarioProxy) {
        this.usuarioProxy = usuarioProxy;
    }

    /**
     * @return the contraProxy
     */
    public String getContraProxy() {
        return contraProxy;
    }

    /**
     * @param contraProxy the contraProxy to set
     */
    public void setContraProxy(String contraProxy) {
        this.contraProxy = contraProxy;
    }

}
