/**
 *  Hive TRV
 *
 *  Initial Copyright 2015 Alex Lee Yuk Cheung (Hive Thermostat DH)
 * 	Modified for use wiht Hive TRV - Ben Lee @Bibbleq
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
 *  VERSION HISTORY
 *
 *	31.10.2019
 *	v1 based on Hive Heating DH code with modification for TRVs
 *	
 *	06.09.2019
 * 	v1.1 display calibration information
 *
 *	13.09.2019
 *	v1.2 Updated to make more efficient (parent caches product information for 2 minutes)
 *
 *	18.09.2019
 *	v1.3 Added refresh capabilities into DH instead of relying on parent SA (causes timeouts if too many TRVs)
 *
 *	8.11.2019
 *	v1.3.1 fixed typo
 *
 *  8.12.2020
 *	v1.4 Update UI for new SmartThings app
 *	v1.4a - Add missing set boost length command for WebCore.
 */
 
preferences 
{
	input( "boostInterval", "number", title: "Boost Interval (minutes)", description: "Boost interval amount in minutes", required: false, defaultValue: 60 )
    input( "boostTemp", "decimal", title: "Boost Temperature (°C)", description: "Boost interval amount in Centigrade", required: false, defaultValue: 22, range: "5..32" )
    input( "maxTempThreshold", "decimal", title: "Max Temperature Threshold (°C)", description: "Set the maximum temperature threshold in Centigrade", required: false, defaultValue: 32, range: "5..32" )
	input( "disableDevice", "bool", title: "Disable Hive TRV?", required: false, defaultValue: false )
}

