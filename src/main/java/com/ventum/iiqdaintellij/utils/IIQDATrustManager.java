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

//    private void queryUserForAcceptance(X509Certificate cert, VirtualFile destKSFile) throws CertificateException {
//
//        CertQuery certQuery = new CertQuery(cert);
//        PlatformUI.getWorkbench().getDisplay().syncExec(certQuery);
//
//        int status = certQuery.getStatus();
//
//        switch (status) {
//            case 0:
//                return;
//            case 1:
//                saveApprovedCert(cert);
//                return; // TODO: Store this cert
//            case 2:
//            default:
//                throw new CertificateException("not in keystore");
//        }
//    }


//    private final class CertQuery implements Runnable {
//        private final X509Certificate last;
//        private int status;
//
//        private CertQuery(X509Certificate last) {
//            this.last = last;
//        }
//
//        public void run() {
//
//            Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
//
//            String dlgTitle = "Untrusted CA Cert";
//            String dlgMessage = "Trust this Certificate?\n\n" +
//                    "Issuer: " + last.getIssuerDN() + "\n" +
//                    "Valid from: " + last.getNotBefore() + "\n" +
//                    "Valid to: " + last.getNotAfter() + "\n";
//            String[] dlgButtons = {"Just Once", "Always", "No"};
//            MessageDialog dlg = new MessageDialog(activeShell, dlgTitle, null,
//                    dlgMessage, MessageDialog.QUESTION, dlgButtons, 2);
//            status = dlg.open();
//            if (DEBUG_TRUST) {
//                CorePlugin.logDebug("status=" + status);
//            }
//        }
//
//        public int getStatus() {
//            return status;
//        }
//    }
}
