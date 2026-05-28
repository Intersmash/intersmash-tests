
package org.jboss.intersmash.model.helm.charts.values.eap82;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * TLS Configuration for the Route
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "enabled",
    "termination",
    "insecureEdgeTerminationPolicy"
})
@Generated("jsonschema2pojo")
public class Tls {

    /**
     * Determines if the Route should be TLS-encrypted. If deploy.tls.enabled is true, the route will use the secure service to acess to the deployment
     * 
     */
    @JsonProperty("enabled")
    @JsonPropertyDescription("Determines if the Route should be TLS-encrypted. If deploy.tls.enabled is true, the route will use the secure service to acess to the deployment")
    private Boolean enabled = true;
    /**
     * Determines the type of TLS termination to use
     * 
     */
    @JsonProperty("termination")
    @JsonPropertyDescription("Determines the type of TLS termination to use")
    private Termination termination = Termination.fromValue("edge");
    /**
     * Determines if insecure traffic should be redirected
     * 
     */
    @JsonProperty("insecureEdgeTerminationPolicy")
    @JsonPropertyDescription("Determines if insecure traffic should be redirected")
    private InsecureEdgeTerminationPolicy insecureEdgeTerminationPolicy = InsecureEdgeTerminationPolicy.fromValue("Redirect");
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Determines if the Route should be TLS-encrypted. If deploy.tls.enabled is true, the route will use the secure service to acess to the deployment
     * 
     */
    @JsonProperty("enabled")
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Determines if the Route should be TLS-encrypted. If deploy.tls.enabled is true, the route will use the secure service to acess to the deployment
     * 
     */
    @JsonProperty("enabled")
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Tls withEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Determines the type of TLS termination to use
     * 
     */
    @JsonProperty("termination")
    public Termination getTermination() {
        return termination;
    }

    /**
     * Determines the type of TLS termination to use
     * 
     */
    @JsonProperty("termination")
    public void setTermination(Termination termination) {
        this.termination = termination;
    }

    public Tls withTermination(Termination termination) {
        this.termination = termination;
        return this;
    }

    /**
     * Determines if insecure traffic should be redirected
     * 
     */
    @JsonProperty("insecureEdgeTerminationPolicy")
    public InsecureEdgeTerminationPolicy getInsecureEdgeTerminationPolicy() {
        return insecureEdgeTerminationPolicy;
    }

    /**
     * Determines if insecure traffic should be redirected
     * 
     */
    @JsonProperty("insecureEdgeTerminationPolicy")
    public void setInsecureEdgeTerminationPolicy(InsecureEdgeTerminationPolicy insecureEdgeTerminationPolicy) {
        this.insecureEdgeTerminationPolicy = insecureEdgeTerminationPolicy;
    }

    public Tls withInsecureEdgeTerminationPolicy(InsecureEdgeTerminationPolicy insecureEdgeTerminationPolicy) {
        this.insecureEdgeTerminationPolicy = insecureEdgeTerminationPolicy;
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

    public Tls withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Tls.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("enabled");
        sb.append('=');
        sb.append(((this.enabled == null)?"<null>":this.enabled));
        sb.append(',');
        sb.append("termination");
        sb.append('=');
        sb.append(((this.termination == null)?"<null>":this.termination));
        sb.append(',');
        sb.append("insecureEdgeTerminationPolicy");
        sb.append('=');
        sb.append(((this.insecureEdgeTerminationPolicy == null)?"<null>":this.insecureEdgeTerminationPolicy));
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
        result = ((result* 31)+((this.termination == null)? 0 :this.termination.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.enabled == null)? 0 :this.enabled.hashCode()));
        result = ((result* 31)+((this.insecureEdgeTerminationPolicy == null)? 0 :this.insecureEdgeTerminationPolicy.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Tls) == false) {
            return false;
        }
        Tls rhs = ((Tls) other);
        return (((((this.termination == rhs.termination)||((this.termination!= null)&&this.termination.equals(rhs.termination)))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.enabled == rhs.enabled)||((this.enabled!= null)&&this.enabled.equals(rhs.enabled))))&&((this.insecureEdgeTerminationPolicy == rhs.insecureEdgeTerminationPolicy)||((this.insecureEdgeTerminationPolicy!= null)&&this.insecureEdgeTerminationPolicy.equals(rhs.insecureEdgeTerminationPolicy))));
    }


    /**
     * Determines if insecure traffic should be redirected
     * 
     */
    @Generated("jsonschema2pojo")
    public enum InsecureEdgeTerminationPolicy {

        ALLOW("Allow"),
        DISABLE("Disable"),
        REDIRECT("Redirect");
        private final String value;
        private final static Map<String, InsecureEdgeTerminationPolicy> CONSTANTS = new HashMap<String, InsecureEdgeTerminationPolicy>();

        static {
            for (InsecureEdgeTerminationPolicy c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        InsecureEdgeTerminationPolicy(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static InsecureEdgeTerminationPolicy fromValue(String value) {
            InsecureEdgeTerminationPolicy constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * Determines the type of TLS termination to use
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Termination {

        EDGE("edge"),
        REENCRYPT("reencrypt"),
        PASSTHROUGH("passthrough");
        private final String value;
        private final static Map<String, Termination> CONSTANTS = new HashMap<String, Termination>();

        static {
            for (Termination c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Termination(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static Termination fromValue(String value) {
            Termination constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
