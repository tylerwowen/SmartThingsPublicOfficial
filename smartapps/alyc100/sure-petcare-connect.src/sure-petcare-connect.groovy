/**
 *  Sure Petcare (Connect)
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
 * 	VERSION HISTORY
 *	06.09.2019 - v1.0 - Initial Version
 */
definition(
    name: "Sure PetCare (Connect)",
    namespace: "alyc100",
    author: "Alex Lee Yuk Cheung",
    description: "Connect your Sure PetCare devices to SmartThings.",
    category: "",
    iconUrl: "https://www.surepetcare.io/assets/images/onboarding/Sure_Petcare_Logo.png",
    iconX2Url: "https://www.surepetcare.io/assets/images/onboarding/Sure_Petcare_Logo.png",
    iconX3Url: "https://www.surepetcare.io/assets/images/onboarding/Sure_Petcare_Logo.png")
    singleInstance: true


preferences {
	page(name:"firstPage", title:"Sure PetCare Device Setup", content:"firstPage", install: true)
    page(name: "loginPAGE")
    page(name: "selectDevicePAGE")
}

def apiURL(path = '/') 			 { return "https://app.api.surehub.io${path}" }
def deviceId()			 { return (Math.abs(new Random().nextInt() % 9999999999) + 1000000000).toString() }

def firstPage() {
	if (username == null || username == '' || password == null || password == '') {
		return dynamicPage(name: "firstPage", title: "", install: true, uninstall: true) {
			section {
    			headerSECTION()
                href("loginPAGE", title: null, description: authenticated() ? "Authenticated as " +username : "Tap to enter Sure PetCare account credentials", state: authenticated())
  			}
    	}
    }
    else
    {
        return dynamicPage(name: "firstPage", title: "", install: true, uninstall: true) {
			section {
            	headerSECTION()
                href("loginPAGE", title: null, description: authenticated() ? "Authenticated as " +username : "Tap to enter Sure PetCare account credentials", state: authenticated())
            }
            if (stateTokenPresent()) {           	
                section ("Choose your Sure PetCare devices:") {
					href("selectDevicePAGE", title: null, description: devicesSelected() ? getDevicesSelectedString() : "Tap to select Sure PetCare devices", state: devicesSelected())
        		}
            } else {
            	section {
            		paragraph "There was a problem connecting to Sure PetCare. Check your user credentials and error logs in SmartThings web console.\n\n${state.loginerrors}"
           		}
           }
    	}
    }
}

def loginPAGE() {
	if (username == null || username == '' || password == null || password == '') {
		return dynamicPage(name: "loginPAGE", title: "Login", uninstall: false, install: false) {
    		section { headerSECTION() }
        	section { paragraph "Enter your Sure PetCare account credentials below to enable SmartThings and Sure PetCare integration." }
    		section {
    			input("username", "text", title: "Username", description: "Your Sure PetCare username (usually an email address)", required: true)
				input("password", "password", title: "Password", description: "Your Sure PetCare password", required: true, submitOnChange: true)
  			}   	
    	}
    }
    else {
    	getSurePetCareAccessToken()
        dynamicPage(name: "loginPAGE", title: "Login", uninstall: false, install: false) {
    		section { headerSECTION() }
        	section { paragraph "Enter your Sure PetCare account credentials below to enable SmartThings and Sure PetCare integration." }
    		section("Sure PetCare Credentials:") {
				input("username", "text", title: "Username", description: "Your Sure PetCare username (usually an email address)", required: true)
				input("password", "password", title: "Password", description: "Your Sure PetCare password", required: true, submitOnChange: true)	
			}    	
    	
    		if (stateTokenPresent()) {
        		section {
                	paragraph "You have successfully connected to Sure PetCare. Click 'Done' to select your Sure PetCare devices."
  				}
        	}
        	else {
        		section {
            		paragraph "There was a problem connecting to Sure PetCare. Check your user credentials and error logs in SmartThings web console.\n\n${state.loginerrors}"
           		}
        	}
        }
    }
}

