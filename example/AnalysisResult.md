
## FlowDroid taint path:

***

**Source**: (J)(RET)`<android.location.LocationManager: android.location.Location getLastKnownLocation(java.lang.String)>`

**Sink**: (J)(ARGS)`<android.util.Log: int d(java.lang.String,java.lang.String)>`

**Path**:

	--> `$r4 = virtualinvoke $r3.<android.location.LocationManager: android.location.Location getLastKnownLocation(java.lang.String)>("network")`

	--> `$r5 = virtualinvoke $r5.<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.Object)>($r4)`

	--> `$r2 = virtualinvoke $r5.<java.lang.StringBuilder: java.lang.String toString()>()`

	--> `virtualinvoke $r1.<com.lynnlyc.webview.MyActivity: void log(java.lang.String)>($r2)`

	--> `staticinvoke <android.util.Log: int d(java.lang.String,java.lang.String)>("WebviewActivity", $r1)`

***

***

**Source**: (J)(RET)`<HybridFlow: java.lang.Object mockSource0_source()>`

**Sink**: (J)(ARGS)`<HybridFlow: void mockSink1_sink(java.lang.Object)>`

**Path**:

	--> `$r4 = staticinvoke <HybridFlow: java.lang.Object mockSource0_source()>()`

	--> `staticinvoke <HybridFlow: void mockSink1_sink(java.lang.Object)>($r4)`

***

***

**Source**: (J)(RET)`<HybridFlow: java.lang.Object mockSource2_logInApp()>`

**Sink**: (J)(ARGS)`<android.os.Handler: boolean sendMessage(android.os.Message)>`

**Path**:

	--> `$r4 = staticinvoke <HybridFlow: java.lang.Object mockSource2_logInApp()>()`

	--> `r11 = (java.lang.String) $r4`

	--> `virtualinvoke $r5.<com.lynnlyc.webview.WebviewDemoInterface: void logInApp(java.lang.String)>(r11)`

	--> `$r3.<android.os.Message: java.lang.Object obj> = $r1`

	--> `virtualinvoke $r6.<android.os.Handler: boolean sendMessage(android.os.Message)>($r3)`

***

***

**Source**: (J)(RET)`<HybridFlow: java.lang.Object mockSource2_logInApp()>`

**Sink**: (J)(ARGS)`<android.util.Log: int d(java.lang.String,java.lang.String)>`

**Path**:

	--> `$r4 = staticinvoke <HybridFlow: java.lang.Object mockSource2_logInApp()>()`

	--> `r11 = (java.lang.String) $r4`

	--> `virtualinvoke $r5.<com.lynnlyc.webview.WebviewDemoInterface: void logInApp(java.lang.String)>(r11)`

	--> `staticinvoke <android.util.Log: int d(java.lang.String,java.lang.String)>("JSInterface", $r1)`

***


## HTML taint path:

***

**Source**: (H)(RET)`webviewdemo,getSystemResource`

**Sink**: (H)(ARGS)`webviewdemo,getSystemResource`

**Path**:

	--> `{{HTML <(RET) webviewdemo,getSystemResource> -> _SOURCE_}}`

	--> `getSystemResource,global,v30,webviewdemo,window`

	--> `getSystemResource,global,log,v30,webviewdemo,window`

	--> `{{HTML <(ARGS) webviewdemo,getSystemResource> -> _SINK_}}`

***

***

**Source**: (H)(RET)`webviewdemo,getSystemResource`

**Sink**: (H)(ARGS)`window,console`

**Path**:

	--> `{{HTML <(RET) webviewdemo,getSystemResource> -> _SOURCE_}}`

	--> `getSystemResource,global,v30,webviewdemo,window`

	--> `console,global,log,logtag,v20,v3,window`

	--> `{{HTML <(ARGS) window,console> -> _SINK_}}`

***

***

**Source**: (H)(RET)`webviewdemo,getAppResource`

**Sink**: (H)(ARGS)`webviewdemo,getAppResource`

**Path**:

	--> `{{HTML <(RET) webviewdemo,getAppResource> -> _SOURCE_}}`

	--> `getAppResource,global,v30,webviewdemo,window`

	--> `getAppResource,global,log,v30,webviewdemo,window`

	--> `{{HTML <(ARGS) webviewdemo,getAppResource> -> _SINK_}}`

