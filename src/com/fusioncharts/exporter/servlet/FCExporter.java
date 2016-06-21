// Version History
// ===============
// 2.0 [ 21 June 2016 ]
// - Support export if direct image base64 encoded data provided (for FusionCharts v 3.11.0 or more)
// - Support for download of xls format
// - Export with images suppported for every format including svg if browser is capable of sending the image data
// 	as base64 data.

package com.fusioncharts.exporter.servlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.ByteArrayInputStream;
import sun.misc.BASE64Decoder;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fusioncharts.exporter.beans.ExportBean;
import com.fusioncharts.exporter.beans.ExportConfiguration;
import com.fusioncharts.exporter.beans.ExportParameterNames;
import com.fusioncharts.exporter.beans.FusionChartsExportData;
import com.fusioncharts.exporter.beans.LogMessageSetVO;
import com.fusioncharts.exporter.error.ErrorHandler;
import com.fusioncharts.exporter.error.LOGMESSAGE;
import com.fusioncharts.exporter.helper.FusionChartsExportHelper;
import com.fusioncharts.exporter.resources.FCExporter_SVG2ALL;

/**
 * Servlet implementation class Exporter
 */
@WebServlet("/Exporter")
public class FCExporter extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private boolean SAVEFOLDEREXISTS = true;

	private static Logger logger = null;
	static {
		logger = Logger.getLogger(FCExporter.class.getName());
	}
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public FCExporter() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Create the bean, set all the properties
		System.out.println("PRINTING PARAMETER");
		//Map<String, String[]> paramMap = request.getParameterMap();
		//System.out.println(paramMap.toString());
                System.out.println(request);

		FusionChartsExportData exportData = new FusionChartsExportData(
                        request.getParameter("stream"), 
                        request.getParameter("stream_type"),
                        request.getParameter("parameters"),
                        request.getParameter("meta_width"), 
                        request.getParameter("meta_height"), 
                        request.getParameter("meta_DOMId"), 
                        request.getParameter("meta_bgColor"), 
                        request.getParameter("encodedImgData"));
		ExportBean exportBean = FusionChartsExportHelper
				.parseExportRequestStream(exportData);               

		String exportTargetWindow = (String) exportBean
				.getExportParameterValue(ExportParameterNames.EXPORTTARGETWINDOW
						.toString());

		String exportFormat = (String) exportBean
				.getExportParameterValue(ExportParameterNames.EXPORTFORMAT
						.toString());

		String exportAction = (String) exportBean
				.getExportParameterValue(ExportParameterNames.EXPORTACTION
						.toString());

		String action = request.getParameter("fcserveraction");

		logger.info("action " + action);
		if (action != null && action.equals("save")) {
			exportAction = "save";
			exportBean.addExportParameter(ExportParameterNames.EXPORTACTION
					.toString(), exportAction);

		}

		// Validate the request parameters in the bean
		LogMessageSetVO logMessageSetVO = exportBean.validate();

		// check if there are any errors in validation
		// if validation is not successful, then output the error
		Set<LOGMESSAGE> errorsSet = logMessageSetVO.getErrorsSet();
		boolean isHTML = exportBean.isHTMLResponse();

		if (errorsSet != null && !errorsSet.isEmpty()) {
			// There are errors - forward to error page
			String meta_values = exportBean.getMetadataAsQueryString(null,
					true, isHTML);
			logMessageSetVO.setOtherMessages(meta_values);
			writeError(response, isHTML, logMessageSetVO, exportTargetWindow);
			return;
		}
		// Else If validation is success, then conduct some more validations and
		// then find the delegate for this exportFormat and delegate the export
		// work to it
		else {
                       
			if (!exportAction.equals("download")) {
				if (!SAVEFOLDEREXISTS) {
					logMessageSetVO.addError(LOGMESSAGE.E508);
					// if server save folder does not exist, then quit writing
					// the error
					String meta_values = exportBean.getMetadataAsQueryString(
							null, true, isHTML);
					logMessageSetVO.setOtherMessages(meta_values);
					writeError(response, isHTML, logMessageSetVO,
							exportTargetWindow);
					return;
				}

				String fileNameWithoutExt = (String) exportBean
						.getExportParameterValue(ExportParameterNames.EXPORTFILENAME
								.toString());
				String extension = FusionChartsExportHelper
						.getExtensionFor(exportFormat.toLowerCase());
				;
				String fileName = fileNameWithoutExt + "." + extension;
				logMessageSetVO = ErrorHandler.checkServerSaveStatus(fileName);
				errorsSet = logMessageSetVO.getErrorsSet();
			}
			if (errorsSet != null && !errorsSet.isEmpty()) {
				// There are errors - forward to error page
				String meta_values = exportBean.getMetadataAsQueryString(null,
						true, isHTML);
				logMessageSetVO.setOtherMessages(meta_values);
				writeError(response, isHTML, logMessageSetVO,
						exportTargetWindow);
				return;
			} 
			else {
                               
				// no errors - call the delegate for the export and
				// delegate the export work to it
				// The delegate will process the request, to create the chart
				// image or pdf and write to the stream directly.

				//create an exporter for converting SVG to ALL formats
				FCExporter_SVG2ALL fcExporter =new FCExporter_SVG2ALL(getServletContext().getRealPath("/"),exportBean);
    
                                ByteArrayOutputStream exportObject;
                                
                                //if stream_type is IMAGE_DATA the it will direct export the output
                                if(exportData.getStream_type().equals("IMAGE-DATA")) 
                                {
                                    String base64ImageData = exportData.getStream().split(",")[1];
                                    byte[] imageByte;
                                    BASE64Decoder decoder = new BASE64Decoder();
                                    imageByte = decoder.decodeBuffer(base64ImageData);

                                    ByteArrayOutputStream bos = new ByteArrayOutputStream(imageByte.length);
                                    bos.write(imageByte, 0, imageByte.length);
                                    exportObject = bos;

                                } else {
                                    //call exportProcessor which processes the SVG stream and returns an image in the form of bytes
                                    exportObject = fcExporter.exportProcessor(response);
                                }
				
                                 
				if(!(exportObject==null))
				{
				//Save the bytes as an image or send them to the browser as download.
				String status = fcExporter.exportOutput(exportObject,
						response);
					if(status==null)
					{
						System.out.println("failure in exporting");
					}
					else
					{
						System.out.println("Exporting successful");
					}
				}
				else
				{
					System.out.println("Error in Export Processor");
				}
			}
		}

	}
	/**
	 * Writes the error.
	 * 
	 * @param response
	 *            HttpServletResponse - to which error has to be written.
	 * @param isHTML
	 *            - whether the response has to be in html format or not.
	 * @param logMessageSetVO
	 *            - LogMessageSetVO - set of errors.
	 * @param exportTargetWindow
	 *            - export parameter specifying the target window.
	 */
	private void writeError(HttpServletResponse response, boolean isHTML,
			LogMessageSetVO logMessageSetVO, String exportTargetWindow) {

		response.setContentType("text/html");
		if (exportTargetWindow != null
				&& exportTargetWindow.equalsIgnoreCase("_self")) {
			response.addHeader("Content-Disposition", "attachment;");
		} else {
			response.addHeader("Content-Disposition", "inline;");
		}
		PrintWriter out;
		try {
			out = response.getWriter();
			out.print(ErrorHandler.buildResponse(logMessageSetVO, isHTML));
		} catch (IOException e) {

		}
	}
	/**
	 * The init of the servlet. Performs all the basic checks and
	 * initializations. Loads the export properties into the bean
	 * ExportConfiguration. Sets the SAVEABSOLUTEPATH equal SAVEPATH to if the
	 * user has given absolute path, else, pre-pends the realpath to SAVEPATH
	 * and sets that. Checks whether save folder exists on the server or not.
	 * 
	 * @see Servlet#init(ServletConfig)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
   
		super.init(config);
		logger.info("FCExporter Servlet Init called");
		// load properties file
		ExportConfiguration.loadProperties();
		File f = new File(ExportConfiguration.SAVEPATH);
		boolean savePathAbsolute = f.isAbsolute();
		logger.info("Is SAVEPATH on server absolute?" + savePathAbsolute);
		String realPath = config.getServletContext().getRealPath(
				ExportConfiguration.SAVEPATH);
		// in some operating systems, from war deployment, getRealPath returns
		// null.
		if (realPath == null) {
			logger.log(Level.SEVERE,
					"For this environment, SAVEPATH should be absolute");
			realPath = "";
		}
		ExportConfiguration.SAVEABSOLUTEPATH = savePathAbsolute ? ExportConfiguration.SAVEPATH
				: realPath;
		// now the properties are loaded into ExportConfiguration
		// check if server folder exists, otherwise log a message
		// The user needs to provide complete path to the save folder
		SAVEFOLDEREXISTS = ErrorHandler.doesServerSaveFolderExist();
		if (!SAVEFOLDEREXISTS)
			logger.warning(LOGMESSAGE.E508.toString() + "Path used: "
					+ ExportConfiguration.SAVEABSOLUTEPATH);
	}
}
