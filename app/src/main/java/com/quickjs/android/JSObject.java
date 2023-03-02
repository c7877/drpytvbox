package com.quickjs.android;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class JSObject {

    private final QuickJSContext context;
    private final long pointer;

    private boolean isReleased;

    public JSObject(QuickJSContext context, long pointer) {
        this.context = context;
        this.pointer = pointer;
    }

    public long getPointer() {
        return pointer;
    }

    public QuickJSContext getContext() {
        return context;
    }

    public Object getProperty(String name) {
        checkReleased();
        return context.getProperty(this, name);
    }

    public void setProperty(String name, String value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, int value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, long value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, JSObject value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, boolean value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, double value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, JSCallFunction value) {
        context.setProperty(this, name, value);
    }

    public String[] getKeys() {
        return context.getKeys(this);
    }

    public boolean contains(String key) {
        return context.contains(this, key);
    }

    /**
     * Class 添加 {@link com.quickjs.android.JSMethod} 的方法会被注入到 JSContext 中
     * 注意：该方法暂不支持匿名内部类的注册，因为匿名内部类构造参数不是无参的，newInstance 时会报错
     *
     * @param name
     * @param clazz
     */
    public void setProperty(String name, Class clazz) {
        Object javaObj = null;
        try {
            javaObj = clazz.newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        if (javaObj == null) {
            throw new NullPointerException("The JavaObj cannot be null. An error occurred in newInstance!");
        }

        JSObject jsObj = context.createNewJSObject();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(JSMethod.class)) {
                Object finalJavaObj = javaObj;
                jsObj.setProperty(method.getName(), args -> {
                    try {
                        return method.invoke(finalJavaObj, args);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    return null;
                });
            }
        }

        setProperty(name, jsObj);
    }

    public void setProperty(Class clazz) {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            JSMethod an = method.getAnnotation(JSMethod.class);
            if (an == null) continue;
            String functionName = method.getName();
            context.getGlobalObject().setProperty(functionName, args -> {
                try {
                    return method.invoke(clazz, getParameters(method, args));
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });

            if (JSUtils.isNotEmpty(an.alias())) {
                context.evaluate("var " + an.alias() + " = " + functionName + ";\n");
            }
        }
    }

    private Object[] getParameters(Method method, Object... args) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] paramCheck = new Object[parameterAnnotations.length];
        JSObject job = context.getGlobalObject();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (JSParameter.class.equals(annotation.annotationType())) {
                    JSParameter parameter = (JSParameter) annotation;
                    String parameterName = parameter.value();
                    if (parameterName.equals("urlKey") && job.contains("MY_URL")) {
                        paramCheck[i] = job.getProperty("MY_URL");
                    }
                    String defaultValue = parameter.defaultValue();
                    if (JSUtils.isNotEmpty(defaultValue)) {
                        paramCheck[i] = JSUtils.escapeJavaScriptString(defaultValue);
                    }
                    int defaultInt = parameter.defaultInt();
                    if (defaultInt >= 0) {
                        paramCheck[i] = defaultInt;
                    }
                }
            }
        }
        for (int i = 0; i < args.length; i++) {
            if (paramCheck[i] == null) {
                paramCheck[i] = args[i];
            }
        }
        return paramCheck;
    }

    public String getString(String name) {
        Object value = getProperty(name);
        return value instanceof String ? (String) value : null;
    }

    /**
     * See {@link JSObject#getString(String)}
     */
    @Deprecated
    public String getStringProperty(String name) {
        Object value = getProperty(name);
        return value instanceof String ? (String) value : null;
    }

    public Integer getInteger(String name) {
        Object value = getProperty(name);
        return value instanceof Integer ? (Integer) value : null;
    }

    /**
     * See {@link JSObject#getInteger(String)}
     */
    @Deprecated
    public Integer getIntProperty(String name) {
        Object value = getProperty(name);
        return value instanceof Integer ? (Integer) value : null;
    }

    public Boolean getBoolean(String name) {
        Object value = getProperty(name);
        return value instanceof Boolean ? (Boolean) value : null;
    }

    /**
     * See {@link JSObject#getBoolean(String)}
     */
    @Deprecated
    public Boolean getBooleanProperty(String name) {
        Object value = getProperty(name);
        return value instanceof Boolean ? (Boolean) value : null;
    }

    public Double getDouble(String name) {
        Object value = getProperty(name);
        return value instanceof Double ? (Double) value : null;
    }

    /**
     * See {@link JSObject#getDouble(String)}
     */
    @Deprecated
    public Double getDoubleProperty(String name) {
        Object value = getProperty(name);
        return value instanceof Double ? (Double) value : null;
    }

    public JSObject getJSObject(String name) {
        Object value = getProperty(name);
        return value instanceof JSObject ? (JSObject) value : null;
    }

    /**
     * See {@link JSObject#getJSObject(String)}
     */
    @Deprecated
    public JSObject getJSObjectProperty(String name) {
        Object value = getProperty(name);
        return value instanceof JSObject ? (JSObject) value : null;
    }

    public com.quickjs.android.JSFunction getJSFunction(String name) {
        Object value = getProperty(name);
        return value instanceof com.quickjs.android.JSFunction ? (com.quickjs.android.JSFunction) value : null;
    }

    /**
     * See {@link JSObject#getJSFunction(String)}
     */
    @Deprecated
    public com.quickjs.android.JSFunction getJSFunctionProperty(String name) {
        Object value = getProperty(name);
        return value instanceof com.quickjs.android.JSFunction ? (com.quickjs.android.JSFunction) value : null;
    }

    public com.quickjs.android.JSArray getJSArray(String name) {
        Object value = getProperty(name);
        return value instanceof com.quickjs.android.JSArray ? (com.quickjs.android.JSArray) value : null;
    }

    /**
     * See {@link JSObject#getJSArray(String)}
     */
    @Deprecated
    public com.quickjs.android.JSArray getJSArrayProperty(String name) {
        Object value = getProperty(name);
        return value instanceof com.quickjs.android.JSArray ? (com.quickjs.android.JSArray) value : null;
    }

    public com.quickjs.android.JSArray getNames() {
        com.quickjs.android.JSFunction getOwnPropertyNames = (com.quickjs.android.JSFunction) context.evaluate("Object.getOwnPropertyNames");
        return (com.quickjs.android.JSArray) getOwnPropertyNames.call(this);
    }

    /**
     * See {@link JSObject#getNames()}
     */
    @Deprecated
    public com.quickjs.android.JSArray getOwnPropertyNames() {
        com.quickjs.android.JSFunction getOwnPropertyNames = (com.quickjs.android.JSFunction) context.evaluate("Object.getOwnPropertyNames");
        return (com.quickjs.android.JSArray) getOwnPropertyNames.call(this);
    }

    /**
     * JSObject 确定不再使用后，调用该方法可主动释放对 JS 对象的引用。
     * 注意：该方法不能调用多次以及释放后不能再被使用对应的 JS 对象。
     */
    public void release() {
        checkReleased();

        context.freeValue(this);
        isReleased = true;
    }

    public void hold() {
        context.hold(this);
    }

    @Override
    public String toString() {
        checkReleased();

        Object formatString = context.evaluate("__format_string;");
        if (formatString instanceof com.quickjs.android.JSFunction) {
            return (String) ((com.quickjs.android.JSFunction) formatString).call(this);
        }

        return super.toString();
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        String[] keys = getKeys();
        for (String key : keys) {
            Object obj = this.getProperty(key);
            if (obj == null || obj instanceof JSFunction) {
                continue;
            }
            if (obj instanceof Number || obj instanceof String || obj instanceof Boolean) {
                try {
                    jsonObject.put(key, obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (obj instanceof com.quickjs.android.JSArray) {
                try {
                    jsonObject.put(key, ((JSArray) obj).toJSONArray());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (obj instanceof JSObject) {
                try {
                    jsonObject.put(key, ((JSObject) obj).toJSONObject());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return jsonObject;
    }

    public String stringify() {
        return context.stringify(this);
    }

    final void checkReleased() {
        if (isReleased) {
            throw new NullPointerException("This JSObject was Released, Can not call this!");
        }
    }

    public boolean isAlive() {
        return QuickJSContext.isLiveObject(context.getRuntime(), this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JSObject jsObject = (JSObject) o;
        return pointer == jsObject.pointer && isReleased == jsObject.isReleased && context == jsObject.context;
    }

}
