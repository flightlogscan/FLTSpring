package com.flt.fltspring.secret;

import com.google.gson.Gson;

import static com.flt.fltspring.secret.AWSSecretRetriever.getSecretString;

public class AzureSecretRetriever {
    public static String getSecret() {

        String secretName = "flight-log-scan-document-ai-secret";
        String secret = getSecretString(secretName);

        Gson gson = new Gson();
        final AzureAPIKeySecret clientSecret;
        clientSecret = gson.fromJson(secret, AzureAPIKeySecret.class);

        System.out.println("clientSecret: " + clientSecret.getClientSecret());
        return clientSecret.getClientSecret();
    }
}
