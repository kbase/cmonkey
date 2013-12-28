package us.kbase.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.Tuple5;
import us.kbase.common.service.Tuple7;
import us.kbase.common.service.UObject;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.idserverapi.IDServerAPIClient;
import us.kbase.kbasegenomes.ContigSet;
import us.kbase.kbasegenomes.Feature;
import us.kbase.kbasegenomes.Genome;
import us.kbase.kbasegenomes.Contig;

public class GenomeImporter {
	
	private static final String FEATURES = "_features";
	private static final String FEATURENAMES = "_feature_names";
	private static final String ID_SERVICE_URL = "http://kbase.us/services/idserver";

	private static IDServerAPIClient _idClient = null;

	private String filePrefix = "";
	private String workDir = "/home/kbase/cmonkey20131126/cache/";
	private String token = null;
	private String wsId = null;

	
	public GenomeImporter (String prefix, String workDir, String wsId, String token) throws Exception{
		if (prefix != null) this.filePrefix = prefix;
		if (workDir != null) this.workDir = workDir;
		if (token == null) {
			throw new Exception("Token not assigned");
		} else {
			this.token = token;
		}
		
		if (wsId == null) {
			throw new Exception("Workspace name not assigned");
		} else {
			this.wsId = wsId;
		}
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

	
	public Genome readGenome (){
		Genome genome = new Genome();
		List<Feature> features = readFeatures(genome);
		genome.setFeatures(features);
		genome.setDomain("Archaea");
		genome.setGeneticCode(0L);
		genome.setSource("undefined");
		genome.setSourceId("undefined");
		List<Tuple7<Long, String, String, String, String, String, String>> publications = new ArrayList<Tuple7<Long,String,String,String,String,String,String>>();
		genome.setPublications(publications);
		genome.setId(getKbaseId("Genome"));
		WsDeluxeUtil.saveObjectToWorkspace(UObject.transformObjectToObject(genome, UObject.class), "KBaseGenomes.Genome-1.0", wsId, genome.getId(), token);
		
		return genome;
	}
	
	public List<String[]> readFeatureNames (){
		List<String[]> returnVal = new ArrayList<String[]>();
		String fileName = workDir + filePrefix + FEATURENAMES;
		BufferedReader br = null;
		try {
			String line = null;
			br = new BufferedReader(new FileReader(fileName));
			while ((line = br.readLine()) != null) {
				if (line.equals("")) {
					// do nothing
				} else if (line.matches("-- *")) {
					// do nothing
				} else {
					String[] fields = line.split("\t");
					returnVal.add(fields);
				}
			}
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					System.out.println(e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
		}
		
		return returnVal;
	}
	
	public List<Feature> readFeatures (Genome genome){
		
		List<Feature> returnVal = new ArrayList<Feature>();
		
		String fileName = workDir + filePrefix + FEATURES;
		
		List<String[]> featureNames = readFeatureNames();
		HashMap<String, String> contigNames = new HashMap<String, String>();
		
		List<String> featureStrings = new ArrayList<String>();
		
		try {
			String line = null;
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			while ((line = br.readLine()) != null) {
				if (line.equals("")) {
					// do nothing
				} else if (line.matches("-- .*")) {
					// do nothing
				} else {
					//System.out.println(line);
					featureStrings.add(line);
					String[] fields = line.split("\t");
					contigNames.put(fields[3], "contig");
				}
			}
			br.close();
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
		}
		
		ContigSet contigSet = readContigs(contigNames);
		contigSet.setSourceId(this.filePrefix);

		byte[] bytesOfMessage = null;
		try {
			bytesOfMessage = contigSet.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] thedigest = md.digest(bytesOfMessage);
		System.out.println("MD5 = " + thedigest);
		contigSet.setMd5(thedigest.toString());
				
		
		
		HashMap<String, String> contigIds = new HashMap<String, String>();
		for (Contig contig : contigSet.getContigs()){
			contigIds.put(contig.getName(), contig.getId());
		}
		List<String> contigIdsList = new ArrayList<String>(contigIds.values());
		genome.setContigIds(contigIdsList);
		String organismName = null;

		
		for (String line: featureStrings){

			String[] fields = line.split("\t");
			Feature feature = new Feature();
			String featureId = fields[0];
			
			feature.setType(fields[1]);
			List<String> aliases = new ArrayList<String>();
			aliases.add(0, featureId); //feature id must be the first element in aliases list  
			aliases.add(1, fields[2]); //primary feature name must be the second element in aliases list  
			aliases.add(2, fields[10]);//gene id must be the third element in aliases list 

			for (String[] featureName : featureNames){
				if (featureName[0].equalsIgnoreCase(featureId)){
					if (featureName[2].equalsIgnoreCase("primary")){
						if (aliases.get(1) == null){
							aliases.add(1, featureName[1]); //if there is no primary name, put it now. Otherwise, do nothing
						}
					} else {
						aliases.add(3, featureName[1]); //put all other aliases into aliases list 
					}
				}
			}
			
			feature.setAliases(aliases);
/*			if (feature.getId() == null){
				feature.setId(feature.getAliases().get(0)); // feature ID would be feature ID if the feature has no primary name 
			}
*/
			feature.setId(getKbaseId("Feature")); // no more external IDs for features!
			
			Tuple5<String, String, Long, String, Long> region = new Tuple5<String, String, Long, String, Long>();
			region.setE1(fields[3]);
			region.setE2(wsId + "/" + contigSet.getId() + "/" + fields[3]);
			region.setE3(Long.parseLong(fields[4]));
			region.setE4(fields[6]);
			region.setE5(Long.parseLong(fields[5]) - Long.parseLong(fields[4]) + 1L);
			List<Tuple5<String, String, Long, String, Long>> location = new ArrayList<Tuple5<String,String,Long,String,Long>>();
			location.add(region);
			feature.setLocation(location);
			feature.setFunction(fields[7]);
			organismName = fields[9];
			
			try {
				bytesOfMessage = feature.toString().getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				md = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			thedigest = md.digest(bytesOfMessage);
			System.out.println("MD5 = " + thedigest);
			feature.setMd5(thedigest.toString());

			returnVal.add(feature);
			
		}
		
		contigSet.setSource(organismName);
		genome.setContigsetRef(wsId + "/" + contigSet.getId());
		genome.setScientificName(organismName);

		WsDeluxeUtil.saveObjectToWorkspace(UObject.transformObjectToObject(contigSet, UObject.class), "KBaseGenomes.ContigSet-1.0", wsId, contigSet.getId(), token);
		
		return returnVal;

	}

	
	
	
	
	
	
	
	
	private ContigSet readContigs(
			HashMap<String, String> contigNames) {
		
		ContigSet contigSet = new ContigSet();
		List<Contig> contigs = new ArrayList<Contig>();
		
		for (String accession : contigNames.keySet()){
			String fileName = this.workDir + this.filePrefix + "_" + accession;
			File file = new File(fileName);
			String sequence = null;
			if (file.exists()) {
				BufferedReader br = null;
				try {
					String line = null;
					br = new BufferedReader(new FileReader(fileName));
					while ((line = br.readLine()) != null) {
						sequence += line;
					}
				} catch (IOException e) {
					System.out.println(e.getLocalizedMessage());
				} finally {
					if (br != null) {
						try {
							br.close();
						} catch (IOException e) {
							System.out.println(e.getLocalizedMessage());
							e.printStackTrace();
						}
					}
				}

				String id = getKbaseId("Contig");
				Contig contig = new Contig().withId(id).withSequence(sequence).withName(accession);
				byte[] bytesOfMessage = null;
				try {
					bytesOfMessage = sequence.getBytes("UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				MessageDigest md = null;
				try {
					md = MessageDigest.getInstance("MD5");
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				byte[] thedigest = md.digest(bytesOfMessage);
				System.out.println("MD5 = " + thedigest);
				contig.setMd5(thedigest.toString());
				contigs.add(contig);

			} else {
				System.out.println("Contig " + accession + " sequence not found : " + fileName);
			}
		}

		contigSet.setContigs(contigs);
		contigSet.setType("Organism");
		contigSet.setId(getKbaseId("ContigSet"));
		return contigSet;
	}

	protected static String getKbaseId(String entityType) {
		String returnVal = null;

		try {
			if (entityType.equals("Feature")) {
				returnVal = "kb|feature."
						+ idClient().allocateIdRange("kb|feature", 1L)
								.toString();
			} else if (entityType.equals("Genome")) {
				returnVal = "kb|genome."
						+ idClient().allocateIdRange("kb|genome", 1L)
								.toString();
			} else if (entityType.equals("Contig")) {
				returnVal = "kb|contig."
						+ idClient().allocateIdRange("kb|contig", 1L)
								.toString();
			} else if (entityType.equals("ContigSet")) {
				returnVal = "kb|contigset."
						+ idClient().allocateIdRange("kb|contigset", 1L)
								.toString();
			} else if (entityType.equals("ProteinFamily")) {
				returnVal = "kb|proteinfamily."
						+ idClient().allocateIdRange("kb|proteinfamily", 1L)
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


}
