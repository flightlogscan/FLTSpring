package com.flt.fltspring.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureAPIKeySecret {

    @SerializedName("ClientSecret")
    private String clientSecret;
}
