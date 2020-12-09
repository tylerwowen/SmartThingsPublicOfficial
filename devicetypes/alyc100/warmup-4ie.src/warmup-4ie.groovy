/**
 *  Warmup 4ie
 *
 *  Copyright 2015,2016,2017,2018 Alex Lee Yuk Cheung
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *	VERSION HISTORY
 *	09.12.2020 	v1.0 - New SmartThings App UI.
 *	08.10.2018 	v1.0 BETA Release 5 - Compatibility for New Smartthings App.
 *	10.12.2017 	v1.0 BETA Release 4 - Fix to boost functionality when thermostat is off.
 *	10.12.2017 	v1.0 BETA Release 3 - Add boost functionality.
 *	05.01.2017	v1.0 BETA Release 2 - Minor fix that prevented 'Manual' mode being selected and activated.
 *  14.12.2016	v1.0 BETA - Initial Release
 */
import groovy.time.TimeCategory 

preferences 
{
	input( "boostInterval", "number", title: "Boost Interval (minutes)", description: "Boost interval amount in minutes", required: false, defaultValue: 10 )
    input( "boostTemp", "decimal", title: "Boost Temperature (°C)", description: "Boost interval amount in Centigrade", required: false, defaultValue: 22, range: "5..32" )
    input( "disableDevice", "bool", title: "Disable Warmup Heating Device?", required: false, defaultValue: false )
}

metadata {
	definition (name: "Warmup 4IE", namespace: "alyc100", author: "Alex Lee Yuk Cheung", ocfDeviceType: "oic.d.thermostat", mnmn: "fBZA", vid: "36a9d325-53b2-37e8-9376-ee404ac3259d") {
		capability "Actuator"
		capability "Polling"
		capability "Refresh"		
        capability "Temperature Measurement"
        capability "Thermostat"
		capability "Thermostat Heating Setpoint"
		capability "Thermostat Mode"
		capability "Thermostat Operating State"
        capability "Health Check"
        capability "tigerdrum36561.boostLabel"
        capability "tigerdrum36561.boostLength"
        capability "tigerdrum36561.airTemperatureMeasurement"
        
        command "heatingSetpointUp"
		command "heatingSetpointDown"
        command "setThermostatMode"
        command "setHeatingSetpoint"
        command "setTemperatureForSlider"
        command "setTemperature"
        command "boostButton"
        command "boostTimeUp"
		command "boostTimeDown"
		command "setBoostLength"
	}

	simulator {
		// TODO: define status and reply messages here
	}
}

def installed() {
	log.debug "Executing 'installed'"
    state.desiredHeatSetpoint = 7
	// execute handlerMethod every 10 minutes.
    runEvery10Minutes(poll)
    sendEvent(name: "checkInterval", value: 20 * 60 + 2 * 60, data: [protocol: "cloud"], displayed: false)
}

def updated() {
	log.debug "Executing 'updated'"
	// execute handlerMethod every 10 minutes.
    unschedule()
    runEvery10Minutes(poll)
    sendEvent(name: "checkInterval", value: 20 * 60 + 2 * 60, data: [protocol: "cloud"], displayed: false)
}

def uninstalled() {
	log.debug "Executing 'uninstalled'"
	unschedule()
}

// parse events into attributes
def parse(String description) {
}

// handle commands

def off() {
	setThermostatMode('off')
}

def on() {
	setThermostatMode('auto')
	
}

def heat() {
	setThermostatMode('heat')
}

def auto() {
	setThermostatMode('auto')
}

def setHeatingSetpoint(temp) {
	log.debug "Executing 'setHeatingSetpoint with temp $temp'"
	def latestThermostatMode = device.latestState('thermostatMode')
    
    if (temp < 5) {
		temp = 5
	}
	if (temp > 32) {
		temp = 32
	}
    def args
    if (settings.disableDevice == null || settings.disableDevice == false) {
    	//if thermostat is off, set to manual 
   		if (latestThermostatMode.stringValue == 'off') {
    		args = [
        		method: "setProgramme", roomId: device.deviceNetworkId, roomMode: "fixed"
        	]
			parent.apiPOSTByChild(args)
    	}
        args = [
        	method: "setProgramme", roomId: device.deviceNetworkId, roomMode: "fixed", fixed: [fixedTemp: "${(temp * 10) as Integer}"]
        ]
        parent.apiPOSTByChild(args)  
    	
    }
    runIn(3, refresh)
}

