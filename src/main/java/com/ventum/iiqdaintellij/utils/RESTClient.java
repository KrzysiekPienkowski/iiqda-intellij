package com.ventum.iiqdaintellij.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.ventum.iiqdaintellij.Exceptions.ConnectionException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.message.BasicNameValuePair;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings("rawtypes")

public class RESTClient {


    private static final boolean DEBUG_REST = Registry.is(IIQDAConstants.PLUGIN_ID + "/debug/RESTClient", true);

    protected String iUrl;
    protected String iUsername;
    protected String iPassword;
    protected int timeout = 1000;
    protected Project fProject = null;


    private CookieStore cookieStore;

    private HttpClientContext localContext;

    protected boolean bDoSubstOnCmp;

    public enum HTTPMethod {
        GET,
        POST,
        PUT,
        PATCH;
    }

    ;

    public RESTClient() {
        this.iUrl = null;
        this.iUsername = null;
        this.iPassword = null;
    }

    public RESTClient(int timeout) {
        this();
        this.timeout = timeout;
    }

    public RESTClient(String url, String user, String pass) {

        this.iUrl = url;
        this.iUsername = user;
        this.iPassword = pass;
    }

    public RESTClient(String url, String user, String pass, int timeout) {
        this(url, user, pass);
        this.timeout = timeout;
    }

    protected CloseableHttpClient getPreEmptiveClient(URI hostUri, Project hostProject) {

        VirtualFile projectKSFile = null;
        if (hostProject != null) {
            projectKSFile = hostProject.getBaseDir().findFileByRelativePath(".keystore");
        }

        return getPreEmptiveClient(hostUri, iUsername, iPassword, projectKSFile);

    }

    private CloseableHttpClient getPreEmptiveClient(URI hostUri, String username, String password, VirtualFile customTrustStore) {

        CredentialsProvider credsProvider = null;
        HttpHost target = null;

        if (username != null && password != null) {
            target = new HttpHost(hostUri.getHost(), hostUri.getPort(), hostUri.getScheme());

            credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(target.getHostName(), target.getPort()),
                    new UsernamePasswordCredentials(username, password));
        }


        FileInputStream f = null;
        SSLConnectionSocketFactory sslsf = null;
        try {

            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            f = new FileInputStream(System.getProperty("java.home") + "/lib/security/cacerts");
            keystore.load(f, "changeit".toCharArray());

            SSLContext mSSLContextInstance = SSLContext.getInstance("TLS");
            TrustManager trustManager = new IIQDATrustManager(keystore, customTrustStore);
            TrustManager[] tms = new TrustManager[]{trustManager};
            mSSLContextInstance.init(null, tms, new SecureRandom());

            sslsf = new SSLConnectionSocketFactory(mSSLContextInstance);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } finally {
            try {
                f.close();
            } catch (Exception e) {
            }
        }

        //    X509HostnameVerifier hostnameVerifier = new AllowAllHostnameVerifier();
        RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(timeout)
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setCircularRedirectsAllowed(true)
                .build();

        HttpClientBuilder bldr = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setDefaultRequestConfig(config)
                .setRedirectStrategy(new LaxRedirectStrategy());
        //   		.setHostnameVerifier(hostnameVerifier)
        if (credsProvider != null) {
            bldr.setDefaultCredentialsProvider(credsProvider);
        }
        CloseableHttpClient httpclient = bldr.build();

        if (target != null) {
            // Create AuthCache instance
            AuthCache authCache = new BasicAuthCache();
            // Generate BASIC scheme object and add it to the local
            // auth cache
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(target, basicAuth);

            localContext = HttpClientContext.create();
            localContext.setAuthCache(authCache);
        }