def selectDevicePAGE() {
	updateLocations()
	dynamicPage(name: "selectDevicePAGE", title: "Devices", uninstall: false, install: false) {
  	section { headerSECTION() }
    if (devicesSelected() == null) {
    	section("Select your Location:") {
			input "selectedLocation", "enum", image: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/warmup-location.png", required:false, title:"Select a Location \n(${state.surePetCareLocations.size() ?: 0} found)", multiple:false, options:state.surePetCareLocations, submitOnChange: true
		}
    }
    else {
    	section("Your location:") {
        	paragraph (image: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/warmup-location.png",
                  "Location: ${state.surePetCareLocations[selectedLocation]}\n(Remove all devices to change)")
        }
    }
    if (selectedLocation) {
    	updateDevices()
    	section("Select your devices:") {
			input "selectedHub", "enum", image: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/surepetcare-hub.png", required:false, title:"Select PetCare Hub Devices \n(${state.surePetCareHubDevices.size() ?: 0} found)", multiple:true, options:state.surePetCareHubDevices
			input "selectedPetDoorConnect", "enum", image: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/surepetcare-petdoor.png", required:false, title:"Select Pet Door Connect Devices \n(${state.surePetCarePetDoorConnectDevices.size() ?: 0} found)", multiple:true, options:state.surePetCarePetDoorConnectDevices
            input "selectedDualScanCatFlapConnect", "enum", image: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/surepetcare-catflap.png", required:false, title:"Select Dual Scan Cat Flap Connect Devices \n(${state.surePetCareDualScanCatFlapConnectDevices.size() ?: 0} found)", multiple:true, options:state.surePetCareDualScanCatFlapConnectDevices
		}
        
        section("Select your pets:") {
			input "selectedPet", "enum", image: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/surepetcare-pet.png", required:false, title:"Select Your Pet \n(${state.surePetPets.size() ?: 0} found)", multiple:true, options:state.surePetPets
			}
    }
  }
}

def headerSECTION() {
	return paragraph (image: "https://www.surepetcare.io/assets/images/onboarding/Sure_Petcare_Logo.png",
                  "${textVersion()}")
}

def stateTokenPresent() {
	return state.surePetCareAccessToken != null && state.surePetCareAccessToken != ''
}

def authenticated() {
	return (state.surePetCareAccessToken != null && state.surePetCareAccessToken != '') ? "complete" : null
}

def devicesSelected() {
	return (selectedHub || selectedPetDoorConnect || selectedDualScanCatFlapConnect || selectedPet) ? "complete" : null
}

def getDevicesSelectedString() {
	if (state.surePetCareHubDevices == null ||
    	state.surePetCarePetDoorConnectDevices == null || 
        state.surePetCareDualScanCatFlapConnectDevices == null || 
        state.surePetPets == null) {
    	updateDevices()
  	}
	def listString = ""
    
	selectedHub.each { childDevice ->    
    	if (null != state.surePetCareHubDevices)
    		listString += "${state.surePetCareHubDevices[childDevice]}\n"
    }
  
	selectedPetDoorConnect.each { childDevice ->
      if (null != state.surePetCarePetDoorConnectDevices) 
           	listString += "${state.surePetCarePetDoorConnectDevices[childDevice]}\n"
	}
    
	selectedDualScanCatFlapConnect.each { childDevice ->
        if (null != state.surePetCareDualScanCatFlapConnectDevices)
            listString += "${state.surePetCareDualScanCatFlapConnectDevices[childDevice]}\n"
	}
    
    selectedPet.each { childDevice ->
        if (null != state.surePetPets)
            listString += "${state.surePetPets[childDevice]}\n"
	}
    // Returns the completed list, and trims the last carrige return
	return listString.trim()
}

// App lifecycle hooks

def installed() {
	log.debug "installed"
	initialize()
	// Check for new devices and remove old ones every 3 hours
	runEvery3Hours('updateDevices')
    // execute refresh method every minute
    runEvery1Minute('refreshDevices')
}

// called after settings are changed
def updated() {
	log.debug "updated"
	initialize()
    unschedule('refreshDevices')
    runEvery1Minute('refreshDevices')
}

def uninstalled() {
	log.info("Uninstalling, removing child devices...")
	unschedule()
	removeChildDevices(getChildDevices())
}

private removeChildDevices(devices) {
	devices.each {
		deleteChildDevice(it.deviceNetworkId) // 'it' is default
	}
}

// called after Done is hit after selecting a Location
def initialize() {
	log.debug "initialize"
	if (selectedHub) {
		addHub()
	}
	if (selectedPetDoorConnect) {
		addPetDoorConnect()
	}
	if(selectedDualScanCatFlapConnect) {
        addDualScanCatFlapConnect()
    }
    
    if (selectedPet) {
    	addPet()
    }
 	runIn(10, 'refreshDevices') // Asynchronously refresh devices so we don't block
}

