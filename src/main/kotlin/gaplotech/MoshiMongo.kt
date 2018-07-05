package com.gaplotech

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.bson.*
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.RawBsonDocumentCodec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry


class MoshiCodecProvider: CodecProvider {
    override fun <T : Any> get(clazz: Class<T>, registry: CodecRegistry): Codec<T>? {
        return if(clazz.isAnnotationPresent(JsonClass::class.java)){
            return MoshiCodec(clazz)
        } else {
            null
        }
    }
}

class MoshiCodec<T>(private val clazz: Class<T>) : Codec<T> {
    private val rawBsonDocumentCodec = RawBsonDocumentCodec()
    private val moshiAdapter = Moshi.Builder().build().adapter(clazz)

    override fun getEncoderClass(): Class<T> = clazz

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): T {
        val bsonDocument = rawBsonDocumentCodec.decode(reader, decoderContext)
        return moshiAdapter.fromJson(bsonDocument.toJson()) ?: throw IllegalStateException("Can not prase ${clazz.name} from bson document")
    }

    override fun encode(writer: BsonWriter, value: T, encoderContext: EncoderContext) {
        val json = moshiAdapter.toJson(value)
        rawBsonDocumentCodec.encode(writer, RawBsonDocument.parse(json), encoderContext)
    }
}

