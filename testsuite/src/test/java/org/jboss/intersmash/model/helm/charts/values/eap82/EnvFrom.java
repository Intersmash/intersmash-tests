
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
 * EnvFromSource represents the source of a set of ConfigMaps
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "configMapRef",
    "prefix",
    "secretRef"
})
@Generated("jsonschema2pojo")
public class EnvFrom {

    /**
     * ConfigMapEnvSource selects a ConfigMap to populate the environment variables with.
     * 
     * The contents of the target ConfigMap's Data field will represent the key-value pairs as environment variables.
     * 
     */
    @JsonProperty("configMapRef")
    @JsonPropertyDescription("ConfigMapEnvSource selects a ConfigMap to populate the environment variables with.\n\nThe contents of the target ConfigMap's Data field will represent the key-value pairs as environment variables.")
    private ConfigMapRef configMapRef;
    /**
     * An optional identifier to prepend to each key in the ConfigMap. Must be a C_IDENTIFIER.
     * 
     */
    @JsonProperty("prefix")
    @JsonPropertyDescription("An optional identifier to prepend to each key in the ConfigMap. Must be a C_IDENTIFIER.")
    private String prefix;
    /**
     * SecretEnvSource selects a Secret to populate the environment variables with.
     * 
     * The contents of the target Secret's Data field will represent the key-value pairs as environment variables.
     * 
     */
    @JsonProperty("secretRef")
    @JsonPropertyDescription("SecretEnvSource selects a Secret to populate the environment variables with.\n\nThe contents of the target Secret's Data field will represent the key-value pairs as environment variables.")
    private SecretRef secretRef;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * ConfigMapEnvSource selects a ConfigMap to populate the environment variables with.
     * 
     * The contents of the target ConfigMap's Data field will represent the key-value pairs as environment variables.
     * 
     */
    @JsonProperty("configMapRef")
    public ConfigMapRef getConfigMapRef() {
        return configMapRef;
    }

    /**
     * ConfigMapEnvSource selects a ConfigMap to populate the environment variables with.
     * 
     * The contents of the target ConfigMap's Data field will represent the key-value pairs as environment variables.
     * 
     */
    @JsonProperty("configMapRef")
    public void setConfigMapRef(ConfigMapRef configMapRef) {
        this.configMapRef = configMapRef;
    }

    public EnvFrom withConfigMapRef(ConfigMapRef configMapRef) {
        this.configMapRef = configMapRef;
        return this;
    }

    /**
     * An optional identifier to prepend to each key in the ConfigMap. Must be a C_IDENTIFIER.
     * 
     */
    @JsonProperty("prefix")
    public String getPrefix() {
        return prefix;
    }

    /**
     * An optional identifier to prepend to each key in the ConfigMap. Must be a C_IDENTIFIER.
     * 
     */
    @JsonProperty("prefix")
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public EnvFrom withPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * SecretEnvSource selects a Secret to populate the environment variables with.
     * 
     * The contents of the target Secret's Data field will represent the key-value pairs as environment variables.
     * 
     */
    @JsonProperty("secretRef")
    public SecretRef getSecretRef() {
        return secretRef;
    }

    /**
     * SecretEnvSource selects a Secret to populate the environment variables with.
     * 
     * The contents of the target Secret's Data field will represent the key-value pairs as environment variables.
     * 
     */
    @JsonProperty("secretRef")
    public void setSecretRef(SecretRef secretRef) {
        this.secretRef = secretRef;
    }

    public EnvFrom withSecretRef(SecretRef secretRef) {
        this.secretRef = secretRef;
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

    public EnvFrom withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EnvFrom.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("configMapRef");
        sb.append('=');
        sb.append(((this.configMapRef == null)?"<null>":this.configMapRef));
        sb.append(',');
        sb.append("prefix");
        sb.append('=');
        sb.append(((this.prefix == null)?"<null>":this.prefix));
        sb.append(',');
        sb.append("secretRef");
        sb.append('=');
        sb.append(((this.secretRef == null)?"<null>":this.secretRef));
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
        result = ((result* 31)+((this.secretRef == null)? 0 :this.secretRef.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.configMapRef == null)? 0 :this.configMapRef.hashCode()));
        result = ((result* 31)+((this.prefix == null)? 0 :this.prefix.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EnvFrom) == false) {
            return false;
        }
        EnvFrom rhs = ((EnvFrom) other);
        return (((((this.secretRef == rhs.secretRef)||((this.secretRef!= null)&&this.secretRef.equals(rhs.secretRef)))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.configMapRef == rhs.configMapRef)||((this.configMapRef!= null)&&this.configMapRef.equals(rhs.configMapRef))))&&((this.prefix == rhs.prefix)||((this.prefix!= null)&&this.prefix.equals(rhs.prefix))));
    }

}