def updateDevices() {
	if (!state.devices) {
		state.devices = [:]
	}
	def devices = devicesList()
  	state.surePetCareHubDevices = [:]
  	state.surePetCarePetDoorConnectDevices = [:]
  	state.surePetCareDualScanCatFlapConnectDevices = [:]

    def selectors = []
	devices.each { device ->
        log.debug "Identified: device ${device.id}: ${device.product_id}: ${device.household_id}: ${device.name}: ${device.serial_number}: ${device.mac_address}"
        selectors.add("${device.id}")
        
        //Hub
        if (device.product_id == 1) {
        	log.debug "Identified: ${device.name} Pet Care Hub"
            def value = "${device.name} PetCare Hub"
            def key = device.id
            state.surePetCareHubDevices["${key}"] = value

            //Update names of devices with PetCare
            def childDevice = getChildDevice("${device.id}")
            if (childDevice) {
            	//Update name of device if different.
            	if(childDevice.name != device.name + " PetCare Hub") {
            		childDevice.name = device.name + " PetCare Hub"
                	log.debug "Device's name has changed."
            	}
            }
        }
        //Pet Door Connect
        else if (device.product_id == 3) {
        	log.debug "Identified: ${device.name} Pet Door Connect"
            def value = "${device.name} Pet Door Connect"
            def key = device.id
            state.surePetCarePetDoorConnectDevices["${key}"] = value

            //Update names of devices with PetCare
            def childDevice = getChildDevice("${device.id}")
            if (childDevice) {
            	//Update name of device if different.
            	if(childDevice.name != device.name + " Pet Door Connect") {
            		childDevice.name = device.name + " Pet Door Connect"
                	log.debug "Device's name has changed."
            	}
            }
        }
        //Dual Scan Cat Flap Connect
        else if (device.product_id == 6) {
        	log.debug "Identified: ${device.name} Dual Scan Cat Flap Connect"
            def value = "${device.name} Dual Scan Cat Flap Connect"
            def key = device.id
            state.surePetCareDualScanCatFlapConnectDevices["${key}"] = value

            //Update names of devices with PetCare
            def childDevice = getChildDevice("${device.id}")
            if (childDevice) {
            	//Update name of device if different.
            	if(childDevice.name != device.name + " Dual Scan Cat Flap Connect") {
            		childDevice.name = device.name + " Dual Scan Cat Flap Connect"
                	log.debug "Device's name has changed."
            	}
            }
        }
    }
    
    if (!state.pets) {
		state.pets = [:]
	}
    def pets = petsList()
    state.surePetPets = [:]
    
    pets.each {pet ->
    	def species = (pet.species_id == 2) ? "dog" : "cat"
    	log.debug "Identified: ${pet.name} the ${species}"
        selectors.add("${pet.id}")
        def value = "${pet.name} the ${species}"
            def key = pet.id
            state.surePetPets["${key}"] = value

            //Update names of pets with PetCare
            def childDevice = getChildDevice("${pet.id}")
            if (childDevice) {
            	//Update name of device if different.
            	if(childDevice.name != pet.name + " the ${species}") {
            		childDevice.name = pet.name + " the ${species}"
                	log.debug "Pet's name has changed."
            	}
            }
    }
    
    //Remove devices if does not exist on the Sure PetCare platform
    getChildDevices().findAll { !selectors.contains("${it.deviceNetworkId}") }.each {
		log.info("Deleting ${it.deviceNetworkId}")
        try {
			deleteChildDevice(it.deviceNetworkId)
        } catch (physicalgraph.exception.NotFoundException e) {
        	log.info("Could not find ${it.deviceNetworkId}. Assuming manually deleted.")
        } catch (physicalgraph.exception.ConflictException ce) {
        	log.info("Device ${it.deviceNetworkId} in use. Please manually delete.")
        }
	} 
}

def updateLocations() {
	def locations = locationsList()
	state.surePetCareLocations = [:]
    
    def selectors = []
	locations.each { location ->
        log.debug "Identified: location ${location.id}: ${location.name}"
            selectors.add("${location.id}")
            def value = "${location.name}"
			def key = location.id
			state.surePetCareLocations["${key}"] = value
    }
}

