package cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.generators;

import cn.edu.buaa.crypto.access.AccessControlEngine;
import cn.edu.buaa.crypto.access.AccessControlParameter;
import cn.edu.buaa.crypto.access.UnsatisfiedAccessControlException;
import cn.edu.buaa.crypto.algebra.generators.PairingDecapsulationGenerator;
import cn.edu.buaa.crypto.algebra.generators.PairingDecryptionGenerator;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.genparams.KPABEGPSW06aDecryptionGenerationParameter;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.serparams.KPABEGPSW06aCipherSerParameter;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.serparams.KPABEGPSW06aPublicKeySerParameter;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.serparams.KPABEGPSW06aSecretKeySerParameter;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.security.InvalidParameterException;
import java.util.Map;

/**
 * Created by Weiran Liu on 2016/11/18.
 *
 * Goyal-Pandey-Sahai-Waters small-universe KP-ABE decryption generator.
 */
public class KPABEGPSW06aDecryptionGenerator implements PairingDecryptionGenerator {
    private KPABEGPSW06aDecryptionGenerationParameter params;

    public void init(CipherParameters params) {
        this.params = (KPABEGPSW06aDecryptionGenerationParameter)params;
    }

    public Element recoverMessage() throws InvalidCipherTextException {
        KPABEGPSW06aPublicKeySerParameter publicKeyParameters = this.params.getPublicKeyParameters();
        KPABEGPSW06aSecretKeySerParameter secretKeyParameters = this.params.getSecretKeyParameters();
        KPABEGPSW06aCipherSerParameter ciphertextParameters = this.params.getCiphertextParameters();
        AccessControlParameter accessControlParameter = secretKeyParameters.getAccessControlParameter();
        AccessControlEngine accessControlEngine = this.params.getAccessControlEngine();
        String[] attributes = this.params.getAttributes();
        Pairing pairing = PairingFactory.getPairing(publicKeyParameters.getParameters());
        try {
            Map<String, Element> omegaElementsMap = accessControlEngine.reconstructOmegas(pairing, attributes, accessControlParameter);
            Element sessionKey = pairing.getGT().newOneElement().getImmutable();
            for (String attribute : omegaElementsMap.keySet()) {
                int index = Integer.parseInt(attribute);
                if (index >= publicKeyParameters.getMaxAttributesNum() || index < 0) {
                    throw new InvalidParameterException("Rho index greater than or equal to the max number of attributes supported");
                }
                Element D = secretKeyParameters.getDsAt(String.valueOf(index));
                Element E = ciphertextParameters.getEsAt(String.valueOf(index));
                Element lambda = omegaElementsMap.get(attribute);
                sessionKey = sessionKey.mul(pairing.pairing(D, E).powZn(lambda)).getImmutable();
            }
            return ciphertextParameters.getEPrime().div(sessionKey).getImmutable();
        } catch (UnsatisfiedAccessControlException e) {
            throw new InvalidCipherTextException("Attributes associated with the ciphertext do not satisfy access policy associated with the secret key.");
        }
    }
}