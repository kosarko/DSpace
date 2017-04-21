package org.dspace.springmvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.google.api.Google;
import org.springframework.social.google.api.drive.DriveFilesPage;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
@RequestMapping("/drive")
public class DriveController {

	private final Google google;

	@Autowired
	public DriveController(Google google) {
		this.google = google;
	}
	
	@ResponseBody
	@RequestMapping(value="/list", method=GET)
	public DriveFilesPage getTaskLists(@RequestParam(required=false) String pageToken) {
		return google.driveOperations().getRootFiles(pageToken);
	}

	@ResponseBody
	@RequestMapping(value = "/test", method = GET)
	public String getTestString(){
		return "TEST";
	}
}
