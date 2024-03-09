package com.flt.fltspring.secret;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureAPIKeySecret {

    @SerializedName("ClientSecret")
    private String clientSecret;
}
