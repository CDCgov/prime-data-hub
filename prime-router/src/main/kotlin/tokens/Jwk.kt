package gov.cdc.prime.router.tokens

import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey

import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyType
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.security.PublicKey
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.RSAPrivateKey

/**
 * I could not find a good library that would easily let us parse and create Jwk data
 * in both JSON and Yaml.  (Yes, a yaml jwk is an oxymoron)
 *
 * A.  com.nimbusds.jose.jwk.JWKSet does not deserialize and serialize properly to/from JSON.
 * When you serialize, it puts the important values down inside a generic 'requiredParams' field.
 * Then when you later attempt to deserialize it back into a JWKSet, it fails.
 *
 * ECPublicKey does NOT track the kid (keyId), which is a requirement for FHIR Auth, since we only use
 * keys that match the kid in the sender's signed JWT.
 *
 * And you can't create an ECPublicKey directly from a JWT object - you have to re-create the JSON string,
 * then parse that back in!
 *
 * However, nimbusds has two features that redeem it somewhat:
 * 1) It has "usds" in its name
 * 2) Its ECKey and RSAKey classes know how to parse PEM files, into its ECPublicKey or RSAPublicKey objects
 *
 * JJWT does not have good support for creating JWKs from raw JSON.
 *
 * mime type application/jwk+json
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class Jwk(
    val kty: String,            // Key type (Alg family).  eg, RSA, EC, oct
    val use: String? = null,    // Intended use.  eg, sig, enc.
    val keyOps: String? = null, // key_ops: Intended use operations.  eg, sign, verify, encrypt
    val alg: String? = null,
    var kid: String? = null,    // key Id
    val x5u: String? = null,    // URI ref to certificate
    val x5c: List<String>? = null, // PKIX certificates. JSON array of String
  val x5t: String? = null,    // certificate thumbprint
  // Algorithm specific fields
  val n: String? = null,      // RSA
  val e: String? = null,      // RSA
  val d: String? = null,      // EC and RSA private
  val crv: String? = null,    // EC
  val p: String? = null,      // RSA private
  val q: String? = null,      // RSA private
  val dp: String? = null,     // RSA private
  val dq: String? = null,     // RSA private
  val qi: String? = null,     // RSA private
  val x: String? = null,      // EC
  val y: String? = null,      // EC
  val k: String? = null,      // symmetric key, eg oct
) {

    fun toECPublicKey(): ECPublicKey {
        if (kty != "EC") error("Cannot convert key type $kty to ECPublicKey")
        return generateECPublicKey(jacksonObjectMapper().writeValueAsString(this))
    }

    fun toECprivateKey(): ECPrivateKey {
        if (kty != "EC") error("Cannot convert key type $kty to ECPrivateKey")
        return generateECPrivateKey(jacksonObjectMapper().writeValueAsString(this))
    }

    fun toRSAPublicKey(): RSAPublicKey {
        if (kty != "RSA") error("Cannot convert key type $kty to RSAPublicKey")
        return generateRSAPublicKey(jacksonObjectMapper().writeValueAsString(this))
    }

    fun toRSAPrivateKey(): RSAPrivateKey {
        if (kty != "RSA") error("Cannot convert key type $kty to RSAPrivateKey")
        return generateRSAPrivateKey(jacksonObjectMapper().writeValueAsString(this))
    }

    companion object {
        fun generateECPublicKey(jwkString: String): ECPublicKey {
            return ECKey.parse(jwkString).toECPublicKey()
        }

        fun generateECPrivateKey(jwkString: String): ECPrivateKey {
            return ECKey.parse(jwkString).toECPrivateKey()
        }

        fun generateRSAPublicKey(jwkString: String): RSAPublicKey {
            return RSAKey.parse(jwkString).toRSAPublicKey()
        }

        fun generateRSAPrivateKey(jwkString: String): RSAPrivateKey {
            return RSAKey.parse(jwkString).toRSAPrivateKey()
        }
    }
}


@JsonInclude(JsonInclude.Include.NON_NULL)
data class JwkSet(
    // A scope is a space separated list of allowed scopes, per OpenID.
    // The official JWK Set spec only calls for 'keys', but custom fields like this are allowed.
    // Syntax for scopes:
    // System scopes have the format system/(:resourceType|*).(read|write|*),
    // which conveys the same access scope as the matching user format user/(:resourceType|*).(read|write|*).
    val scope: String,
    // Each scope has a list of keys associated with it.  Having a list of keys allows for
    // overlapping key rotation
    val keys: List<Jwk>
) {
    fun filterByKid(kid: String): List<Jwk> {
        return keys.filter { !it.kid.isNullOrEmpty() && kid == it.kid }
    }
}

/*   DELETE THIS
// This requires   @JsonDeserialize(contentUsing = PublicKeyDeserializer::class)
class PublicKeyDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<PublicKey?>(vc) {

    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): PublicKey {
        val node: JsonNode = jp.codec.readTree(jp)
        val json = node.toString()
        val keyType = node["kty"].textValue()
        if (keyType.isNullOrBlank()) error("Cannot convert this json to a public key obj: $json")
        return when (keyType) {
            "EC" -> com.nimbusds.jose.jwk.ECKey.parse(json).toECPublicKey()
            "RSA" -> com.nimbusds.jose.jwk.RSAKey.parse(json).toRSAPublicKey()
            else -> error("Unsupported Key type $keyType")
        }
    }
}
*/

/*  NOT SURE IF THIS IS NEEDED.  We want to generate a JwkSet from a PEMfile.
        fun generateFromPemFile(pemFile: File): JWK {
            if (!pemFile.exists()) error("Unable to find pem file ${pemFile.absolutePath}")
            val jwk = com.nimbusds.jose.jwk.JWK.parseFromPEMEncodedObjects(pemFile.readText())
            jwk.
            return generateFromJson(jwkJsonString)
        }

        fun generateFromPpkFile(ppkFile: File): Jwk
        {
        }


        fun generateFromJson(json: String): Jwk {
            val foo = com.nimbusds.jose.jwk.JWKSet.parse(json)
            foo.
            return jacksonObjectMapper().readValue(json, Jwk::class.java)
        }
*/

/* DELETE THIS
class JWKSetSerializer @JvmOverloads constructor(t: Class<JWKSet?>? = null) : StdSerializer<JWKSet>(t) {
    override fun serialize(
        value: JWKSet, jgen: JsonGenerator, provider: SerializerProvider
    ) {
        jgen.writeStartObject()
        jgen.writeArrayFieldStart("keys")
        value.keys.forEach {
//            jgen.writeStringField("kty", it.keyType.toString())
//            jgen.writeStringField("kid", it.keyID)
//            jgen.writeNumberField("owner", value.owner.id)
            jgen.writeObject(it)
        }
        jgen.writeEndArray()
        jgen.writeEndObject()
    }
}
*/
/*
class JWKSetDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<JWKSet?>(vc) {

    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): JWKSet {
        val node: JsonNode = jp.codec.readTree(jp)
        val json = node.toString()
        val jwkSet = JWKSet.parse(json)
        return jwkSet
    }
}
*/
