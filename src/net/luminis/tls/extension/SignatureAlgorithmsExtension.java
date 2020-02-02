package net.luminis.tls.extension;

import net.luminis.tls.DecodeErrorException;
import net.luminis.tls.TlsConstants;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.luminis.tls.TlsConstants.SignatureScheme.*;

/**
 * The TLS supported groups extension.
 * See https://tools.ietf.org/html/rfc8446#section-4.2.3
 * "Note: This enum is named "SignatureScheme" because there is already a "SignatureAlgorithm" type in TLS 1.2,
 * which this replaces.  We use the term "signature algorithm" throughout the text."
 */
public class SignatureAlgorithmsExtension extends Extension {

    private List<TlsConstants.SignatureScheme> algorithms = new ArrayList<>();

    public SignatureAlgorithmsExtension() {
        algorithms = List.of(new TlsConstants.SignatureScheme[] {
                ecdsa_secp256r1_sha256,
                rsa_pss_rsae_sha256,
                rsa_pkcs1_sha256,
                ecdsa_secp384r1_sha384,
                rsa_pss_rsae_sha384,
                rsa_pkcs1_sha384,
                rsa_pss_rsae_sha512,
                rsa_pkcs1_sha512,
                rsa_pkcs1_sha1
        });
    }

    public SignatureAlgorithmsExtension(TlsConstants.SignatureScheme... signatureAlgorithms) {
        this.algorithms = List.of(signatureAlgorithms);
    }

    public SignatureAlgorithmsExtension(ByteBuffer buffer) throws DecodeErrorException {
        int extensionDataLength = parseExtensionHeader(buffer, TlsConstants.ExtensionType.signature_algorithms);
        int supportedAlgorithmsLength = buffer.getShort();
        if (extensionDataLength != 2 + supportedAlgorithmsLength) {
            throw new DecodeErrorException("inconsistent length");
        }
        if (supportedAlgorithmsLength % 2 != 0) {
            throw new DecodeErrorException("invalid group length");
        }

        for (int i = 0; i < supportedAlgorithmsLength; i += 2) {
            int supportedAlgorithmsBytes = buffer.getShort() % 0xffff;
            TlsConstants.SignatureScheme algorithm = Arrays.stream(TlsConstants.SignatureScheme.values())
                    .filter(item -> item.value == supportedAlgorithmsBytes)
                    .findFirst()
                    .orElseThrow(() -> new DecodeErrorException("invalid signature scheme value"));
            algorithms.add(algorithm);
        }
    }

    @Override
    public byte[] getBytes() {
        int extensionLength = 2 + algorithms.size() * 2;
        ByteBuffer buffer = ByteBuffer.allocate(4 + extensionLength);
        buffer.putShort(TlsConstants.ExtensionType.signature_algorithms.value);
        buffer.putShort((short) extensionLength);  // Extension data length (in bytes)

        buffer.putShort((short) (algorithms.size() * 2));
        for (TlsConstants.SignatureScheme namedGroup: algorithms) {
            buffer.putShort(namedGroup.value);
        }

        return buffer.array();
    }

    public List<TlsConstants.SignatureScheme> getSignatureAlgorithms() {
        return algorithms;
    }

}
