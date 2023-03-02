package com.github.tvbox.osc.util.js;

import android.util.Log;

import androidx.annotation.Keep;

import com.quickjs.android.JSArray;
import com.quickjs.android.JSMethod;
import com.quickjs.android.JSObject;

import java.util.HashMap;
import java.util.Map;

public class ConsolePlugin {
    private int count;
    private final Map<String, Long> timer = new HashMap<>();
    @Keep
    @JSMethod
    public final void log(String msg) {
        count++;
        println(Log.DEBUG, msg);
    }
    @Keep
    @JSMethod
    public final void info(String msg) {
        count++;
        println(Log.INFO, msg);
    }
    @Keep
    @JSMethod
    public final void error(String msg) {
        count++;
        println(Log.ERROR, msg);
    }
    @Keep
    @JSMethod
    public final void warn(String msg) {
        count++;
        println(Log.WARN, msg);
    }
    @Keep
    public void println(int priority, String msg) {
        Log.println(priority, "QuickJS", msg);
    }
    @Keep
    @JSMethod
    public final int count() {
        return count;
    }

    @Keep
    @JSMethod
    public final void table(JSObject obj) {
        if (obj instanceof JSArray) {
            log(((JSArray) obj).toJSONArray().toString());
        } else if (obj != null) {
            log(obj.toJSONObject().toString());
        }
    }

    @Keep
    @JSMethod
    public final void time(String name) {
        if (timer.containsKey(name)) {
            warn(String.format("Timer '%s' already exists", name));
            return;
        }
        timer.put(name, System.currentTimeMillis());
    }
    @Keep
    @JSMethod
    public final void timeEnd(String name) {
        Long startTime = timer.get(name);
        if (startTime != null) {
            float ms = (System.currentTimeMillis() - startTime);
            log(String.format("%s: %s ms", name, ms));
        }
        timer.remove(name);
    }
    @Keep
    @JSMethod
    public void trace() {
        log("This 'console.trace' function is not supported");
    }
    @Keep
    @JSMethod
    public void clear() {
        log("This 'console.clear' function is not supported");
    }
    @Keep
    @JSMethod
    public void group(String name) {
        log("This 'console.group' function is not supported");
    }
    @Keep
    @JSMethod
    public void groupCollapsed(String name) {
        log("This 'console.groupCollapsed' function is not supported");
    }
    @Keep
    @JSMethod
    public void groupEnd(String name) {
        log("This 'console.groupEnd' function is not supported");
    }
}
