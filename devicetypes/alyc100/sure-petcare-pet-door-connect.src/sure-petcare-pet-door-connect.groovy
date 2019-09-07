/**
 *  Sure PetCare Pet Door Connect
 *
 *  Copyright 2019 Alex Lee Yuk Cheung
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
 *  VERSION HISTORY
 *  06.09.2019 - v1.0 - Initial Version
 */
 

metadata {
	definition (name: "Sure PetCare Pet Door Connect", namespace: "alyc100", author: "Alex Lee Yuk Cheung") {
		capability "Polling"
		capability "Refresh"
        capability "Actuator"
        capability "Health Check"
        capability "Battery"
		capability "Lock"
		capability "Sensor"
        
        attribute "network","string"
        
        command "toggleLockMode"
        command "setLockMode", ["string"]
	}

	tiles(scale: 2) {
    	multiAttributeTile(name: "flap", width: 6, height: 4, type:"generic") {
			tileAttribute("device.lockMode", key:"PRIMARY_CONTROL", canChangeBackground: true){
				attributeState("both", label: 'LOCKED', action: "toggleLockMode", icon: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/devicetypes/alyc100/surepetcare-flap-lock.png", backgroundColor: "#ef6d6a", nextState:"waiting")
				attributeState("in", label: 'PETS OUT', action: "toggleLockMode", icon: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/devicetypes/alyc100/surepetcare-flap-out.png", backgroundColor: "#f88e4c", nextState:"waiting")
                attributeState("out", label: 'PETS IN', action: "toggleLockMode", icon: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/devicetypes/alyc100/surepetcare-flap-in.png", backgroundColor: "#81cb65", nextState:"waiting")
				attributeState("none", label: 'UNLOCKED', action: "toggleLockMode", icon: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/devicetypes/alyc100/surepetcare-flap-unlock.png", backgroundColor: "#33a1ff", nextState:"waiting")
            	attributeState("waiting", label:'Please Wait...', backgroundColor:"#ffffff")
            }
		}
        valueTile("serial_number", "device.serial_number", decoration: "flat", width: 3, height: 1) {
			state "default", label: 'Serial Number:\n${currentValue}'
		}
        valueTile("mac_address", "device.mac_address", decoration: "flat", width: 3, height: 1) {
			state "default", label: 'MAC Address:\n${currentValue}'
		}
        valueTile("created_at", "device.created_at", decoration: "flat", width: 3, height: 1) {
			state "default", label: 'Created at:\n${currentValue}'
		}
        valueTile("updated_at", "device.updated_at", decoration: "flat", width: 3, height: 1) {
			state "default", label: 'Updated at:\n${currentValue}'
		}
        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
			state "battery", label:'${currentValue}%\nbattery', unit:""
		}
        valueTile("device_rssi", "device.device_rssi", canChangeBackground: true, width: 2, height: 2){
			state "default", label: 'RSSI ${currentValue}',
            backgroundColors:[
				[value: -50, color: "#33cc33"],
                [value: -60, color: "#ffff99"],
                [value: -70, color: "#ff9933"],
                [value: -100, color: "#ff0000"]
			]
		}
        standardTile("network", "device.network", width: 2, height: 2, inactiveLabel: false, canChangeIcon: false) {
			state ("default", label:'unknown', icon: "st.unknown.unknown.unknown")
			state ("Connected", label:'Online', icon: "st.Health & Wellness.health9", backgroundColor: "#79b821")
			state ("Pending", label:'Pending', icon: "st.Health & Wellness.health9", backgroundColor: "#ffa500")
			state ("Not Connected", label:'Offline', icon: "st.Health & Wellness.health9", backgroundColor: "#bc2323")
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
		main (["flap"])
		details(["flap", "device_rssi", "battery", "serial_number", "mac_address", "created_at", "updated_at", "network", "refresh"])
	}
}

// handle commands
def installed() {
    sendEvent(name: "checkInterval", value: 1 * 60 * 60, data: [protocol: "cloud"], displayed: false)
}

def updated() {
    sendEvent(name: "checkInterval", value: 1 * 60 * 60, data: [protocol: "cloud"], displayed: false)
}

def poll() {
	log.debug "Executing 'poll'"
	
    def resp = parent.apiGET("/api/me/start")
	if (resp.status != 200) {
    	sendEvent(name: 'network', value: "Not Connected" as String)
		log.error("Unexpected result in poll(): [${resp.status}] ${resp.data}")
		return []
	}
    
    def response = resp.data.data.devices
    def flap = response.find{device.deviceNetworkId.toInteger() == it.id}
    sendEvent(name: "serial_number", value: flap.serial_number)
    sendEvent(name: "mac_address", value: flap.mac_address)
    sendEvent(name: "created_at", value: flap.created_at)
    sendEvent(name: "updated_at", value: flap.updated_at)
    def lockMode
    switch (flap.status.locking.mode) {
    	case 0:
            lockMode = "none"
       		break;
        case 1:
        	lockMode = "out"
            break;
        case 2:
        	lockMode = "in"
            break;
        default:
        	lockMode = "both"
			break;
        }
    if (lockMode == "none") {
    	 sendEvent(name: "lock", value: "unlocked", displayed: true)
    } else {
    	 sendEvent(name: "lock", value: "locked", displayed: true)
    }
    sendEvent(name: "lockMode", value: lockMode)
    if (flap.status.online) {
    	sendEvent(name: 'network', value: "Connected" as String)
    } else {
    	sendEvent(name: 'network', value: "Pending" as String)
    }
    def batteryPercent = getBatteryPercent(flap.status.battery)
    sendEvent(name: 'battery', value: batteryPercent)
    sendEvent(name: 'device_rssi', value: flap.status.signal.device_rssi)
    sendEvent(name: 'hub_rssi', value: flap.status.signal.hub_rssi)
}

def toggleLockMode() {
	log.debug "Executing 'toggleLockMode'"
    def lockMode
	if (device.currentState("lockMode").getValue() == "both") { 
    	lockMode = "in"
    } else if (device.currentState("lockMode").getValue() == "in")  {
    	lockMode = "out"
    } else if (device.currentState("lockMode").getValue() == "out")  {
    	lockMode = "none"
    } else {
    	lockMode = "both"
    }
    setLockMode(lockMode)
    sendEvent(name: "lockMode", value: lockMode)
    refresh()
}

def setLockMode(mode) {
	def modeValue
	switch (mode) {
    	case "none":
            modeValue = 0
       		break;
        case "out":
        	modeValue = 1
            break;
        case "in":
        	modeValue = 2
            break;
        default:
        	modeValue = 3
			break;
    }
	def body = [
    	locking: modeValue
    ]
	def resp = parent.apiPUT("/api/device/" + device.deviceNetworkId + "/control", body)
}

def getBatteryPercent(voltage) {
	def percentage
	switch (voltage) {
    	case voltage < 4:
            percentage = 1
       		break;
        case voltage < 4.5:
            percentage = 25
       		break;
        case voltage < 5:
            percentage = 50
       		break;
        case voltage < 5.5:
            percentage = 75
       		break;
        default:
            percentage = 100
       		break;
    }
}

def lock() {
	log.debug "Executing 'lock'"
	setLockMode("both")
}

def unlock() {
	log.debug "Executing 'unlock'"
	setLockMode("none")
}

def refresh() {
	log.debug "Executing 'refresh'"
	poll()    
}