***

***

**Source**: (H)(RET)`webviewdemo,getAppResource`

**Sink**: (H)(ARGS)`window,console`

**Path**:

	--> `{{HTML <(RET) webviewdemo,getAppResource> -> _SOURCE_}}`

	--> `getAppResource,global,v30,webviewdemo,window`

	--> `console,global,log,logtag,v20,v3,window`

	--> `{{HTML <(ARGS) window,console> -> _SINK_}}`

***

***

**Source**: (H)(RET)`document,getElementById`

**Sink**: (H)(ARGS)`window,console`

**Path**:

	--> `{{HTML <(RET) document,getElementById> -> _SOURCE_}}`

	--> `document,getElementById,global,resource,v19,v8,window`

	--> `global,msg`

	--> `app,console,global,log,msg,sending,to,v34,v38,v44,window`

	--> `{{HTML <(ARGS) window,console> -> _SINK_}}`

***

***

**Source**: (H)(RET)`document,getElementById`

**Sink**: (H)(ARGS)`webviewdemo,logInApp`

**Path**:

	--> `{{HTML <(RET) document,getElementById> -> _SOURCE_}}`

	--> `document,getElementById,global,resource,v19,v8,window`

	--> `global,msg`

	--> `global,logInApp,msg,v54,webviewdemo,window`

	--> `{{HTML <(ARGS) webviewdemo,logInApp> -> _SINK_}}`

***

***

**Source**: (H)(RET)`document,getElementById`

**Sink**: (H)(ARGS)`interface,saveInfo`

**Path**:

	--> `{{HTML <(RET) document,getElementById> -> _SOURCE_}}`

	--> `document,getElementById,global,pwd,v20,v22`

	--> `document,getElementById,global,interface,pwd,saveInfo,v12,v20,v22,value,window`

	--> `{{HTML <(ARGS) interface,saveInfo> -> _SINK_}}`

***

***

**Source**: (H)(RET)`document,getElementById`

**Sink**: (H)(ARGS)`interface,saveInfo`

**Path**:

	--> `{{HTML <(RET) document,getElementById> -> _SOURCE_}}`

	--> `document,getElementById,global,pwd,v20,v22`

	--> `document,getElementById,global,interface,pwd,saveInfo,v12,v20,v22,value,window`

	--> `{{HTML <(ARGS) interface,saveInfo> -> _SINK_}}`

***


## Hybrid bridges:

***

**Source**: (J)(ARGS)`<android.webkit.WebView: void loadUrl(java.lang.String)>`

**Sink**: (H)(CODE)`http://XXXXX`

**Path**:

	--> `(J)(ARGS)<android.webkit.WebView: void loadUrl(java.lang.String)>`

	--> `(H)(CODE)http://XXXXX`

***

***

**Source**: (J)(ARGS)`<HybridFlow: void mockSink5_getSystemResource(java.lang.Object)>`

**Sink**: (H)(RET)`webviewdemo,getSystemResource`

**Path**:

	--> `(J)(ARGS)<HybridFlow: void mockSink5_getSystemResource(java.lang.Object)>`

	--> `(H)(RET)webviewdemo,getSystemResource`

***

***

**Source**: (H)(ARGS)`webviewdemo,logInApp`

**Sink**: (J)(RET)`<HybridFlow: java.lang.Object mockSource2_logInApp()>`

**Path**:

	--> `(H)(ARGS)webviewdemo,logInApp`

	--> `(J)(RET)<HybridFlow: java.lang.Object mockSource2_logInApp()>`

***

***

**Source**: (H)(ARGS)`interface,saveInfo`

**Sink**: (J)(RET)`<HybridFlow: java.lang.Object mockSource8_saveInfo()>`

**Path**:

	--> `(H)(ARGS)interface,saveInfo`

	--> `(J)(RET)<HybridFlow: java.lang.Object mockSource8_saveInfo()>`

***

***

**Source**: (H)(ARGS)`webviewdemo,getSystemResource`

