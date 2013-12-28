package us.kbase.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import us.kbase.auth.TokenFormatException;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.UObject;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.expressionservices.ExpressionSample;
import us.kbase.expressionservices.ExpressionSeries;
import us.kbase.idserverapi.IDServerAPIClient;
import us.kbase.kbasegenomes.Feature;
import us.kbase.kbasegenomes.Genome;

public class ExpressionSeriesImporter {

	private static final String ID_SERVICE_URL = "http://kbase.us/services/idserver";

	private static IDServerAPIClient _idClient = null;

	private String fileName = null;
	private String token = null;
	private String workspaceName = null;
	private Genome genome;
	private HashMap<String, String> aliases;

	
	public ExpressionSeriesImporter (String genomeRef, String fileName, String workspaceName, String token) throws Exception{
		if (fileName == null) {
			System.out.println("Expression data file name required");
		} else {
			this.fileName = fileName;
		}
		if (token == null) {
			throw new Exception("Token not assigned");
		} else {
			this.token = token;
		}
		
		if (workspaceName == null) {
			throw new Exception("Workspace name not assigned");
		} else {
			this.workspaceName = workspaceName;
		}
		
		this.genome = WsDeluxeUtil.getObjectFromWsByRef(genomeRef, token).getData().asClassInstance(Genome.class);
		this.aliases = readFeatures(this.genome);
		

	}
	
	private HashMap<String,String> readFeatures (Genome genome) {
		HashMap<String,String> aliases = new HashMap<String, String>(); 
		for (Feature f : genome.getFeatures()){
			String id = f.getId();
			for (String a : f.getAliases()){
				//System.out.println("alias = " + a + " : id = " + id);
				aliases.put(a, id);
			}
		}
		return aliases;
	}

	protected static IDServerAPIClient idClient() {
		if (_idClient == null) {
			URL idServerUrl = null;
			try {
				idServerUrl = new URL(ID_SERVICE_URL);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			_idClient = new IDServerAPIClient(idServerUrl);
		}
		return _idClient;
	}
	
	public List<String> importExpressionSeriesFile() throws TokenFormatException, IOException, JsonClientException {
		List<String> result = new ArrayList<String>();
		ExpressionSeries series = new ExpressionSeries();
		try {
			series.setKbId(getKbaseId("ExpressionSeries"));//("TestExpressionSeries");//
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		series.setSourceId(fileName);
		series.setExternalSourceDate("unknown");
		result.add(series.getKbId());

		List<String> sampleRefs = new ArrayList<String>();
		List<String> conditions = new ArrayList<String>();
		List<HashMap<String, Double>> dataValues = new ArrayList<HashMap<String, Double>>();
		
		Long samplesNumber = 0L;
		try {
			String line = null;
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			while ((line = br.readLine()) != null) {
				if (line.equals("")) {
					// do nothing
				} else if (line.matches("GENE\t.*")) {
					line = line.substring(5);
					String[] fields = line.split("\t");
					for (String field : fields) {
						conditions.add(field);
						 //System.out.println(field);
						samplesNumber++;
					}
					for (Integer i = 0; i < samplesNumber; i++) {
						HashMap<String, Double> dataValue = new HashMap<String, Double>();
						dataValues.add(dataValue);
					}

				} else {
					String[] fields = line.split("\t");
					Integer j = 0;
					String id = getFeatureId(fields[0]);
					if (id != null){
						while (j < samplesNumber) {
							dataValues.get(j).put(getFeatureId(fields[0]),
									Double.valueOf(fields[j + 1]));
							 //System.out.println(fields[0]+" "+fields[j+1]);
							j++;
						}
					} else {
						System.out.println("id not found : " + fields[0]);
					}
				}
			}
			br.close();
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
		}

		 System.out.println(conditions.toString());
		Long startSamplesId = getKbaseIds("ExpressionSample", samplesNumber);
		for (Integer i = 0; i < samplesNumber; i++) {
			ExpressionSample sample = new ExpressionSample();
			//Integer sampleNo = i+1;
			sample.setKbId("kb|sample." + startSamplesId.toString());//("TestExpressionSample"+sampleNo.toString());//
			startSamplesId++;
			sample.setSourceId(conditions.get(i));
			sample.setType("microarray");
			sample.setNumericalInterpretation("undefined");
			sample.setExternalSourceDate("undefined");
			sample.setGenomeId("kb|genome.1");
			sample.setExpressionLevels(dataValues.get(i));
			sampleRefs.add(workspaceName + "/" + sample.getKbId());
			WsDeluxeUtil.saveObjectToWorkspace(UObject.transformObjectToObject(sample, UObject.class), "ExpressionServices.ExpressionSample-1.0", workspaceName, sample.getKbId(), token.toString());
			result.add(sample.getKbId());
						
		}
		series.setExpressionSampleIds(sampleRefs);
		WsDeluxeUtil.saveObjectToWorkspace(UObject.transformObjectToObject(series, UObject.class), "ExpressionServices.ExpressionSeries-1.0", workspaceName, series.getKbId(), token.toString());
		return result;
	}

	private String getFeatureId(String featureAlias) {
		String featureId = this.aliases.get(featureAlias);
		return featureId;
	}
	
	protected static String getKbaseId(String entityType) {
		String returnVal = null;
		try {
			if (entityType.equals("ExpressionSeries")) {
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

	protected static Long getKbaseIds (String entityType, Long count) {

		Long returnVal = null;

		try {
			if (entityType.equals("ExpressionSeries")) {
				returnVal = idClient().allocateIdRange("kb|series", 1L);
			} else if (entityType.equals("ExpressionSample")) {
				returnVal = idClient().allocateIdRange("kb|sample", 1L);
			} else {
				System.err.println("ID requested for unknown type "
						+ entityType);
				return null;
			}
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

}
