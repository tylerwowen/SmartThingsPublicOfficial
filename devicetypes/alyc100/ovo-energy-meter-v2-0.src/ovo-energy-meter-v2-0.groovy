/**
 *  OVO Energy Meter V2.0
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
 *  VERSION HISTORY
 *  v2.0 - Initial V2.0 Release with OVO Energy (Connect) app
 *
 *  v2.1 - Improve pricing calculations using contract info from OVO. Notification framework for high costs.
 *		   Enable alert for specified daily cost level breach.
 *	v2.1b - Allow cost alert level to be decimal
 *
 *	v2.2 - Percentage comparison from previous cost values added into display
 *	v2.2.1 - Add current consumption price based on unit price from OVO account API not OVO live API
 *	v2.2.1b - Remove double negative on percentage values.
 *	v2.2.2 - Change current hour logic to accommodate GMT/BST.
 *	v2.2.2b - Alter Simple Date Format hour string
 *
 *	10.11.2016: v2.3 - Added historical power chart for the last 5 days.
 *	10.11.2016: v2.3.1 - Fix chart Android compatibility.
 *	11.11.2016: v2.3.2 - Move chart data into state variable
 *	11.11.2016: v2.3.3 - Prevent potential executeAction() error when adding device.
 *	11.11.2016: v2.3.4 - Migrate variable from data to state.
 *	11.11.2016: v2.3.5 - Bug Fix. Silly state variable not initialised on first run.
 *	11.11.2016: v2.3.6 - Reduce number of calls to account API.
 *	12.11.2016: v2.3.7 - Stop yesterday cost comparison being 0%.
 *
 *	06.12.2016: v2.4 - Better API failure handling and recovery. Historical and yesterday power feed from OVO API.
 *  06.12.2016: v2.4.1 - Relax setting offline mode to 60 minute down time.
 *  07.12.2016: v2.4.1b - Handle when OVO API hasn't generated yesterday's total figures at midnight.
 *  07.12.2016: v2.4.1c - Add 'Pending' connection status for short API issues
 *  11.01.2017: v2.4.2 - Resolve Android issues for Chart data.
 *  18.02.2018: v2.4.3 - Enable chart display when API returns corrupt historic data.
 *
 *	01.06.2018: v2.5 	- Remove all live feed tiles as they are now unsupported on OVO.
 *						- Add yesterday's total power consumption into main tile.
 *						- Update chart data with historical values only.
 *
 *	10.10.2018: v2.6 - Compatibility with New Smartthings App
 */
 
preferences 
{
	input( "costAlertLevel", "decimal", title: "Set cost alert level (£)", description: "Send alert when daily cost reaches amount", required: false, defaultValue: 10.00 )
}

