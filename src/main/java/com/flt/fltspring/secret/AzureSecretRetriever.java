package com.flt.fltspring.secret;

import com.google.gson.Gson;

import static com.flt.fltspring.secret.AWSSecretRetriever.getSecretString;

public class AzureSecretRetriever {
    public static String getSecret() {

        String secretName = "lanceinstance_formrecognizer_key";
        String secret = getSecretString(secretName);

        Gson gson = new Gson();
        AzureAPIKeySecret clientSecret = gson.fromJson(secret, AzureAPIKeySecret.class);

        return clientSecret.getClientSecret();
    }
}
