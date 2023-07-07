/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package firmaENG;

/**
 * @author d26151365
 */
public class Verificacion {

    private String nombre;
    private boolean cubreTodoElDocumento;
    private int numeroRevision;
    private int totalRevisiones;
    private String suscriptor;
    private boolean revisionAlterada;
    private boolean certificadoVerificado;
    private String nombreArchivoOrigen;
    private String falla;

    /**
     * @return the nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * @param nombre the nombre to set
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * @return the cubreTodoElDocumento
     */
    public boolean isCubreTodoElDocumento() {
        return cubreTodoElDocumento;
    }

    /**
     * @param cubreTodoElDocumento the cubreTodoElDocumento to set
     */
    public void setCubreTodoElDocumento(boolean cubreTodoElDocumento) {
        this.cubreTodoElDocumento = cubreTodoElDocumento;
    }

    /**
     * @return the numeroRevision
     */
    public int getNumeroRevision() {
        return numeroRevision;
    }

    /**
     * @param numeroRevision the numeroRevision to set
     */
    public void setNumeroRevision(int numeroRevision) {
        this.numeroRevision = numeroRevision;
    }

    /**
     * @return the totalRevisiones
     */
    public int getTotalRevisiones() {
        return totalRevisiones;
    }

    /**
     * @param totalRevisiones the totalRevisiones to set
     */
    public void setTotalRevisiones(int totalRevisiones) {
        this.totalRevisiones = totalRevisiones;
    }

    /**
     * @return the suscriptor
     */
    public String getSuscriptor() {
        return suscriptor;
    }

    /**
     * @param suscriptor the suscriptor to set
     */
    public void setSuscriptor(String suscriptor) {
        this.suscriptor = suscriptor;
    }

    /**
     * @return the revisionAlterada
     */
    public boolean isRevisionAlterada() {
        return revisionAlterada;
    }

    /**
     * @param revisionAlterada the revisionAlterada to set
     */
    public void setRevisionAlterada(boolean revisionAlterada) {
        this.revisionAlterada = revisionAlterada;
    }

    /**
     * @return the certificadoVerificado
     */
    public boolean isCertificadoVerificado() {
        return certificadoVerificado;
    }

    /**
     * @param certificadoVerificado the certificadoVerificado to set
     */
    public void setCertificadoVerificado(boolean certificadoVerificado) {
        this.certificadoVerificado = certificadoVerificado;
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
     * @return the falla
     */
    public String getFalla() {
        return falla;
    }

    /**
     * @param falla the falla to set
     */
    public void setFalla(String falla) {
        this.falla = falla;
    }

}
