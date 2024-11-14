# Jactl Intellij Plugin

This is a plugin for [Intellij IDEA](https://www.jetbrains.com/idea/) that adds support for editing/running/debugging
[Jactl](https://jactl.io) language scripts.

## Features

The following features are provided by the plugin:

* Syntax colouring
* Error highlighting
* Intelligent indenting
* Code reformat
* Completions
* Show definition/show usages navigation
* Comment/uncomment selected code
* Rename refactoring
* Move file/move directory refactoring
* Paired brace matching
* Build/run/debug support

## Installation

Within Intellij, navigate to **Settings... -> Plugins -> Marketplace**, search
for _Jactl_, and install the plugin.

## Configuration

Make sure that under **File -> Project Structure... -> Project Settings -> Modules** you configure the directory where your
Jactl scripts and class files will be located in your project (e.g. src/main/jactl).

You will need to also add the Jactl library to your project.
Under **File -> Project Structure... -> Project Settings -> Libraries** add a new library and select **Java** from the
drop-down list if you want to use a Jactl JAR you already have downloaded locally, or select **Maven** to download
a version from Maven.
If you are downloading from Maven, enter the Maven name for the version of the Jactl library.
For example:
```
io.jactl:jactl:2.1.0
```

### Globals Script

If you are using Jactl as an embedded scripting language within your application then when you invoke the Jactl
scripts you can pass in "global" variables (a map of name/value pairs) that the script can then access.
This is one way to share information between the application and the scripts.

The plugin supports the ability to specify what global variables exist.
This is used when editing for completions, and so that references to global variables don't show up as
errors.

It is use used during building so that references to unknown global variables don't cause compile errors
and is used during run/debug to get the actual values to pass into the script.

The plugin allows you to configure a default globals script file under
**Settings... -> Languages & Frameworks -> Jactl**.
Select the file that will be used to define the global variables.

The globals script file is a Jactl script itself that should return a map of names with values.

Note that the value types are what is used during editing and compiling but the values will be used when
running/debugging scripts.
For running/debugging, you can also override the globals script to use in the run configuration.

Here is an example globals variable script:
```groovy
[
  info: 'some text',
  context: [ sessionId: 12345 ],
  request: [ 
    msgType: 'PaymentReq',
    requestId: 123456789,
    merchant: [
      merchantId: 99999999,
      name: 'Frederick & Sons',
      address: [ street:'123 Main St', city:'Mainsville', state:'Mainstate', country:'Country' ]
    ],
    biller: [
      billerId: 88888888
    ]
  ]
]
```