**Sink**: (J)(RET)`<HybridFlow: java.lang.Object mockSource4_getSystemResource()>`

**Path**:

	--> `(H)(ARGS)webviewdemo,getSystemResource`

	--> `(J)(RET)<HybridFlow: java.lang.Object mockSource4_getSystemResource()>`

***

***

**Source**: (H)(ARGS)`window,confirm`

**Sink**: (J)(RET)`<HybridFlow: java.lang.Object mockSource10_onJsConfirm()>`

**Path**:

	--> `(H)(ARGS)window,confirm`

	--> `(J)(RET)<HybridFlow: java.lang.Object mockSource10_onJsConfirm()>`

***

***

**Source**: (J)(ARGS)`<android.webkit.WebView: void loadUrl(java.lang.String)>`

**Sink**: (H)(CODE)`taintjs_1`

**Path**:

	--> `(J)(ARGS)<android.webkit.WebView: void loadUrl(java.lang.String)>`

	--> `(H)(CODE)taintjs_1`

***

***

**Source**: (J)(ARGS)`<android.webkit.WebView: void loadUrl(java.lang.String)>`

**Sink**: (H)(CODE)`taintjs_2`

**Path**:

	--> `(J)(ARGS)<android.webkit.WebView: void loadUrl(java.lang.String)>`

	--> `(H)(CODE)taintjs_2`

***

***

**Source**: (H)(ARGS)`window,alert`

**Sink**: (J)(RET)`<HybridFlow: java.lang.Object mockSource11_onJsAlert()>`

**Path**:

	--> `(H)(ARGS)window,alert`

	--> `(J)(RET)<HybridFlow: java.lang.Object mockSource11_onJsAlert()>`

***

***

**Source**: (H)(PUT)`window,location,href`

**Sink**: (J)(RET)`<HybridFlow: java.lang.Object mockSource14_shouldOverrideUrlLoading()>`

**Path**:

	--> `(H)(PUT)window,location,href`

	--> `(J)(RET)<HybridFlow: java.lang.Object mockSource14_shouldOverrideUrlLoading()>`

***

***

**Source**: (H)(ARGS)`window,console`

**Sink**: (J)(RET)`<HybridFlow: java.lang.Object mockSource12_onConsoleMessage()>`

**Path**:

	--> `(H)(ARGS)window,console`

	--> `(J)(RET)<HybridFlow: java.lang.Object mockSource12_onConsoleMessage()>`

***

***

**Source**: (J)(ARGS)`<android.webkit.WebView: void loadUrl(java.lang.String)>`

**Sink**: (H)(CODE)`taintjs_0`

**Path**:

	--> `(J)(ARGS)<android.webkit.WebView: void loadUrl(java.lang.String)>`

	--> `(H)(CODE)taintjs_0`

***

***

**Source**: (J)(ARGS)`<HybridFlow: void mockSink7_getAppResource(java.lang.Object)>`

**Sink**: (H)(RET)`webviewdemo,getAppResource`

**Path**:

	--> `(J)(ARGS)<HybridFlow: void mockSink7_getAppResource(java.lang.Object)>`

	--> `(H)(RET)webviewdemo,getAppResource`

***

***

**Source**: (H)(ARGS)`window,prompt`

**Sink**: (J)(RET)`<HybridFlow: java.lang.Object mockSource13_onJsPrompt()>`

**Path**:

	--> `(H)(ARGS)window,prompt`

	--> `(J)(RET)<HybridFlow: java.lang.Object mockSource13_onJsPrompt()>`

***

***

**Source**: (J)(ARGS)`<HybridFlow: void mockSink3_logInApp(java.lang.Object)>`

**Sink**: (H)(RET)`webviewdemo,logInApp`

**Path**:

	--> `(J)(ARGS)<HybridFlow: void mockSink3_logInApp(java.lang.Object)>`

	--> `(H)(RET)webviewdemo,logInApp`

***

***

**Source**: (H)(ARGS)`webviewdemo,getAppResource`

**Sink**: (J)(RET)`<HybridFlow: java.lang.Object mockSource6_getAppResource()>`

