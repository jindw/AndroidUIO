AndroidUIO
==========

Simple Android UI&amp;IO Utility 



IO
----
 * Auto cannceled http request
    * simple callback 
    * auto canceled
    * auto cached
    
```java
    //the http request will be canceled after activity finished
    //and auto paused after the activity is paused when the task is pauseable.
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
```
    
 * auto binded image view and image url
    * simple bind method 
    * auto counting and gc
    * auto gif movie support
    
```java
    //the image url will auto load and parse and set as the drawable for the imageView 
    //if the image url is a animated gif,then it is show as a movie!
    UIO.bind(imageView,imageUrl);
    
    //you can set the drawableFactory for the image process(such as radious, shawdown....)
    //and set the loading drawable, fallback drawable for the action
    UIO.bind(imageView,imageUrl,drawableFactory);
    
```

 * auto http stream to java object

    define your java class and the json convert to java object automatic
    
 * KeyValueStorage
 * SQLiteMapping
 * some network check and listener registroy  unitity 
    

UI 
----
 * Get your context whenever and wherever possible 

   get your current application Context or current Activity（top Activity）
 * some simple UI static method

    show toast with simple static mathod 
 * and other
 
  
Others
----
 * automatic lifecycle management
