package com.gaplotech

import akka.http.javadsl.model.HttpEntity
import akka.http.javadsl.model.MediaTypes
import akka.http.javadsl.model.RequestEntity
import akka.http.javadsl.marshalling.Marshaller
import akka.http.javadsl.unmarshalling.Unmarshaller

import akka.util.ByteString
import com.squareup.moshi.Moshi

object MoshiMarshaller {
    val moshi = Moshi.Builder().build()

    inline fun <reified T> marshaller(): Marshaller<T, RequestEntity> {
        return marshaller(moshi)
    }

    inline fun <reified T> marshaller(mapper: Moshi): Marshaller<T, RequestEntity> {
        return Marshaller.wrapEntity(
            { u -> toJSON(mapper, u, T::class.java) },
            Marshaller.stringToEntity(),
            MediaTypes.APPLICATION_JSON
        )
    }

    fun <T> byteStringUnmarshaller(expectedType: Class<T>): Unmarshaller<ByteString, T> {
        return byteStringUnmarshaller(moshi, expectedType)
    }

    fun <T> unmarshaller(expectedType: Class<T>): Unmarshaller<HttpEntity, T> {
        return unmarshaller(moshi, expectedType)
    }

    fun <T> unmarshaller(mapper: Moshi, expectedType: Class<T>): Unmarshaller<HttpEntity, T> {
        return Unmarshaller.forMediaType(MediaTypes.APPLICATION_JSON, Unmarshaller.entityToString())
            .thenApply { s -> fromJSON(mapper, s, expectedType) }
    }

    fun <T> byteStringUnmarshaller(mapper: Moshi, expectedType: Class<T>): Unmarshaller<ByteString, T> {
        return Unmarshaller.sync { s -> fromJSON(mapper, s.utf8String(), expectedType) }
    }

    fun <T> toJSON(mapper: Moshi, `object`: T, expectedType: Class<T>): String {
        return mapper.adapter(expectedType).toJson(`object`) ?: throw IllegalArgumentException("Cannot marshal to JSON: $`object`")
    }

    fun <T> fromJSON(mapper: Moshi, json: String, expectedType: Class<T>): T {
        return mapper.adapter(expectedType).fromJson(json) ?: throw IllegalArgumentException("Cannot unmarshal JSON as " + expectedType.simpleName)
    }
}
