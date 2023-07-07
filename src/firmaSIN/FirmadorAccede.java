/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package firmaSIN;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.io.RandomAccessSourceFactory;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.security.*;
import com.itextpdf.text.pdf.security.MakeSignature.CryptoStandard;
import firmaENG.Verificacion;
import frimaOPE.MensajeUI;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author d26151365
 */
public class FirmadorAccede {

    private final String[] urlOcsp;
    private final String[] urlTS;
    private String nombreAlmacen;
    private String aliasCertificado;
    private String razonFirma;
    private String ciudadFirma;
    private String nombreArchivoOrigen;
    private String nombreProveedor;
    private long cantidadFirmada;
    private boolean certificadoValido;
    private Certificate[] cadenaPersonal;
    private String[] urlsCAS;
    private String nombreArchAux;
    private KeyStore almacenCert;
    private String contraCertificado;
    private String urlOCSP;
    private int idxCrl;
    private List<CrlClient> listaCrl;
    private String[] urlCrl;
    private MensajeUI mensaje;
    private String tsPrior;
    private String dirFuenteFirma;
    private String dirImgFirma;
    private boolean firmaVisible;
    private PrintWriter logSucesos;
    private boolean cerrarDocumento;

    /**
     *
     */
    public FirmadorAccede() {
        String[] url = {"", ""};

        setUrlsCAS(url);

        urlTS = new String[4];
        urlTS[0] = "http://timestamp.globalsign.com/scripts/timestamp.dll";
        urlTS[1] = "http://timestamp.comodoca.com/authenticode";
        urlTS[2] = "http://timestamp.verisign.com/scripts/timstamp.dll";
        urlTS[3] = "http://tsa.starfieldtech.com";

        urlCrl = new String[1];
        //urlCrl[0] = "http://ws32785:81/revoke.crl";
        //urlCrl[1] = "https://crl.cacert.org/revoke.crl";

        urlOcsp = new String[1];
        urlOcsp[0] = "https://ocsp.cacert.org";

    }

