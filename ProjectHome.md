This application is a Java Webstart application that can be run from a web site without installing any desktop software and will publish your desktop screen as an RTMP video stream to a Red5 server using a standalone RTMP client and the Screen Video Codec.

It also includes a Flex viewer application which can be used to remotely control the desktop. Double click the desktop screen and follow the shadow mouse pointer. You should see two mouse pointers, your real one and the remote one lagging behind.

![http://red5-screenshare.googlecode.com/files/Image9.jpg](http://red5-screenshare.googlecode.com/files/Image9.jpg)

This application is based on the source code for the open-meetings desktop publisher, but converts the screen frames into a RTMP video stream on the client at source. It uses the Flazr library (http://www.flazr.com/)by Peter Thomas.

![http://red5-screenshare.googlecode.com/files/screenshare.jpg](http://red5-screenshare.googlecode.com/files/screenshare.jpg)

Unzip screenshare..zip and move the screenshare folder to webapps.

Edit screenshare.jnlp
```
<jnlp spec='1.0+' codebase='http://my_red5_server:5080/screenshare'> 

   <argument>my_red5_server</argument> 
   <argument>screenshare</argument> 
   <argument>1935</argument> 
   <argument>screen_share</argument> 
   <argument>flashsv2</argument> 

```

Replace my\_red5\_server with the hostname or ip address of your Red5 server.<p />
Leave 1935 unless your RTMP port is something else.<p />
Leave screenshare as the application name unless you are using your own application.<p />
Replace screen\_share with your choce of stream name. <p />
Keep the screen codec setting of flashsv2 unless it does not work, then try flashsv1<p />

Example for red5 server on localhost port 5080
```
<jnlp spec='1.0+' codebase='http://localhost:5080/screenshare'> 

   <argument>localhost</argument> 
   <argument>screenshare</argument> 
   <argument>1935</argument> 
   <argument>screen_share</argument> 
   <argument>flashsv2</argument> 

```

You can also run it from the command line. For example:

**java -classpath screenshare.jar org.redfire.screen.ScreenShare my\_red5\_server.com oflademo 1935 screen\_share flashsv2**

See do\_run1.cmd and do\_run2.cmd for Windows command files

Edit screenviewer.html
```

		var stream = getPageParameter('stream', 'screen_share');
		var url = getPageParameter('url', 'rtmp:/xmpp');
		var control = getPageParameter('control', 'true');
```

control is either true or false and determines if you allow remote desktop control or not.

To access the publisher, use

http://my_red5_server:5080/screenshare/screenshare.jnlp

To access viewer, use

http://my_red5_server:5080/screenshare/screenviewer.html