package com.secnium.iast.core.util;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * @author dongzhiyong@huoxian.cn
 */
public class IASTTrustManager implements X509TrustManager {
    @Override
    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return new java.security.cert.X509Certificate[]{};
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
    }
}
