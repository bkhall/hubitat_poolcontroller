/**
 *  Copyright 2019 Brad Sileo
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

metadata {
	definition (name: "Pentair Water Thermostat", namespace: "bsileo", author: "Brad Sileo") {		
		capability "Temperature Measurement"		
		capability "Refresh"		
        capability "Switch"
        attribute "heatingSetpoint", "NUMBER"

        command "setHeaterMode", [[name:"Heater mode*","type":"ENUM","description":"Heater mode to set","constraints":["Off", "Heater", ,"Solar Pref","Solar Only"]]]
        attribute "heaterMode",  "string"
        
		command "lowerHeatingSetpoint"
		command "raiseHeatingSetpoint"			    	
	    command "heaterOn"
        command "heaterOff"
        command "nextMode"
	}

    preferences {     
         section("General:") {
            input (
        	name: "configLoggingLevelIDE",
        	title: "IDE Live Logging Level:\nMessages with this level and higher will be logged to the IDE.",
        	type: "enum",
        	options: [
        	    "0" : "None",
        	    "1" : "Error",
        	    "2" : "Warning",
        	    "3" : "Info",
        	    "4" : "Debug",
        	    "5" : "Trace"
        	],
        	defaultValue: "3",
            displayDuringSetup: true,
        	required: false
            )
        }
    }
    
	/*tiles {
		standardTile("mode", "device.thermostatMode", width:2, height:2, inactiveLabel: false, decoration: "flat") {
			state "OFF",  action:"nextMode",  nextState: "updating", icon: "st.thermostat.heating-cooling-off"
			state "Heater", action:"nextMode", nextState: "updating", icon: "st.thermostat.heat"	            
        	state "Solar Only", label:'${currentValue}', action:"nextMode",  nextState: "updating", icon: "https://bsileo.github.io/SmartThings_Pentair/solar-only.png"
            state "Solar Pref", label:'${currentValue}', action:"nextMode",  nextState: "updating", icon: "https://bsileo.github.io/SmartThings_Pentair/solar-preferred.jpg"
			state "updating", label:"Updating...", icon: "st.Home.home1"
		}
               
        multiAttributeTile(name:"temperature", type:"generic", width:3, height:2, canChangeIcon: true) {
			tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
				attributeState("temperature", label:'${currentValue}°',
					backgroundColors:[
							// Celsius
							[value: 0, color: "#153591"],
							[value: 7, color: "#1e9cbb"],
							[value: 15, color: "#90d2a7"],
							[value: 23, color: "#44b621"],
							[value: 28, color: "#f1d801"],
							[value: 35, color: "#d04e00"],
							[value: 37, color: "#bc2323"],
							// Fahrenheit
							[value: 40, color: "#153591"],
							[value: 44, color: "#1e9cbb"],
							[value: 59, color: "#90d2a7"],
							[value: 74, color: "#44b621"],
							[value: 84, color: "#f1d801"],
							[value: 95, color: "#d04e00"],
							[value: 96, color: "#bc2323"]
					]
				)
			}           
		}
		
                    
		standardTile("lowerHeatingSetpoint", "device.heatingSetpoint", width:2, height:1, inactiveLabel: false, decoration: "flat") {
			state "heatingSetpoint", action:"lowerHeatingSetpoint", icon:"st.thermostat.thermostat-left"
		}
		valueTile("heatingSetpoint", "device.heatingSetpoint", width:2, height:1, inactiveLabel: false, decoration: "flat") {
			state "heatingSetpoint", label:'${currentValue}° heat', backgroundColor:"#ffffff"
		}
		standardTile("raiseHeatingSetpoint", "device.heatingSetpoint", width:2, height:1, inactiveLabel: false, decoration: "flat") {
			state "heatingSetpoint", action:"raiseHeatingSetpoint", icon:"st.thermostat.thermostat-right"
		}
	
	    standardTile("refresh", "device.thermostatMode", width:2, height:2, inactiveLabel: false, decoration: "flat") {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		main "mode"
		details(["temperature", "lowerHeatingSetpoint", "heatingSetpoint", "raiseHeatingSetpoint","mode", "refresh"])
	}*/
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {	
    state.scale = "F"
}

def parse(String description) {
}


def refresh() {
	pollDevice()
}

def pollDevice() {
    getParent().poll()
}

def raiseHeatingSetpoint() {
	alterSetpoint(true)
}

def lowerHeatingSetpoint() {
	alterSetpoint(false)
}


// Adjusts nextHeatingSetpoint either .5° C/1° F) if raise true/false
def alterSetpoint(raise) {
	def locationScale = getTemperatureScale()
	def deviceScale = (state.scale == 1) ? "F" : "C"
	def heatingSetpoint = getTempInLocalScale("heatingSetpoint")
	def targetValue = heatingSetpoint
	def delta = (locationScale == "F") ? 1 : 0.5
	targetValue += raise ? delta : - delta
	
	sendEvent("name": "heatingSetpoint", "value": targetValue,
				unit: getTemperatureScale(), eventType: "ENTITY_UPDATE", displayed: true)    
    getParent().updateSetpoint(device,targetValue)
}

