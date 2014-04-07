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
    
    //you can set the drawableFactory for the image process(such as radious, shawdown....)
    //and set the loading drawable, fallback drawable for the action
    UIO.bind(imageView,imageUrl,drawableFactory);

#### Utility
-------
 * KeyValueStorage
 * SQLiteMapping
 * some network state getter and listener register
 * other utility 
 
#### Other Documents
----
  * [中文版](doc/README_zh.md)
  * [English Version](doc/README_en.md)

 
