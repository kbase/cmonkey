package us.kbase.cmonkey;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import us.kbase.auth.AuthToken;
import us.kbase.auth.TokenFormatException;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.expressionservices.ExpressionSeries;
import us.kbase.userandjobstate.UserAndJobStateClient;

public class CmonkeyServerCaller {

	private static final String JOB_SERVICE = "http://140.221.84.180:7083";
	private static UserAndJobStateClient _jobClient = null;

	private static final String AWE_SERVICE = "http://140.221.85.171:7080/job";
	private static Integer connectionReadTimeOut = 30 * 60 * 1000;
	private static boolean deployCluster = false;

/*	public static CmonkeyRunResult buildCmonkeyNetwork(
			ExpressionSeries series, CmonkeyRunParameters params,
			String jobId, AuthToken authPart) throws Exception {
		CmonkeyRunResult returnVal = CmonkeyServerImpl.buildCmonkeyNetwork(
				series, params, jobId, authPart.toString(), null);
		return returnVal;
	}*/

	public static String buildCmonkeyNetworkJobFromWs(String wsId,
			CmonkeyRunParameters params, AuthToken authPart)
			throws Exception {

		String returnVal = null;

		returnVal = jobClient(authPart).createJob();

		if (deployCluster == false) {
			CmonkeyServerThread cmonkeyServerThread = new CmonkeyServerThread(
					wsId, params, returnVal, authPart.toString());
			cmonkeyServerThread.start();
		} else {
			String jsonArgs = prepareJson (wsId, returnVal, params, authPart.toString());
			String result = executePost(jsonArgs);
			System.out.println(result);
		}
		return returnVal;
	}

	protected static UserAndJobStateClient jobClient(AuthToken token)
			throws TokenFormatException, UnauthorizedException, IOException {
		if (_jobClient == null) {
			URL jobServiceUrl = new URL(JOB_SERVICE);
			_jobClient = new UserAndJobStateClient(jobServiceUrl, token);
			_jobClient.setAuthAllowedForHttp(true);
		}
		return _jobClient;
	}
	
	protected static String prepareJson (String wsId, String jobId, CmonkeyRunParameters params, String token){
		String returnVal = "{\"info\": {\"pipeline\": \"cmonkey-runner-pipeline\",\"name\": \"cmonkey\",\"project\": \"default\"" +
				",\"user\": \"default\",\"clientgroups\":\"\",\"sessionId\":\"" + jobId +
				"\"},\"tasks\": [{\"cmd\": {\"args\": \"";
		returnVal +=" --job " + jobId + " --method build_cmonkey_network_job_from_ws --ws '" + wsId + "' --series '" + params.getSeriesId() + "' --genome '" + params.getGenomeId() + "'";
		
		if (params.getMotifsScoring() == 0L){
			returnVal += " --nomotifs 1"; 
		} else {
			returnVal += " --nomotifs 0";
		}
		if (params.getNetworksScoring() == 0L){
			returnVal += " --nonetworks 1"; 
		} else {
			returnVal += " --nonetworks 0";
		}
		if (params.getOperomeId() != null){
			returnVal += " --operons '" + params.getOperomeId() + "'"; 
		} else {
			returnVal += " --operons 'null'";
		}
		if (params.getNetworkId() != null){
			returnVal += " --string '" + params.getNetworkId() + "'"; 
		} else {
			returnVal += " --string 'null'";
		}
		
		returnVal += " --token '" + token+"'";
		returnVal+="\", \"description\": \"running cMonkey service\", \"name\": \"run_cmonkey\"}, \"dependsOn\": [], \"outputs\": {\""+
		jobId + ".tgz\": {\"host\": \"http://140.221.84.236:8000\"}},\"taskid\": \"0\",\"skip\": 0,\"totalwork\": 1}]}";
		
		System.out.println(returnVal);
		return returnVal;
	}

	protected static String executePost(String jsonRequest) {
		URL url;
		HttpURLConnection connection = null;
		PrintWriter writer = null;
		try {
			// Create connection
			url = new URL(AWE_SERVICE);
			String boundary = Long.toHexString(System.currentTimeMillis());
			connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(10000);
			if (connectionReadTimeOut != null) {
				connection.setReadTimeout(connectionReadTimeOut);
			}
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type",
				    "multipart/form-data; boundary=" + boundary);
			connection.setDoOutput(true);
			//connection.setDoInput(true);
			OutputStream output = connection.getOutputStream();
		    writer = new PrintWriter(new OutputStreamWriter(output),
		            true); // true = autoFlush, important!
		    String CRLF = "\r\n";
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"upload\"; filename=\"cmonkey.awe\"")
		            .append(CRLF);
		    writer.append("Content-Type: application/octet-stream").append(CRLF);
		    writer.append(CRLF).flush();
		    writer.append(jsonRequest).append(CRLF);
		    writer.flush();
		    writer.append("--" + boundary + "--").append(CRLF);
		    writer.append(CRLF).flush();

			// Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return response.toString();

		} catch (Exception e) {

			e.printStackTrace();
			return null;

		} finally {
			if (writer != null) writer.close();

			if (connection != null) {
				connection.disconnect();
			}
		}
	}

}
