package org.dspace.springmvc;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.submission.submit.UploadStep;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.google.api.Google;
import org.springframework.social.google.api.drive.DriveFile;
import org.springframework.social.google.api.drive.DriveFilesPage;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
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
	@RequestMapping(value="/files", method=GET)
	public DriveFilesPage getTaskLists(@RequestParam(required=false) String pageToken) {
        try {
		    return google.driveOperations().getRootFiles(pageToken);
        }catch(Exception e){
            return new DriveFilesPage();
        }
	}

	@ResponseBody
	@RequestMapping(value="/files/{dirId}", method=GET)
	public DriveFilesPage getFilesList(@PathVariable String dirId, @RequestParam(required=false) String pageToken) {
		return google.driveOperations().getFiles(dirId, pageToken);
	}

	@ResponseBody
	@RequestMapping(value="/files/{id}", method=GET)
	public void getFile(HttpServletRequest request, HttpServletResponse response, @PathVariable String id) throws IOException, SQLException, AuthorizeException {
		DriveFile driveFile = google.driveOperations().getFile(id);
		InputStream is = google.driveOperations().downloadFile(driveFile).getInputStream();
		int item_id = (Integer)request.getSession().getAttribute(UploadStep.ITEM_ID);
		Context context = ContextUtil.obtainContext(request);
		Item item = Item.find(context, item_id);
		Bundle[] bundles = item.getBundles("ORIGINAL");
		Bitstream bitstream;
		if (bundles.length > 0)
		{
		    bitstream = bundles[0].createBitstream(is);
		}else{

		    bitstream = item.createSingleBitstream(is);
		}
		bitstream.setName(driveFile.getTitle());
		if(driveFile.getDescription() != null){
			bitstream.setDescription(driveFile.getDescription());
		}
		bitstream.setFormat(BitstreamFormat.findByMIMEType(context, driveFile.getMimeType()));
		bitstream.update();
		item.update();
		String url = (String) request.getSession().getAttribute(UploadStep.RETURN_TO);
		response.sendRedirect(url);
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
