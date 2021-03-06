package us.kbase.cmonkey;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import us.kbase.auth.AuthException;
import us.kbase.auth.AuthService;
import us.kbase.auth.AuthToken;
import us.kbase.auth.TokenFormatException;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.UObject;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.kbaseexpression.ExpressionSample;
import us.kbase.kbaseexpression.ExpressionSeries;
import us.kbase.idserverapi.IDServerAPIClient;
import us.kbase.userandjobstate.InitProgress;
import us.kbase.userandjobstate.Results;
import us.kbase.userandjobstate.UserAndJobStateClient;
import us.kbase.util.GenomeExporter;
import us.kbase.util.NetworkExporter;
import us.kbase.util.WsDeluxeUtil;
import us.kbase.workspace.ObjectData;

public class CmonkeyServerImpl {
	
	private static IDServerAPIClient _idClient = null;

	private static SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ssZ");

	protected static IDServerAPIClient idClient() throws TokenFormatException,
			UnauthorizedException, IOException {
		if (_idClient == null) {
			URL idServerUrl = new URL(CmonkeyServerConfig.ID_SERVICE_URL);
			_idClient = new IDServerAPIClient(idServerUrl);
		}
		return _idClient;
	}

	protected static void startup() {
		File propertiesFile;
		String kbTop = System.getenv("KB_TOP");
		if (!kbTop.substring(kbTop.length() - 1).equals("/")) {
			kbTop = kbTop + "/";
		}
		propertiesFile = new File (kbTop + "/services/cmonkey/cmonkey.properties");
		Properties prop = new Properties();
		InputStream input = null;
		 
		try {
	 
			input = new FileInputStream(propertiesFile);
			// load a properties file
			prop.load(input);
			// set service configs
			CmonkeyServerConfig.CMONKEY_DIRECTORY = prop.getProperty("cmonkey");
			CmonkeyServerConfig.CMONKEY_RUN_PATH = CmonkeyServerConfig.CMONKEY_DIRECTORY + "cmonkey.py";
			CmonkeyServerConfig.JOB_SERVICE_URL = prop.getProperty("ujs_url");
			CmonkeyServerConfig.AWE_SERVICE_URL = prop.getProperty("awe_url");
			CmonkeyServerConfig.ID_SERVICE_URL = prop.getProperty("id_url");
			CmonkeyServerConfig.WS_SERVICE_URL = prop.getProperty("ws_url");
			CmonkeyServerConfig.AWF_CONFIG_FILE = prop.getProperty("awf_config");
	 
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void buildCmonkeyNetworkJobFromWs(String wsName,
			CmonkeyRunParameters params, String jobId, String token,
			String currentDir) throws Exception {
		// Let's start!
		String aweTaskId = "";
		if (jobId != null) {
			aweTaskId = getJobStatus(jobId, token) + " ";
			updateJobProgress(jobId,
					aweTaskId+"AWE task started. Preparing input...", token);
		}
		// get expression data
		ExpressionSeries series;
		try {
			series = WsDeluxeUtil
					.getObjectFromWsByRef(params.getSeriesRef(), token).getData()
					.asClassInstance(ExpressionSeries.class);
		} catch (TokenFormatException e) {
			finishJobWithError(jobId, e.getMessage(), "Expression series download error", token);
			e.printStackTrace();
			throw new Exception ("Expression series download error");
		} catch (UnauthorizedException e) {
			finishJobWithError(jobId, e.getMessage(), "Expression series download error", token);
			e.printStackTrace();
			throw new Exception ("Expression series download error");
		} catch (IOException e) {
			finishJobWithError(jobId, e.getMessage(), "Expression series download error", token);
			e.printStackTrace();
			throw new Exception ("Expression series download error");
		} catch (JsonClientException e) {
			finishJobWithError(jobId, e.getMessage(), "Expression series download error", token);
			e.printStackTrace();
			throw new Exception ("Expression series download error");
		}
		// create result object
		CmonkeyRunResult cmonkeyRunResult;
		// get ID for the result
		try {
			cmonkeyRunResult = new CmonkeyRunResult()
					.withId(getKbaseId("CmonkeyRunResult"));
		} catch (TokenFormatException e) {
			finishJobWithError(jobId, e.getMessage(), "Unable to get ID for cMonkey result", token);
			e.printStackTrace();
			throw new Exception ("Unable to get ID for cMonkey result");
		} catch (UnauthorizedException e) {
			finishJobWithError(jobId, e.getMessage(), "Unable to get ID for cMonkey result", token);
			e.printStackTrace();
			throw new Exception ("Unable to get ID for cMonkey result");
		} catch (IOException e) {
			finishJobWithError(jobId, e.getMessage(), "Unable to get ID for cMonkey result", token);
			e.printStackTrace();
			throw new Exception ("Unable to get ID for cMonkey result");
		} catch (JsonClientException e) {
			finishJobWithError(jobId, e.getMessage(), "Unable to get ID for cMonkey result", token);
			e.printStackTrace();
			throw new Exception ("Unable to get ID for cMonkey result");
		}

		// create working directory
		String jobPath = createDirs(jobId, currentDir);
		// start log file
		System.setErr(new PrintStream(new File(jobPath + "servererror.txt")));
		FileWriter writer = new FileWriter(jobPath + "serveroutput.txt");
		Date date = new Date();
		writer.write("log file created " + dateFormat.format(date) + "\n");
		writer.flush();
		// prepare input file
		Set<String> genomeIds = series.getGenomeExpressionSampleIdsMap().keySet();
		List<String> sampleIdsList = null;
		if (genomeIds.size() > 1) {
			finishJobWithError(jobId, "ExpressionSeries contains more than one genome ID", "Incompatible expression series format", token);
			throw new Exception ("ExpressionSeries contains more than one genome ID");
		} else {
			String genomeId = genomeIds.iterator().next();
			sampleIdsList = series.getGenomeExpressionSampleIdsMap().get(genomeId);
		}
		try {
			createInputTable(jobPath, sampleIdsList, token);
		} catch (TokenFormatException e) {
			finishJobWithError(jobId, e.getMessage(), "Expression samples download error", token);
			e.printStackTrace();
			throw new Exception ("Expression samples download error");
		} catch (IOException e) {
			finishJobWithError(jobId, e.getMessage(), "Expression samples download error", token);
			e.printStackTrace();
			throw new Exception ("Expression samples download error");
		} catch (JsonClientException e) {
			finishJobWithError(jobId, e.getMessage(), "Expression samples download error", token);
			e.printStackTrace();
			throw new Exception ("Expression samples download error");
		}
		writer.write("Input file created\n");
		writer.flush();
		series = null;
		// prepare cache files
		String genomeName;
		try {
			genomeName = prepareCacheFiles(jobId, jobPath + "cache/", params,
					token, writer);
		} catch (TokenFormatException e1) {
			finishJobWithError(jobId, e1.getMessage(), "Genome data download/write error", token);
			e1.printStackTrace();
			throw new Exception ("Genome data download/write error");
		} catch (IOException e1) {
			finishJobWithError(jobId, e1.getMessage(), "Genome data download/write error", token);
			e1.printStackTrace();
			throw new Exception ("Genome data download/write error");
		} catch (JsonClientException e1) {
			finishJobWithError(jobId, e1.getMessage(), "Genome data download/write error", token);
			e1.printStackTrace();
			throw new Exception ("Genome data download/write error");
		}
		writer.write("Cache files created in " + jobPath + "cache/\n");
		writer.flush();
		// generate command line
		String commandLine = generateCmonkeyCommandLine(jobPath, params);
		writer.write(commandLine + "\n");
		writer.flush();
		gc();
		// cross fingers and run cmonkey-python
		if (jobId != null)
			updateJobProgress(jobId,
					aweTaskId+"Input prepared. Starting cMonkey program...", token);
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
			updateJobProgress(jobId, aweTaskId+"cMonkey finished. Processing output...",
					token);
		String sqlFile = jobPath + "out/cmonkey_run.db";
		writer.write(sqlFile + "\n");
		writer.flush();
		String status;
		try {
			status = parseCmonkeySql(sqlFile, cmonkeyRunResult, genomeName);
		} catch (NullPointerException e1) {
			finishJobWithError(jobId, e1.getMessage(), "SQL database error. Missing some data?", token);
			e1.printStackTrace();
			throw new Exception ("SQL database error. Missing some data?");
		} catch (SQLException e1) {
			finishJobWithError(jobId, e1.getMessage(), "SQL error. Missing some data?", token);
			e1.printStackTrace();
			throw new Exception ("SQL error. Missing some data?");
		} catch (IOException e1) {
			finishJobWithError(jobId, e1.getMessage(), "Database file error", token);
			e1.printStackTrace();
			throw new Exception ("Database file error");
		}
		if (status != null) {
			writer.write("Error: " + status);
			if (jobId != null)
				finishJobWithError(jobId, "Error: " + status, "cMonkey execution error", token);
			// close log file
			writer.close();
		} else {
			writer.write(cmonkeyRunResult.getId() + "\n");
			cmonkeyRunResult.setParameters(params);
			// save result
			try {
				WsDeluxeUtil.saveObjectToWorkspace(UObject.transformObjectToObject(
						cmonkeyRunResult, UObject.class),
						CmonkeyServerConfig.CMONKEY_RUN_RESULT_TYPE, wsName, cmonkeyRunResult
								.getId(), token.toString());
			} catch (TokenFormatException e) {
				finishJobWithError(jobId, e.getMessage(), "Cmonkey run result upload error", token);
				e.printStackTrace();
				throw new Exception ("Cmonkey run result upload error");
			} catch (UnauthorizedException e) {
				finishJobWithError(jobId, e.getMessage(), "Cmonkey run result upload error", token);
				e.printStackTrace();
				throw new Exception ("Cmonkey run result upload error");
			} catch (IOException e) {
				finishJobWithError(jobId, e.getMessage(), "Cmonkey run result upload error", token);
				e.printStackTrace();
				throw new Exception ("Cmonkey run result upload error");
			} catch (JsonClientException e) {
				finishJobWithError(jobId, e.getMessage(), "Cmonkey run result upload error", token);
				e.printStackTrace();
				throw new Exception ("Cmonkey run result upload error");
			}
			// close log file
			writer.close();
			// clean up (if not on AWE)
			if (CmonkeyServerConfig.DEPLOY_AWE == false) {
				File fileDelete = new File(jobPath);
				deleteDirectoryRecursively(fileDelete);
				deleteFilesByPattern(CmonkeyServerConfig.CMONKEY_DIRECTORY, "cmonkey-checkpoint.*");
			}
			if (jobId != null)
				finishJob(jobId, wsName, cmonkeyRunResult.getId(), "Finished",
						token.toString());
		}
	}

	protected static String prepareCacheFiles(String jobId, String cachePath,
			CmonkeyRunParameters params, String token, FileWriter writer)
			throws Exception {
		// get genome, contigset and export
		String genomeName = "";
		try {
			genomeName = GenomeExporter.writeGenome(params.getGenomeRef(),
				"my_favorite_pet", cachePath, token);
		} catch (Exception e) {
			finishJobWithError(jobId, e.getMessage(), "Genome data export error", token);
			e.printStackTrace();
			throw new Exception ("Genome data export error");
		}
		writer.write("Genome files created\n");
		writer.flush();
		// get operons and export
		gc();
		if (!((params.getOperomeRef() == null) || (params.getOperomeRef()
				.equals("")))) {
			try {
				NetworkExporter.exportOperons(params.getOperomeRef(), "1",
						cachePath, token);
			} catch (Exception e) {
				finishJobWithError(jobId, e.getMessage(), "Operons data export error", token);
				e.printStackTrace();
				throw new Exception ("Operons data export error");
			}

			writer.write("Operons file created\n");
			writer.flush();
		}
		// get string and export
		gc();
		if (!((params.getNetworkRef() == null) || (params.getNetworkRef()
				.equals("")))) {
			try {
				NetworkExporter.exportString(params.getNetworkRef(), "1",
						cachePath, token);
			} catch (Exception e) {
				finishJobWithError(jobId, e.getMessage(), "STRING data export error", token);
				e.printStackTrace();
				throw new Exception ("STRING data export error");
			}
			
			writer.write("String file created\n");
			writer.flush();
		}
		return genomeName;
	}

	protected static String createDirs(String jobId, String currentDir) {
		String jobPath = null;

		if (currentDir == null) {
			jobPath = CmonkeyServerConfig.JOB_DIRECTORY + jobId + "/";
			new File(jobPath).mkdir();
		} else {
			jobPath = currentDir + "/" + jobId + "/";
			new File(jobPath).mkdir();
			CmonkeyServerConfig.DEPLOY_AWE = true;
		}
		new File(jobPath + "out/").mkdir();
		new File(jobPath + "cache/").mkdir();
		new File(jobPath + "tmp/").mkdir();

		return jobPath;
	}

	protected static String generateCmonkeyCommandLine(String jobPath,
			CmonkeyRunParameters params) {

		String outputDirectory = jobPath + "out";
		String cacheDirectory = jobPath + "cache";
		String inputFile = jobPath + "input.txt";

		String commandLine = CmonkeyServerConfig.CMONKEY_RUN_PATH
				+ " --rsat_organism my_favorite_pet --rsat_dir "
				+ cacheDirectory + " --ratios " + inputFile + " --cache "
				+ cacheDirectory + " --out " + outputDirectory
				+ " --config " + CmonkeyServerConfig.CMONKEY_DIRECTORY + "config/config.ini ";
		// Set options
		if (params.getMotifsScoring() == 0L) {
			commandLine += " --nomotifs";
		}
		if (!((params.getOperomeRef() == null) || (params.getOperomeRef()
				.equals("")))) {
			commandLine += " --operons " + cacheDirectory + "/gnc1.named";
		} else {
			commandLine += " --nooperons";
		}
		if (!((params.getNetworkRef() == null) || (params.getNetworkRef()
				.equals("")))) {
			commandLine += " --string " + cacheDirectory + "/1.gz";
			if (params.getNetworksScoring() == 0L) {
				commandLine += " --nonetworks";
			}
		} else {
			commandLine += " --nostring --nonetworks";
		}
		return commandLine;
	}

	protected static void startJob(String jobId, String desc, Long tasks,
			String token) throws UnauthorizedException, IOException,
			JsonClientException, AuthException {

		String status = "cmonkey service job started. Preparing input...";
		InitProgress initProgress = new InitProgress();
		initProgress.setPtype("task");
		initProgress.setMax(tasks);
		Date date = new Date();
		date.setTime(date.getTime() + 10080000L);
		URL jobServiceUrl = new URL(CmonkeyServerConfig.JOB_SERVICE_URL);
		UserAndJobStateClient jobClient = new UserAndJobStateClient(jobServiceUrl, new AuthToken(token));
		jobClient.startJob(jobId, AuthService.login(CmonkeyServerConfig.SERVICE_LOGIN, new String(CmonkeyServerConfig.SERVICE_PASSWORD)).getToken().toString(), status, desc, initProgress,
				dateFormat.format(date));
	}

	protected static void updateJobProgress(String jobId, String status,
			String token) throws UnauthorizedException, IOException,
			JsonClientException, AuthException {
		Date date = new Date();
		date.setTime(date.getTime() + 1000000L);
		URL jobServiceUrl = new URL(CmonkeyServerConfig.JOB_SERVICE_URL);
		UserAndJobStateClient jobClient = new UserAndJobStateClient(jobServiceUrl, new AuthToken(token));
		jobClient.updateJobProgress(jobId, AuthService.login(CmonkeyServerConfig.SERVICE_LOGIN, new String(CmonkeyServerConfig.SERVICE_PASSWORD)).getToken().toString(), status, 1L,
				dateFormat.format(date));
	}

	protected static String getJobStatus(String jobId,
			String token) throws UnauthorizedException, IOException,
			JsonClientException, AuthException {
		URL jobServiceUrl = new URL(CmonkeyServerConfig.JOB_SERVICE_URL);
		UserAndJobStateClient jobClient = new UserAndJobStateClient(jobServiceUrl, new AuthToken(token));
		String retVal = jobClient.getJobStatus(jobId).getE3();
		return retVal;
	}

	protected static void finishJob(String jobId, String wsId, String objectId,
			String status, String token) throws UnauthorizedException,
			IOException, JsonClientException, AuthException {
		String error = null;
		Results res = new Results();
		List<String> workspaceIds = new ArrayList<String>();
		workspaceIds.add(wsId + "/" + objectId);
		res.setWorkspaceids(workspaceIds);
		URL jobServiceUrl = new URL(CmonkeyServerConfig.JOB_SERVICE_URL);
		UserAndJobStateClient jobClient = new UserAndJobStateClient(jobServiceUrl, new AuthToken(token));
		jobClient.completeJob(jobId, AuthService.login(CmonkeyServerConfig.SERVICE_LOGIN, new String(CmonkeyServerConfig.SERVICE_PASSWORD)).getToken().toString(), status, error, res);
	}

	protected static void finishJobWithError(String jobId, String error, String status, String token) throws UnauthorizedException,
	IOException, JsonClientException, AuthException {
		Results res = new Results();
		URL jobServiceUrl = new URL(CmonkeyServerConfig.JOB_SERVICE_URL);
		UserAndJobStateClient jobClient = new UserAndJobStateClient(jobServiceUrl, new AuthToken(token));
		jobClient.completeJob(jobId, AuthService.login(CmonkeyServerConfig.SERVICE_LOGIN, new String(CmonkeyServerConfig.SERVICE_PASSWORD)).getToken().toString(), status, error, res);
	}


	protected static String getKbaseId(String entityType)
			throws TokenFormatException, UnauthorizedException, IOException,
			JsonClientException {
		String returnVal = null;
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
					+ idClient().allocateIdRange("kb|masthit", 1L).toString();
		} else if (entityType.equals("ExpressionSeries")) {
			returnVal = "kb|series."
					+ idClient().allocateIdRange("kb|series", 1L).toString();
		} else if (entityType.equals("ExpressionSample")) {
			returnVal = "kb|sample."
					+ idClient().allocateIdRange("kb|sample", 1L).toString();
		} else {
			System.out.println("ID requested for unknown type " + entityType);
		}
		return returnVal;
	}

	protected static void createInputTable(String jobPath,
			List<String> sampleRefs, String token) throws TokenFormatException,
			IOException, JsonClientException {

		List<ObjectData> objects = WsDeluxeUtil.getObjectsFromWsByRef(sampleRefs, token);
		List<ExpressionSample> samples = new ArrayList<ExpressionSample>();
		for (ObjectData o : objects) {
			ExpressionSample s = o.getData().asClassInstance(
					ExpressionSample.class);
			samples.add(s);
		}

		BufferedWriter writer = null;
		writer = new BufferedWriter(new FileWriter(jobPath + "input.txt"));
		writer.write("GENE");

		List<Map<String, Double>> dataCollection = new ArrayList<Map<String, Double>>();
		// make list of conditions
		for (ExpressionSample sample : samples) {
			writer.write("\t" + sample.getId());
			Map<String, Double> dataSet = sample.getExpressionLevels();
			dataCollection.add(dataSet);
		}
		// make list of genes
		List<String> geneNames = new ArrayList<String>();
		for (ExpressionSample sample : samples) {
			geneNames.addAll(sample.getExpressionLevels().keySet());
		}
		List<String> uniqueGeneNames = new ArrayList<String>(
				new HashSet<String>(geneNames));
		for (String geneName : uniqueGeneNames) {
			writer.write("\n" + geneName);
			DecimalFormat df = new DecimalFormat("0.000");
			for (Map<String, Double> dataSetMap : dataCollection) {
				if (dataSetMap.containsKey(geneName)) {
					if (dataSetMap.get(geneName).toString().matches("-.*")) {
						writer.write("\t" + df.format(dataSetMap.get(geneName)));
					} else {
						writer.write("\t "
								+ df.format(dataSetMap.get(geneName)));
					}
				} else {
					writer.write("\tNA");
				}
			}
		}
		writer.write("\n");
		writer.close();
	}

	protected static Integer executeCommand(String commandLine, String jobPath,
			String jobId, String token) throws InterruptedException,
			IOException {
		Integer exitVal = null;
		Process p = Runtime.getRuntime().exec(commandLine, null,
				new File(CmonkeyServerConfig.CMONKEY_DIRECTORY));

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
		return exitVal;

	}

	protected static String parseCmonkeySql(String sqlFile,
			CmonkeyRunResult cmonkeyRunResult, String genomeName)
			throws ClassNotFoundException, SQLException, IOException,
			JsonClientException, NullPointerException {
		CmonkeySqlite database = new CmonkeySqlite(sqlFile);
		String status = database.buildCmonkeyRunResult(cmonkeyRunResult,
				genomeName);
		database.disconnect();
		return status;
	}

	protected static void deleteFilesByPattern(String folder,
			final String pattern) {
		File dir = new File(folder);
		File fileDelete;

		for (String file : dir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.matches(pattern);
			}
		})) {
			String temp = new StringBuffer(folder).append(File.separator)
					.append(file).toString();
			fileDelete = new File(temp);
			boolean isdeleted = fileDelete.delete();
			System.out.println("file : " + temp + " is deleted : " + isdeleted);
		}
	}

	/*
	 * public static CmonkeyRunResult buildCmonkeyNetwork(ExpressionSeries
	 * series, CmonkeyRunParameters params, String jobId, String token, String
	 * currentDir) throws Exception { CmonkeyRunResult cmonkeyRunResult = new
	 * CmonkeyRunResult();
	 * cmonkeyRunResult.setId(getKbaseId("CmonkeyRunResult")); String jobPath =
	 * null; if (currentDir == null) { jobPath = JOB_PATH + jobId + "/"; new
	 * File(jobPath).mkdir(); } else { jobPath = currentDir + "/" + jobId + "/";
	 * awe = true; }
	 * 
	 * // prepare input
	 * 
	 * String inputTable = getInputTable(series); FileWriter writer = new
	 * FileWriter(jobPath + "serveroutput.txt"); writer.write(inputTable);
	 * writer.flush();
	 * 
	 * // check list of genes String organismName = getOrganismName(wsName,
	 * series.getKbId(), token); writer.write("Organism name = " + organismName
	 * + "\n"); writer.flush(); String organismCode = getKeggCode(organismName);
	 * writer.write("Organism code = " + organismCode + "\n"); writer.flush();
	 * 
	 * // save input file writeInputFile(jobPath + "input.txt", inputTable); //
	 * generate command line String commandLine =
	 * generateCmonkeyCommandLine(jobPath, params, organismCode);
	 * writer.write(commandLine + "\n"); writer.flush(); // run if (jobId !=
	 * null) updateJobProgress(jobId,
	 * "Input prepared. Starting cMonkey program...", token); Integer exitVal =
	 * executeCommand(commandLine, jobPath, jobId, token); if (exitVal != null)
	 * { writer.write("ExitValue: " + exitVal.toString() + "\n");
	 * writer.flush(); } else { writer.write("ExitValue: null\n");
	 * writer.flush(); }
	 * 
	 * // parse results
	 * 
	 * if (jobId != null) updateJobProgress(jobId,
	 * "cMonkey finished. Processing output...", token);
	 * 
	 * String sqlFile = jobPath + "out/cmonkey_run.db"; writer.write(sqlFile +
	 * "\n"); writer.flush(); parseCmonkeySql(sqlFile, cmonkeyRunResult); String
	 * resultId = getKbaseId("CmonkeyRunResult"); writer.write(resultId + "\n");
	 * cmonkeyRunResult.setId(resultId);
	 * 
	 * writer.close(); // clean up if (awe == false) {
	 * Runtime.getRuntime().exec("rm -r " + jobPath);
	 * Runtime.getRuntime().exec("rm " + JOB_PATH + "cmonkey-checkpoint*"); }
	 * 
	 * return cmonkeyRunResult; }
	 */

	public static void deleteDirectoryRecursively(File startFile) {
		if (startFile.isDirectory()) {
			for (File f : startFile.listFiles()) {
				deleteDirectoryRecursively(f);
			}
			startFile.delete();
		} else {
			startFile.delete();
		}
	}

	public static void gc() {
		Object obj = new Object();
		@SuppressWarnings("rawtypes")
		java.lang.ref.WeakReference ref = new java.lang.ref.WeakReference<Object>(
				obj);
		obj = null;
		while (ref.get() != null) {
			System.out.println("garbage collector");
			System.gc();
		}
	}

}
