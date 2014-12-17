<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://www.fusioncharts.com/jsp/core" prefix="fc"%>
<c:set var="folderPath" value="../../FusionCharts/" />
<c:set var="jsPath" value="${folderPath}" scope="request" />
<script src="${jsPath}fusioncharts.js"></script>
<div>
	<fc:render chartId="ColumnId" swfFilename="column2d" width="400" height="400"
		debugMode="false" registerWithJS="false" xmlUrl="Data/Data.xml"
		renderer="javascript" windowMode="transparent" />
</div>