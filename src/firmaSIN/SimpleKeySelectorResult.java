/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package firmaSIN;

import javax.xml.crypto.KeySelectorResult;
import java.security.Key;
import java.security.PublicKey;

/**
 * @author d26151365
 */
public final class SimpleKeySelectorResult implements KeySelectorResult {

    private Key clave;

    SimpleKeySelectorResult(PublicKey pk) {
        setClave((Key) pk);
    }

    @Override
    public Key getKey() {
        return getClave();
    }

    /**
     * @return the clave
     */
    public Key getClave() {
        return clave;
    }

    /**
     * @param clave the clave to set
     */
    public void setClave(Key clave) {
        this.clave = clave;
    }

}
