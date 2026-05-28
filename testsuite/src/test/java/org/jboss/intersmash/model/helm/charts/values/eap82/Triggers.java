
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
 * Webhooks to trigger building the application image
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "githubSecret",
    "genericSecret"
})
@Generated("jsonschema2pojo")
public class Triggers {

    /**
     * Name of the secret containing the WebHookSecretKey for the GitHub Webhook
     * 
     */
    @JsonProperty("githubSecret")
    @JsonPropertyDescription("Name of the secret containing the WebHookSecretKey for the GitHub Webhook")
    private String githubSecret;
    /**
     * Name of the secret containing the WebHookSecretKey for the Generic Webhook
     * 
     */
    @JsonProperty("genericSecret")
    @JsonPropertyDescription("Name of the secret containing the WebHookSecretKey for the Generic Webhook")
    private String genericSecret;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Name of the secret containing the WebHookSecretKey for the GitHub Webhook
     * 
     */
    @JsonProperty("githubSecret")
    public String getGithubSecret() {
        return githubSecret;
    }

    /**
     * Name of the secret containing the WebHookSecretKey for the GitHub Webhook
     * 
     */
    @JsonProperty("githubSecret")
    public void setGithubSecret(String githubSecret) {
        this.githubSecret = githubSecret;
    }

    public Triggers withGithubSecret(String githubSecret) {
        this.githubSecret = githubSecret;
        return this;
    }

    /**
     * Name of the secret containing the WebHookSecretKey for the Generic Webhook
     * 
     */
    @JsonProperty("genericSecret")
    public String getGenericSecret() {
        return genericSecret;
    }

    /**
     * Name of the secret containing the WebHookSecretKey for the Generic Webhook
     * 
     */
    @JsonProperty("genericSecret")
    public void setGenericSecret(String genericSecret) {
        this.genericSecret = genericSecret;
    }

    public Triggers withGenericSecret(String genericSecret) {
        this.genericSecret = genericSecret;
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

    public Triggers withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Triggers.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("githubSecret");
        sb.append('=');
        sb.append(((this.githubSecret == null)?"<null>":this.githubSecret));
        sb.append(',');
        sb.append("genericSecret");
        sb.append('=');
        sb.append(((this.genericSecret == null)?"<null>":this.genericSecret));
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
        result = ((result* 31)+((this.githubSecret == null)? 0 :this.githubSecret.hashCode()));
        result = ((result* 31)+((this.genericSecret == null)? 0 :this.genericSecret.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Triggers) == false) {
            return false;
        }
        Triggers rhs = ((Triggers) other);
        return ((((this.githubSecret == rhs.githubSecret)||((this.githubSecret!= null)&&this.githubSecret.equals(rhs.githubSecret)))&&((this.genericSecret == rhs.genericSecret)||((this.genericSecret!= null)&&this.genericSecret.equals(rhs.genericSecret))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

}
