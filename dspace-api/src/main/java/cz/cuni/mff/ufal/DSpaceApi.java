/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.*;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;

import cz.cuni.mff.ufal.dspace.AbstractPIDService;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.handle.HandlePlugin;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

import cz.cuni.mff.ufal.dspace.PIDService;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.UserMetadata;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.UserRegistration;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.VerificationTokenEperson;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

/*
 * This class is an extension of the DSpace source code. Initial version is 1.6.2
 * 
 * 2013/11/27 - this is also the tunnel between utilities database and DSpace e.g., VerificationTokenEperson 
 */
public class DSpaceApi {

	private static final Logger log = cz.cuni.mff.ufal.Logger.getLogger(DSpaceApi.class);

	public static IFunctionalities getFunctionalityManager() {

		IFunctionalities manager = null;

		String className = ConfigurationManager.getProperty(
				"lr", "lr.utilities.functionalityManager.class"); // cz.cuni.mff.ufal.lindat.utilities.HibernateFunctionalityManager

		try {

			log.debug("Loading class : " + className);

			@SuppressWarnings("unchecked")
			Class<IFunctionalities> functionalities = (Class<IFunctionalities>) Class.forName(className);
			Constructor<IFunctionalities> constructor = functionalities.getConstructor(String.class);
			ConfigurationService configurationService = new DSpace().getConfigurationService();
			String lr_cfg = configurationService.getProperty("dspace.dir") + File.separator + "config/modules/lr.cfg";
			manager = constructor.newInstance(lr_cfg);

			log.debug("Class " + className + " loaded successfully");
			
		} catch (Exception e) {
			log.error("Failed to initialize the IFunctionalities", e);
		}
		
		return manager;		
	}

	public static boolean authorizeBitstream(Context context,
			DSpaceObject dspaceObject) throws AuthorizeException {

		EPerson currentUser = context.getCurrentUser();
		boolean userExists = currentUser != null;

		int userID = 0; // user not logged in

		if (userExists) {
			userID = currentUser.getID();
		}

		// This manager is applicable just for bitstreams for the moment :-)
		if (dspaceObject.getType() == Constants.BITSTREAM) {

			int resourceID = dspaceObject.getID();
			
            try {
                Item item = (Item) dspaceObject.getParentObject();
                EPerson submitter = item.getSubmitter();
                if (submitter != null) {
                    // The submitter is always authorized to access his own
                    // bitstreams
                    if (userID != 0 && submitter.getID() == userID) {
                        return true;
                    }
                }
            } catch (SQLException sqle) {
                log.error("Failed to get parent object for bitstream", sqle);
            } catch (ClassCastException ex) {
            	// parent object is not an Item
            	// special bitstreams e.g. images of community/collection
            }
			
			DSpace dspace = new DSpace();
			//HttpSession session = null;
			ServletRequest request = null;
			String dtoken = null;
			try{
				//session = dspace.getSessionService().getCurrentSession();
				request = dspace.getRequestService().getCurrentRequest().getHttpServletRequest();
				dtoken = request.getParameter("dtoken");
			}catch(IllegalStateException e){
				//If the dspace kernel is null (eg. when we get here from OAI)
			}catch(Exception e){
			}


			
			IFunctionalities manager = DSpaceApi.getFunctionalityManager();			

			// if the bitstream is already authorized for current session
			
			// We are not using this at the moment
            /*

			if(session != null){
				String authorizedBitstreams = (String) session.getAttribute("AuthorizedBitstreams");
				ArrayList<String> authorizeBitstreamsList = null;
				if (authorizedBitstreams != null) {
					authorizeBitstreamsList = new ArrayList<String>(
							Arrays.asList(authorizedBitstreams.split("\\.")));
				}
				if (authorizeBitstreamsList != null && authorizeBitstreamsList.contains("" + resourceID)) {
					manager.close();
					return true;
				}
			} */

			if (dtoken != null && !dtoken.isEmpty()) {		
				manager.openSession();
				boolean tokenFound = manager.verifyToken(resourceID, dtoken);				
				manager.close();
	            // Check token				
			    if(tokenFound) { // database token match with url token 
			        return true;
			    } else {
			    	throw new AuthorizeException("The download token is invalid or expires.");
			    }
			    
			}			

			// Check licenses
			manager.openSession();
			boolean allowed = manager.isUserAllowedToAccessTheResource(userID, resourceID);
			manager.close();					
		    if (!allowed) {
		        throw new MissingLicenseAgreementException("Missing license agreement!");
		    }
 
		}
		return true;
	}
	
