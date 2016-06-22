/**
 * 
 */
package com.fusioncharts.exporter.beans;

import org.json.JSONObject;

/**
 * This bean contains the fields corresponding to the request parameters
 * obtained from FusionCharts chart.
 * 
 * 
 * @author InfoSoft Global (P) Ltd.
 * 
 */


public class FusionChartsExportData {

	protected String stream = null;
        protected String stream_type = null;
	protected String parameters = null;
	protected String meta_width = null;
	protected String meta_height = null;
	protected String meta_DOMId = null;
	protected String meta_bgColor = null;
	protected JSONObject meta_bgImageData = null;

	/**
	 * Constructor from fields.
	 * 
	 * @param stream
	 * @param parameters
	 * @param meta_width
	 * @param meta_height
	 * @param meta_DOMId
	 * @param meta_bgColor
	 */
	public FusionChartsExportData(String stream, String steam_type ,String parameters,
			String meta_width, String meta_height, String meta_DOMId,
			String meta_bgColor, String meta_bgImageData) {
		super();
		this.stream = stream;
                this.stream_type = steam_type;
		this.parameters = parameters;
		this.meta_width = checkW(meta_width);
		this.meta_height = checkH(meta_height);
		this.meta_DOMId = meta_DOMId;
		this.meta_bgColor = meta_bgColor;
         
		if(meta_bgImageData != null){
			this.meta_bgImageData = new JSONObject(meta_bgImageData);
		}
	}

	/**
	 * Returns the value in the field meta_bgColor
	 * 
	 * @return the meta_bgColor
	 */
	public String getMeta_bgColor() {
		return meta_bgColor;
	}

	/**
	 * Returns the value in the field meta_DOMId
	 * 
	 * @return the meta_DOMId
	 */
	public String getMeta_DOMId() {
		return meta_DOMId;
	}

	/**
	 * Returns the value in the field meta_height
	 * 
	 * @return the meta_height
	 */
	public String getMeta_height() {
		return meta_height;
	}

	/**
	 * Returns the value in the field meta_width
	 * 
	 * @return the meta_width
	 */
	public String getMeta_width() {
		return meta_width;
	}

	/**
	 * Returns the value in the field parameters
	 * 
	 * @return the parameters
	 */
	public String getParameters() {
		return parameters;
	}

	/**
	 * Returns the value in the field stream
	 * 
	 * @return the stream
	 */
	public String getStream() {
		return stream;
	}
        
        /**
	 * Returns the value in the field stream_type
	 * 
	 * @return the stream
	 */
	public String getStream_type() {
		return stream_type;
	}
            
	/**
	 * Sets the value for meta_bgColor
	 * 
	 * @param meta_bgColor
	 *            the meta_bgColor to set
	 */
	public void setMeta_bgColor(String meta_bgColor) {
		this.meta_bgColor = meta_bgColor;
	}

	/**
	 * Sets the value for meta_DOMId
	 * 
	 * @param meta_DOMId
	 *            the meta_DOMId to set
	 */
	public void setMeta_DOMId(String meta_DOMId) {
		this.meta_DOMId = meta_DOMId;
	}

	/**
	 * Sets the value for meta_height
	 * 
	 * @param meta_height
	 *            the meta_height to set
	 */
	public void setMeta_height(String meta_height) {
		this.meta_height = meta_height;
	}

	/**
	 * Sets the value for meta_width
	 * 
	 * @param meta_width
	 *            the meta_width to set
	 */
	public void setMeta_width(String meta_width) {
		this.meta_width = meta_width;
	}

	/**
	 * Sets the value for parameters
	 * 
	 * @param parameters
	 *            the parameters to set
	 */
	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	/**
	 * Sets the value for stream
	 * 
	 * @param stream
	 *            the stream to set
	 */
	public void setStream(String stream) {
		this.stream = stream;
	}

	public JSONObject getMeta_bgImageData() {
		return meta_bgImageData;
	}

	public void setMeta_bgImageData(JSONObject meta_bgImageData) {
		this.meta_bgImageData = meta_bgImageData;
	}
        
        
        private String checkW(String w) {
            if(w == null)
                w = "500";
            return w;
        }
        
        private String checkH(String H) {
            if(H == null)
                H = "300";
            return H;
        }
}
