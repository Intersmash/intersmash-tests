
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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "image",
    "build",
    "deploy"
})
@Generated("jsonschema2pojo")
public class HelmEap82Release {

    @JsonProperty("image")
    private Image image;
    /**
     * Configuration to build the application image
     * 
     */
    @JsonProperty("build")
    @JsonPropertyDescription("Configuration to build the application image")
    private Build build;
    /**
     * Configuration to deploy the application
     * 
     */
    @JsonProperty("deploy")
    @JsonPropertyDescription("Configuration to deploy the application")
    private Deploy deploy;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("image")
    public Image getImage() {
        return image;
    }

    @JsonProperty("image")
    public void setImage(Image image) {
        this.image = image;
    }

    public HelmEap82Release withImage(Image image) {
        this.image = image;
        return this;
    }

    /**
     * Configuration to build the application image
     * 
     */
    @JsonProperty("build")
    public Build getBuild() {
        return build;
    }

    /**
     * Configuration to build the application image
     * 
     */
    @JsonProperty("build")
    public void setBuild(Build build) {
        this.build = build;
    }

    public HelmEap82Release withBuild(Build build) {
        this.build = build;
        return this;
    }

    /**
     * Configuration to deploy the application
     * 
     */
    @JsonProperty("deploy")
    public Deploy getDeploy() {
        return deploy;
    }

    /**
     * Configuration to deploy the application
     * 
     */
    @JsonProperty("deploy")
    public void setDeploy(Deploy deploy) {
        this.deploy = deploy;
    }

    public HelmEap82Release withDeploy(Deploy deploy) {
        this.deploy = deploy;
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

    public HelmEap82Release withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(HelmEap82Release.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("image");
        sb.append('=');
        sb.append(((this.image == null)?"<null>":this.image));
        sb.append(',');
        sb.append("build");
        sb.append('=');
        sb.append(((this.build == null)?"<null>":this.build));
        sb.append(',');
        sb.append("deploy");
        sb.append('=');
        sb.append(((this.deploy == null)?"<null>":this.deploy));
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
        result = ((result* 31)+((this.image == null)? 0 :this.image.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.build == null)? 0 :this.build.hashCode()));
        result = ((result* 31)+((this.deploy == null)? 0 :this.deploy.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof HelmEap82Release) == false) {
            return false;
        }
        HelmEap82Release rhs = ((HelmEap82Release) other);
        return (((((this.image == rhs.image)||((this.image!= null)&&this.image.equals(rhs.image)))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.build == rhs.build)||((this.build!= null)&&this.build.equals(rhs.build))))&&((this.deploy == rhs.deploy)||((this.deploy!= null)&&this.deploy.equals(rhs.deploy))));
    }

}