def setHeatingSetpointToDesired() {
	setHeatingSetpoint(state.newSetpoint)
}

def setNewSetPointValue(newSetPointValue) {
	log.debug "Executing 'setNewSetPointValue' with value $newSetPointValue"
    state.newSetpoint = newSetPointValue
    state.desiredHeatSetpoint = state.newSetpoint
	sendEvent("name":"desiredHeatSetpoint", "value": state.desiredHeatSetpoint, displayed: false)
	log.debug "Setting heat set point up to: ${state.newSetpoint}"
    setHeatingSetpointToDesired()
}

def heatingSetpointUp(){
	log.debug "Executing 'heatingSetpointUp'"
	setNewSetPointValue(getHeatTemp().toInteger() + 1)
}

def heatingSetpointDown(){
	log.debug "Executing 'heatingSetpointDown'"
	setNewSetPointValue(getHeatTemp().toInteger() - 1)
}

def setTemperature(value) {
	log.debug "Executing 'setTemperature with $value'"
    def currentTemp = device.currentState("temperature").doubleValue
	(value < currentTemp) ? (setNewSetPointValue(getHeatTemp().toInteger() - 1)) : (setNewSetPointValue(getHeatTemp().toInteger() + 1))
}

def setTemperatureForSlider(value) {
	log.debug "Executing 'setTemperatureForSlider with $value'"
	setNewSetPointValue(value)  
}

def getHeatTemp() { 
	return state.desiredHeatSetpoint == null ? device.currentValue("heatingSetpoint") : state.desiredHeatSetpoint
}

def emergencyHeat() {
	log.debug "Executing 'boost'"
	
    def latestThermostatMode = device.latestState('thermostatMode')
    
    //Don't do if already in BOOST mode.
	if (latestThermostatMode.stringValue != 'emergency heat') {
		setThermostatMode('emergency heat')
    }
    else {
    	log.debug "Already in boost mode."
    }
}


def setThermostatMode(mode) {
	if (settings.disableDevice == null || settings.disableDevice == false) {
		mode = mode == 'cool' ? 'heat' : mode
		log.debug "Executing 'setThermostatMode with mode $mode'"
    	def args
    	if (mode == 'off') {
        	//Sets whole location to off instead of individual thermostat. Awaiting Warmup API update.
        	
            /*args = [
        		method: "setRunModeByRoomIdArray", roomIdArray: [device.deviceNetworkId as Integer], values: [runMode: "frost"]
        	]
        	parent.apiPOSTByChild(args)
        	*/
     		parent.setLocationToFrost()
    	} else if (mode == 'heat') {
    		args = [
        		method: "setProgramme", roomId: device.deviceNetworkId, roomMode: "fixed"
        	]	
            parent.apiPOSTByChild(args)
    	} else if (mode == 'emergency heat') {  
    		if (state.boostLength == null || state.boostLength == '')
        	{
        		state.boostLength = 60
            	sendEvent("name":"boostLength", "value": 60, displayed: true)
        	}
            args = [
        		method: "setOverride", rooms: ["$device.deviceNetworkId"], type: 3, temp: getBoostTempValue(), until: getBoostEndTime()
        	]
            parent.apiPOSTByChild(args)
   		} else {
        	args = [
        		method: "setProgramme", roomId: device.deviceNetworkId, roomMode: "schedule"
        	]
            parent.apiPOSTByChild(args)
        }
		mode = mode == 'range' ? 'auto' : mode    	
    }
    runIn(3, refresh)
}

def setBoostLength(minutes) {
	log.debug "Executing 'setBoostLength with length $minutes minutes'"
    if (minutes < 5) {
		minutes = 5
	}
	if (minutes > 300) {
		minutes = 300
	}
    state.boostLength = minutes
    sendEvent("name":"boostLength", "value": state.boostLength, "unit":"minutes", displayed: true)
}

def getBoostIntervalValue() {
	if (settings.boostInterval == null) {
    	return 10
    } 
    return settings.boostInterval.toInteger()
}

def getBoostTempValue() {
	log.debug "Executing 'getBoostTempValue'"
	def retVal 
	if (settings.boostTemp == null) {
    	retVal = "220"
    } 
    retVal = ((settings.boostTemp as Integer) * 10) as String
    return retVal
}