    public void EstablecerUrlExt() {
        InputStream in;

        for (String s : urlTS) {

            try {
                in = new URL(s).openConnection().getInputStream();

                byte[] buf = new byte[1024];
                if (in.read(buf) != -1) {
                    setTsPrior(s);
                    break;
                }
            } catch (MalformedURLException ex) {
                Logger.getLogger(FirmadorAccede.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(FirmadorAccede.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    public void BajarCrl() throws MalformedURLException, IOException {
        InputStream in;
        ByteArrayOutputStream baos;
        List<CrlClient> crlList;
        CrlClient crlClient;

        crlList = new ArrayList<>();
        if (getUrlCrl() != null) {
            for (String cr : getUrlCrl()) {
                if (cr != null) {
                    if (!"".equals(cr)) {
                        in = new URL(cr).openConnection().getInputStream();

                        baos = new ByteArrayOutputStream();
                        byte[] buf = new byte[1024];
                        while (in.read(buf) != -1) {
                            baos.write(buf);
                        }
                        crlClient = new CrlClientOffline(baos.toByteArray());
                        crlList.add(crlClient);
                    }
                }
            }
        }

        if (crlList.size() > 0) {
            setListaCrl(crlList);
        }

    }

    public int getCantidadHojas(String sArchivo) throws FileNotFoundException, IOException {
        File file;

        file = new File(sArchivo);

        RandomAccessFile raf = new RandomAccessFile(file, "r");
        RandomAccessFileOrArray pdfFile = new RandomAccessFileOrArray(new RandomAccessSourceFactory().createSource(raf));
        PdfReader reader = new PdfReader(pdfFile, new byte[0]);
        int pages = reader.getNumberOfPages();
        reader.close();
        return pages;
    }

    public int getCantidadHojas(byte byt[]) throws FileNotFoundException, IOException {
        PdfReader reader = new PdfReader(byt, new byte[0]);
        int pages = reader.getNumberOfPages();
        reader.close();
        return pages;
    }


    /**
     * @param conTS
     * @param conOCSP
     * @param pagActual
     * @param totPag
     * @throws IOException
     * @throws DocumentException
     * @throws GeneralSecurityException
     * @throws Exception
     */
    public void firmar(byte pdf[], boolean conTS, boolean conOCSP, int pagActual, int totPag) throws IOException, DocumentException, GeneralSecurityException, Exception {

        String extension;
        char[] cContra;
        String sNombreCampo;
        PdfReader reader = null;
        boolean docCertificado;
        long epoch;
        ExternalSignature es;
        FileOutputStream os = null;

        System.out.println(("getAliasCertificado(): " + getAliasCertificado() + " - " + getAlmacenCert().getCertificate(getAliasCertificado())));
        System.out.println(("getAliasCertificado(): " + getAliasCertificado() + " - " + getAlmacenCert().getCertificateChain(getAliasCertificado())));
        Certificate[] chain = getAlmacenCert().getCertificateChain(getAliasCertificado());
        System.out.println("chain: " + chain);
        System.out.println("chain: " + chain.length);
        if (chain == null) {
            throw new Exception("Error: No se ha encontrado el certificado con el alias suministrado.");
        }

       /*
       Calendar cal = null;
       
       Object fails[] = PdfPKCS7.verifyCertificates(chain, getAlmacenCert(), null, cal);
       if (fails == null) {
           System.out.println("Certificates verified against the KeyStore");
       } else {
           System.out.println("Certificate failed: " + fails[1]);
       }
        */
        setCadenaPersonal(chain);

        System.out.println("getContraCertificado(): " + getContraCertificado());
        if (getContraCertificado() == null) {
            cContra = null;
        } else {
            cContra = getContraCertificado().toCharArray();
        }

        try {
            System.out.println("getAlmacenCert().size(): " + getAlmacenCert().size());
            Enumeration en = getAlmacenCert().aliases();
            while (en.hasMoreElements()) {
                System.out.println("EN: " + en.nextElement());
            }
            PrivateKey key = (PrivateKey) getAlmacenCert().getKey(getAliasCertificado(), cContra);

            if (key == null) {
                throw new Exception("Error: No se ha encontrado una clave privada para el certificado solicitado. " + getAliasCertificado());
            }

            PublicKey llPub = chain[0].getPublicKey();

           /*
        if (isCertificadoValido() == false) {
        validarCertificado();
        }
            */
            extension = getExtensionArchivoOrigen();
            if (getNombreArchivoOrigen().lastIndexOf(".") != -1) {
                setNombreArcAux(getNombreArchivoOrigen().substring(0, getNombreArchivoOrigen().lastIndexOf(".")) + "_firmado." + extension.toLowerCase());
            }

            switch (extension) {
                case "PDF":

                    reader = new PdfReader(getNombreArchivoOrigen());
                    AcroFields af = reader.getAcroFields();

                    docCertificado = false;

                    PdfDictionary asd;
                    SignaturePermissions perms = null;

                    for (String nombre : af.getSignatureNames()) {

                        asd = af.getSignatureDictionary(nombre);
                        perms = new SignaturePermissions(asd, perms);
                        if (perms.isCertification()) {
                            docCertificado = true;
                            break;
                        }

                    }

                    if (docCertificado) {
                        throw new Exception("El documento se encuentra certificado y no acepta modificaciones.");
                    }

                    //   reader = new PdfReader(getNombreArchivoOrigen());
                    os = new FileOutputStream(getNombreArcAux());

                    PdfStamper stamper = PdfStamper.createSignature(reader, os, '\0', null, true);

                    PdfSignatureAppearance appearance = stamper.getSignatureAppearance();

                    appearance.setReason(getRazonFirma());
                    appearance.setLocation(getCiudadFirma());

                    org.bouncycastle.asn1.x500.X500Name x500name = new JcaX509CertificateHolder((X509Certificate) chain[0]).getSubject();
                    RDN cn = x500name.getRDNs(BCStyle.CN)[0];

                    epoch = System.currentTimeMillis();
                    sNombreCampo = cn.getFirst().getValue().toString().replaceAll(" ", "") + String.valueOf(epoch / 1000) + String.valueOf(pagActual);

                    if (isCerrarDocumento()) {
                        if (totPag < pagActual) {
                            appearance.setCertificationLevel(PdfSignatureAppearance.CERTIFIED_NO_CHANGES_ALLOWED);
                            sNombreCampo = "Certificado";
                            pagActual = totPag;
                        } else {
                            appearance.setCertificationLevel(PdfSignatureAppearance.NOT_CERTIFIED);
                        }
                    } else {
                        appearance.setCertificationLevel(PdfSignatureAppearance.NOT_CERTIFIED);
                    }

                    System.out.println("Directorio de la Clase: " + FirmadorAccede.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());

                    if (isFirmaVisible()) {
                        if (totPag < pagActual) {
                            pagActual = totPag;
                        }

                        appearance.setCertificate(chain[0]);

                        appearance.setLayer2Font(new Font(BaseFont.createFont(getDirFuenteFirma(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED), 6));

                        appearance.setVisibleSignature(new Rectangle(450, 3, 595, 33), pagActual, sNombreCampo);

                        appearance.setImage(Image.getInstance(getDirImgFirma()));
                        appearance.setImageScale(-1);

                        appearance.setSignatureGraphic(Image.getInstance(getDirImgFirma()));

                        appearance.setLayer2Text("Firmado Digitalmente por: " + cn.getFirst().getValue().toString() + "\nFecha: " + new SimpleDateFormat("yyyy.MM.dd HH:mm:ssZ").format(Calendar.getInstance().getTime()));

                        appearance.setLayer4Text("Firma válida");
                    }

                    switch (getAlmacenCert().getProvider().getName()) {
                        case "SUN":
                        case "SunJSSE":
                            es = new PrivateKeySignature(key, DigestAlgorithms.SHA256, "SunRsaSign");
                            break;

                        case "SunMSCAPI":
                            es = new PrivateKeySignature(key, DigestAlgorithms.SHA256, getAlmacenCert().getProvider().getName());
                            break;

                        default:
                            es = new PrivateKeySignature(key, DigestAlgorithms.SHA256, getAlmacenCert().getProvider().getName());
                            break;
                    }

                    //busca la url en el certificado
                    OcspClient ocsp;
                    // if (conOCSP) {
                    ocsp = new OcspClientBouncyCastle();
                    //}

                    ExternalDigest digest = new BouncyCastleDigest();

                    TSAClient tsc = null;
                    if (conTS) {
                        if (getTsPrior() != null) {
                            tsc = new TSAClientBouncyCastle(getTsPrior());
                        }
                    }

                    MakeSignature.signDetached(appearance, digest, es, chain, getListaCrl(), ocsp, tsc, 0, CryptoStandard.CMS);

                    acumularFirmados();

                    acondicionarArchivos();

                    break;

                case "XML":
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setNamespaceAware(true);
                    DocumentBuilder builder = dbf.newDocumentBuilder();
                    FileInputStream entrada = new FileInputStream(getNombreArchivoOrigen());
                    Document doc = builder.parse(entrada);

                    DOMSignContext dsc = new DOMSignContext(key, doc.getDocumentElement());

                    XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

                    Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA1, null), Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);

                    SignedInfo si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, (C14NMethodParameterSpec) null), fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null), Collections.singletonList(ref));

                    KeyInfoFactory kif = fac.getKeyInfoFactory();

                    KeyValue kv = kif.newKeyValue(llPub);
                    KeyInfo ki = kif.newKeyInfo(Collections.singletonList(kv));

                    XMLSignature signature = fac.newXMLSignature(si, ki);

                    signature.sign(dsc);

                    acumularFirmados();

                    OutputStream sal;
                    sal = new FileOutputStream(getNombreArcAux());
                    TransformerFactory tf = TransformerFactory.newInstance();
                    Transformer trans = tf.newTransformer();
                    trans.transform(new DOMSource(doc), new StreamResult(sal));

                    entrada.close();
                    sal.close();

                    acondicionarArchivos();

                    entrada = null;
                    sal = null;
                    os = null;

                    break;

            }
        } catch (DocumentException | IOException | InvalidAlgorithmParameterException | KeyException | NoSuchAlgorithmException | MarshalException | XMLSignatureException | ParserConfigurationException | TransformerException | SAXException e) {
            getMensaje().setTexto("Error: " + getNombreArchivoOrigen() + " " + e.getClass().getName() + " " + e.getMessage());
            getMensaje().setTipo(MensajeUI.ERROR);

            if (logSucesos != null) {
                Path destino = Paths.get(getNombreArchivoOrigen());
                logSucesos.println("ERROR|" + destino.toString() + "|" + e.getMessage().replaceAll("\n", " "));
                logSucesos.flush();
            }

        } catch (GeneralSecurityException e) {
            getMensaje().setTexto("Error: " + getNombreArchivoOrigen() + " " + e.getClass().getName() + " " + e.getMessage());
            getMensaje().setTipo(MensajeUI.ERROR);

            if (logSucesos != null) {
                Path destino = Paths.get(getNombreArchivoOrigen());
                logSucesos.println("ERROR|" + destino.toString() + "|" + e.getMessage().replaceAll("\n", " "));
                logSucesos.flush();
            }

        } catch (Exception e) {
            e.printStackTrace();
            getMensaje().setTexto("Error: " + getNombreArchivoOrigen() + " " + e.getClass().getName() + " " + e.getMessage());
            getMensaje().setTipo(MensajeUI.ERROR);

            if (logSucesos != null) {
                Path destino = Paths.get(getNombreArchivoOrigen());
                logSucesos.println("ERROR|" + destino.toString() + "|" + e.getMessage().replaceAll("\n", " "));
                logSucesos.flush();
            }

        } finally {
            if (reader != null) {
                reader.close();
            }

            File f = new File(getNombreArcAux());
            if (f.exists() && !f.isDirectory()) {
                Path origen = Paths.get(getNombreArcAux());
                if (os != null) {
                    os.close();
                }
                Files.delete(origen);
            }

        }
    }


    /**
     * @param conTS
     * @param conOCSP
     * @param pagActual
     * @param totPag
     * @throws IOException
     * @throws DocumentException
     * @throws GeneralSecurityException
     * @throws Exception
     */
    public void firmar(boolean conTS, boolean conOCSP, int pagActual, int totPag) throws IOException, DocumentException, GeneralSecurityException, Exception {

        String extension;
        char[] cContra;
        String sNombreCampo;
        PdfReader reader = null;
        boolean docCertificado;
        long epoch;
        ExternalSignature es;
        FileOutputStream os = null;

        System.out.println(("getAliasCertificado(): " + getAliasCertificado() + " - " + getAlmacenCert().getCertificate(getAliasCertificado())));
        System.out.println(("getAliasCertificado(): " + getAliasCertificado() + " - " + getAlmacenCert().getCertificateChain(getAliasCertificado())));
        Certificate[] chain = getAlmacenCert().getCertificateChain(getAliasCertificado());
        System.out.println("chain: " + chain);
        System.out.println("chain: " + chain.length);
        if (chain == null) {
            throw new Exception("Error: No se ha encontrado el certificado con el alias suministrado.");
        }

        /*
        Calendar cal = null;
        
        Object fails[] = PdfPKCS7.verifyCertificates(chain, getAlmacenCert(), null, cal);
        if (fails == null) {
            System.out.println("Certificates verified against the KeyStore");
        } else {
            System.out.println("Certificate failed: " + fails[1]);
        }
         */
        setCadenaPersonal(chain);

        System.out.println("getContraCertificado(): " + getContraCertificado());
        if (getContraCertificado() == null) {
            cContra = null;
        } else {
            cContra = getContraCertificado().toCharArray();
        }

        try {
            System.out.println("getAlmacenCert().size(): " + getAlmacenCert().size());
            Enumeration en = getAlmacenCert().aliases();
            while (en.hasMoreElements()) {
                System.out.println("EN: " + en.nextElement());
            }
            PrivateKey key = (PrivateKey) getAlmacenCert().getKey(getAliasCertificado(), cContra);

            if (key == null) {
                throw new Exception("Error: No se ha encontrado una clave privada para el certificado solicitado. " + getAliasCertificado());
            }

            PublicKey llPub = chain[0].getPublicKey();

            /*
         if (isCertificadoValido() == false) {
         validarCertificado();
         }
             */
            extension = getExtensionArchivoOrigen();
            if (getNombreArchivoOrigen().lastIndexOf(".") != -1) {
                setNombreArcAux(getNombreArchivoOrigen().substring(0, getNombreArchivoOrigen().lastIndexOf(".")) + "_firmado." + extension.toLowerCase());
            }

            switch (extension) {
                case "PDF":

                    reader = new PdfReader(getNombreArchivoOrigen());
                    AcroFields af = reader.getAcroFields();

                    docCertificado = false;

                    PdfDictionary asd;
                    SignaturePermissions perms = null;

                    for (String nombre : af.getSignatureNames()) {

                        asd = af.getSignatureDictionary(nombre);
                        perms = new SignaturePermissions(asd, perms);
                        if (perms.isCertification()) {
                            docCertificado = true;
                            break;
                        }

                    }

                    if (docCertificado) {
                        throw new Exception("El documento se encuentra certificado y no acepta modificaciones.");
                    }

                    //   reader = new PdfReader(getNombreArchivoOrigen());
                    os = new FileOutputStream(getNombreArcAux());

                    PdfStamper stamper = PdfStamper.createSignature(reader, os, '\0', null, true);

                    PdfSignatureAppearance appearance = stamper.getSignatureAppearance();

                    appearance.setReason(getRazonFirma());
                    appearance.setLocation(getCiudadFirma());

                    org.bouncycastle.asn1.x500.X500Name x500name = new JcaX509CertificateHolder((X509Certificate) chain[0]).getSubject();
                    RDN cn = x500name.getRDNs(BCStyle.CN)[0];

                    epoch = System.currentTimeMillis();
                    sNombreCampo = cn.getFirst().getValue().toString().replaceAll(" ", "") + String.valueOf(epoch / 1000) + String.valueOf(pagActual);

                    if (isCerrarDocumento()) {
                        if (totPag < pagActual) {
                            appearance.setCertificationLevel(PdfSignatureAppearance.CERTIFIED_NO_CHANGES_ALLOWED);
                            sNombreCampo = "Certificado";
                            pagActual = totPag;
                        } else {
                            appearance.setCertificationLevel(PdfSignatureAppearance.NOT_CERTIFIED);
                        }
                    } else {
                        appearance.setCertificationLevel(PdfSignatureAppearance.NOT_CERTIFIED);
                    }

                    System.out.println("Directorio de la Clase: " + FirmadorAccede.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());

                    if (isFirmaVisible()) {
                        if (totPag < pagActual) {
                            pagActual = totPag;
                        }

                        appearance.setCertificate(chain[0]);

                        appearance.setLayer2Font(new Font(BaseFont.createFont(getDirFuenteFirma(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED), 6));

                        appearance.setVisibleSignature(new Rectangle(450, 3, 595, 33), pagActual, sNombreCampo);

                        appearance.setImage(Image.getInstance(getDirImgFirma()));
                        appearance.setImageScale(-1);

                        appearance.setSignatureGraphic(Image.getInstance(getDirImgFirma()));

                        appearance.setLayer2Text("Firmado Digitalmente por: " + cn.getFirst().getValue().toString() + "\nFecha: " + new SimpleDateFormat("yyyy.MM.dd HH:mm:ssZ").format(Calendar.getInstance().getTime()));

                        appearance.setLayer4Text("Firma válida");
                    }

                    switch (getAlmacenCert().getProvider().getName()) {
                        case "SUN":
                        case "SunJSSE":
                            es = new PrivateKeySignature(key, DigestAlgorithms.SHA256, "SunRsaSign");
                            break;

                        case "SunMSCAPI":
                            es = new PrivateKeySignature(key, DigestAlgorithms.SHA256, getAlmacenCert().getProvider().getName());
                            break;

                        default:
                            es = new PrivateKeySignature(key, DigestAlgorithms.SHA256, getAlmacenCert().getProvider().getName());
                            break;
                    }

                    //busca la url en el certificado
                    OcspClient ocsp;
                    // if (conOCSP) {
                    ocsp = new OcspClientBouncyCastle();
                    //}

                    ExternalDigest digest = new BouncyCastleDigest();

                    TSAClient tsc = null;
                    if (conTS) {
                        if (getTsPrior() != null) {
                            tsc = new TSAClientBouncyCastle(getTsPrior());
                        }
                    }

                    MakeSignature.signDetached(appearance, digest, es, chain, getListaCrl(), ocsp, tsc, 0, CryptoStandard.CMS);

                    acumularFirmados();

                    acondicionarArchivos();

                    break;

                case "XML":
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setNamespaceAware(true);
                    DocumentBuilder builder = dbf.newDocumentBuilder();
                    FileInputStream entrada = new FileInputStream(getNombreArchivoOrigen());
                    Document doc = builder.parse(entrada);

                    DOMSignContext dsc = new DOMSignContext(key, doc.getDocumentElement());

                    XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

                    Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA1, null), Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);

                    SignedInfo si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, (C14NMethodParameterSpec) null), fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null), Collections.singletonList(ref));

