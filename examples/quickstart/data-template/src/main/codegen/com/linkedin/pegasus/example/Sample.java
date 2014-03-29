
package com.linkedin.pegasus.example;

import java.util.List;
import javax.annotation.Generated;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.SetMode;


/**
 * A sample pegasus schema
 * 
 */
@Generated(value = "com.linkedin.pegasus.generator.PegasusDataTemplateGenerator", comments = "LinkedIn Data Template. Generated from /home/jbetz/projects/restli-sbt-plugin/example/data-template/src/main/pegasus/com/linkedin/pegasus/example/Sample.pdsc.", date = "Thu Mar 27 15:06:39 PDT 2014")
public class Sample
    extends RecordTemplate
{

    private final static Sample.Fields _fields = new Sample.Fields();
    private final static RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema("{\"type\":\"record\",\"name\":\"Sample\",\"namespace\":\"com.linkedin.pegasus.example\",\"doc\":\"A sample pegasus schema\",\"fields\":[{\"name\":\"message\",\"type\":\"string\",\"optional\":true},{\"name\":\"treatment\",\"type\":\"string\"},{\"name\":\"id\",\"type\":\"long\"}]}"));
    private final static RecordDataSchema.Field FIELD_Message = SCHEMA.getField("message");
    private final static RecordDataSchema.Field FIELD_Treatment = SCHEMA.getField("treatment");
    private final static RecordDataSchema.Field FIELD_Id = SCHEMA.getField("id");

    public Sample() {
        super(new DataMap(), SCHEMA);
    }

    public Sample(DataMap data) {
        super(data, SCHEMA);
    }

    public static Sample.Fields fields() {
        return _fields;
    }

    /**
     * Existence checker for message
     * 
     * @see Fields#message
     */
    public boolean hasMessage() {
        return contains(FIELD_Message);
    }

    /**
     * Remover for message
     * 
     * @see Fields#message
     */
    public void removeMessage() {
        remove(FIELD_Message);
    }

    /**
     * Getter for message
     * 
     * @see Fields#message
     */
    public String getMessage(GetMode mode) {
        return obtainDirect(FIELD_Message, String.class, mode);
    }

    /**
     * Getter for message
     * 
     * @see Fields#message
     */
    public String getMessage() {
        return getMessage(GetMode.STRICT);
    }

    /**
     * Setter for message
     * 
     * @see Fields#message
     */
    public Sample setMessage(String value, SetMode mode) {
        putDirect(FIELD_Message, String.class, String.class, value, mode);
        return this;
    }

    /**
     * Setter for message
     * 
     * @see Fields#message
     */
    public Sample setMessage(String value) {
        putDirect(FIELD_Message, String.class, String.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    /**
     * Existence checker for treatment
     * 
     * @see Fields#treatment
     */
    public boolean hasTreatment() {
        return contains(FIELD_Treatment);
    }

    /**
     * Remover for treatment
     * 
     * @see Fields#treatment
     */
    public void removeTreatment() {
        remove(FIELD_Treatment);
    }

    /**
     * Getter for treatment
     * 
     * @see Fields#treatment
     */
    public String getTreatment(GetMode mode) {
        return obtainDirect(FIELD_Treatment, String.class, mode);
    }

    /**
     * Getter for treatment
     * 
     * @see Fields#treatment
     */
    public String getTreatment() {
        return getTreatment(GetMode.STRICT);
    }

    /**
     * Setter for treatment
     * 
     * @see Fields#treatment
     */
    public Sample setTreatment(String value, SetMode mode) {
        putDirect(FIELD_Treatment, String.class, String.class, value, mode);
        return this;
    }

    /**
     * Setter for treatment
     * 
     * @see Fields#treatment
     */
    public Sample setTreatment(String value) {
        putDirect(FIELD_Treatment, String.class, String.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    /**
     * Existence checker for id
     * 
     * @see Fields#id
     */
    public boolean hasId() {
        return contains(FIELD_Id);
    }

    /**
     * Remover for id
     * 
     * @see Fields#id
     */
    public void removeId() {
        remove(FIELD_Id);
    }

    /**
     * Getter for id
     * 
     * @see Fields#id
     */
    public Long getId(GetMode mode) {
        return obtainDirect(FIELD_Id, Long.class, mode);
    }

    /**
     * Getter for id
     * 
     * @see Fields#id
     */
    public Long getId() {
        return getId(GetMode.STRICT);
    }

    /**
     * Setter for id
     * 
     * @see Fields#id
     */
    public Sample setId(Long value, SetMode mode) {
        putDirect(FIELD_Id, Long.class, Long.class, value, mode);
        return this;
    }

    /**
     * Setter for id
     * 
     * @see Fields#id
     */
    public Sample setId(Long value) {
        putDirect(FIELD_Id, Long.class, Long.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    /**
     * Setter for id
     * 
     * @see Fields#id
     */
    public Sample setId(long value) {
        putDirect(FIELD_Id, Long.class, Long.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    @Override
    public Sample clone()
        throws CloneNotSupportedException
    {
        return ((Sample) super.clone());
    }

    @Override
    public Sample copy()
        throws CloneNotSupportedException
    {
        return ((Sample) super.copy());
    }

    public static class Fields
        extends PathSpec
    {


        public Fields(List<String> path, String name) {
            super(path, name);
        }

        public Fields() {
            super();
        }

        public PathSpec message() {
            return new PathSpec(getPathComponents(), "message");
        }

        public PathSpec treatment() {
            return new PathSpec(getPathComponents(), "treatment");
        }

        public PathSpec id() {
            return new PathSpec(getPathComponents(), "id");
        }

    }

}