def getBoostEndTime() {
	log.debug "Executing 'getBoostEndTime'"
	use( TimeCategory ) {
    	def endDate = new Date() + state.boostLength.minutes
		return endDate.format("HH:mm")
	}
}

def boostTimeUp() {
	log.debug "Executing 'boostTimeUp'"
    //Round down results
    int boostIntervalValue = getBoostIntervalValue()
    def newBoostLength = (state.boostLength + boostIntervalValue) - (state.boostLength % boostIntervalValue)
	setBoostLength(newBoostLength)
}

def boostTimeDown() {
	log.debug "Executing 'boostTimeDown'"
    //Round down result
    int boostIntervalValue = getBoostIntervalValue()
    def newBoostLength = (state.boostLength - boostIntervalValue) - (state.boostLength % boostIntervalValue)
	setBoostLength(newBoostLength)
}

def boostButton() {
	log.debug "Executing 'boostButton'"
	setThermostatMode('emergency heat')
}

def poll() {
    log.debug "Executing 'poll'"
	def room = parent.getStatus(device.deviceNetworkId)
	if (room == []) {
		log.error("Unexpected result in parent.getStatus()")
		return []
	}
    log.debug room
    def modeMsg = ""
    def airTempMsg = ""
    def mode = room.runMode[0]
    if (mode == "fixed") mode = "heat"
    else if (mode == "off" || mode == "frost") mode = "off"
    else if (mode == "prog" || mode == "schedule") mode = "auto"
    else if (mode == "override") mode = "emergency heat"
    sendEvent(name: 'thermostatMode', value: mode) 
    modeMsg = "Mode: " + mode.toUpperCase() + "."
    
 	//Boost button label
    def boostLabel = "OFF"
    
    //If Warmup heating device is set to disabled, then force off if not already off.
    if (settings.disableDevice != null && settings.disableDevice == true && activeHeatCoolMode != "OFF") {
    	//Sets whole location to off instead of individual thermostat. Awaiting Warmup API update.
        /*args = [
        		method: "setRunModeByRoomIdArray", roomIdArray: [device.deviceNetworkId as Integer], values: [runMode: "frost"]
        	]
        parent.apiPOSTByChild(args)
        */
        parent.setLocationToFrost()
    	mode = 'off'
    } else if (mode == "emergency heat") {       
        def boostTime = room.overrideDur
        boostLabel = boostTime + "min remaining"
        sendEvent("name":"boostTimeRemaining", "value": boostTime + " mins")
    }
    
    if (settings.disableDevice != null && settings.disableDevice == true) {
    	modeMsg = "DISABLED"
    }
    
    def temperature = String.format("%2.1f",(room.currentTemp[0] as BigDecimal)/ 10)
    sendEvent(name: 'temperature', value: temperature, unit: "C", state: "heat")
    
    def heatingSetpoint = String.format("%2.1f",(room.targetTemp[0] as BigDecimal) / 10)
    sendEvent(name: 'heatingSetpoint', value: heatingSetpoint, unit: "C", state: "heat")
    sendEvent(name: 'coolingSetpoint', value: heatingSetpoint, unit: "C", state: "heat")
    sendEvent(name: 'thermostatSetpoint', value: heatingSetpoint, unit: "C", state: "heat", displayed: false)
    
    if ((room.targetTemp[0] as BigDecimal) > (room.currentTemp[0] as BigDecimal)) {
        	sendEvent(name: 'thermostatOperatingState', value: "heating")
        }       
        else {
        	sendEvent(name: 'thermostatOperatingState', value: "idle")
        } 
    
    sendEvent(name: 'thermostatFanMode', value: "off", displayed: false)
    
    def airTemperature = String.format("%2.1f",(room.airTemp[0] as BigDecimal) / 10)
    sendEvent("name": "airTemperature", "value": airTemperature, unit: "C")
    sendEvent("name":"statusMsg", "value": modeMsg + " " + airTempMsg, displayed: false)
    
    state.desiredHeatSetpoint = (int) Double.parseDouble(heatingSetpoint)
    sendEvent("name":"desiredHeatSetpoint", "value": state.desiredHeatSetpoint, unit: "C", displayed: false)   
    
    sendEvent("name":"boostLabel", "value": boostLabel, displayed: false)
}

def refresh() {
	log.debug "Executing 'refresh'"
	poll()
}