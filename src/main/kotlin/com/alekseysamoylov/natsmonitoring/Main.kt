package com.alekseysamoylov.natsmonitoring

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.apache.http.HttpHost
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.XContentType
import java.util.*


class Main {
}

fun main() {
    val (request, response, result) = "http://localhost:8222/streaming/channelsz?subs=1"
        .httpGet()
        .responseString()
    val json = Json(JsonConfiguration(
        encodeDefaults = true,
        strictMode = false,
        unquoted = false,
        allowStructuredMapKeys = true,
        prettyPrint = false,
        indent = "    ",
        useArrayPolymorphism = false,
        classDiscriminator = "type"
    ))

    when (result) {
        is Result.Failure -> {
            val ex = result.getException()
            println(ex)
        }
        is Result.Success -> {
            val data = result.get()
            println(data)
            val dataObj = json.parse(Data.serializer(), data)
            println(dataObj)
        }
    }



}


fun createElasticIndexIfAbsentAndAddDataToElastic(channel: Channel) {
    val restHighLevelClient = RestHighLevelClient(
        RestClient.builder(
            HttpHost("localhost", 9200, "http"),
            HttpHost("localhost", 9201, "http")))

    val dataMessage = HashMap<String, Any>()


    val channelMessage = HashMap<String, Any>()
    channelMessage["type"] = "text"
    val properties = HashMap<String, Any>()
    properties["userId"] = channelMessage
    properties["name"] = channelMessage
    val mapping = HashMap<String, Any>()
    mapping["properties"] = properties

    val createIndexRequest = CreateIndexRequest(CH_INDX)
    createIndexRequest.settings( Settings.builder()
        .put("index.number_of_shards", 1)
        .put("index.number_of_replicas", 2))

    createIndexRequest.mapping(mapping)

    val getIndexRequest = GetIndexRequest(CH_INDX)
    val exists = restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT)
    if (!exists) {
        val indexResponse = restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT)
        println("response id: " + indexResponse.index())
    }

    val request3 = IndexRequest(CH_INDX)
    request3.id(UUID.randomUUID().toString())
    request3.source(ObjectMapper().writeValueAsString(channel), XContentType.JSON)
    val indexResponse = restHighLevelClient.index(request3, RequestOptions.DEFAULT)
    println("response id: " + indexResponse.getId())
    println("response id: " + indexResponse.getResult().name)


    restHighLevelClient.close()
}

@Serializable
data class Channel(val name: String, val msgs: Int)
@Serializable
data class Data(val offset: Int, val channels: List<Channel>)
private const val CH_INDX = "channel_index"