// set the local value for the heatingSetpoint. Doesd NOT update the parent / Pentair platform!!!
def setHeatingSetpoint(degrees) {
   log.debug "setHeatingSetpoint " + device.deviceNetworkId + "-" + degrees
	def timeNow = now()
    if (degrees) {	
    	if (!state.heatingSetpointTriggeredAt || (1 * 2 * 1000 < (timeNow - state.heatingSetpointTriggeredAt))) {
			state.heatingSetpointTriggeredAt = timeNow               
			state.heatingSetpoint = degrees.toDouble()
			sendEvent(name: "heatingSetpoint", value:state.heatingSetpoint, unit: getTemperatureScale())    	
		}
	}
}

// local action to move me to the next available heater mode and update the poolController
def nextMode() {
	logger("Going to nextMode....." + device.currentValue("temperature"),"trace")
    def currentMode = device.currentValue("heaterMode")
	def supportedModes = getModeMap()
    def nextIndex = 0;
    logger("${currentMode} moving to next in ${supportedModes}","trace")
    supportedModes.eachWithIndex {name, index ->
    	log.trace("${index}:${name} -->${nextIndex}  ${name} == ${currentMode}")
    	if (name == currentMode) { 
        	nextIndex = index +1
            return
         }
    }
    logger("nextMode id=${nextIndex}  compare to " + supportedModes.size(),"trace")    
    if (nextIndex >= supportedModes.size()) {nextIndex=0 }
    logger("Going to nextMode with id =${nextIndex}","debug")
    heaterToMode(nextIndex)
}

def getModeMap() { 
    def mm = null
    if (getParent().getDataValue("includeSolar")=='true') {
    	mm =  ["OFF",
            "Heater",
        	"Solar Pref",
        	"Solar Only"
     	]
    }
    else {
     mm = 
    	[
        "OFF",
        "Heater"     
     	]
    }
    return mm
}

// called by parent to me to change the mode locally in HT - these do NOT update poolController
// These do NOT update poolController!!
def switchToModeID(id) {
 	log.info("Going to mode ID ${id}")
	def mm = getModeMap()
    log.debug("Map it via ${mm} = ${mm[id]}")
	switchToMode(mm[id])
}

def switchToMode(nextMode) {
 	log.debug("switchToMode from parent--> '${nextMode}'")
   	sendEvent(name: "heaterMode", value: nextMode, displayed:true, descriptionText: "$device.displayName is in ${nextMode} mode")
}

def setThermostatMode(String value) {
	switchToMode(value)
}


// Commands
def on() {
    heaterOn()
}

def off() {
	heaterOff()
}

// Command actions locally to update the poolController with a new mode from my commands
def heaterOn() {
	// set it to mode 1
    log.debug("HEATER to ON ${device}")
	getParent().heaterOn(device)
}

def heaterOff() {
	// set it to mode 0
	log.debug("HEATER OFF ${device}")
	getparent().heaterOff(device)
}

def heaterToMode(modeID) {
	// mode is the code to pass to poolControl for this device to set the correct heater mode
	log.debug("HEATER ${device} to ${modeID}")
	getParent().heaterSetMode(device, modeID)
}


def setTemperature(t) {
	log.debug(device.label + " current temp set to ${t}") 
    sendEvent(name: 'temperature', value: t, unit:"F")    
    log.debug(device.label + " DONE current temp set to ${t}") 
}

// Get stored temperature from currentState in current local scale
def getTempInLocalScale(state) {
	def temp = device.currentState(state)
	if (temp && temp.value && temp.unit) {
		return getTempInLocalScale(temp.value.toBigDecimal(), temp.unit)
	}
	return 0
}

// get/convert temperature to current local scale
def getTempInLocalScale(temp, scale) {
	if (temp && scale) {
		def scaledTemp = convertTemperatureIfNeeded(temp.toBigDecimal(), scale).toDouble()
		return (getTemperatureScale() == "F" ? scaledTemp.round(0).toInteger() : roundC(scaledTemp))
	}
	return 0
}

def getTempInDeviceScale(state) {
	def temp = device.currentState(state)
	if (temp && temp.value && temp.unit) {
		return getTempInDeviceScale(temp.value.toBigDecimal(), temp.unit)
	}
	return 0
}

def getTempInDeviceScale(temp, scale) {
	if (temp && scale) {
		def deviceScale = (state.scale == 1) ? "F" : "C"
		return (deviceScale == scale) ? temp :
				(deviceScale == "F" ? celsiusToFahrenheit(temp).toDouble().round(0).toInteger() : roundC(fahrenheitToCelsius(temp)))
	}
	return 0
}

def roundC (tempC) {
	return (Math.round(tempC.toDouble() * 2))/2
}

/**
 *  logger()
 *
 *  Wrapper function for all logging.
 **/

private logger(msg, level = "debug") {

    switch(level) {
        case "error":
            if (state.loggingLevelIDE >= 1) log.error msg
            break

        case "warn":
            if (state.loggingLevelIDE >= 2) log.warn msg
            break

        case "info":
            if (state.loggingLevelIDE >= 3) log.info msg
            break

        case "debug":
            if (state.loggingLevelIDE >= 4) log.debug msg
            break

        case "trace":
            if (state.loggingLevelIDE >= 5) log.trace msg
            break

        default:
            log.debug msg
            break
    }
}