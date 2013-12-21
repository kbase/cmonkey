package us.kbase.cmonkey;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import us.kbase.auth.AuthException;
import us.kbase.auth.AuthToken;
import us.kbase.auth.TokenFormatException;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.Tuple11;
import us.kbase.common.service.UObject;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.expressionservices.ExpressionSample;
import us.kbase.expressionservices.ExpressionSeries;
import us.kbase.idserverapi.IDServerAPIClient;
import us.kbase.userandjobstate.InitProgress;
import us.kbase.userandjobstate.Results;
import us.kbase.userandjobstate.UserAndJobStateClient;
import us.kbase.workspace.ObjectData;
import us.kbase.workspace.ObjectIdentity;
import us.kbase.workspace.ObjectSaveData;
import us.kbase.workspace.SaveObjectsParams;
import us.kbase.workspace.WorkspaceClient;

/*import us.kbase.workspaceservice.GetObjectOutput;
 import us.kbase.workspaceservice.GetObjectParams;
 import us.kbase.workspaceservice.ObjectData;
 import us.kbase.workspaceservice.SaveObjectParams;
 import us.kbase.workspaceservice.WorkspaceServiceClient;*/

public class CmonkeyServerImpl {
	// private static Integer tempFileId = 0;
	private static final String JOB_PATH = "/var/tmp/cmonkey/";
	// private static final String CMONKEY_COMMAND = "cmonkey-python";
	private static final String CMONKEY_DIR = "/kb/runtime/cmonkey-python/";
	private static final String CMONKEY_COMMAND = "/kb/runtime/cmonkey-python/cmonkey.py";
	private static final String DATA_PATH = "/etc/cmonkey-python/KEGG_taxonomy";
	private static final String ID_SERVICE_URL = "http://kbase.us/services/idserver";
	// private static final String WS_SERVICE_URL =
	// "http://kbase.us/services/workspace";
	private static final String WS_SERVICE_URL = "http://140.221.84.209:7058";

	private static final String JOB_SERVICE_URL = "http://140.221.84.180:7083";
	private static IDServerAPIClient _idClient = null;
	// private static WorkspaceServiceClient _wsClient = null;
	private static WorkspaceClient _wsClient = null;
	private static UserAndJobStateClient _jobClient = null;
	private static Date date = new Date();
	private static SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ssZ");
	private static boolean awe = false;

	protected static IDServerAPIClient idClient() throws TokenFormatException,
			UnauthorizedException, IOException {
		if (_idClient == null) {
			URL idServerUrl = new URL(ID_SERVICE_URL);
			_idClient = new IDServerAPIClient(idServerUrl);
		}
		return _idClient;
	}

	/*
	 * protected static WorkspaceServiceClient wsClient(String token) throws
	 * TokenFormatException, UnauthorizedException, IOException{ if(_wsClient ==
	 * null) { URL workspaceClientUrl = new URL (WS_SERVICE_URL); AuthToken
	 * authToken = new AuthToken(token); _wsClient = new
	 * WorkspaceServiceClient(workspaceClientUrl, authToken);
	 * _wsClient.setAuthAllowedForHttp(true); } return _wsClient; }
	 */

	protected static WorkspaceClient wsClient(String token) {
		if(_wsClient == null)
		{
			URL workspaceClientUrl;
			try {
				workspaceClientUrl = new URL (WS_SERVICE_URL);
				AuthToken authToken = new AuthToken(token);
				_wsClient = new WorkspaceClient(workspaceClientUrl, authToken);
				_wsClient.setAuthAllowedForHttp(true);
			} catch (MalformedURLException e) {
				System.err.println("Bad URL? Unable to communicate with workspace service at" + WS_SERVICE_URL);
				e.printStackTrace();
			} catch (TokenFormatException e) {
				System.err.println("Unable to authenticate");
				e.printStackTrace();
			} catch (UnauthorizedException e) {
				System.err.println("Unable to authenticate in workspace service at" + WS_SERVICE_URL);
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("Unable to communicate with workspace service at" + WS_SERVICE_URL);
				e.printStackTrace();
			}
		}
		return _wsClient;
	}

	protected static UserAndJobStateClient jobClient(String token)
			throws UnauthorizedException, IOException, AuthException {
		if (_jobClient == null) {
			URL jobServiceUrl = new URL(JOB_SERVICE_URL);
			AuthToken authToken = new AuthToken(token);
			_jobClient = new UserAndJobStateClient(jobServiceUrl, authToken);
			_jobClient.setAuthAllowedForHttp(true);
		}
		return _jobClient;
	}

