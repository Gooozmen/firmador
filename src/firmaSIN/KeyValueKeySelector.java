/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package firmaSIN;

import javax.xml.crypto.*;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import java.security.KeyException;
import java.security.PublicKey;
import java.util.List;

/**
 * @author d26151365
 */
public class KeyValueKeySelector extends KeySelector {

    static boolean algEquals(String algURI, String algName) {
        if (algName.equalsIgnoreCase("DSA")
                && algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1)) {
            return true;
        } else if (algName.equalsIgnoreCase("RSA")
                && algURI.equalsIgnoreCase(SignatureMethod.RSA_SHA1)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public KeySelectorResult select(KeyInfo keyInfo,
                                    KeySelector.Purpose purpose,
                                    AlgorithmMethod method,
                                    XMLCryptoContext context)
            throws KeySelectorException {

        if (keyInfo == null) {
            throw new KeySelectorException("Null KeyInfo object!");
        }
        SignatureMethod sm = (SignatureMethod) method;
        List list = keyInfo.getContent();

        for (Object list1 : list) {
            XMLStructure xmlStructure = (XMLStructure) list1;
            if (xmlStructure instanceof KeyValue) {
                PublicKey pk = null;
                try {
                    pk = ((KeyValue) xmlStructure).getPublicKey();
                } catch (KeyException ke) {
                    throw new KeySelectorException(ke);
                }
                // make sure algorithm is compatible with method
                if (algEquals(sm.getAlgorithm(),
                        pk.getAlgorithm())) {
                    return new SimpleKeySelectorResult(pk);
                }
            }
        }
        throw new KeySelectorException("No KeyValue element found !");
    }
}
