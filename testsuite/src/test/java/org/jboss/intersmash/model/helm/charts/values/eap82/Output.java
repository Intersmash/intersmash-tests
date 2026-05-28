
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
 * Configuration for the built application image
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "kind",
    "pushSecret"
})
@Generated("jsonschema2pojo")
public class Output {

    /**
     * Determines where the application images will be pushed
     * 
     */
    @JsonProperty("kind")
    @JsonPropertyDescription("Determines where the application images will be pushed")
    private Kind kind = Kind.fromValue("ImageStreamTag");
    /**
     * Name of the Push Secret
     * 
     */
    @JsonProperty("pushSecret")
    @JsonPropertyDescription("Name of the Push Secret")
    private String pushSecret;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Determines where the application images will be pushed
     * 
     */
    @JsonProperty("kind")
    public Kind getKind() {
        return kind;
    }

    /**
     * Determines where the application images will be pushed
     * 
     */
    @JsonProperty("kind")
    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public Output withKind(Kind kind) {
        this.kind = kind;
        return this;
    }

    /**
     * Name of the Push Secret
     * 
     */
    @JsonProperty("pushSecret")
    public String getPushSecret() {
        return pushSecret;
    }

    /**
     * Name of the Push Secret
     * 
     */
    @JsonProperty("pushSecret")
    public void setPushSecret(String pushSecret) {
        this.pushSecret = pushSecret;
    }

    public Output withPushSecret(String pushSecret) {
        this.pushSecret = pushSecret;
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

    public Output withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Output.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("kind");
        sb.append('=');
        sb.append(((this.kind == null)?"<null>":this.kind));
        sb.append(',');
        sb.append("pushSecret");
        sb.append('=');
        sb.append(((this.pushSecret == null)?"<null>":this.pushSecret));
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
        result = ((result* 31)+((this.pushSecret == null)? 0 :this.pushSecret.hashCode()));
        result = ((result* 31)+((this.kind == null)? 0 :this.kind.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Output) == false) {
            return false;
        }
        Output rhs = ((Output) other);
        return ((((this.pushSecret == rhs.pushSecret)||((this.pushSecret!= null)&&this.pushSecret.equals(rhs.pushSecret)))&&((this.kind == rhs.kind)||((this.kind!= null)&&this.kind.equals(rhs.kind))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }


    /**
     * Determines where the application images will be pushed
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Kind {

        IMAGE_STREAM_TAG("ImageStreamTag"),
        DOCKER_IMAGE("DockerImage");
        private final String value;
        private final static Map<String, Kind> CONSTANTS = new HashMap<String, Kind>();

        static {
            for (Kind c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Kind(String value) {
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
        public static Kind fromValue(String value) {
            Kind constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
