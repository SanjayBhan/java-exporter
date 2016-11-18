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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;


import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import javax.imageio.ImageIO;
import java.awt.Color;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

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
import com.fusioncharts.exporter.servlet.FCExporter;

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
	        
	private static Logger logger = null;
	static {
		logger = Logger.getLogger(FCExporter_SVG2ALL.class.getName());
	}

	/**
	 * Constructor-Sets the path of the application and the export data
	 * @param realPath path of the application
	 * @param exportBean object containing all information of export
	 */
	public FCExporter_SVG2ALL(String realPath, ExportBean exportBean) {
		this.appPath=realPath + "/";
		this.exportBean=exportBean;
                //loading configuaration file
                ExportConfiguration.loadProperties();
	}

	/**
	 * Create image from SVG stream and send back as bytes
	 * @param response
	 * @return
	 * @throws IOException
	 */
	public ByteArrayOutputStream exportProcessor(HttpServletResponse response) {
		//get OS name
		String OS=(System.getProperty("os.name"));
		System.out.println(OS);

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
		
		// Background image meta data
		JSONObject bgImgMeta = this.exportBean.getMetadata().getBgImage(); 
		
		
		//for JPG first set to PNG
		String extension2=new String();
		if (extension.equals("jpeg") || extension.equals("jpg")) {
			extension = "png";
			extension2 = "jpg";
                        
		}else if (extension.equals("pdf")) {
			extension = "png";
			extension2 = "pdf";
		}

		//if not SVG
		if (!extension.equals("svg") ) {

			//create temporary directory
			createDirectory("", "fusioncharts_temp");
			createDirectory("fusioncharts_temp/", "temp");
			
			//create temporary file Name
			long timeInMills = System.currentTimeMillis();
			String tempName= new String("temp"+timeInMills);
			String tempOutputFileName = null,
                               tempOutputJpgPdfFileName = null,
                               tempPngFIleName = null;
			if(OS.startsWith("Windows"))
			{
				tempOutputFileName=new String(appPath+"fusioncharts_temp/"+tempName+"."+extension);
				tempOutputJpgPdfFileName=new String(appPath+"fusioncharts_temp/"+tempName+(extension2.equals("jpg") ? ".jpg" : ".pdf"));
			}
			else if(OS.startsWith("Linux"))
			{
				tempOutputFileName=new String(appPath+"fusioncharts_temp/"+tempName+"."+extension);
				tempOutputJpgPdfFileName=new String(appPath+"fusioncharts_temp/"+tempName+(extension2.equals("jpg") ? ".jpg" : ".pdf"));
			}
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
			File tempBgImgFile = null;
                                  
                    try {
                            if(exportBean.getStreamType().equals("bmp")){
                                String bstr = exportBean.getStream();
                                BufferedImage img = map(600, 350, bstr);
                                ImageIO.write(img, "PNG", new File(tempOutputFileName));

                            } else {
                                
				svgFile = File.createTempFile("fusioncahrts", ".svg",new File(appPath+"fusioncharts_temp"));
				System.out.println("SVG file saved at:"+ svgFile.getAbsolutePath());
				
				String svgStr = exportBean.getStream();
                                
				if(bgImgMeta != null){
					String encodingPrefix = "base64,";
					Iterator<String> imageItr = bgImgMeta.keys();
					String imgKey = null;
					while(imageItr.hasNext()){
						imgKey = imageItr.next();
						String dataURL = (String)((JSONObject)bgImgMeta.get(imgKey)).get("encodedData"); 
						String imgName = (String)((JSONObject)bgImgMeta.get(imgKey)).get("name");
						String imgExt = (String)((JSONObject)bgImgMeta.get(imgKey)).get("type");
						
						int contentStartIndex = dataURL.indexOf(encodingPrefix) + encodingPrefix.length();
						String bgImgEncoded = dataURL.substring(contentStartIndex);
						
						tempBgImgFile = createOrOverrideFile(appPath+"fusioncharts_temp/temp/" + imgName + "." + imgExt);
						
						if(tempBgImgFile != null){
							OutputStream _imgStream = new FileOutputStream(tempBgImgFile);
							if(_imgStream != null){
								_imgStream.write(Base64.decodeBase64(bgImgEncoded.getBytes()));
								_imgStream.close();
							}
						}
                                                
                                                String fullImgName = imgName + "." + imgExt;
                                                String svgImgRef = "temp/"+fullImgName;

                                                while(svgStr.indexOf(svgImgRef) != -1){
                                                        svgStr = svgStr.replaceAll(svgImgRef, dataURL);
                                                }
					}
                                        
				}
                                
                                BufferedWriter bw = new BufferedWriter(new FileWriter(svgFile));
                                bw.write(svgStr);
                                bw.close();
                            }       
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

                        //setting file name with path of png file
                        tempPngFIleName = tempOutputFileName;
                        
			//create a process to run Inkscape command in Windows
			if(OS.startsWith("Windows") && exportBean.getStreamType().equals("svg"))
			{
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
			}
			//create a process to run inkscape command in Linux
			else if(OS.startsWith("Linux") && exportBean.getStreamType().equals("svg"))
			{
				Process p = null;
				try {
					p = Runtime.getRuntime().exec(cmd);
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
			}
			//if format is jpg then convert the png to jpg using ImageMagick
			if (extension2.equals("jpg") || extension2.equals("pdf")) {
				//create the command to be fed into Imagemagick
				StringBuffer sBuffJPG=new StringBuffer();
                                //setting file name with path of png file
                                tempPngFIleName = tempOutputFileName;
				sBuffJPG.append("convert -quality 100 "+tempOutputFileName+" "+ tempOutputJpgPdfFileName);
                                tempOutputFileName=tempOutputJpgPdfFileName;
				String cmdJPG=sBuffJPG.toString();
				System.out.println("Command ImageMagick :"+cmdJPG);

				//create a process to run Imagemagick command
				if(OS.startsWith("Windows"))
				{
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
				else if(OS.startsWith("Linux"))
				{
					Process pJPG = null;
					try {
						pJPG = Runtime.getRuntime().exec(cmdJPG);
						//print Inkscape message 
						BufferedReader r = new BufferedReader(new InputStreamReader(pJPG.getInputStream()));
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
				}
			}

			//************************************IMAGE CREATED**************************************************//
			//get the image file
			File outImgFile = new File(tempOutputFileName);

			//Error if file not created
			if(outImgFile.length()<10)
			{
				errorSetVO.addError(LOGMESSAGE.E521);
			}
			
			//***************-CONVERSION FROM IMG to BYTE STREAM-***************************//
			
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
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();// img written in bos
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
                        
			//Delete the temporary files
                        if(ExportConfiguration.DELETETEMPFILES){
                            if(svgFile != null)
                                deleteFilesInFolder(svgFile.getAbsolutePath());
                            if(tempOutputFileName != null)
                                deleteFilesInFolder(tempOutputFileName);
                            if(tempPngFIleName != null)
                                deleteFilesInFolder(tempPngFIleName);
                        }
			//put bytes in export object
			exportObject=bos;
		} 
		//if exportFormat is SVG send bytes directly
		else if (extension.equals("svg")) {
			String imgKey = null;
			String svgStr=exportBean.getStream();
			
			if(bgImgMeta != null){
				Iterator<String> imageItr = bgImgMeta.keys();
				while(imageItr.hasNext()){
					imgKey = imageItr.next();
					
					String dataURL = (String)((JSONObject)bgImgMeta.get(imgKey)).get("encodedData");
					String imgName = (String)((JSONObject)bgImgMeta.get(imgKey)).get("name");
					String imgExt = (String)((JSONObject)bgImgMeta.get(imgKey)).get("type");
					String fullImgName = imgName + "." + imgExt;
					String svgImgRef = "temp/"+fullImgName;
					
					while(svgStr.indexOf(svgImgRef) != -1){
						svgStr = svgStr.replaceAll(svgImgRef, dataURL);
					}
					
				}
			}
			

			// dind image tag in svg 
			byte[] buf = svgStr.getBytes();
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
	private void createDirectory(String baseprefix, String dirName) {
		File f = new File(appPath + baseprefix + dirName);
		if (!f.exists()) {
			f.mkdir();
		}
	}
	
	private File createOrOverrideFile(String filePath) {
		File f = new File(filePath);
		if (!f.exists() || f.isDirectory()) {
			try {
				f.createNewFile();
				return f;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	private void deleteFilesInFolder(String folderPath) {
            //if any file or folder location came null
            if(folderPath == null)
                return;
		
            File temp_folder=new File(folderPath);	
            if(temp_folder.isDirectory()){
                String[]entries = temp_folder.list();
                for(String s: entries){
                    File currentFile = new File(temp_folder.getPath(),s);
                    currentFile.delete();
                }
                temp_folder.delete();
            } else {
                temp_folder.delete();
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
        
        public static Color hex2Rgb(String colorStr) {
            if(colorStr.length() == 6)
                return new Color(
                    Integer.valueOf( colorStr.substring( 0, 2 ), 16 ),
                    Integer.valueOf( colorStr.substring( 2, 4 ), 16 ),
                    Integer.valueOf( colorStr.substring( 4, 6 ), 16 ));
            else 
                return new Color(
                    Integer.valueOf( colorStr.substring( 0, 1 ) + colorStr.substring( 0, 1 ), 16 ),
                    Integer.valueOf( colorStr.substring( 1, 2 ) + colorStr.substring( 1, 2 ), 16 ),
                    Integer.valueOf( colorStr.substring( 2, 3 ) + colorStr.substring( 2, 3 ), 16 ));
        }
        
        private static BufferedImage map( int sizeX, int sizeY, String bstr ){
            final BufferedImage res = new BufferedImage( sizeX, sizeY, BufferedImage.TYPE_INT_RGB );
            int x=0, y=0;
            
            for (String retval: bstr.split(";")) {

                y=0;
                for(String retval1: retval.split(",")){
                    String[] code = retval1.split("_");
 
                    for(int x1 = 0; x1 < Integer.parseInt(code[1]) ; x1++){

                        if(code[0].length() == 0)
                            res.setRGB(y, x, hex2Rgb("ffffff").getRGB());    
                        else if(code[0].length() > 2)
                            res.setRGB(y, x, hex2Rgb(code[0]).getRGB());
                            
                        y++;
                    }
                }
                
                x++;
            }
            return res;
        }
}
