
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
 * ObjectFieldSelector selects an APIVersioned field of an object.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "apiVersion",
    "fieldPath"
})
@Generated("jsonschema2pojo")
public class FieldRef {

    /**
     * Version of the schema the FieldPath is written in terms of, defaults to "v1".
     * 
     */
    @JsonProperty("apiVersion")
    @JsonPropertyDescription("Version of the schema the FieldPath is written in terms of, defaults to \"v1\".")
    private String apiVersion;
    /**
     * Path of the field to select in the specified API version.
     * (Required)
     * 
     */
    @JsonProperty("fieldPath")
    @JsonPropertyDescription("Path of the field to select in the specified API version.")
    private String fieldPath;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Version of the schema the FieldPath is written in terms of, defaults to "v1".
     * 
     */
    @JsonProperty("apiVersion")
    public String getApiVersion() {
        return apiVersion;
    }

    /**
     * Version of the schema the FieldPath is written in terms of, defaults to "v1".
     * 
     */
    @JsonProperty("apiVersion")
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public FieldRef withApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    /**
     * Path of the field to select in the specified API version.
     * (Required)
     * 
     */
    @JsonProperty("fieldPath")
    public String getFieldPath() {
        return fieldPath;
    }

    /**
     * Path of the field to select in the specified API version.
     * (Required)
     * 
     */
    @JsonProperty("fieldPath")
    public void setFieldPath(String fieldPath) {
        this.fieldPath = fieldPath;
    }

    public FieldRef withFieldPath(String fieldPath) {
        this.fieldPath = fieldPath;
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

    public FieldRef withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FieldRef.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("apiVersion");
        sb.append('=');
        sb.append(((this.apiVersion == null)?"<null>":this.apiVersion));
        sb.append(',');
        sb.append("fieldPath");
        sb.append('=');
        sb.append(((this.fieldPath == null)?"<null>":this.fieldPath));
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
        result = ((result* 31)+((this.fieldPath == null)? 0 :this.fieldPath.hashCode()));
        result = ((result* 31)+((this.apiVersion == null)? 0 :this.apiVersion.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof FieldRef) == false) {
            return false;
        }
        FieldRef rhs = ((FieldRef) other);
        return ((((this.fieldPath == rhs.fieldPath)||((this.fieldPath!= null)&&this.fieldPath.equals(rhs.fieldPath)))&&((this.apiVersion == rhs.apiVersion)||((this.apiVersion!= null)&&this.apiVersion.equals(rhs.apiVersion))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

}