def addHub() {
	updateDevices()
    
    selectedHub.each { device ->
    	def childDevice = getChildDevice("${device}")
        if (!childDevice && state.surePetCareHubDevices[device] != null) {
    		log.info("Adding device ${device}: ${state.surePetCareHubDevices[device]}")

        	def data = [
                	name: state.surePetCareHubDevices[device],
					label: state.surePetCareHubDevices[device]
				]
            childDevice = addChildDevice(app.namespace, "Sure PetCare Hub", "$device", null, data)

			log.debug "Created ${state.surePetCareHubDevices[device]} with id: ${device}"
		} else {
			log.debug "found ${state.surePetCareHubDevices[device]} with id ${device} already exists"
		}
    }
}

def addPetDoorConnect() {
	updateDevices()
    
    selectedPetDoorConnect.each { device ->
    	def childDevice = getChildDevice("${device}")
        if (!childDevice && state.surePetCarePetDoorConnectDevices[device] != null) {
    		log.info("Adding device ${device}: ${state.surePetCarePetDoorConnectDevices[device]}")

        	def data = [
                	name: state.surePetCarePetDoorConnectDevices[device],
					label: state.surePetCarePetDoorConnectDevices[device]
				]
            childDevice = addChildDevice(app.namespace, "Sure PetCare Pet Door Connect", "$device", null, data)

			log.debug "Created ${state.surePetCarePetDoorConnectDevices[device]} with id: ${device}"
		} else {
			log.debug "found ${state.surePetCarePetDoorConnectDevices[device]} with id ${device} already exists"
		}
    }
}

def addDualScanCatFlapConnect() {
	updateDevices()
    
    selectedDualScanCatFlapConnect.each { device ->
    	def childDevice = getChildDevice("${device}")
        if (!childDevice && state.surePetCareDualScanCatFlapConnectDevices[device] != null) {
    		log.info("Adding device ${device}: ${state.surePetCareDualScanCatFlapConnectDevices[device]}")

        	def data = [
                	name: state.surePetCareDualScanCatFlapConnectDevices[device],
					label: state.surePetCareDualScanCatFlapConnectDevices[device]
				]
            childDevice = addChildDevice(app.namespace, "Sure PetCare Pet Door Connect", "$device", null, data)

			log.debug "Created ${state.surePetCareDualScanCatFlapConnectDevices[device]} with id: ${device}"
		} else {
			log.debug "found ${state.surePetCareDualScanCatFlapConnectDevices[device]} with id ${device} already exists"
		}
    }
}

def addPet() {
	updateDevices()
    
    selectedPet.each { device ->
    	def childDevice = getChildDevice("${device}")
        if (!childDevice && state.surePetPets[device] != null) {
    		log.info("Adding pet ${device}: ${state.surePetPets[device]}")

        	def data = [
                	name: state.surePetPets[device],
					label: state.surePetPets[device]
				]
            childDevice = addChildDevice(app.namespace, "Sure PetCare Pet", "$device", null, data)

			log.debug "Created ${state.surePetPets[device]} with id: ${device}"
		} else {
			log.debug "found ${state.surePetPets[device]} with id ${device} already exists"
		}
    }
}

def refreshDevices() {
	log.info("Executing refreshDevices...")
    if (atomicState.refreshCounter == null || atomicState.refreshCounter >= 10) {
    	atomicState.refreshCounter = 0
    } else {
    	atomicState.refreshCounter = atomicState.refreshCounter + 1
    }
	getChildDevices().each { device ->
    	if (atomicState.refreshCounter == 10) {
        	log.info("Low Freq Refreshing device ${device.name} ...")
            try {
    			device.refresh()
        	} catch (e) {
        		//WORKAROUND - Catch unexplained exception when refreshing devices.
        		logResponse(e.response)
        	}
        } else if (device.typeName.contains("Pet") || device.typeName.contains("Pet Door Connect")) {
        	log.info("High Freq Refreshing device ${device.typeName}...")
			device.refresh()
        }
	}
}

def devicesList() {
	logErrors([]) {
    	def resp = apiGET('/api/household/' + selectedLocation + '/device')
		if (resp.status == 200) {
			return resp.data.data
		} else {
			log.error("Non-200 from device list call. ${resp.status} ${resp.data}")
			return []
		}
	}
}

def petsList() {
	logErrors([]) {
    
    	def body = [:]
		def resp = apiGET('/api/household/' + selectedLocation + '/pet')
		if (resp.status == 200) {
            log.debug resp.data.data
			return resp.data.data
		} else {
			log.error("Non-200 from location list call. ${resp.status} ${resp.data}")
			return []
		}
	}
}

