# What is the Hubitat Pool Controller?
A collection of devices designed to interface with a nodejs-poolController instance which is talking on the RS-485 bus to allow viewing and setting pool control options. Includes devices to manage the Pool pump, lights and heater, the spa pump and heater, the chlorinator, all Circuits and Features, and Intellichem devices.

This code is NO LONGER SUPPORTED with SmartThings with the removal of the "Classic": app. The [SHPL](https://github.com/SANdood/SmartThings-Hubitat-Portability-Library) is awesome and made compatibility possible in the past so thanks to [Barry Burke](https://github.com/SANdood)

# License
Copyright (C) 2017-2021  Brad Sileo / bsileo / brad@sileo.name

## Installation Instructions
1. Install and configure [Nodejs-Poolcontroller](https://github.com/tagyoureit/nodejs-poolController) (version 6+ is required!)
          https://github.com/tagyoureit/nodejs-poolController
          *Be sure to get the latest code form the Next branch*

## Note
This version is *NOT* compatible with the 5.3.3 version of nodejs-poolController. If you are using that version, consider upgrading! The last deprecated version of this code for use with 5.3.3 is available [here](https://github.com/bsileo/hubitat_poolcontroller/tree/NJPC-5.3.3). There is no forward migration path from that version to this version as all Apps and Drivers have been renamed and refactored.

## Hubitat - Package Manager

2. Install and configure the [Hubitat Package Manager](https://github.com/dcmeglio/hubitat-packagemanager) and use it to install this code

## Manual Approach
2. Open Apps Code, select "New App" and then either:

- Paste the code for the Master App into it and Save or Click Import and use this Import URL:

	* [pool_controller_app.groovy](https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/master/smartapps/bsileo/pool-controller.src/pool-controller.groovy)

3. Install all of the [Drivers](https://github.com/bsileo/hubitat_poolcontroller/tree/master/devicetypes/bsileo) into Drivers Code following this same procedure:

	* pool_controller.groovy
	* pool_controller_body.groovy
	* pool_controller_chlorinator.groovy
	* pool_controller_heater.groovy
	* pool_controller_intellibrite.groovy
	* pool_controller_intellichem.groovy
	* pool_controller_pump.groovy

## Setup your Installation
4. Go to Apps, Add User App and create a "Pool Controller" app. The Nodejs-Poolcontroller should be autolocated, or you can manually enter the details. Follow the prompts to complete installation.


## Setup the Event Interface

Enable the Event interface on the poolController to push updates to the hub. Look for a section like the following in config.json and make the appropriate changes to set the Host, Port (39501 for Hubitat) and set Enabled to true. You can include multiple instances of this section if you want to send events to several hubs.

```
 "hubitat": {
        "name": "hubitat",
        "enabled": true,
        "fileName": "smartThings-Hubitat.json",
        "globals": {},
        "options": {
          "host": "10.0.0.39",
          "port": 39501
        }
```