                    KeyInfoFactory kif = fac.getKeyInfoFactory();

                    KeyValue kv = kif.newKeyValue(llPub);
                    KeyInfo ki = kif.newKeyInfo(Collections.singletonList(kv));

                    XMLSignature signature = fac.newXMLSignature(si, ki);

                    signature.sign(dsc);

                    acumularFirmados();

                    OutputStream sal;
                    sal = new FileOutputStream(getNombreArcAux());
                    TransformerFactory tf = TransformerFactory.newInstance();
                    Transformer trans = tf.newTransformer();
                    trans.transform(new DOMSource(doc), new StreamResult(sal));

                    entrada.close();
                    sal.close();

                    acondicionarArchivos();

                    entrada = null;
                    sal = null;
                    os = null;

                    break;

            }
        } catch (DocumentException | IOException | InvalidAlgorithmParameterException | KeyException | NoSuchAlgorithmException | MarshalException | XMLSignatureException | ParserConfigurationException | TransformerException | SAXException e) {
            getMensaje().setTexto("Error: " + getNombreArchivoOrigen() + " " + e.getClass().getName() + " " + e.getMessage());
            getMensaje().setTipo(MensajeUI.ERROR);

            if (logSucesos != null) {
                Path destino = Paths.get(getNombreArchivoOrigen());
                logSucesos.println("ERROR|" + destino.toString() + "|" + e.getMessage().replaceAll("\n", " "));
                logSucesos.flush();
            }

        } catch (GeneralSecurityException e) {
            getMensaje().setTexto("Error: " + getNombreArchivoOrigen() + " " + e.getClass().getName() + " " + e.getMessage());
            getMensaje().setTipo(MensajeUI.ERROR);

            if (logSucesos != null) {
                Path destino = Paths.get(getNombreArchivoOrigen());
                logSucesos.println("ERROR|" + destino.toString() + "|" + e.getMessage().replaceAll("\n", " "));
                logSucesos.flush();
            }

        } catch (Exception e) {
            e.printStackTrace();
            getMensaje().setTexto("Error: " + getNombreArchivoOrigen() + " " + e.getClass().getName() + " " + e.getMessage());
            getMensaje().setTipo(MensajeUI.ERROR);

            if (logSucesos != null) {
                Path destino = Paths.get(getNombreArchivoOrigen());
                logSucesos.println("ERROR|" + destino.toString() + "|" + e.getMessage().replaceAll("\n", " "));
                logSucesos.flush();
            }

        } finally {
            if (reader != null) {
                reader.close();
            }

            File f = new File(getNombreArcAux());
            if (f.exists() && !f.isDirectory()) {
                Path origen = Paths.get(getNombreArcAux());
                if (os != null) {
                    os.close();
                }
                Files.delete(origen);
            }

        }
    }

    /**
     * @return @throws java.io.IOException
     * @throws com.itextpdf.text.DocumentException
     * @throws java.security.GeneralSecurityException
     */
    public List<Verificacion> verificar() throws IOException, DocumentException, GeneralSecurityException, Exception {
        String extension;
        List<Verificacion> salida;
        Verificacion esta;

        salida = new ArrayList<>();

        KeyStore ks = KeyStore.getInstance(getNombreAlmacen(), getNombreProveedor());
        if (ks == null) {
            throw new Exception("Error: No se ha podido cargar el almacén de certificados.");
        }
        ks.load(null);

        extension = getExtensionArchivoOrigen();
        if (getNombreArchivoOrigen().lastIndexOf(".") != -1) {
            setNombreArcAux(getNombreArchivoOrigen().substring(0, getNombreArchivoOrigen().lastIndexOf(".")) + "_firmado." + extension.toLowerCase());
        }

        switch (extension) {
//            case "PDF":
//                PdfReader reader = new PdfReader(getNombreArchivoOrigen());
//                AcroFields af = reader.getAcroFields();
//                ArrayList<String> nombres = af.getSignatureNames();
//                for (String nombre : nombres) {
//                    esta = new Verificacion();
//
//                    esta.setNombreArchivoOrigen(getNombreArchivoOrigen());
//                    esta.setNombre(nombre);
//                    esta.setCubreTodoElDocumento(af.signatureCoversWholeDocument(nombre));
//                    esta.setNumeroRevision(af.getRevision(nombre));
//                    esta.setTotalRevisiones(af.getTotalRevisions());
//                    PdfPKCS7 pk = af.verifySignature(nombre);
//                    Calendar cal = pk.getSignDate();
//                    Certificate[] pkc = pk.getCertificates();
//                    //Certificate[] pkc = pk.getSignCertificateChain();
//                    esta.setSuscriptor(PdfPKCS7.getSubjectFields(pk.getSigningCertificate()).toString());
//                    esta.setRevisionAlterada(!pk.verify());
//                    Object fails[] = PdfPKCS7.verifyCertificates(pkc, ks, null, cal);
//                    if (fails == null) {
//                        esta.setFalla(null);
//                        esta.setCertificadoVerificado(true);
//                    } else {
//                        esta.setFalla(fails[1].toString());
//                        esta.setCertificadoVerificado(false);
//                    }
//
//                    salida.add(esta);
//
//                }
//
//                break;

            case "XML":
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);

                DocumentBuilder builder = dbf.newDocumentBuilder();
                FileInputStream entrada = new FileInputStream(getNombreArchivoOrigen());
                Document doc = builder.parse(entrada);

                NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
                if (nl.getLength() == 0) {
                    throw new Exception("Error: No se han encontrado nodos de firma.");
                }

                for (int n = 0; n < nl.getLength(); n++) {
                    esta = new Verificacion();

                    DOMValidateContext valContext = new DOMValidateContext(new KeyValueKeySelector(), nl.item(n));

                    XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");

                    XMLSignature signature = factory.unmarshalXMLSignature(valContext);

                    if (signature.validate(valContext) == true) {

                    } else {
                        boolean sv = signature.getSignatureValue().validate(valContext);
                        System.out.println("signature validation status: " + sv);

                        Iterator i = signature.getSignedInfo().getReferences().iterator();
                        for (int j = 0; i.hasNext(); j++) {
                            boolean refValid = ((Reference) i.next()).validate(valContext);
                            System.out.println("ref[" + j + "] validity status: "
                                    + refValid);
                        }
                    }

                    salida.add(esta);

                }

                break;

        }

        return salida;

    }

    private void acondicionarArchivos() throws IOException {
        Path borrar = Paths.get(getNombreArchivoOrigen());
        Files.delete(borrar);

        Path origen = Paths.get(getNombreArcAux());
        Path destino = Paths.get(getNombreArchivoOrigen());

        Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);

        Files.delete(origen);

        System.out.println("FIRMADO: " + destino.toString());

        if (logSucesos != null) {
            logSucesos.println("FIRMADO|" + destino.toString());
            logSucesos.flush();
        }

    }

    private void validarCertificado() throws GeneralSecurityException, Exception {
        KeyStore estanteria;
        Certificate certAlmacen;
        X509Certificate certAlmacenX509;
        PublicKey certAlmacenPB;
        PublicKey certCadenaPB;
        String nombreAlias;
        Enumeration aliasCert;
        int encontrados = 0;

        String[] nombreAlmacenes = {"Windows-MY", "Windows-ROOT"};

        X509Certificate pb = (X509Certificate) getCadenaPersonal()[getCadenaPersonal().length - 1];
        PublicKey cc = pb.getPublicKey();
        try {
            getCadenaPersonal()[0].verify(cc);

            pb = (X509Certificate) getCadenaPersonal()[0];
            pb.checkValidity();

            CICLO:
            for (String nomAl : nombreAlmacenes) {
                estanteria = KeyStore.getInstance(nomAl, getNombreProveedor());
                estanteria.load(null);
                aliasCert = estanteria.aliases();
                while (aliasCert.hasMoreElements()) {
                    nombreAlias = (String) aliasCert.nextElement();
                    certAlmacen = estanteria.getCertificate(nombreAlias);
                    certAlmacenX509 = (X509Certificate) certAlmacen;
                    certAlmacenPB = certAlmacenX509.getPublicKey();
                    for (Certificate certCadena : getCadenaPersonal()) {
                        certCadenaPB = certCadena.getPublicKey();
                        if (certAlmacenPB.toString() == null ? certCadenaPB.toString() == null : certAlmacenPB.toString().equals(certCadenaPB.toString())) {
                            encontrados++;
                            if (encontrados == getCadenaPersonal().length) {
                                break CICLO;
                            }
                        }
                    }
                }
            }
            if (encontrados == getCadenaPersonal().length) {
                setCertificadoValido(true);
            } else {
                throw new Exception("Error: No se han encontrado todos los certificados de la cadena de confianza.");
            }
        } catch (GeneralSecurityException e) {
            throw new Exception("Error: " + e.getMessage());
        } catch (Exception e) {
            throw new Exception("Error: " + e.getMessage());
        }
    }

    public String getExtensionArchivoOrigen() {
        String extension;
        extension = getNombreArchivoOrigen().substring(getNombreArchivoOrigen().lastIndexOf(".") + 1).toUpperCase();
        return extension;
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
     * @return the ciudadFirma
     */
    public String getCiudadFirma() {
        return ciudadFirma;
    }

    /**
     * @param ciudadFirma the ciudadFirma to set
     */
    public void setCiudadFirma(String ciudadFirma) {
        this.ciudadFirma = ciudadFirma;
    }

    /**
     * @return the nombreArchivoOrigen
     */
    public String getNombreArchivoOrigen() {
        return nombreArchivoOrigen;
    }

    /**
     * @param nombreArchivoOrigen the nombreArchivoOrigen to set
     */
    public void setNombreArchivoOrigen(String nombreArchivoOrigen) {
        this.nombreArchivoOrigen = nombreArchivoOrigen;
    }

    /**
     * @return the nombreProveedor
     */
    public String getNombreProveedor() {
        return nombreProveedor;
    }

    /**
     * @param nombreProveedor the nombreProveedor to set
     */
    public void setNombreProveedor(String nombreProveedor) {
        this.nombreProveedor = nombreProveedor;
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

    private void acumularFirmados() {
        cantidadFirmada++;
    }

    /**
     * @return the certificadoValido
     */
    private boolean isCertificadoValido() {
        return certificadoValido;
    }

    /**
     * @param certificadoValido the certificadoValido to set
     */
    private void setCertificadoValido(boolean certificadoValido) {
        this.certificadoValido = certificadoValido;
    }

    /**
     * @return the cadenaPersonal
     */
    private Certificate[] getCadenaPersonal() {
        return cadenaPersonal;
    }

    /**
     * @param cadenaPersonal the cadenaPersonal to set
     */
    private void setCadenaPersonal(Certificate[] cadenaPersonal) {
        this.cadenaPersonal = cadenaPersonal;
    }

    /**
     * @return the urlsCAS
     */
    private String[] getUrlsCAS() {
        return urlsCAS;
    }

    /**
     * @param urlsCAS the urlsCAS to set
     */
    private void setUrlsCAS(String[] urlsCAS) {
        this.urlsCAS = urlsCAS;
    }

    /**
     * @return the nombreArchAux
     */
    private String getNombreArcAux() {
        return nombreArchAux;
    }

    /**
     * @param nombreArcAux the nombreArchAux to set
     */
    private void setNombreArcAux(String nombreArcAux) {
        this.nombreArchAux = nombreArcAux;
    }

    /**
     * @return the almacenCert
     */
    public KeyStore getAlmacenCert() {
        return almacenCert;
    }

    /**
     * @param almacenCert the almacenCert to set
     */
    public void setAlmacenCert(KeyStore almacenCert) {
        this.almacenCert = almacenCert;
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
     * @return the urlOCSP
     */
    public String getUrlOCSP() {
        return urlOCSP;
    }

    /**
     * @param urlOCSP the urlOCSP to set
     */
    public void setUrlOCSP(String urlOCSP) {
        this.urlOCSP = urlOCSP;
    }

    /**
     * @return the idxCrl
     */
    public int getIdxCrl() {
        return idxCrl;
    }

    /**
     * @param idxCrl the idxCrl to set
     */
    public void setIdxCrl(int idxCrl) {
        this.idxCrl = idxCrl;
    }

    /**
     * @return the listaCrl
     */
    public List<CrlClient> getListaCrl() {
        return listaCrl;
    }

    /**
     * @param listaCrl the listaCrl to set
     */
    public void setListaCrl(List<CrlClient> listaCrl) {
        this.listaCrl = listaCrl;
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
     * @return the tsPrior
     */
    public String getTsPrior() {
        return tsPrior;
    }

    /**
     * @param tsPrior the tsPrior to set
     */
    public void setTsPrior(String tsPrior) {
        this.tsPrior = tsPrior;
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

}