**Path**:

	--> `(H)(ARGS)webviewdemo,getAppResource`

	--> `(J)(RET)<HybridFlow: java.lang.Object mockSource6_getAppResource()>`

***

***

**Source**: (J)(ARGS)`<HybridFlow: void mockSink9_saveInfo(java.lang.Object)>`

**Sink**: (H)(RET)`interface,saveInfo`

**Path**:

	--> `(J)(ARGS)<HybridFlow: void mockSink9_saveInfo(java.lang.Object)>`

	--> `(H)(RET)interface,saveInfo`

***

***

**Source**: (J)(ARGS)`<android.webkit.WebView: void loadUrl(java.lang.String)>`

**Sink**: (H)(CODE)`http://lynnblog.sinaapp.com/webview`

**Path**:

	--> `(J)(ARGS)<android.webkit.WebView: void loadUrl(java.lang.String)>`

	--> `(H)(CODE)http://lynnblog.sinaapp.com/webview`

***



## Merged taint paths:
***

**Source**: (H)(RET)`document,getElementById`

**Sink**: (J)(ARGS)`<android.os.Handler: boolean sendMessage(android.os.Message)>`

**Path**:

	--> `{{HTML <(RET) document,getElementById> -> _SOURCE_}}`

	--> `document,getElementById,global,resource,v19,v8,window`

	--> `global,msg`

	--> `global,logInApp,msg,v54,webviewdemo,window`

	--> `{{HTML <(ARGS) webviewdemo,logInApp> -> _SINK_}}`

	--> `(H)(ARGS)webviewdemo,logInApp`

	--> `(J)(RET)<HybridFlow: java.lang.Object mockSource2_logInApp()>`

	--> `$r4 = staticinvoke <HybridFlow: java.lang.Object mockSource2_logInApp()>()`

	--> `r11 = (java.lang.String) $r4`

	--> `virtualinvoke $r5.<com.lynnlyc.webview.WebviewDemoInterface: void logInApp(java.lang.String)>(r11)`

	--> `$r3.<android.os.Message: java.lang.Object obj> = $r1`

	--> `virtualinvoke $r6.<android.os.Handler: boolean sendMessage(android.os.Message)>($r3)`

***

***

**Source**: (J)(RET)`<android.location.LocationManager: android.location.Location getLastKnownLocation(java.lang.String)>`

**Sink**: (J)(ARGS)`<android.util.Log: int d(java.lang.String,java.lang.String)>`

**Path**:

	--> `$r4 = virtualinvoke $r3.<android.location.LocationManager: android.location.Location getLastKnownLocation(java.lang.String)>("network")`

	--> `$r5 = virtualinvoke $r5.<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.Object)>($r4)`

	--> `$r2 = virtualinvoke $r5.<java.lang.StringBuilder: java.lang.String toString()>()`

	--> `virtualinvoke $r1.<com.lynnlyc.webview.MyActivity: void log(java.lang.String)>($r2)`

	--> `staticinvoke <android.util.Log: int d(java.lang.String,java.lang.String)>("WebviewActivity", $r1)`

***

***

**Source**: (H)(RET)`document,getElementById`

**Sink**: (J)(ARGS)`<android.util.Log: int d(java.lang.String,java.lang.String)>`

**Path**:

	--> `{{HTML <(RET) document,getElementById> -> _SOURCE_}}`

	--> `document,getElementById,global,resource,v19,v8,window`

	--> `global,msg`

	--> `global,logInApp,msg,v54,webviewdemo,window`

	--> `{{HTML <(ARGS) webviewdemo,logInApp> -> _SINK_}}`

	--> `(H)(ARGS)webviewdemo,logInApp`

	--> `(J)(RET)<HybridFlow: java.lang.Object mockSource2_logInApp()>`

	--> `$r4 = staticinvoke <HybridFlow: java.lang.Object mockSource2_logInApp()>()`

	--> `r11 = (java.lang.String) $r4`

	--> `virtualinvoke $r5.<com.lynnlyc.webview.WebviewDemoInterface: void logInApp(java.lang.String)>(r11)`

	--> `staticinvoke <android.util.Log: int d(java.lang.String,java.lang.String)>("JSInterface", $r1)`

***

