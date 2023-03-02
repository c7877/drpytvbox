package com.github.tvbox.osc.util.js;

import android.content.Context;

import com.github.catvod.crawler.Spider;
import com.github.tvbox.osc.util.FileUtils;
import com.quickjs.android.JSArray;
import com.quickjs.android.JSModule;
import com.quickjs.android.JSObject;

import java.util.HashMap;
import java.util.List;

public class SpiderJS extends Spider {

    private final String key;
    private String js;
    private final String ext;
    private JSObject jsObject = null;

    public SpiderJS(String key, String js, String ext) {
        this.key = key;
        this.js = js;
        this.ext = ext;
    }

    void checkLoaderJS() {
        if (jsObject == null) {
            try {
                String moduleKey = key;
                String jsContent = FileUtils.loadModule(js);
                try {
                    if (js.contains(".js?")) {
                        int spIdx = js.indexOf(".js?");
                        String[] query = js.substring(spIdx + 4).split("[&=]");
                        js = js.substring(0, spIdx);
                        for (int i = 0; i < query.length; i += 2) {
                            String key = query[i];
                            String val = query[i + 1];
                            String sub = JSModule.convertModuleName(js, val);
                            String content = FileUtils.loadModule(sub);
                            jsContent = jsContent.replace("__" + key.toUpperCase() + "__", content);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (jsContent.contains("export default {") || jsContent.contains("export default{")) {
                    jsContent = jsContent.replace("export default {", "globalThis." + moduleKey + " = {")
                            .replace("export default{", "globalThis." + moduleKey + " = {");
                }
                jsContent = jsContent.replace("__JS_SPIDER__", "globalThis." + moduleKey);
                JSEngine.getInstance().getJsContext().evaluateModule(jsContent, js);
                jsObject = (JSObject) JSEngine.getInstance().jsContext.getProperty(JSEngine.getInstance().getGlobalObj(), moduleKey);
                jsObject.getJSFunction("init").call(ext);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    String jsEvel(String func, Object... args) {
        checkLoaderJS();
        if (jsObject != null) {
            try {
                return (String) jsObject.getJSFunction(func).call(args);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        return "";
    }

    @Override
    public void init(Context context, String extend) {
        super.init(context, extend);
        checkLoaderJS();
    }

    @Override
    public String homeContent(boolean filter) {
        return jsEvel("home", filter);
    }

    @Override
    public String homeVideoContent() {
        return jsEvel("homeVod");
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        try {
            JSObject obj = JSEngine.getInstance().getJsContext().createNewJSObject();
                if (extend != null) {
                    for (String s : extend.keySet()) {
                        obj.setProperty(s, extend.get(s));
                    }
                }
            return jsEvel("category", tid, pg, filter, obj);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return "";

    }

    @Override
    public String detailContent(List<String> ids) {
        return jsEvel("detail", ids.get(0));
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        try {
            JSArray array = JSEngine.getInstance().getJsContext().createNewJSArray();
                if (vipFlags != null) {
                    for (int i = 0; i < vipFlags.size(); i++) {
                        array.set(vipFlags.get(i), i);
                    }
                }
            return jsEvel("play", flag, id, array);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return "";
    }

    @Override
    public String searchContent(String key, boolean quick) {
        return jsEvel("search", key, quick);
    }
}