metadata {
	definition (name: "OVO Energy Meter V2.0", namespace: "alyc100", author: "Alex Lee Yuk Cheung", ocfDeviceType: "oic.d.switch", mnmn: "SmartThings", vid: "generic-switch-power") {
		capability "Polling"
		capability "Power Meter"
		capability "Refresh"
        capability "Sensor"
        capability "Health Check"
        
        attribute "network","string"
	}

	tiles(scale: 2) {
  		multiAttributeTile(name:"power", type:"generic", width:6, height:4, canChangeIcon: true) {
    		tileAttribute("device.power", key: "PRIMARY_CONTROL") {
      			attributeState "default", label: '${currentValue} Wh', icon:"st.Appliances.appliances17", backgroundColor:"#0a9928"
    		}
  		}
        
        valueTile("consumptionPrice", "device.consumptionPrice", decoration: "flat", width: 3, height: 2) {
			state "default", label: 'Curr. Cost:\n${currentValue}/h'
		}
        valueTile("unitPrice", "device.unitPrice", decoration: "flat", width: 3, height: 2) {
			state "default", label: 'Unit Price:\n${currentValue}'
		}
        
        valueTile("totalDemand", "device.averageDailyTotalPower", decoration: "flat", width: 3, height: 1) {
			state "default", label: 'Total Power:\n${currentValue} kWh'
		}
        valueTile("totalConsumptionPrice", "device.currentDailyTotalPowerCost", decoration: "flat", width: 3, height: 1) {
			state "default", label: 'Total Cost:\n${currentValue}'
		}
        
        valueTile("yesterdayTotalPower", "device.yesterdayTotalPower", decoration: "flat", width: 3, height: 1) {
			state "default", label: 'Yesterday Total Power :\n${currentValue} kWh'
		}
        valueTile("yesterdayTotalPowerCost", "device.yesterdayTotalPowerCost", decoration: "flat", width: 3, height: 1) {
			state "default", label: 'Yesterday Total Cost:\n${currentValue}'
		}
        
        standardTile("network", "device.network", width: 2, height: 2, inactiveLabel: false, canChangeIcon: false) {
			state ("default", label:'unknown', icon: "st.unknown.unknown.unknown")
			state ("Connected", label:'Online', icon: "st.Health & Wellness.health9", backgroundColor: "#79b821")
			state ("Pending", label:'Pending', icon: "st.Health & Wellness.health9", backgroundColor: "#ffa500")
			state ("Not Connected", label:'Offline', icon: "st.Health & Wellness.health9", backgroundColor: "#bc2323")
		}
        
		standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
        htmlTile(name:"chartHTML", action: "getImageChartHTML", width: 6, height: 5, whiteList: ["www.gstatic.com", "raw.githubusercontent.com"])
        
		main (["power"])
		details(["power", "unitPrice", "yesterdayTotalPower", "yesterdayTotalPowerCost", "chartHTML", "refresh"])
	}
}

mappings {
    path("/getImageChartHTML") {action: [GET: "getImageChartHTML"]}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'power' attribute

}

// handle commands
def installed() {
    sendEvent(name: "checkInterval", value: 48 * 60 * 60 + 2 * 60, data: [protocol: "cloud"], displayed: false)
}

def updated() {
    sendEvent(name: "checkInterval", value: 48 * 60 * 60 + 2 * 60, data: [protocol: "cloud"], displayed: false)
}

def poll() {
	log.debug "Executing 'poll'"
	refreshLiveData()
}

def refresh() {
	log.debug "Executing 'refresh'"
	poll()    
}

def refreshLiveData() {
	//Get current hour
    //state.hour = null
    def df = new java.text.SimpleDateFormat("HH")
    if (location.timeZone) {
		df.setTimeZone(location.timeZone)
	}
	else {
		df.setTimeZone(TimeZone.getTimeZone("Europe/London"))
	}
	def currentHour = df.format(new Date()).toInteger()
        
        if ((state.hour == null) || (state.hour != currentHour)) {
            //Add historical figures to chart data object
            addHistoricalPowerToChartData()
            setYesterdayPowerValues()
            
        	//Reset at midnight or initial call
        	if ((state.hour == null) || (currentHour == 0)) { 
                sendEvent(name: 'costAlertLevelPassed', value: "false", displayed: false)
            }       	
            
        	state.hour = currentHour
        }
}

private def calculatePercentChange(current, previous) {
	def delta = current - previous
    if (previous != 0) {
    	return  Math.round((delta / previous) * 100)
    } else {
    	if (delta > 0) return 1000
        else if (delta == 0) return 0
        else return -1000
    }    
}

def getCostAlertLevelValue() {
	if (settings.costAlertLevel == null) {
    	return "10"
    } 
    return settings.costAlertLevel
}

def getAggregatePower(fromDate, toDate) {
	return parent.apiGET("https://live.ovoenergy.com/api/live/meters/${device.deviceNetworkId}/consumptions/aggregated?from=${fromDate.format("yyyy-MM-dd")}T00%3A00%3A00.000Z&to=${toDate.format("yyyy-MM-dd")}T00%3A00%3A00.000Z&granularity=DAY")
}

