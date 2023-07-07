package ar.com.rgp.main;

public class FirmadorException extends Exception {

    public String mensaje;
    public Long codigo;


    public FirmadorException(Long idException, String excMensaje) {
        this.codigo = idException;
        this.mensaje = excMensaje;
    }

    public String getLocalizedMessage() {
        return getMessage();
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    @Override
    public String getMessage() {
        return "FRM-[" + this.codigo + "]: " + this.mensaje;
    }

    @Override
    public String toString() {
        return getMessage();
    }


}
