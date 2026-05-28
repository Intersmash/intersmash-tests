
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
 * ResourceFieldSelector represents container resources (cpu, memory) and their output format
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "containerName",
    "divisor",
    "resource"
})
@Generated("jsonschema2pojo")
public class ResourceFieldRef__1 {

    /**
     * Container name: required for volumes, optional for env vars
     * 
     */
    @JsonProperty("containerName")
    @JsonPropertyDescription("Container name: required for volumes, optional for env vars")
    private String containerName;
    @JsonProperty("divisor")
    private Object divisor;
    /**
     * Required: resource to select
     * (Required)
     * 
     */
    @JsonProperty("resource")
    @JsonPropertyDescription("Required: resource to select")
    private String resource;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Container name: required for volumes, optional for env vars
     * 
     */
    @JsonProperty("containerName")
    public String getContainerName() {
        return containerName;
    }

    /**
     * Container name: required for volumes, optional for env vars
     * 
     */
    @JsonProperty("containerName")
    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public ResourceFieldRef__1 withContainerName(String containerName) {
        this.containerName = containerName;
        return this;
    }

    @JsonProperty("divisor")
    public Object getDivisor() {
        return divisor;
    }

    @JsonProperty("divisor")
    public void setDivisor(Object divisor) {
        this.divisor = divisor;
    }

    public ResourceFieldRef__1 withDivisor(Object divisor) {
        this.divisor = divisor;
        return this;
    }

    /**
     * Required: resource to select
     * (Required)
     * 
     */
    @JsonProperty("resource")
    public String getResource() {
        return resource;
    }

    /**
     * Required: resource to select
     * (Required)
     * 
     */
    @JsonProperty("resource")
    public void setResource(String resource) {
        this.resource = resource;
    }

    public ResourceFieldRef__1 withResource(String resource) {
        this.resource = resource;
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

    public ResourceFieldRef__1 withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ResourceFieldRef__1 .class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("containerName");
        sb.append('=');
        sb.append(((this.containerName == null)?"<null>":this.containerName));
        sb.append(',');
        sb.append("divisor");
        sb.append('=');
        sb.append(((this.divisor == null)?"<null>":this.divisor));
        sb.append(',');
        sb.append("resource");
        sb.append('=');
        sb.append(((this.resource == null)?"<null>":this.resource));
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
        result = ((result* 31)+((this.divisor == null)? 0 :this.divisor.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.resource == null)? 0 :this.resource.hashCode()));
        result = ((result* 31)+((this.containerName == null)? 0 :this.containerName.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ResourceFieldRef__1) == false) {
            return false;
        }
        ResourceFieldRef__1 rhs = ((ResourceFieldRef__1) other);
        return (((((this.divisor == rhs.divisor)||((this.divisor!= null)&&this.divisor.equals(rhs.divisor)))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.resource == rhs.resource)||((this.resource!= null)&&this.resource.equals(rhs.resource))))&&((this.containerName == rhs.containerName)||((this.containerName!= null)&&this.containerName.equals(rhs.containerName))));
    }

}