	protected static void cleanUpOnStart() {
		try {
			Runtime.getRuntime().exec("rm -r " + JOB_PATH + "*");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

/*	public static CmonkeyRunResult buildCmonkeyNetwork(ExpressionSeries series,
			CmonkeyRunParameters params, String jobId, String token,
			String currentDir) throws Exception {
		CmonkeyRunResult cmonkeyRunResult = new CmonkeyRunResult();
		cmonkeyRunResult.setId(getKbaseId("CmonkeyRunResult"));
		String jobPath = null;
		if (currentDir == null) {
			jobPath = JOB_PATH + jobId + "/";
			new File(jobPath).mkdir();
		} else {
			jobPath = currentDir + "/" + jobId + "/";
			awe = true;
		}

		// prepare input

		String inputTable = getInputTable(series);
		FileWriter writer = new FileWriter(jobPath + "serveroutput.txt");
		writer.write(inputTable);
		writer.flush();

		// check list of genes
		String organismName = getOrganismName(wsName, series.getKbId(), token);
		writer.write("Organism name = " + organismName + "\n");
		writer.flush();
		String organismCode = getKeggCode(organismName);
		writer.write("Organism code = " + organismCode + "\n");
		writer.flush();

		// save input file
		writeInputFile(jobPath + "input.txt", inputTable);
		// generate command line
		String commandLine = generateCmonkeyCommandLine(jobPath, params,
				organismCode);
		writer.write(commandLine + "\n");
		writer.flush();
		// run
		if (jobId != null)
			updateJobProgress(jobId,
					"Input prepared. Starting cMonkey program...", token);
		Integer exitVal = executeCommand(commandLine, jobPath, jobId, token);
		if (exitVal != null) {
			writer.write("ExitValue: " + exitVal.toString() + "\n");
			writer.flush();
		} else {
			writer.write("ExitValue: null\n");
			writer.flush();
		}

		// parse results

		if (jobId != null)
			updateJobProgress(jobId, "cMonkey finished. Processing output...",
					token);

		String sqlFile = jobPath + "out/cmonkey_run.db";
		writer.write(sqlFile + "\n");
		writer.flush();
		parseCmonkeySql(sqlFile, cmonkeyRunResult);
		String resultId = getKbaseId("CmonkeyRunResult");
		writer.write(resultId + "\n");
		cmonkeyRunResult.setId(resultId);

		writer.close();
		// clean up
		if (awe == false) {
			Runtime.getRuntime().exec("rm -r " + jobPath);
			Runtime.getRuntime().exec("rm " + JOB_PATH + "cmonkey-checkpoint*");
		}

		return cmonkeyRunResult;
	}*/

	public static void buildCmonkeyNetworkJobFromWs(String wsName,
			CmonkeyRunParameters params, String jobId, String token,
			String currentDir) throws Exception {
		
		String desc = "Cmonkey service job. Method: buildCmonkeyNetworkJobFromWs. Input: "
				+ params.getSeriesId() + ". Workspace: " + wsName + ".";
		if (jobId != null)
			startJob(jobId, desc, 23L, token.toString());

		ExpressionSeries series = getObjectFromWsByRef(params.getSeriesId(), token).getData().asClassInstance(ExpressionSeries.class);
		

		CmonkeyRunResult cmonkeyRunResult = new CmonkeyRunResult().withId(getKbaseId("CmonkeyRunResult"));
		
		

		String jobPath = createDirs(jobId, currentDir);

		
		prepareCacheFiles(jobPath, params.getNetworkId(), params.getOperomeId());

		
		
		// prepare input
		System.out.println(series.getExpressionSampleIds());
		String inputTable = getInputTable(wsName, series.getExpressionSampleIds(), token);
		FileWriter writer = new FileWriter(jobPath + "serveroutput.txt");
		writer.write(inputTable);
		writer.flush();

		// check list of genes
		String organismName = getOrganismName(wsName, series, token);
		writer.write("Organism name = " + organismName + "\n");
		writer.flush();
		String organismCode = getKeggCode(organismName);
		writer.write("Organism code = " + organismCode + "\n");
		writer.flush();

		// save input file
		writeInputFile(jobPath + "input.txt", inputTable);
		// generate command line
		String commandLine = generateCmonkeyCommandLine(jobPath, params,
				organismCode);
		writer.write(commandLine + "\n");
		writer.flush();
		// run
		if (jobId != null)
			updateJobProgress(jobId,
					"Input prepared. Starting cMonkey program...", token);
		Integer exitVal = executeCommand(commandLine, jobPath, jobId, token);
		if (exitVal != null) {
			writer.write("ExitValue: " + exitVal.toString() + "\n");
			writer.flush();
		} else {
			writer.write("ExitValue: null\n");
			writer.flush();
		}

		// parse results

		if (jobId != null)
			updateJobProgress(jobId, "cMonkey finished. Processing output...",
					token);

		String sqlFile = jobPath + "out/cmonkey_run.db";
		writer.write(sqlFile + "\n");
		writer.flush();
		parseCmonkeySql(sqlFile, cmonkeyRunResult);
		String resultId = getKbaseId("CmonkeyRunResult");
		writer.write(resultId + "\n");
		cmonkeyRunResult.setId(resultId);
		if (params.getOperomeId() == null) {
			params.setOperomeId("undefined");
		}
		if (params.getNetworkId() == null) {
			params.setNetworkId("undefined");
		}
		cmonkeyRunResult.setParameters(params);

		writer.close();
		// clean up
		if (awe == false) {
			Runtime.getRuntime().exec("rm -r " + jobPath);
			Runtime.getRuntime().exec("rm " + JOB_PATH + "cmonkey-checkpoint*");
		}
		
		
		
		
		saveObjectToWorkspace(
				UObject.transformObjectToObject(cmonkeyRunResult, UObject.class),
				"Cmonkey.CmonkeyRunResult-5.0", wsName, cmonkeyRunResult.getId(),
				token.toString());
		if (jobId != null)
			finishJob(jobId, wsName, cmonkeyRunResult.getId(), token.toString());

	}

	private static void prepareCacheFiles(String jobPath, String networkId,
		String operomeId) {

		
		
		//get genome
		//get contigset
		//write sequence files
		//write features and feature_names files
		
		//get operons and export
		
		//get string and export

	
}

	protected static String createDirs( String jobId, String currentDir){
		String jobPath = null;

		if (currentDir == null) {
			jobPath = JOB_PATH + jobId + "/";
			new File(jobPath).mkdir();
		} else {
			jobPath = currentDir + "/" + jobId + "/";
			awe = true;
		}
		new File(jobPath + "out/").mkdir();
		new File(jobPath + "cache/").mkdir();
		
		return jobPath;
	}
	
	protected static String generateCmonkeyCommandLine(String jobPath,
			CmonkeyRunParameters params, String organismCode) {

		String outputDirectory = jobPath + "out";
		String cacheDirectory = jobPath + "cache";
		String inputFile = jobPath + "input.txt";

		String commandLine = CMONKEY_COMMAND + " --organism " + organismCode
				+ " --ratios " + inputFile + " --out " + outputDirectory
				+ " --cachedir " + cacheDirectory;// + " --config " +
													// CONFIG_PATH;
		// Set options
		if (params.getMotifsScoring() == 0L) {
			commandLine += " --nomotifs";
		}
		if (params.getNetworksScoring() == 0L) {
			commandLine += " --nonetworks";
		}
		if (params.getOperomeId() == null) {
			commandLine += " --nooperons";
		}
		if (params.getNetworkId() == null) {
			commandLine += " --nostring";
		}

		return commandLine;
	}

	protected static void startJob(String jobId, String desc, Long tasks,
			String token) {

		String status = "cmonkey service job started. Preparing input...";
		InitProgress initProgress = new InitProgress();
		initProgress.setPtype("task");
		initProgress.setMax(tasks);
		date.setTime(date.getTime() + 108000000L);

		try {
			// System.out.println(dateFormat.format(date));
			jobClient(token).startJob(jobId, token, status, desc, initProgress,
					dateFormat.format(date));
		} catch (JsonClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuthException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected static void updateJobProgress(String jobId, String status,
			String token) {
		try {
			date.setTime(date.getTime() + 1000000L);
			jobClient(token).updateJobProgress(jobId, token, status, 1L,
					dateFormat.format(date));
		} catch (JsonClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuthException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected static void finishJob(String jobId, String wsId, String objectId,
			String token) {
		try {
			String status = "Finished";
			String error = null;

			Results res = new Results();
			List<String> workspaceIds = new ArrayList<String>();
			workspaceIds.add(wsId + "/" + objectId);
			res.setWorkspaceids(workspaceIds);
			jobClient(token).completeJob(jobId, token, status, error, res);
		} catch (JsonClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuthException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected static String getKbaseId(String entityType) {
		String returnVal = null;

		try {
			if (entityType.equals("CmonkeyRunResult")) {
				returnVal = "kb|cmonkeyrunresult."
						+ idClient().allocateIdRange("kb|cmonkeyrunresult", 1L)
								.toString();
			} else if (entityType.equals("CmonkeyNetwork")) {
				returnVal = "kb|cmonkeynetwork."
						+ idClient().allocateIdRange("kb|cmonkeynetwork", 1L)
								.toString();
			} else if (entityType.equals("CmonkeyCluster")) {
				returnVal = "kb|cmonkeycluster."
						+ idClient().allocateIdRange("kb|cmonkeycluster", 1L)
								.toString();
			} else if (entityType.equals("CmonkeyMotif")) {
				returnVal = "kb|cmonkeymotif."
						+ idClient().allocateIdRange("kb|cmonkeymotif", 1L)
								.toString();
			} else if (entityType.equals("MastHit")) {
				returnVal = "kb|masthit."
						+ idClient().allocateIdRange("kb|masthit", 1L)
								.toString();
			} else if (entityType.equals("ExpressionSeries")) {
				returnVal = "kb|series."
						+ idClient().allocateIdRange("kb|series", 1L)
								.toString();
			} else if (entityType.equals("ExpressionSample")) {
				returnVal = "kb|sample."
						+ idClient().allocateIdRange("kb|sample", 1L)
								.toString();
			} else {
				System.out.println("ID requested for unknown type "
						+ entityType);
			}
		} catch (TokenFormatException e) {
			System.err.println("Unable to get KBase ID for " + entityType + " from " + ID_SERVICE_URL + ": Token Format Exception");
			e.printStackTrace();
		} catch (UnauthorizedException e) {
			System.err.println("Unable to get KBase ID for " + entityType + " from " + ID_SERVICE_URL + ": Unauthorized Exception");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Unable to get KBase ID for " + entityType + " from " + ID_SERVICE_URL + ": IO Exception");
			e.printStackTrace();
		} catch (JsonClientException e) {
			System.err.println("Unable to get KBase ID for " + entityType + " from " + ID_SERVICE_URL + ": Json error");
			e.printStackTrace();
		}
		return returnVal;
	}

	@SuppressWarnings("unused")
	protected static String getOrganismName(String wsName, ExpressionSeries series, String token)
			throws Exception {
		//return "Halobacterium";
		String organismName = null;
		List<String> sampleRefs = series.getExpressionSampleIds();
		
		ExpressionSample sample = UObject.transformObjectToObject(getObjectFromWsByRef( sampleRefs.get(0), token.toString()), ExpressionSample.class);
		String genomeId = sample.getGenomeId();
		for (int i = 1; i < sampleRefs.size(); i++){
			String sampleGenome = UObject.transformObjectToObject(getObjectFromWsByRef(sampleRefs.get(i), token.toString()), ExpressionSample.class).getGenomeId();
			if (sampleGenome != genomeId){
				throw new Exception(
						"Genes in input data samples " + sample.getGenomeId() + " and " + sampleRefs.get(i) + " belong to different organisms");
			}
		}
		
/*		Microbesonline microbesonline = new Microbesonline();
		List<String> geneNames = new ArrayList<String>();
		for (ExpressionSample set : series.getSamples()) {
			for (ExpressionDataPoint point : set.getPoints()) {
				geneNames.add(point.getGeneId());
			}
		}
		geneNames = new ArrayList<String>(new HashSet<String>(geneNames));
		for (int i = 0; i < geneNames.size(); i++) {
			try {
				organismName = microbesonline
						.getGenomeForGene(geneNames.get(i));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println(geneNames.get(i));
				e.printStackTrace();
			}
			if (organismName != null) {
				break;
			}
		}

		if (organismName == null) {
			throw new Exception("Organism name cannot be identified");
		}

		for (String geneName : geneNames) {
			if ((microbesonline.getGenomeForGene(geneName) != null)
					&& (!microbesonline.getGenomeForGene(geneName).equals(
							organismName))) {
				throw new Exception(
						"Genes in input data series belong to different organisms");
			}
		}*/
		if (organismName == null)  {
			return "Halobacterium sp. NRC-1";
		} else {
			return organismName;
		}
	}

	protected static String getKeggCode(String organismName) {
		String result = null;

		try {
			String line = null;
			BufferedReader br = new BufferedReader(new FileReader(DATA_PATH));
			while ((line = br.readLine()) != null) {
				if ((line.equals("")) || (line.matches("#.*"))) {
					// do nothing
				} else {
					String[] fields = line.split("\t");
					if (fields[3].equals(organismName)) {
						result = fields[1];
					}
				}
			}
			br.close();
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
		}
		return result;
	}

	protected static String getInputTable(String wsName, List<String> sampleRefs, String token) {
		
		
		List<UObject> objects = getObjectsFromWsByRef(sampleRefs, token);
		List<ExpressionSample> samples = new ArrayList<ExpressionSample>();
		for (UObject o : objects){
			ExpressionSample s = UObject.transformObjectToObject(o, ExpressionSample.class);
			samples.add(s);
		}
		
		String result = "GENE";
		List<Map<String, Double>> dataCollection = new ArrayList<Map<String, Double>>();
		// make list of conditions
		for (ExpressionSample sample : samples) {
			result += "\t" + sample.getKbId();
			Map<String, Double> dataSet = sample.getExpressionLevels();
			dataCollection.add(dataSet);
		}
		// make list of genes
		List<String> geneNames = new ArrayList<String>();
		for (ExpressionSample sample : samples) {
			Map<String, Double> values = sample.getExpressionLevels();
			for (String gene : values.keySet()){
				geneNames.add(gene);
			}
			
		}
		List<String> uniqueGeneNames = new ArrayList<String>(
				new HashSet<String>(geneNames));
		for (String geneName : uniqueGeneNames) {
			result += "\n" + geneName;
			DecimalFormat df = new DecimalFormat("0.000");
			for (Map<String, Double> dataSetMap : dataCollection) {
				if (dataSetMap.containsKey(geneName)) {
					if (dataSetMap.get(geneName).toString().matches("-.*")) {
						result += "\t"
								+ df.format(dataSetMap.get(geneName));
					} else {
						result += "\t "
								+ df.format(dataSetMap.get(geneName));
					}
				} else {
					result += "\tNA";
				}
			}
		}
		result += "\n";
		return result;
	}

	protected static void writeInputFile(String inputFileName, String input) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(inputFileName));
			writer.write(input);
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				System.out.println(e.getLocalizedMessage());
			}
		}

	}

	protected static Integer executeCommand(String commandLine, String jobPath)
			throws InterruptedException {
		Integer exitVal = executeCommand(commandLine, jobPath, null, null);
		return exitVal;
	}

	protected static Integer executeCommand(String commandLine, String jobPath,
			String jobId, String token) throws InterruptedException {
		Integer exitVal = null;
		try {
			Process p = Runtime.getRuntime().exec(commandLine, null,
					new File(CMONKEY_DIR));

			StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(),
					"ERROR", jobId, token, jobPath + "errorlog.txt");

			// any output?
			StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(),
					"OUTPUT", jobId, token, jobPath + "out.txt");

			// kick them off
			errorGobbler.start();
			outputGobbler.start();

			// any error???
			exitVal = p.waitFor();
			System.out.println("ExitValue: " + exitVal);
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
		} finally {
		}
		return exitVal;

	}

	protected static void parseCmonkeySql(String sqlFile,
			CmonkeyRunResult cmonkeyRunResult) throws Exception {
		CmonkeySqlite database = new CmonkeySqlite(sqlFile);
		database.buildCmonkeyRunResult(cmonkeyRunResult);
		database.disconnect();
	}

	protected static List<UObject> getObjectsFromWorkspace(String workspaceName,
			List<String> names, String token) {
		try {
			List<ObjectIdentity> objectsIdentity = new ArrayList<ObjectIdentity>();
			for (String name : names){
				System.out.println(name);
			ObjectIdentity objectIdentity = new ObjectIdentity().withWorkspace(workspaceName).withName(name);
			objectsIdentity.add(objectIdentity);
			}
			List<ObjectData> output = wsClient(token.toString()).getObjects(
					objectsIdentity);

			List<UObject> returnVal = new ArrayList<UObject>();
			
			for (ObjectData data : output) {
				returnVal.add(data.getData());
			}
			return returnVal;
		} catch (IOException e) {
			System.err.println("Unable to get objects " + names.toString()
					+ " from workspace " + workspaceName + ": IO Exception");
			e.printStackTrace();
		} catch (JsonClientException e) {
			System.err.println("Unable to get objects " + names.toString()
					+ " from workspace " + workspaceName + ": JSON Client Exception");
			e.printStackTrace();
		}
		return null;
	}

	protected static List<UObject> getObjectsFromWsByRef(
			List<String> refs, String token) {
		try {
			List<ObjectIdentity> objectsIdentity = new ArrayList<ObjectIdentity>();
			for (String ref : refs){
				System.out.println(ref);
			ObjectIdentity objectIdentity = new ObjectIdentity().withRef(ref);
			objectsIdentity.add(objectIdentity);
			}
			List<ObjectData> output = wsClient(token.toString()).getObjects(
					objectsIdentity);

			List<UObject> returnVal = new ArrayList<UObject>();
			
			for (ObjectData data : output) {
				returnVal.add(data.getData());
			}
			return returnVal;
		} catch (IOException e) {
			System.err.println("Unable to get objects " + refs.toString() + " : IO Exception");
			e.printStackTrace();
		} catch (JsonClientException e) {
			System.err.println("Unable to get objects " + refs.toString() + " : JSON Client Exception");
			e.printStackTrace();
		}
		return null;
	}

	
	protected static ObjectData getObjectFromWorkspace(String workspaceName,
			String name, String token) {
		try {
			List<ObjectIdentity> objectsIdentity = new ArrayList<ObjectIdentity>();
			ObjectIdentity objectIdentity = new ObjectIdentity().withName(name)
					.withWorkspace(workspaceName);
			objectsIdentity.add(objectIdentity);
			List<ObjectData> output = wsClient(token.toString()).getObjects(
					objectsIdentity);

			return output.get(0);
		} catch (IOException e) {
			System.err.println("Unable to get object " + name
					+ " from workspace " + workspaceName + ": IO Exception");
			e.printStackTrace();
		} catch (JsonClientException e) {
			System.err.println("Unable to get object " + name
					+ " from workspace " + workspaceName + ": JSON Client Exception");
			e.printStackTrace();
		}
		return null;
	}

	protected static ObjectData getObjectFromWsByRef(String ref, String token) {
		try {
			List<ObjectIdentity> objectsIdentity = new ArrayList<ObjectIdentity>();
			ObjectIdentity objectIdentity = new ObjectIdentity().withRef(ref);
			objectsIdentity.add(objectIdentity);
			List<ObjectData> output = wsClient(token.toString()).getObjects(
					objectsIdentity);

			return output.get(0);
		} catch (IOException e) {
			System.err.println("Unable to get object " + ref + ": IO Exeption");
			e.printStackTrace();
		} catch (JsonClientException e) {
			System.err.println("Unable to get object " + ref + ": JSON Client Exception");
			e.printStackTrace();
		}
		return null;
	}

	protected static void saveObjectToWorkspace(UObject object, String type,
			String workspaceName, String name, String token) {

		SaveObjectsParams params = new SaveObjectsParams();
		params.setWorkspace(workspaceName);

		ObjectSaveData objectToSave = new ObjectSaveData();
		objectToSave.setData(object);
		objectToSave.setName(name);
		objectToSave.setType(type);
		Map<String, String> metadata = new HashMap<String, String>();
		objectToSave.setMeta(metadata);
		List<ObjectSaveData> objectsData = new ArrayList<ObjectSaveData>();
		objectsData.add(objectToSave);
		params.setObjects(objectsData);

		List<Tuple11<Long, String, String, String, Long, String, Long, String, String, Long, Map<String, String>>> ret = null;
		try {
			ret = wsClient(token).saveObjects(params);
		} catch (IOException e) {
			System.err.println("Unable to write object to workspace at " + WS_SERVICE_URL + " : IO Exception");
			e.printStackTrace();
		} catch (JsonClientException e) {
			System.err.println("Unable to write object to workspace at " + WS_SERVICE_URL + " : JSON Client Exception");
			e.printStackTrace();
		}

		System.out.println("Saving object:");
		System.out.println(ret.get(0).getE1());
		System.out.println(ret.get(0).getE2());
		System.out.println(ret.get(0).getE3());
		System.out.println(ret.get(0).getE4());
		System.out.println(ret.get(0).getE5());
		System.out.println(ret.get(0).getE6());
		System.out.println(ret.get(0).getE7());
		System.out.println(ret.get(0).getE8());
		System.out.println(ret.get(0).getE9());
		System.out.println(ret.get(0).getE10());
		System.out.println(ret.get(0).getE11());

	}

}
