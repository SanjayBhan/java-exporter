package com.fusioncharts.exporter.resources;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import com.fusioncharts.exporter.beans.ChartMetadata;
import com.fusioncharts.exporter.beans.ExportBean;
import com.fusioncharts.exporter.beans.ExportConfiguration;
import com.fusioncharts.exporter.beans.ExportParameterNames;
import com.fusioncharts.exporter.beans.LogMessageSetVO;
import com.fusioncharts.exporter.error.ErrorHandler;
import com.fusioncharts.exporter.error.LOGMESSAGE;
import com.fusioncharts.exporter.error.Status;
import com.fusioncharts.exporter.helper.FusionChartsExportHelper;
import com.fusioncharts.exporter.helper.InputValidator;

public class FCExporter_SVG2ALL 
{
	//contains all the data 
	private ExportBean exportBean = null;
	//for validating the extension
	private InputValidator inputValidator = new InputValidator();
	//receive the output image as bytes
	private ByteArrayOutputStream exportObject;
	//absolute path of the application
	private String appPath=new String();

	/**
	 * Constructor-Sets the path of the application and the export data
	 * @param realPath path of the application
	 * @param exportBean object conatining all information of export
	 */
	public FCExporter_SVG2ALL(String realPath, ExportBean exportBean) {
		this.appPath=realPath;
		this.exportBean=exportBean;
	}

