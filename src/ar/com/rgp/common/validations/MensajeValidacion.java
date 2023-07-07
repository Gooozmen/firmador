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

import java.util.Objects;

/**
 *
 * @author Angelo Wolf angelowolf21@gmail.com
 */
public class MensajeValidacion {

    private String campo;
    private String mensajeError;

    public MensajeValidacion() {
    }

    public MensajeValidacion(String campo, String mensajeError) {
        this.campo = campo;
        this.mensajeError = mensajeError;
    }

    public String getCampo() {
        return campo;
    }

    public void setCampo(String campo) {
        this.campo = campo;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public void setMensajeError(String mensajeError) {
        this.mensajeError = mensajeError;
    }

    @Override
    public String toString() {
        return "MensajeValidacion{" + "campo=" + campo + ", mensajeError=" + mensajeError + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.campo);
        hash = 79 * hash + Objects.hashCode(this.mensajeError);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MensajeValidacion other = (MensajeValidacion) obj;
        if (!Objects.equals(this.campo, other.campo)) {
            return false;
        }
        if (!Objects.equals(this.mensajeError, other.mensajeError)) {
            return false;
        }
        return true;
    }

}
