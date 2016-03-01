/*
 Copyright 2016 Herko ter Horst
 __________________________________________________________________________

 Filename: _androidautoApp.js
 __________________________________________________________________________
 */

log.addSrcFile("_androidautoApp.js", "_androidauto");

function _androidautoApp(uiaId)
{
    log.debug("Constructor called.");

    // Base application functionality is provided in a common location via this call to baseApp.init().
    // See framework/js/BaseApp.js for details.
    baseApp.init(this, uiaId);
    
//    framework.sendEventToMmui("common", "SelectBTAudio");

}


/*********************************
 * App Init is standard function *
 * called by framework           *
 *********************************/

/*
 * Called just after the app is instantiated by framework.
 * All variables local to this app should be declared in this function
 */
_androidautoApp.prototype.appInit = function()
{
    log.debug("_androidautoApp appInit  called...");

    //Context table
    //@formatter:off
    this._contextTable = {
        "Start": { // initial context must be called "Start"
            "sbName": "Android Auto",
            "template": "AndroidAutoTmplt",
            "properties" : {
                "keybrdInputSurface" : "NATGUI_SURFACE", 
                "visibleSurfaces" :  ["NATGUI_SURFACE"]    // Do not include JCI_OPERA_PRIMARY in this list            
            },// end of list of controlProperties
            "templatePath": "apps/_androidauto/templates/AndroidAuto", //only needed for app-specific templates
            "readyFunction": this._StartContextReady.bind(this)
        } // end of "AndroidAuto"
    }; // end of this.contextTable object
    //@formatter:on

    //@formatter:off
    this._messageTable = {
        // haven't yet been able to receive messages from MMUI
    };
    //@formatter:on
};

/**
 * =========================
 * CONTEXT CALLBACKS
 * =========================
 */
_androidautoApp.prototype._StartContextReady = function ()
{
    // do anything you want here
	if (!document.getElementById("jquery1-script")) {
		var docBody = document.getElementsByTagName("body")[0];
		if (docBody) {
			var script1 = document.createElement("script");
			script1.setAttribute("id", "jquery1-script");
			script1.setAttribute("src", "/jci/gui/apps/_androidauto/js/jquery.min.js");
			script1.addEventListener('load', function () {
				androidauto();
			}, false);
			docBody.appendChild(script1);
		}
	} else {
		androidauto();
		}
};

function androidauto() {
	var ws = new WebSocket('ws://localhost:9999/');
	ws.onopen = function() {
		ws.send("export LD_LIBRARY_PATH=/data_persist/dev/androidauto/custlib:/jci/lib:/jci/opera/3rdpartylibs/freetype:/usr/lib/imx-mm/audio-codec:/usr/lib/imx-mm/parser:/data_persist/dev/lib: \n");
		ws.send("headunit \n");
	};
	
	ws.onmessage = function(event) {
		var psconsole = $('#aaStatusText');
		psconsole.focus();
		psconsole.append(event.data + '\n');
		
		if(psconsole.length)
			psconsole.scrollTop(psconsole[0].scrollHeight - psconsole.height());
	};
}  

/**
 * =========================
 * Framework register
 * Tell framework this .js file has finished loading
 * =========================
 */
framework.registerAppLoaded("_androidauto", null, false);
