<?xml version="1.0" encoding="UTF-8" ?>
<%@ page import="orange.olps.svi.util.Util" %>
<%
response.setHeader("Cache-Control","max-age="+Util.getHttpCacheControlMaxAge());
%>
<vxml xmlns="http://www.w3.org/2001/vxml" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.w3.org/2001/vxml http://www.w3.org/TR/voicexml20/vxml.xsd" version="2.0">

<property name="documentmaxage" value="<%=Util.getDocumentMaxAge() %>"/>
<property name="documentmaxstale" value="0"/>
<property name="audiomaxage" value="<%=Util.getAudioMaxAge() %>"/>
<property name="audiomaxstale" value="0"/>
<property name="fetchtimeout" value="<%=Util.getFetchtimeout() %>"/>
</vxml>
