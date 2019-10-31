package com.alekseysamoylov.natsmonitoring

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.FuelError
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
import kotlin.concurrent.schedule


class Main {
}

lateinit var restHighLevelClient: RestHighLevelClient

fun main() {
    restHighLevelClient = RestHighLevelClient(
        RestClient.builder(
            HttpHost("localhost", 9200, "http"),
            HttpHost("localhost", 9201, "http")
        )
    )

    println("Press ENTER to exit")
    try {
        Timer().schedule(0L, 5000L) { uploadNatsMetricsToElastic() }
        readLine()
    } finally {
        restHighLevelClient.close()
    }
}

fun uploadNatsMetricsToElastic() {
    when (val result = requestNatsStats()) {
        is Result.Failure -> {
            val ex = result.getException()
            println(ex)
        }
        is Result.Success -> {
            proceedSuccessfulNatsMetricsResponse(result)
        }
    }
}

fun proceedSuccessfulNatsMetricsResponse(result: Result<String, FuelError>) {
    val data = result.get()
    println(data)
    val dataObj = json.parse(Data.serializer(), data)
    for (channel in dataObj.channels) {
        createElasticIndexIfAbsentAndAddDataToElastic(channel)
    }
    println(dataObj)
}

fun requestNatsStats(): Result<String, FuelError> {
    val (request, response, result) = "http://localhost:8222/streaming/channelsz?subs=1"
        .httpGet()
        .responseString()
    return result
}

fun createElasticIndexIfAbsentAndAddDataToElastic(channel: Channel) {


    val textMessage = HashMap<String, Any>()
    textMessage["type"] = "text"
    val integerMessage = HashMap<String, Any>()
    integerMessage["type"] = "integer"
    val channelProperties = HashMap<String, Any>()
    channelProperties["name"] = textMessage
    channelProperties["msgs"] = integerMessage
    val mapping = HashMap<String, Any>()
    mapping["properties"] = channelProperties

    val createIndexRequest = CreateIndexRequest(CH_INDX)
    createIndexRequest.settings(Settings.EMPTY)

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
    println("response id: " + indexResponse.id)
    println("response id: " + indexResponse.result.name)
}

@Serializable
data class Channel(val name: String, val msgs: Int)

@Serializable
data class Data(val offset: Int, val channels: List<Channel>)

private const val CH_INDX = "channel_index"
private val json = Json(
    JsonConfiguration(
        encodeDefaults = true,
        strictMode = false,
        unquoted = false,
        allowStructuredMapKeys = true,
        prettyPrint = false,
        indent = "    ",
        useArrayPolymorphism = false,
        classDiscriminator = "type"
    )
)
