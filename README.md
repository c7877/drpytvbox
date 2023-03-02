# Box

App default settings can be set here :
/src/main/java/com/github/tvbox/osc/base/App.java

    private void initParams() {
        // Hawk
        Hawk.init(this).build();
        Hawk.put(HawkConfig.DEBUG_OPEN, false);

        putDefault(HawkConfig.HOME_REC, 1);       // Home Rec 0=豆瓣, 1=推荐, 2=历史
        putDefault(HawkConfig.PLAY_TYPE, 1);      // Player   0=系统, 1=IJK, 2=Exo
        putDefault(HawkConfig.IJK_CODEC, "硬解码");// IJK Render 软解码, 硬解码
        putDefault(HawkConfig.PLAY_RENDER, 1);// 渲染方式 0=texture, 1=surface
        putDefault(HawkConfig.PLAY_SPEED, true);// 倍速记忆
        putDefault(HawkConfig.HOME_SHOW_SOURCE, true);// 显示首页源
//        putDefault(HawkConfig.HOME_NUM, 2);       // History Number
//        putDefault(HawkConfig.DOH_URL, 2);        // DNS
//        putDefault(HawkConfig.SEARCH_VIEW, 1);    // Text or Picture

    }