        return httpclient;
    }

    public Object doGenericGet(String url) throws ConnectionException {
        return doGenericGet(url, (String) null, (String) null);
    }

    public Object doGenericGet(String url, String username, String password) throws ConnectionException {
        return doGenericGet(url, null, username, password);
    }

    public Object doGenericGet(String url, String username, String password, Class returnType) throws ConnectionException {
        return doGenericGet(url, null, username, password, returnType);
    }

    public Object doGenericGet(String url, Map<String, String> headers, String username, String password) throws ConnectionException {
        return doGenericGet(url, null, username, password, Map.class);
    }

    public Object doGenericGet(String url, Map<String, String> headers, Class returnType) throws ConnectionException {
        return doGenericGet(url, headers, null, null, returnType);
    }


    public Object doGenericJSONPost(String url, Map<String, String> args, String username, String password) throws ConnectionException {
        return doGenericJSONPost(url, args, username, password, null);
    }

    public Object doGenericJSONPost(String url, Map<String, String> args, String username, String password, Class returnType) throws ConnectionException {

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = null;
        try {
            // Serialize the object array into a JSON string
            jsonString = objectMapper.writeValueAsString(args);

            // Print the serialized JSON string
            System.out.println("Serialized JSON string: " + jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return doGenericJSONPost(url, jsonString, username, password, returnType);
    }

    private UrlEncodedFormEntity toFormUrlEncodedContent(Map<String, String> args) throws UnsupportedEncodingException {
        List<NameValuePair> nvc = new ArrayList<NameValuePair>();
        for (Entry<String, String> itm : args.entrySet()) {
            nvc.add(new BasicNameValuePair(itm.getKey(), itm.getValue()));
        }
        return new UrlEncodedFormEntity(nvc);
    }

    public Object doGenericFormURLEncodedPost(String url, Map<String, String> args) throws ConnectionException {
        return doGenericFormURLEncodedPost(url, args, null, null);
    }

    public Object doGenericFormURLEncodedPost(String url, Map<String, String> args, String username, String password) throws ConnectionException {
        return doGenericFormURLEncodedPost(url, args, null, username, password);
    }

    public Object doGenericFormURLEncodedPost(String url, Map<String, String> args, Map<String, String> headers) throws ConnectionException {
        return doGenericFormURLEncodedPost(url, args, headers, null, null);
    }

    public Object doGenericFormURLEncodedPost(String url, Map<String, String> args, Map<String, String> headers, String username, String password) throws ConnectionException {


        if (headers == null) {
            headers = new HashMap<String, String>();
        }

        headers.put("accept", "application/json");
        try {
            UrlEncodedFormEntity entity = toFormUrlEncodedContent(args);
            return doIDNRestCall(HTTPMethod.POST, url, headers, entity, username, password, null);
        } catch (UnsupportedEncodingException ue) {
            throw new ConnectionException("Unsupported Encoding");
        }

    }

    public Object doGenericJSONPost(String url, String jsonString, String username, String password, Class returnType) throws ConnectionException {

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("accept", "application/json");

        try {
            StringEntity entity = new StringEntity(jsonString);
            return doIDNRestCall(HTTPMethod.POST, url, headers, entity, username, password, returnType);
        } catch (UnsupportedEncodingException ue) {
            throw new ConnectionException("Unsupported Encoding");
        }


    }

    public Object doGenericGet(String url, Map<String, String> headers, String username, String password, Class returnType) throws ConnectionException {

        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        headers.put("accept", "application/json");

        return doIDNRestCall(HTTPMethod.GET, url, headers, null, username, password, returnType);

    }

    private Object doIDNRestCall(HTTPMethod method, String url, Map<String, String> headers, HttpEntity entity, String username, String password, Class returnType) throws ConnectionException {

        if (returnType == null) returnType = Map.class; // default to Map (like a JSON structure)

        URI hostUri = null;
        try {
            hostUri = new URI(url);
        } catch (URISyntaxException ue) {
            throw new ConnectionException("Invalid URI: " + url);
        }


        CloseableHttpClient httpclient = getPreEmptiveClient(hostUri, username, password, null);

        HttpUriRequest request = null;
        switch (method) {
            case GET:
                request = new HttpGet(hostUri);
                break;
            case POST:
                request = new HttpPost(hostUri);
                break;
            case PUT:
                request = new HttpPut(hostUri);
                break;
            case PATCH:
                request = new HttpPatch(hostUri);
                break;
            default:
                throw new ConnectionException("Unknown method '" + method.toString() + " : " + url);
        }

        // Set any headers
        if (headers != null) {
            for (Entry<String, String> itm : headers.entrySet()) {
                request.setHeader(itm.getKey(), itm.getValue());
            }
        }

        // some kind of payload
        if (entity != null) {
            ((HttpEntityEnclosingRequest) request).setEntity(entity);
        }
        // make sure we have some kind of context to hold the cookies. Mmmmmm, cooookies
        if (localContext == null) {
            localContext = HttpClientContext.create();
        }
        if (cookieStore != null) {
            localContext.setCookieStore(this.cookieStore);
        }

        try {
            CloseableHttpResponse response = httpclient.execute(request, localContext);
            cookieStore = localContext.getCookieStore();
            System.out.println("Cookies: " + cookieStore);

            BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));

            String output = "";

            String line;
            while ((line = br.readLine()) != null) {
                output += line + "\n";
            }
            if (response.getStatusLine().getStatusCode() == 200) {
                if (response.getEntity().getContentType().getValue().startsWith("application/json")) {
                    // Deserialize will not allow us to return JSON as a string - so for this special
                    // case we'll skip the deserializing
                    Object retMap = output;
                    if (returnType != String.class) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            // Deserialize the string into a Map<String, Object>
                            retMap = objectMapper.readValue(output, new TypeReference<Map<String, Object>>() {
                            });

                            // Print the deserialized map
                            System.out.println("Deserialized Map: " + retMap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return retMap;
                } else if (response.getEntity().getContentType().getValue().startsWith("text/html")) {
                    return output;
                } else {
                    throw new ConnectionException("Unexpected content type " + response.getEntity().getContentType());
                }
            } else {
                System.out.println("fail: payload=\n" + output);
                throw new ConnectionException(method.toString() + " Failed: reason=" + response.getStatusLine().getStatusCode());
            }

        } catch (ClientProtocolException e) {
            throw new ConnectionException("Connection failed - " + e, e);
        } catch (IOException e) {
            throw new ConnectionException("Connection failed - " + e, e);
        }

    }
}

