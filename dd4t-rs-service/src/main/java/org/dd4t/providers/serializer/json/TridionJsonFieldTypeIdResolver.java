package org.dd4t.providers.serializer.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import org.dd4t.contentmodel.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves Tridion Field Types to their proper concrete
 * classes, which are subclasses of the abstract BaseField class.
 * <p/>
 * This is done based on the "FieldType" parameter which
 * is sent by the DD4T templating assembly for each serialized Tridion Field.
 *
 * @author R. Kempees
 * @see org.dd4t.contentmodel.impl.BaseField
 * @since 04.06.2014
 */
public class TridionJsonFieldTypeIdResolver implements TypeIdResolver {

    private static final Logger LOG = LoggerFactory.getLogger(TridionJsonFieldTypeIdResolver.class);
    private static final Map<FieldType, String> fieldTypes = new HashMap<>();
    private static final String NAMESPACE_PREFIX = "org.dd4t.contentmodel.impl.";

    static {
        fieldTypes.put(FieldType.TEXT, NAMESPACE_PREFIX + "TextField");
        fieldTypes.put(FieldType.MULTILINETEXT, NAMESPACE_PREFIX + "TextField");
        fieldTypes.put(FieldType.XHTML, NAMESPACE_PREFIX + "XhtmlField");
        fieldTypes.put(FieldType.KEYWORD, NAMESPACE_PREFIX + "KeywordField");
        fieldTypes.put(FieldType.EMBEDDED, NAMESPACE_PREFIX + "EmbeddedField");
        // This (5) is actually a MM Link field, but the links are needed for multi values and meta
        fieldTypes.put(FieldType.MULTIMEDIALINK, NAMESPACE_PREFIX + "ComponentLinkField"); // TODO: rename
        fieldTypes.put(FieldType.COMPONENTLINK, NAMESPACE_PREFIX + "ComponentLinkField");
        fieldTypes.put(FieldType.EXTERNALLINK, NAMESPACE_PREFIX + "TextField"); // Is actually ExternalUrl
        fieldTypes.put(FieldType.NUMBER, NAMESPACE_PREFIX + "NumericField");
        fieldTypes.put(FieldType.DATE, NAMESPACE_PREFIX + "DateField");
        fieldTypes.put(FieldType.UNKNOWN, NAMESPACE_PREFIX + "BaseField");
    }

    private JavaType mBaseType;

    public TridionJsonFieldTypeIdResolver() {
    }

    @Override
    public void init(final JavaType javaType) {
        LOG.info("Instantiating TridionJsonFieldTypeResolver");
        mBaseType = javaType;
    }

    @Override
    public String idFromValue(final Object o) {
        return idFromValueAndType(o, o.getClass());
    }

    @Override
    public String idFromValueAndType(final Object o, final Class<?> aClass) {
        String name = aClass.getName();

        if (null == o) {
            return "-1";
        }

        return getIdFromClass(name);
    }

    @Override
    public String idFromBaseType() {
        return "-1";
    }

    @Override
    public JavaType typeFromId(final String s) {
        String clazzName = getClassForKey(s);
        Class<?> clazz;

        try {
            LOG.debug("Loading a '{}'", clazzName);
            clazz = ClassUtil.findClass(clazzName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot find class '" + clazzName + "'");
        }

        return TypeFactory.defaultInstance().constructSpecializedType(mBaseType, clazz);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }

    private String getClassForKey(String type) {
        LOG.trace("Fetching field type for {}", type);
        FieldType fieldType = getFieldTypeByName(type);

        if (fieldTypes.containsKey(fieldType)) {
            String result = fieldTypes.get(fieldType);
            LOG.debug("Returning: {}", result);
            return result;
        }

        throw new IllegalArgumentException("Field Type " + type + " not defined!");
    }

    private FieldType getFieldTypeByName(String type) {
        try {
            return FieldType.valueOf(type);
        } catch (IllegalArgumentException iae) {
            try {
                int value = Integer.parseInt(type);
                for (FieldType fieldType : FieldType.values()) {
                    if (fieldType.getValue() == value) {
                        return fieldType;
                    }
                }
            } catch (NumberFormatException nfe) {
                LOG.warn("Cannot identify FieldType enum for " + type);
            }
        }

        return FieldType.UNKNOWN;
    }

    private String getIdFromClass(String aClassName) {
        if (!aClassName.startsWith(NAMESPACE_PREFIX)) {
            aClassName = NAMESPACE_PREFIX + aClassName;
        }

        for (Map.Entry<FieldType, String> entry : fieldTypes.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(aClassName)) {
                LOG.trace("Found {}::{}", entry.getKey(), entry.getValue());
                return entry.getKey().toString();
            }
        }
        return "-1";
    }
}