	/**
	 * Create image from SVG stream and send back as bytes
	 * @param response
	 * @return
	 * @throws IOException
	 */
	public ByteArrayOutputStream exportProcessor(HttpServletResponse response) {
		//get extension
		String exportFormat = (String) exportBean
				.getExportParameterValue("exportformat");
		String extension = FusionChartsExportHelper
				.getExtensionFor(exportFormat.toLowerCase());

		//Initialize error set 
		LogMessageSetVO errorSetVO = new LogMessageSetVO();
		String exportTargetWindow = (String) exportBean
				.getExportParameterValue(ExportParameterNames.EXPORTTARGETWINDOW
						.toString());

		//is Download
		boolean isHTML = exportBean.isHTMLResponse();
		//get meta values
		String meta_values = exportBean.getMetadataAsQueryString(null,
				true, isHTML);

		//for JPG first set to PNG
		String extension2=new String();
		if (extension.equals("jpeg") || extension.equals("jpg")) {
			extension = "png";
			extension2 = "jpg";
		}

		//if not SVG
		if (!extension.equals("svg") ) {

			//create temporary directory
			createDirectory();

			//create temporary file Name
			long timeInMills = System.currentTimeMillis();
			String tempName= new String("temp"+timeInMills);
			String tempOutputFileName=new String(appPath+"fusioncharts_temp\\"+tempName+"."+extension);
			String tempOutputJpgFileName=new String(appPath+"fusioncharts_temp\\"+tempName+".jpg");

			//Set Inkscape and ImageMagick path
			String inkscapePath=new String(ExportConfiguration.INKSCAPE_PATH);
			String imageMagickPath=new String(ExportConfiguration.IMAGEMAGICK_PATH);


			//Get metadata from object	
			ChartMetadata metadata = exportBean.getMetadata();

			//Get size from object
			Double width = new Double(metadata.getWidth());
			Double height = new Double(metadata.getHeight());

			//Put the size in string
			String size =new String();
			if (width!=null&&height!=null) {
				size = "-w "+ width +" -h "+ height;
			}

			// override the size in case of pdf output
			if (extension.equals("pdf")) {
				size = "";
			}

			//Put Background color of the chart in string
			String bgcolor = new String();
			bgcolor = metadata.getBgColor();
			if (bgcolor.equalsIgnoreCase(null) || bgcolor.equalsIgnoreCase("") || bgcolor.equalsIgnoreCase(null)) {
				bgcolor = "FFFFFF";
			}
			String bg=new String();
			if (!bgcolor.isEmpty()) {
				bg = " --export-background="+bgcolor;
			}

			//Create temporary SVG file to feed data to Inkscape and ImageMagick
			File svgFile = null;
			try {
				svgFile = File.createTempFile("fusioncahrts", ".svg",new File(appPath+"fusioncharts_temp"));
				//System.out.println("SVG file saved at:"+ svgFile.getAbsolutePath());
				BufferedWriter bw = new BufferedWriter(new FileWriter(svgFile));
				bw.write(exportBean.getStream());
				bw.close();
			} catch (IOException e) {
				errorSetVO.addError(LOGMESSAGE.E518);
				errorSetVO.setOtherMessages(meta_values);
				//e.printStackTrace();
				writeError(response, isHTML, errorSetVO, exportTargetWindow);
				return null;
			}

			//create the command to be fed into Inkscape
			StringBuffer sBuff = new StringBuffer();
			sBuff.append("inkscape "+bg);
			sBuff.append(" --without-gui "+ svgFile);
			sBuff.append(" --export-"+extension+"="+tempOutputFileName);
			sBuff.append(" "+size);
			String cmd=sBuff.toString();
			System.out.println("Command Inkscape :"+cmd);

			//create a process to run inkscape command
			ProcessBuilder builder = new ProcessBuilder(
					"CMD", "/C",cmd);
			builder.redirectErrorStream(true);
			builder.directory(new File(inkscapePath));

			//execute the process
			Process p = null;
			try {
				p = builder.start();
				//print Inkscape message 
				BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line = null;
				while (true) {
					line = r.readLine();
					if (line == null) { break; }
					System.out.println(line);
				}
			} catch (IOException e) {
				errorSetVO.addError(LOGMESSAGE.E519);
				errorSetVO.setOtherMessages(meta_values);
				//e.printStackTrace();
				writeError(response, isHTML, errorSetVO, exportTargetWindow);
				return null;
			}

			//if format is jpg then convert the png to jpg using ImageMagick
			if (extension2.equals("jpg")) {
				//create the command to be fed into Imagemagick
				StringBuffer sBuffJPG=new StringBuffer();
				sBuffJPG.append("convert -quality 100 "+tempOutputFileName+" "+ tempOutputJpgFileName);
				tempOutputFileName=tempOutputJpgFileName;
				String cmdJPG=sBuffJPG.toString();
				System.out.println("Command ImageMagick :"+cmdJPG);

				//create a process to run inkscape command
				ProcessBuilder builderJPG = new ProcessBuilder(
						"CMD", "/C",cmdJPG);
				builderJPG.redirectErrorStream(true);
				builderJPG.directory(new File(imageMagickPath));
				//execute the process
				Process pJPG = null;
				try {
					pJPG = builderJPG.start();
					//print imagemagick message
					BufferedReader r2 = new BufferedReader(new InputStreamReader(pJPG.getInputStream()));
					String line2 = null;
					while (true) {
						line2 = r2.readLine();
						if (line2 == null) { break; }
						System.out.println(line2);
					}
				} catch (IOException e) {
					errorSetVO.addError(LOGMESSAGE.E520);
					errorSetVO.setOtherMessages(meta_values);
					//e.printStackTrace();
					writeError(response, isHTML, errorSetVO, exportTargetWindow);
					return null;
				}
			}

			//get the image file
			File outImgFile = new File(tempOutputFileName);

			//Error if file not created
			if(outImgFile.length()<10)
			{
				errorSetVO.addError(LOGMESSAGE.E521);
			}


			//Put the image file in ByteArrayOutputStream 
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(outImgFile);
			} catch (FileNotFoundException e) {
				errorSetVO.addError(LOGMESSAGE.E521);
				errorSetVO.setOtherMessages(meta_values);
				//e.printStackTrace();
				writeError(response, isHTML, errorSetVO, exportTargetWindow);
				return null;
			}
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			try {
				for (int readNum; (readNum = fis.read(buf)) != -1;) {
					bos.write(buf, 0, readNum);
				}
			} catch (IOException ex) {
				errorSetVO.addError(LOGMESSAGE.E522);
				ex.printStackTrace();
				writeError(response, isHTML, errorSetVO, exportTargetWindow);
				return null;
			}finally{
				try {
					fis.close();
					bos.close();
				} catch (IOException e) {
					System.out.println("error in closing file input or byte input");
					e.printStackTrace();
				}
			}
			//Delete the temporary file s and then the folder itself
			File temp_folder=new File(appPath + "fusioncharts_temp");
			String[]entries = temp_folder.list();
			for(String s: entries){
				File currentFile = new File(temp_folder.getPath(),s);
				currentFile.delete();
			}
			temp_folder.delete();

			//put bytes in export object
			exportObject=bos;
		} 
		//if exportFormat is SVG send bytes directly
		else if (extension.equals("svg")) {
			byte[] buf = exportBean.getStream().getBytes();
			ByteArrayOutputStream bos = new ByteArrayOutputStream(buf.length);
			bos.write(buf, 0, buf.length);
			exportObject=bos;
		} 
		//format not supported raise error
		else
		{
			//Format not supported
			errorSetVO.addError(LOGMESSAGE.E517);
			errorSetVO.setOtherMessages(meta_values);
			writeError(response, isHTML, errorSetVO, exportTargetWindow);
			return null;
		}
		//if all is well return the exportObject
		return exportObject;
	}

	/**
	 * Receives the bytes and sends response 
	 * @param exportObject
	 * @param response
	 * @return
	 */
	public String exportOutput( ByteArrayOutputStream exportObject, HttpServletResponse response) {

		String action = (String) exportBean
				.getExportParameterValue("exportaction");
		String exportFormat = (String) exportBean
				.getExportParameterValue("exportformat");
		String exportTargetWindow = (String) exportBean
				.getExportParameterValue("exporttargetwindow");

		String fileNameWithoutExt = (String) exportBean
				.getExportParameterValue("exportfilename");
		String extension = FusionChartsExportHelper
				.getExtensionFor(exportFormat.toLowerCase());
		;

		fileNameWithoutExt = inputValidator.checkForMaliciousCharaters(fileNameWithoutExt);

		String fileName = fileNameWithoutExt + "." + extension;		
		boolean isHTML = false;
		if (action.equals("download"))
			isHTML = true;

		LogMessageSetVO logMessageSetVO = new LogMessageSetVO();
		String noticeMessage = "";
		String meta_values = exportBean.getMetadataAsQueryString(null, false,
				isHTML);
		//save
		if (!action.equals("download")) {
			noticeMessage = "&notice=";
			//get path
			String pathToSaveFolder = ExportConfiguration.SAVEABSOLUTEPATH;

			String completeFilePath = pathToSaveFolder + File.separator
					+ fileName;
			String completeFilePathWithoutExt = pathToSaveFolder
					+ File.separator + fileNameWithoutExt;
			File saveFile = new File(completeFilePath);

			//if file already exists add suffix if overwriting is false
			if (saveFile.exists()) {
				noticeMessage += LOGMESSAGE.W509;
				if (!ExportConfiguration.OVERWRITEFILE) {
					if (ExportConfiguration.INTELLIGENTFILENAMING) {
						noticeMessage += LOGMESSAGE.W514;
						completeFilePath = FusionChartsExportHelper
								.getUniqueFileName(completeFilePathWithoutExt,
										extension);
						File tempFile = new File(completeFilePath);
						fileName = tempFile.getName();
						noticeMessage += LOGMESSAGE.W515 + fileName;
						logMessageSetVO.addWarning(LOGMESSAGE.W515);
					}
				}
			}
			
			String pathToDisplay = ExportConfiguration.HTTP_URI + "/"
					+ fileName;
			if (ExportConfiguration.HTTP_URI.endsWith("/")) {
				pathToDisplay = ExportConfiguration.HTTP_URI + fileName;
			}
			
			//System.out.println("complete file path :"+completeFilePath);
			
			//write to file
			OutputStream outputStream = null;
			try {
				outputStream = new FileOutputStream (completeFilePath);
				exportObject.writeTo(outputStream);
				outputStream.close();	
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				System.out.println("File not found!!");
				return null;
			} catch (IOException e) {
				System.out.println("Error in writing to file!!");
				e.printStackTrace();
				return null;
			}
			
			meta_values = exportBean.getMetadataAsQueryString(pathToDisplay,
					false, isHTML);
			if (logMessageSetVO.getErrorsSet() == null
					|| logMessageSetVO.getErrorsSet().isEmpty()) {
				// if there are no errors
				PrintWriter out;
				try {
					out = response.getWriter();
					out.print(meta_values + noticeMessage + "&statusCode="
							+ Status.SUCCESS.getCode() + "&statusMessage="
							+ Status.SUCCESS);
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		//download
		else{

			// modifies response
			response.setContentType(FusionChartsExportHelper
					.getMimeTypeFor(exportFormat.toLowerCase()));

			if (exportTargetWindow.equalsIgnoreCase("_self")) {
				response.addHeader("Content-Disposition",
						"attachment; filename=\"" + fileName + "\"");
			} else {
				response.addHeader("Content-Disposition",
						"inline; filename=\"" + fileName + "\"");
			}

			// obtains response's output stream
			OutputStream outStream;
			byte[] bytes = exportObject.toByteArray();
			response.setContentLength((int)  bytes.length);
			try {
				outStream = response.getOutputStream();
				outStream.write(bytes);
				outStream.close();  
			} catch (Throwable e) {
				return null;
			}
		}
		return "success";
	}

	/**
	 * Create a temporary directory
	 */
	private void createDirectory() {
		File f = new File(appPath + "fusioncharts_temp");
		if (!f.exists()) {
			f.mkdir();
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
}
