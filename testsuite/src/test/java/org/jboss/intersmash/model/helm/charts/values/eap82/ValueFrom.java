
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
 * EnvVarSource represents a source for the value of an EnvVar.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "configMapKeyRef",
    "fieldRef",
    "resourceFieldRef",
    "secretKeyRef"
})
@Generated("jsonschema2pojo")
public class ValueFrom {

    /**
     * Selects a key from a ConfigMap.
     * 
     */
    @JsonProperty("configMapKeyRef")
    @JsonPropertyDescription("Selects a key from a ConfigMap.")
    private ConfigMapKeyRef configMapKeyRef;
    /**
     * ObjectFieldSelector selects an APIVersioned field of an object.
     * 
     */
    @JsonProperty("fieldRef")
    @JsonPropertyDescription("ObjectFieldSelector selects an APIVersioned field of an object.")
    private FieldRef fieldRef;
    /**
     * ResourceFieldSelector represents container resources (cpu, memory) and their output format
     * 
     */
    @JsonProperty("resourceFieldRef")
    @JsonPropertyDescription("ResourceFieldSelector represents container resources (cpu, memory) and their output format")
    private ResourceFieldRef resourceFieldRef;
    /**
     * SecretKeySelector selects a key of a Secret.
     * 
     */
    @JsonProperty("secretKeyRef")
    @JsonPropertyDescription("SecretKeySelector selects a key of a Secret.")
    private SecretKeyRef secretKeyRef;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Selects a key from a ConfigMap.
     * 
     */
    @JsonProperty("configMapKeyRef")
    public ConfigMapKeyRef getConfigMapKeyRef() {
        return configMapKeyRef;
    }

    /**
     * Selects a key from a ConfigMap.
     * 
     */
    @JsonProperty("configMapKeyRef")
    public void setConfigMapKeyRef(ConfigMapKeyRef configMapKeyRef) {
        this.configMapKeyRef = configMapKeyRef;
    }

    public ValueFrom withConfigMapKeyRef(ConfigMapKeyRef configMapKeyRef) {
        this.configMapKeyRef = configMapKeyRef;
        return this;
    }

    /**
     * ObjectFieldSelector selects an APIVersioned field of an object.
     * 
     */
    @JsonProperty("fieldRef")
    public FieldRef getFieldRef() {
        return fieldRef;
    }

    /**
     * ObjectFieldSelector selects an APIVersioned field of an object.
     * 
     */
    @JsonProperty("fieldRef")
    public void setFieldRef(FieldRef fieldRef) {
        this.fieldRef = fieldRef;
    }

    public ValueFrom withFieldRef(FieldRef fieldRef) {
        this.fieldRef = fieldRef;
        return this;
    }

    /**
     * ResourceFieldSelector represents container resources (cpu, memory) and their output format
     * 
     */
    @JsonProperty("resourceFieldRef")
    public ResourceFieldRef getResourceFieldRef() {
        return resourceFieldRef;
    }

    /**
     * ResourceFieldSelector represents container resources (cpu, memory) and their output format
     * 
     */
    @JsonProperty("resourceFieldRef")
    public void setResourceFieldRef(ResourceFieldRef resourceFieldRef) {
        this.resourceFieldRef = resourceFieldRef;
    }

    public ValueFrom withResourceFieldRef(ResourceFieldRef resourceFieldRef) {
        this.resourceFieldRef = resourceFieldRef;
        return this;
    }

    /**
     * SecretKeySelector selects a key of a Secret.
     * 
     */
    @JsonProperty("secretKeyRef")
    public SecretKeyRef getSecretKeyRef() {
        return secretKeyRef;
    }

    /**
     * SecretKeySelector selects a key of a Secret.
     * 
     */
    @JsonProperty("secretKeyRef")
    public void setSecretKeyRef(SecretKeyRef secretKeyRef) {
        this.secretKeyRef = secretKeyRef;
    }

    public ValueFrom withSecretKeyRef(SecretKeyRef secretKeyRef) {
        this.secretKeyRef = secretKeyRef;
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

    public ValueFrom withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ValueFrom.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("configMapKeyRef");
        sb.append('=');
        sb.append(((this.configMapKeyRef == null)?"<null>":this.configMapKeyRef));
        sb.append(',');
        sb.append("fieldRef");
        sb.append('=');
        sb.append(((this.fieldRef == null)?"<null>":this.fieldRef));
        sb.append(',');
        sb.append("resourceFieldRef");
        sb.append('=');
        sb.append(((this.resourceFieldRef == null)?"<null>":this.resourceFieldRef));
        sb.append(',');
        sb.append("secretKeyRef");
        sb.append('=');
        sb.append(((this.secretKeyRef == null)?"<null>":this.secretKeyRef));
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
        result = ((result* 31)+((this.secretKeyRef == null)? 0 :this.secretKeyRef.hashCode()));
        result = ((result* 31)+((this.configMapKeyRef == null)? 0 :this.configMapKeyRef.hashCode()));
        result = ((result* 31)+((this.fieldRef == null)? 0 :this.fieldRef.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.resourceFieldRef == null)? 0 :this.resourceFieldRef.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ValueFrom) == false) {
            return false;
        }
        ValueFrom rhs = ((ValueFrom) other);
        return ((((((this.secretKeyRef == rhs.secretKeyRef)||((this.secretKeyRef!= null)&&this.secretKeyRef.equals(rhs.secretKeyRef)))&&((this.configMapKeyRef == rhs.configMapKeyRef)||((this.configMapKeyRef!= null)&&this.configMapKeyRef.equals(rhs.configMapKeyRef))))&&((this.fieldRef == rhs.fieldRef)||((this.fieldRef!= null)&&this.fieldRef.equals(rhs.fieldRef))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.resourceFieldRef == rhs.resourceFieldRef)||((this.resourceFieldRef!= null)&&this.resourceFieldRef.equals(rhs.resourceFieldRef))));
    }

}
