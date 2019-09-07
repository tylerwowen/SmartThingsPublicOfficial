/**
 *  Sure PetCare Pet
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
 *	VERSION HISTORY
 *	07.09.2019 - v1.0b - Bug fix. Change method of finding 'look through' events.
 			   - Add Tag ID to tiles
 *	06.09.2019 - v1.0 - Initial Version
 */
metadata {
	definition (name: "Sure PetCare Pet", namespace: "alyc100", author: "Alex Lee Yuk Cheung", ocfDeviceType: "oic.r.sensor.presence", mnmn: "SmartThings", vid: "surepet-pet-presence") {
		capability "Sensor"
		capability "Polling"
		capability "Refresh"
		capability "Presence Sensor"
        
		command "refresh"
	}
    
	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
		standardTile("presence", "device.presence", width: 4, height: 4, canChangeIcon: false, canChangeBackground: true) {
			state("present", 	labelIcon:"st.presence.tile.mobile-present", 	backgroundColor:"#00a0dc")
			state("not present",labelIcon:"st.presence.tile.mobile-not-present",backgroundColor:"#cccccc")
		}
        
        valueTile("tag", "device.tag", decoration: "flat", width: 4, height: 1) {
			state "default", label: 'Tag Number:\n${currentValue}'
		}
        
        valueTile("petInfo", "device.petInfo", decoration: "flat", width: 4, height: 1) {
			state "default", label: '${currentValue}'
		}
        
        standardTile("refresh", "device.refresh", width:2, height:2, decoration: "flat") {
			state("default", label:'refresh', action:"refresh.refresh", icon:"st.secondary.refresh-icon")
		}

        main(["presence"])
        details(["presence", "tag", "petInfo", "refresh"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'switch' attribute

}

// handle commands
def installed() {
	log.debug "Executing 'installed'"
}

def updated() {
	log.debug "Executing 'updated'"
}

def poll() {
	log.debug "Executing 'poll' for ${device} ${this} ${device.deviceNetworkId}"
    
    def resp = parent.apiGET("/api/me/start")
	if (resp.status != 200) {
		log.error("Unexpected result in poll(): [${resp.status}] ${resp.data}")
		return []
	}
    def response = resp.data.data.pets
    def pet = response.find{device.deviceNetworkId.toInteger() == it.id}
    def presence = pet.position.where
    def pres = (presence == 1) ? "present" : "not present"
    sendEvent(name: 'presence', value: pres, descriptionText: "${device.name} is ${pres.toLowerCase()}", displayed: true)
    
    def tag_id = pet.tag_id
    response = resp.data.data.tags
    def tag = response.find{tag_id == it.id}
    sendEvent(name: 'tag_id', value: tag_id, displayed: true)
    sendEvent(name: 'tag', value: tag.tag.toString() + ".", displayed: true)
    
    //Pick up look through flap events
    resp = parent.apiGET("/api/timeline/household/" + parent.getHouseholdID() + "/pet")
    if (resp.status != 200) {
		log.error("Unexpected result in poll(): [${resp.status}] ${resp.data}")
		return []
	}
    
    if (!state.lastTimePetLooked) {
    	state.lastTimePetLooked = now()
    }
    
    for(def entry : resp.data.data) {
    	if (entry.movements) { 
        	if (entry.movements[0].tag_id == device.currentState("tag_id").getValue() && entry.movements[0].direction == 0) {
            	def movementDate = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", entry.movements[0].created_at)
            	if (state.lastTimePetLooked < movementDate.getTime()) {
                	state.lastTimePetLooked = movementDate.getTime()
                    //Notify pet peek event.
                    def entryDeviceName = "flap"
                    if (entry.devices) {
                    	entryDeviceName = entry.devices[0].name
                    }
                    def msg = "${device.name} looked through ${entryDeviceName} at ${movementDate.format("hh:mm aaa dd MMM yyyy", parent.getTimeZone())}"
                    sendEvent(name: "petInfo", value: msg, displayed: true, linkText: "${device.displayName}", descriptionText: msg)
                	break
            	}
        	}
    	} 
	}
}

def refresh() {
	log.debug "Executing 'refresh'"
	poll()
}

def setPhotoUrl(url) {
	state.photoURL = url
}