def locationsList() {
	logErrors([]) {
    
    	def body = [:]
		def resp = apiGET('/api/household')
		if (resp.status == 200) {
            log.debug resp.data.data
			return resp.data.data
		} else {
			log.error("Non-200 from location list call. ${resp.status} ${resp.data}")
			return []
		}
	}
}

def getSurePetCareAccessToken() {  
	try {
    	def params = [
			uri: apiURL('/api/auth/login'),
        	contentType: 'application/json',
        	headers: [
              'Content-Type': 'application/json'
        	],
        	body: [
        		email_address: settings.username,
                password: settings.password,
                device_id: deviceId()  	
    		]
        ]

		state.cookie = ''

		httpPostJson(params) {response ->
			log.debug "Request was successful, $response.status"
			log.debug response.headers

        	state.cookie = response?.headers?.'Set-Cookie'?.split(";")?.getAt(0)
			log.debug "Adding cookie to collection: $cookie"
        	log.debug "auth: $response.data"
			log.debug "cookie: $state.cookie"
        	log.debug "sessionid: ${response.data.data.token}"

        	state.surePetCareAccessToken = response.data.data.token
        	// set the expiration to 5 minutes
			state.surePetCareAccessToken_expires_at = new Date().getTime() + 300000
            state.loginerrors = null
		}
    } catch (groovyx.net.http.HttpResponseException e) {
    	state.surePetCareAccessToken = null
        state.surePetCareAccessToken_expires_at = null
   		state.loginerrors = "Error: ${e.response.status}: ${e.response.data}"
    	logResponse(e.response)
		return e.response
    }
}

def apiPOST(path, body = [:]) {
	def bodyString = new groovy.json.JsonBuilder(body).toString()
	log.debug("Beginning API POST: ${apiURL(path)}, ${bodyString}")
    try {
    	httpPost(uri: apiURL(path), body: bodyString, headers: apiRequestHeaders() ) {
    		response ->
			logResponse(response)
			return response
        }
	} catch (groovyx.net.http.HttpResponseException e) {
		logResponse(e.response)
		return e.response
	}
}

def apiPUT(path, body = [:]) {
	def bodyString = new groovy.json.JsonBuilder(body).toString()
	log.debug("Beginning API POST: ${apiURL(path)}, ${bodyString}")
    try {
    	httpPut(uri: apiURL(path), body: bodyString, headers: apiRequestHeaders() ) {
    		response ->
			logResponse(response)
			return response
        }
	} catch (groovyx.net.http.HttpResponseException e) {
		logResponse(e.response)
		return e.response
	}
}

def apiGET(path) {
	log.debug("Beginning API GET: ${apiURL(path)}")
    try {
    	httpGet(uri: apiURL(path), headers: apiRequestHeaders() ) {
    		response ->
			logResponse(response)
			return response
        }
	} catch (groovyx.net.http.HttpResponseException e) {
		logResponse(e.response)
		return e.response
	}
}

def getHouseholdID() {
	return selectedLocation
}

def getTimeZone() {
	def tz = null
	if(location?.timeZone) { tz = location?.timeZone }
	if(!tz) { log.warn "No time zone has been retrieved from SmartThings. Please try to open your ST location and press Save." }
	return tz
}

Map apiRequestHeaders() {
   return [ "Authorization": "Bearer $state.surePetCareAccessToken",
            "Content-Type": "application/json"
	]
}

def logResponse(response) {
	log.info("Status: ${response.status}")
	log.info("Body: ${response.data}")
}

def logErrors(options = [errorReturn: null, logObject: log], Closure c) {
	try {
		return c()
	} catch (groovyx.net.http.HttpResponseException e) {
		options.logObject.error("got error: ${e}, body: ${e.getResponse().getData()}")
		if (e.statusCode == 401) { // token is expired
			state.remove("surePetCareAccessToken")
			options.logObject.warn "Access token is not valid"
		}
		return options.errorReturn
	} catch (java.net.SocketTimeoutException e) {
		options.logObject.warn "Connection timed out, not much we can do here"
		return options.errorReturn
	}
}



private def textVersion() {
    def text = "Sure PetCare (Connect)\nVersion: 1.0\nDate: 06092019(2300)"
}

private def textCopyright() {
    def text = "Copyright © 2019 Alex Lee Yuk Cheung"
}