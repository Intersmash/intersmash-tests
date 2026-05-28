
package org.jboss.intersmash.model.helm.charts.values.eap82;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * EAP S2I images for JDK 25
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "builderImage",
    "runtimeImage"
})
@Generated("jsonschema2pojo")
public class Jdk25 {

    /**
     * EAP S2I Builder image for JDK 25
     * 
     */
    @JsonProperty("builderImage")
    @JsonPropertyDescription("EAP S2I Builder image for JDK 25")
    private String builderImage;
    /**
     * EAP S2I Runtime image for JDK 25
     * 
     */
    @JsonProperty("runtimeImage")
    @JsonPropertyDescription("EAP S2I Runtime image for JDK 25")
    private String runtimeImage;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * EAP S2I Builder image for JDK 25
     * 
     */
    @JsonProperty("builderImage")
    public String getBuilderImage() {
        return builderImage;
    }

    /**
     * EAP S2I Builder image for JDK 25
     * 
     */
    @JsonProperty("builderImage")
    public void setBuilderImage(String builderImage) {
        this.builderImage = builderImage;
    }

    public Jdk25 withBuilderImage(String builderImage) {
        this.builderImage = builderImage;
        return this;
    }

    /**
     * EAP S2I Runtime image for JDK 25
     * 
     */
    @JsonProperty("runtimeImage")
    public String getRuntimeImage() {
        return runtimeImage;
    }

    /**
     * EAP S2I Runtime image for JDK 25
     * 
     */
    @JsonProperty("runtimeImage")
    public void setRuntimeImage(String runtimeImage) {
        this.runtimeImage = runtimeImage;
    }

    public Jdk25 withRuntimeImage(String runtimeImage) {
        this.runtimeImage = runtimeImage;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public Jdk25 withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Jdk25 .class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("builderImage");
        sb.append('=');
        sb.append(((this.builderImage == null)?"<null>":this.builderImage));
        sb.append(',');
        sb.append("runtimeImage");
        sb.append('=');
        sb.append(((this.runtimeImage == null)?"<null>":this.runtimeImage));
        sb.append(',');
        sb.append("additionalProperties");
        sb.append('=');
        sb.append(((this.additionalProperties == null)?"<null>":this.additionalProperties));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.builderImage == null)? 0 :this.builderImage.hashCode()));
        result = ((result* 31)+((this.runtimeImage == null)? 0 :this.runtimeImage.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Jdk25) == false) {
            return false;
        }
        Jdk25 rhs = ((Jdk25) other);
        return ((((this.builderImage == rhs.builderImage)||((this.builderImage!= null)&&this.builderImage.equals(rhs.builderImage)))&&((this.runtimeImage == rhs.runtimeImage)||((this.runtimeImage!= null)&&this.runtimeImage.equals(rhs.runtimeImage))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

}
