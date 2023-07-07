/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package firmapdf;

import firmaENG.Firmador;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.*;

/**
 * @author 20261513650
 */
public class FirmaPDF {

    private static final int BUFFER = 2048;
    /**
     * @param args the command line arguments
     */
    private static List<String> lstDirectorios;
    private static ZipOutputStream out;
    private static PrintWriter salidaBorrado;
    private static String Mensaje;

    public static void main(String[] args) {

        try {
            // TODO code application logic here

            //borrarDirectorios("C:\\Users\\20261513650\\Documents\\SOLE\\01. DOCUMENTACION");
            if (args.length != 14) {
                Logger.getLogger(FirmaPDF.class.getName()).log(Level.SEVERE, "No se han pasado todo los parámetros");
                return;
            }

            String[] vecDirectorio = {args[0]};
            boolean bFirCH;
            boolean bFirVS;

            //borrar si han quedado
            File preBorrar;
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(vecDirectorio[0] + "/NO_BORRADOS.txt")), "ISO-8859-1"))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    System.out.println(line);

                    preBorrar = new File(line);
                    if (preBorrar.exists() && preBorrar.isDirectory()) {
                        preBorrar.delete();
                    }

                }
                // Always close files.

                bufferedReader.close();
            } catch (Exception ex) {
                Logger.getLogger(FirmaPDF.class.getName()).log(Level.SEVERE, null, ex);
            }

            bFirCH = false;
            if (args[1].equalsIgnoreCase("S")) {
                bFirCH = true;
            }

            bFirVS = false;
            if (args[2].equalsIgnoreCase("S")) {
                bFirVS = true;
            }

            Firmador fir = new Firmador();

            //fir.setAliasCertificado("identidad_servicio");
            //fir.setTipoAlmacen("Windows-MY");
            fir.setAliasCertificado(args[5]);
            fir.setTipoAlmacen(args[6]);
            fir.setNombreAlmacen(args[7]);
            fir.setContraAlmacen(args[8]);
            fir.setNombreCiudadFirma("Córdoba, Argentina");
            fir.setRazonFirma("Aseguramiento del no cambio del contenido del documento");

            if (args[13].equalsIgnoreCase("NULL") == false) {
                fir.setContraCertificado(args[13]);
            }
            /*
            if (args[14].equalsIgnoreCase("NULL") == false) {
                fir.setUsuarioProxy(args[14]);
            }

            if (args[15].equalsIgnoreCase("NULL") == false) {
                fir.setContraProxy(args[15]);
            }
             */
            if (args[9].equalsIgnoreCase("S")) {
                fir.setConTS(true);
            } else {
                fir.setConTS(false);
            }

            if (args[10].equalsIgnoreCase("S")) {
                fir.setCerrarDocumento(true);
            } else {
                fir.setCerrarDocumento(false);
            }

            if (args[11].equalsIgnoreCase("NULL") == false) {
                fir.setUrlCrl(args[11].split(";"));
            }

            fir.setDirectorioOrigen(vecDirectorio);

            //fir.setDirImgFirma("logo_gobcordoba.png");
            fir.setDirImgFirma(args[3]);
            //fir.setDirFuenteFirma("Roboto-Italic.ttf");
            fir.setDirFuenteFirma(args[4]);

            fir.setFirmaCadaHoja(bFirCH);
            fir.setFirmaVisible(bFirVS);

            lstDirectorios = new ArrayList<>();

            generarDirectorios(vecDirectorio[0]);

            BufferedWriter bwl = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(vecDirectorio[0] + "/" + args[12] + ".txt", true), "ISO-8859-1"));
            PrintWriter loSalida = new PrintWriter(bwl);
            // Date dIni;
            //dIni = new Date();
            //loSalida.println(" ");
            //loSalida.println("Inicio de proceso " + dIni.toString());

            fir.setLogSucesos(loSalida);

            fir.firmar();

            setMensaje(fir.getMensaje().getTexto());

            lstDirectorios.stream().forEach((dirRaiz) -> {
                ComprimirArchivos(vecDirectorio[0], dirRaiz);
            });

            try {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(vecDirectorio[0] + "/NO_BORRADOS.txt"), "ISO-8859-1"));
                salidaBorrado = new PrintWriter(bw);

                lstDirectorios.stream().forEach((dirRaiz) -> {
                    borrarDirectorios(vecDirectorio[0] + "/" + dirRaiz);
                });

                salidaBorrado.close();

                //dIni = new Date();
                //loSalida.println("Fin de proceso " + dIni.toString());
                loSalida.close();

            } catch (IOException e) {
                //exception handling left as an exercise for the reader
            }

            if (fir.getMensaje() == null) {
                Logger.getLogger(FirmaPDF.class.getName()).log(Level.INFO, "Sin novedad");
            } else {
                Logger.getLogger(FirmaPDF.class.getName()).log(Level.INFO, fir.getMensaje().getTexto());
            }
        } catch (Exception ex) {
            Logger.getLogger(FirmaPDF.class.getName()).log(Level.SEVERE, null, ex);
            setMensaje(ex.getMessage());

        }
    }

    private static void generarDirectorios(String sDirectorio) {

        try {
            String extension;
            String sDirectorioCrear;

            //veo si vienen los ZIPs
            DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(sDirectorio));
            Iterator<Path> iter = stream.iterator();
            while (iter.hasNext()) {
                Path path = iter.next();

                extension = path.getFileName().toString().substring(path.getFileName().toString().lastIndexOf(".") + 1).toUpperCase();
                if (extension.equalsIgnoreCase("ZIP")) {

                    sDirectorioCrear = path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf("."));
                    lstDirectorios.add(sDirectorioCrear);

                    //descomprimir
                    DescomprimirArchivos(sDirectorio + "/" + path.getFileName().toString(), sDirectorio + "/" + sDirectorioCrear);

                }
            }
        } catch (IOException ex) {
            Logger.getLogger(FirmaPDF.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void DescomprimirArchivos(String sArchivo, String sDirectorio) {

        boolean success;
        FileOutputStream fos;
        BufferedOutputStream dest = null;
        BufferedInputStream is = null;
        ZipEntry entry;
        ZipFile zipfile;
        Enumeration e;

        try {

            zipfile = new ZipFile(sArchivo, Charset.forName("CP850"));

            e = zipfile.entries();

            while (e.hasMoreElements()) {
                entry = (ZipEntry) e.nextElement();
                System.out.println("Extracting: " + entry);
                is = new BufferedInputStream(zipfile.getInputStream(entry));
                int count;
                byte data[] = new byte[BUFFER];
                if (!Files.exists(Paths.get(sDirectorio))) {
                    success = (new File(sDirectorio)).mkdirs();
                }

                if (entry.isDirectory()) {
                    success = (new File(sDirectorio + "/" + entry.getName())).mkdirs();
                } else {
                    fos = new FileOutputStream(sDirectorio + "/" + entry.getName());
                    dest = new BufferedOutputStream(fos, BUFFER);
                    while ((count = is.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                    is.close();
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(FirmaPDF.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void ComprimirArchivos(String sDirectRaiz, String sDirectCreado) {

        File arch;
        FileOutputStream dest;
        CheckedOutputStream checksum;
        File f;

        try {
            arch = new File(sDirectRaiz + "/" + sDirectCreado + ".zip");
            if (arch.exists() && !arch.isDirectory()) {
                arch.delete();
            }
            arch = null;

            dest = new FileOutputStream(sDirectRaiz + "/" + sDirectCreado + ".zip");
            checksum = new CheckedOutputStream(dest, new Adler32());
            out = new ZipOutputStream(new BufferedOutputStream(checksum));
            out.setMethod(ZipOutputStream.DEFLATED);
            out.setLevel(Deflater.BEST_COMPRESSION);

            AgregarArchivo(sDirectRaiz, sDirectCreado);

            out.close();
            System.out.println("checksum: " + checksum.getChecksum().getValue());

        } catch (Exception ex) {
            Logger.getLogger(FirmaPDF.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void AgregarArchivo(String sDirectRaiz, String sDirectCreado) throws FileNotFoundException, IOException {

        File arch;
        File f;
        BufferedInputStream origin = null;
        String sDirEntrada;

        byte data[] = new byte[BUFFER];
        // get a list of files from current directory
        f = new File(sDirectRaiz + "/" + sDirectCreado);
        String files[] = f.list();

        for (String file : files) {
            arch = new File(sDirectRaiz + "/" + sDirectCreado + "/" + file);
            if (!arch.isDirectory()) {
                System.out.println("Adding: " + sDirectRaiz + "/" + sDirectCreado + "/" + file);

                FileInputStream fi = new FileInputStream(sDirectRaiz + "/" + sDirectCreado + "/" + file);
                origin = new BufferedInputStream(fi, BUFFER);

                if (!sDirectCreado.contains("/")) {
                    sDirEntrada = file;
                } else {
                    sDirEntrada = sDirectCreado.substring(sDirectCreado.indexOf("/") + 1) + "/" + file;
                }

                ZipEntry entry = new ZipEntry(sDirEntrada);
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();

                arch = new File(sDirectRaiz + "/" + sDirectCreado + "/" + file);
                if (arch.exists() && !arch.isDirectory()) {
                    arch.delete();
                }
            } else {
                AgregarArchivo(sDirectRaiz, sDirectCreado + "/" + file);
            }

        }

    }

    private static void borrarDirectorios(String sDirectorioPadre) {

        File arch;

        arch = new File(sDirectorioPadre);
        for (String f : arch.list()) {
            borrarDirectorios(sDirectorioPadre + "/" + f);
        }

        if (arch.exists() && arch.isDirectory()) {
            if (arch.delete()) {
                Logger.getLogger(FirmaPDF.class.getName()).log(Level.INFO, "Se borr\u00f3 {0}", arch.getAbsolutePath());
            } else {
                salidaBorrado.println(sDirectorioPadre);
            }

        }

    }

    /**
     * @return the Mensaje
     */
    public static String getMensaje() {
        return Mensaje;
    }

    /**
     * @param aMensaje the Mensaje to set
     */
    public static void setMensaje(String aMensaje) {
        Mensaje = aMensaje;
    }

}