metadata {
	definition (name: "Hive TRV", namespace: "alyc100", author: "Alex Lee Yuk Cheung", ocfDeviceType: "oic.d.thermostat", mnmn: "fBZA", vid: "de5c3738-3ae6-38b7-adfd-bbd98cba41d1") {
		capability "Actuator"
		capability "Polling"
		capability "Refresh"
		capability "Temperature Measurement"
        capability "Thermostat"
		capability "Thermostat Heating Setpoint"
		capability "Thermostat Mode"
		capability "Thermostat Operating State"
        capability "Health Check"
		capability "Battery"
        capability "tigerdrum36561.boostLabel"
        capability "tigerdrum36561.boostLength"
        
        command "heatingSetpointUp"
		command "heatingSetpointDown"
        command "boostTimeUp"
		command "boostTimeDown"
        command "setThermostatMode"
        command "setHeatingSetpoint"
        command "setTemperatureForSlider"
        command "setBoostLength"
        command "boostButton"
	}

	simulator {
		// TODO: define status and reply messages here
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'temperature' attribute
	// TODO: handle 'heatingSetpoint' attribute
	// TODO: handle 'thermostatSetpoint' attribute
	// TODO: handle 'thermostatMode' attribute
	// TODO: handle 'thermostatOperatingState' attribute
}

def installed() {
	log.debug "Executing 'installed'"
    state.boostLength = 60
    state.desiredHeatSetpoint = 7
    sendEvent(name: "checkInterval", value: 10 * 60 + 2 * 60, data: [protocol: "cloud"], displayed: false)
}

void updated() {
	log.debug "Executing 'updated'"
    sendEvent(name: "checkInterval", value: 10 * 60 + 2 * 60, data: [protocol: "cloud"], displayed: false)
}

// handle commands
def setHeatingSetpoint(temp) {
	log.debug "Executing 'setHeatingSetpoint with temp $temp'"
	def latestThermostatMode = device.latestState('thermostatMode')
    
    if (temp < 5) {
		temp = 5
	}
	if (temp > 32) {
		temp = 32
	}
         
    if (settings.disableDevice == null || settings.disableDevice == false) {
    	//if thermostat is off, set to manual 
        def args
   		if (latestThermostatMode.stringValue == 'off') {
    		args = [
        		mode: "MANUAL", 
                target: temp
            ]
		
    	} 
    	else {
    	// {"target":7.5}
    		args = [
        		target: temp
        	]               
    	}
    	def resp = parent.apiPOST("/nodes/trvcontrol/${device.deviceNetworkId}", args)    	
    }
    runIn(4, refresh)
}

def setBoostLength(minutes) {
	log.debug "Executing 'setBoostLength with length $minutes minutes'"
	//modified minimum boost to be 15 minutes as TRV can take 15 minutes to pick up new set points
    if (minutes < 5) {
		minutes = 5
	}
	if (minutes > 300) {
		minutes = 300
	}
    sendEvent("name":"boostLength", "value": minutes, "unit": "minutes", displayed: true)
}

def getBoostIntervalValue() {
	if (settings.boostInterval == null) {
    	return 10
    } 
    return settings.boostInterval.toInteger()
}

def getBoostTempValue() {
	if (settings.boostTemp == null) {
    	return "22"
    } 
    return settings.boostTemp
}

def getMaxTempThreshold() {
	if (settings.maxTempThreshold == null) {
    	return "32"
    } 
    return settings.maxTempThreshold
}

def boostTimeUp() {
	log.debug "Executing 'boostTimeUp'"
    //Round down result
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

def setHeatingSetpointToDesired() {
	setHeatingSetpoint(state.newSetpoint)
}

def setNewSetPointValue(newSetPointValue) {
	log.debug "Executing 'setNewSetPointValue' with value $newSetPointValue"
	unschedule('setHeatingSetpointToDesired')
    state.newSetpoint = newSetPointValue
    state.desiredHeatSetpoint = state.newSetpoint
	sendEvent("name":"desiredHeatSetpoint", "value": state.desiredHeatSetpoint, displayed: false)
	log.debug "Setting heat set point up to: ${state.newSetpoint}"
    runIn(3, setHeatingSetpointToDesired)
}

def heatingSetpointUp(){
	log.debug "Executing 'heatingSetpointUp'"
	setNewSetPointValue(getHeatTemp().toInteger() + 1)
}

def heatingSetpointDown(){
	log.debug "Executing 'heatingSetpointDown'"
	setNewSetPointValue(getHeatTemp().toInteger() - 1)
}

def setTemperatureForSlider(value) {
	log.debug "Executing 'setTemperatureForSlider with $value'"
	setNewSetPointValue(value)  
}

def getHeatTemp() { 
	return state.desiredHeatSetpoint == null ? device.currentValue("heatingSetpoint") : state.desiredHeatSetpoint
}

def off() {
	setThermostatMode('off')
}

def heat() {
	setThermostatMode('heat')
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

def auto() {
	setThermostatMode('auto')
}

def setThermostatMode(mode) {
	if (settings.disableDevice == null || settings.disableDevice == false) {
		mode = mode == 'cool' ? 'heat' : mode
		log.debug "Executing 'setThermostatMode with mode $mode'"
    	def args = [
        		mode: "SCHEDULE"
            ]
    	if (mode == 'off') {
     		args = [
        		mode: "OFF"
            ]
    	} else if (mode == 'heat') {
        	//mode": "MANUAL", "target": 20
    		args = [ 
                target: 20
            ]
    	} else if (mode == 'emergency heat') {  
    		if (state.boostLength == null || state.boostLength == '')
        	{
        		state.boostLength = 60
            	sendEvent("name":"boostLength", "value": 60, "unit": "minutes",  displayed: true)
        	}
    		//"mode": "BOOST","boost": 60,"target": 22
			args = [
            	mode: "BOOST",
                boost: state.boostLength,
                target: getBoostTempValue()
        	]
   		}
    
    	def resp = parent.apiPOST("/nodes/trvcontrol/${device.deviceNetworkId}", args)
		mode = mode == 'range' ? 'auto' : mode    	
    }
    runIn(4, refresh)
}

def setDeviceID() {
	def DeviceID = parent.getDeviceID(device.deviceNetworkId)
    state.DeviceID = DeviceID
}

def poll() {
	log.debug "Executing 'poll' for $device.deviceNetworkId"
	def currentDevice = parent.getDeviceTRVStatus(device.deviceNetworkId)
	if (currentDevice == []) {
		return []
	}
    log.debug "${device.name} status: ${currentDevice}"
	
    if (state.DeviceID == null) {
    	log.debug "Device ID Null"
        setDeviceID()
    }
    
    //Get Device info (Product & Device API requests return differnt sets of info)
    def currentDeviceDetails = parent.getDeviceInfo(state.DeviceID)
	//log.debug "${device.name} details: ${currentDeviceDetails}"
	
	//update battery
	sendEvent("name": "battery", "value": currentDeviceDetails.props.battery, displayed: true)
	//log.debug "Battery: ${currentDeviceDetails.props.battery}"
	
	//update signal
	sendEvent("name": "signal", "value": currentDeviceDetails.props.signal, displayed: true)
	//log.debug "Signal: ${currentDeviceDetails.props.signal}"
    
    //update calibration status
    sendEvent("name": "calibrationstatus", "value": currentDeviceDetails.state.calibrationStatus, displayed: true)
	//log.debug "Calibrationstatus: ${currentDeviceDetails.state.calibrationStatus}"

    //update calibration timestamp
    sendEvent("name": "calibrationtimestamp", "value": currentDeviceDetails.props.calibration, displayed: true)
	//log.debug "Calibration Time: ${currentDeviceDetails.props.calibration}"
    
	//Construct status message
	def statusMsg = ""
	
	//Boost button label
	if (state.boostLength == null || state.boostLength == '')
        {
        	state.boostLength = 60
            sendEvent("name":"boostLength", "value": 60, "unit": "minutes", displayed: true)
        } else {
        	sendEvent("name":"boostLength", "value": state.boostLength, "unit": "minutes", displayed: true)
        }
	def boostLabel = "OFF"
	
	// get temperature status
	def temperature = currentDevice.props.temperature
	def heatingSetpoint = currentDevice.state.target as Double
	
	//Check heating set point against maximum threshold value.
	log.debug "Maximum temperature threshold set to: " + getMaxTempThreshold()
	if ((getMaxTempThreshold() as BigDecimal) < (heatingSetpoint as BigDecimal)) {
		log.debug "Maximum temperature threshold exceeded. " + heatingSetpoint + " is higher than " + getMaxTempThreshold()
		sendEvent(name: 'maxtempthresholdbreach', value: heatingSetpoint, unit: "C", displayed: false)
		//Force temperature threshold to Hive API.
		def args = [
			target: getMaxTempThreshold()
		]
		parent.apiPOST("/nodes/trvcontrol/${device.deviceNetworkId}", args)   
		heatingSetpoint = String.format("%2.1f", getMaxTempThreshold())           
	}
	
	// convert temperature reading of 1 degree to 7 as Hive app does
	if (heatingSetpoint == "1.0") {
		heatingSetpoint = "7.0"
	}
	sendEvent(name: 'temperature', value: temperature, unit: "C", state: "heat")
	sendEvent(name: 'heatingSetpoint', value: heatingSetpoint, unit: "C", state: "heat")
	//sendEvent(name: 'coolingSetpoint', value: heatingSetpoint, unit: "C", state: "heat")
	sendEvent(name: 'thermostatSetpoint', value: heatingSetpoint, unit: "C", state: "heat", displayed: false)
	//sendEvent(name: 'thermostatFanMode', value: "off", displayed: false)
	
	state.desiredHeatSetpoint = heatingSetpoint
	sendEvent("name":"desiredHeatSetpoint", "value": state.desiredHeatSetpoint, unit: "C", displayed: false)
	
	// determine hive operating mode
	def mode = currentDevice.state.mode.toLowerCase()
	
	//If Hive heating device is set to disabled, then force off if not already off.
	if (settings.disableDevice != null && settings.disableDevice == true && mode != "off") {
		def args = [
			mode: "OFF"
		]
		parent.apiPOST("/nodes/trvcontrol/${device.deviceNetworkId}", args)
		mode = 'off'
	}
	
    switch (mode) {
        case "boost":
            mode = 'emergency heat'          
            def boostTime = currentDevice.state.boost
            statusMsg = "Boost " + boostTime + "min"
            boostLabel = boostTime + "min remaining"
            sendEvent("name":"boostTimeRemaining", "value": boostTime + " mins")
        case "manual":
            mode = 'heat'
            statusMsg = statusMsg + " Manual"
        case "auto":
            statusMsg = statusMsg + " Schedule"
        default:
        	log.debug "default"
    }
	
	if (settings.disableDevice != null && settings.disableDevice == true) {
		statusMsg = "DISABLED"
	}
	
	sendEvent(name: 'thermostatMode', value: mode) 
	
	// determine if Hive heating relay is on
	def stateHeatingRelay = (heatingSetpoint as BigDecimal) > (temperature as BigDecimal)
	
	//log.debug "stateHeatingRelay: $stateHeatingRelay"
	//log.debug "Working status: ${currentDevice.props.working}"
	
	if (stateHeatingRelay && currentDevice.props.working.toBoolean() == true) {
		//log.debug "heating"
		sendEvent(name: 'thermostatOperatingState', value: "heating")
	}       
	else {
		//log.debug "idle"
		sendEvent(name: 'thermostatOperatingState', value: "idle")
	}  

	sendEvent("name":"hiveHeating", "value": statusMsg, displayed: false)  
	sendEvent("name":"boostLabel", "value": boostLabel, displayed: false)

}

def refresh() {
	log.debug "Executing 'refresh'"
	unschedule('poll')
	runEvery5Minutes('poll')
}