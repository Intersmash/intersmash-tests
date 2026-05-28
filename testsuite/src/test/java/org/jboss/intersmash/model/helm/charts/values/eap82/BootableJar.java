
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
 * Configuration specific to Bootable Jar Build (applicable only if build mode is set to bootable-jar)
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "builderImage"
})
@Generated("jsonschema2pojo")
public class BootableJar {

    /**
     * The JDK Builder image
     * 
     */
    @JsonProperty("builderImage")
    @JsonPropertyDescription("The JDK Builder image")
    private String builderImage;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * The JDK Builder image
     * 
     */
    @JsonProperty("builderImage")
    public String getBuilderImage() {
        return builderImage;
    }

    /**
     * The JDK Builder image
     * 
     */
    @JsonProperty("builderImage")
    public void setBuilderImage(String builderImage) {
        this.builderImage = builderImage;
    }

    public BootableJar withBuilderImage(String builderImage) {
        this.builderImage = builderImage;
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

    public BootableJar withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(BootableJar.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("builderImage");
        sb.append('=');
        sb.append(((this.builderImage == null)?"<null>":this.builderImage));
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
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof BootableJar) == false) {
            return false;
        }
        BootableJar rhs = ((BootableJar) other);
        return (((this.builderImage == rhs.builderImage)||((this.builderImage!= null)&&this.builderImage.equals(rhs.builderImage)))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

}