	public static void updateFileDownloadStatistics(int userID, int resourceID) {	   
	    IFunctionalities manager = DSpaceApi.getFunctionalityManager();	    
        manager.openSession();
		manager.updateFileDownloadStatistics(userID, resourceID);
        manager.close();
	}	    		

	public static boolean registerUser(String organization, EPerson eperson) {
		IFunctionalities manager = DSpaceApi.getFunctionalityManager();
		manager.openSession();
		UserRegistration user = manager.registerUser(eperson.getID(), eperson.getEmail(), organization, true);
		manager.close();
		return user != null;
	}
	
	/**
	 * Add information to a user, this will be stored in utilities 
	 * user_metadata table under eperson_id.
	 * 
	 * @param eperson
	 * @param metadata
	 * @return
	 */
	public static boolean addUserMetadata( EPerson eperson, String key, String value ) 
	{
		IFunctionalities metadataUtil = DSpaceApi.getFunctionalityManager();
		metadataUtil.openSession();
		boolean result = metadataUtil.addUserMetadata(eperson.getID(), key, value);
		metadataUtil.close();
		return result;				
	}
	
	public static boolean addUserMetadata(EPerson eperson, Map<String, String> metadata ) 
	{		
        IFunctionalities metadataUtil = DSpaceApi.getFunctionalityManager();
		try{
			metadataUtil.openSession();
			for ( Map.Entry<String, String> m : metadata.entrySet() ) 
			{
				metadataUtil.addUserMetadata(eperson.getID(), m.getKey(), m.getValue());
			}
			metadataUtil.close();
            return true;
		}catch(Exception e){
			log.error("Error while adding user metadata", e);
			metadataUtil.close();
			return false;
		}
	}

	public static Map<String, String> getUserMetadata( EPerson eperson ) 
	{
		Map<String, String> metadata_map = new HashMap<String, String> ();		
        IFunctionalities metadataUtil = DSpaceApi.getFunctionalityManager();
		try{
			metadataUtil.openSession();
			List<UserMetadata> metadatas = metadataUtil.getUserMetadata(eperson.getID());			
			for ( UserMetadata me : metadatas ) {
				metadata_map.put( me.getMetadataKey(), me.getMetadataValue() );
			}
			metadataUtil.close();
		}catch(Exception e){
			log.error("Error while querying for user metadata from utilities MetadataEperson", e);
			metadataUtil.close();
			return null;
		}		
		return metadata_map;
	}

