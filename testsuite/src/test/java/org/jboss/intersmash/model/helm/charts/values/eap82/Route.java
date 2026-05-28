
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
 * Route configuration
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "enabled",
    "host",
    "tls"
})
@Generated("jsonschema2pojo")
public class Route {

    /**
     * Enable/Disable creating a Route for the application
     * 
     */
    @JsonProperty("enabled")
    @JsonPropertyDescription("Enable/Disable creating a Route for the application")
    private Boolean enabled = true;
    /**
     * alias/DNS that points to the service. If not specified a route name will typically be automatically chosen
     * 
     */
    @JsonProperty("host")
    @JsonPropertyDescription("alias/DNS that points to the service. If not specified a route name will typically be automatically chosen")
    private String host;
    /**
     * TLS Configuration for the Route
     * 
     */
    @JsonProperty("tls")
    @JsonPropertyDescription("TLS Configuration for the Route")
    private Tls tls;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Enable/Disable creating a Route for the application
     * 
     */
    @JsonProperty("enabled")
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Enable/Disable creating a Route for the application
     * 
     */
    @JsonProperty("enabled")
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Route withEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * alias/DNS that points to the service. If not specified a route name will typically be automatically chosen
     * 
     */
    @JsonProperty("host")
    public String getHost() {
        return host;
    }

    /**
     * alias/DNS that points to the service. If not specified a route name will typically be automatically chosen
     * 
     */
    @JsonProperty("host")
    public void setHost(String host) {
        this.host = host;
    }

    public Route withHost(String host) {
        this.host = host;
        return this;
    }

    /**
     * TLS Configuration for the Route
     * 
     */
    @JsonProperty("tls")
    public Tls getTls() {
        return tls;
    }

    /**
     * TLS Configuration for the Route
     * 
     */
    @JsonProperty("tls")
    public void setTls(Tls tls) {
        this.tls = tls;
    }

    public Route withTls(Tls tls) {
        this.tls = tls;
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

    public Route withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Route.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("enabled");
        sb.append('=');
        sb.append(((this.enabled == null)?"<null>":this.enabled));
        sb.append(',');
        sb.append("host");
        sb.append('=');
        sb.append(((this.host == null)?"<null>":this.host));
        sb.append(',');
        sb.append("tls");
        sb.append('=');
        sb.append(((this.tls == null)?"<null>":this.tls));
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
        result = ((result* 31)+((this.host == null)? 0 :this.host.hashCode()));
        result = ((result* 31)+((this.tls == null)? 0 :this.tls.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.enabled == null)? 0 :this.enabled.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Route) == false) {
            return false;
        }
        Route rhs = ((Route) other);
        return (((((this.host == rhs.host)||((this.host!= null)&&this.host.equals(rhs.host)))&&((this.tls == rhs.tls)||((this.tls!= null)&&this.tls.equals(rhs.tls))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.enabled == rhs.enabled)||((this.enabled!= null)&&this.enabled.equals(rhs.enabled))));
    }

}
