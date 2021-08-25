/**
 *  Hive Hot Water
 *
 *  Copyright 2017 Alex Lee Yuk Cheung
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
 *	VERSION HISTORY
 *    25.02.2016
 *    v2.0 BETA - Initial Release
 *	  v2.0b - Fix blank temperature readings on Android ST app
 *
 *	  10.03.2017
 *	  v2.0c - Fix to boost mode.
 *
 *	  30.05.2017
 *	  v2.1 - Updated to use Hive Beekeeper API.
 *	  v2.1b - Bug fix. Refresh bug prevents installation of Hive devices.
 *
 *	  30.10.2017
 *	  v3.0 - Version refactor to reflect BeeKeeper API update.
 *
 *	  08.10.2018
 *	  v3.1 - First attempt at New Smartthings App compatibility.
 *
 *	  08.12.2020
 *	  v3.1 - New Smartthings App UI.
 *	  v3.1a - Add missing set boost length command for WebCore.
 *	  v3.1b - Tweak to boost length command.
 */

metadata {
	definition (name: "Hive Hot Water", namespace: "alyc100", author: "Alex Lee Yuk Cheung", ocfDeviceType: "oic.d.thermostat", mnmn: "fBZA", vid: "e1ac13ba-6325-3cd8-8908-46b6ab04a313") {
		capability "Actuator"
		capability "Polling"
		capability "Refresh"
        capability "Thermostat"
		capability "Thermostat Mode"
		capability "Thermostat Operating State"
        capability "Health Check"
        capability "tigerdrum36561.boostLabel"
        capability "tigerdrum36561.boostLength"
        
        command "setThermostatMode"
        command "setBoostLength"
	}

	simulator {
		// TODO: define status and reply messages here
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'switch' attribute
	// TODO: handle 'thermostatMode' attribute

}

def installed() {
	log.debug "Executing 'installed'"
    state.boostLength = 60
    sendEvent(name: "checkInterval", value: 10 * 60 + 2 * 60, data: [protocol: "cloud"], displayed: false)
}

void updated() {
	log.debug "Executing 'updated'"
    sendEvent(name: "checkInterval", value: 10 * 60 + 2 * 60, data: [protocol: "cloud"], displayed: false)
}

// handle commands
def setHeatingSetpoint(temp) {
	//Not implemented	
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
    sendEvent("name":"boostLength", "value": minutes, "units":"minutes", displayed: true)
    
    def latestThermostatMode = device.latestState('thermostatMode')
}

def heatingSetpointUp(){
	//Not implemented
}

def heatingSetpointDown(){
	//Not implemented
}

def on() {
	log.debug "Executing 'on'"
	setThermostatMode('heat')
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
	mode = mode == 'cool' ? 'heat' : mode
    def args = [
        		mode: "SCHEDULE"
            ]
    if (mode == 'off') {
     	args = [
        		mode: "OFF"
            ]
    } else if (mode == 'heat') {
    	//{"nodes":[{"attributes":{"activeHeatCoolMode":{"targetValue":"HEAT"},"activeScheduleLock":{"targetValue":false}}}]}
    	args = [
        		mode: "MANUAL"
            ]
    } else if (mode == 'emergency heat') {
    	if (state.boostLength == null || state.boostLength == '')
        {
        	state.boostLength = 60
            sendEvent("name":"boostLength", "value": 60, "units":"minutes", displayed: true)
        }
    	args = [
            mode: "BOOST",
            boost: state.boostLength
        ]
    }
    
    def resp = parent.apiPOST("/nodes/hotwater/${device.deviceNetworkId}", args)
	if (resp.status != 200) {
		log.error("Unexpected result in poll(): [${resp.status}] ${resp.data}")
		return []
	}
    else {
		mode = mode == 'range' ? 'auto' : mode
        runIn(3, refresh)
	}
}

def poll() {
	log.debug "Executing 'poll'"
	def currentDevice = parent.getDeviceStatus(device.deviceNetworkId)
	if (currentDevice == []) {
		return []
	}
    log.debug "$device.name status: $currentDevice"
    
        //Construct status message
        def statusMsg = "Currently"
        
        //Boost button label
    	def boostLabel = "OFF"
        
        // determine hive hot water operating mode
        def mode = currentDevice.state.mode.toLowerCase()
        
        if (mode == "off") {
            statusMsg = statusMsg + " set to OFF"
        }
        else if (mode == "boost") {
        	mode = 'emergency heat'
            statusMsg = statusMsg + " set to BOOST"
            def boostTime = currentDevice.state.boost
            boostLabel = boostTime + " min remaining"
            sendEvent("name":"boostTimeRemaining", "value": boostTime + " mins")
        }
        else if (mode == "manual") {
        	mode = 'heat'
            statusMsg = statusMsg + " set to ON"
        }
        else {
        	mode = 'auto'
        	statusMsg = statusMsg + " set to SCHEDULE"
        }
        
        sendEvent(name: 'thermostatMode', value: mode) 
        
        // determine if Hive hot water relay is on
        def stateHotWaterRelay = currentDevice.state.status
        
        log.debug "stateHotWaterRelay: $stateHotWaterRelay"
        
        if (stateHotWaterRelay == "ON") {
        	sendEvent(name: 'temperature', value: 99, unit: "C", state: "heat", displayed: false)
        	sendEvent(name: 'heatingSetpoint', value: 99, unit: "C", state: "heat", displayed: false)
        	sendEvent(name: 'coolingSetpoint', value: 99, unit: "C", state: "heat", displayed: false)
            sendEvent(name: 'thermostatOperatingState', value: "heating")
            statusMsg = statusMsg + " and is HEATING"
        }       
        else {
        	sendEvent(name: 'temperature', value: 0, unit: "C", state: "heat", displayed: false)
       	 	sendEvent(name: 'heatingSetpoint', value: 0, unit: "C", state: "heat", displayed: false)
        	sendEvent(name: 'coolingSetpoint', value: 0, unit: "C", state: "heat", displayed: false)
            sendEvent(name: 'thermostatOperatingState', value: "idle")
            statusMsg = statusMsg + " and is IDLE"
        }
        sendEvent("name":"hiveHotWater", "value":statusMsg, displayed: false)
        sendEvent("name":"boostLabel", "value": boostLabel, displayed: false)
    
}

def refresh() {
	log.debug "Executing 'refresh'"
	poll()
}