package us.kbase.cmonkey;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import us.kbase.auth.AuthService;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.Tuple7;
import us.kbase.common.service.UObject;
import us.kbase.userandjobstate.Results;
import us.kbase.util.WsDeluxeUtil;

public class CmonkeyClientTest {

	private static final String USER_NAME = "aktest";
	private static final String PASSWORD = "1475rokegi";
	private static final String workspaceName = "AKtest";
//	private String serverUrl = "http://140.221.84.195:7049";
//	private String serverUrl = "http://140.221.85.173:7078";
	private String serverUrl = "http://localhost:7049";
	private String quickTestSeriesRef = "QuickTestExpressionDataSeries";
	private String testSeriesRef = "AKtest/kb|series.269";
	private String genomeRef = "AKtest/kb|genome.8";
	private String testStringNetworkRef = "AKtest/kb|interactionset.5";
	private String testOperonNetworkRef = "AKtest/kb|interactionset.6";


	
	@Test
	public final void testQuickBuildCmonkeyNetworkJobFromWs() throws Exception {
		AuthToken token = AuthService.login(USER_NAME, new String(PASSWORD)).getToken();
//		System.out.println(token.toString());
		CmonkeyRunParameters params = new CmonkeyRunParameters();
		params.setMotifsScoring(0L);
		params.setNetworksScoring(0L);
		params.setGenomeRef(genomeRef);
		params.setSeriesRef(testSeriesRef);
		URL url = new URL(serverUrl);
		CmonkeyClient client = new CmonkeyClient(url, token);
		client.setAuthAllowedForHttp(true);
		String jobId = client.buildCmonkeyNetworkJobFromWs(workspaceName, params);
		
		System.out.println("Job ID = " + jobId);
		assertNotNull(jobId);
		String resultId = "";

		String status = "";
		Integer waitingTime = 2;
		while (!status.equalsIgnoreCase("finished")){
			
			try {
			    Thread.sleep(120000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		
			try {
				Tuple7<String,String,String,Long,String,Long,Long> t = CmonkeyServerImpl.jobClient(token.toString()).getJobStatus(jobId); 
				//System.out.println(t.getE1());
				//System.out.println(t.getE2());
				status = t.getE3();
				//System.out.println(t.getE3());//Status
				//System.out.println(t.getE4());
				//System.out.println(t.getE5());
				//System.out.println(t.getE6());
				//System.out.println(t.getE7());
				System.out.println("Waiting time: "+ waitingTime.toString() + " minutes");
				waitingTime += 2;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonClientException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			Results res = CmonkeyServerImpl.jobClient(token.toString()).getResults(jobId);			
			resultId = res.getWorkspaceids().get(0);
			System.out.println("Result ID = " + resultId);
			assertNotNull(resultId);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String[] resultIdParts = resultId.split("/");
		resultId = resultIdParts[1];
		
		CmonkeyRunResult result = WsDeluxeUtil.getObjectFromWorkspace(workspaceName, resultId, token.toString()).getData().asClassInstance(CmonkeyRunResult.class);
		
		assertEquals(Long.valueOf("3"), result.getNetwork().getClustersNumber());
		assertEquals(Long.valueOf("2001"), result.getLastIteration());

	}

	@Test
	public final void testBuildCmonkeyNetworkJobFromWs() throws Exception {
		AuthToken token = AuthService.login(USER_NAME, new String(PASSWORD)).getToken();
//		System.out.println(token.toString());
		CmonkeyRunParameters params = new CmonkeyRunParameters();
		params.setMotifsScoring(1L);
		params.setNetworksScoring(1L);
		params.setOperomeRef(testOperonNetworkRef);
		params.setNetworkRef(testStringNetworkRef);
		params.setSeriesRef(testSeriesRef);
		params.setGenomeRef(genomeRef);

		URL url = new URL(serverUrl);
		CmonkeyClient client = new CmonkeyClient(url, token);
		client.setAuthAllowedForHttp(true);
		String jobId = client.buildCmonkeyNetworkJobFromWs(workspaceName, params);
		
		System.out.println("Job ID = " + jobId);
		assertNotNull(jobId);
		String resultId = "";

		String status = "";
		Integer waitingTime = 2;
		while (!status.equalsIgnoreCase("finished")){
			
			try {
			    Thread.sleep(120000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		
			try {
				Tuple7<String,String,String,Long,String,Long,Long> t = CmonkeyServerImpl.jobClient(token.toString()).getJobStatus(jobId); 
				System.out.println(t.getE1());
				System.out.println(t.getE2());
				status = t.getE3();
				System.out.println(t.getE3());//Status
				System.out.println(t.getE4());
				System.out.println(t.getE5());
				System.out.println(t.getE6());
				System.out.println(t.getE7());
				System.out.println("Waiting time: "+ waitingTime.toString() + " minutes");
				waitingTime += 2;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonClientException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			Results res = CmonkeyServerImpl.jobClient(token.toString()).getResults(jobId);			
			resultId = res.getWorkspaceids().get(0);
			System.out.println("Result ID = " + resultId);
			assertNotNull(resultId);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String[] resultIdParts = resultId.split("/");
		resultId = resultIdParts[1];

		CmonkeyRunResult result = WsDeluxeUtil.getObjectFromWsByRef(workspaceName+"/"+resultId, token.toString()).getData().asClassInstance(CmonkeyRunResult.class);

		assertEquals(Long.valueOf("43"), result.getNetwork().getClustersNumber());
		assertEquals(Long.valueOf("2001"), result.getLastIteration());

	}

}
