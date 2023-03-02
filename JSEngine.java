package com.github.tvbox.osc.util.js;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Keep;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.event.LoadingEvent;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.MD5;
import com.github.tvbox.osc.util.OkGoHelper;
import com.quickjs.android.JSArray;
import com.quickjs.android.JSCallFunction;
import com.quickjs.android.JSFunction;
import com.quickjs.android.JSMethod;
import com.quickjs.android.JSModule;
import com.quickjs.android.JSObject;
import com.quickjs.android.JSParameter;
import com.quickjs.android.JSUtils;
import com.quickjs.android.QuickJSContext;
import com.google.gson.Gson;
import com.lzy.okgo.OkGo;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JSEngine {
    private static final String TAG = "JSEngine";
    public QuickJSContext jsContext;
    private OkHttpClient okHttpClient;
    public static final ThreadLocal<Map<String, Object>> threadMap = new ThreadLocal<>();

    public QuickJSContext getJsContext() {
        return jsContext;
    }

    public JSObject getGlobalObj() {
        return jsContext.getGlobalObject();
    }

    String loadModule(String name) {
        try {
            if(name.contains("cheerio.min.js")){
                name = "cheerio.min.js";
            } else if(name.contains("crypto-js.js")){
                name = "crypto-js.js";
            } else if(name.contains("dayjs.min.js")){
                name = "dayjs.min.js";
            } else if(name.contains("uri.min.js")){
                name = "uri.min.js";
            } else if(name.contains("underscore-esm-min.js")){
                name = "underscore-esm-min.js";
            }

            if (name.startsWith("http://") || name.startsWith("https://")) {
                String cache = FileUtils.getCache(MD5.encode(name));
                if (JSUtils.isEmpty(cache)) {
                    String netStr = OkGo.<String>get(name).headers("User-Agent", "Mozilla/5.0").execute().body().string();
                    if (!TextUtils.isEmpty(netStr)) {
                        FileUtils.setCache(604800, MD5.encode(name), netStr);
                    }
                    return netStr;
                }
                return cache;
            }
            if (name.startsWith("assets://")) {
                return FileUtils.getAsOpen(name.substring(9));
            } else if (FileUtils.isAsFile(name, "js/lib")) {
                return FileUtils.getAsOpen("js/lib/" + name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    static JSEngine instance = null;

    public static JSEngine getInstance() {
        if (instance == null)
            instance = new JSEngine();
        return instance;
    }

    public void stopAll() {
        OkGo.getInstance().cancelTag("js_okhttp_tag");
        jsContext.close();
    }

    JSEngine () {
        if(jsContext == null){
            jsContext = QuickJSContext.create();
        }
        JSModule.setModuleLoader(this::loadModule);
        initProperty();
        initLocalStorage();
        initConsole();
    }
    void initConsole() {
        jsContext.evaluate("var console = {};");
        JSObject console = (JSObject) jsContext.getGlobalObject().getProperty("console");
        console.setProperty("log", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                StringBuilder b = new StringBuilder();
                for (Object o : args) {
                    b.append(o == null ? "null" : o.toString());
                }
                System.out.println(TAG + " >>> " + b.toString());
                Log.println(Log.DEBUG, "QuickJS", b.toString());
                return null;
            }
        });
    }

    void initLocalStorage() {
        jsContext.evaluate("var local = {};");
        JSObject console = (JSObject) jsContext.getGlobalObject().getProperty("local");
        console.setProperty("get", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                try {
                    return Hawk.get("jsRuntime_" + args[0].toString() + "_" + args[1].toString(), "");
                } catch (Exception e) {
                    Hawk.delete("jsRuntime_" + args[0].toString() + "_" + args[1].toString());
                    return args[2].toString();
                }
            }
        });
        console.setProperty("set", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                try {
                    Hawk.put("jsRuntime_" + args[0].toString() + "_" + args[1].toString(), args[2].toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        console.setProperty("delete", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                try {
                    Hawk.delete("jsRuntime_" + args[0].toString() + "_" + args[1].toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }
    @Keep
    @JSMethod(alias = "req")
    public Object ajax(@JSParameter("url") String url, @JSParameter("o2") Object o2) {
        try {
            JSONObject opt = ((JSObject) o2).toJSONObject();
            Headers.Builder headerBuilder = new Headers.Builder();
            JSONObject optHeader = opt.optJSONObject("headers");
            if (optHeader != null) {
                Iterator<String> hdKeys = optHeader.keys();
                while (hdKeys.hasNext()) {
                    String k = hdKeys.next();
                    String v = optHeader.optString(k);
                    headerBuilder.add(k, v);
                }
            }
            Headers headers = headerBuilder.build();
            Request.Builder requestBuilder = new Request.Builder().url(url).headers(headers);
            Object tag = getTagFromThread();
            if (tag != null) {
                requestBuilder.tag(tag);
            } else {
                requestBuilder.tag("js_okhttp_tag");
            }
            Request request;
            String contentType = null;
            if (!JSUtils.isEmpty(headers.get("content-type"))) {
                contentType = headers.get("Content-Type");
            }
            String method = opt.optString("method").toLowerCase();
            String charset = null;
            if (contentType != null && contentType.split("charset=").length > 1) {
                charset = contentType.split("charset=")[1];
            }

            if (method.equals("post")) {
                RequestBody body = null;
                String data = opt.optString("data", "").trim();
                if (!data.isEmpty()) {
                    body = RequestBody.create(MediaType.parse("application/json"), data);
                }
                if (body == null) {
                    String dataBody = opt.optString("body", "").trim();
                    if (!dataBody.isEmpty() && contentType != null) {
                        body = RequestBody.create(MediaType.parse(contentType), opt.optString("body", ""));
                    }
                }
                if (body == null) {
                    body = RequestBody.create(null, "");
                }
                request = requestBuilder.post(body).build();
            } else if (method.equals("header")) {
                request = requestBuilder.head().build();
            } else {
                request = requestBuilder.get().build();
            }
            okHttpClient = opt.optInt("redirect", 1) == 1 ? OkGoHelper.getDefaultClient() : OkGoHelper.getNoRedirectClient();
            OkHttpClient.Builder builder = okHttpClient.newBuilder();
            if (opt.has("timeout")) {
                long timeout = opt.optInt("timeout");
                builder.readTimeout(timeout, TimeUnit.MILLISECONDS);
                builder.writeTimeout(timeout, TimeUnit.MILLISECONDS);
                builder.connectTimeout(timeout, TimeUnit.MILLISECONDS);
            }
            Response response = builder.build().newCall(request).execute();
            JSObject jsObject = jsContext.createNewJSObject();
            Set<String> resHeaders = response.headers().names();
            JSObject resHeader = jsContext.createNewJSObject();
            for (String header : resHeaders) {
                resHeader.setProperty(header, response.header(header));
            }
            jsObject.setProperty("headers", resHeader);
            jsObject.setProperty("code", response.code());

            int returnBuffer = opt.optInt("buffer", 0);
            if (returnBuffer == 1) {
                JSArray array = jsContext.createNewJSArray();
                byte[] bytes = response.body().bytes();
                for (int i = 0; i < bytes.length; i++) {
                    array.set(bytes[i], i);
                }
                jsObject.setProperty("content", array);
            } else if (returnBuffer == 2) {
                jsObject.setProperty("content", Base64.encodeToString(response.body().bytes(), Base64.DEFAULT));
            } else {
                String res;
                byte[] responseBytes = UTF8BOMFighter.removeUTF8BOM(response.body().bytes());
                MediaType mediaType = response.body().contentType();
                Charset charsets = null;
                if (mediaType != null) {
                    charsets = mediaType.charset();
                }
                if (!JSUtils.isEmpty(charset)) {
                    res = new String(responseBytes, Charset.forName(charset));
                } else if (charsets != null) { //根据http头判断
                    res = new String(responseBytes, charsets);
                } else {
                    //根据内容判断
                    res = new String(responseBytes, Charset.forName(EncodingDetect.getEncodeInHtml(responseBytes)));
                }
                jsObject.setProperty("content", res);
            }
            return jsObject;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return "";
    }

    @Keep
    @JSMethod(alias = "bf")
    public Object batchFetch(@JSParameter("params") Object params) {
        JSArray res = (JSArray) params;
        JSONArray jsonArray = res.toJSONArray();
        return jsContext.parseJSON(new Gson().toJson(batchExecute(jsonArray, (url, options) -> (String) ajax(url, options))));
    }

    private List<String> batchExecute(JSONArray jsonArray, UrlTaskExecutor executor) {
        Map<Integer, String> indexMap = new ConcurrentHashMap<>();
        try {
            String tag = FileUtils.genUUID();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                String url = jsonObject.getString("url");
                Object options = jsonObject.get("options");
                if (JSUtils.isEmpty(url)) {
                    continue;
                }
                addTagForThread(tag);
                try {
                    String s = executor.execute(url, options);
                    indexMap.put(i, s);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Thread.sleep((long) jsonArray.length() / 16 * 1000 + 10000);//毫秒
            cancelByTag(tag);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String[] data = new String[jsonArray.length()];
        for (Map.Entry<Integer, String> entry : indexMap.entrySet()) {
            data[entry.getKey()] = entry.getValue();
        }
        return new ArrayList<>(Arrays.asList(data));
    }

    private interface UrlTaskExecutor {
        String execute(String url, Object options);
    }

    @Keep
    @JSMethod(alias = "be")
    public void batchExecute(@JSParameter("tasks") Object task, @JSParameter("listener0") Object listener0,
                             @JSParameter(value = "success", defaultInt = 0) int success) {
        JSArray tasks = task == null ? null : (JSArray) task;
        JSObject listener = listener0 == null ? null : (JSObject) listener0;
        JSFunction listenerFunc = listener != null && listener.contains("func") ? listener.getJSFunction("func") : null;
        if (success <= 0 || success > tasks.length()) {
            success = tasks.length();
        }
        String tag = FileUtils.genUUID();
        for (int i = 0; i < success; i++) {
            JSObject jsonObject = (JSObject) tasks.get(i);
            addTagForThread(tag);
            String id = jsonObject.getString("id");
            String error = null;
            Object result = null;
            try {
                result = jsonObject.getJSFunction("func").call(jsonObject.getProperty("param"));
            } catch (Exception e) {
                e.printStackTrace();
                error = e.getMessage();
            } finally {
                try {
                    if (listenerFunc != null) {
                        Object res = listenerFunc.call(listener.getProperty("param"), id, error, result);
                        if (res != null) {
                            //用户主动返回了break表示已经获取到想要的结果了
                            if ("break".equals((String) res)) {
                                cancelByTag(tag);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Keep
    @JSMethod
    public void toast(@JSParameter("str") String str) {
        App.getInstance().getHomeActivity().runOnUiThread(() -> Toast.makeText(App.getInstance(), str, Toast.LENGTH_SHORT).show());
    }

    @Keep
    @JSMethod
    public void showLoading(@JSParameter("str") String str) {
        EventBus.getDefault().post(new LoadingEvent(str, true));
    }

    @Keep
    @JSMethod
    public void hideLoading() {
        EventBus.getDefault().post(new LoadingEvent(null, false));
    }

    @Keep
    @JSMethod
    public String joinUrl(@JSParameter("parent") String parent, @JSParameter("child") String child) {
        return HtmlParser.joinUrl(parent, child);
    }

    /**
     * 解析dom
     *
     * @return
     */
    @Keep
    @JSMethod(alias = "pdfh")
    public String parseDomForHtml(@JSParameter("html") String html, @JSParameter("rule") String rule) {
        return HtmlParser.parseDomForUrl(html, rule, "");
    }

    /**
     * 解析dom
     *
     * @return
     */
    @Keep
    @JSMethod(alias = "pdfa")
    public Object parseDomForArray(@JSParameter("html") String html, @JSParameter("rule") String rule) {
        return jsContext.parseJSON(new Gson().toJson(HtmlParser.parseDomForList(html, rule)));
    }

    /**
     * 解析dom
     *
     * @return
     */
    @Keep
    @JSMethod(alias = "pd")
    public String parseDom(@JSParameter("html") String html, @JSParameter("rule") String rule, @JSParameter("urlKey") String urlKey) {
        return HtmlParser.parseDomForUrl(html, rule, urlKey);
    }

    private void addTagForThread(Object tag) {
        Map<String, Object> map = threadMap.get();
        if (map == null) {
            map = new HashMap<>();
            threadMap.set(map);
        }
        map.put("_tag", tag);
    }

    public Object getTagFromThread() {
        Map<String, Object> map = threadMap.get();
        if (map == null) {
            return null;
        }
        return map.get("_tag");
    }

    public void cancelByTag(Object tag) {
        try {
            if (okHttpClient != null) {
                for (Call call : okHttpClient.dispatcher().queuedCalls()) {
                    if (tag.equals(call.request().tag())) {
                        call.cancel();
                    }
                }
                for (Call call : okHttpClient.dispatcher().runningCalls()) {
                    if (tag.equals(call.request().tag())) {
                        call.cancel();
                    }
                }
            }
            OkGo.getInstance().cancelTag(tag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initProperty() {
        Method[] methods = this.getClass().getMethods();
        for (Method method : methods) {
            JSMethod an = method.getAnnotation(JSMethod.class);
            if (an == null) continue;
            String functionName = method.getName();

            getGlobalObj().setProperty(functionName, args -> {
                try {
                    return method.invoke(this, getParameters(method, args));
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });

            if (JSUtils.isNotEmpty(an.alias())) {
                jsContext.evaluate("var " + an.alias() + " = " + functionName + ";\n");
            }
        }
    }

    private Object[] getParameters(Method method, Object... args) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] paramCheck = new Object[parameterAnnotations.length];
        JSObject job = getGlobalObj();
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
}
