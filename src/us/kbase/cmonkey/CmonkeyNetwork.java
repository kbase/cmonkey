
package us.kbase.cmonkey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * <p>Original spec-file type: CmonkeyNetwork</p>
 * <pre>
 * Represents network generated by a single run of cMonkey
 * string id - identifier of cMonkey-generated network
 * int iteration - number of the last iteration
 * string genome_name - organism name
 * int rows_number - number of rows
 * int columns_number - number of columns
 * int clusters_number - number of clusters
 * list<CmonkeyCluster> clusters - list of biclusters
 * @optional genome_name rows_number columns_number clusters_number clusters
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "id",
    "iteration",
    "genome_name",
    "rows_number",
    "columns_number",
    "clusters_number",
    "clusters"
})
public class CmonkeyNetwork {

    @JsonProperty("id")
    private String id;
    @JsonProperty("iteration")
    private Long iteration;
    @JsonProperty("genome_name")
    private String genomeName;
    @JsonProperty("rows_number")
    private Long rowsNumber;
    @JsonProperty("columns_number")
    private Long columnsNumber;
    @JsonProperty("clusters_number")
    private Long clustersNumber;
    @JsonProperty("clusters")
    private List<CmonkeyCluster> clusters;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public CmonkeyNetwork withId(String id) {
        this.id = id;
        return this;
    }

    @JsonProperty("iteration")
    public Long getIteration() {
        return iteration;
    }

    @JsonProperty("iteration")
    public void setIteration(Long iteration) {
        this.iteration = iteration;
    }

    public CmonkeyNetwork withIteration(Long iteration) {
        this.iteration = iteration;
        return this;
    }

    @JsonProperty("genome_name")
    public String getGenomeName() {
        return genomeName;
    }

    @JsonProperty("genome_name")
    public void setGenomeName(String genomeName) {
        this.genomeName = genomeName;
    }

    public CmonkeyNetwork withGenomeName(String genomeName) {
        this.genomeName = genomeName;
        return this;
    }

    @JsonProperty("rows_number")
    public Long getRowsNumber() {
        return rowsNumber;
    }

    @JsonProperty("rows_number")
    public void setRowsNumber(Long rowsNumber) {
        this.rowsNumber = rowsNumber;
    }

    public CmonkeyNetwork withRowsNumber(Long rowsNumber) {
        this.rowsNumber = rowsNumber;
        return this;
    }

    @JsonProperty("columns_number")
    public Long getColumnsNumber() {
        return columnsNumber;
    }

    @JsonProperty("columns_number")
    public void setColumnsNumber(Long columnsNumber) {
        this.columnsNumber = columnsNumber;
    }

    public CmonkeyNetwork withColumnsNumber(Long columnsNumber) {
        this.columnsNumber = columnsNumber;
        return this;
    }

    @JsonProperty("clusters_number")
    public Long getClustersNumber() {
        return clustersNumber;
    }

    @JsonProperty("clusters_number")
    public void setClustersNumber(Long clustersNumber) {
        this.clustersNumber = clustersNumber;
    }

    public CmonkeyNetwork withClustersNumber(Long clustersNumber) {
        this.clustersNumber = clustersNumber;
        return this;
    }

    @JsonProperty("clusters")
    public List<CmonkeyCluster> getClusters() {
        return clusters;
    }

    @JsonProperty("clusters")
    public void setClusters(List<CmonkeyCluster> clusters) {
        this.clusters = clusters;
    }

    public CmonkeyNetwork withClusters(List<CmonkeyCluster> clusters) {
        this.clusters = clusters;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        return ((((((((((((((((("CmonkeyNetwork"+" [id=")+ id)+", iteration=")+ iteration)+", genomeName=")+ genomeName)+", rowsNumber=")+ rowsNumber)+", columnsNumber=")+ columnsNumber)+", clustersNumber=")+ clustersNumber)+", clusters=")+ clusters)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
