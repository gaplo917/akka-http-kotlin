package com.gaplotech

import com.mongodb.async.client.MongoCollection
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.experimental.runBlocking
import org.bson.codecs.configuration.CodecRegistries
import org.litote.kmongo.async.KMongo
import org.litote.kmongo.coroutine.getCollectionOfName
import org.litote.kmongo.coroutine.insertOne
import org.litote.kmongo.coroutine.toList


@JsonClass(generateAdapter = true)
data class TestMoshi(
    val id: Int,
    val idOpt: Int?,
    val idDefaultVal: Int = 100,
    val xs: List<Int>,
    val xsDefaultVal: List<Int> = listOf(100)
)


fun main(args: Array<String>) {

    val moshi = Moshi.Builder().build()

    val jsonAdapter = moshi.adapter(TestMoshi::class.java)

    val event = jsonAdapter.fromJson("""{
        "id": 1,
        "xs": [1,2,3]
    }""".trimIndent())


    println(event)

    val url = "mongodb://localhost:27017"
    val client = KMongo.createClient(url)
    val db = { dbName: String ->
        client.getDatabase(dbName).withCodecRegistry(
            CodecRegistries.fromProviders(MoshiCodecProvider())
        )
    }
    val collection: MongoCollection<Order> = db("testing").getCollectionOfName("order")

    runBlocking {
        collection.insertOne(Order(id = 1, idOpt = 2, xs = listOf(1,2,3)))
        println(collection.find().toList())
    }


}
