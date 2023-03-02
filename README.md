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
# git操作
1、问题描述：
本地提交文件到git,提交了不应该的或者不需要的文件或者目录

2、出现场景
例如pycharm工程提交时未将.idea添加至.ignore，错误将.idea文件夹提交到了git,或者安卓项目把build目录提交到了git

3、解决措施
通过git命令清除，步骤如下：
打开idea系列产品的terminal

3.1 $ ls # 查看有哪些文件夹 
3.2 $ cd # 切换到目标目录 

3.4 $ git rm -r --cached build # 删除build文件夹（删除的是缓存中的build）

3.5 $ git commit -m '删除编译目录' # 提交,添加操作说明（将缓存中的变动提交到本地仓库.idea）

3.6 $ git push -u origin master # 将本次更改更新到github项目上去（将本地仓库的变动同步到远程仓库）

# 问题反馈

[在线提交反馈信息](https://support.qq.com/product/513601)