def setYesterdayPowerValues() {
	//Store the day's power info as yesterdays
    def date = new Date()
    def resp = getAggregatePower((date - 2), date)
    if (resp.status != 200) {
    	log.error("Unexpected result in setYesterdayPowerValues(): [${resp.status}] ${resp.data}")
	} else {
    	def consumptions = resp.data.consumptions
        if (consumptions[1].dataError != "NotFound") {
    		//consumptions[1].price, consumptions[0].price
        	def yesterdayTotalPower = Math.round((consumptions[1].consumption as BigDecimal) * 1000) / 1000
        	sendEvent(name: 'yesterdayTotalPower', value: "$yesterdayTotalPower", unit: "KWh", displayed: false)
            def yesterdayTotalPowerInWh = Math.round((consumptions[1].consumption as BigDecimal) * 1000)
        	sendEvent(name: 'power', value: "$yesterdayTotalPowerInWh", unit: "Wh", displayed: true)
        
        	def yesterdayTotalPowerCost = (Math.round((consumptions[1].price as BigDecimal) * 100))/100
        
        	def formattedCostYesterdayComparison = 0
        	//Calculate cost difference between days
        	def costYesterdayComparison = calculatePercentChange(consumptions[1].price as BigDecimal, consumptions[0].price as BigDecimal)
        	formattedCostYesterdayComparison = costYesterdayComparison
        	if (costYesterdayComparison >= 0) {
        		formattedCostYesterdayComparison = "+" + formattedCostYesterdayComparison
        	}
                    
        	yesterdayTotalPowerCost = String.format("%1.2f",yesterdayTotalPowerCost)
        	sendEvent(name: 'yesterdayTotalPowerCost', value: "£$yesterdayTotalPowerCost (" + formattedCostYesterdayComparison + "%)", displayed: false)
            
            def unitPrice = (Math.round((consumptions[1].tariff as BigDecimal) * 100))/100
            sendEvent(name: 'unitPrice', value: "£$unitPrice", displayed: false)
            
            //Send event to raise notification on high cost
        	if ((yesterdayTotalPowerCost as BigDecimal) > (getCostAlertLevelValue() as BigDecimal)) {
        		sendEvent(name: 'costAlertLevelPassed', value: "£${getCostAlertLevelValue()}")
        	} else {
        		sendEvent(name: 'costAlertLevelPassed', value: "false", displayed: false)
        	}
            
    	} else {
        	sendEvent(name: 'yesterdayTotalPower', value: "TBD", unit: "KWh", displayed: false)
            sendEvent(name: 'yesterdayTotalPowerCost', value: "being calculated...", displayed: false)
        	sendEvent(name: 'costAlertLevelPassed', value: "false", displayed: false)
        }
    }
}

def addHistoricalPowerToChartData() {
    def date = new Date()
	def resp = getAggregatePower((date - 7), date)
    if (resp.status != 200) {
    	log.error("Unexpected result in addHistoricalPowerToChartData(): [${resp.status}] ${resp.data}")
	}
    else {
    	def consumptions = resp.data.consumptions
        if (consumptions[6]) {
    		state.chartData = [consumptions[6].price, consumptions[5].price, consumptions[4].price, consumptions[3].price, consumptions[2].price, consumptions[1].price, consumptions[0].price]
    	} else {
        	state.chartData = [0, consumptions[5].price, consumptions[4].price, consumptions[3].price, consumptions[2].price, consumptions[1].price, consumptions[0].price]
    	}
    }
}

