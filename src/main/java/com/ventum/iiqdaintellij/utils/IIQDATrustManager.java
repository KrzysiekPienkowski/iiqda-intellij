package com.ventum.iiqdaintellij.utils;

import com.intellij.openapi.vfs.VirtualFile;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

public class IIQDATrustManager implements X509TrustManager {

    private KeyStore keystore;
    private VirtualFile fKSFile;

    public IIQDATrustManager(KeyStore keystore, VirtualFile projectKSFile) {
        this.keystore = keystore;
        this.fKSFile = projectKSFile;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] paramArrayOfX509Certificate,
                                   String paramString) throws CertificateException {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certChain,
                                   String paramString) throws CertificateException {
        X509Certificate cert = certChain[certChain.length - 1];
        boolean hasCert = false;
        hasCert = hasCert(cert, keystore);
        if (!hasCert) {
            if (fKSFile.exists()) {
                try {
                    KeyStore projectKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
                    InputStream is = fKSFile.getInputStream();
                    projectKeystore.load(is, "changeit".toCharArray());
                    hasCert = hasCert(cert, projectKeystore);
                } catch (KeyStoreException | NoSuchAlgorithmException
                         | IOException e) {
                    throw new CertificateException("Error checking project keystore " + e);
                }
            }
        }
        if (!hasCert) {
            //   queryUserForAcceptance(cert, fKSFile);
        }
    }

    private boolean hasCert(X509Certificate cert, KeyStore keystore) {
        boolean hasCert = false;
        try {
            Enumeration<String> aliases = keystore.aliases();
            while (aliases.hasMoreElements() && !hasCert) {
                String alias = aliases.nextElement();
                try {
                    cert.verify(keystore.getCertificate(alias).getPublicKey());
                    hasCert = true;
                    break;
                } catch (Exception e) {
                }
            }
        } catch (KeyStoreException e) {

        }
        return hasCert;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }

}