	public static void main(String[] t) {
		try {
			handle_HandleManager_createId(null, 9999, "11372/LRT-", "TEST-1");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Create a new handle PID. This is modified implementation for UFAL, using
	 * the PID service pidconsortium.eu as wrapped in the PIDService class.
	 * 
	 * Note: this function creates a handle to a provisional existing URL and
	 * the handle must be updated to point to the final URL once DSpace is able
	 * to report the URL exists (otherwise the pidservice will refuse to set the
	 * URL)
	 * 
	 * @return A new handle PID
	 * @exception Exception
	 *                If error occurrs
	 */
	public static String handle_HandleManager_createId(Logger log, int id, String prefix, String suffix) throws IOException {
		
		/* Modified by PP for use pidconsortium.eu at UFAL/CLARIN */
		
		String base_url = ConfigurationManager.getProperty("dspace.url") + "?dummy=" + id;
		
		/* OK check whether this url has not received pid earlier */
			//This should usually return null (404)
			String handle = null;
			try {
				handle = PIDService.findHandle(base_url, prefix);
			} catch(Exception e) {
				log.error("Error finding handle: " + e);
			}
			//if not then log and reuse - this is a dummy url, those should not be seen anywhere
			if(handle != null){
				log.warn("Url [" + base_url + "] already has PID(s) ("+handle+").");
				return handle;
			}
		/* /OK/ */
		
		log.debug("Asking for a new PID using a dummy URL " + base_url);

		/* request a new PID, initially pointing to dspace base_uri+id */
		String pid = null;
		try {
		    if(suffix != null && !suffix.isEmpty() && PIDService.supportsCustomPIDs()) {
		        pid = PIDService.createCustomPID(base_url, prefix, suffix);
		    }
		    else {
		        pid = PIDService.createPID(base_url, prefix);
		    }
		}catch(Exception e) {
			throw new IOException(e);
		}

		log.debug("got PID " + pid);
		return pid;
	}

	/**
	 * Modify an existing PID to point to the corresponding DSpace handle
	 * 
	 * @exception SQLException
	 *                If a database error occurs
	 */
	public static void handle_HandleManager_registerFinalHandleURL(Logger log,
			String pid, DSpaceObject dso) throws IOException {
		if (pid == null) {
			log.info("Modification failed invalid/null PID.");
			return;
		}
		
		String url = ConfigurationManager.getProperty("dspace.url");
		url = url + (url.endsWith("/") ? "" : "/") + "handle/" + pid;
		
		/*
		 * request modification of the PID to point to the correct URL, which
		 * itself should contain the PID as a substring
		 */
		log.debug("Asking for changing the PID '" + pid + "' to " + url);
		
		try {
			Map<String, String> fields = HandlePlugin.extractMetadata(dso);
			PIDService.modifyPID(pid, url, fields);
		} catch (Exception e) {
			throw new IOException("Failed to map PID " + pid + " to " + url
				+ " (" + e.toString() + ")");
		}
		
	}
	
	/**
	 * Return the eperson assigned to that token and sets the required email
	 * @param context
	 * @param token
	 * @return
	 */
	public static EPerson getEPersonByToken(Context context, String token){
		EPerson e = null;
		int eid = 0;
		String email = null;
		String organization = null;
		IFunctionalities manager = DSpaceApi.getFunctionalityManager();
		manager.openSession();
		try{
			final VerificationTokenEperson vte = manager.getVerificationToken(token);
			eid = vte.getEid();
			email = vte.getEmail();
			e = EPerson.find(context, eid);
			context.turnOffAuthorisationSystem();
			e.setEmail(email);
			organization = e.getNetid().replaceAll(".*(\\[.*\\])", "$1");
			manager.registerUser(eid, email, organization, true);
			e.setCanLogIn(false); //Don't know why, but taking from ShibAuthentication
			e.update();
			context.commit();			
			context.restoreAuthSystemState();
			manager.close();
		}
		catch(AuthorizeException ae){
			log.error("Error while filling the email", ae);
			log.info(String.format("token = %s, eid = %d, email = %s, organization = %s", token, eid, email, organization));
			e = null;
		}
		catch(SQLException sqle){
			log.error("Error while filling the email", sqle);
			log.info(String.format("token = %s, eid = %d, email = %s, organization = %s", token, eid, email, organization));
			e = null;
		}
		catch(RuntimeException ex){
			log.error("Error while filling the email", ex);
			log.info(String.format("token = %s, eid = %d, email = %s, organization = %s", token, eid, email, organization));
			e = null;
		}
		finally {
			manager.close();
		}
		return e;
	}
	
	public static boolean deleteToken(String token){
		IFunctionalities manager = DSpaceApi.getFunctionalityManager();
		try{
			manager.openSession();
			VerificationTokenEperson vte = manager.getVerificationToken(token);			
			manager.delete(VerificationTokenEperson.class, vte);
			manager.close();
			return true;
		}catch(RuntimeException e){
			manager.close();
			return false;
		}
	}
	
	public static boolean addToken(String token, int epersonID, String email){	
		IFunctionalities manager = DSpaceApi.getFunctionalityManager();
		try{
			VerificationTokenEperson vte = new VerificationTokenEperson(token, epersonID, email);			
			manager.openSession();
			manager.persist(VerificationTokenEperson.class, vte);
			manager.close();
			return true;
		}catch(RuntimeException e) {
			manager.close();
			return false;
		}		
	}
	
	public static boolean sendRegistrationInfo(Context context, String email, int eid) throws IOException, MessagingException{
		IFunctionalities manager = DSpaceApi.getFunctionalityManager();
		manager.openSession();
		boolean exists = false;
		String token = "";
		VerificationTokenEperson vte = null;
		try{
			vte = manager.getVerificationTokenByEmail(email);
			if(vte != null){
				exists = true;
			}
		}catch(RuntimeException e){
			manager.close();
			return false;
		}

        // If it already exists, send it again
        if (!exists)
        {
            token = Utils.generateHexKey();
        	DSpaceApi.addToken(token,eid, email);
        }
        else{
        	token = vte.getToken();
        }

        String base = ConfigurationManager.getProperty("dspace.url");

        String specialLink = new StringBuffer().append(base).append(
                base.endsWith("/") ? "" : "/").append(
                "set-email").append("?")
                .append("token=").append(token)
                .toString();
        Locale locale = context.getCurrentLocale();
        Email bean = Email.getEmail(I18nUtil.getEmailFilename(locale, "register"));
        bean.addRecipient(email);
        bean.addArgument(specialLink);
        bean.send();
        manager.close();
		return true;
	}

    public static String convertBytesToHumanReadableForm(long bytes) {
    	int exp = (int)(Math.log(bytes) / Math.log(1024));
    	String units[] = new String[]{"bytes", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
    	double val = bytes / Math.pow(1024, exp);
    	if(val == (int)val)
    		return String.format("%.0f %s", val, units[exp]);
    	else
    		return String.format("%.2f %s", val, units[exp]);
    }
    
}

