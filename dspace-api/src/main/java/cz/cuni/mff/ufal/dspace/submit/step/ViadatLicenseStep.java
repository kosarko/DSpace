/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.submit.step;

import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.submit.AbstractProcessingStep;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * modified for LINDAT/CLARIN
 */
public class ViadatLicenseStep extends org.dspace.submit.step.LicenseStep {

	/** log4j logger */
	private static Logger log = Logger.getLogger(ViadatLicenseStep.class);

	@Override
	public int doProcessing(Context context, HttpServletRequest request,
			HttpServletResponse response, SubmissionInfo subInfo)
			throws ServletException, IOException, SQLException,
			AuthorizeException {

				IFunctionalities iface = cz.cuni.mff.ufal.DSpaceApi.getFunctionalityManager();

				// Add the license to all bitstreams(files)
				Item item = subInfo.getSubmissionItem().getItem();

				//After the review step there can be the granted license..
				boolean file_uploaded = (0 < item.getBundles("ORIGINAL").length) && (0 < item.getBundles("ORIGINAL")[0].getBitstreams().length); 
				if ( file_uploaded ) {
					String selected_license = "https://ufal.mff.cuni.cz/grants/viadat/license";
					iface.openSession();
					final LicenseDefinition license_def = iface.getLicenseByDefinition(selected_license);


					for (Bundle bundle : item.getBundles()) {
						Bitstream[] bitstreams = bundle.getBitstreams();
						for (Bitstream bitstream : bitstreams) {
							// first remove all old ones and then add new ones
							int resource_id = bitstream.getID();
							iface.detachLicenses(resource_id);
                            iface.attachLicense(license_def.getLicenseId(), resource_id);
						}
					}
					

                    String license_uri = license_def.getDefinition();
                    String license_label = license_def.getLicenseLabel().getLabel();
                    String license_name = license_def.getName();

					item.clearMetadata("dc", "rights", "holder", Item.ANY);
					item.clearMetadata("dc", "rights", "uri", Item.ANY);
					item.clearMetadata("dc", "rights", null, Item.ANY);
					
	            	item.clearMetadata("dc", "rights", "label", Item.ANY);
					
					item.addMetadata(MetadataSchema.DC_SCHEMA, "rights", "uri",
							Item.ANY, license_uri);
					item.addMetadata(MetadataSchema.DC_SCHEMA, "rights", null,
							Item.ANY, license_name);
					
	            	item.addMetadata("dc", "rights", "label", Item.ANY, license_label);
	
					// Save changes to database
					item.update();
					
		            // For Default License between user and repo
		            EPerson submitter = context.getCurrentUser();

		            // remove any existing DSpace license (just in case the user
		            // accepted it previously)
		            item.removeDSpaceLicense();

		            // For Default License between user and repo					
		            String license = LicenseUtils.getLicenseText(context
		                    .getCurrentLocale(), subInfo.getSubmissionItem()
		                    .getCollection(), item, submitter);

		            LicenseUtils.grantLicense(context, item, license);
	
					// commit changes
					context.commit();
					iface.close();
				}
		return STATUS_COMPLETE;
	}

	// add another page
	@Override
	public int getNumberOfPages(HttpServletRequest request,
			SubmissionInfo subInfo) throws ServletException {
		return super.getNumberOfPages(request, subInfo);
	}

	public static String getUserId(EPerson submitter) {
		return submitter.getEmail();
	}

}



