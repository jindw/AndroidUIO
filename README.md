Description
----
####Http Request && Cache && schedule automatic 
--------
	//the http request will be canceled after activity finished
    //and auto paused after the activity is paused when the task is pauseable.
    //and auto resume after the activity is resume again.
    Cancelable task = UIO.get(new Callback<MyClass>(){
        //http stream will be auto parsed and mapping to you standard javabean object.
         public void callback(MyClass javabean){
             //TODO:...
         }
         public void error(Throwable ex, boolean callbackError){
             //TODO:....
         }
    });
    // and you can cancel it any time by yourself
    // task.cancel();
    
    // double callback (cache first show and update on need)
    Cancelable task = UIO.get(new CacheCallback<MyClass>(){
        //http stream will be auto parsed and mapping to you standard javabean object.
         public void callback(MyClass javabean){
            //TODO: on cache parsed!
         }
         public void update(MyClass javabean){
         	//TODO: on new network result（null for not modified data）
         }
         public void error(Throwable ex, boolean callbackError){
            //TODO:....
         }
    });

#### Image Bind
--------
    //the image url will auto load and parse and set as the drawable for the imageView 
    //if the image url is a animated gif,then it is show as a movie!
    UIO.bind(imageView,imageUrl);
    
    //you can set the drawableFactory for the image loading(getLoadingDrawable) and process(such as radious, shawdown....)
    //and set the fallback drawable for the action
    UIO.bind(imageView,imageUrl,drawableFactory,fallbackResourceId);
    
    
    //you can set the callback for the bind action(callback on image load(or cached) and bind to image view)
    UIO.bind(imageView, imageUrl,drawableFactory,fallbackResourceId,  callback);//Callback<Drawable> 

#### Utility
-------
 * KeyValueStorage
 	Replace the SharePreferences interface. the keyValueStroage is a dsl for typed key value stroage.
 	 
 * SQLiteMapping
 	a simple ormapping.
 * Network State
   * network state getter 
   		UIO.isInternetConnected()
   		UIO.isWifiConnected()
   		UIO.getMobileGeneration()
   * and listener register
		UIO.Ext.addWifiCallback(Callback<Boolean> wifiAvaliableCallback);
		UIO.Ext.addNetworkCallback(Callback<Boolean> networkAvaliableCallback);
		UIO.Ext.removeWifiCallback && UIO.Ext.removeNetworkCallback(callback)
 * other utility 
 	//show toast and clear on activity destroyed
 	UIO.showLongTips("long show");
 	UIO.showShortTips("short show");
 
#### Other Documents
----
  * [中文版](doc/README_zh.md)
  * [English Version](doc/README_en.md)

 
