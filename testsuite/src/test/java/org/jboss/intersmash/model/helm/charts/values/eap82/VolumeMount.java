
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
 * VolumeMount describes a mounting of a Volume within a container.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "mountPath",
    "mountPropagation",
    "name",
    "readOnly",
    "subPath"
})
@Generated("jsonschema2pojo")
public class VolumeMount {

    /**
     * Path within the container at which the volume should be mounted.  Must not contain ':'.
     * (Required)
     * 
     */
    @JsonProperty("mountPath")
    @JsonPropertyDescription("Path within the container at which the volume should be mounted.  Must not contain ':'.")
    private String mountPath;
    /**
     * mountPropagation determines how mounts are propagated from the host to container and the other way around. When not set, MountPropagationHostToContainer is used. This field is alpha in 1.8 and can be reworked or removed in a future release.
     * 
     */
    @JsonProperty("mountPropagation")
    @JsonPropertyDescription("mountPropagation determines how mounts are propagated from the host to container and the other way around. When not set, MountPropagationHostToContainer is used. This field is alpha in 1.8 and can be reworked or removed in a future release.")
    private String mountPropagation;
    /**
     * This must match the Name of a Volume.
     * (Required)
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("This must match the Name of a Volume.")
    private String name;
    /**
     * Mounted read-only if true, read-write otherwise (false or unspecified). Defaults to false.
     * 
     */
    @JsonProperty("readOnly")
    @JsonPropertyDescription("Mounted read-only if true, read-write otherwise (false or unspecified). Defaults to false.")
    private Boolean readOnly;
    /**
     * Path within the volume from which the container's volume should be mounted. Defaults to "" (volume's root).
     * 
     */
    @JsonProperty("subPath")
    @JsonPropertyDescription("Path within the volume from which the container's volume should be mounted. Defaults to \"\" (volume's root).")
    private String subPath;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Path within the container at which the volume should be mounted.  Must not contain ':'.
     * (Required)
     * 
     */
    @JsonProperty("mountPath")
    public String getMountPath() {
        return mountPath;
    }

    /**
     * Path within the container at which the volume should be mounted.  Must not contain ':'.
     * (Required)
     * 
     */
    @JsonProperty("mountPath")
    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }

    public VolumeMount withMountPath(String mountPath) {
        this.mountPath = mountPath;
        return this;
    }

    /**
     * mountPropagation determines how mounts are propagated from the host to container and the other way around. When not set, MountPropagationHostToContainer is used. This field is alpha in 1.8 and can be reworked or removed in a future release.
     * 
     */
    @JsonProperty("mountPropagation")
    public String getMountPropagation() {
        return mountPropagation;
    }

    /**
     * mountPropagation determines how mounts are propagated from the host to container and the other way around. When not set, MountPropagationHostToContainer is used. This field is alpha in 1.8 and can be reworked or removed in a future release.
     * 
     */
    @JsonProperty("mountPropagation")
    public void setMountPropagation(String mountPropagation) {
        this.mountPropagation = mountPropagation;
    }

    public VolumeMount withMountPropagation(String mountPropagation) {
        this.mountPropagation = mountPropagation;
        return this;
    }

    /**
     * This must match the Name of a Volume.
     * (Required)
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * This must match the Name of a Volume.
     * (Required)
     * 
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public VolumeMount withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Mounted read-only if true, read-write otherwise (false or unspecified). Defaults to false.
     * 
     */
    @JsonProperty("readOnly")
    public Boolean getReadOnly() {
        return readOnly;
    }

    /**
     * Mounted read-only if true, read-write otherwise (false or unspecified). Defaults to false.
     * 
     */
    @JsonProperty("readOnly")
    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public VolumeMount withReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    /**
     * Path within the volume from which the container's volume should be mounted. Defaults to "" (volume's root).
     * 
     */
    @JsonProperty("subPath")
    public String getSubPath() {
        return subPath;
    }

    /**
     * Path within the volume from which the container's volume should be mounted. Defaults to "" (volume's root).
     * 
     */
    @JsonProperty("subPath")
    public void setSubPath(String subPath) {
        this.subPath = subPath;
    }

    public VolumeMount withSubPath(String subPath) {
        this.subPath = subPath;
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

    public VolumeMount withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(VolumeMount.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("mountPath");
        sb.append('=');
        sb.append(((this.mountPath == null)?"<null>":this.mountPath));
        sb.append(',');
        sb.append("mountPropagation");
        sb.append('=');
        sb.append(((this.mountPropagation == null)?"<null>":this.mountPropagation));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("readOnly");
        sb.append('=');
        sb.append(((this.readOnly == null)?"<null>":this.readOnly));
        sb.append(',');
        sb.append("subPath");
        sb.append('=');
        sb.append(((this.subPath == null)?"<null>":this.subPath));
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
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.mountPath == null)? 0 :this.mountPath.hashCode()));
        result = ((result* 31)+((this.readOnly == null)? 0 :this.readOnly.hashCode()));
        result = ((result* 31)+((this.mountPropagation == null)? 0 :this.mountPropagation.hashCode()));
        result = ((result* 31)+((this.subPath == null)? 0 :this.subPath.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof VolumeMount) == false) {
            return false;
        }
        VolumeMount rhs = ((VolumeMount) other);
        return (((((((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name)))&&((this.mountPath == rhs.mountPath)||((this.mountPath!= null)&&this.mountPath.equals(rhs.mountPath))))&&((this.readOnly == rhs.readOnly)||((this.readOnly!= null)&&this.readOnly.equals(rhs.readOnly))))&&((this.mountPropagation == rhs.mountPropagation)||((this.mountPropagation!= null)&&this.mountPropagation.equals(rhs.mountPropagation))))&&((this.subPath == rhs.subPath)||((this.subPath!= null)&&this.subPath.equals(rhs.subPath))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

}