def getImageChartHTML() {
	try {
    	def date = new Date()
		if (state.chartData == null) {
    		state.chartData = [0, 0, 0, 0, 0, 0, 0]
    	} else {
        	def removeNull = state.chartData.collect { it == null ? it = 0 : it }
            state.chartData = removeNull
		}
        def topValue = state.chartData.max()
		def hData = """
        	<h4 style="font-size: 22px; font-weight: bold; text-align: center; background: #00a1db; color: #f5f5f5;">Historical Costs</h4><br>
	  		<div id="main_graph" style="width: 100%;"><img src="http://chart.googleapis.com/chart?cht=bvg&chs=350x200&chxt=x,y,y&chco=0a9928|0a9928|0a9928|0a9928|0a9928|0a9928|0a9928&chd=t:${state.chartData.getAt(6)},${state.chartData.getAt(5)},${state.chartData.getAt(4)},${state.chartData.getAt(3)},${state.chartData.getAt(2)},${state.chartData.getAt(1)},${state.chartData.getAt(0)}&chds=0,${topValue+1}&chxl=0:|${(date - 7).format("d MMM")}|${(date - 6).format("d MMM")}|${(date - 5).format("d MMM")}|${(date - 4).format("d MMM")}|${(date - 3).format("d MMM")}|${(date - 2).format("d MMM")}|${(date - 1).format("d MMM")}|2:|Cost&chxp=2,50&chxr=1,0,${topValue+1}&chbh=a,10,10&chxs=1N*cGBPsz2*"></div>		  
			"""

		def mainHtml = """
		<!DOCTYPE html>
		<html>
			<head>
				<meta http-equiv="cache-control" content="max-age=0"/>
				<meta http-equiv="cache-control" content="no-cache"/>
				<meta http-equiv="expires" content="0"/>
				<meta http-equiv="expires" content="Tue, 01 Jan 1980 1:00:00 GMT"/>
				<meta http-equiv="pragma" content="no-cache"/>
				<meta name="viewport" content="width = device-width, user-scalable=no, initial-scale=1.0">

				<link rel="stylesheet prefetch" href="${getCssData()}"/>
			</head>
			<body>
                ${hData}
			</body>
			</html>
		"""
		render contentType: "text/html", data: mainHtml, status: 200
	}
	catch (ex) {
		log.error "getImageChartHTML Exception:", ex
	}
}

def getCssData() {
	def cssData = null
	def htmlInfo
	state.cssData = null

	if(htmlInfo?.cssUrl && htmlInfo?.cssVer) {
		if(state?.cssData) {
			if (state?.cssVer?.toInteger() == htmlInfo?.cssVer?.toInteger()) {
				cssData = state?.cssData
			} else if (state?.cssVer?.toInteger() < htmlInfo?.cssVer?.toInteger()) {
				cssData = getFileBase64(htmlInfo.cssUrl, "text", "css")
				state.cssData = cssData
				state?.cssVer = htmlInfo?.cssVer
			}
		} else {
			cssData = getFileBase64(htmlInfo.cssUrl, "text", "css")
			state?.cssData = cssData
			state?.cssVer = htmlInfo?.cssVer
		}
	} else {
		cssData = getFileBase64(cssUrl(), "text", "css")
	}
	return cssData
}

def getFileBase64(url, preType, fileType) {
	try {
		def params = [
			uri: url,
			contentType: '$preType/$fileType'
		]
		httpGet(params) { resp ->
			if(resp.data) {
				def respData = resp?.data
				ByteArrayOutputStream bos = new ByteArrayOutputStream()
				int len
				int size = 4096
				byte[] buf = new byte[size]
				while ((len = respData.read(buf, 0, size)) != -1)
					bos.write(buf, 0, len)
				buf = bos.toByteArray()
				String s = buf?.encodeBase64()
				return s ? "data:${preType}/${fileType};base64,${s.toString()}" : null
			}
		}
	}
	catch (ex) {
		log.error "getFileBase64 Exception:", ex
	}
}

def cssUrl()	 { return "https://raw.githubusercontent.com/desertblade/ST-HTMLTile-Framework/master/css/smartthings.css" }