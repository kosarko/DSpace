package org.dspace.springmvc;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.google.api.Google;
import org.springframework.social.google.api.drive.DriveFilesPage;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

import java.sql.SQLException;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
@RequestMapping("/drive")
public class DriveController {

	Logger log = Logger.getLogger(DriveController.class);

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
	public String getTestString(HttpServletRequest request){
		try {
			Context context = ContextUtil.obtainContext(request);
			EPerson ePerson = context.getCurrentUser();
			String name = ePerson != null ? ePerson.getName() : "Null eperson";
			return String.format("TEST %s", name);

		}catch (SQLException e){
			log.error(e);
			return "forward:/error";
		}
	}

	@ExceptionHandler(Exception.class)
	public String handleExceptions(){
		return "forward:/error";
	}
}
