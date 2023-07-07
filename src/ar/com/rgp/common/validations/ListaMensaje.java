/**
 * MIT License
 *
 * Copyright (c) 2017 Wolf Angelo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ar.com.rgp.common.validations;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Angelo Wolf angelowolf21@gmail.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListaMensaje {

    private List<MensajeError> listaErrores;
    private List<MensajeValidacion> listaValidaciones;

    public ListaMensaje() {
        this.listaErrores = new ArrayList<>();
        this.listaValidaciones = new ArrayList<>();
    }

    public ListaMensaje(List<MensajeError> listaErrores, List<MensajeValidacion> listaValidaciones) {
        this.listaErrores = listaErrores;
        this.listaValidaciones = listaValidaciones;
    }

    public List<MensajeError> getListaErrores() {
        return listaErrores;
    }

    public void setListaErrores(List<MensajeError> listaErrores) {
        this.listaErrores = listaErrores;
    }

    public List<MensajeValidacion> getListaValidaciones() {
        return listaValidaciones;
    }

    public void setListaValidaciones(List<MensajeValidacion> listaValidaciones) {
        this.listaValidaciones = listaValidaciones;
    }

    /**
     * Agrega el objeto MensajeError a la lista de mensajes de errores.
     *
     * @param mensajeError
     */
    public void agregarMensajeError(MensajeError mensajeError) {
        this.listaErrores.add(mensajeError);
    }

    /**
     * Crea un objeto MensajeError y lo agrega a la lista de mensajes de
     * errores.
     *
     * @param mensajeError
     */
    public void agregarMensajeError(String mensajeError) {
        this.listaErrores.add(new MensajeError(mensajeError));
    }

    /**
     * Agrega el objeto MensajeValidacion a la lista de mensajes de validacion.
     *
     * @param mensajeValidacion Objeto a agegar.
     */
    public void agregarMensajeValidacion(MensajeValidacion mensajeValidacion) {
        this.listaValidaciones.add(mensajeValidacion);
    }

    /**
     * Crea un objeto MensajeValidacion y lo agrega a la lista de mensajes de
     * validacion.
     *
     * @param campo El nombre del campo.
     * @param mensajeError El mensaje de error a mostrar.
     */
    public void agregarMensajeValidacion(String campo, String mensajeError) {
        this.listaValidaciones.add(new MensajeValidacion(campo, mensajeError));
    }

    /**
     * Limpia todos los errores.
     */
    public void limpiarMensajeError() {
        this.listaErrores.clear();
    }

    /**
     * limpia todos los errores de validacion.
     */
    public void limpiarMensajeValidacion() {
        this.listaValidaciones.clear();
    }

    /**
     * Limpia todos los errores y errores de validacion.
     */
    public void limpiarMensajes() {
        this.limpiarMensajeError();
        this.limpiarMensajeValidacion();
    }

    /**
     * Verifica si contiene errores de validacion o errores.
     *
     * @return True si NO tiene errores. false lo contrario.
     */
    @JsonProperty("empty")
    public boolean isEmpty() {
        return this.listaErrores.isEmpty() && this.listaValidaciones.isEmpty();
    }

    /**
     * Agrega los mensajes de error y validacion a esta ListaMensaje
     *
     * @param listaMensaje
     */
    public void agregarListaMensaje(ListaMensaje listaMensaje) {
        this.listaErrores.addAll(listaMensaje.getListaErrores());
        this.listaValidaciones.addAll(listaMensaje.getListaValidaciones());
    }
}