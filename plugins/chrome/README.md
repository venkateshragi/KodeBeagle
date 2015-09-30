# KodeBeagle chrome extension
This is a chrome browser extension, which helps users to search for code snippets from various open source repositories.
## Table of contents
* [Installation steps](#installation-steps)
    *  [From Github releases](#from-github-releases)
    *  [From chrome store](#from-chrome-store)
* [Using the chrome extension](#using-chrome-ext)
* [Developer notes](#dev-notes)
    * [Requirements.](#requirements)
    * [Loading unpacked extension.](#load-unpacked-ext)
    * [Packaging extension.](#packaging-ext)
    * [Updating extension.](#update-ext)
    * [Publishing extension.](#publish-ext)

<a id="installation-steps">
## Installation steps
<a>

<a id="from-github-releases">
#### From Github releases
<a>

1. Download the .crx file from the Github release or follow the steps on _"Packaging extension"_ below.
2. Open the chrome browser and type in - _**chrome://extensions**_. Drag and drop the .crx file on this page and it will instruct with a message as _**Drop to install**_,  now drop the extension.
![Chrome Ext](http://chrome-extension-downloader.com/images/chrome-extensions-drop.png)

<a id="from-chrome-store">
#### From Chrome store
<a>

- Soon the extension will be available in the chrome extensions factory for public use and once available we can install it from the factory directly.

<a id="using-chrome-ext">
## Using the chrome extension
<a>

Once the extension has been installed, it's time for us to make use of this extension.

1. Select either a collection of code phrases or a word on the browser window and right click, upon doing this we should see a context menu saying _**"Search this selection on KodeBeagle"**_

![](https://github.com/PrashanthAmbure/KodeBeagle/blob/chrome_ext/plugins/chrome/screenshots/Screen-2.png)
2. And a new tab opens up which lists out the code snippets for the selection from various repositories.

![](https://github.com/PrashanthAmbure/KodeBeagle/blob/chrome_ext/plugins/chrome/screenshots/Screen-3.png)

<a id="dev-notes">
## Developer notes
<a>

<a id="requirements">
### Requirements
<a>

* [NodeJS & NPM](http://nodejs.org/download)
* _gulp_ node package
* chrome browser

<a id="load-unpacked-ext">
### Loading unpacked extension.
<a>

1. Open the chrome browser and type in - _**chrome://extensions**_.
2. Ensure that the _"Developer mode"_ checkbox in the top right-hand corner is checked.
3. Click on _'Load unpacked extension'_ and choose the _src_ folder from the repo. Once the folder is uploaded we should be seeing something similar to below snap.
![](https://github.com/PrashanthAmbure/KodeBeagle/blob/chrome_ext/plugins/chrome/screenshots/Screen-4.png)
4. We're now ready to debug the extension code, to do so, open the _**backgroundpage**_ from the chrome extensions page and setup the breakpoints.  

<a id="packaging-ext">
### Packaging extension.
<a>

> This section is relevant only for the following three reasons...              
> 1 - Either the crx file is not available on the Github,   
> 2 - Or we would like to update the extension,     
> 3 - Or upload the extension to the chrome web store  
> **Note:** For #2 and #3 we will be using the same pem file.

Once the extension code gets stable, its time to create the .crx file which can be published to chrome extension factory for public use. Please note that chrome extensions are packaged as signed zip files with the file extension as _"crx"_.
**Note:** We package our own extension to distribute a non-public version.
1. Get the _.pem_ file from **_project leads_** and copy it to the root directory of the extension and do not change the .pem file name.
2. Open the terminal and execute _**sudo npm install**_ command which would download all the required node module dependencies mentioned in _package.json_.
3. Now run the gulp task using the following command - _**gulp create:crx**_. This command creates a .crx file under build directory .
4. Open the chrome browser and type in - _**chrome://extensions**_. Drag and drop the .crx file on this page and it will instruct with a message as _**Drop to install**_, now drop the extension.
![Chrome Ext](http://chrome-extension-downloader.com/images/chrome-extensions-drop.png)

<a id="updating-ext">
### Updating an extension
<a>

To create an updated version of your extension:
1. Increase the version number in manifest.json.
2. Follow the steps from _Packaging extension_

<a id="publish-ext">
### Publishing an extension
<a>

[Click here](https://developer.chrome.com/webstore/publish) to publish chrome extension.

Alternatively, please follow the steps in the below YouTube video.

[![Image](http://img.youtube.com/vi/Gn_jlvkHTnM/0.jpg)](https://www.youtube.com/watch?v=Gn_jlvkHTnM)
