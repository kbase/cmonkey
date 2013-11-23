package us.kbase.cmonkey;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientCaller;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.UnauthorizedException;

/**
 * <p>Original spec-file module name: Cmonkey</p>
 * <pre>
 * Module KBaseCmonkey version 1.0
 * This module provides a set of methods for work with cMonkey biclustering tool.
 * Data types summary
 * Input data types: 
 * ExpressionDataSeries represents a list of expression data samples that serve as an input of cMonkey.
 * ExpressionDataSample data type represents a sample of expression data for a single condition.
 * ExpressionDataPoint data type represents a relative expression value for a single gene.
 * Output data types:
 * CmonkeyRun data type represents data generated by a single run of cMonkey (run_infos table of cMonkey results)
 * CmonkeyNetwork data type represents bicluster network
 * CmonkeyCluter data type represents a single bicluster from cMonkey network, with links to genes, experimental conditions and motifs
 * CmonkeyMotif data type represents a single motif identifed for a bicluster
 * Methods summary
 * build_cmonkey_network - Starts cMonkey server run for a series of expression data and returns job ID of the run 
 * build_cmonkey_network_from_ws - Starts cMonkey server run for a series of expression data stored in workspace and returns job ID of the run
 * build_cmonkey_network_job_from_ws - Starts cMonkey server run for a series of expression data stored in workspace and returns job ID of the run
 * </pre>
 */
public class CmonkeyClient {
    private JsonClientCaller caller;
    private static URL DEFAULT_URL = null;
    static {
        try {
            DEFAULT_URL = new URL("http://kbase.us/services/cmonkey");
        } catch (MalformedURLException mue) {
            throw new RuntimeException("Compile error in client - bad url compiled");
        }
    }

    public CmonkeyClient() {
       caller = new JsonClientCaller(DEFAULT_URL);
    }

    public CmonkeyClient(URL url) {
        caller = new JsonClientCaller(url);
    }

    public CmonkeyClient(URL url, AuthToken token) throws UnauthorizedException, IOException {
        caller = new JsonClientCaller(url, token);
    }

    public CmonkeyClient(URL url, String user, String password) throws UnauthorizedException, IOException {
        caller = new JsonClientCaller(url, user, password);
    }

    public CmonkeyClient(AuthToken token) throws UnauthorizedException, IOException {
        caller = new JsonClientCaller(DEFAULT_URL, token);
    }

    public CmonkeyClient(String user, String password) throws UnauthorizedException, IOException {
        caller = new JsonClientCaller(DEFAULT_URL, user, password);
    }

	public void setConnectionReadTimeOut(Integer milliseconds) {
		this.caller.setConnectionReadTimeOut(milliseconds);
	}

    public boolean isAuthAllowedForHttp() {
        return caller.isAuthAllowedForHttp();
    }

    public void setAuthAllowedForHttp(boolean isAuthAllowedForHttp) {
        caller.setAuthAllowedForHttp(isAuthAllowedForHttp);
    }

    /**
     * <p>Original spec-file function name: build_cmonkey_network</p>
     * <pre>
     * Starts cMonkey server run for a series of expression data and returns run result 
     * ExpressionDataSeries series - series of expression data samples for cMonkey run
     * CmonkeyRunParameters params - parameters of cMonkey run
     * string job_id - identifier of cMonkey job
     * </pre>
     * @param   series   instance of type {@link us.kbase.cmonkey.ExpressionDataSeries ExpressionDataSeries}
     * @param   params   instance of type {@link us.kbase.cmonkey.CmonkeyRunParameters CmonkeyRunParameters}
     * @return   parameter "cmonkey_run_result" of type {@link us.kbase.cmonkey.CmonkeyRunResult CmonkeyRunResult}
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public CmonkeyRunResult buildCmonkeyNetwork(ExpressionDataSeries series, CmonkeyRunParameters params) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(series);
        args.add(params);
        TypeReference<List<CmonkeyRunResult>> retType = new TypeReference<List<CmonkeyRunResult>>() {};
        List<CmonkeyRunResult> res = caller.jsonrpcCall("Cmonkey.build_cmonkey_network", args, retType, true, false);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: build_cmonkey_network_from_ws</p>
     * <pre>
     * Starts cMonkey server run for a series of expression data stored in workspace and returns ID of the run result object
     * string ws_id - workspace id
     * string series_id - kbase id of expression data series for cMonkey run
     * CmonkeyRunParameters params - parameters of cMonkey run
     * string job_id - identifier of cMonkey job
     * </pre>
     * @param   wsId   instance of String
     * @param   collectionId   instance of String
     * @param   params   instance of type {@link us.kbase.cmonkey.CmonkeyRunParameters CmonkeyRunParameters}
     * @return   parameter "cmonkey_run_result_id" of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String buildCmonkeyNetworkFromWs(String wsId, String collectionId, CmonkeyRunParameters params) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(wsId);
        args.add(collectionId);
        args.add(params);
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("Cmonkey.build_cmonkey_network_from_ws", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: build_cmonkey_network_job_from_ws</p>
     * <pre>
     * Starts cMonkey server run for a series of expression data stored in workspace and returns job ID of the run
     * string ws_id - workspace id
     * string series_id - kbase id of expression data series for cMonkey run
     * CmonkeyRunParameters params - parameters of cMonkey run
     * string job_id - identifier of cMonkey job
     * </pre>
     * @param   wsId   instance of String
     * @param   seriesId   instance of String
     * @param   params   instance of type {@link us.kbase.cmonkey.CmonkeyRunParameters CmonkeyRunParameters}
     * @return   parameter "cmonkey_run_result_job_id" of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String buildCmonkeyNetworkJobFromWs(String wsId, String seriesId, CmonkeyRunParameters params) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(wsId);
        args.add(seriesId);
        args.add(params);
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("Cmonkey.build_cmonkey_network_job_from_ws", args, retType, true, true);
        return res.get(0);
    }
}
